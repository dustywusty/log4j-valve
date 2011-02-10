package org.apache.log4j;

/**
 * A simple way to test our logging from the command line. Just pass the following arguments and make sure a log4j
 * configuration file (properties or XML) is on your classpath as well.
 * 
 * <ul>
 * <li>logging level (DEBUG, INFO, WARN, ERROR, FATAL)</li>
 * <li>message to log (quoted)</li>
 * <li>(optional) exception message to log (quoted)</li>
 * </ul>
 * 
 * <p>
 * Easiest way to get here is to build an all-in-one jar with the Maven Assembly plugin:
 * 
 * <pre>
 * mvn assembly:assmebly
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Now just create a <code>log4j.properties</code> file somewhere and reference it on your classpath:
 * 
 * <pre>
 * java -cp target/scribe-log4j-appender-1-jar-with-dependencies.jar:src/test/resources org.apache.log4j.TestLogging info "here is the test message"
 * </pre>
 * 
 * </p>
 * 
 * @author Josh Devins
 */
public class TestLogging {

    private static final Logger LOGGER = Logger.getLogger(TestLogging.class);

    public static void main(final String[] args) {

        if (args.length != 2 && args.length != 3) {
            throw new IllegalArgumentException("Two or three paramters are required");
        }

        Level level = Level.toLevel(args[0]);
        String message = args[1];
        String exceptionMessage = null;

        if (args.length == 3) {
            exceptionMessage = args[2];
        }

        if (args.length == 2) {
            LOGGER.log(level, message);
        } else {
            LOGGER.log(level, message, new Exception(exceptionMessage));
        }
    }
}
