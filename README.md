Scribe log4j Appender
===

History
---

Alex Loddengaard (http://github.com/alexlod/scribe-log4j-appender)
 - original author

Chris Goffinet (http://github.com/lenn0x/Scribe-log4j-Appender)
 - cleaned up code to work in non-hadoop environments
 - added support to reconnect if it loses a connection or if Scribe goes away

Running
---

You will need the following libraries at runtime only:

libthrift-r808609.jar
log4j-1.2.15.jar
slf4j-api-1.5.8.jar
slf4j-log4j12-1.5.8.jar

Configuration
---

# Add scribe to end of rootLogger

log4j.rootLogger=DEBUG,stdout,scribe

#
# Add this to your log4j.properties
#
# You can adjust the scribe_host and scribe_port you want messages sent to by setting
# scribe_host and scribe_port
#
# You can also set the hostname if you do not want to rely on Java picking the correct hostname

log4j.appender.scribe=org.apache.log4j.net.ScribeAppender
log4j.appender.scribe.scribe_category=MyScribeCategoryName
log4j.appender.scribe.DatePattern='.'yyyy-MM-dd-HH
log4j.appender.scribe.layout=org.apache.log4j.PatternLayout
log4j.appender.scribe.layout.ConversionPattern=%5p [%t] %d{ISO8601} %F (line %L) %m%n

