package com.aegisql.conveyor.cart.command;

import static org.junit.Assert.*;

import java.util.function.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.UserBuilder;

public class CommandTest {
	
	public static class TestOut implements Supplier<String> {
		boolean ready = false;
		
		String out = "TestOut";
		
		public TestOut() {
			
		}

		@Override
		public String get() {
			return out;
		}
	}

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
	public void testCheckCommand() throws InterruptedException {
		AssemblingConveyor<Integer, String, String> ac = new AssemblingConveyor<>();

		TestOut testOut = new TestOut();
		
		ac.setBuilderSupplier(()->{
			return testOut;
		});
		ac.setDefaultCartConsumer((label, value, builder) -> {
			TestOut to = (TestOut) builder;
			to.out = value.toString();
			System.out.println("OUT set "+to.out);
		});
		
		ac.setReadinessEvaluator((toBuilder)->{
			TestOut to = (TestOut)toBuilder;
			return to.ready;
		});
		
		StringBuilder out = new StringBuilder();
		
		ac.setResultConsumer((bin)->{
			System.out.println(bin);
			out.append(bin.product);
		});
		
		Cart<Integer,String,String> c = new ShoppingCart<>(1, "Test", "");
		
		CheckBuildCommand<Integer> check = new CheckBuildCommand<Integer>(1);
		
		ac.add(c);
		
		Thread.sleep(100);
		ac.addCommand(check);
		Thread.sleep(100);
		assertEquals(0, out.length());
		testOut.ready = true;
		ac.addCommand(check);
		Thread.sleep(100);
		
		assertEquals("Test",out.toString());
		ac.addCommand(check);
		Thread.sleep(50);
		
	}

}