/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.function.BiConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Enum Command.
 */
public enum Command  implements SmartLabel<AssemblingConveyor> {
	
	/** The create build. */
	CREATE_BUILD(AssemblingConveyor::createNow),
	
	/** The cancel build. */
	CANCEL_BUILD(AssemblingConveyor::cancelNow),
	
	/** The timeout build. */
	TIMEOUT_BUILD(AssemblingConveyor::timeoutNow);

	/**
	 * Instantiates a new command.
	 *
	 * @param setter the setter
	 */
	Command(BiConsumer<AssemblingConveyor, Object> setter) {
		this.setter = setter;
	}
	
	/** The setter. */
	BiConsumer<AssemblingConveyor, Object> setter;
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.SmartLabel#getSetter()
	 */
	@Override
	public BiConsumer<AssemblingConveyor, Object> getSetter() {
		return setter;
	}
	
}
