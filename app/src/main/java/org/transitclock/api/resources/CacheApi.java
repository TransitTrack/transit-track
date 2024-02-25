/* (C)2023 */
package org.transitclock.api.resources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.transitclock.api.data.ApiArrivalDepartures;
import org.transitclock.api.data.ApiCacheDetails;
import org.transitclock.api.data.ApiHistoricalAverage;
import org.transitclock.api.data.ApiHistoricalAverageCacheKeys;
import org.transitclock.api.data.ApiHoldingTime;
import org.transitclock.api.data.ApiHoldingTimeCacheKeys;
import org.transitclock.api.data.ApiKalmanErrorCacheKeys;
import org.transitclock.api.data.ApiPredictionsForStopPath;
import org.transitclock.api.utils.StandardParameters;
import org.transitclock.api.utils.WebUtils;
import org.transitclock.service.dto.IpcArrivalDeparture;
import org.transitclock.service.dto.IpcHistoricalAverage;
import org.transitclock.service.dto.IpcHistoricalAverageCacheKey;
import org.transitclock.service.dto.IpcHoldingTime;
import org.transitclock.service.dto.IpcHoldingTimeCacheKey;
import org.transitclock.service.dto.IpcKalmanErrorCacheKey;
import org.transitclock.service.dto.IpcPredictionForStopPath;
import org.transitclock.service.contract.CacheQueryInterface;
import org.transitclock.service.contract.HoldingTimeInterface;
import org.transitclock.service.contract.PredictionAnalysisInterface;

/**
 * Contains the API commands for the Transitime API for getting info on data that is cached.
 *
 * <p>The data output can be in either JSON or XML. The output format is specified by the accept
 * header or by using the query string parameter "format=json" or "format=xml".
 *
 * @author SkiBu Smith
 */
@Component
@Path("/key/{key}/agency/{agency}")
public class CacheApi extends BaseApiResource {

