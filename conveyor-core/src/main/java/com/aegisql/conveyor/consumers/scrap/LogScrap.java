package com.aegisql.conveyor.consumers.scrap;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LogResult.
 *
 * @param <K> the key type
 */
public class LogScrap <K> implements ScrapConsumer<K,Object> {

	/**
	 * The Enum Level.
	 */
	public static enum Level {
		
		/** The trace. */
		TRACE,
		
		/** The debug. */
		DEBUG,
		
		/** The info. */
		INFO,
		
		/** The warn. */
		WARN,
		
		/** The error. */
		ERROR,
		
		/** The stdout. */
		STDOUT,
		
		/** The stderr. */
		STDERR
	}
	
	/** The Constant stdout. */
	private final static ScrapConsumer<?,?> stdout = bin->{
		System.out.println(""+bin);		
	};
	
	/** The Constant stderr. */
	private final static ScrapConsumer<?,?> stderr = bin->{
		System.err.println(""+bin);		
	};
	
	/** The consumer. */
	private final ScrapConsumer<?,?> consumer;
	
	/**
	 * Instantiates a new log result.
	 */
	public LogScrap() {
		consumer = bin->Conveyor.LOG.debug("{}",bin);
	}

	/**
	 * Instantiates a new log result.
	 *
	 * @param log the log
	 * @param level the level
	 */
	public LogScrap(Logger log, Level level) {
		switch (level) {
		case TRACE:
			consumer = bin->log.trace("{}",bin);
			break;
		case DEBUG:
			consumer = bin->log.debug("{}",bin);
			break;
		case INFO:
			consumer = bin->log.info("{}",bin);
			break;
		case WARN:
			consumer = bin->log.warn("{}",bin);
			break;
		case ERROR:
			consumer = bin->log.error("{}",bin);
			break;
		case STDOUT:
			consumer = stdout;
			break;
		case STDERR:
			consumer = stderr;
			break;
		default:
			consumer = stdout;
			break;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,Object> t) {
		consumer.accept((ScrapBin) t);
	}

	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.TRACE);
	}
	
	/**
	 * Trace.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> trace(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.TRACE);
	}

	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.DEBUG);
	}
	
	/**
	 * Debug.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> debug(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.DEBUG);
	}

	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.INFO);
	}
	
	/**
	 * Info.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> info(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.INFO);
	}

	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.WARN);
	}
	
	/**
	 * Warn.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> warn(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.WARN);
	}

	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,Logger logger) {
		return new LogScrap<>(logger,Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,String logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.ERROR);
	}
	
	/**
	 * Error.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @param logger the logger
	 * @return the log scrap
	 */
	public static <K,E> LogScrap<K> error(Conveyor<K, ?, E> conveyor,Class<?> logger) {
		return new LogScrap<>(LoggerFactory.getLogger(logger),Level.ERROR);
	}

	/**
	 * Std out.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> stdOut(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.STDOUT);
	}

	/**
	 * Std err.
	 *
	 * @param <K> the key type
	 * @param <E> the element type
	 * @param conveyor the conveyor
	 * @return the log result
	 */
	public static <K,E> LogScrap<K> stdErr(Conveyor<K, ?, E> conveyor) {
		return new LogScrap<>(Conveyor.LOG,Level.STDERR);
	}

}