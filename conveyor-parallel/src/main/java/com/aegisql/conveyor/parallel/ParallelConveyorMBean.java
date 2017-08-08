/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel;

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
	public String getName();
	
	/**
	 * Gets the type.
	 *
	 * @return the type
	 */
	public String getType();
	
	/**
	 * Gets the inner conveyors count.
	 *
	 * @return the inner conveyors count
	 */
	public int getInnerConveyorsCount();
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning();

}