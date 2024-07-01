/* (C)2023 */
package org.transitclock.domain.structs;

import com.querydsl.jpa.impl.JPAQuery;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.CallbackException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.DiscriminatorOptions;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.classic.Lifecycle;
import org.transitclock.core.TemporalDifference;
import org.transitclock.domain.hibernate.HibernateUtils;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.utils.Geo;
import org.transitclock.utils.IntervalTimer;
import org.transitclock.utils.Time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * For persisting an Arrival or a Departure time. Should use Arrival or Departure subclasses.
 *
 * <p>Implements Lifecycle so that can have the onLoad() callback be called when reading in data so
 * that can intern() member strings. In order to do this the String members could not be declared as
 * final since they are updated after the constructor is called. By interning the member strings
 * less than half (about 40%) of the RAM is used. This is very important when reading in large
 * batches of ArrivalDeparture objects!
 *
 * @author SkiBu Smith
 */
@Entity
@Getter @Setter
@RequiredArgsConstructor
@DynamicUpdate
@Table(name = "arrivals_departures",
    indexes = {
            @Index(name = "ArrivalsDeparturesTimeIndex", columnList = "time"),
            @Index(name = "ArrivalsDeparturesRouteTimeIndex", columnList = "route_short_name, time")
    }
)
@Slf4j
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorOptions(force = true)
public abstract class ArrivalDeparture implements Lifecycle, Serializable {

    @NonNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", insertable = false, updatable = false)
    private ArrivalsOrDepartures type;

    @Id
    @Column(name = "vehicle_id", length = 60)
    private String vehicleId;

