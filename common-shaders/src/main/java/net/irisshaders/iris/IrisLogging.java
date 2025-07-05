package net.irisshaders.iris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class IrisLogging {
    public static final IrisLogging IRIS_LOGGER = new IrisLogging(IrisConstants.MODNAME);


	public static boolean ENABLE_SPAM = false; // FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final Marker FATAL_MARKER = MarkerFactory.getMarker("FATAL");

	private final Logger logger;

	private IrisLogging(String loggerName) {
		this.logger = LoggerFactory.getLogger(loggerName);
	}

	public void fatal(String fatal) {
		this.logger.error(FATAL_MARKER, fatal);
	}

	public void fatal(String fatal, Throwable t) {
		this.logger.error(FATAL_MARKER, fatal, t);
	}

	public void error(String error) {
		this.logger.error(error);
	}

	public void error(String error, Object... o) {
		this.logger.error(error, o);
	}

	public void error(String error, Throwable t) {
		this.logger.error(error, t);
	}

	public void warn(String warning) {
		this.logger.warn(warning);
	}

	public void warn(String warning, Object... object) {
		this.logger.warn(warning, object);
	}

	public void warn(String warning, Throwable t) {
		this.logger.warn(warning, t);
	}

	public void warn(Throwable o) {
		this.logger.warn("", o);
	}

	public void info(String info) {
		this.logger.info(info);
	}

	public void info(String info, Object... o) {
		this.logger.info(info, o);
	}

	public void debug(String debug) {
		this.logger.debug(debug);
	}

	public void debug(String debug, Throwable t) {
		this.logger.debug(debug, t);
	}
}
