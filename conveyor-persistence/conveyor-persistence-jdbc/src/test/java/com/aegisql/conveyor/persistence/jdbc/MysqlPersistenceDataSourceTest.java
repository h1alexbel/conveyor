package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import com.aegisql.conveyor.loaders.PartLoader;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.*;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class MysqlPersistenceDataSourceTest {

	JdbcPersistenceBuilder persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("mysql",Integer.class)
			.autoInit(true)
			.host(Tester.getMysqlHost())
			.port(Tester.getMysqlPort())
			.user(Tester.getMysqlUser())
			.password(Tester.getMysqlPassword())
			.jdbcConnection(cf->{
				MysqlDataSource dataSource = new MysqlDataSource();
				dataSource.setURL(cf.getUrl());
				dataSource.setUser(cf.getUser());
				dataSource.setPassword(cf.getPassword());
				return dataSource;
			});

	@BeforeClass
	public static void setUpBeforeClass() {
		Assume.assumeTrue(Tester.testMySqlConnection());
		Tester.removeLocalMysqlDatabase("conveyor_db");

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
		Cart<Integer,String,String> c2 = new ShoppingCart<Integer, String, String>(2, "value2", "label",System.currentTimeMillis()+1000000);
		p.savePart(p.nextUniquePartId(), c1);
		p.savePart(p.nextUniquePartId(), c2);

		p.archiveExpiredParts();
		
		Cart<Integer,?,String> rc1 = p.getPart(1);
		Cart<Integer,?,String> rc2 = p.getPart(2);
		System.out.println(rc1);
		System.out.println(rc2);
		//assertNotNull(rc1);
		//assertNotNull(rc2);

		Thread.sleep(3000);
		p.archiveExpiredParts();

		Cart<Integer,?,String> rc12 = p.getPart(1);
		Cart<Integer,?,String> rc22 = p.getPart(2);
		assertNull(rc12);
		assertNotNull(rc22);
		
	}
	
	@Test
	public void testSaveAndRead() throws Exception {
		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
				.autoInit(false)
				.partTable("PART1")
				.completedLogTable("COMPLETED_LOG1")
				.user(Tester.getMysqlUser())
				.password(Tester.getMysqlPassword())
				.addField(String.class, "ADDON")
				.addUniqueFields("ADDON")
				.deleteArchiving()
				;
		
		JdbcPersistence<Integer> p = jpb.init().build();
		assertNotNull(p);
		p.archiveAll();
		Cart<Integer,String,String> cartA = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartA.addProperty("ADDON", "A");
		Cart<Integer,String,String> cartB = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartB.addProperty("ADDON", "B");
		Cart<Integer,String,String> cartC = new ShoppingCart<Integer, String, String>(100, "test", "label");
		cartC.addProperty("ADDON", "A");

		p.savePart(1, cartA);
		p.savePart(2, cartB);
		Cart restored = p.getPart(1);
		assertNotNull(restored);
		System.out.println(restored);
		assertEquals(100, restored.getKey());
		assertEquals("label", restored.getLabel());
		assertEquals("test", restored.getValue());
		try {
			p.savePart(3, cartC);
			fail("Must not be saved!");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	static class BalanceBuilder implements Supplier<Double> {
		Double summ = 0.00;
		@Override
		public Double get() {
			return summ;
		}
		public void add(Double m) {
			summ += m;
		}
		public void withdraw(Double m) {
			if(m > summ) {
				throw new KeepRunningConveyorException("Withdraw of "+m+" rejected. Balance is low");
			}
			summ -= m;
		}
	}

	enum BALANCE_OPERATION implements SmartLabel<BalanceBuilder>{
		ADD{{ setter = (bb,val)->bb.add((Double)val); }},
		WITHDRAW{{ setter = (bb,val)->bb.withdraw((Double)val); }},
		CLOSE{{ setter = (bb,val)->{}; }}
		;
		BiConsumer<BalanceBuilder, Object> setter;
		@Override
		public BiConsumer<BalanceBuilder, Object> get() {
			return setter;
		}
	}

	@Test
	public void testConvWithTransactionField() throws Exception {

		LastResultReference<Integer,Double> result = new LastResultReference();

		JdbcPersistenceBuilder<Integer> jpb = JdbcPersistenceBuilder.presetInitializer("mysql", Integer.class)
				.autoInit(false)
				.partTable("BALANCE")
				.completedLogTable("BALANCE_LOG")
				.user(Tester.getMysqlUser())
				.password(Tester.getMysqlPassword())
				.addField(Long.class, "TRANSACTION_ID")
				.addUniqueFields("TRANSACTION_ID")
				;
		AssemblingConveyor<Integer, BALANCE_OPERATION, Double> balance = new AssemblingConveyor<>();

		PersistentConveyor<Integer, BALANCE_OPERATION, Double> persistentBalance = jpb.init().build().wrapConveyor(balance);

		persistentBalance.setReadinessEvaluator(Conveyor.getTesterFor(balance).accepted(BALANCE_OPERATION.CLOSE));
		persistentBalance.setBuilderSupplier(BalanceBuilder::new);
		persistentBalance.resultConsumer(result).set();
		persistentBalance.scrapConsumer(LogScrap.stdErr(balance)).set();

		PartLoader<Integer, BALANCE_OPERATION> loader = persistentBalance
				.part()
				.id(1);

		loader
				.label(BALANCE_OPERATION.ADD)
				.value(100.00)
				.addProperty("TRANSACTION_ID",1)
				.place();
		loader
				.label(BALANCE_OPERATION.ADD)
				.value(100.00)
				.addProperty("TRANSACTION_ID",1) //duplicate
				.place();
		loader
				.label(BALANCE_OPERATION.ADD)
				.value(200.00)
				.addProperty("TRANSACTION_ID",2)
				.place();
		loader
				.label(BALANCE_OPERATION.WITHDRAW)
				.value(50.00)
				.addProperty("TRANSACTION_ID",3)
				.place();
		loader
				.label(BALANCE_OPERATION.WITHDRAW)
				.value(500.00) //over the limit
				.addProperty("TRANSACTION_ID",4)
				.place();
		loader
				.label(BALANCE_OPERATION.CLOSE)
				.addProperty("TRANSACTION_ID",5)
				.place().join();
		System.out.println(result);
		assertEquals(250.00,result.getCurrent(),0.001);
	}
	
}
