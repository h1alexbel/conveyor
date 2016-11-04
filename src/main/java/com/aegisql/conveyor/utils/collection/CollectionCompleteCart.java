package com.aegisql.conveyor.utils.collection;

import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.cart.AbstractCart;
import com.aegisql.conveyor.cart.Cart;

public class CollectionCompleteCart <K,V> extends AbstractCart<K, V, CollectionBuilderLabel<V>> {

	private static final long serialVersionUID = -4120525801809562774L;

	public CollectionCompleteCart(K k, long ttl, TimeUnit timeUnit) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), ttl, timeUnit);
	}

	public CollectionCompleteCart(K k, long expiration) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel(), expiration);
	}

	public CollectionCompleteCart(K k) {
		super(k, null, CollectionBuilderLabel.completeCollectionLabel());
	}

	@Override
	public Cart<K, V, CollectionBuilderLabel<V>> copy() {
		return new CollectionCompleteCart<K,V>(getKey(), getExpirationTime());
	}

}
