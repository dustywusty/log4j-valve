Log4j-valve
======

[![Build Status](https://travis-ci.org/clarkda/log4j-valve.svg?branch=master)](https://travis-ci.org/clarkda/log4j-valve)

Log4j-access-valve is a Tomcat valve that enables ACCESS logging via Log4j

Getting Started
---------------

Prerequisities
--------------

* ..
* ..

Build & Test
------------

To build:

    $ gradle build

Generating Intellij IDE modules:

    $ gradle idea

Installation
-------------

* ..
* ..

Installing Log4j
-------------

```
mkdir -p /var/lib/tomcat7/lib /var/lib/tomcat7/bin && \
    wget -P /var/lib/tomcat7/bin \
      http://mirror.metrocast.net/apache/tomcat/tomcat-7/v7.0.54/bin/extras/tomcat-juli.jar && \
    wget -P /var/lib/tomcat7/lib \
      http://mirror.metrocast.net/apache/tomcat/tomcat-7/v7.0.54/bin/extras/tomcat-juli-adapters.jar

cd /tmp && \
    wget http://archive.apache.org/dist/logging/log4j/1.2.16/apache-log4j-1.2.16.tar.gz && \
    tar -zxvf apache-log4j-1.2.16.tar.gz && \
    cp apache-log4j-1.2.16/log4j-1.2.16.jar /var/lib/tomcat7/lib/
    
mv /var/lib/tomcat7/conf/logging.properties /var/lib/tomcat7/conf/logging.properties.old

````

Acknowledgements 
-------------
* [Alex Loddengaard](http://github.com/alexlod/scribe-log4j-appender)
* [Chris Goffinet](http://github.com/lenn0x/Scribe-log4j-Appender)
* [Josh Devins](http://github.com/joshdevins/Scribe-log4j-Appender)

See HISTORY.md for more info
