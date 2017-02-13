package com.aegisql.conveyor.utils.collection;

import java.util.Collection;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.loaders.MultiKeyPartLoader;
import com.aegisql.conveyor.loaders.PartLoader;

// TODO: Auto-generated Javadoc
/**
 * The Class CollectionConveyor.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class CollectionConveyor <K,V> extends AssemblingConveyor<K, SmartLabel<CollectionBuilder<V>>, Collection<V>> {

	/**
	 * Instantiates a new collection conveyor.
	 */
	public CollectionConveyor() {
		super();
		this.setName("CollectionConveyor");
	}

	public final SmartLabel<CollectionBuilder<V>> ITEM = SmartLabel.of((b,v)->{
		CollectionBuilder<V> builder = (CollectionBuilder<V>)b;
		CollectionBuilder.add(builder, (V)v);
	});

	public final SmartLabel<CollectionBuilder<V>> COMPLETE = SmartLabel.of((b,v)->{
		CollectionBuilder<V> builder = (CollectionBuilder<V>)b;
		CollectionBuilder.complete(builder, (V)v);
	});

	@Override
	public <X> PartLoader<K, SmartLabel<CollectionBuilder<V>>, X, Collection<V>, Boolean> part() {
		return (PartLoader<K, SmartLabel<CollectionBuilder<V>>, X, Collection<V>, Boolean>) super.part().label(ITEM);
	}


	public <X> MultiKeyPartLoader<K, SmartLabel<CollectionBuilder<V>>, X, Collection<V>, Boolean> multiKeyPart() {
		return (MultiKeyPartLoader<K, SmartLabel<CollectionBuilder<V>>, X, Collection<V>, Boolean>) super.multiKeyPart().label(ITEM);
	}

	
}
