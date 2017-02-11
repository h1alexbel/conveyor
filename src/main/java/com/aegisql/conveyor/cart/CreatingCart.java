package com.aegisql.conveyor.cart;

import java.util.Objects;
import java.util.function.Supplier;

import com.aegisql.conveyor.BuilderSupplier;

// TODO: Auto-generated Javadoc
/**
 * The Class CreatingCart.
 *
 * @param <K> the key type
 * @param <B> the generic type
 * @param <L> the generic type
 */
public class CreatingCart<K, B, L> extends AbstractCart<K, BuilderSupplier<B>, L> implements Supplier<BuilderSupplier<B>> {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4985202264573416558L;

	/**
	 * Instantiates a new creating cart.
	 *
	 * @param k the k
	 * @param v the v
	 * @param cretion the creation time
	 * @param expiration the expiration time
	 */
	public CreatingCart(K k, BuilderSupplier<B> v, long creation, long expiration) {
		super(k, v, null, creation, expiration);
		Objects.requireNonNull(k);
	}

	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@Override
	public BuilderSupplier<B> get() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.cart.Cart#copy()
	 */
	@Override
	public Cart<K,BuilderSupplier<B>,L> copy() {
		return new CreatingCart<K,B,L>(getKey(),getValue(),getCreationTime(), getExpirationTime());
	}

}
