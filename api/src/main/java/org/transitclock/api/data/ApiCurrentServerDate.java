/* (C)2023 */
package org.transitclock.api.data;

import java.util.Date;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "servertime")
public class ApiCurrentServerDate {
    @XmlAttribute
    private Date currentTime;

    /**
     * Need a no-arg constructor for Jersey. Otherwise get really obtuse "MessageBodyWriter not
     * found for media type=application/json" exception.
     */
    protected ApiCurrentServerDate() {}

    public ApiCurrentServerDate(Date currentTime) {
        this.currentTime = currentTime;
    }
}
