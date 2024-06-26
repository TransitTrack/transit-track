/* (C)2023 */
package org.transitclock.domain.structs;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.CallbackException;
import org.hibernate.Session;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.classic.Lifecycle;

/**
 * A database object for persisting information on how accurate a prediction was compared to the
 * actual measured arrival/departure time for the vehicle.
 *
 * <p>Serializable since Hibernate requires such.
 *
 * <p>Implements Lifecycle so that can have the onLoad() callback be called when reading in data so
 * that can intern() member strings. In order to do this the String members could not be declared as
 * final since they are updated after the constructor is called.
 *
 * @author SkiBu Smith
 */
@Entity
@DynamicUpdate
@Getter @Setter @ToString
@Table(
    name = "prediction_accuracy",
    indexes = {
        @Index(name = "PredictionAccuracyTimeIndex", columnList = "arrival_departure_time")
    }
)
public class PredictionAccuracy implements Lifecycle, Serializable {

    // Need an ID but using regular columns doesn't really make
    // sense. So use an auto generated one. Not final since
    // autogenerated and therefore not set in constructor.
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    // Not declared final since using intern() when reading from db
    @Column(name = "route_id", length = 60)
    private String routeId;

    // routeShortName is included because for some agencies the
    // route_id changes when there are schedule updates. But the
    // routeShortName is more likely to stay consistent. Therefore
    // it is better for when querying for arrival/departure data
    // over a time span.
    // Not declared final since using intern() when reading from db
    @Column(name = "route_short_name", length = 60)
    private String routeShortName;

    // Not declared final since using intern() when reading from db
    @Column(name = "direction_id", length = 60)
    private String directionId;

    // Not declared final since using intern() when reading from db
    @Column(name = "stop_id", length = 60)
    private String stopId;

    // So can see which trip predictions for so can easily determine
    // what the travel times are and see if they appear to be correct.
    // Not declared final since using intern() when reading from db
    @Column(name = "trip_id", length = 60)
    private String tripId;

    // The actual arrival time that corresponds to the prediction time
    @Column(name = "arrival_departure_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date arrivalDepartureTime;

    // The predicted time the vehicle was expected to arrive/depart the stop
    @Column(name = "predicted_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date predictedTime;

    // The time the prediction was read. This allows us to determine
    // how far out into the future the prediction is for.
    @Column(name = "prediction_read_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date predictionReadTime;

    // Positive means vehicle arrived at stop later then predicted for and
    // negative value means vehicle arrived earlier.
    @Column(name = "prediction_accuracy_msecs")
    private final int predictionAccuracyMsecs;

    @Column(name = "prediction_source", length = 60)
    private String predictionSource;

    /* TODO */
    // @Column(length=60)
    @Transient
    private String predictionAlgorithm;

    @Column(name = "vehicle_id", length = 60)
    private String vehicleId;

    @Column(name = "affected_by_wait_stop")
    private final Boolean affectedByWaitStop;

    public PredictionAccuracy(
            String routeId,
            String routeShortName,
            String directionId,
            String stopId,
            String tripId,
            Date arrivalDepartureTime,
            Date predictedTime,
            Date predictionReadTime,
            String predictionSource,
            String predictionAlgorithm,
            String vehicleId,
            Boolean affectedByWaitStop) {
        this.routeId = routeId;
        this.routeShortName = routeShortName;
        this.directionId = directionId;
        this.stopId = stopId;
        this.tripId = tripId;
        this.arrivalDepartureTime = arrivalDepartureTime;
        this.predictedTime = predictedTime;
        this.predictionReadTime = predictionReadTime;
        this.predictionAccuracyMsecs =
                arrivalDepartureTime != null ? (int) (arrivalDepartureTime.getTime() - predictedTime.getTime()) : 0;
        this.predictionSource = predictionSource;
        this.vehicleId = vehicleId;
        this.affectedByWaitStop = affectedByWaitStop;
        this.predictionAlgorithm = predictionAlgorithm;
    }

    protected PredictionAccuracy() {
        this.routeId = null;
        this.routeShortName = null;
        this.directionId = null;
        this.stopId = null;
        this.tripId = null;
        this.arrivalDepartureTime = null;
        this.predictedTime = null;
        this.predictionReadTime = null;
        this.predictionAccuracyMsecs = -1;
        this.predictionSource = null;
        this.vehicleId = null;
        this.affectedByWaitStop = null;
        this.predictionAlgorithm = null;
    }

    public int getPredictionLengthMsecs() {
        return (int) (predictedTime.getTime() - predictionReadTime.getTime());
    }

    /**
     * True if the prediction is based on scheduled departure time, false if not. Null if feed of
     * predictions doesn't provide that information.
     *
     * @return
     */
    public Boolean isAffectedByWaitStop() {
        return affectedByWaitStop;
    }

    /** Callback due to implementing Lifecycle interface. Used to compact string members by them. */
    @Override
    public void onLoad(Session s, Object id) throws CallbackException {
        if (routeId != null) routeId = routeId.intern();
        if (routeShortName != null) routeShortName = routeShortName.intern();
        if (directionId != null) directionId = directionId.intern();
        if (stopId != null) stopId = stopId.intern();
        if (tripId != null) tripId = tripId.intern();
        if (predictionSource != null) predictionSource = predictionSource.intern();
        if (predictionAlgorithm != null) predictionAlgorithm = predictionAlgorithm.intern();
        if (vehicleId != null) vehicleId = vehicleId.intern();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PredictionAccuracy that)) return false;
        return id == that.id && predictionAccuracyMsecs == that.predictionAccuracyMsecs
            && Objects.equals(routeId, that.routeId)
            && Objects.equals(routeShortName, that.routeShortName)
            && Objects.equals(directionId, that.directionId)
            && Objects.equals(stopId, that.stopId)
            && Objects.equals(tripId, that.tripId)
            && Objects.equals(arrivalDepartureTime, that.arrivalDepartureTime)
            && Objects.equals(predictedTime, that.predictedTime)
            && Objects.equals(predictionReadTime, that.predictionReadTime)
            && Objects.equals(predictionSource, that.predictionSource)
//            && Objects.equals(predictionAlgorithm, that.predictionAlgorithm)
            && Objects.equals(vehicleId, that.vehicleId)
            && Objects.equals(affectedByWaitStop, that.affectedByWaitStop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, routeId, routeShortName, directionId, stopId, tripId, arrivalDepartureTime, predictedTime, predictionReadTime, predictionAccuracyMsecs, predictionSource,
//            predictionAlgorithm,
            vehicleId, affectedByWaitStop);
    }
}
