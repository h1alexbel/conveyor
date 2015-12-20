/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.aegisql.conveyor.DelayHolder;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.TestingState;
import com.aegisql.conveyor.TimeoutAction;

public class ReactivePersonBuilder2 implements Supplier<Person>, TestingState<Integer, PersonBuilderLabel2>, TimeoutAction ,Delayed {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	
	private boolean forceReady = false;
	
	private DelayHolder delay = new DelayHolder(100,TimeUnit.MILLISECONDS);
	
	public ReactivePersonBuilder2() {

	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public static void setFirstName(ReactivePersonBuilder2 builder, String firstName) {
		builder.firstName = firstName;
	}

	public static void setLastName(ReactivePersonBuilder2 builder, String lastName) {
		builder.lastName = lastName;
	}

	public static void setDateOfBirth(ReactivePersonBuilder2 builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}

	@Override
	public boolean test(State<Integer, PersonBuilderLabel2> state) {
		return state.previouslyAccepted == 3 || forceReady;
	}

	public void setForceReady(boolean forceReady) {
		this.forceReady = forceReady;
	}

	@Override
	public void onTimeout() {
		if(firstName != null && lastName != null) {
			forceReady = true;
		}
	}

	@Override
	public int compareTo(Delayed o) {
		return delay.compareTo(((ReactivePersonBuilder2)o).delay);
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return delay.getDelay(unit);
	}
	
}