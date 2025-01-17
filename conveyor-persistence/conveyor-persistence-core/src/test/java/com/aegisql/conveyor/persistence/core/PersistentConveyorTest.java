package com.aegisql.conveyor.persistence.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Acknowledge;
import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.persistence.core.harness.PersistTestImpl;
import com.aegisql.conveyor.persistence.core.harness.Trio;
import com.aegisql.conveyor.persistence.core.harness.TrioConveyor;
import com.aegisql.conveyor.persistence.core.harness.TrioPart;

public class PersistentConveyorTest {

	@BeforeClass
	public static void setUpBeforeClass() {
		BasicConfigurator.configure();
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void veryBasicTest() {
		Persistence<Integer> p = new PersistTestImpl();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);
	
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
	}

	
	@Test
	public void simpleAckTest() throws InterruptedException {
		PersistTestImpl p = new PersistTestImpl();
		TrioConveyor tc = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc = new PersistentConveyor(p, tc);
		pc.setAutoAcknowledge(false);
		
		AtomicReference<Acknowledge> ref = new AtomicReference<>();
		
		pc.resultConsumer().andThen(bin->{
			ref.set(bin.acknowledge);
		}).set();
		pc.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p);
		pc.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p);
		assertEquals(1, tc.results.size());
		assertNotNull(ref.get());
		assertFalse(ref.get().isAcknowledged());
		assertFalse(p.isEmpty());
		ref.get().ack();
		assertTrue(ref.get().isAcknowledged());
		Thread.sleep(1000);
		System.out.println(p);
		assertTrue(p.isEmpty());
	}

	
	@Test
	public void simpleReplayTest() throws InterruptedException {
		PersistTestImpl p1 = new PersistTestImpl();
		TrioConveyor tc1 = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1);
		pc1.setName("TC1");
		pc1.part().id(1).label(TrioPart.TEXT1).value("txt1").place();
		pc1.part().id(1).label(TrioPart.TEXT2).value("txt2").place().join();
		System.out.println(p1);
		
		pc1.stop();
		TrioConveyor tc2 = new TrioConveyor();
		
		PersistTestImpl p2 = new PersistTestImpl(p1);
		//Must copy state from the previous persistence
		assertFalse(p2.isEmpty());
		//p1 must be empty after moving data to p1. 
		assertTrue(p1.isEmpty());
		PersistentConveyor<Integer, TrioPart, Trio> pc2 = new PersistentConveyor(p2, tc2);
		pc2.setName("TC2");
		pc2.part().id(1).label(TrioPart.NUMBER).value(1).place().join();
		System.out.println(p1);
		assertEquals(0, tc1.results.size());
		assertEquals(1, tc2.results.size());
		System.out.println(tc2);
		System.out.println("P1="+p1);
		System.out.println("P2="+p2);
		//p2 must be empty after completion. 
		Thread.sleep(100);
		assertTrue(p2.isEmpty());
	}

	@Test
	public void testAccessByName() {
		PersistTestImpl p1 = new PersistTestImpl();
		TrioConveyor tc1 = new TrioConveyor();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = new PersistentConveyor(p1, tc1);
		
		pc1.setName("test_name");
		
		Conveyor pc2 = Conveyor.byName("test_name");
		assertEquals(pc1,pc2);
		
	}
	
	@Test
	public void testConveyorCreationApi() {
		PersistTestImpl p1 = new PersistTestImpl();
		
		PersistentConveyor<Integer, TrioPart, Trio> pc1 = p1.getConveyor();
		
		pc1.setName("test_name");
		
		Conveyor pc2 = Conveyor.byName("test_name");
		assertEquals(pc1,pc2);
		
		
	}
}
