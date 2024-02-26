/* (C)2023 */
package org.transitclock.api.resources;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateParam {

    private LocalDate date;

    public DateParam(String in) throws RuntimeException {
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_DATE);

        } catch (Exception exception) {
            throw new RuntimeException("400");
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public String format() {
        return date.toString();
    }
}
