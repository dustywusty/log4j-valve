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
 
