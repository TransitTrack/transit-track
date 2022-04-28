<%-- Provides schedule adherence data in JSON format. Provides for
     the specified route the number arrivals/departures that
     are early, number late, number on time, and number total for each
     direction for each stop. 
     Request parameters are:
       a - agency ID
       r - route ID or route short name. 
       dateRange - in format "xx/xx/xx to yy/yy/yy"
       beginDate - date to begin query. For if dateRange not used.
       numDays - number of days can do query. Limited to 31 days. For if dateRange not used.
       beginTime - for optionally specifying time of day for query for each day
       endTime - for optionally specifying time of day for query for each day
       allowableEarlyMinutes - how early vehicle can be and still be OK.  Decimal format OK. 
       allowableLateMinutes - how early vehicle can be and still be OK. Decimal format OK.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="org.transitclock.reports.GenericJsonQuery" %>
<%@ page import="org.transitclock.reports.SqlUtils" %>
<%
try {
System.out.println("test");
String allowableEarlyStr = request.getParameter("allowableEarly");
if (allowableEarlyStr == null || allowableEarlyStr.isEmpty())
	allowableEarlyStr = "1.0";
String allowableEarlyMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableEarlyStr) + " seconds'";

String allowableLateStr = request.getParameter("allowableLate");
if (allowableLateStr == null || allowableLateStr.isEmpty())
	allowableLateStr = "3.0";
String allowableLateMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableLateStr) + " seconds'";


