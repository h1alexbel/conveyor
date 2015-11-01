package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssemblingConveyorTest {

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
	public static class User {
		final String first;
		final String last;
		final int yearOfBirth;

		public User(String first, String last, int yob) {
			super();
			this.first = first;
			this.last = last;
			this.yearOfBirth = yob;
		}

		public String getFirst() {
			return first;
		}

		public String getLast() {
			return last;
		}
		
		public int getYearOfBirth() {
			return yearOfBirth;
		}

		@Override
		public String toString() {
			return "User [first=" + first + ", last=" + last + ", born in " + yearOfBirth + "]";
		}

	}

	public static class UserBuilder implements Builder<User> {

		String first;
		String last;
		Integer yearOfBirth;

		public Integer getYearOfBirth() {
			return yearOfBirth;
		}

		public void setYearOfBirth(Integer yob) {
			this.yearOfBirth = yob;
		}

		public String getFirst() {
			return first;
		}

		public void setFirst(String first) {
			this.first = first;
		}

		public String getLast() {
			return last;
		}

		public void setLast(String last) {
			this.last = last;
		}

		@Override
		public User build() {
			return new User(first, last, yearOfBirth);
		}

		@Override
		public boolean ready() {
			return first != null && last != null && yearOfBirth != null;
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
	public void testDelayed() throws InterruptedException {
		Cart<Integer, String, String> c1 = new Cart<>(1, "A", "setFirst",1,TimeUnit.SECONDS);
		Cart<Integer, String, String> c2 = new Cart<>(1, "B", "setLast",c1.getExpirationTime());

		assertFalse(c1.expired());
		assertFalse(c2.expired());
		System.out.println(c1);
		System.out.println(c2);
		assertEquals(1000, c1.getExpirationTime() - c1.getCreationTime());
		System.out.println(c1.getDelay(TimeUnit.MILLISECONDS));
		
		BlockingQueue q = new DelayQueue();
		q.add(c1);
		assertNull(q.poll());
		Thread.sleep(1000);
		assertNotNull(q.poll());
		
	}
	
	@Test
	public void testBasics() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> c 
		= new AssemblingConveyor<>(
				    UserBuilder::new,
				    (cart, builder) -> {
					UserBuilder userBuilder = (UserBuilder) builder;
					switch (cart.getLabel()) {
					case "setFirst":
						userBuilder.setFirst((String) cart.getValue());
						break;
					case "setLast":
						userBuilder.setLast((String) cart.getValue());
						break;
					case "setYearOfBirth":
						userBuilder.setYearOfBirth((Integer) cart.getValue());
						break;
					default:
						throw new RuntimeException("Unknown cart " + cart);
					}
				},
				    res->{
				    	outQueue.add(res);
				    },
				    bad -> {
				    	System.out.println("Stall: "+bad);
				    });

		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new Cart<>(1, "Doe", "setLast");
		Cart<Integer, String, String> c3 = new Cart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new Cart<>(1, 1999, "setYearOfBirth");

		Cart<Integer, Integer, String> c5 = new Cart<>(3, 1999, "setBlah");

		c.offer(c1);
		System.out.println(outQueue.poll());
		c.offer(c2);
		c.offer(c3);
		c.offer(c4);
		Thread.sleep(100);

		System.out.println(outQueue.poll());
		System.out.println(outQueue.poll());

		c.offer(c5);
		Thread.sleep(100);

		c.stop();
	}

}
