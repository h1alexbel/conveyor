/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.caching_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.Test;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.IgnoreResult;
import com.aegisql.conveyor.demo.ThreadPool;
import com.aegisql.conveyor.utils.caching.CachingConveyor;

public class Demo {
	
	public static void main(String[] args) throws InterruptedException {
		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		
		// I - Create labels describing building steps
		final SmartLabel<PersonBuilder> FIRST_NAME    = SmartLabel.of(PersonBuilder::setFirstName);
		final SmartLabel<PersonBuilder> LAST_NAME     = SmartLabel.of(PersonBuilder::setLastName);
		final SmartLabel<PersonBuilder> DATE_OF_BIRTH = SmartLabel.of(PersonBuilder::setDateOfBirth);
		
		// II - Create conveyor
		CachingConveyor<Integer, SmartLabel<PersonBuilder>, Person> conveyor = new CachingConveyor<>();
		
		conveyor.setDefaultBuilderTimeout(1, TimeUnit.HOURS);
		conveyor.setIdleHeartBeat(1, TimeUnit.SECONDS);
		
		// III - Tell it how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// IV - Tell it where to put the Product (asynchronously)
		conveyor.resultConsumer().first( IgnoreResult.of(conveyor) ).set();
		
		// IV - Add data to conveyor queue 
		pool.runAsynchWithDelay(10,()->{
			conveyor.part().id(1).value("John").label(FIRST_NAME).place();
			}
		);
		pool.runAsynchWithDelay(20,()->{
			conveyor.part().id(1).value("Silver").label(LAST_NAME).place();
			}
		);
		pool.runAsynchWithDelay(50,()->{
			try {
				conveyor.part().id(1).value(format.parse("1695-11-10")).label(DATE_OF_BIRTH).place();
			} catch (Exception e) {}
			}
		);
		
		Thread.sleep(100);
		
		Supplier<? extends Person> personSupplier = conveyor.getProductSupplier(1);
				
		System.out.println( personSupplier.get() );
		
		pool.shutdown();
		conveyor.stop();
	}

	@Test
	public void test() throws Exception {
		main(null);
	}
}
