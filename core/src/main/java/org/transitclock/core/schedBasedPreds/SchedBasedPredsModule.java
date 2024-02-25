/* (C)2023 */
package org.transitclock.core.schedBasedPreds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.transitclock.Module;
import org.transitclock.config.data.AgencyConfig;
import org.transitclock.config.data.PredictionConfig;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.BlockInfoProvider;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.domain.structs.AvlReport;
import org.transitclock.domain.structs.AvlReport.AssignmentType;
import org.transitclock.domain.structs.Block;
import org.transitclock.domain.structs.Location;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.service.dto.IpcVehicle;
import org.transitclock.service.dto.IpcVehicleComplete;
import org.transitclock.utils.SystemTime;
import org.transitclock.utils.Time;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The schedule based predictions module runs in the background. Every few minutes it looks for
 * blocks that do not have an associated vehicle. For these blocks the module creates a schedule
 * based vehicle at the location of the beginning of the block and generates predictions for the
 * entire block that are based on the scheduled departure time. The purpose of this module is to
 * generate predictions well in advance even if vehicles are assigned just a few minutes before a
 * vehicle is scheduled to start a block. This feature should of course only be used if most of the
 * time the blocks are actually run. It should not be used for agencies such as SFMTA where
 * blocks/trips are often missed because would then be often providing predictions when no vehicle
 * will arrive.
 *
 * <p>Schedule based predictions are removed once a regular vehicle is assigned to the block or the
 * schedule based vehicle is timed out via TimeoutHandlerModule due to it being
 * transitclock.timeout.allowableNoAvlForSchedBasedPredictions after the scheduled departure time
 * for the assignment.
 *
 * @author SkiBu Smith
 */
@Slf4j
@Component
public class SchedBasedPredsModule extends Module {
    private final VehicleDataCache vehicleDataCache;
    private final AvlProcessor avlProcessor;
    private final BlockInfoProvider blockInfoProvider;
    private final DbConfig dbConfig;

    public SchedBasedPredsModule(VehicleDataCache vehicleDataCache, AvlProcessor avlProcessor, BlockInfoProvider blockInfoProvider, DbConfig dbConfig) {
        this.vehicleDataCache = vehicleDataCache;
        this.avlProcessor = avlProcessor;
        this.blockInfoProvider = blockInfoProvider;
        this.dbConfig = dbConfig;
    }

    /**
     * Goes through all the blocks to find which ones don't have vehicles. For those blocks create a
     * schedule based vehicle with associated predictions.
     */
    private void createSchedBasedPredsAsNecessary() {
        // Determine all the block IDs already in use so that can skip these
        // when doing the somewhat expensive searching for currently active
        // blocks.
        Set<String> blockIdsAlreadyAssigned = new HashSet<>();
        Collection<IpcVehicleComplete> vehicles = vehicleDataCache.getVehiclesIncludingSchedBasedOnes();
        for (IpcVehicle vehicle : vehicles) {
            String blockId = vehicle.getBlockId();
            if (blockId != null) blockIdsAlreadyAssigned.add(blockId);
        }

        // Determine which blocks are coming up or currently active
        List<Block> activeBlocks = blockInfoProvider.getCurrentlyActiveBlocks(
                null, // Get for all routes
                blockIdsAlreadyAssigned,
                PredictionConfig.beforeStartTimeMinutes.getValue() * Time.SEC_PER_MIN,
                PredictionConfig.afterStartTimeMinutes.getValue() * Time.SEC_PER_MIN);

        // For each block about to start see if no associated vehicle
        for (Block block : activeBlocks) {
            // Is there a vehicle associated with the block?
            Collection<String> vehiclesForBlock = vehicleDataCache.getVehiclesByBlockId(block.getId());
            if (vehiclesForBlock == null || vehiclesForBlock.isEmpty()) {
                // No vehicle associated with the active block so create a
                // schedule based one. First create a fake AVL report that
                // corresponds to the first stop of the block.
                String vehicleId = "block_" + block.getId() + "_schedBasedVehicle";
                long referenceTime = SystemTime.getMillis();
                long blockStartEpochTime =
                        dbConfig.getTime().getEpochTime(block.getStartTime(), referenceTime);
                Location location = block.getStartLoc();
                if (location != null) {
                    AvlReport avlReport = new AvlReport(vehicleId, blockStartEpochTime, location, "Schedule");

                    // Set the block assignment for the AVL report and indicate
                    // that it is for creating scheduled based predictions
                    avlReport.setAssignment(block.getId(), AssignmentType.BLOCK_FOR_SCHED_BASED_PREDS);

                    logger.info(
                            "Creating a schedule based vehicle for blockId={}. "
                                    + "The fake AVL report is {}. The block is {}",
                            block.getId(),
                            avlReport,
                            block.toShortString());

                    // Process that AVL report to generate predictions and such
                    avlProcessor.processAvlReport(avlReport);
                }
            }
        }
    }
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            // Do the actual work
            createSchedBasedPredsAsNecessary();
        } catch (Exception e) {
            logger.error("Error with SchedBasedPredsModule for agencyId={}", AgencyConfig.getAgencyId(), e);
        }
    }

    @Override
    public int initialExecutionDelay() {
        // No need to run at startup since haven't yet had change to assign
        // vehicles to blocks yet. So sleep a bit first, unless specified to
        // run immediately by the processImmediatelyAtStartup configuration
        // parameter.
        if (!PredictionConfig.processImmediatelyAtStartup.getValue()) {
            return PredictionConfig.timeBetweenPollingMsec.getValue();
        }

        return 0;
    }

    @Override
    public ExecutionType getExecutionType() {
        return ExecutionType.FIXED_RATE;
    }
}
