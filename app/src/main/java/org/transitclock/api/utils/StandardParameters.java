/* (C)2023 */
package org.transitclock.api.utils;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.BindParam;

/**
 * For getting the standard parameters from the URI used to access the feed. Includes the key,
 * agency, and the media type (JSON or XML). Does not include command specific parameters.
 *
 * @author SkiBu Smith
 */
@Data
public class StandardParameters {

    @BindParam("agency")
//    @Parameter(description = "Specify the agency the request is intended to.")
    private String agencyId;

    @BindParam("format")
    private String formatOverride;

    // Note: Specifying a default value so that don't get a
    // 400 bad request when using wget and headers not set. But
    // this isn't enough. Still getting Bad Request. But leaving
    // this in as documentation that it was tried.
//    @HeaderParam("accept")
    @BindParam("accept")
//    @DefaultValue("application/json")
    String acceptHeader = "application/json";

//    @Context
    HttpServletRequest request;

    public void setAgency(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgency() {
        return agencyId;
    }

    /**
     * Returns the media type to use for the response based on optional accept header and the
     * optional format specification in the query string of the URL. Setting format in query string
     * overrides what is set in accept header. This way it is always simple to generate a http get
     * for particular format simply by setting query string.
     *
     * <p>If format specification is incorrect then BadRequest WebApplicationException is thrown.
     *
     * <p>The media type is not determined in the constructor because then an exception would cause
     * an ugly error message because it would be handled before the root-resource class get method
     * is being called.
     *
     * @return The resulting media type
     */
    public String getMediaType() throws RuntimeException {
        return "text/plain";
//        // Use default of APPLICATION_JSON
//        String mediaType = MediaType.APPLICATION_JSON;
//
//        // If mediaType specified (to something besides "*/*") in accept
//        // header then start with it.
//        if (acceptHeader != null && !acceptHeader.contains("*/*")) {
//            if (acceptHeader.contains(MediaType.APPLICATION_JSON))
//                mediaType = MediaType.APPLICATION_JSON;
//            else if (acceptHeader.contains(MediaType.APPLICATION_XML))
//                mediaType = MediaType.APPLICATION_XML;
//            else
//                throw WebUtils.badRequestException("Accept header \"Accept: "
//                        + acceptHeader
//                        + "\" is not valid. Must be \""
//                        + MediaType.APPLICATION_JSON
//                        + "\" or \""
//                        + MediaType.APPLICATION_XML
//                        + "\"");
//        }
//
//        // If mediaType format is overridden using the query string format
//        // parameter then use it.
//        if (formatOverride != null) {
//            // Always use lower case
//            formatOverride = formatOverride.toLowerCase();
//
//            // If mediaType override set properly then use it
//            mediaType = switch (formatOverride) {
//                case "json" -> MediaType.APPLICATION_JSON;
//                case "xml" -> MediaType.APPLICATION_XML;
//                case "human" -> MediaType.TEXT_PLAIN;
//                default -> throw WebUtils.badRequestException("Format \"format="
//                        + formatOverride
//                        + "\" from query string not valid. "
//                        + "Format must be \"json\" or \"xml\"");
//            };
//        }
//
//        return mediaType;
    }



    /**
     * For creating a Response of a single object of the appropriate media type.
     *
     * @param object Object to be returned in XML or JSON
     * @return The created response in the proper media type.
     */
    public <T> ResponseEntity<T> createResponse(T object) {
        return ResponseEntity.ok(object);
    }

    /**
     * Simple getter for the agency ID
     *
     * @return
     */
    public String getAgencyId() {
        return agencyId;
    }

    /**
     * Returns the HttpServletRequest.
     *
     * @return
     */
    public HttpServletRequest getRequest() {
        return request;
    }
}
