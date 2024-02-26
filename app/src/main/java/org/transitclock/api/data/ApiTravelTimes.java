/* (C)2023 */
package org.transitclock.api.data;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import org.transitclock.domain.structs.TravelTimesForStopPath;
import org.transitclock.domain.structs.TravelTimesForTrip;

/**
 * @author SkiBu Smith
 */
public class ApiTravelTimes {

    @XmlAttribute
    private int configRev;

    @XmlAttribute
    private int travelTimeRev;

    @XmlAttribute
    private String tripPatternId;

    @XmlAttribute
    private String tripCreatedForId;

    @XmlElement(name = "travelTimesForStopPath")
    private List<ApiTravelTimesForStopPath> travelTimesForStopPaths;


    /**
     * Need a no-arg constructor for Jersey. Otherwise get really obtuse "MessageBodyWriter not
     * found for media type=application/json" exception.
     */
    protected ApiTravelTimes() {}

    /**
     * Constructor
     *
     * @param travelTimes
     */
    public ApiTravelTimes(TravelTimesForTrip travelTimes) {
        this.configRev = travelTimes.getConfigRev();
        this.travelTimeRev = travelTimes.getTravelTimesRev();
        this.tripPatternId = travelTimes.getTripPatternId();
        this.tripCreatedForId = travelTimes.getTripCreatedForId();

        this.travelTimesForStopPaths = new ArrayList<ApiTravelTimesForStopPath>();

        for (int stopPathIndex = 0;
                stopPathIndex < travelTimes.getTravelTimesForStopPaths().size();
                ++stopPathIndex) {
            TravelTimesForStopPath travelTimesForStopPath = travelTimes.getTravelTimesForStopPath(stopPathIndex);
            this.travelTimesForStopPaths.add(new ApiTravelTimesForStopPath(stopPathIndex, travelTimesForStopPath));
        }
    }
}
