Log4j Scribe Appender
===

A Scribe appender for Log4j allowing log events to be sent to a local or remote Scribe instance. This is probably best used with an `AsyncAppender` wrapped around it (if you are performance crazy). You should definitely also look into setting a backup appender for messages that are dropped, so you can recover them later through some other means (if you are super paranoid about losing messages).

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

Building
---

You will need the following libraries installed in your Maven repository since they don't exist in the central repo:

 * [Thrift](http://thrift.apache.org) - 0.6.0

Configuration
---

	log4j.rootLogger=INFO, stdout, scribe
	
	# stdout/console appender
	log4j.appender.stdout=org.apache.log4j.ConsoleAppender
	log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
	log4j.appender.stdout.layout.ConversionPattern=%5p [%t] (%F:%L) - %m%n

	# Scribe appender
	log4j.appender.scribe=org.apache.log4j.net.ScribeAppender

	# do NOT use a trailing %n unless you want a newline to be transmitted to Scribe after every message
	log4j.appender.scribe.layout=org.apache.log4j.PatternLayout
	log4j.appender.scribe.layout.ConversionPattern=%d{ISO8601} %m

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
 * Include the following additional jars in Tomcat's `lib` directory (these are also defined in the Maven pom):
  * `thrift-0.6.0.jar`
  * `slf4j-log4j12-1.6.1.jar`
  * `log4j-scribe-appender-N.jar` (that is, this source after it has been compiled and packaged)
 * Configure your Tomcat's `server.xml` to use the `Log4JAccessLogValve`
 * Configure your Tomcat's `log4j.properties` for the access logging (to file and/or Scribe)

The following is a sample `conf/settings.xml` configuration:

    <?xml version='1.0' encoding='utf-8'?>
    <Server port="8005" shutdown="SHUTDOWN">

      <Service name="Catalina">
        <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />

        <Engine name="Catalina" defaultHost="localhost">

          <Host name="localhost"  appBase="webapps"
                unpackWARs="true" autoDeploy="true"
                xmlValidation="false" xmlNamespaceAware="false">

            <!-- use log4j for access logging -->
            <!-- set the logger name to access, this will need to match the logger name in log4j.properties in lib -->
            <!-- use the Apache common log format -->
            <Valve className="org.apache.catalina.valves.Log4JAccessLogValve"
                   loggerName="access" pattern="common" resolveHosts="false" />
          </Host>
        </Engine>
      </Service>
    </Server>

The following is a sample, corresponding `lib/log4j.properties` configuration:

    # appenders
    log4j.appender.CATALINA=org.apache.log4j.DailyRollingFileAppender
    log4j.appender.CATALINA.file=${catalina.base}/logs/catalina
    log4j.appender.CATALINA.encoding=UTF-8
    log4j.appender.CATALINA.append=true
    log4j.appender.CATALINA.DatePattern='.'yyyy-MM-dd'.log'
    log4j.appender.CATALINA.layout=org.apache.log4j.PatternLayout
    log4j.appender.CATALINA.layout.ConversionPattern=%d [%t] %-5p %c- %m%n

    log4j.appender.LOCALHOST=org.apache.log4j.DailyRollingFileAppender
    log4j.appender.LOCALHOST.file=${catalina.base}/logs/localhost
    log4j.appender.LOCALHOST.encoding=UTF-8
    log4j.appender.LOCALHOST.append=true
    log4j.appender.LOCALHOST.DatePattern='.'yyyy-MM-dd'.log'
    log4j.appender.LOCALHOST.layout=org.apache.log4j.PatternLayout
    log4j.appender.LOCALHOST.layout.ConversionPattern=%d [%t] %-5p %c- %m%n

    log4j.appender.ACCESS=org.apache.log4j.DailyRollingFileAppender
    log4j.appender.ACCESS.file=${catalina.base}/logs/access
    log4j.appender.ACCESS.encoding=UTF-8
    log4j.appender.ACCESS.append=true
    log4j.appender.ACCESS.DatePattern='.'yyyy-MM-dd'.log'
    log4j.appender.ACCESS.layout=org.apache.log4j.PatternLayout
    log4j.appender.ACCESS.layout.ConversionPattern=%m%n

	log4j.appender.SCRIBE_ACCESS=org.apache.log4j.net.ScribeAppender
	log4j.appender.SCRIBE_ACCESS.layout=org.apache.log4j.PatternLayout
	log4j.appender.SCRIBE_ACCESS.layout.ConversionPattern=%d{ISO8601} %m
	log4j.appender.SCRIBE_ACCESS.category=tomcat.access

    log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
    log4j.appender.CONSOLE.encoding=UTF-8
    log4j.appender.CONSOLE.layout=org.apache.log4j.PatternLayout
    log4j.appender.CONSOLE.layout.ConversionPattern=%d [%t] %-5p %c- %m%n

    # loggers -> appenders
    log4j.rootLogger=INFO, CATALINA

    log4j.logger.org.apache.catalina.core.ContainerBase.[Catalina].[localhost]=INFO, LOCALHOST
    log4j.additivity.org.apache.catalina.core.ContainerBase.[Catalina].[localhost]=false

    log4j.logger.access=INFO, ACCESS, SCRIBE_ACCESS
    log4j.additivity.access=false
