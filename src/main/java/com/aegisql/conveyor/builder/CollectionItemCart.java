package com.aegisql.conveyor.builder;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.cart.AbstractCart;

public class CollectionItemCart <K,V> extends AbstractCart<K, V, CollectionBuilderLabel<V>> {

	private static final long serialVersionUID = -4120525801809562774L;

	public CollectionItemCart(K k, V v, long ttl, TimeUnit timeUnit) {
		super(k, v, CollectionBuilderLabel.addItemLabel(), ttl, timeUnit);
	}

	public CollectionItemCart(K k, V v, long expiration) {
		super(k, v, CollectionBuilderLabel.addItemLabel(), expiration);
	}

	public CollectionItemCart(K k, V v) {
		super(k, v, CollectionBuilderLabel.addItemLabel());
	}

	public CollectionItemCart<K,V> nextItem(V newValue) {
		return new CollectionItemCart<>(this.getKey(), newValue, this.getExpirationTime());
	}
	
	public CollectionCompleteCart<K,V> compelte() {
		return new CollectionCompleteCart<>(this.getKey(), this.getExpirationTime());		
	}

}
