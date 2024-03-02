/* (C)2023 */
package org.transitclock.core.predictiongenerator;

import org.transitclock.core.Indices;
import org.transitclock.core.avl.space.SpatialMatch;
import org.transitclock.core.VehicleState;
import org.transitclock.domain.structs.AvlReport;

public interface PredictionComponentElementsGenerator {
    /* this generates a prediction for travel time between stops */
    long getTravelTimeForPath(Indices indices, AvlReport avlReport, VehicleState vehicleState);

    long getStopTimeForPath(Indices indices, AvlReport avlReport, VehicleState vehicleState);

    long expectedTravelTimeFromMatchToEndOfStopPath(AvlReport avlReport, SpatialMatch match);
}
