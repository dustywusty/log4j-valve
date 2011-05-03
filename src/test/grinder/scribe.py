from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from org.apache.log4j.net import ScribeAppender
from org.apache.log4j import PatternLayout, Level, Logger
from org.apache.log4j.spi import LoggingEvent

test = Test(1, 'Log message')

# setup the ScribeAppender
appender = ScribeAppender()

appender.setName('scribe')
appender.setThreshold(Level.INFO)
appender.setLayout(PatternLayout('%p - %m'))

appender.setRemoteHost('scribe.la.devbln.europe.nokia.com')
appender.setLocalHostname('test host')

logger = Logger.getLogger('grinder')

test.wrap(appender)

class TestRunner:
    def __call__(self):
        message = "Test log message: %s, %s" % (grinder.threadNumber, grinder.runNumber)
        event = LoggingEvent('grinder', logger, Level.INFO, message, None)
        test.append(event)
