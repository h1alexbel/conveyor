package com.aegisql.conveyor.cart;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class FutureCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class FutureCart<K, B, L> extends AbstractCart<K, CompletableFuture<B>, L> implements Supplier<CompletableFuture<B>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202262573406558L;

	/**
	 * Instantiates a new future cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param expiration the expiration
	 */
	public FutureCart(K k, CompletableFuture<B> v, long expiration) {
		super(k, v, null, expiration);
		Objects.requireNonNull(k);
	}

	public FutureCart(K k, CompletableFuture<B> v, long creation, long expiration) {
		super(k, v, null, creation, expiration);
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public CompletableFuture<B> get() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K, CompletableFuture<B>, L> copy() {
		return new FutureCart<K, B, L>(getKey(), getValue(), getExpirationTime());
	}

	@Override
	public ScrapConsumer<K, Cart<K, CompletableFuture<B>, L>> getScrapConsumer() {
		return super.getScrapConsumer().andThen(bin->{
			CompletableFuture<B> f = bin.scrap.getValue();
			if(bin.error !=null) {
				f.completeExceptionally(bin.error);
			} else {
				f.cancel(true);
			}
		});
	}
	
	
}
