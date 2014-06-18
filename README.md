Log4j Valve


History
---

[Alex Loddengaard](http://github.com/alexlod/scribe-log4j-appender)

 * original author

[Chris Goffinet](http://github.com/lenn0x/Scribe-log4j-Appender)

 * cleaned up code to work in non-Hadoop environments
 * added support to reconnect if it loses a connection or if Scribe goes away

[Josh Devins](http://github.com/joshdevins/Scribe-log4j-Appender)

 * reorganized and refactored
 * removed checked in libs and provided basic Maven pom
 * regenerated Thrift classes (just in case)
 * added better error reporting through log4j
 * added test cases
 * added Tomcat log4j access log valve

[Dustin Clark](https://github.com/clarkda/log4j-valve)
 
 * Stripped out scribe functionality
 * Project now provides ONLY log4j access logging for Tomcat 7
 * Updated Josh's code for the new lifecycle / catalina APIs
 * Stripped out maven in favor of gradle build support
 
Configuration
---

    log4j.rootLogger=INFO, stdout, scribe
	
    # stdout/console appender
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%5p [%t] (%F:%L) - %m%n

    # Scribe appender
    log4j.appender.scribe=org.apache.log4j.net.ScribeAppender

    # error handling appender - all errors and dropped messages are sent to this appender
    log4j.appender.scribe.ErrorHandler.appender-ref=stdout

    # do NOT use a trailing %n unless you want a newline to be transmitted to Scribe after every message
    # of course, this depends on your Scribe configuration as well, if you are having it append newline or not
    log4j.appender.scribe.layout=org.apache.log4j.PatternLayout
    log4j.appender.scribe.layout.ConversionPattern=%d{ISO8601} %m%n

    # optional properties
    # canonical hostname will be looked up if not provided in localHostname
    log4j.appender.scribe.category=application.appender.category
    log4j.appender.scribe.remoteHost=127.0.0.1
    log4j.appender.scribe.remotePort=1463
    log4j.appender.scribe.localHostname=app01.host.com
    log4j.appender.scribe.stackTraceDepth=1

Tomcat Logging
---

Logging to log4j in Tomcat requires a few changes. Please read the [guide](http://tomcat.apache.org/tomcat-6.0-doc/logging.html#Using_Log4j) provided on the Tomcat site. What this does not cover however, is how to get Tomcat to send access logging to log4j. By default, Tomcat ships with the [AccessLogValve](http://tomcat.apache.org/tomcat-6.0-doc/config/valve.html#Access_Log_Valve) which manages logging, file rolling, etc. internally without the use of a logging framework. To use log4j and in turn Scribe for access logging, you will need to ensure that you have followed a few steps:

 * Tomcat's [log4j guide](http://tomcat.apache.org/tomcat-6.0-doc/logging.html#Using_Log4j), putting all the jars in the right places
 * Include the following additional jars in Tomcat's `lib` directory (most of these are also defined in the Maven pom):
  * `log4j-scribe-appender-N.jar` (that is, this source after it has been compiled and packaged)
 * Configure your Tomcat's `server.xml` to use the `Log4JAccessLogValve`
 * Configure your Tomcat's `log4j.properties` for the access logging (to file and/or Scribe)

