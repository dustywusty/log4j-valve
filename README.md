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
 
 * Create a bin/ & lib/ directory in /var/lib/tomcat7
 * Add tomcat-juli.jar to our bin/ directory
 * Add tomcat-juli-adapters.jar to our lib/ directory
 * Add log4j-1.2.<version>.jar to our lib/ directory
 * Add a log4j.properties conf to lib/ -- you can use the example file
 * Move the old logging.properties conf out of the way -- /var/lib/tomcat7/conf/logging.properties


Acknowledgements 
-------------
* [Alex Loddengaard](http://github.com/alexlod/scribe-log4j-appender)
* [Chris Goffinet](http://github.com/lenn0x/Scribe-log4j-Appender)
* [Josh Devins](http://github.com/joshdevins/Scribe-log4j-Appender)

See HISTORY.md for more info
