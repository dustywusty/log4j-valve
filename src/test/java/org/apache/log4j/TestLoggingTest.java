package org.apache.log4j;

import org.junit.Test;

public class TestLoggingTest {

    @Test
    public void testInfoLoggingToConsole() {
        TestLogging.main(new String[] { "info", "here is the message" });
    }

    @Test
    public void testInfoLoggingToConsole_Exception() {
        TestLogging.main(new String[] { "info", "test message", "exception message" });
    }
}