    @Path("/command/kalmanerrorcachekeys")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Gets the list of Kalman Cache error.",
            description = "Gets the list of Kalman Cache error.",
            tags = {"kalman", "cache"})
    public Response getKalmanErrorCacheKeys(@BeanParam StandardParameters stdParameters)
            throws WebApplicationException {
        try {
            List<IpcKalmanErrorCacheKey> result = cacheQueryInterface.getKalmanErrorCacheKeys();
            ApiKalmanErrorCacheKeys keys = new ApiKalmanErrorCacheKeys(result);
            Response response = stdParameters.createResponse(keys);

            return response;
        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/scheduledbasedhistoricalaveragecachekeys")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Gets a list of the keys that have values in the historical average cache for"
                    + " schedules based services.",
            description = "Gets a list of the keys that have values in the historical average cache for"
                    + " schedules based services.",
            tags = {"cache"})
    public Response getSchedulesBasedHistoricalAverageCacheKeys(@BeanParam StandardParameters stdParameters)
            throws WebApplicationException {
        try {
            List<IpcHistoricalAverageCacheKey> result =
                    cacheQueryInterface.getScheduledBasedHistoricalAverageCacheKeys();

            ApiHistoricalAverageCacheKeys keys = new ApiHistoricalAverageCacheKeys(result);

            Response response = stdParameters.createResponse(keys);

            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    // TODO This is not completed and should not be used.
    @Path("/command/frequencybasedhistoricalaveragecachekeys")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Gets a list of the keys that have values in the historical average cache for"
                    + " frequency based services.",
            description = "Gets a list of the keys that have values in the historical average cache for"
                    + " frequency based services.<font color=\"#FF0000\">This is not completed"
                    + " and should not be used.<font>",
            tags = {"cache"})
    public Response getFrequencyBasedHistoricalAverageCacheKeys(@BeanParam StandardParameters stdParameters)
            throws WebApplicationException {
        try {
            List<IpcHistoricalAverageCacheKey> result =
                    cacheQueryInterface.getFrequencyBasedHistoricalAverageCacheKeys();

            ApiHistoricalAverageCacheKeys keys = new ApiHistoricalAverageCacheKeys(result);

            Response response = stdParameters.createResponse(keys);

            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/holdingtimecachekeys")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Gets a list of the keys for the holding times in the cache.",
            description = "Gets a list of the keys for the holding times in the cache.",
            tags = {"cache"})
    public Response getHoldingTimeCacheKeys(@BeanParam StandardParameters stdParameters)
            throws WebApplicationException {
        try {
            List<IpcHoldingTimeCacheKey> result = cacheQueryInterface.getHoldingTimeCacheKeys();

            ApiHoldingTimeCacheKeys keys = new ApiHoldingTimeCacheKeys(result);

            Response response = stdParameters.createResponse(keys);

            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    /**
     * Returns info about a cache.
     *
     * @param stdParameters
     * @param cachename this is the name of the cache to get the size of.
     * @return
     * @throws WebApplicationException
     */
    @Path("/command/cacheinfo")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns the number of entries in the cacheName cache.",
            description = "Returns the number of entries in the cacheName cache. The name is passed"
                    + " throug the cachename parameter.",
            tags = {"cache"})
    public Response getCacheInfo(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Name of the cache", required = true) @QueryParam(value = "cachename")
                    String cachename)
            throws WebApplicationException {
        try {

            Integer size = cacheQueryInterface.entriesInCache(cachename);

            if (size != null) return stdParameters.createResponse(new ApiCacheDetails(cachename, size));
            else throw new Exception("No cache named:" + cachename);

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/stoparrivaldeparturecachedata")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns a list of current arrival or departure events for a specified stop"
                    + " that are in the cache.",
            description = "Returns a list of current arrival or departure events for a specified stop"
                    + " that are in the cache.",
            tags = {"cache"})
    public Response getStopArrivalDepartureCacheData(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Stop Id.", required = true) @QueryParam(value = "stopid") String stopid,
            @QueryParam(value = "date") Date date)
            throws WebApplicationException {
        try {

            List<IpcArrivalDeparture> result = cacheQueryInterface.getStopArrivalDepartures(stopid);

            ApiArrivalDepartures apiResult = new ApiArrivalDepartures(result);
            Response response = stdParameters.createResponse(apiResult);
            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/triparrivaldeparturecachedata")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns the arrivals and departures for a trip on a specific day and start" + " time.",
            description = "Returns a list  of the arrivals and departures for a trip on a specific day"
                    + " and start time.Either tripId or date must be specified.",
            tags = {"cache"})
    public Response getTripArrivalDepartureCacheData(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "if specified, returns the list for that tripId.", required = false)
                    @QueryParam(value = "tripId")
                    String tripid,
            @Parameter(description = "if specified, returns the list for that date.", required = false)
                    @QueryParam(value = "date")
                    DateParam date,
            @Parameter(description = "if specified, returns the list for that starttime.", required = false)
                    @QueryParam(value = "starttime")
                    Integer starttime)
            throws WebApplicationException {
        try {

            LocalDate queryDate = null;
            if (date != null) queryDate = date.getDate();
            List<IpcArrivalDeparture> result =
                    cacheQueryInterface.getTripArrivalDepartures(tripid, queryDate, starttime);

            ApiArrivalDepartures apiResult = new ApiArrivalDepartures(result);
            Response response = stdParameters.createResponse(apiResult);
            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    /*
     * This will give the historical cache value for an individual stop path
     * index of a trip private String tripId; private Integer stopPathIndex;
     */
    @Path("/command/historicalaveragecachedata")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns the historical cache value for an individual stop path index of a" + " trip.",
            description = "Returns the historical cache value for an individual stop path index of a" + " trip.",
            tags = {"cache"})
    public Response getHistoricalAverageCacheData(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Trip Id", required = true) @QueryParam(value = "tripId") String tripId,
            @Parameter(description = "Stop path index", required = true) @QueryParam(value = "stopPathIndex")
                    Integer stopPathIndex) {
        try {
            IpcHistoricalAverage result = cacheQueryInterface.getHistoricalAverage(tripId, stopPathIndex);
            Response response = stdParameters.createResponse(new ApiHistoricalAverage(result));
            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/getkalmanerrorvalue")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns the latest Kalman error value for a the stop path of a trip.",
            description = "Returns the latest Kalman error value for a the stop path of a trip.",
            tags = {"kalman", "cache"})
    public Response getKalmanErrorValue(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Trip Id", required = true) @QueryParam(value = "tripId") String tripId,
            @Parameter(description = "Stop path index", required = true) @QueryParam(value = "stopPathIndex")
                    Integer stopPathIndex) {
        try {

            Double result = cacheQueryInterface.getKalmanErrorValue(tripId, stopPathIndex);

            Response response = stdParameters.createResponse(result);

            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/getstoppathpredictions")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns a list of predictions for a the stop path of a trip.",
            description = "Returns a list of predictions for a the stop path of a trip.",
            tags = {"cache"})
    // TODO: (vsperez) I believe date is not used at all
    public Response getStopPathPredictions(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Algorith used for calculating the perdiction", required = false)
                    @QueryParam(value = "algorithm")
                    String algorithm,
            @Parameter(description = "Trip Id", required = true) @QueryParam(value = "tripId") String tripId,
            @Parameter(description = "Stop path index", required = true) @QueryParam(value = "stopPathIndex")
                    Integer stopPathIndex,
            @Parameter(description = "Specified the date.", required = true) @QueryParam(value = "date")
                    DateParam date) {
        try {
            LocalTime midnight = LocalTime.MIDNIGHT;
            Date end_date = null;
            Date start_date = null;
            if (date != null) {
                LocalDate now = date.getDate();

                LocalDateTime todayMidnight = LocalDateTime.of(now, midnight);
                LocalDateTime yesterdatMidnight = todayMidnight.plusDays(-1);

                end_date =
                        Date.from(todayMidnight.atZone(ZoneId.systemDefault()).toInstant());
                start_date = Date.from(
                        yesterdatMidnight.atZone(ZoneId.systemDefault()).toInstant());
            }

            List<IpcPredictionForStopPath> result = predictionAnalysisInterface.getCachedTravelTimePredictions(
                    tripId, stopPathIndex, start_date, end_date, algorithm);

            Response response = stdParameters.createResponse(new ApiPredictionsForStopPath(result));

            return response;

        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }

    @Path("/command/getholdingtime")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Operation(
            summary = "Returns the IpcHoldingTime for a specific stop Id and vehicle Id.",
            description = "Returns the IpcHoldingTime for a specific stop Id and vehicle Id.",
            tags = {"cache"})
    public Response getHoldingTime(
            @BeanParam StandardParameters stdParameters,
            @Parameter(description = "Stop id", required = true) @QueryParam(value = "stopId") String stopId,
            @Parameter(description = "Vehicle id", required = true) @QueryParam(value = "vehicleId") String vehicleId) {
        try {
            IpcHoldingTime result = holdingTimeInterface.getHoldTime(stopId, vehicleId);

            Response response = stdParameters.createResponse(new ApiHoldingTime(result));

            return response;
        } catch (Exception e) {
            // If problem getting result then return a Bad Request
            throw WebUtils.badRequestException(e.getMessage());
        }
    }
}
