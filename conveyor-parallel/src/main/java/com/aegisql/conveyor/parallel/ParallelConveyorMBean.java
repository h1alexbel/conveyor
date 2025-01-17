/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.Conveyor;

// TODO: Auto-generated Javadoc
/**
 * The Interface ParallelConveyorMBean.
 */
public interface ParallelConveyorMBean {
	
	/**
	 * Gets the name.
	 *
	 * @return the name
	 */
	String getName();
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	String getType();
	
	/**
	 * Gets the inner conveyors count.
	 *
	 * @return the inner conveyors count
	 */
	int getInnerConveyorsCount();
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	boolean isRunning();

	/**
	 * Conveyor.
	 *
	 * @param <K> the key type
	 * @param <L> the generic type
	 * @param <OUT> the generic type
	 * @return the conveyor
	 */
	<K,L,OUT> Conveyor<K, L, OUT> conveyor();
	
	/**
	 * Stop.
	 */
	void stop();
	
	/**
	 * Complete and stop.
	 */
	void completeAndStop();
	
	/**
	 * Sets the idle heart beat msec.
	 *
	 * @param msec the new idle heart beat msec
	 */
	void idleHeartBeatMsec(long msec);
	
	/**
	 * Sets the default builder timeout msec.
	 *
	 * @param msec the new default builder timeout msec
	 */
	void defaultBuilderTimeoutMsec(long msec);
	
	/**
	 * Reject unexpireable carts older than msec.
	 *
	 * @param msec the msec
	 */
	void rejectUnexpireableCartsOlderThanMsec(long msec);
	
	/**
	 * Sets the expiration postpone time msec.
	 *
	 * @param msec the new expiration postpone time msec
	 */
	void expirationPostponeTimeMsec(long msec);
	
	boolean isSuspended();
	
	void suspend();
	
	void resume();

	
}
