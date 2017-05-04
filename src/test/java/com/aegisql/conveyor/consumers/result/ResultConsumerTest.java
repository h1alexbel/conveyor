package com.aegisql.conveyor.consumers.result;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.PrintStreamScrap;
import com.aegisql.conveyor.consumers.scrap.ScrapMap;
import com.aegisql.conveyor.consumers.scrap.ScrapQueue;
import com.aegisql.conveyor.consumers.scrap.ScrapReference;
import com.aegisql.conveyor.consumers.scrap.StreamScrap;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.ScalarConvertingConveyorTest.StringToUserBuulder;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingConveyor;

public class ResultConsumerTest {


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
	public void testLogConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(LogResult.info(sc).andThen(b->usr.set(b.product)));
		sc.setScrapConsumer(LogScrap.error(sc));
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(usr.get());
	}

	@Test
	public void testRefConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		ResultReference<String,User> q = ResultReference.of(sc);
		ScrapReference<String> s = ScrapReference.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.setResultConsumer(q);
		sc.setScrapConsumer(s);
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertNotNull(q.getCurrent());
		assertNotNull(s);
		assertNotNull(s.getCurrent());
		System.out.println(q);
		System.out.println(s);
	}

	
	@Test
	public void testQueueConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		ResultQueue<String,User> q = ResultQueue.of(sc);
		ScrapQueue<String> s = ScrapQueue.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.setResultConsumer(q);
		sc.setScrapConsumer(s);
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertEquals(1,q.size());
		assertNotNull(s);
		assertEquals(1,s.size());
		System.out.println(q);
		System.out.println(s);
	}

	@Test
	public void testMapConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		ResultMap<String,User> q = ResultMap.of(sc);
		ScrapMap<String> m = ScrapMap.of(sc);
		sc.setBuilderSupplier(StringToUserBuulder::new);
		sc.setResultConsumer(q);
		sc.setScrapConsumer(m);
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		assertNotNull(q);
		assertEquals(1,q.size());
		assertTrue(q.containsKey("test"));
		assertNotNull(m);
		assertEquals(1,m.size());
		assertTrue(m.containsKey("bad"));
		assertNotNull(m.get("bad"));
		assertEquals(1,m.get("bad").size());
		System.out.println(q);
		System.out.println(m);
	}

	
	@Test
	public void testPrintStreamConsumers() throws InterruptedException, ExecutionException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(PrintStreamResult.of(sc,System.out).andThen(PrintStreamResult.of(sc,System.out,u->u.getFirst()+" "+u.getLast())).andThen(b->usr.set(b.product)));
		sc.setScrapConsumer(PrintStreamScrap.of(sc,System.err).andThen(PrintStreamScrap.of(sc,System.err,o->{
			return "ERROR: "+o;
		})));
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(usr.get());
	}

	@Test
	public void testStreamConsumers() throws InterruptedException, ExecutionException, IOException {
		ScalarConvertingConveyor<String, String, User> sc = new ScalarConvertingConveyor<>();
		sc.setBuilderSupplier(StringToUserBuulder::new);
		AtomicReference<User> usr = new AtomicReference<User>(null);
		sc.setResultConsumer(StreamResult.of(sc,"/tmp/testStreamConsumers.out").andThen(StreamResult.of(sc,"/tmp/testStreamConsumers2.out",b->b.product.getFirst()+" "+b.product.getLast())).andThen(b->usr.set(b.product)));
		sc.setScrapConsumer(StreamScrap.of(sc,"/tmp/testStreamConsumers.err"));
		String csv = "John,Dow,1990";
		sc.part().id("bad").ttl(-1,TimeUnit.MILLISECONDS).value(csv).place();
		CompletableFuture<Boolean> cf = sc.part().id("test").value(csv).place();
		cf.get();
		sc.completeAndStop().get();
		assertNotNull(usr.get());
	}

}
