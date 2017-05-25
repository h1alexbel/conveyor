package com.aegisql.conveyor.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite.Status;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.ResultQueue;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;

// TODO: Auto-generated Javadoc
/**
 * The Class ResultQueueTest.
 */
public class ResultQueueTest {

	/**
	 * Sets the up before class.
	 *
	 * @throws Exception the exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Tear down.
	 *
	 * @throws Exception the exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test queue.
	 */
	@Test
	public void testQueue() {
		ResultQueue<String,User> q = new ResultQueue<>();
		assertEquals(0, q.size());
		ConcurrentLinkedDeque<User> u = q.<ConcurrentLinkedDeque<User>>unwrap();
		assertNotNull(u);
		ProductBin<String, User> b1 = new ProductBin<>("", new User("","",1999), 0, Status.READY);
		q.accept(b1);

		assertEquals(1, q.size());
		
		User u1 = q.poll();
		assertNotNull(u1);
		assertEquals(0, q.size());

		ProductBin<String, User> b2 = new ProductBin<>("", new User("","",1999), 0, Status.READY);
		q.accept(b2);
		assertEquals(1, u.size());
		User u2 = u.poll();
		assertNotNull(u2);
		assertEquals(0, u.size());

		
	}
	
	/**
	 * Test simple conveyor.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConveyor() throws InterruptedException {
		
		ResultQueue<Integer,User> outQueue = new ResultQueue<>();
		
		AssemblingConveyor<Integer, String, User> 
		conveyor = new AssemblingConveyor<>();
		conveyor.setBuilderSupplier(UserBuilder::new);
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.SECONDS);
		assertEquals(1000, conveyor.getDefaultBuilderTimeout());
		conveyor.setIdleHeartBeat(Duration.ofMillis(500));
		assertEquals(500,conveyor.getExpirationCollectionIdleInterval());
		conveyor.setDefaultCartConsumer((label, value, builder) -> {
			UserBuilder userBuilder = (UserBuilder) builder;
			switch (label) {
			case "setFirst":
//				System.out.println("1---- "+value);
				userBuilder.setFirst((String) value);
				break;
			case "setLast":
//				System.out.println("2---- "+value);
				userBuilder.setLast((String) value);
				break;
			case "setYearOfBirth":
//				System.out.println("3---- "+value);
				userBuilder.setYearOfBirth((Integer) value);
				break;
			default:
//				System.out.println("E---- "+value);
				throw new RuntimeException("Unknown label " + label);
			}
		});
		conveyor.resultConsumer(outQueue).set();
		conveyor.setReadinessEvaluator((state, builder) -> {
			return state.previouslyAccepted == 3;
		});
		
		ShoppingCart<Integer, String, String> c1 = new ShoppingCart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new ShoppingCart<>(1,"Doe", "setLast");
		Cart<Integer, String, String> c3 = new ShoppingCart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new ShoppingCart<>(1,1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new ShoppingCart<>(3, 1999, "setBlah");

		Cart<Integer, String, String> c6 = new ShoppingCart<>(6, "Ann", "setFirst");
		Cart<Integer, String, String> c7 = new ShoppingCart<>(7, "Nik", "setLast", 1, TimeUnit.HOURS);


		conveyor.place(c1);
		User u0 = outQueue.poll();
		assertNull(u0);
		conveyor.place(c2);
		conveyor.place(c3);
		conveyor.place(c4);
		conveyor.place(c6);
		Thread.sleep(100);
		conveyor.setIdleHeartBeat(1000, TimeUnit.MILLISECONDS);
		User u1 = outQueue.poll();
		assertNotNull(u1);
		System.out.println(u1);
		User u2 = outQueue.poll();
		assertNull(u2);
		conveyor.place(c7);
		conveyor.command().id(8).ttl(1,TimeUnit.SECONDS).create();
		conveyor.command().id(8).ttl(1,TimeUnit.SECONDS).create(UserBuilder::new);
		Thread.sleep(100);
		conveyor.command().id(6).cancel();
		conveyor.command().id(7).timeout();

		conveyor.place(c5);
		Thread.sleep(2000);
		System.out.println("COL:"+conveyor.getCollectorSize());
		System.out.println("DEL:"+conveyor.getDelayedQueueSize());
		System.out.println("IN :"+conveyor.getInputQueueSize());
		conveyor.stop();
		Thread.sleep(1000);
	}


}