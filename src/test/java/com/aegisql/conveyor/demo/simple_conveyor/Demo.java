/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.simple_conveyor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.demo.ThreadPool;

public class Demo {

	public static void main(String[] args) throws ParseException, InterruptedException, ExecutionException {

		ThreadPool pool                   = new ThreadPool();
		SimpleDateFormat format           = new SimpleDateFormat("yyyy-MM-dd");
		AtomicReference<Person> personRef = new AtomicReference<>();
		
		// I - Create conveyor
		// Generic types:
		// Integer - type of the unique build ID
		// String  - type of the message labels
		// Person  - type of the Product
		Conveyor<Integer, String, Person> conveyor = new AssemblingConveyor<>();
		
		// II - Tell conveyor how to create the Builder
		conveyor.setBuilderSupplier(PersonBuilder::new);
		
		// III - Explain conveyor how to process Building Parts
		conveyor.setDefaultCartConsumer(Conveyor.getConsumerFor(conveyor)
				.when("FirstName", (builder,value)->{
					((PersonBuilder) builder).setFirstName((String)value);
				})
				.when("LastName", (builder,value)->{
					((PersonBuilder) builder).setLastName((String)value);
				})
				//note how we tell the filter to ignore label case
				.filter((l)->"dateofbirth".equalsIgnoreCase(l), (builder,value)->{
					((PersonBuilder) builder).setDateOfBirth((Date) value);
				})
			);
		
		// IV - Build is ready when builder accepted three different pieces of data
		conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(3));
		
		// V - Tell conveyor where to put created Person object
		//     Product receiver should not block the thread 
		conveyor.setResultConsumer( bin-> personRef.set(bin.product) );
		
		// VI - Optionally: retrieve completable future of the build
		CompletableFuture<Person> future = conveyor.createBuildFuture(1);
		
		// VII - Send data to conveyor asynchronously
		pool.runAsynchWithDelay(10,()->{
			conveyor.add(1, "John", "FirstName");
			}
		);
		pool.runAsynchWithDelay(10,()->{
			conveyor.add(1, "Silver", "LastName");
			}
		);
		pool.runAsynchWithDelay(10,()->{
			try {
				conveyor.add(1, format.parse("1695-11-10"), "DateOfBirth");
			} catch (Exception e) {}
			}
		);
		// No guess work. Method get() of the future
		// Will return result as soon as its built
		// and sent to the consumer
		Person person = future.get();
		
		System.out.println( "Person from asynchronous source: "+personRef.get() );
		System.out.println( "Person synchronized: "+person );
		
		pool.shutdown();
		conveyor.stop();
	}
	
	@Test
	public void test() throws Exception {
		main(null);
	}


}