String sql =
	"WITH trips_early_query_with_time AS ( SELECT tripid AS trips_early, "
	+ "	 round(EXTRACT(EPOCH FROM (ad.scheduledTime - ad.time))::numeric / 60, 2) AS difference_in_seconds, \n"
	//+ "	 abs(((ad.time / 1000) - (ad.scheduledTime / 1000))) AS difference_in_seconds,  \n"
	+ "	 s.id AS stop_id, \n"
	+ "	 ad.stopOrder AS stop_order \n"
	+ " 	FROM ArrivalsDepartures ad, Stops s  \n"
			+ "WHERE "
		    // To get stop name
		    + " ad.configRev = s.configRev \n"
		    + " AND ad.stopId = s.id \n"
		    // Only need arrivals/departures that have a schedule time
		    + " AND ad.scheduledTime IS NOT NULL \n"
		    // Specifies which routes to provide data for
		    + SqlUtils.routeClause(request, "ad") + "\n"
		    + SqlUtils.timeRangeClause(request, "ad.time", 7) + "\n"
		    + " AND scheduledTime-time > " + allowableEarlyMinutesStr + " \n"
	+ "	 ORDER BY directionid, ad.stopOrder, s.name \n"
	+ "), \n"
	
	+ "trips_late_query_with_time AS ( SELECT tripid AS trips_late,  "
	+ "	 round(EXTRACT(EPOCH FROM (ad.time - ad.scheduledTime))::numeric / 60, 2) AS difference_in_seconds,  \n"
	//+ "	 ((ad.time / 1000) - (ad.scheduledTime / 1000)) AS difference_in_seconds,  \n"
	+ "	 s.id AS stop_id, \n"
	+ "	 ad.stopOrder AS stop_order \n"
	+ "	FROM ArrivalsDepartures ad, Stops s  \n"
			+ "WHERE "
		    // To get stop name
		    + " ad.configRev = s.configRev \n"
		    + " AND ad.stopId = s.id \n"
		    // Only need arrivals/departures that have a schedule time
		    + " AND ad.scheduledTime IS NOT NULL \n"
		    // Specifies which routes to provide data for
		    + SqlUtils.routeClause(request, "ad") + "\n"
		    + SqlUtils.timeRangeClause(request, "ad.time", 7) + "\n"
		    + " AND time-scheduledTime > " + allowableLateMinutesStr + " \n"
	+ "	 ORDER BY directionid, ad.stopOrder, s.name \n"
	+ "), \n"
	+ "trips_late_query_v2 AS ( "
	+ "		SELECT array_to_string(array_agg(trips_late::text || ' (' || difference_in_seconds::text || ')' order by trips_late::text), '; ') AS trips_late,   \n"
	+ "		 stop_id,  \n"
	+ "		 stop_order  \n"
	+ "	 	FROM trips_late_query_with_time \n"
	+ "		 GROUP BY stop_id, stop_order \n"
	+ "	), \n"
	+ "	trips_early_query_v2 AS (  \n"
	+ "		SELECT array_to_string(array_agg(trips_early::text || ' (' || difference_in_seconds::text || ')' order by trips_early::text), '; ') AS trips_early,  \n"
	+ "		 stop_id,  \n"
	+ "		 stop_order  \n"
	+ "	 	FROM trips_early_query_with_time \n"
	+ "		 GROUP BY stop_id, stop_order \n"
	+ "	) \n"
	+ "SELECT " 
	+ "     COUNT(CASE WHEN scheduledTime-time > " + allowableEarlyMinutesStr + " THEN 1 ELSE null END) as early, \n"
	+ "     COUNT(CASE WHEN scheduledTime-time <= " + allowableEarlyMinutesStr + " AND time-scheduledTime <= " 
				+ allowableLateMinutesStr + " THEN 1 ELSE null END) AS ontime, \n" 
    + "     COUNT(CASE WHEN time-scheduledTime > " + allowableLateMinutesStr + " THEN 1 ELSE null END) AS late, \n" 
    + "     COUNT(*) AS total, \n"
    + "     s.name AS stop_name, \n"
    + "     ad.directionid AS direction_id, \n"
    + " 	trips_early_query_v2.trips_early as trips_early, \n"
    + " 	trips_late_query_v2.trips_late as trips_late  \n"
    + "FROM ArrivalsDepartures ad"
    + "	INNER JOIN Stops s ON ad.stopId = s.id \n"
    + "	LEFT JOIN trips_early_query_v2 ON s.id = trips_early_query_v2.stop_id AND ad.stopOrder = trips_early_query_v2.stop_order \n"
    + "	LEFT JOIN trips_late_query_v2 ON s.id = trips_late_query_v2.stop_id AND ad.stopOrder = trips_late_query_v2.stop_order \n"
    + "WHERE "
    // To get stop name
    + " ad.configRev = s.configRev \n"
    + " AND ad.stopId = s.id \n"
    // Only need arrivals/departures that have a schedule time
    + " AND ad.scheduledTime IS NOT NULL \n"
    // Specifies which routes to provide data for
    + SqlUtils.routeClause(request, "ad") + "\n"
    + SqlUtils.timeRangeClause(request, "ad.time", 7) + "\n"
    // Grouping and ordering is a bit complicated since might also be looking
    // at old arrival/departure data that doen't have stoporder defined. Also,
    // when configuration changes happen then the stop order can change. 
    // Therefore want to group by directionId and stop name. Need to also 
    // group by stop order so that can output it, which can be useful for 
    // debugging, plus need to order by stop order. For the ORDER BY clause
    // need to order by direction id and stop order, but also the stop name
    // as a backup for if stoporder not defined for data and is therefore 
    // always the same and doesn't provide any ordering info.
    + " GROUP BY directionid, s.name, s.id, ad.stopOrder, trips_early_query_v2.trips_early, trips_late_query_v2.trips_late \n"
    + " ORDER BY directionid, ad.stopOrder, s.name";

// Just for debugging
System.out.println("\nFor schedule adherence by stop query sql=\n" + sql);
    		
// Do the query and return result in JSON format    
String agencyId = request.getParameter("a");
String jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
response.setContentType("application/json");
response.setHeader("Access-Control-Allow-Origin", "*");
response.getWriter().write(jsonString);
} catch (Exception e) {
	response.setStatus(400);
	response.getWriter().write(e.getMessage());
	return;
}%>