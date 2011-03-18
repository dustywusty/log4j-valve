package org.apache.catalina.valves;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.log4j.Logger;
import org.omg.PortableInterceptor.RequestInfo;

/**
 * An implementation of a Tomcat access logging valve that uses log4j instead of writing straight to file. This will
 * allow us to send access logging over Scribe using the {@link org.apache.log4j.net.ScribeAppender}. This will log to
 * log4j at the INFO level. Note that thread name field is currently not supported. The logger's name is set using a
 * property of the valve.
 * 
 * @see AccessLogValve AccessLogValve for details on formatting
 * 
 * @author Josh Devins
 */
public final class Log4JAccessLogValve extends BaseValve {

    /**
     * AccessLogElement writes the partial message into the buffer.
     */
    protected interface AccessLogElement {

        public void addElement(StringBuffer buf, Date date, Request request, Response response, long time);
    }

    /**
     * write bytes sent, excluding HTTP headers - %b, %B
     */
    protected class ByteSentElement implements AccessLogElement {

        private final boolean conversion;

        /**
         * if conversion is true, write '-' instead of 0 - %b
         */
        public ByteSentElement(final boolean conversion) {
            this.conversion = conversion;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            long length = response.getContentCountLong();
            if (length <= 0 && conversion) {
                buf.append('-');
            } else {
                buf.append(length);
            }
        }
    }

    /**
     * write a specific cookie - %{xxx}c
     */
    protected class CookieElement implements AccessLogElement {

        private final String header;

        public CookieElement(final String header) {
            this.header = header;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            String value = "-";
            Cookie[] c = request.getCookies();
            if (c != null) {
                for (Cookie element : c) {
                    if (header.equals(element.getName())) {
                        value = element.getValue();
                        break;
                    }
                }
            }
            buf.append(value);
        }
    }

    /**
     * write date and time, in Common Log Format - %t
     */
    protected class DateAndTimeElement implements AccessLogElement {

        private Date currentDate = new Date(0);

