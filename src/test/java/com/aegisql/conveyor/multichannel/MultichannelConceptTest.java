package com.aegisql.conveyor.multichannel;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.multichannel.UserBuilderEvents;

// TODO: Auto-generated Javadoc
/**
 * The Class MultichannelConceptTest.
 */
public class MultichannelConceptTest {

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
	 * Test simple concept.
	 *
	 * @throws InterruptedException the interrupted exception
	 */
	@Test
	public void testSimpleConcept() throws InterruptedException {
		
		/*
		 * This is a simple fully scalable concept based on independent conveyors for each
		 * channel and one merging conveyor.
		 * 
		 * Additional merging smart labels - one per channel
		 * Partially implemented result. This is a requirement for channels
		 * Concept of label filter. Merging conv should only accept merging labels
		 * channels should accept corresponding labels too
		 * 
		 * */

		AssemblingConveyor<Integer, UserBuilderEvents, User> merge = new AssemblingConveyor<>();
		merge.setBuilderSupplier(UserBuilder::new);
		merge.setName("MERGE");
		merge.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.first != null && ub.last != null && ub.yearOfBirth != null;
		});
		merge.setResultConsumer(bin->{
			System.out.println("Merged "+bin.product);

		});
		merge.addCartBeforePlacementValidator(cart->{
			System.out.println("placement: "+cart);
			if(cart.getLabel() != UserBuilderEvents.MERGE_A && cart.getLabel() != UserBuilderEvents.MERGE_B) {
				throw new RuntimeException("Unacceptable label "+cart);
			}
		});
		
		
		AssemblingConveyor<Integer, UserBuilderEvents, User> ch1 = new AssemblingConveyor<>();
		ch1.setName("CH1");
		ch1.setBuilderSupplier(UserBuilder::new);
		ch1.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.first != null && ub.last != null;
		});
		ch1.setResultConsumer(bin->{
			ShoppingCart<Integer, User, UserBuilderEvents> cart = new ShoppingCart<>(bin.key, bin.product, UserBuilderEvents.MERGE_A,bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			merge.add(cart);
			System.out.println("A sent "+bin.product);
		});

		AssemblingConveyor<Integer, UserBuilderEvents, User> ch2 = new AssemblingConveyor<>();
		ch2.setName("CH2");
		ch2.setBuilderSupplier(UserBuilder::new);
		ch2.setReadinessEvaluator((state,builder)->{
			UserBuilder ub = (UserBuilder)builder;
			return ub.yearOfBirth != null;
		});
		ch2.setResultConsumer(bin->{
			ShoppingCart<Integer, User, UserBuilderEvents> cart = new ShoppingCart<>(bin.key, bin.product, UserBuilderEvents.MERGE_B,bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			merge.add(cart);
			System.out.println("B sent "+bin.product);
		});

		ShoppingCart<Integer, String, UserBuilderEvents> cartA1 = new ShoppingCart<>(1,"John", UserBuilderEvents.SET_FIRST,100,TimeUnit.MILLISECONDS);
		ShoppingCart<Integer, String, UserBuilderEvents> cartA2 = new ShoppingCart<>(1,"Silver", UserBuilderEvents.SET_LAST,100,TimeUnit.MILLISECONDS);
		
		ShoppingCart<Integer, Integer, UserBuilderEvents> cartB1 = new ShoppingCart<>(1,1695, UserBuilderEvents.SET_YEAR,100,TimeUnit.MILLISECONDS);

		ch1.add(cartA1);
		ch1.add(cartA2);
		ch2.add(cartB1);
		
		Thread.sleep(20);
		
	}

}
