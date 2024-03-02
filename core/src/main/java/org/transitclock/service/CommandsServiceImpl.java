/* (C)2023 */
package org.transitclock.service;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.TemporalMatch;
import org.transitclock.core.VehicleState;
import org.transitclock.core.avl.AvlReportProcessor;
import org.transitclock.core.dataCache.PredictionDataCache;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.domain.hibernate.DataDbLogger;
import org.transitclock.domain.hibernate.HibernateUtils;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.VehicleEvent;
import org.transitclock.domain.structs.VehicleToBlockConfig;
import org.transitclock.service.contract.CommandsInterface;
import org.transitclock.service.dto.IpcAvl;
import org.transitclock.service.dto.IpcVehicleComplete;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;

@Slf4j
@Component
public class CommandsServiceImpl implements CommandsInterface {
    @Autowired
    private PredictionDataCache predictionDataCache;
    @Autowired
    private VehicleDataCache vehicleDataCache;
    @Autowired
    private AvlProcessor avlProcessor;
    @Autowired
    private VehicleStateManager vehicleStateManager;
    @Autowired
    private DataDbLogger dataDbLogger;
    @Autowired
    private AvlReportProcessor avlReportProcessor;

    public CommandsServiceImpl() {
    }

    /**
     * Called on server side via RMI when AVL data is to be processed
     *
     * @param avlData AVL data sent to server
     * @return Null if OK, otherwise an error message
     */
    @Override
    public String pushAvl(IpcAvl avlData) {
        // Use AvlExecutor to actually process the data using a thread executor
        AvlReport avlReport = new AvlReport(avlData);
        logger.debug("Processing AVL report {}", avlReport);
        avlReportProcessor.process(avlReport);

        // Return that was successful
        return null;
    }

    /**
     * Called on server side via RMI when AVL data is to be processed
     *
     * @param avlDataCollection AVL data sent to server
     * @return Null if OK, otherwise an error message
     */
    @Override
    public String pushAvl(Collection<IpcAvl> avlDataCollection) {
        for (IpcAvl avlData : avlDataCollection) {
            // Use AvlExecutor to actually process the data using a thread executor
            AvlReport avlReport = new AvlReport(avlData);
            logger.debug("Processing AVL report {}", avlReport);
            avlReportProcessor.process(avlReport);
        }

        // Return that was successful
        return null;
    }

    @Override
    public void setVehicleUnpredictable(String vehicleId) {
        VehicleState vehicleState = vehicleStateManager.getVehicleState(vehicleId);

        // Create a VehicleEvent to record what happened
        AvlReport avlReport = vehicleState.getAvlReport();
        TemporalMatch lastMatch = vehicleState.getMatch();
        boolean wasPredictable = vehicleState.isPredictable();

        String vehicleEvent = "Command called to make vehicleId unpredicable. ";
        String eventDescription = "Command called to make vehicleId unpredicable. ";
        VehicleEvent vehicleEvent1 = new VehicleEvent(
                avlReport,
                lastMatch,
                vehicleEvent,
                eventDescription,
                false, // predictable
                wasPredictable, // becameUnpredictable
                null);// supervisor
        dataDbLogger.add(vehicleEvent1);


        // Update the state of the vehicle
        vehicleState.setMatch(null);

        // Remove the predictions that were generated by the vehicle
        predictionDataCache.removePredictions(vehicleState);

        // Update VehicleDataCache with the new state for the vehicle
        vehicleDataCache.updateVehicle(vehicleState);
    }

    private VehicleState getVehicleStateForTrip(String tripId, LocalDateTime _startTripTime) {
        /* The startTripTime parameter should not be null if noSchedule */
        long startTripTime = 0;
        if (_startTripTime != null)
            startTripTime = _startTripTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L;
        /*
         * Get the vehicle associated to the tripId. Is it possible to have more than 1 bus with the
         * same tripId??
         */
        Collection<IpcVehicleComplete> ipcVehicleCompletList =
                vehicleDataCache.getVehiclesIncludingSchedBasedOnes();
        VehicleState vehicleState = null;
        for (IpcVehicleComplete _ipcVehicle : ipcVehicleCompletList) {

            if (_ipcVehicle.getTripId() != null && _ipcVehicle.getTripId().compareTo(tripId) == 0) {
                VehicleState _vehicleState = vehicleStateManager.getVehicleState(_ipcVehicle.getId());
                boolean noSchedule = _vehicleState.getTrip().isNoSchedule();
                if (!noSchedule) {
                    vehicleState = _vehicleState;
                    break;
                } else if (noSchedule && _ipcVehicle.getTripStartEpochTime() == startTripTime) {
                    vehicleState = _vehicleState;
                    break;
                }
            }
        }
        return vehicleState;
    }

    @Override
    public String cancelTrip(String tripId, LocalDateTime startTripTime) {

        // String vehicleId=	"block_" + blockId + "_schedBasedVehicle";
        VehicleState vehicleState = this.getVehicleStateForTrip(tripId, startTripTime);
        if (vehicleState == null) return "TripId id is not currently available";

        AvlReport avlReport = vehicleState.getAvlReport();
        if (avlReport != null) {
            vehicleState.setCanceled(true);
            vehicleDataCache.updateVehicle(vehicleState);
            avlProcessor.processAvlReport(avlReport);
            return null;
        } else return "vehicle with this trip id does not have avl report";
    }

    @Override
    public String reenableTrip(String tripId, LocalDateTime startTripTime) {

        // String vehicleId=	"block_" + blockId + "_schedBasedVehicle";
        VehicleState vehicleState = this.getVehicleStateForTrip(tripId, startTripTime);
        if (vehicleState == null) return "TripId id is not currently available";
        AvlReport avlReport = vehicleState.getAvlReport();
        if (avlReport != null) {
            vehicleState.setCanceled(false);
            vehicleDataCache.updateVehicle(vehicleState);
            avlProcessor.processAvlReport(avlReport);
            return null;
        } else return "vehicle with this trip id does not have avl report";
    }

    @Override
    public String addVehicleToBlock(
            String vehicleId, String blockId, String tripId, Date assignmentDate, Date validFrom, Date validTo) {
        VehicleToBlockConfig vehicleToBlockConfig = new VehicleToBlockConfig(vehicleId, blockId, tripId, assignmentDate, validFrom, validTo);
        dataDbLogger.add(vehicleToBlockConfig);
        return null;
    }

    @Override
    public String removeVehicleToBlock(long id) {
        Session session = HibernateUtils.getSession();
        try {
            VehicleToBlockConfig.deleteVehicleToBlockConfig(id, session);
            session.close();
        } catch (Exception ex) {
            session.close();
        }
        return null;
    }
}
