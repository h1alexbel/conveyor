package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LogResult;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderExpireable;
import com.aegisql.conveyor.user.UserBuilderSmart;

// TODO: Auto-generated Javadoc
/**
 * The Class PostponeExpirationTest.
 */
public class PostponeExpirationTest {

	/**
	 * Sets the up before class.
	 *
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
	}

	/**
	 * Tear down after class.
	 *
	 */
	@AfterClass
	public static void tearDownAfterClass() {
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
	 *  The out queue.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	

	/**
	 * Test default expiration postpone.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testDefaultExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();

		conveyor.scrapConsumer(LogScrap.error(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setExpirationPostponeTime(Duration.ofMillis(100));
		conveyor.setDefaultBuilderTimeout(100, TimeUnit.MILLISECONDS);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);

		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST);
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1,1999, UserBuilderEvents.SET_YEAR);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		Thread.sleep(50);
		long et1 = conveyor.getExpirationTime(1);		
		conveyor.place(c2);
		Thread.sleep(50);
		long et2 = conveyor.getExpirationTime(1);
		conveyor.place(c3);
		Thread.sleep(50); //over original exp time

		long diff = et2-et1;
		System.out.println("et1="+et1+" diff="+diff);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}

	/**
	 * Test cart expiration postpone.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testCartExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, UserBuilderEvents, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilderSmart::new);

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.scrapConsumer(LogScrap.error(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);

		ShoppingCart<Integer, String, UserBuilderEvents> c1 = new ShoppingCart<>(1, "John",
				UserBuilderEvents.SET_FIRST,100, TimeUnit.MILLISECONDS);
		Cart<Integer, String, UserBuilderEvents> c2 = new ShoppingCart<>(1,"Doe", UserBuilderEvents.SET_LAST,150, TimeUnit.MILLISECONDS);

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		Thread.sleep(110); //created with 10, but 100 added by second cart
		Cart<Integer, Integer, UserBuilderEvents> c3 = new ShoppingCart<>(1, 1999, UserBuilderEvents.SET_YEAR,100, TimeUnit.MILLISECONDS);
		conveyor.place(c3);

		Thread.sleep(100);

		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}

	/**
	 * Test builder expiration postpone.
	 *
	 * @throws InterruptedException the interrupted exception
	 * @throws ExecutionException the execution exception
	 */
	@Test
	public void testBuilderExpirationPostpone() throws InterruptedException, ExecutionException {
		AssemblingConveyor<Integer, String, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(()->new UserBuilderExpireable(100));

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.scrapConsumer(LogScrap.error(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.setIdleHeartBeat(1, TimeUnit.MILLISECONDS);
		conveyor.setDefaultCartConsumer((l,v,b)->{
			UserBuilderExpireable ub = (UserBuilderExpireable)b;
			switch(l) {
				case "FIRST":
					ub.setFirst(v.toString());
					break;
				case "LAST":
					ub.setLast(v.toString());
					break;
				case "YEAR":
					ub.setYearOfBirth((Integer)v);
					break;
			}
		});
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John",
				"FIRST");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "LAST");

		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		Thread.sleep(110); //created with 10, but 100 added by second cart
		Cart<Integer, Integer, String> c3 = new ShoppingCart<>(1, 1999, "YEAR");
		CompletableFuture<User> f = conveyor.future().id(1).get(); 
		conveyor.place(c3);

		f.get();

		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.stop();
	}


	/**
	 * Test timeout expiration postpone.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testTimeoutExpirationPostpone() throws InterruptedException {
		AssemblingConveyor<Integer, String, User> conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(()-> new UserBuilderExpireable(1000));

		/** The out queue. */
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();

		conveyor.resultConsumer().first(outQueue).andThen(LogResult.debug(conveyor)).set();
		conveyor.scrapConsumer(LogScrap.error(conveyor)).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			UserBuilderExpireable be = (UserBuilderExpireable)builder;
			return be.ready();
		});
		conveyor.setDefaultCartConsumer((l,v,b)->{
			System.out.println("Cart " + l + " " +v);
			UserBuilderExpireable be = (UserBuilderExpireable)b;
			if("FIRST".equals(l)) be.setFirst(v.toString());
		});
		conveyor.setName("User Assembler");

		conveyor.enablePostponeExpiration(true);
		conveyor.enablePostponeExpirationOnTimeout(true);
		conveyor.setIdleHeartBeat(10, TimeUnit.MILLISECONDS);
		AtomicBoolean timeouted = new AtomicBoolean(false);
		conveyor.setOnTimeoutAction((b)->{
			UserBuilderExpireable be = (UserBuilderExpireable)b;
			if(!timeouted.get()) {
				System.out.println("timeout added");
				be.addExpirationTime(1000);
				timeouted.set(true);
			} else {
				System.out.println("timeout now");
				be.setReady(true);
			}
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John",
				"FIRST");


		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);

		Thread.sleep(3000);
		u0 = outQueue.poll();
		assertNotNull(u0);
		assertTrue(timeouted.get());
		conveyor.stop();
	}

	
}
