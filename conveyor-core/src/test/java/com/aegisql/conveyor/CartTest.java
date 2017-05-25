/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

// TODO: Auto-generated Javadoc
/**
 * The Class CartTest.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class CartTest {

	/** The running. */
	private boolean running = true;
	
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
	 * Test.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void test() throws InterruptedException {
		ShoppingCart<String,String,String> c = new ShoppingCart<>("k","v1","l",100,TimeUnit.MILLISECONDS);
		
		assertFalse(c.expired());
		
		long delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay > 0);
		assertTrue(delay <= 100);
		
		Thread.sleep(110);
		
		assertTrue(c.expired());
		
		delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		
		assertTrue(delay < 0);

	}

	/**
	 * Unexpireable test.
	 */
	@Test
	public void unexpireableTest() {
		Cart<String,String,String> c = new ShoppingCart<>("k","v1","l");
		
		assertFalse(c.expired());
		long delay = c.toDelayed().getDelay(TimeUnit.MILLISECONDS);
		assertTrue(delay > 0);

	}

	/**
	 * Test closure.
	 */
	@Test
	public void testClosure() {
		Consumer<String> c = s->{
			System.out.println(s+running);
		};
		
		c.accept("running before=");
		running=false;
		c.accept("running after=");
		
	}
	
}