Scribe log4j Appender
===

History
---

[Alex Loddengaard](http://github.com/alexlod/scribe-log4j-appender)

 * original author

[Chris Goffinet](http://github.com/lenn0x/Scribe-log4j-Appender)

 * cleaned up code to work in non-Hadoop environments
 * added support to reconnect if it loses a connection or if Scribe goes away

[Josh Devins](http://github.com/joshdevins/Scribe-log4j-Appender)

 * reorganized
 * removed checked in libs and provided basic Maven pom
 * regenerated Thrift classes and manually removed dependency on SLF4J

Building
---

You will need the following libraries installed in your Maven repository:

 * [Thrift](http://thrift.apache.org) - 0.6.0

Configuration
---

	log4j.rootLogger=DEBUG,stdout,scribe
	
	log4j.appender.scribe=org.apache.log4j.net.ScribeAppender

	# optional properties
	log4j.appender.scribe.category=default
	log4j.appender.scribe.remoteHost=127.0.0.1
	log4j.appender.scribe.remoteHost=1463
	log4j.appender.scribe.localHostname=app01.host.com # canonical hostname will be looked up if not provided

	log4j.appender.scribe.DatePattern=yyyy-MM-dd-HH
	log4j.appender.scribe.layout=org.apache.log4j.PatternLayout
	log4j.appender.scribe.layout.ConversionPattern=%5p [%t] %d{ISO8601} %F (%L) %m%n

