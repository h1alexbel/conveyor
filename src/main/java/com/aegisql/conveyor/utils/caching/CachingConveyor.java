package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.cart.Cart;

public class CachingConveyor<K, L, OUT> extends AssemblingConveyor<K, L, OUT> {
	
	private final static Logger LOG = LoggerFactory.getLogger(CachingConveyor.class);
	
	public CachingConveyor() {
		super();
		this.setReadinessEvaluator( (k,l) -> false);
		this.setName("CachingConveyor");
		this.setOnTimeoutAction(builder->{
			LOG.debug("Timeout evicting builder {}",builder);			
		});
		this.setScrapConsumer(bin->{
			LOG.error("Evicting on error {}",bin);
		});
	}

	public Supplier<? extends OUT> getProductSupplier(K key) {
		BuildingSite<K, L, Cart<K, ?, L>, ? extends OUT> site = collector.get(key);
		if(site == null) {
			return null;
		} else {
			return site.getProductSupplier();
		}
	}
	
}
