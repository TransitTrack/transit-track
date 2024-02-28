/* (C)2023 */
package org.transitclock.gtfs;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.transitclock.ApplicationProperties;
import org.transitclock.Module;
import org.transitclock.config.data.AgencyConfig;
import org.transitclock.config.data.GtfsConfig;
import org.transitclock.utils.HttpGetFile;
import org.transitclock.utils.Time;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Downloads GTFS file from web server if it has been updated and notifies users. Useful for
 * automatically determining when GTFS data has been updated by an agency.
 *
 * <p>When a GTFS file is downloaded then this module also e-mails recipients specified by the
 * parameter transitclock.monitoring.emailRecipients
 *
 * @author SkiBu Smith
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "transitclock.gtfs.auto-update.enabled", havingValue = "true")
public class GtfsUpdatedModule extends Module {
    private final ApplicationProperties.Gtfs gtfsConfig;

    public GtfsUpdatedModule(ApplicationProperties properties) {
        this.gtfsConfig = properties.getGtfs();
    }

    /**
     * Copies the specified file to a directory at the same directory level but with the directory
     * name that is the last modified date of the file (e.g. 03-28-2015).
     *
     * @param fullFileName The full name of the file to be copied
     */
    private static void archive(String fullFileName) {
        // Determine name of directory to archive file into. Use date of
        // lastModified time of file e.g. yyyy-MM-dd. Putting year first
        // and then month means that the directories will be listed
        // chronologically when listed using unix ls command.
        File file = new File(fullFileName);
        Date lastModified = new Date(file.lastModified());
        DateFormat readableDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dirName = readableDateFormat.format(lastModified);

        // Copy the file to the sibling directory with the name that is the
        // last modified date (e.g. 03-28-2015)
        Path source = Paths.get(fullFileName);
        Path target = source.getParent().getParent().resolve(dirName).resolve(source.getFileName());

        logger.info("Archiving file {} to {}", source, target);

        try {
            // Create the directory where file is to go
            String fullDirName = target.getParent().toString();
            new File(fullDirName).mkdir();

            // Copy the file to the directory
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            logger.error("Was not able to archive GTFS file {} to {}", source, target);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    @Scheduled(fixedRateString = "${transitclock.gtfs.auto-update.intervalMsec}")
    public void run() {
        logger.info("Checking to see if GTFS should be downloaded " + "because it was modified. {}", gtfsConfig.getAutoUpdate().getUrl());

        // Construct the getter
        HttpGetFile httpGetFile = new HttpGetFile(GtfsConfig.url.getValue(), GtfsConfig.dirName.getValue());

        // If file hasn't been modified then don't want to download it
        // since it can be large. Therefore determine age of previously
        // downloaded file and use If-Modified-Since to only actually
        // get the file if it is newer on the web server
        File file = new File(httpGetFile.getFullFileName());
        boolean fileAlreadyExists = file.exists();

        if (fileAlreadyExists) {
            // Get the last modified time of the local file. Add 10 minutes
            // since the web server might be load balanced and the files
            // on the different servers might have slightly different last
            // modified times. To make sure that don't keep downloading
            // from the different servers until get the file with most
            // recent modified time add 10 minutes to the time to indicate
            // that as long as the local file is within 10 minutes of the
            // remote file that it is ok.
            long lastModified = file.lastModified() + 10 * Time.MS_PER_MIN;

            httpGetFile.addRequestHeader("If-Modified-Since", Time.httpDate(lastModified));

            logger.debug(
                "The file {} already exists so using " + "If-Modified-Since header of \"{}\" or {} msec.",
                httpGetFile.getFullFileName(),
                Time.httpDate(lastModified),
                lastModified);
        }

        try {
            // Actually get the file from web server
            int httpResponseCode = httpGetFile.getFile();

            // If got a new file (instead of getting a NOT MODIFIED
            // response) then send message to those monitoring so that
            // the GTFS file can be processed.
            if (httpResponseCode == HttpStatus.SC_OK) {
                if (fileAlreadyExists)
                    logger.info(
                        "Got remote file because version on web server " + "is newer. Url={} dir={}",
                        httpGetFile.getFullFileName(),
                        gtfsConfig.getAutoUpdate().getDirName());
                else
                    logger.info(
                        "Got remote file because didn't have a local " + "copy of it. Url={} dir={}",
                        httpGetFile.getFullFileName(),
                        gtfsConfig.getAutoUpdate().getDirName());

                // Make copy of GTFS zip file in separate directory for archival
                archive(httpGetFile.getFullFileName());
            } else if (httpResponseCode == HttpStatus.SC_NOT_MODIFIED) {
                // If not modified then don't need to do anything
                logger.info(
                    "Remote GTFS file {} not updated (got "
                        + "HTTP NOT_MODIFIED status 304) since the local "
                        + "one  at {} has last modified date of {}",
                    gtfsConfig.getAutoUpdate().getUrl(),
                    httpGetFile.getFullFileName(),
                    new Date(file.lastModified()));
            } else {
                // Got unexpected response so log issue
                logger.error(
                    "Error retrieving remote GTFS file {} . Http " + "response code={}",
                    gtfsConfig.getAutoUpdate().getUrl(),
                    httpResponseCode);
            }
        } catch (IOException e) {
            logger.error("Error retrieving {} . {}", GtfsConfig.url.getValue(), e.getMessage());
        }
    }

    @Override
    public ExecutionType getExecutionType() {
        return ExecutionType.FIXED_RATE;
    }

    @Override
    public int executionPeriod() {
        return Math.toIntExact(GtfsConfig.intervalMsec.getValue());
    }
}
