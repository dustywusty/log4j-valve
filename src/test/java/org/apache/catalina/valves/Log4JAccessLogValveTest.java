package org.apache.catalina.valves;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class Log4JAccessLogValveTest {

    private Log4JAccessLogValve valve;

    @After
    public void after() throws Exception {
        valve.stop();
    }

    @Before
    public void before() throws Exception {
        valve = new Log4JAccessLogValve();
        valve.setLoggerName("access");
        valve.setPattern("common");
        valve.start();
    }

    @Test
    public void testInvoke() throws Exception {

        Request request = Mockito.mock(Request.class);
        Response response = Mockito.mock(Response.class);

        Mockito.when(request.getRemoteHost()).thenReturn("remoteHost");
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getRequestURI()).thenReturn("/foo/bar");
        Mockito.when(request.getProtocol()).thenReturn("HTTP/1.1");

        Mockito.when(response.getStatus()).thenReturn(200);

        valve.invoke(request, response);
    }

    @Test
    public void testLogOnly() throws Exception {
        valve.log("test log message");
    }
}
