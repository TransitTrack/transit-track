/* (C)2023 */
package org.transitclock.api;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Declares that all classes in package org.transitclock.api.rootResources will be searched for
 * being a root-resource class with methods annotated with @Path indicating that it handles
 * requests.
 *
 * <p>Uses "v1" for the @ApplicationPath to specify the version of the feed.
 *
 * @author SkiBu Smith
 */
@ApplicationPath("v1")
public class ApiApplication extends ResourceConfig {

    public ApiApplication() {
        // Register all root-resource classes in package that handle @Path
        // requests
        packages("org.transitclock.api.rootResources");
    }
}