/* (C)2023 */
package org.transitclock.api.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.config.BooleanConfigValue;
import org.transitclock.config.IntegerConfigValue;

import java.util.*;

public class ScheduleAdherenceController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleAdherenceController.class);
    // TODO: Combine routeScheduleAdherence and stopScheduleAdherence
    // - Make this a REST endpoint
    // problem - negative schedule adherence means we're late

    private static IntegerConfigValue scheduleEarlySeconds =
            new IntegerConfigValue("transitclock.web.scheduleEarlyMinutes", -120, "Schedule Adherence early limit");

    public static int getScheduleEarlySeconds() {
        return scheduleEarlySeconds.getValue();
    }

    private static IntegerConfigValue scheduleLateSeconds =
            new IntegerConfigValue("transitclock.web.scheduleLateMinutes", 420, "Schedule Adherence late limit");

    public static int getScheduleLateSeconds() {
        return scheduleLateSeconds.getValue();
    }

    private static BooleanConfigValue usePredictionLimits = new BooleanConfigValue(
            "transitme.web.userPredictionLimits",
            Boolean.FALSE,
            "use the allowable early/late report params or use configured schedule limits");

//    private static final String ADHERENCE_SQL = "(time - scheduledTime) AS scheduleAdherence";
//    private static final Projection ADHERENCE_PROJECTION = Projections.sqlProjection(
//            ADHERENCE_SQL, new String[] {"scheduleAdherence"}, new Type[] {DoubleType.INSTANCE});
//    private static final Projection AVG_ADHERENCE_PROJECTION = Projections.sqlProjection(
//            "avg" + ADHERENCE_SQL, new String[] {"scheduleAdherence"}, new Type[] {DoubleType.INSTANCE});

    public static List<Object> stopScheduleAdherence(
            Date startDate,
            int numDays,
            String startTime,
            String endTime,
            List<String> stopIds,
            boolean byStop,
            String datatype) {

        return groupScheduleAdherence(startDate, numDays, startTime, endTime, "stopId", stopIds, byStop, datatype);
    }

    public static List<Object> routeScheduleAdherence(
            Date startDate,
            int numDays,
            String startTime,
            String endTime,
            List<String> routeIds,
            boolean byRoute,
            String datatype) {

        return groupScheduleAdherence(startDate, numDays, startTime, endTime, "routeId", routeIds, byRoute, datatype);
    }

    public static List<Integer> routeScheduleAdherenceSummary(
            Date startDate,
            int numDays,
            String startTime,
            String endTime,
            Double earlyLimitParam,
            Double lateLimitParam,
            List<String> routeIds) {

        int count = 0;
        int early = 0;
        int late = 0;
        int ontime = 0;
        Double earlyLimit =
                (usePredictionLimits.getValue() ? earlyLimitParam : (double) scheduleEarlySeconds.getValue());
        Double lateLimit = (usePredictionLimits.getValue() ? lateLimitParam : (double) scheduleLateSeconds.getValue());
        List<Object> results = routeScheduleAdherence(startDate, numDays, startTime, endTime, routeIds, false, null);

        for (Object o : results) {
            count++;
            HashMap hm = (HashMap) o;
            Double d = (Double) hm.get("scheduleAdherence");
            if (d > lateLimit) {
                late++;
            } else if (d < earlyLimit) {
                early++;
            } else {
                ontime++;
            }
        }
        logger.info(
                "query complete -- earlyLimit={}, lateLimit={}, early={}, ontime={}, late={}," + " count={}",
                earlyLimit,
                lateLimit,
                early,
                ontime,
                late,
                count);
        double earlyPercent = (1.0 - (double) (count - early) / count) * 100;
        double onTimePercent = (1.0 - (double) (count - ontime) / count) * 100;
        double latePercent = (1.0 - (double) (count - late) / count) * 100;
        logger.info(
                "count={} earlyPercent={} onTimePercent={} latePercent={}",
                count,
                earlyPercent,
                onTimePercent,
                latePercent);
        Integer[] summary = new Integer[] {count, (int) earlyPercent, (int) onTimePercent, (int) latePercent};
        return Arrays.asList(summary);
    }

    private static List<Object> groupScheduleAdherence(
            Date startDate,
            int numDays,
            String startTime,
            String endTime,
            String groupName,
            List<String> idsOrEmpty,
            boolean byGroup,
            String datatype) {
/*

        var qentity = QArrivalDeparture.arrivalDeparture;
        Session session = HibernateUtils.getSession();
        JPAQuery<ArrivalDeparture> query = new JPAQuery<>(session);

        // filter ids which may be empty.
        List<String> ids = new ArrayList<>();
        if (idsOrEmpty != null)
            for (String id : idsOrEmpty)
                if (!StringUtils.isBlank(id)) {
                    ids.add(id);
                }

        Date endDate = new Date(startDate.getTime() + (numDays * Time.MS_PER_DAY));

        ProjectionList proj = Projections.projectionList();

        if (byGroup)
            proj.add(Projections.groupProperty(groupName), groupName)
                .add(Projections.rowCount(), "count");
        else
            proj.add(Projections.property("routeId"), "routeId")
                    .add(Projections.property("stopId"), "stopId")
                    .add(Projections.property("tripId"), "tripId");

        proj.add(byGroup ? AVG_ADHERENCE_PROJECTION : ADHERENCE_PROJECTION, "scheduleAdherence");

        DetachedCriteria criteria = DetachedCriteria.forClass(ArrivalDeparture.class)
                .add(Restrictions.between("time", startDate, endDate))
                .add(Restrictions.isNotNull("scheduledTime"));

        if ("arrival".equals(datatype)) criteria.add(Restrictions.eq("isArrival", true));
        else if ("departure".equals(datatype)) criteria.add(Restrictions.eq("isArrival", false));

        String sql = "time({alias}.time) between ? and ?";
        String[] values = {startTime, endTime};
        Type[] types = {StringType.INSTANCE, StringType.INSTANCE};
        criteria.add(Restrictions.sqlRestriction(sql, values, types));

        criteria.setProjection(proj).setResultTransformer(DetachedCriteria.ALIAS_TO_ENTITY_MAP);

        if (ids != null && ids.size() > 0) criteria.add(Restrictions.in(groupName, ids));
*/
        return Collections.emptyList();
    }

    private static Date endOfDay(Date endDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(endDate);
        c.set(Calendar.HOUR, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        return c.getTime();
    }
//
//    private static List<Object> dbify(DetachedCriteria criteria) {
//        Session session = HibernateUtils.getSession();
//        try {
//            return criteria.getExecutableCriteria(session).list();
//        } finally {
//            session.close();
//        }
//    }
}
