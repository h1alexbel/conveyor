package com.aegisql.conveyor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderEvents2;
import com.aegisql.conveyor.user.UserBuilderSmart;
import com.aegisql.conveyor.user.UserBuilderTesting;

public class SmartConveyorTest {

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testBasicsSmart() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, Cart<Integer, ?, UserBuilderEvents>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res);
				    });
		conveyor.setReadinessEvaluator((lot, builder) -> {
			return lot.previouslyAccepted == 2;
		});
		conveyor.setName("User Assembler");
		Cart<Integer, String, UserBuilderEvents> c1 = new Cart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, String, UserBuilderEvents> c3 = new Cart<>(2, "Mike", UserBuilderEvents.CREATE);
		Cart<Integer, Integer, UserBuilderEvents> c4 = c1.nextCart(1999, UserBuilderEvents.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testBasicsTesting() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents2, Cart<Integer, ?, UserBuilderEvents2>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderTesting::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res);
				    });
		conveyor.setName("Testing User Assembler");
		Cart<Integer, String, UserBuilderEvents2> c1 = new Cart<>(1, "John", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents2> c2 = c1.nextCart("Doe", UserBuilderEvents2.SET_LAST);
		Cart<Integer, String, UserBuilderEvents2> c3 = new Cart<>(2, "Mike", UserBuilderEvents2.SET_FIRST);
		Cart<Integer, Integer, UserBuilderEvents2> c4 = c1.nextCart(1999, UserBuilderEvents2.SET_YEAR);


		conveyor.offer(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.offer(c2);
		conveyor.offer(c3);
		conveyor.offer(c4);
		Thread.sleep(100);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);

		Thread.sleep(100);

		conveyor.stop();
	}

	@Test
	public void testRejectedStartOffer() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, Cart<Integer, ?, UserBuilderEvents>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res);
				    });
		conveyor.setReadinessEvaluator((lot, builder) -> {
			return lot.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		Cart<Integer, String, UserBuilderEvents> c1 = new Cart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.offer(c1));
		Thread.sleep(1100);
		assertFalse(conveyor.offer(c2));
		conveyor.stop();
	}

	@Test(expected=IllegalStateException.class) //???? Failed
	public void testRejectedStartAdd() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, Cart<Integer, ?, UserBuilderEvents>, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		conveyor.setResultConsumer(res->{
				    	outQueue.add(res);
				    });
		conveyor.setReadinessEvaluator((lot, builder) -> {
			return lot.previouslyAccepted == 2;
		});
		conveyor.rejectUnexpireableCartsOlderThan(1, TimeUnit.SECONDS);
		Cart<Integer, String, UserBuilderEvents> c1 = new Cart<>(1, "John", UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = c1.nextCart("Doe", UserBuilderEvents.SET_LAST);
		assertTrue(conveyor.add(c1));
		Thread.sleep(1000);
		conveyor.add(c2);
		conveyor.stop();
	}

	@Test
	public void testSmart() {
		UserBuilderSmart ub = new UserBuilderSmart();
		
		UserBuilderEvents.SET_FIRST.getSetter().accept(ub, "first");
		UserBuilderEvents.SET_LAST.getSetter().accept(ub, "last");
		UserBuilderEvents.SET_YEAR.getSetter().accept(ub, 1970);
		
		System.out.println(ub.build());
		
	}
		
}
