/* (C)2023 */
package org.transitclock.api.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.transitclock.domain.structs.Agency;
import org.transitclock.domain.structs.Extent;
import org.transitclock.domain.structs.Location;
import org.transitclock.domain.webstructs.WebAgency;
import org.transitclock.service.contract.ConfigInterface;
import org.transitclock.utils.Time;

/**
 * For determining predictions by location for when agency is not specified so need to look through
 * all agencies.
 *
 * @author Michael
 */
public class PredsByLoc {

    // The cache of extents. Keyed on agencyId. Should not be accessed directly.
    // Should instead use getAgencyExtents().
    private static final Map<String, Extent> agencyExtentsCache = new HashMap<>();
    private static long cacheUpdatedTime = 0;

    // The maximum allowable maxDistance for getting predictions by location
    public static final double MAX_MAX_DISTANCE = 2000.0;

    private static final long CACHE_VALID_MSEC = 4 * Time.MS_PER_HOUR;

    /**
     * Returns the cache of agency extents. If haven't read in extents from the servers in more than
     * 4 hours then the cache is updated before it is returned.
     *
     * @return cache of extents
     */
    private static Map<String, Extent> getAgencyExtents(ConfigInterface configInterface) {
        // If updated cache recently then simply return it
        if (System.currentTimeMillis() < cacheUpdatedTime + CACHE_VALID_MSEC) {
            return agencyExtentsCache;
        }

        // Haven't updated cache in a while so update it now
        Collection<WebAgency> webAgencies = WebAgency.getCachedOrderedListOfWebAgencies();

        // For each agency get the extent
        for (WebAgency webAgency : webAgencies) {
            Agency agency = webAgency.getAgency();
            if (agency != null) {
                agencyExtentsCache.put(webAgency.getAgencyId(), agency.getExtent());
            }
        }

        // Return the update cache
        return agencyExtentsCache;
    }

    /**
     * Returns list of agencies that are with the specified distance of the latitude and longitude.
     *
     * @param latitude
     * @param longitude
     * @param distance
     * @return List of agencies that are nearby
     */
    public static List<String> getNearbyAgencies(ConfigInterface configInterface, double latitude, double longitude, double distance) {
        // For results of method
        List<String> nearbyAgencies = new ArrayList<>();

        // Determine which agencies are nearby and add them to list
        Location loc = new Location(latitude, longitude);
        Map<String, Extent> agencyExtents = getAgencyExtents(configInterface);
        for (String agencyId : agencyExtents.keySet()) {
            Extent agencyExtent = agencyExtents.get(agencyId);
            if (agencyExtent.isWithinDistance(loc, distance)) {
                nearbyAgencies.add(agencyId);
            }
        }

        // Return agencies that are nearby
        return nearbyAgencies;
    }
}
