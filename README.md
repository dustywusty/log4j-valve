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

Running
---

You will need the following libraries installed in your local Maven repository:

 * [Thrift](http://thrift.apache.org)

Configuration
---

	log4j.rootLogger=DEBUG,stdout,scribe
	
	log4j.appender.scribe=org.apache.log4j.net.ScribeAppender
	log4j.appender.scribe.scribe_category=MyScribeCategoryName
	log4j.appender.scribe.DatePattern='.'yyyy-MM-dd-HH
	log4j.appender.scribe.layout=org.apache.log4j.PatternLayout
	log4j.appender.scribe.layout.ConversionPattern=%5p [%t] %d{ISO8601} %F (line %L) %m%n

