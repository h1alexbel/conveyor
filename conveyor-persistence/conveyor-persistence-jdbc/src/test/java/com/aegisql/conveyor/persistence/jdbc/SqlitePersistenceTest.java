package com.aegisql.conveyor.persistence.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.aegisql.conveyor.persistence.jdbc.engine.connectivity.ConnectionFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.JdbcPersistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.sqlite.SQLiteConfig;

public class SqlitePersistenceTest {

	JdbcPersistenceBuilder<Integer> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("sqlite", Integer.class)
			.autoInit(true)
			.database("conveyor_db_sqlite");
	
	@BeforeClass
	public static void setUpBeforeClass() {
		Tester.removeFile("conveyor_db_sqlite");
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
	public void testWithDefaultArchivingStrategy() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		
		Persistence<Integer> p = persistenceBuilder
				.encryptionSecret("dfqejrfljheq")
				.idSupplier(ids::incrementAndGet).build();//pb.build();
		assertNotNull(p);
		p.archiveAll();
		long id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(1, id);
		id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(2, id);
		Cart<Integer,String,String> c = new ShoppingCart<Integer, String, String>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer,?,String> c2 = p.getPart(2);
		System.out.println(c2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		System.out.println("all IDs "+allIds);
		assertNotNull(allIds);
		assertEquals(1,allIds.size());
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1,allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		System.out.println("Completed:"+completed);
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
	}

	@Test
	public void testWithArchivedArchivingStrategy() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		
		Persistence<Integer> p = persistenceBuilder
				.idSupplier(ids::incrementAndGet)
				.setArchived()
				.build();
		assertNotNull(p);
		p.archiveAll();
		
		long id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(1, id);
		id = p.nextUniquePartId();
		System.out.println("ID="+id);
		assertEquals(2, id);
		Cart<Integer,String,String> c = new ShoppingCart<Integer, String, String>(1, "value", "label");
		p.savePart(id, c);
		p.saveCompletedBuildKey(1);
		Cart<Integer,?,String> c2 = p.getPart(2);
		System.out.println(c2);
		assertNotNull(c2);
		Collection<Long> allIds = p.getAllPartIds(1);
		System.out.println("all IDs "+allIds);
		assertNotNull(allIds);
		assertEquals(1,allIds.size());
		Collection<Cart<Integer,?,String>> allCarts = p.getAllParts();
		assertNotNull(allCarts);
		assertEquals(1,allCarts.size());
		Set<Integer> completed = p.getCompletedKeys();
		System.out.println("Completed:"+completed);
		assertNotNull(completed);
		assertTrue(completed.contains(1));
		p.archiveCompleteKeys(completed);
		p.archiveKeys(completed);
		p.archiveParts(allIds);
		assertNull(p.getPart(2));
	}

	
	@Test
	public void testDeleteExpired() throws Exception {
		AtomicLong ids = new AtomicLong(0);
		Persistence<Integer> p = persistenceBuilder
				.partTable("exp_test")
				.completedLogTable("exp_test_complete")
				.idSupplier(ids::incrementAndGet)
				.setArchived().build();
		assertNotNull(p);
		p.archiveAll();
		
		Cart<Integer,String,String> c1 = new ShoppingCart<Integer, String, String>(1, "value1", "label",System.currentTimeMillis()+1000);
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(2, "value2", "label",System.currentTimeMillis()+100000);
		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);

		p.archiveExpiredParts();
		
		Cart<Integer,?,String> rc1 = p.getPart(1);
		Cart<Integer,?,String> rc2 = p.getPart(2);
		System.out.println(rc1);
		System.out.println(rc2);
		assertNotNull(rc1);
		assertNotNull(rc2);

		Thread.sleep(3000);
		p.archiveExpiredParts();

		Cart<Integer,?,String> rc12 = p.getPart(1);
		Cart<Integer,?,String> rc22 = p.getPart(2);
		assertNull(rc12);
		assertNotNull(rc22);
		
	}
	
	@Test
	public void testSaveAndRead() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("sqlite", Integer.class)
				.autoInit(true)
				.database("./conveyor_db_sqlite")
				.partTable("PART2")
				.completedLogTable("COMPLETED_LOG2")
				;
		
		JdbcPersistence<Integer> p = jpb.build();
		
		assertNotNull(p);
		Cart<Integer,String,String> cart = new ShoppingCart<Integer, String, String>(100, "test", "label");
		p.savePart(1, cart);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
		System.out.println(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
	}

	@Test
	public void testSaveAndReadInMemory() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("sqlite-memory", Integer.class)
				.autoInit(true)
				.database("conveyor_db_sqlite_mem")
				.partTable("PART_MEM")
				.completedLogTable("COMPLETED_LOG_MEM")
				;

		JdbcPersistence<Integer> p = jpb.build();
		assertNotNull(p);
		p.archiveAll();
		Cart<Integer,String,String> cart = new ShoppingCart<Integer, String, String>(100, "test", "label");
		p.savePart(1, cart);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
		System.out.println(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
	}


	@Test
	public void sqliteConfigTest() {
		SQLiteConfig config = new SQLiteConfig();
		config.setJournalMode(SQLiteConfig.JournalMode.TRUNCATE);
		config.setLockingMode(SQLiteConfig.LockingMode.EXCLUSIVE);

		ConnectionFactory cf = ConnectionFactory.cachingExternalConnectionFactoryInstance(()->{
			try {
				return config.createConnection("jdbc:sqlite:pragma_test");
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});
	}

	
}
