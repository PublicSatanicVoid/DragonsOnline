package mc.dragons.core.logging;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import mc.dragons.core.Dragons;
import mc.dragons.core.logging.correlation.CorrelationLogger;

public class DragonsLogger extends Logger {
	private CorrelationLogger CORRELATION;
	
	private CorrelationLogger getCorrelation() {
		if(CORRELATION == null) {
			CORRELATION = Dragons.getInstance().getLightweightLoaderRegistry().getLoader(CorrelationLogger.class);
		}
		return CORRELATION;
	}
	
	public DragonsLogger(String name) {
		super(name, "sun.util.logging.resources.logging");
	}
	
	public UUID newCID() {
		return getCorrelation().registerNewCorrelationID();
	}
	
	public void discardCID(UUID cid) {
		getCorrelation().discard(cid);
	}

	public void log(UUID cid, Level level, String message) {
		getCorrelation().log(cid, level, message);
	}
	
	public void severe(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.SEVERE, message);
	}
	
	public void warning(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.WARNING, message);
	}
	
	public void notice(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.NOTICE, message);
	}
	
	public void notice(String message) {
		log(LogLevel.NOTICE, message);
	}
	
	public void info(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.INFO, message);
	}
	
	public void debug(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.DEBUG, message);
	}
	
	public void debug(String message) {
		log(LogLevel.DEBUG, message);
	}
	
	public void trace(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.TRACE, message);
	}
	
	public void trace(String message) {
		log(LogLevel.TRACE, message);
	}
	
	public void verbose(UUID cid, String message) {
		getCorrelation().log(cid, LogLevel.VERBOSE, message);
	}
	
	public void verbose(String message) {
		log(LogLevel.VERBOSE, message);
	}
	
	@Deprecated
	public void fine(String message) {
		super.fine(message);
	}

	@Deprecated
	public void finer(String message) {
		super.finer(message);
	}
	
	@Deprecated
	public void finest(String message) {
		super.finest(message);
	}
	
}