    // Originally did not use msec precision (datetime(3)) specification
    // because arrival/departure times are only estimates and having such
    // precision is not generally appropriate. But found that then some
    // arrival and departures for a stop would have the same time and when
    // one would query for the arrivals/departures and order by time one
    // could get a departure before an arrival. To avoid this kind of
    // incorrect ordering using the additional precision. And this way
    // don't have to add an entire second to a departure time to make
    // sure that it is after the arrival. Adding a second is an
    // exaggeration because it implies the vehicle was stopped for a second
    // when most likely it zoomed by the stop. It looks better to add
    // only a msec to make the departure after the arrival.
    @Id
    @Column(name = "time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date time;

    @Id
    @Column(name = "stop_id", length = 60)
    private String stopId;

    // From the GTFS stop_times.txt file for the trip. The gtfsStopSeq can
    // be different from stopPathIndex. The stopIndex is included here so that
    // it is easy to find the corresponding stop in the stop_times.txt file.
    // It needs to be part of the @Id because can have loops for a route
    // such that a stop is served twice on a trip. Otherwise would get a
    // constraint violation.
    @Id
    @Column(name = "gtfs_stop_seq")
    private final int gtfsStopSeq;

    @Id
    @Column(name = "is_arrival")
    private final boolean isArrival;

    @Id
    @Column(name = "trip_id", length = 60)
    private String tripId;

    // The revision of the configuration data that was being used
    @Column(name = "config_rev")
    final int configRev;

    // So can match the ArrivalDeparture time to the AvlReport that
    // generated it by using vehicleId and avlTime.
    @Column(name = "avl_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date avlTime;

    // The schedule time will only be set if the schedule info was available
    // from the GTFS data and it is the proper type of arrival or departure
    // stop (there is an arrival schedule time and this is the last stop for
    // a trip and and this is an arrival time OR there is a departure schedule
    // time and this is not the last stop for a trip and this is a departure
    // time. Otherwise will be null.
    @Column(name = "scheduled_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date scheduledTime;

    @Column(name = "block_id", length = 60)
    private String blockId;

    @Column(name = "route_id", length = 60)
    private String routeId;

    // routeShortName is included because for some agencies the
    // route_id changes when there are schedule updates. But the
    // routeShortName is more likely to stay consistent. Therefore
    // it is better for when querying for arrival/departure data
    // over a timespan.
    @Column(name = "route_short_name", length = 60)
    private String routeShortName;

    @Column(name = "service_id", length = 60)
    private String serviceId;

    @Column(name = "direction_id", length = 60)
    private String directionId;

    // The index of which trip this is within the block.
    @Column(name = "trip_index")
    private final int tripIndex;

    /* this is required for frequenecy based services */
    @Column(name = "freq_start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private final Date freqStartTime;

    // The index of which stop path this is within the trip.
    // Different from the GTFS gtfsStopSeq. The stopPathIndex starts
    // at 0 and increments by one for every stop. The GTFS gtfsStopSeq
    // on the other hand doesn't need to be sequential.
    @Column(name = "stop_path_index")
    private final int stopPathIndex;

    // The order of the stop for the direction of the route. This can
    // be useful for displaying data in proper stop order. The member
    // stopPathIndex is for the current trip, but since a route's
    // direction can have multiple trip patterns the stopPathIndex
    // is not sufficient for properly ordering data for a route/direction.
    // Declared an Integer instead of an int because might not always
    // be set.
    @Column(name = "stop_order")
    private final Integer stopOrder;

    // Sometimes want to look at travel times using arrival/departure times.
    // This would be complicated if had to get the path length by using
    // tripIndex to determine trip to determine trip pattern to determine
    // StopPath to determine length. So simply storing the stop path
    // length along with arrivals/departures so that it is easy to obtain
    // for post-processing.
    @Column(name = "stop_path_length")
    private final float stopPathLength;

    // So can easily create copy constructor withUpdatedTime()
    @Transient
    private final Block block;

    // Needed because some methods need to know if dealing with arrivals or
    // departures.

    public enum ArrivalsOrDepartures {
        ARRIVALS,
        DEPARTURES
    }

    protected ArrivalDeparture(
            int configRev,
            String vehicleId,
            Date time,
            Date avlTime,
            Block block,
            int tripIndex,
            int stopPathIndex,
            boolean isArrival,
            Date freqStartTime,
            DbConfig dbConfig) {
        this.vehicleId = vehicleId;
        this.time = time;
        this.avlTime = avlTime;
        this.block = block;
        this.tripIndex = tripIndex;
        this.stopPathIndex = stopPathIndex;
        this.isArrival = isArrival;
        this.configRev = configRev;
        this.freqStartTime = freqStartTime;

        // Some useful convenience variables

        if (block != null) {
            Trip trip = block.getTrip(tripIndex);
            StopPath stopPath = trip.getStopPath(stopPathIndex);
            String stopId = stopPath.getStopId();
            // Determine and store stop order
            this.stopOrder = trip.getRoute(dbConfig).getStopOrder(dbConfig, trip.getDirectionId(), stopId, stopPathIndex);

            // Determine the schedule time, which is a bit complicated.
            // Of course, only do this for schedule based assignments.
            // The schedule time will only be set if the schedule info was available
            // from the GTFS data and it is the proper type of arrival or departure
            // stop (there is an arrival schedule time and this is the last stop for
            // a trip and and this is an arrival time OR there is a departure schedule
            // time and this is not the last stop for a trip and this is a departure
            // time.
            Date scheduledEpochTime = null;
            if (!trip.isNoSchedule()) {
                ScheduleTime scheduleTime = trip.getScheduleTime(stopPathIndex);
                if (stopPath.isLastStopInTrip() && scheduleTime.getArrivalTime() != null && isArrival) {
                    long epochTime = dbConfig.getTime().getEpochTime(scheduleTime.getArrivalTime(), time);
                    scheduledEpochTime = new Date(epochTime);
                } else if (!stopPath.isLastStopInTrip() && scheduleTime.getDepartureTime() != null && !isArrival) {
                    long epochTime = dbConfig.getTime().getEpochTime(scheduleTime.getDepartureTime(), time);
                    scheduledEpochTime = new Date(epochTime);
                }
            }
            this.scheduledTime = scheduledEpochTime;

            this.blockId = block.getId();
            this.tripId = trip.getId();
            this.directionId = trip.getDirectionId();
            this.stopId = stopId;
            this.gtfsStopSeq = stopPath.getGtfsStopSeq();
            this.stopPathLength = (float) stopPath.getLength();
            this.routeId = trip.getRouteId();
            this.routeShortName = trip.getRouteShortName();
            this.serviceId = block.getServiceId();
        } else {
            /* have to do this as they are final */
            this.stopPathLength = 0;
            this.gtfsStopSeq = 0;
            this.scheduledTime = null;
            this.tripId = "";
            this.stopId = "";
            this.serviceId = "";
            this.stopOrder = 0;
        }
    }

    protected ArrivalDeparture() {
        this.vehicleId = null;
        this.time = null;
        this.avlTime = null;
        this.block = null;
        this.directionId = null;
        this.tripIndex = -1;
        this.stopPathIndex = -1;
        this.stopOrder = null;
        this.isArrival = false;
        this.configRev = -1;
        this.scheduledTime = null;
        this.blockId = null;
        this.tripId = null;
        this.stopId = null;
        this.gtfsStopSeq = -1;
        this.stopPathLength = Float.NaN;
        this.routeId = null;
        this.routeShortName = null;
        this.serviceId = null;
        this.freqStartTime = null;
    }

    /**
     * Callback due to implementing Lifecycle interface. Used to compact string members by interning
     * them.
     */
    @Override
    public void onLoad(Session s, Object id) throws CallbackException {
        if (vehicleId != null) vehicleId = vehicleId.intern();
        if (stopId != null) stopId = stopId.intern();
        if (tripId != null) tripId = tripId.intern();
        if (blockId != null) blockId = blockId.intern();
        if (routeId != null) routeId = routeId.intern();
        if (routeShortName != null) routeShortName = routeShortName.intern();
        if (serviceId != null) serviceId = serviceId.intern();
        if (directionId != null) directionId = directionId.intern();
    }

    @Override
    public String toString() {
        return (isArrival ? "Arrival  " : "Departure")
                + " ["
                + "vehicleId="
                + vehicleId
                // + ", isArrival=" + isArrival
                + ", time="
                + Time.dateTimeStrMsec(time)
                + ", route="
                + routeId
                + ", rteName="
                + routeShortName
                + ", directionId="
                + directionId
                + ", stop="
                + stopId
                + ", gtfsStopSeq="
                + gtfsStopSeq
                + ", stopIdx="
                + stopPathIndex
                + ", freqStartTime="
                + freqStartTime
                + ", stopOrder="
                + stopOrder
                + ", avlTime="
                + Time.timeStrMsec(avlTime)
                + ", trip="
                + tripId
                + ", tripIdx="
                + tripIndex
                + ", block="
                + blockId
                + ", srv="
                + serviceId
                + ", cfg="
                + configRev
                + ", pathLnth="
                + Geo.distanceFormat(stopPathLength)
                + (scheduledTime != null ? ", schedTime=" + Time.timeStr(scheduledTime) : "")
                + (scheduledTime != null
                        ? ", schedAdh=" + new TemporalDifference(scheduledTime.getTime() - time.getTime())
                        : "")
                + "]";
    }

    /**
     * For querying large amount of data. With a Hibernate Iterator not all the data is read in at
     * once. This means that can iterate over a large dataset without running out of memory. But
     * this can be slow because when using iterate() an initial query is done to get all of Id
     * column data and then a separate query is done when iterating over each row. Doing an
     * individual query per row is of course quite time consuming. Better to use
     * getArrivalsDeparturesFromDb() with a fairly large batch size of ~50000.
     *
     * <p>Note that the session needs to be closed externally once done with the Iterator.
     */
    public static Iterator<ArrivalDeparture> getArrivalsDeparturesDbIterator(
            Session session, Date beginTime, Date endTime) throws HibernateException {
        // Create the query. Table name is case-sensitive and needs to be the
        // class name instead of the name of the db table.
        String hql = "FROM ArrivalDeparture WHERE time >= :beginDate AND time < :endDate";
        var query = session.createQuery(hql, ArrivalDeparture.class);

        // Set the parameters
        query.setParameter("beginDate", beginTime);
        query.setParameter("endDate", endTime);

        @SuppressWarnings("unchecked")
        Iterator<ArrivalDeparture> iterator = query.stream().iterator();
        return iterator;
    }

    /**
     * Read in arrivals and departures for a vehicle, over a time range.
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(Date beginTime, Date endTime, String vehicleId) {
        // Call in standard getArrivalsDeparturesFromDb() but pass in
        // sql clause
        return getArrivalsDeparturesFromDb(
                beginTime,
                endTime,
                "AND vehicleId='" + vehicleId + "'",
                0,
                0, // Don't use batching
                null); // Read both arrivals and departures
    }

    /**
     * Reads in arrivals and departures for a particular trip and service. Create session and uses
     * it
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(
            Date beginTime, Date endTime, String tripId, String serviceId) {
        Session session = HibernateUtils.getSession();

        return ArrivalDeparture.getArrivalsDeparturesFromDb(session, beginTime, endTime, tripId, serviceId);
    }

    /**
     * Reads in arrivals and departures for a particular trip and service. Uses session provided
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(
            Session session, Date beginTime, Date endTime, String tripId, String serviceId) {
        JPAQuery<ArrivalDeparture> query = new JPAQuery<>(session);
        var qentity = QArrivalDeparture.arrivalDeparture;

        query = query.from(qentity)
            .where(qentity.tripId.eq(tripId))
            .where(qentity.time.gt(beginTime))
            .where(qentity.time.lt(endTime));

        if (serviceId != null) {
            query.where(qentity.serviceId.eq(serviceId));
        }

        return query.fetch();
    }

    /**
     * Reads in arrivals and departures for a particular stopPathIndex of a trip between two dates.
     * Uses session provided
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(
            Session session, Date beginTime, Date endTime, String tripId, Integer stopPathIndex) {
        JPAQuery<ArrivalDeparture> query = new JPAQuery<>(session);
        var qentity = QArrivalDeparture.arrivalDeparture;

        query.select(qentity);
        if (tripId != null) {
            query.where(qentity.tripId.eq(tripId));

            if (stopPathIndex != null) {
                query.where(qentity.stopPathIndex.eq(stopPathIndex));
            }
        }

        query.where(qentity.time.gt(beginTime))
                .where(qentity.time.lt(endTime));

        return query.fetch();
    }

    /**
     * Reads the arrivals/departures for the timespan specified. All of the data is read in at once
     * so could present memory issue if reading in a very large amount of data. For that case
     * probably best to instead use getArrivalsDeparturesDb() where one specifies the firstResult
     * and maxResult parameters.
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(String projectId, Date beginTime, Date endTime) {
        IntervalTimer timer = new IntervalTimer();

        // Get the database session. This is supposed to be pretty lightweight
        Session session = HibernateUtils.getSession(projectId);

        // Create the query. Table name is case-sensitive and needs to be the
        // class name instead of the name of the db table.
        try (session) {
            var query = session
                    .createQuery("FROM ArrivalDeparture WHERE time >= :beginDate AND time < :endDate", ArrivalDeparture.class)
                    .setParameter("beginDate", beginTime)
                    .setParameter("endDate", endTime);
            List<ArrivalDeparture> arrivalsDeparatures = query.list();
            logger.debug("Getting arrival/departures from database took {} msec", timer.elapsedMsec());
            return arrivalsDeparatures;
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Allows batch retrieval of data. This is likely the best way to read in large amounts of data.
     * Using getArrivalsDeparturesDbIterator() reads in only data as needed so good with respect to
     * memory usage but it does a separate query for each row. Reading in list of all data is quick
     * but can cause memory problems if reading in a very large amount of data. This method is a
     * good compromise because it only reads in a batch of data at a time so is not as memory
     * intensive yet it is quite fast. With a batch size of 50k found it to run in under 1/4 the
     * time as with the iterator method.
     *
     * @param dbName Name of the database to retrieve data from. If set to null then will use db
     *     name configured by Java property transitclock.db.dbName
     * @param beginTime
     * @param endTime
     * @param sqlClause The clause is added to the SQL for retrieving the arrival/departures. Useful
     *     for ordering the results. Can be null.
     * @param firstResult For when reading in batch of data at a time.
     * @param maxResults For when reading in batch of data at a time. If set to 0 then will read in
     *     all data at once.
     * @param arrivalOrDeparture Enumeration specifying whether to read in just arrivals or just
     *     departures. Set to null to read in both.
     * @return List<ArrivalDeparture> or null if there is an exception
     */
    public static List<ArrivalDeparture> getArrivalsDeparturesFromDb(
            Date beginTime,
            Date endTime,
            String sqlClause,
            final Integer firstResult,
            final Integer maxResults,
            ArrivalsOrDepartures arrivalOrDeparture) {
        // Get the database session. This is supposed to be pretty light weight
        try (Session session = HibernateUtils.getSession()) {
            String hql = "FROM ArrivalDeparture WHERE time between :beginDate AND :endDate";
            if (arrivalOrDeparture != null) {
                if (arrivalOrDeparture == ArrivalsOrDepartures.ARRIVALS) {
                    hql += " AND isArrival = true";
                } else {
                    hql += " AND isArrival = false";
                }
            }
            if (sqlClause != null) {
                hql += " " + sqlClause;
            }
            var query = session.createQuery(hql, ArrivalDeparture.class)
                    .setParameter("beginDate", beginTime)
                    .setParameter("endDate", endTime);

            // Only get a batch of data at a time if maxResults specified
            if (firstResult != null) {
                query.setFirstResult(firstResult);
            }
            if (maxResults != null && maxResults > 0) {
                query.setMaxResults(maxResults);
            }

            return query.list();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }

        return new ArrayList<>();
    }

    public static long getArrivalsDeparturesCountFromDb(
            String dbName, Date beginTime, Date endTime, ArrivalsOrDepartures arrivalOrDeparture) {

        // Get the database session. This is supposed to be pretty lightweight
        try (Session session = HibernateUtils.getSession(dbName)) {
            String hql = "select count(*) FROM ArrivalDeparture WHERE time >= :beginDate AND time < :endDate";
            if (arrivalOrDeparture != null) {
                if (arrivalOrDeparture == ArrivalsOrDepartures.ARRIVALS) {
                    hql += " AND isArrival = true";
                } else {
                    hql += " AND isArrival = false";
                }
            }

            var query = session.createQuery(hql, Long.class)
                    .setParameter("beginDate", beginTime)
                    .setParameter("endDate", endTime);

            return query.uniqueResult();
        } catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }
        return 0L;
    }

    public Date getDate() {
        return time;
    }

    public long getTime() {
        return time.getTime();
    }

    public boolean isArrival() {
        return isArrival;
    }

    /**
     * Can be more clear than using !isArrival()
     *
     * @return
     */
    public boolean isDeparture() {
        return !isArrival;
    }

    /**
     * The schedule time will only be set if the schedule info was available from the GTFS data and
     * it is the proper type of arrival or departure stop (there is an arrival schedule time and
     * this is the last stop for a trip and and this is an arrival time OR there is a departure
     * schedule time and this is not the last stop for a trip and this is a departure time.
     * Otherwise will be null.
     *
     * @return
     */
    public Date getScheduledDate() {
        return scheduledTime;
    }

    /**
     * Same as getScheduledDate() but returns long epoch time.
     *
     * @return
     */
    public long getScheduledTime() {
        return scheduledTime.getTime();
    }

    /**
     * Returns the schedule adherence for the stop if there was a schedule time. Otherwise returns
     * null.
     *
     * @return
     */
    public TemporalDifference getScheduleAdherence() {
        // If there is no schedule time for this stop then there
        // is no schedule adherence information.
        if (scheduledTime == null) {
            return null;
        }

        // Return the schedule adherence
        return new TemporalDifference(scheduledTime.getTime() - time.getTime());
    }

    /**
     * @return the gtfsStopSequence associated with the arrival/departure
     */
    public int getGtfsStopSequence() {
        return gtfsStopSeq;
    }

    /**
     * Because using a composite Id Hibernate wants this member.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArrivalDeparture other = (ArrivalDeparture) obj;
        if (avlTime == null) {
            if (other.avlTime != null)
                return false;
        } else if (!avlTime.equals(other.avlTime))
            return false;
        if (block == null) {
            if (other.block != null)
                return false;
        } else if (!block.equals(other.block))
            return false;
        if (blockId == null) {
            if (other.blockId != null)
                return false;
        } else if (!blockId.equals(other.blockId))
            return false;
        if (configRev != other.configRev)
            return false;
        if (directionId == null) {
            if (other.directionId != null)
                return false;
        } else if (!directionId.equals(other.directionId))
            return false;
        if (gtfsStopSeq != other.gtfsStopSeq)
            return false;
        if (isArrival != other.isArrival)
            return false;
        if (routeId == null) {
            if (other.routeId != null)
                return false;
        } else if (!routeId.equals(other.routeId))
            return false;
        if (routeShortName == null) {
            if (other.routeShortName != null)
                return false;
        } else if (!routeShortName.equals(other.routeShortName))
            return false;
        if (scheduledTime == null) {
            if (other.scheduledTime != null)
                return false;
        } else if (!scheduledTime.equals(other.scheduledTime))
            return false;
        if (serviceId == null) {
            if (other.serviceId != null)
                return false;
        } else if (!serviceId.equals(other.serviceId))
            return false;
        if (stopId == null) {
            if (other.stopId != null)
                return false;
        } else if (!stopId.equals(other.stopId))
            return false;
        if (stopOrder == null) {
            if (other.stopOrder != null)
                return false;
        } else if (!stopOrder.equals(other.stopOrder))
            return false;
        if (stopPathIndex != other.stopPathIndex)
            return false;
        if (Float.floatToIntBits(stopPathLength) != Float
                .floatToIntBits(other.stopPathLength))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        if (tripId == null) {
            if (other.tripId != null)
                return false;
        } else if (!tripId.equals(other.tripId))
            return false;
        if (tripIndex != other.tripIndex)
            return false;
        if (vehicleId == null) {
            if (other.vehicleId != null)
                return false;
        } else if (!vehicleId.equals(other.vehicleId))
            return false;
        return true;
    }

    /**
     * Because using a composite Id Hibernate wants this member.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((avlTime == null) ? 0 : avlTime.hashCode());
        result = prime * result + ((block == null) ? 0 : block.hashCode());
        result = prime * result + ((blockId == null) ? 0 : blockId.hashCode());
        result = prime * result + configRev;
        result = prime * result + ((directionId == null) ? 0 : directionId.hashCode());
        result = prime * result + gtfsStopSeq;
        result = prime * result + (isArrival ? 1231 : 1237);
        result = prime * result + ((routeId == null) ? 0 : routeId.hashCode());
        result = prime * result + ((routeShortName == null) ? 0 : routeShortName.hashCode());
        result = prime * result + ((scheduledTime == null) ? 0 : scheduledTime.hashCode());
        result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
        result = prime * result + ((stopId == null) ? 0 : stopId.hashCode());
        result = prime * result + ((stopOrder == null) ? 0 : stopOrder.hashCode());
        result = prime * result + stopPathIndex;
        result = prime * result + Float.floatToIntBits(stopPathLength);
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + ((tripId == null) ? 0 : tripId.hashCode());
        result = prime * result + tripIndex;
        result = prime * result + ((vehicleId == null) ? 0 : vehicleId.hashCode());
        return result;
    }
}