        private String currentDateString = null;

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (currentDate != date) {
                synchronized (this) {
                    if (currentDate != date) {
                        StringBuffer current = new StringBuffer(32);
                        current.append('[');
                        current.append(dayFormatter.format(date)); // Day
                        current.append('/');
                        current.append(lookup(monthFormatter.format(date))); // Month
                        current.append('/');
                        current.append(yearFormatter.format(date)); // Year
                        current.append(':');
                        current.append(timeFormatter.format(date)); // Time
                        current.append(' ');
                        current.append(getTimeZone(date)); // Timezone
                        current.append(']');
                        currentDateString = current.toString();
                        currentDate = date;
                    }
                }
            }
            buf.append(currentDateString);
        }
    }

    /**
     * write time taken to process the request - %D, %T
     */
    protected class ElapsedTimeElement implements AccessLogElement {

        private final boolean millis;

        /**
         * if millis is true, write time in millis - %D
         * if millis is false, write time in seconds - %T
         */
        public ElapsedTimeElement(final boolean millis) {
            this.millis = millis;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (millis) {
                buf.append(time);
            } else {
                // second
                buf.append(time / 1000);
                buf.append('.');
                int remains = (int) (time % 1000);
                buf.append(remains / 100);
                remains = remains % 100;
                buf.append(remains / 10);
                buf.append(remains % 10);
            }
        }
    }

    /**
     * write incoming headers - %{xxx}i
     */
    protected class HeaderElement implements AccessLogElement {

        private final String header;

        public HeaderElement(final String header) {
            this.header = header;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            String value = request.getHeader(header);
            if (value == null) {
                buf.append('-');
            } else {
                buf.append(value);
            }
        }
    }

    /**
     * write remote host name - %h
     */
    protected class HostElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(request.getRemoteHost());
        }
    }

    /**
     * write HTTP status code of the response - %s
     */
    protected class HttpStatusCodeElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (response != null) {
                buf.append(response.getStatus());
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * write local IP address - %A
     */
    protected class LocalAddrElement implements AccessLogElement {

        private String value = null;

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (value == null) {
                synchronized (this) {
                    try {
                        value = InetAddress.getLocalHost().getHostAddress();
                    } catch (Throwable e) {
                        value = "127.0.0.1";
                    }
                }
            }
            buf.append(value);
        }
    }

    /**
     * write local port on which this request was received - %p
     */
    protected class LocalPortElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(request.getServerPort());
        }
    }

    /**
     * write local server name - %v
     */
    protected class LocalServerNameElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(request.getServerName());
        }
    }

    /**
     * write remote logical username from identd (always returns '-') - %l
     */
    protected class LogicalUserNameElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append('-');
        }
    }

    /**
     * write request method (GET, POST, etc.) - %m
     */
    protected class MethodElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (request != null) {
                buf.append(request.getMethod());
            }
        }
    }

    /**
     * write request protocol - %H
     */
    protected class ProtocolElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(request.getProtocol());
        }
    }

    /**
     * write Query string (prepended with a '?' if it exists) - %q
     */
    protected class QueryElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            String query = null;
            if (request != null) {
                query = request.getQueryString();
            }
            if (query != null) {
                buf.append('?');
                buf.append(query);
            }
        }
    }

    /**
     * write remote IP address - %a
     */
    protected class RemoteAddrElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(request.getRemoteAddr());
        }
    }

    /**
     * write an attribute in the ServletRequest - %{xxx}r
     */
    protected class RequestAttributeElement implements AccessLogElement {

        private final String header;

        public RequestAttributeElement(final String header) {
            this.header = header;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            Object value = null;
            if (request != null) {
                value = request.getAttribute(header);
            } else {
                value = "??";
            }
            if (value != null) {
                if (value instanceof String) {
                    buf.append((String) value);
                } else {
                    buf.append(value.toString());
                }
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * write first line of the request (method and request URI) - %r
     */
    protected class RequestElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (request != null) {
                buf.append(request.getMethod());
                buf.append(' ');
                buf.append(request.getRequestURI());
                if (request.getQueryString() != null) {
                    buf.append('?');
                    buf.append(request.getQueryString());
                }
                buf.append(' ');
                buf.append(request.getProtocol());
            } else {
                buf.append("- - ");
            }
        }
    }

    /**
     * write requested URL path - %U
     */
    protected class RequestURIElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (request != null) {
                buf.append(request.getRequestURI());
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * write a specific response header - %{xxx}o
     */
    protected class ResponseHeaderElement implements AccessLogElement {

        private final String header;

        public ResponseHeaderElement(final String header) {
            this.header = header;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (null != response) {
                String[] values = response.getHeaderValues(header);
                if (values.length > 0) {
                    for (int i = 0; i < values.length; i++) {
                        String string = values[i];
                        buf.append(string);
                        if (i + 1 < values.length) {
                            buf.append(",");
                        }
                    }
                    return;
                }
            }
            buf.append("-");
        }
    }

    /**
     * write an attribute in the HttpSession - %{xxx}s
     */
    protected class SessionAttributeElement implements AccessLogElement {

        private final String header;

        public SessionAttributeElement(final String header) {
            this.header = header;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            Object value = null;
            if (null != request) {
                HttpSession sess = request.getSession(false);
                if (null != sess) {
                    value = sess.getAttribute(header);
                }
            } else {
                value = "??";
            }
            if (value != null) {
                if (value instanceof String) {
                    buf.append((String) value);
                } else {
                    buf.append(value.toString());
                }
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * write user session ID - %S
     */
    protected class SessionIdElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (request != null) {
                if (request.getSession(false) != null) {
                    buf.append(request.getSessionInternal(false).getIdInternal());
                } else {
                    buf.append('-');
                }
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * write any string
     */
    protected class StringElement implements AccessLogElement {

        private final String str;

        public StringElement(final String str) {
            this.str = str;
        }

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            buf.append(str);
        }
    }

    /**
     * write thread name - %I
     */
    protected class ThreadNameElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            RequestInfo info = (RequestInfo) request.getCoyoteRequest().getRequestProcessor();
            if (info != null) {
                buf.append(((org.apache.coyote.RequestInfo) info).getWorkerThreadName());
            } else {
                buf.append("-");
            }
        }
    }

    /**
     * write remote user that was authenticated (if any), else '-' - %u
     */
    protected class UserElement implements AccessLogElement {

        public void addElement(final StringBuffer buf, final Date date, final Request request, final Response response,
                final long time) {
            if (request != null) {
                String value = request.getRemoteUser();
                if (value != null) {
                    buf.append(value);
                } else {
                    buf.append('-');
                }
            } else {
                buf.append('-');
            }
        }
    }

    /**
     * The set of month abbreviations for log messages.
     */
    protected static final String months[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
            "Nov", "Dec" };

    /**
     * The system timezone.
     */
    private TimeZone timezone = null;

    /**
     * The time zone offset relative to GMT in text form when daylight saving
     * is not in operation.
     */
    private String timeZoneNoDST = null;

    /**
     * The time zone offset relative to GMT in text form when daylight saving
     * is in operation.
     */
    private String timeZoneDST = null;

    /**
     * A date formatter to format Dates into a day string in the format
     * "dd".
     */
    private SimpleDateFormat dayFormatter = null;

    /**
     * A date formatter to format a Date into a month string in the format
     * "MM".
     */
    private SimpleDateFormat monthFormatter = null;

    /**
     * A date formatter to format a Date into a year string in the format
     * "yyyy".
     */
    private SimpleDateFormat yearFormatter = null;

    /**
     * A date formatter to format a Date into a time in the format
     * "kk:mm:ss" (kk is a 24-hour representation of the hour).
     */
    private SimpleDateFormat timeFormatter = null;

    /**
     * The pattern used to format our access log lines.
     */
    protected String pattern = null;

    /**
     * enabled this component
     */
    protected boolean enabled = true;

    private Logger logger;

    /**
     * Array of AccessLogElement, they will be used to make log message.
     */
    protected AccessLogElement[] logElements = null;

    /**
     * Are we doing conditional logging. default false.
     */
    protected String condition = null;

    /**
     * The system time when we last updated the Date that this valve
     * uses for log lines.
     */
    private Date currentDate = null;

    private long currentMillis = 0;

    private String loggerName;

    /**
     * Return whether the attribute name to look for when
     * performing conditional loggging. If null, every
     * request is logged.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * @return Returns the enabled.
     */
    public boolean getEnabled() {
        return enabled;
    }

    public String getLoggerName() {
        return loggerName;
    }

    /**
     * Return the format pattern.
     */
    public String getPattern() {
        return this.pattern;
    }

    /**
     * Log a message summarizing the specified request and response, according
     * to the format specified by the <code>pattern</code> property.
     * 
     * @param request
     *        Request being processed
     * @param response
     *        Response being processed
     * 
     * @exception IOException
     *            if an input/output error has occurred
     * @exception ServletException
     *            if a servlet error has occurred
     */
    @Override
    public void invoke(final Request request, final Response response) throws IOException, ServletException {

        if (started && getEnabled()) {

            // Pass this request on to the next valve in our pipeline
            long t1 = System.currentTimeMillis();

            if (getNext() != null) {
                getNext().invoke(request, response);
            }

            long t2 = System.currentTimeMillis();
            long time = t2 - t1;

            if (logElements == null || condition != null && null != request.getRequest().getAttribute(condition)) {
                return;
            }

            Date date = getDate();
            StringBuffer result = new StringBuffer();

            for (AccessLogElement logElement : logElements) {
                logElement.addElement(result, date, request, response, time);
            }

            log(result.toString());

        } else {

            if (getNext() != null) {
                getNext().invoke(request, response);
            }
        }
    }

    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     * 
     * @param condition
     *        Set to null to log everything
     */
    public void setCondition(final String condition) {
        this.condition = condition;
    }

    /**
     * @param enabled
     *        The enabled to set.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setLoggerName(final String loggerName) {
        this.loggerName = loggerName;
    }

    /**
     * Set the format pattern, first translating any recognized alias.
     * 
     * @param pattern
     *        The new pattern
     */
    public void setPattern(String pattern) {
        if (pattern == null) {
            pattern = "";
        }
        if (pattern.equals(Constants.AccessLog.COMMON_ALIAS)) {
            pattern = Constants.AccessLog.COMMON_PATTERN;
        }
        if (pattern.equals(Constants.AccessLog.COMBINED_ALIAS)) {
            pattern = Constants.AccessLog.COMBINED_PATTERN;
        }
        this.pattern = pattern;
        logElements = createLogElements();
    }

    @Override
    protected void afterStart() throws LifecycleException {

        // Initialize the timeZone, Date formatters, and currentDate
        timezone = TimeZone.getDefault();
        timeZoneNoDST = calculateTimeZoneOffset(timezone.getRawOffset());

        int offset = timezone.getDSTSavings();
        timeZoneDST = calculateTimeZoneOffset(timezone.getRawOffset() + offset);

        dayFormatter = new SimpleDateFormat("dd");
        dayFormatter.setTimeZone(timezone);
        monthFormatter = new SimpleDateFormat("MM");
        monthFormatter.setTimeZone(timezone);
        yearFormatter = new SimpleDateFormat("yyyy");
        yearFormatter.setTimeZone(timezone);
        timeFormatter = new SimpleDateFormat("HH:mm:ss");
        timeFormatter.setTimeZone(timezone);

        currentDate = new Date();

        // create the logger
        if (loggerName == null || loggerName.length() == 0) {
            throw new LifecycleException("Log4jAccessLogValve: no logger name set");
        }

        logger = Logger.getLogger(loggerName);
    }

    /**
     * parse pattern string and create the array of AccessLogElement
     */
    protected AccessLogElement[] createLogElements() {

        List<AccessLogElement> list = new ArrayList<AccessLogElement>();
        boolean replace = false;
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (replace) {
                /*
                 * For code that processes {, the behavior will be ... if I do
                 * not enounter a closing } - then I ignore the {
                 */
                if ('{' == ch) {
                    StringBuffer name = new StringBuffer();
                    int j = i + 1;
                    for (; j < pattern.length() && '}' != pattern.charAt(j); j++) {
                        name.append(pattern.charAt(j));
                    }
                    if (j + 1 < pattern.length()) {
                        /* the +1 was to account for } which we increment now */
                        j++;
                        list.add(createAccessLogElement(name.toString(), pattern.charAt(j)));
                        i = j; /* Since we walked more than one character */
                    } else {
                        // D'oh - end of string - pretend we never did this
                        // and do processing the "old way"
                        list.add(createAccessLogElement(ch));
                    }
                } else {
                    list.add(createAccessLogElement(ch));
                }
                replace = false;
            } else if (ch == '%') {
                replace = true;
                list.add(new StringElement(buf.toString()));
                buf = new StringBuffer();
            } else {
                buf.append(ch);
            }
        }

        if (buf.length() > 0) {
            list.add(new StringElement(buf.toString()));
        }

        return list.toArray(new AccessLogElement[0]);
    }

    /**
     * Log the specified message to log4j at the INFO level.
     * 
     * @param message
     *        Message to be logged
     */
    protected void log(final String message) {

        if (logger.isInfoEnabled()) {
            logger.info(message);
        }
    }

    private String calculateTimeZoneOffset(long offset) {
        StringBuffer tz = new StringBuffer();
        if (offset < 0) {
            tz.append("-");
            offset = -offset;
        } else {
            tz.append("+");
        }

        long hourOffset = offset / (1000 * 60 * 60);
        long minuteOffset = offset / (1000 * 60) % 60;

        if (hourOffset < 10) {
            tz.append("0");
        }
        tz.append(hourOffset);

        if (minuteOffset < 10) {
            tz.append("0");
        }
        tz.append(minuteOffset);

        return tz.toString();
    }

    /**
     * create an AccessLogElement implementation
     */
    private AccessLogElement createAccessLogElement(final char pattern) {
        switch(pattern) {
            case 'a' :
                return new RemoteAddrElement();
            case 'A' :
                return new LocalAddrElement();
            case 'b' :
                return new ByteSentElement(true);
            case 'B' :
                return new ByteSentElement(false);
            case 'D' :
                return new ElapsedTimeElement(true);
            case 'h' :
                return new HostElement();
            case 'H' :
                return new ProtocolElement();
            case 'l' :
                return new LogicalUserNameElement();
            case 'm' :
                return new MethodElement();
            case 'p' :
                return new LocalPortElement();
            case 'q' :
                return new QueryElement();
            case 'r' :
                return new RequestElement();
            case 's' :
                return new HttpStatusCodeElement();
            case 'S' :
                return new SessionIdElement();
            case 't' :
                return new DateAndTimeElement();
            case 'T' :
                return new ElapsedTimeElement(false);
            case 'u' :
                return new UserElement();
            case 'U' :
                return new RequestURIElement();
            case 'v' :
                return new LocalServerNameElement();
            case 'I' :
                return new ThreadNameElement();
            default:
                return new StringElement("???" + pattern + "???");
        }
    }

    /**
     * create an AccessLogElement implementation which needs header string
     */
    private AccessLogElement createAccessLogElement(final String header, final char pattern) {
        switch(pattern) {
            case 'i' :
                return new HeaderElement(header);
            case 'c' :
                return new CookieElement(header);
            case 'o' :
                return new ResponseHeaderElement(header);
            case 'r' :
                return new RequestAttributeElement(header);
            case 's' :
                return new SessionAttributeElement(header);
            default:
                return new StringElement("???");
        }
    }

    /**
     * This method returns a Date object that is accurate to within one second.
     * If a thread calls this method to get a Date and it's been less than 1
     * second since a new Date was created, this method simply gives out the
     * same Date again so that the system doesn't spend time creating Date
     * objects unnecessarily.
     * 
     * @return Date
     */
    private Date getDate() {

        // Only create a new Date once per second, max.
        long systime = System.currentTimeMillis();
        if (systime - currentMillis > 1000) {
            synchronized (this) {
                if (systime - currentMillis > 1000) {
                    currentDate = new Date(systime);
                    currentMillis = systime;
                }
            }
        }

        return currentDate;
    }

    private String getTimeZone(final Date date) {

        if (timezone.inDaylightTime(date)) {
            return timeZoneDST;
        }

        return timeZoneNoDST;
    }

    /**
     * Return the month abbreviation for the specified month, which must
     * be a two-digit String.
     * 
     * @param month
     *        Month number ("01" .. "12").
     */
    private String lookup(final String month) {
        int index;
        try {
            index = Integer.parseInt(month) - 1;
        } catch (Throwable t) {
            index = 0; // Can not happen, in theory
        }
        return months[index];
    }
}
