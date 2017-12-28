package com.aegisql.conveyor.config;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.aegisql.conveyor.Conveyor;

public class ConveyorConfigurationTest {

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
	public void test1() throws IOException {
		ConveyorConfiguration.build("src/test/resources/test1.yaml");
		Conveyor<Integer,String,String> c = Conveyor.byName("test1");
		assertNotNull(c);
	}

	@Test
	public void test2() throws IOException {
		ConveyorConfiguration.build("src/test/resources/test2.properties");
		Conveyor<Integer,String,String> c = Conveyor.byName("test2");
		assertNotNull(c);
	}

}