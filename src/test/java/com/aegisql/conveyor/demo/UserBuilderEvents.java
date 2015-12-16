/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.function.BiConsumer;

import com.aegisql.conveyor.SmartLabel;

// TODO: Auto-generated Javadoc
/**
 * The Enum UserBuilderEvents.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public enum UserBuilderEvents implements SmartLabel<ReactivePersonBuilder1> {
	
	SET_FIRST(ReactivePersonBuilder1::setFirstName),
	SET_LAST(ReactivePersonBuilder1::setLastName),
	SET_YEAR(ReactivePersonBuilder1::setDateOfBirth)
	;

	BiConsumer<ReactivePersonBuilder1, Object> setter;

	<T> UserBuilderEvents(BiConsumer<ReactivePersonBuilder1,T> setter) {
		this.setter = (BiConsumer<ReactivePersonBuilder1, Object>) setter;
	}
	
	@Override
	public BiConsumer<ReactivePersonBuilder1, Object> getSetter() {
		return setter;
	}
}
