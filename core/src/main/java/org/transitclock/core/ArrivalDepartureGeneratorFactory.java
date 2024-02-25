/* (C)2023 */
package org.transitclock.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.transitclock.config.ClassConfigValue;
import org.transitclock.core.dataCache.*;
import org.transitclock.core.dataCache.frequency.FrequencyBasedHistoricalAverageCache;
import org.transitclock.core.dataCache.scheduled.ScheduleBasedHistoricalAverageCache;
import org.transitclock.core.holdingmethod.HoldingTimeGenerator;
import org.transitclock.domain.hibernate.DataDbLogger;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.utils.ClassInstantiator;

/**
 * For instantiating a ArrivalDepartureGenerator object that generates arrival/departure data when a
 * new match is generated for a vehicle. The class to be instantiated can be set using the config
 * variable transitclock.core.arrivalDepartureGeneratorClass
 *
 * @author SkiBu Smith
 */
@Configuration
public class ArrivalDepartureGeneratorFactory {
    @Value("${transitclock.factory.arrival-departure-generator:org.transitclock.core.ArrivalDepartureGeneratorDefaultImpl}")
    private Class<?> neededClass;

    @Bean
    public ArrivalDepartureGenerator arrivalDepartureGenerator(ScheduleBasedHistoricalAverageCache scheduleBasedHistoricalAverageCache, FrequencyBasedHistoricalAverageCache frequencyBasedHistoricalAverageCache, HoldingTimeCache holdingTimeCache, VehicleStateManager vehicleStateManager, HoldingTimeGenerator holdingTimeGenerator, TravelTimes travelTimes, TripDataHistoryCacheInterface tripDataHistoryCacheInterface, StopArrivalDepartureCacheInterface stopArrivalDepartureCacheInterface, DwellTimeModelCacheInterface dwellTimeModelCacheInterface, DataDbLogger dataDbLogger, DbConfig dbConfig) {
        // If the PredictionGenerator hasn't been created yet then do so now
        return new ArrivalDepartureGeneratorDefaultImpl(scheduleBasedHistoricalAverageCache, frequencyBasedHistoricalAverageCache, holdingTimeCache, vehicleStateManager, holdingTimeGenerator, travelTimes, tripDataHistoryCacheInterface, stopArrivalDepartureCacheInterface, dwellTimeModelCacheInterface, dataDbLogger, dbConfig);
    }
}
