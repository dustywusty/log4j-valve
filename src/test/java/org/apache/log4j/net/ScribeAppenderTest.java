package org.apache.log4j.net;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ScribeAppenderTest {

    private static final Logger LOGGER = Logger.getLogger(ScribeAppenderTest.class);

    private ScribeAppender appender;

    private ErrorHandler mockErrorHandler;

    @Before
    public void before() {
        appender = new ScribeAppender();

        appender.setName("scribe");
        appender.setThreshold(Level.DEBUG);
        appender.setLayout(new PatternLayout("%p - %m"));

        mockErrorHandler = Mockito.mock(ErrorHandler.class);
        appender.setErrorHandler(mockErrorHandler);
    }

    @Test
    public void testAppend() {

        append("message");

        Mockito.verify(mockErrorHandler).error("DROP - no connection: [] INFO - message");
    }

    @Test
    public void testDefaultConfiguration() {
        validateConfiguration("default", "127.0.0.1", 1463, null);
    }

    @Test
    public void testSettingConfiguration() {

        appender.setCategory("category");
        appender.setRemoteHost("remoteHost");
        appender.setRemotePort(1);
        appender.setLocalHostname("localHostname");

        validateConfiguration("category", "remoteHost", 1, "localHostname");
    }

    private void append(final String message) {
        append(message, null);
    }

    private void append(final String message, final Throwable throwable) {

        LoggingEvent event = new LoggingEvent("org.apache.log4j.net.ScribeAppenderTest", LOGGER, Level.INFO, message,
                throwable);
        appender.append(event);
    }

    private void validateConfiguration(final String category, final String remoteHost, final int remotePort,
            final String localHostname) {

        Assert.assertEquals(category, appender.getCategory());
        Assert.assertEquals(remoteHost, appender.getRemoteHost());
        Assert.assertEquals(remotePort, appender.getRemotePort());
        Assert.assertEquals(localHostname, appender.getLocalHostname());
    }
}
