package org.apache.log4j.net;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class ScribeAppenderTest {

    private ScribeAppender appender;

    @Before
    public void before() {
        appender = new ScribeAppender();
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

    private void validateConfiguration(final String category, final String remoteHost, final int remotePort,
            final String localHostname) {

        Assert.assertEquals(category, appender.getCategory());
        Assert.assertEquals(remoteHost, appender.getRemoteHost());
        Assert.assertEquals(remotePort, appender.getRemotePort());
        Assert.assertEquals(localHostname, appender.getLocalHostname());
    }
}
