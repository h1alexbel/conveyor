package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.MultiKeyCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderSmart;

public class MultiKeyTest {

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
	public void testMultiSimple() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.setResultConsumer(bin->{});
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.createBuildFuture(1,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));
		CompletableFuture<User> f2 = c.createBuildFuture(2,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		CartLoader<Integer, SmartLabel<UserBuilderSmart>, ?, ?, ?> loader1 = c.id(1);
		CartLoader<Integer, SmartLabel<UserBuilderSmart>, ?, ?, ?> loader2 = c.id(2);
		loader1.part("FIRST").label(l1).place();
		loader1.part("LAST").label(l2).place();
		loader2.part("SECOND").label(l1).place();
		c.add(new MultiKeyCart<Integer, String, SmartLabel<UserBuilderSmart>>("TEST", multi));
		loader2.part("LAST").label(l2).place();
		
		loader1.part(1999).label(l3).place();
		loader2.part(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(2, counter.get());
	}
	@Test
	public void testMultiWithPredicate() throws InterruptedException, ExecutionException {
		
		AssemblingConveyor<Integer, SmartLabel<UserBuilderSmart>, User> c = new AssemblingConveyor<>();
		c.setResultConsumer(bin->{});
		SmartLabel<UserBuilderSmart> l1 = SmartLabel.of(UserBuilderSmart::setFirst);
		SmartLabel<UserBuilderSmart> l2 = SmartLabel.of(UserBuilderSmart::setLast);
		SmartLabel<UserBuilderSmart> l3 = SmartLabel.of(UserBuilderSmart::setYearOfBirth);

		AtomicInteger counter = new AtomicInteger(0);
		
		CompletableFuture<User> f1 = c.createBuildFuture(1,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));
		CompletableFuture<User> f2 = c.createBuildFuture(2,BuilderSupplier.of(UserBuilderSmart::new)
				.readyAlgorithm(new ReadinessTester().accepted(l1, l2, l3)));

		SmartLabel<UserBuilderSmart> multi = SmartLabel.of((builder,value)->{
			System.out.println("--- visited --- "+builder.get() + " --- " + value);
			counter.incrementAndGet();
		});

		CartLoader<Integer, SmartLabel<UserBuilderSmart>, ?, ?, ?> loader1 = c.id(1);
		CartLoader<Integer, SmartLabel<UserBuilderSmart>, ?, ?, ?> loader2 = c.id(2);
		loader1.part("FIRST").label(l1).place();
		loader1.part("LAST").label(l2).place();
		loader2.part("SECOND").label(l1).place();

		c.add(new MultiKeyCart<Integer, String, SmartLabel<UserBuilderSmart>>((key)->{
			return key % 2 == 0;
					},"TEST", multi));
		loader2.part("LAST").label(l2).place();
		
		loader1.part(1999).label(l3).place();
		loader2.part(2001).label(l3).place();
		User u = f1.get();
		assertEquals("FIRST",u.getFirst());
		assertEquals("LAST",u.getLast());
		assertEquals(1999,u.getYearOfBirth());
		assertEquals(1, counter.get());
	}

}
