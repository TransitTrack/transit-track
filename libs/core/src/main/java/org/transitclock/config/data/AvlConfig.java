/* (C)2023 */
package org.transitclock.config.data;

import org.transitclock.config.*;

/**
 * Handles the AVL configuration data.
 *
 * @author SkiBu Smith
 */
public class AvlConfig {

    /**
     * How frequently an AVL feed should be polled for new data.
     *
     * @return
     */
    public static int getSecondsBetweenAvlFeedPolling() {
        return secondsBetweenAvlFeedPolling.getValue();
    }

    private static final IntegerConfigValue secondsBetweenAvlFeedPolling = new IntegerConfigValue(
            "transitclock.avl.feedPollingRateSecs", 5, "How frequently an AVL feed should be polled for new data.");

    /**
     * For when polling AVL XML feed.
     *
     * @return
     */
    public static int getAvlFeedTimeoutInMSecs() {
        return avlFeedTimeoutInMSecs.getValue();
    }

    private static final IntegerConfigValue avlFeedTimeoutInMSecs = new IntegerConfigValue(
            "transitclock.avl.feedTimeoutInMSecs",
            10000,
            "For when polling AVL XML feed. The feed logs error if "
                    + "the timeout value is exceeded when performing the XML "
                    + "request.");

    /**
     * Max speed that an AVL report is allowed to have.
     *
     * @return max speed in m/s
     */
    public static double getMaxAvlSpeed() {
        return maxAvlSpeed.getValue();
    }

    private static final DoubleConfigValue maxAvlSpeed = new DoubleConfigValue(
            "transitclock.avl.maxSpeed",
            31.3, // 31.3m/s = 70mph
            "Max speed between AVL reports for a vehicle. If this value is exceeded then the AVL report is ignored.");

    private static DoubleConfigValue alternativeMaxSpeed = new DoubleConfigValue(
            "transitclock.avl.alternativemaxspeed",
            15.0, // 31.3m/s = 70mph
            "Alternative max speed between AVL reports for a vehicle. If this value is exceeded then the AVL report is ignored.");

    public static DoubleConfigValue getAlternativeMaxSpeed() {
        return alternativeMaxSpeed;
    }

    public static void setAlternativeMaxSpeed(DoubleConfigValue alternativeMaxSpeed) {
        AvlConfig.alternativeMaxSpeed = alternativeMaxSpeed;
    }

    /**
     * Maximum number of stopPaths to look ahead.
     *
     * @return max number
     */
    public static int getMaxStopPathsAhead() {
        return maxStopPathsAhead.getValue();
    }

    private static final IntegerConfigValue maxStopPathsAhead =
            new IntegerConfigValue("transitclock.avl.maxStopPathsAhead", 999, "Max stopPaths ahead to look for match.");

    /**
     * If AVL report speed is below this threshold then the heading is not considered valid.
     *
     * @return
     */
    public static double minSpeedForValidHeading() {
        return minSpeedForValidHeading.getValue();
    }

    private static final DoubleConfigValue minSpeedForValidHeading = new DoubleConfigValue(
            "transitclock.avl.minSpeedForValidHeading",
            1.5, // 1.5m/s = .34mph
            "If AVL report speed is below this threshold then the " + "heading is not considered valid.");

    /**
     * For filtering out bad AVL reports. The default values of latitude 15.0 to 55.0 and longitude
     * of -135.0 to -60.0 are for North America, including Mexico and Canada. Can see maps of
     * lat/lon at http://www.mapsofworld.com/lat_long/north-america.html
     *
     * @return
     */
    public static float getMinAvlLatitude() {
        return minAvlLatitude.getValue();
    }

    public static String getMinAvlLatitudeParamName() {
        return minAvlLatitude.getID();
    }

    private static final FloatConfigValue minAvlLatitude = new FloatConfigValue(
            "transitclock.avl.minLatitude",
            15.0f,
            "For filtering out bad AVL reports. The default values "
                    + "of latitude 15.0 to 55.0 and longitude of -135.0 to "
                    + "-60.0 are for North America, including Mexico and "
                    + "Canada. Can see maps of lat/lon at "
                    + "http://www.mapsofworld.com/lat_long/north-america.html");

    public static float getMaxAvlLatitude() {
        return maxAvlLatitude.getValue();
    }

    public static String getMaxAvlLatitudeParamName() {
        return maxAvlLatitude.getID();
    }

    private static final FloatConfigValue maxAvlLatitude = new FloatConfigValue(
            "transitclock.avl.maxLatitude",
            55.0f,
            "For filtering out bad AVL reports. The default values "
                    + "of latitude 15.0 to 55.0 and longitude of -135.0 to "
                    + "-60.0 are for North America, including Mexico and "
                    + "Canada. Can see maps of lat/lon at "
                    + "http://www.mapsofworld.com/lat_long/north-america.html");

    public static float getMinAvlLongitude() {
        return minAvlLongitude.getValue();
    }

    public static String getMinAvlLongitudeParamName() {
        return minAvlLongitude.getID();
    }

