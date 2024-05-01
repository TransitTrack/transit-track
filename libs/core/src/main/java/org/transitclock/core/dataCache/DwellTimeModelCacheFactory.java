/* (C)2023 */
package org.transitclock.core.dataCache;

import org.transitclock.config.ClassConfigValue;
import org.transitclock.core.dataCache.ehcache.scheduled.DwellTimeModelCache;
import org.transitclock.core.prediction.scheduled.dwell.DwellModel;
import org.transitclock.properties.PredictionProperties;

import org.ehcache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Sean Óg Crudden Factory that will provide cache to hold dwell time model class instances
 *     for each stop.
 */
@Configuration
public class DwellTimeModelCacheFactory {
    private static final ClassConfigValue className = new ClassConfigValue(
            "transitclock.core.cache.dwellTimeModelCache",
            DummyDwellTimeModelCacheImpl.class,
            "Specifies the class used to cache RLS data for a stop.");


    @Bean
    public DwellTimeModelCacheInterface dwellTimeModelCacheInterface(CacheManager cm,
                                                                     DwellModel dwellModel,
                                                                     StopArrivalDepartureCacheInterface stopArrivalDepartureCacheInterface,
                                                                     PredictionProperties properties) {
        var value = className.getValue();
        if (value == DwellTimeModelCache.class) {
            return new DwellTimeModelCache(cm, dwellModel, stopArrivalDepartureCacheInterface, properties.getRls());
        }

        return new DummyDwellTimeModelCacheImpl();
    }
}
