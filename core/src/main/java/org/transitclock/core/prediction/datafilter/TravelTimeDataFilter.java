/* (C)2023 */
package org.transitclock.core.prediction.datafilter;

import org.transitclock.service.dto.IpcArrivalDeparture;

/**
 * @author scrudden Interface to implement to filter out unwanted travel time data.
 */
public interface TravelTimeDataFilter {
    boolean filter(IpcArrivalDeparture departure, IpcArrivalDeparture arrival);
}