    private static final FloatConfigValue minAvlLongitude = new FloatConfigValue(
            "transitclock.avl.minLongitude",
            -135.0f,
            "For filtering out bad AVL reports. The default values "
                    + "of latitude 15.0 to 55.0 and longitude of -135.0 to "
                    + "-60.0 are for North America, including Mexico and "
                    + "Canada. Can see maps of lat/lon at "
                    + "http://www.mapsofworld.com/lat_long/north-america.html");

    public static float getMaxAvlLongitude() {
        return maxAvlLongitude.getValue();
    }

    public static String getMaxAvlLongitudeParamName() {
        return maxAvlLongitude.getID();
    }

    private static final FloatConfigValue maxAvlLongitude = new FloatConfigValue(
            "transitclock.avl.maxLongitude",
            -60.0f,
            "For filtering out bad AVL reports. The default values "
                    + "of latitude 15.0 to 55.0 and longitude of -135.0 to "
                    + "-60.0 are for North America, including Mexico and "
                    + "Canada. Can see maps of lat/lon at "
                    + "http://www.mapsofworld.com/lat_long/north-america.html");

    /**
     * So can filter out unpredictable assignments such as for training coaches, service vehicles,
     * or simply vehicles that are not in service and should not be attempted to be made
     * predictable. Returns empty string, the default value if
     * transitclock.avl.unpredictableAssignmentsRegEx is not set.
     */
    public static String getUnpredictableAssignmentsRegEx() {
        return unpredictableAssignmentsRegEx.getValue();
    }

    private static final StringConfigValue unpredictableAssignmentsRegEx = new StringConfigValue(
            "transitclock.avl.unpredictableAssignmentsRegEx",
            "", // default value
            "So can filter out unpredictable assignments such as for "
                    + "training coaches, service vehicles, or simply vehicles "
                    + "that are not in service and should not be attempted to "
                    + "be made predictable. Returns empty string, the default "
                    + "value if transitclock.avl.unpredictableAssignmentsRegEx "
                    + "is not set.");

//    /**
//     * Minimum allowable time in seconds between AVL reports for a vehicle. If get a report closer
//     * than this number of seconds to the previous one then the new report is filtered out and not
//     * processed. Important for when reporting rate is really high, such as every few seconds.
//     */
//    public static int getMinTimeBetweenAvlReportsSecs() {
//        return minTimeBetweenAvlReportsSecs.getValue();
//    }
//
//    private static final IntegerConfigValue minTimeBetweenAvlReportsSecs = new IntegerConfigValue(
//            "transitclock.avl.minTimeBetweenAvlReportsSecs",
//            5,
//            "Minimum allowable time in seconds between AVL reports for "
//                    + "a vehicle. If get a report closer than this number of "
//                    + "seconds to the previous one then the new report is "
//                    + "filtered out and not processed. Important for when "
//                    + "reporting rate is really high, such as every few "
//                    + "seconds.");


    public static StringConfigValue url =
            new StringConfigValue("transitclock.avl.url", "The URL of the AVL feed to poll.");

    public static StringConfigValue authenticationUser = new StringConfigValue(
            "transitclock.avl.authenticationUser",
            "If authentication used for the feed then this specifies " + "the user.");

    public static StringConfigValue authenticationPassword = new StringConfigValue(
            "transitclock.avl.authenticationPassword",
            "If authentication used for the feed then this specifies " + "the password.");

//    public static BooleanConfigValue shouldProcessAvl = new BooleanConfigValue(
//            "transitclock.avl.shouldProcessAvl",
//            true,
//            "Usually want to process the AVL data when it is read in "
//                    + "so that predictions and such are generated. But if "
//                    + "debugging then can set this param to false.");




//    public static BooleanConfigValue processInRealTime = new BooleanConfigValue(
//            "transitclock.avl.processInRealTime",
//            false,
//            "For when getting batch of AVL data from a CSV file. "
//                    + "When true then when reading in do at the same speed as "
//                    + "when the AVL was created. Set to false it you just want "
//                    + "to read in as fast as possible.");


    // avl executor
//    public static final IntegerConfigValue avlQueueSize = new IntegerConfigValue(
//            "transitclock.avl.queueSize",
//            2000,
//            "How many items to go into the blocking AVL queue "
//                    + "before need to wait for queue to have space. Should "
//                    + "be approximately 50% more than the number of reports "
//                    + "that will be read during a single AVL polling cycle. "
//                    + "If too big then wasteful. If too small then not all the "
//                    + "data will be rejected by the ThreadPoolExecutor. ");

//    public static IntegerConfigValue numAvlThreads = new IntegerConfigValue(
//            "transitclock.avl.numThreads",
//            1,
//            "How many threads to be used for processing the AVL "
//                    + "data. For most applications just using a single thread "
//                    + "is probably sufficient and it makes the logging simpler "
//                    + "since the messages will not be interleaved. But for "
//                    + "large systems with lots of vehicles then should use "
//                    + "multiple threads, such as 3-15 so that more of the cores "
//                    + "are used.");
}
