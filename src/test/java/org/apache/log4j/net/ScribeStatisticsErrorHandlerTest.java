package org.apache.log4j.net;

import org.apache.log4j.net.ScribeAppender.ERROR;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ScribeStatisticsErrorHandlerTest {

    private ScribeStatisticsErrorHandler errorHandler;

    @Before
    public void before() {
        errorHandler = new ScribeStatisticsErrorHandler();
    }

    @Test
    public void testErrorCounts() {
        errors(ERROR.GENERAL, 10);
        errors(ERROR.DROP_NO_CONNECTION, 100);
        errors(ERROR.DROP_TRY_LATER, 5);

        validateError(ERROR.GENERAL, 10);
        validateError(ERROR.DROP_NO_CONNECTION, 100);
        validateError(ERROR.DROP_TRY_LATER, 5);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testErrorHandlerMethodNotImplemented() {
        errorHandler.error("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidErrorCode_TooHigh() {
        errorHandler.getErrorCount(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidErrorCode_TooLow() {
        errorHandler.getErrorCount(3);
    }

    private void errors(final ERROR error, final int n) {

        for (int i = 0; i < n; i++) {
            errorHandler.error("", null, error.ordinal());
        }
    }

    private void validateError(final ERROR error, final long n) {

        Assert.assertEquals(n, errorHandler.getErrorCount(error));
        Assert.assertEquals(n, errorHandler.getErrorCount(error.ordinal()));
        Assert.assertEquals((Long) n, errorHandler.getErrorCounts().get(error));
    }
}
