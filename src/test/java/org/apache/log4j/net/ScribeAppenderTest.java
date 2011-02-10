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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

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

        append("message");

        // there should be nothing running on port 0 anyways
        appender.setRemotePort(0);

        Mockito.verify(mockErrorHandler).error("DROP - no connection: [] INFO - message");
        Mockito.verify(mockErrorHandler).error(Matchers.contains("Connection refused"));
    }

    @Test
    public void testDefaultConfiguration() {
        validateConfiguration("default", "127.0.0.1", 1463, null);
    }

    @Test
    public void testLogEvent() throws IOException, TException {

        startService();

        setupLogMock("default", "test message");

        append("test message");

        stopService();
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

    private void setupLogMock(final String category, final String message) throws TException {
        setupLogMock(category, message, ResultCode.OK);
    }

    private void setupLogMock(final String category, final String message, final ResultCode resultCode)
            throws TException {

        Mockito.when(mockScribeIface.Log(Matchers.argThat(new LogEntryMatcher(category, message)))).thenReturn(
                resultCode);
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
            final String localHostname) {

        Assert.assertEquals(category, appender.getCategory());
        Assert.assertEquals(remoteHost, appender.getRemoteHost());
        Assert.assertEquals(remotePort, appender.getRemotePort());
        Assert.assertEquals(localHostname, appender.getLocalHostname());
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
