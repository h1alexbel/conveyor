package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.converters.StringConverter;
import com.aegisql.conveyor.persistence.jdbc.harness.Tester;
import org.junit.*;

import java.sql.*;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LearnDerbyTest {

	@BeforeClass
	public static void setUpBeforeClass() {
		Tester.removeDirectory("testDb1");
	}

	@AfterClass
	public static void tearDownAfterClass() {
	}

	@Before
	public void setUp() {
		Tester.sleep(1000);
	}

	@After
	public void tearDown() {
	}
		
	JdbcPersistenceBuilder<String> persistenceBuilder = JdbcPersistenceBuilder.presetInitializer("derby", String.class)
			.schema("testDb1").autoInit(true).setArchived().labelConverter(new StringConverter<String>(){
				@Override
				public String fromPersistence(String p) {
					return p;
				}

				@Override
				public String conversionHint() {
					return "L:String";
				}});

	@Test
	public void stackTrace() {
		assertEquals(Tester.getTestMethod(),"stackTrace");
		assertEquals(Tester.getTestClass(),"LearnDerbyTest");
	}

	@Test
	public void testDbConnection() throws Exception {
		String connectionURL = "jdbc:derby:testDb1;create=true";

	    Connection conn = DriverManager.getConnection(connectionURL);
	    Statement st = conn.createStatement();
	    try {
	    	st.execute("DROP TABLE TEST");
	    } catch(SQLException e) {
	    	
	    }
	    st.close();

	    st = conn.createStatement();
	    try {
	    	st.execute("CREATE TABLE TEST (ID INT PRIMARY KEY)");
	    } catch(SQLException e) {
	    	
	    }
	    st.close();
	    st = conn.createStatement();
	    st.execute("INSERT INTO TEST (ID) VALUES(100)");
	    st.close();

	    st = conn.createStatement();
	    ResultSet r = st.executeQuery("SELECT * FROM TEST");
	    r.next();
	    long id = r.getLong(1);
	    System.out.println(id);
	    r.close();
	    st.close();

	    conn.close();
	}
	
	@Test
	public void testPers() throws Exception {
		Persistence<String> p = persistenceBuilder.build();
		p.archiveAll();
		p.savePart(p.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "one", "ONE"));
		p.savePartId("1", 100);
		p.savePart(p.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "two", "TWO"));
		p.savePartId("1", 200);
		p.saveCompletedBuildKey("1");
		Collection<Cart<String,?,String>> allCarts = p.getAllParts();
		System.out.println(allCarts);
		assertNotNull(allCarts);
		assertEquals(2, allCarts.size());
		Collection<Long> ids = p.getAllPartIds("1");
		System.out.println(ids);
		assertNotNull(ids);
		assertEquals(2, ids.size());
		Collection<String> completed = p.getCompletedKeys();
		System.out.println(completed);
		assertNotNull(completed);
		assertEquals(1, completed.size());
		p.archiveParts(ids);
		p.archiveCompleteKeys(completed);
		p.archiveParts(ids);
		p.archiveAll();
	}

	@Test
	public void testPers2() throws Exception {
		Persistence<String> p1 = persistenceBuilder.build();
		p1.archiveAll();
		p1.savePart(p1.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "one", "ONE"));
		p1.savePartId("1", 100);
		p1.savePart(p1.nextUniquePartId(), new ShoppingCart<String, String, String>("1", "two", "TWO"));
		p1.savePartId("1", 200);
		p1.saveCompletedBuildKey("1");
		Collection<Cart<String,?,String>> allCarts = p1.getAllParts();
		System.out.println(allCarts);
		assertNotNull(allCarts);
		assertEquals(2, allCarts.size());
		Collection<Long> ids = p1.getAllPartIds("1");
		System.out.println(ids);
		assertNotNull(ids);
		assertEquals(2, ids.size());
		Collection<String> completed = p1.getCompletedKeys();
		System.out.println(completed);
		assertNotNull(completed);
		assertEquals(1, completed.size());
		
		Persistence<String> p2 = persistenceBuilder.build();

		Collection<Cart<String,?,String>> allCarts2 = p2.getAllParts();
		System.out.println(allCarts2);
		assertNotNull(allCarts2);
		assertEquals(2, allCarts2.size());
		Collection<Long> ids2 = p2.getAllPartIds("1");
		System.out.println(ids2);
		assertNotNull(ids2);
		assertEquals(2, ids2.size());
		Collection<String> completed2 = p2.getCompletedKeys();
		System.out.println(completed2);
		assertNotNull(completed2);
		assertEquals(1, completed2.size());

		
		p1.archiveParts(ids);
		p1.archiveCompleteKeys(completed);
		p1.archiveParts(ids);
		p1.archiveAll();
	}

	
}
