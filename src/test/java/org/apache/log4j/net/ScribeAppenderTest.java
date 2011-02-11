package org.apache.log4j.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.matchers.StartsWith;

import com.facebook.scribe.thrift.ResultCode;
import com.facebook.scribe.thrift.scribe;

/**
 * Tests for {@link ScribeAppender}.
 * 
 * @see <a href="http://jitr.org/xref/org/jitr/JitrUtils.html">Jitr</a> for random port finding method
 * 
 * @author Josh Devins
 */
public class ScribeAppenderTest {

    private static final Logger LOGGER = Logger.getLogger(ScribeAppenderTest.class);

    private ScribeAppender appender;

    private ErrorHandler mockErrorHandler;

    private scribe.Iface mockScribeIface;

    private TServer server;

    private Thread serverThread;

    @After
    public void after() {
        Mockito.verifyNoMoreInteractions(mockErrorHandler);
        Mockito.validateMockitoUsage();
    }

    @Before
    public void before() {
        appender = new ScribeAppender();

        appender.setName("scribe");
        appender.setThreshold(Level.DEBUG);
        appender.setLayout(new PatternLayout("%p - %m"));

        mockErrorHandler = Mockito.mock(ErrorHandler.class);
        appender.setErrorHandler(mockErrorHandler);

        mockScribeIface = Mockito.mock(scribe.Iface.class);
    }

    @Test
    public void testAppend_NoConnection() {

        // there should be nothing running on port 1 anyways
        appender.setRemotePort(1);
        appender.setLocalHostname("localHostname");

        append("test message");

        Mockito.verify(mockErrorHandler).error("DROP - no connection: [localHostname] INFO - test message");
        Mockito.verify(mockErrorHandler).error(Matchers.contains("Connection refused"));
    }

    @Test
    public void testDefaultConfiguration() {
        validateConfiguration("default", "127.0.0.1", 1463, null, 1);
    }

    @Test
    public void testRemoteAppend_Ok() throws IOException, TException {

        startService();

        setupScribeMock("test message");
        append("test message");

        stopService();
    }

    @Test
    public void testRemoteAppend_OkTwice() throws IOException, TException {

        startService();

        setupScribeMock("test message");
        append("test message");
        append("test message");

        stopService();
    }

    @Test
    public void testRemoteAppend_ReconnectionWithFailure() throws IOException, TException {

        // success on the first try
        testRemoteAppend_Ok();

        // fail on the second try
        appender.close();
        testAppend_NoConnection();

        // success again on the next try
        startService();
        append("test message");
        stopService();
    }

    @Test
    public void testRemoteAppend_TryLater() throws IOException, TException {

        startService();

        setupScribeMock("test message", ResultCode.TRY_LATER);
        append("test message");

        stopService();

        Mockito.verify(mockErrorHandler).error("DROP - TRY_LATER: [localHostname] INFO - test message");
    }

    @Test
    public void testRemoteAppend_WithException() throws IOException, TException {

        startService();

        setupScribeMock(
                "default",
                new StartsWith(
                        "[localHostname] INFO - test message {java.lang.Exception: test exception message\tat org.apache.log4j.net.ScribeAppenderTest.testRemoteAppend_WithException"),
                ResultCode.OK);

        append("test message", new Exception("test exception message"));

        stopService();
    }

    @Test
    public void testSettingConfiguration() {

        appender.setCategory("category");
        appender.setRemoteHost("remoteHost");
        appender.setRemotePort(1);
        appender.setLocalHostname("localHostname");
        appender.setStackTraceDepth(0);

        validateConfiguration("category", "remoteHost", 1, "localHostname", 0);
    }

    private void append(final String message) {
        append(message, null);
    }

    private void append(final String message, final Throwable throwable) {

        LoggingEvent event = new LoggingEvent("org.apache.log4j.net.ScribeAppenderTest", LOGGER, Level.INFO, message,
                throwable);
        appender.append(event);
    }

    private void setupScribeMock(final String message) throws TException {
        setupScribeMock(message, ResultCode.OK);
    }

    private void setupScribeMock(final String category, final Matcher<String> messageMatcher,
            final ResultCode resultCode) throws TException {

        appender.setLocalHostname("localHostname");

        Mockito.when(mockScribeIface.Log(Matchers.argThat(new LogEntryMatcher(category, messageMatcher)))).thenReturn(
                resultCode);
    }

    private void setupScribeMock(final String message, final ResultCode resultCode) throws TException {
        setupScribeMock("default", new IsEqual<String>("[localHostname] INFO - " + message), resultCode);
    }

    private void startService() throws TTransportException, IOException {

        int port = getRandomUnusedPort();

        // basic connection
        TServerTransport serverTransport = new TServerSocket(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),
                port));
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);

        // protocol and transport
        args.protocolFactory(new TBinaryProtocol.Factory());
        args.transportFactory(new TFramedTransport.Factory());

        // inject mock processor
        scribe.Processor scribeProcessor = new scribe.Processor(mockScribeIface);
        args.processor(scribeProcessor);

        server = new TThreadPoolServer(args);

        // simple spawn new thread to run server in
        // only caveat here is we miss any exceptions thrown from the invocation of server.serve()
        serverThread = new Thread() {

            @Override
            public void run() {
                server.serve();
            }
        };

        serverThread.start();

        appender.setRemotePort(port);
    }

    private void stopService() {
        server.stop();
    }

    private void validateConfiguration(final String category, final String remoteHost, final int remotePort,
            final String localHostname, final int stackTraceDepth) {

        Assert.assertEquals(category, appender.getCategory());
        Assert.assertEquals(remoteHost, appender.getRemoteHost());
        Assert.assertEquals(remotePort, appender.getRemotePort());
        Assert.assertEquals(localHostname, appender.getLocalHostname());
        Assert.assertEquals(stackTraceDepth, appender.getStackTraceDepth());
    }

    /**
     * Provides a quick way to get a random, unused port by opening a {@link ServerSocket} and
     * getting the locally assigned port for the server socket.
     */
    public static int getRandomUnusedPort() throws IOException {

        final int port;
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            port = socket.getLocalPort();

        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return port;
    }
}
