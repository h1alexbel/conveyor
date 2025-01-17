package com.aegisql.conveyor.consumers.scrap;

import java.util.concurrent.atomic.AtomicReference;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;

// TODO: Auto-generated Javadoc
/**
 * The Class LastScrapReference.
 *
 * @param <K> the key type
 */
public class LastScrapReference<K> implements ScrapConsumer<K,Object> {

	/** The ref. */
	AtomicReference<ScrapBin<K,Object>> ref = new AtomicReference<>();
	
	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ScrapBin<K,Object> bin) {
		ref.set(bin);
	}
	
	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public ScrapBin<K,Object> getCurrent() {
		return ref.get();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapReference [" + ref.get() + "]";
	}

	/**
	 * Of.
	 *
	 * @param <K> the key type
	 * @param conv the conv
	 * @return the last scrap reference
	 */
	public static <K> LastScrapReference<K> of(Conveyor<K, ?, ?> conv) {
		return new LastScrapReference<>();
	}
	
}
