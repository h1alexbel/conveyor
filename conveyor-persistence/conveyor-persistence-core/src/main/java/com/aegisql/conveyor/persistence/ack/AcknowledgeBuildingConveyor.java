package com.aegisql.conveyor.persistence.ack;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.persistence.cleanup.PersistenceCleanupBatchConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;

public class AcknowledgeBuildingConveyor <K> extends AssemblingConveyor<K, SmartLabel<AcknowledgeBuilder<K>>, List<Long>> {
	
	public final SmartLabel<AcknowledgeBuilder<K>> CART     = SmartLabel.of("CART", (b,cart)->{ AcknowledgeBuilder.processCart(b, (Cart<K,?,?>)cart); });
	public final SmartLabel<AcknowledgeBuilder<K>> READY    = SmartLabel.of("READY", (b,key)->{ AcknowledgeBuilder.keyReady(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K>> COMPLETE = SmartLabel.of("COMPLETE", (b,status)->{ AcknowledgeBuilder.complete(b, (Status)status); });
	public final SmartLabel<AcknowledgeBuilder<K>> REPLAY   = SmartLabel.of("REPLAY", (b,key)->{ AcknowledgeBuilder.replay(b, (K)key); });
	public final SmartLabel<AcknowledgeBuilder<K>> MODE     = SmartLabel.of("MODE", (b,mode)->{ AcknowledgeBuilder.setMode(b, (Boolean)mode); });
	
	private final AtomicBoolean initializationMode = new AtomicBoolean(true);

	public <L,OUT> AcknowledgeBuildingConveyor(Persistence<K> persistence, Conveyor<K, L, OUT> forward, PersistenceCleanupBatchConveyor<K> cleaner) {
		super();
		this.setName("AcknowledgeBuildingConveyor<"+(forward == null ? "":forward.getName())+">");
		this.setBuilderSupplier( () -> new AcknowledgeBuilder<>(persistence, forward)  );
		this.resultConsumer(bin->{
			if(cleaner != null) {
				cleaner.part().label(cleaner.KEY).value(bin.key).place();
				cleaner.part().label(cleaner.CART_IDS).value(bin.product).place();
			}
		}).set();
	}
	
	public void setInitializationMode(boolean mode) {
		this.initializationMode.set(mode);
	}
	
}
