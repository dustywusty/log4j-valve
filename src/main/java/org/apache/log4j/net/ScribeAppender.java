/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import com.facebook.scribe.thrift.LogEntry;
import com.facebook.scribe.thrift.ResultCode;
import com.facebook.scribe.thrift.scribe.Client;

/**
 * A basic log4j appender for sending log messages to a remote Scribe instance. The logic in
 * {@link #append(LoggingEvent)} will drop any log events that fail to be sent to Scribe. All failures are handled by
 * log4j error handling mechanism which should log to the backup appender if defined or STDERR if there is no backup
 * appender.
 * 
 * <p>
 * This is based on previous Scribe appenders as well as the built-in log4j appenders like {@link JMSAppender} and
 * {@link SyslogAppender}.
 * </p>
 * 
 * @see http://github.com/alexlod/scribe-log4j-appender
 * @see http://github.com/lenn0x/Scribe-log4j-Appender
 * 
 * @author Josh Devins
 */
public class ScribeAppender extends AppenderSkeleton {

    public static enum ERROR {
        GENERAL, DROP_NO_CONNECTION, DROP_TRY_LATER;
    }

    public static final String DEFAULT_REMOTE_HOST = "127.0.0.1";

    public static final int DEFAULT_REMOTE_PORT = 1463;

    private static final String DEFAULT_CATEGORY = "default";

    private static final int DEFAULT_STACK_TRACE_DEPTH = 1;

    private String remoteHost = DEFAULT_REMOTE_HOST;

    private int remotePort = DEFAULT_REMOTE_PORT;

    private String category = DEFAULT_CATEGORY;

    private String localHostname;

    private int stackTraceDepth = DEFAULT_STACK_TRACE_DEPTH;

    private Client client;

    private TFramedTransport transport;

    /**
     * Delegates to {@link #appendAndGetError(LoggingEvent)}
     */
    @Override
    public synchronized void append(final LoggingEvent event) {
        appendAndGetError(event);
    }

    /**
     * Appends a log message to remote Scribe server. This is currently made thread safe by synchronizing this method,
     * however this is not very efficient and should be refactored. Method will return null if no errors ocurred,
     * otherwise the specific error will be returned.
     * 
     * TODO: Refactor for better effeciency and thread safety
     */
    public ERROR appendAndGetError(final LoggingEvent event) {

        String message = buildMessage(event);

        boolean connected = connectIfNeeded();

        if (!connected) {
            getErrorHandler().error("DROP - no connection: " + message, null, ERROR.DROP_NO_CONNECTION.ordinal());
            return ERROR.DROP_NO_CONNECTION;
        }

        ERROR error = null;
        try {
            // log it to the client
            List<LogEntry> logEntries = new ArrayList<LogEntry>(1);
            logEntries.add(new LogEntry(category, message));

            ResultCode resultCode = client.Log(logEntries);

            // drop the message if Scribe can't handle it, this should end up in the backup appender
            if (ResultCode.TRY_LATER == resultCode) {

                // nicely formatted for batch processing
                getErrorHandler().error("DROP - TRY_LATER: " + message, null, ERROR.DROP_TRY_LATER.ordinal());
                error = ERROR.DROP_TRY_LATER;
            }

        } catch (TException e) {
            transport.close();
            handleError("TException on log attempt", e);
            error = ERROR.GENERAL;

        } catch (Exception e) {
            handleError("Unhandled Exception on log attempt", e);
            error = ERROR.GENERAL;
        }

        return error;
    }

    /**
     * Close transport if open.
     */
    @Override
    public synchronized void close() {
        if (isConnected()) {
            transport.close();
        }
    }

    public String getCategory() {
        return category;
    }

    public String getLocalHostname() {
        return localHostname;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getStackTraceDepth() {
        return stackTraceDepth;
    }

    public synchronized boolean isConnected() {
        return transport != null && transport.isOpen();
    }

    public boolean requiresLayout() {
        return true;
    }

    public void setCategory(final String category) {
        Validate.notEmptyString(category, "Category must not be empty");
        this.category = category;
    }

    public void setLocalHostname(final String localHostname) {
        this.localHostname = localHostname;
    }

    public void setRemoteHost(final String remoteHost) {
        Validate.notEmptyString(remoteHost, "Remote host must not be empty");
        this.remoteHost = remoteHost;
    }

    public void setRemotePort(final int remotePort) {
        Validate.positiveInteger(remotePort, "Remote port must at least be a positive integer");
        this.remotePort = remotePort;
    }

    public void setStackTraceDepth(final int stackTraceDepth) {
        Validate.positiveInteger(stackTraceDepth, "Stack trace depth must be a positive integer");
        this.stackTraceDepth = stackTraceDepth;
    }

    private String buildMessage(final LoggingEvent event) {

        String stackTrace = null;
        if (event.getThrowableInformation() != null) {
            String[] stackTraceArray = event.getThrowableInformation().getThrowableStrRep();

            if (stackTraceArray != null && stackTraceArray.length > 0) {
                StringBuilder sb = new StringBuilder();

                // first n lines of stack trace only
                // recall that first is the root and depth refers to the cause
                for (int i = 0; i < stackTraceDepth + 1; i++) {
                    sb.append(stackTraceArray[i]);
                }

                stackTrace = sb.toString();
            }
        }

        findAndSetLocalHostnameIfNeeded();

        // build log message to send with or without stack trace
        if (stackTrace == null) {
            return String.format("[%s] %s", localHostname, layout.format(event), stackTrace);
        }

        return String.format("[%s] %s {%s}", localHostname, layout.format(event), stackTrace);
    }

    /**
     * Connect to Scribe if not open, reconnecting if a previous connection has failed.
     * 
     * @return connection success
     */
    private boolean connectIfNeeded() {
        if (isConnected()) {
            return true;
        }

        // connection was dropped, needs to be reopened
        if (transport != null && !transport.isOpen()) {
            transport.close();
        }

        try {
            establishConnection();
            return true;

        } catch (TTransportException e) {
            handleError("TTransportException on connect", e);

        } catch (UnknownHostException e) {
            handleError("UnknownHostException on connect", e);

        } catch (IOException e) {
            handleError("IOException on connect", e);

        } catch (Exception e) {
            handleError("Unhandled Exception on connect", e);
        }

        return false;
    }

    /**
     * Thrift boilerplate connection code. No error handling is attempted and all excetions are passed back up.
     */
    private void establishConnection() throws TTransportException, UnknownHostException, IOException {
        TSocket sock = new TSocket(new Socket(remoteHost, remotePort));
        transport = new TFramedTransport(sock);

        TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
        client = new Client(protocol, protocol);
    }

    /**
     * If no {@link #localHostname} has been set, this will attempt to set it.
     */
    private void findAndSetLocalHostnameIfNeeded() {
        if (localHostname == null) {
            try {
                localHostname = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                // can't get hostname
                localHostname = "";
            }
        }
    }

    private void handleError(final String failure, final Exception e) {
        // error code is not used
        getErrorHandler().error(
                "Failure in ScribeAppender: name=[" + name + "], failure=[" + failure + "], exception=["
                        + e.getMessage() + "]", null, ERROR.GENERAL.ordinal());
    }
}
