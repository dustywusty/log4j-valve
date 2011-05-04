package org.apache.log4j.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.net.ScribeAppender.ERROR;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A simple {@link ErrorHandler} implementation that records and exposes only statistics about errors. This is useful
 * during load testing as a means to gather details of any failed appends. Implementation is thread safe and can be used
 * by multiple threads concurrently.
 * 
 * @author Josh Devins
 */
public class ScribeStatisticsErrorHandler implements ErrorHandler {

    private final Map<Integer, AtomicLong> errorCounts;

    public ScribeStatisticsErrorHandler() {

        // initialize counter
        ERROR[] errors = ERROR.values();
        errorCounts = new ConcurrentHashMap<Integer, AtomicLong>(errors.length);

        for (ERROR error : errors) {
            errorCounts.put(error.ordinal(), new AtomicLong(0L));
        }
    }

    /**
     * No-op.
     */
    @Override
    public void activateOptions() {
    }

    /**
     * Not implemented.
     */
    @Override
    public void error(final String message) {
        throw new UnsupportedOperationException("Not implemented, use error(String, Exception, int)");
    }

    /**
     * Records the number of errors given the enumeration key {@link ScribeAppender#ERROR}.
     */
    @Override
    public void error(final String message, final Exception e, final int errorCode) {

        validateErrorCode(errorCode);
        errorCounts.get(errorCode).incrementAndGet();
    }

    /**
     * Delegated to {@link #error(String, Exception, int))}
     */
    @Override
    public void error(final String message, final Exception e, final int errorCode, final LoggingEvent event) {
        error(message, e, errorCode);
    }

    /**
     * Retruns the error count for the given error.
     */
    public long getErrorCount(final ERROR error) {
        return getErrorCount(error.ordinal());
    }

    /**
     * Retruns the error count for the given error code.
     */
    public long getErrorCount(final int errorCode) {

        validateErrorCode(errorCode);
        return errorCounts.get(errorCode).longValue();
    }

    /**
     * Returns a new map of error counts as a best effort, copied representation of the current internal state/counts.
     */
    public Map<ERROR, Long> getErrorCounts() {

        ERROR[] errors = ERROR.values();
        Map<ERROR, Long> rtn = new HashMap<ScribeAppender.ERROR, Long>(errors.length);
        for (ERROR error : errors) {
            rtn.put(error, errorCounts.get(error.ordinal()).longValue());
        }

        return rtn;
    }

    /**
     * No-op.
     */
    @Override
    public void setAppender(final Appender appender) {
    }

    /**
     * No-op.
     */
    @Override
    public void setBackupAppender(final Appender appender) {
    }

    /**
     * No-op.
     */
    @Override
    public void setLogger(final Logger logger) {
    }

    private void validateErrorCode(final int errorCode) {

        if (errorCode < 0 || errorCode >= ERROR.values().length) {
            throw new IllegalArgumentException("Invalid error code: " + errorCode);
        }
    }

}
