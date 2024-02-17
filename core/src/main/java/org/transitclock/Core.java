/* (C)2023 */
package org.transitclock;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.transitclock.config.data.CoreConfig;
import org.transitclock.core.ServiceUtils;
import org.transitclock.core.TimeoutHandlerModule;
import org.transitclock.core.dataCache.PredictionDataCache;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.domain.hibernate.DataDbLogger;
import org.transitclock.domain.structs.ActiveRevision;
import org.transitclock.domain.structs.Agency;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.service.*;
import org.transitclock.utils.Time;
import org.transitclock.utils.threading.ExtendedScheduledThreadPoolExecutor;
import org.transitclock.utils.threading.NamedThreadFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.TimeZone;
import java.util.concurrent.*;

/**
 * The main class for running a Transitime Core real-time data processing system. Handles command
 * line arguments and then initiates AVL feed.
 *
 * @author SkiBu Smith
 */
@Slf4j
public class Core {
    private static Core SINGLETON;

    // Contains the configuration data read from database
    private final DbConfig configData;

    private final DataDbLogger dataDbLogger;

    @SneakyThrows
    private Core(@NonNull String agencyId, ModuleRegistry moduleRegistry) {
         // Read in config rev from ActiveRevisions table in db
         ActiveRevision activeRevision = ActiveRevision.get(agencyId);

         // If config rev not set properly then simply log error.
         // Originally would also exit() but found that want system to
         // work even without GTFS configuration so that can test AVL feed.
         if (!activeRevision.isValid()) {
             logger.error("ActiveRevisions in database is not valid. The configuration revs must be set to proper values. {}", activeRevision);
         }
         int configRev = activeRevision.getConfigRev();

         // Set the timezone so that when dates are read from db or are logged
         // the time will be correct. Therefore, this needs to be done right at
         // the start of the application, before db is read.
         TimeZone timeZone = Agency.getTimeZoneFromDb(agencyId);
         TimeZone.setDefault(timeZone);

         // Clears out the session factory so that a new one will be created for
         // future db access. This way new db connections are made. This is
         // useful for dealing with timezones and postgres. For that situation
         // want to be able to read in timezone from db so can set default
         // timezone. Problem with postgres is that once a factory is used to
         // generate sessions the database will continue to use the default
         // timezone that was configured at that time. This means that future
         // calls to the db will use the wrong timezone! Through this function
         // one can read in timezone from database, set the default timezone,
         // clear the factory so that future db connections will use the newly
         // configured timezone, and then successfully process dates.
         // HibernateUtils.clearSessionFactory();

         // Read in all GTFS based config data from the database
         configData = new DbConfig(agencyId, configRev);
         ApplicationContext.registerSingleton(configData);
         ApplicationContext.registerSingleton(configData.getServiceUtils());
         ApplicationContext.registerSingleton(configData.getTime());

         // Create the DataDBLogger so that generated data can be stored
         // to database via a robust queue. But don't actually log data
         // if in playback mode since then would be writing data again
         // that was first written when predictor was run in real time.
         // Note: DataDbLogger needs to be started after the timezone is set.
         // Otherwise when running for a different timezone than what the
         // computer is setup for then can log data using the wrong time!
         // This is strange since setting TimeZone.setDefault() is supposed
         // to work across all threads it appears that sometimes it wouldn't
         // work if Db logger started first.
         dataDbLogger = DataDbLogger.getDataDbLogger(agencyId, CoreConfig.storeDataInDatabase(), CoreConfig.pauseIfDbQueueFilling());
         ApplicationContext.registerSingleton(dataDbLogger);


         moduleRegistry.createAndSchedule(TimeoutHandlerModule.class);

         // Start any optional modules.
         var optionalModuleNames = CoreConfig.getOptionalModules();
         for (Class<?> moduleName : optionalModuleNames) {
             logger.info("Starting up optional module {}", moduleName);
             try {
                 moduleRegistry.createAndSchedule(moduleName);
             } catch (NoSuchMethodException e) {
                 logger.error("Failed to start {} because could not find constructor with agencyId arg", moduleName, e);
             } catch (InvocationTargetException e) {
                 logger.error("Failed to start {}", moduleName, e);
             } catch (InstantiationException e) {
                 logger.error("Failed to start {}", moduleName, e);
             } catch (IllegalAccessException e) {
                 logger.error("Failed to start {}", moduleName, e);
             }
         }

     }

    /**
     * Creates the Core object for the application. There can only be one Core object per
     * application. Uses CoreConfig.getAgencyId() to determine the agencyId. This means it typically
     * uses the agency ID specified by the Java property -Dtransitclock.core.agencyId .
     *
     * <p>Usually doesn't need to be called directly because can simply use Core.getInstance().
     *
     * <p>Synchronized to ensure that don't create more than a single Core.
     *
     * @return The Core singleton, or null if could not create it
     */
    public static synchronized Core createCore(@NonNull String agencyId,
                                               @NonNull ModuleRegistry registry) {
        // Make sure only can have a single Core object
        if (SINGLETON != null) {
            logger.error("Core singleton already created. Cannot create another one.");
            return SINGLETON;
        }

        SINGLETON = new Core(agencyId, registry);

        return SINGLETON;
    }


    public static synchronized Core getInstance() {
        if (SINGLETON == null) {
            throw new RuntimeException();
        }
        return SINGLETON;
    }

    /**
     * Makes the config data available to all
     *
     * @return
     */
    public DbConfig getDbConfig() {
        return configData;
    }

    /**
     * Returns the ServiceUtils object that can be reused for efficiency.
     */
    public ServiceUtils getServiceUtils() {
        return configData.getServiceUtils();
    }

    public Time getTime() {
        return configData.getTime();
    }

    /**
     * Returns the DataDbLogger for logging data to db.
     *
     * @return
     */
    public DataDbLogger getDbLogger() {
        return dataDbLogger;
    }
}
