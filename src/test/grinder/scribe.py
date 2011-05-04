from net.grinder.script.Grinder import grinder
from net.grinder.script import Test
from org.apache.log4j.net import ScribeAppender, ScribeStatisticsErrorHandler
from org.apache.log4j import PatternLayout, Level, Logger
from org.apache.log4j.spi import LoggingEvent

test = Test(1, 'Log')

# setup the ScribeAppender
appender = ScribeAppender()

appender.setName('scribe')
appender.setThreshold(Level.INFO)
appender.setLayout(PatternLayout('%p - %m'))

appender.setRemoteHost('scribe.la.devbln.europe.nokia.com')
appender.setLocalHostname('test host')

errorHandler = ScribeStatisticsErrorHandler()
appender.setErrorHandler(errorHandler)

logger = Logger.getLogger('grinder')

class TestRunner:
    def __call__(self):
        
        wrappedAppender = test.wrap(appender)
        
        # append test log message
        message = "Test log message: %s, %s" % (grinder.threadNumber, grinder.runNumber)
        event = LoggingEvent('grinder', logger, Level.INFO, message, None)
        
        # delay reporting stats
        grinder.statistics.delayReports = 1
        
        error = wrappedAppender.appendAndGetError(event)
        
        # report success or failure
        grinder.statistics.forLastTest.success = int(error == None)
        grinder.statistics.report()
