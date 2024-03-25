<%-- Provides schedule adherence data in JSON format. Provides
     count of times for each time bucket. The json time_period is 
     the floor of the time span span. So it is -60 for -60 to -30 seconds 
     late, -30 for -30 seconds to 0 seconds late,
     0 for 0 seconds to 30 seconds early, 30 for 30 seconds to 60 seconds
     early, 60 for 60 seconds to 90 seconds late, etc
     
     Request parameters are:
       a - agency ID
       r - route ID or route short name. Can specify multiple routes. 
           Not specifying route provides data for all routes.
       dateRange - in format "xx/xx/xx to yy/yy/yy"
       beginDate - date to begin query. For if dateRange not used.
       numDays - number of days can do query. Limited to 31 days. For if dateRange not used.
       beginTime - for optionally specifying time of day for query for each day
       endTime - for optionally specifying time of day for query for each day
       allowableEarly - how early vehicle can be and still be OK.  Decimal format OK. 
       allowableLate - how early vehicle can be and still be OK. Decimal format OK.
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="org.transitclock.core.reports.GenericJsonQuery" %>
<%@ page import="org.transitclock.core.reports.SqlUtils" %>
<%
    try {
        int BUCKET_TIME = 30;
        String epochCommandPre = "EXTRACT (EPOCH FROM ";
        String epochCommandPost = ")";

        String sql =
                "SELECT "
                    + "  COUNT(*) AS counts_per_time_period, \n"
                    // Put into time buckets of every BUCKET_TIME seconds.
                    + "  FLOOR(" + epochCommandPre + " (scheduled_time-time)" + epochCommandPost + " / " + BUCKET_TIME + ")*" + BUCKET_TIME + " AS time_period \n"
                + "FROM arrivals_departures ad\n"
                + "WHERE "
                    // Only need arrivals/departures that have a schedule time
                    + " ad.scheduled_time IS NOT NULL \n"
                    // Ignore stops where schedule adherence really far off
                    + " AND ABS(" + epochCommandPre + " (scheduled_time-time)" + epochCommandPost + ") < 3600\n"
                    // Specifies which routes to provide data for
                    + SqlUtils.routeClause(request.getParameter("r"), "ad") + "\n"
                    + SqlUtils.timeRangeClause(request, "ad.time", 7) + "\n"
                // Grouping needed to put times in time buckets
                + " GROUP BY time_period \n"
                // Order by lateness so can easily understand results
                + " ORDER BY time_period;";

        String agencyId = request.getParameter("a");
        String jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
        response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getWriter().write(jsonString);
    } catch (Exception e) {
        response.setStatus(400);
        response.getWriter().write(e.getMessage());
        return;
    }
%>