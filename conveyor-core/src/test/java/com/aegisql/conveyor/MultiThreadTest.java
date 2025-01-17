/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class MultiThreadTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class MultiThreadTest {
	
	/** The size. */
	public static int SIZE = 1000;
	
	/** The in user. */
	public static User[] inUser  = new User[SIZE];
	
	/** The out user. */
	public static User[] outUser = new User[SIZE];

	/** The conveyor. */
	public static 		AssemblingConveyor<Integer, String, User> 
	conveyor = new AssemblingConveyor<>();

	
	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeClass
	public static void setUpBeforeClass() {

		conveyor.setBuilderSupplier( UserBuilder::new );
	    conveyor.setDefaultCartConsumer( (label, value, builder) -> {
		UserBuilder userBuilder = (UserBuilder) builder;
		if(label == null) {
			userBuilder.setReady(true);
			return;
		}
		switch (label) {
		case "setFirst":
			userBuilder.setFirst((String) value);
			break;
		case "setLast":
			userBuilder.setLast((String) value);
			break;
		case "setYearOfBirth":
			userBuilder.setYearOfBirth((Integer) value);
			break;
		default:
			throw new RuntimeException("Unknown label " + label);
		}
	    } );
	    conveyor.setReadinessEvaluator( (state, builder) -> {
		UserBuilder userBuilder = (UserBuilder) builder;
		return state.previouslyAccepted == 3 || userBuilder.ready();
	    });
   
	    conveyor.setName("Multithread User Builder");
		
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		conveyor.stop();
		Thread.sleep(100);
	}

	/**
	 * Sets the up.
	 *
	 */
	@Before
	public void setUp() {
	}

	/**
	 * Tear down.
	 *
	 */
	@After
	public void tearDown() {
	}
	
	
	
	/**
	 * Gets the random ints.
	 *
	 * @return the random ints
	 */
	public static int[] getRandomInts() {
		List<Integer> ids = new ArrayList<>(SIZE);
		for(int i = 0; i < SIZE; i++) {
			ids.add(i);
		}
		Collections.shuffle(ids);
		int[] idInt = new int[SIZE];
		for(int i = 0; i < SIZE; i++) {
			idInt[i] = ids.get(i);
		}
		return idInt;
	}
	
	/**
	 * Test exception.
	 */
	@Test(expected=NullPointerException.class)
	public void testException() {
		conveyor.place(null);
	}
	
	/**
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void test() throws InterruptedException {

	conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
	conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
	assertFalse(conveyor.isOnTimeoutAction());
	conveyor.setOnTimeoutAction((builder)->{
		System.out.println("timeout "+builder.get());
	});
	assertTrue(conveyor.isOnTimeoutAction());
	/** The out queue. */
	ResultQueue<Integer,User> outQueue = new ResultQueue<>();

	conveyor.resultConsumer().first(outQueue).set();
		
	Thread runFirst = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, String, String> cart = new ShoppingCart<>(ids[i], "First_"+ids[i], "setFirst",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	

	Thread runLast = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, String, String> cart = new ShoppingCart<>(ids[i], "Last_"+ids[i], "setLast",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	

	Thread runYear = new Thread(()->{
		Random r = new Random();
		int[] ids = getRandomInts();
		for(int i = 0; i < SIZE; i++) {
			User u = inUser[ids[i]];
			Cart<Integer, Integer, String> cart = new ShoppingCart<>(ids[i], 1900+r.nextInt(100), "setYearOfBirth",1,TimeUnit.SECONDS);
			if(r.nextInt(100) == 22) continue;
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
			conveyor.place(cart);
		}
	});	
	
	runFirst.start();
	runLast.start();
	runYear.start();
		
	while(	runFirst.isAlive() || runLast.isAlive() || runYear.isAlive() ) {
		Thread.sleep(1000);
	}

	System.out.println("Good data");
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}
	

	System.out.println("Left: "+conveyor.getCollectorSize());
	Thread.sleep(3000);

	System.out.println("Incomplete data");
	while(!outQueue.isEmpty()) {
		System.out.println(outQueue.poll());
	}

	assertEquals(0,conveyor.getInputQueueSize());
	assertEquals(0,conveyor.getCollectorSize());
	assertEquals(0,conveyor.getDelayedQueueSize());

	}

}
