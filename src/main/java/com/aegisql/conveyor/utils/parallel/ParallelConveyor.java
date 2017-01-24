/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.utils.parallel;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderLoader;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.CommandLabel;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.FutureLoader;
import com.aegisql.conveyor.FutureSupplier;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ParallelConveyorMBean;
import com.aegisql.conveyor.PartLoader;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.State;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.CreatingCart;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.cart.ShoppingCart;
import com.aegisql.conveyor.cart.command.GeneralCommand;

// TODO: Auto-generated Javadoc
/**
 * The Class ParallelConveyor.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the label type
 * @param <OUT> the Product type
 */
public abstract class ParallelConveyor<K, L, OUT> implements Conveyor<K, L, OUT> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ParallelConveyor.class);

	/** The expiration collection interval. */
	protected long expirationCollectionInterval = 0;
	
	/** The expiration collection unit. */
	protected TimeUnit expirationCollectionUnit = TimeUnit.MILLISECONDS;
	
	/** The builder timeout. */
	protected long builderTimeout = 0;
	
	/** The on timeout action. */
	protected Consumer<Supplier<? extends OUT>> timeoutAction;
	
	/** The running. */
	protected volatile boolean running = true;

	/** The conveyors. */
	protected final List<Conveyor<K, L, OUT>> conveyors = new ArrayList<>();

	/** The pf. */
	protected int pf;
	
	/** The balancing command. */
	protected Function<GeneralCommand<K,?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand;
	
	/** The balancing cart. */
	protected Function<Cart<K,?,L>, List<? extends Conveyor<K, L, OUT>>> balancingCart;

	/** The name. */
	protected String name = "ParallelConveyor";

	/** The l balanced. */
	protected boolean lBalanced = false;

	/** The accepted labels. */
	private Set<L> acceptedLabels = new HashSet<>();

	/** The forwarding results. */
	protected boolean forwardingResults = false;
	
	/** The object name. */
	private ObjectName objectName;
	
	/** The Constant mBeanServer. */
	private final static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
	
	/** The builder supplier. */
	protected BuilderSupplier<OUT> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};
	
	/**
	 * Instantiates a new parallel conveyor.
	 */
	protected ParallelConveyor() {
		this.setMbean(name);
	}
	
	
	@Override
	public <X> PartLoader<K, L, X, OUT, Boolean> part() {
		return new PartLoader<K,L,X,OUT,Boolean>(cl -> {
			return place(new ShoppingCart<K,Object,L>(cl.key, cl.partValue, cl.label, cl.expirationTime));
		});
	}

	@Override
	public BuilderLoader<K, OUT, Boolean> build() {
		return new BuilderLoader<K, OUT, Boolean> (cl -> {
			CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key,cl.value,cl.expirationTime);
			return place(cart);
		});
	}

	@Override
	public BuilderLoader<K, OUT, OUT> buildFuture() {
		return new BuilderLoader<K, OUT, OUT> (cl -> {
			BuilderSupplier<OUT> bs = cl.value;
			CompletableFuture<OUT> future = new CompletableFuture<OUT>();
			if(bs == null) {
				bs = builderSupplier.withFuture(future);
			} else {
				bs = bs.withFuture(future);
			}
			CreatingCart<K, OUT, L> cart = new CreatingCart<K, OUT, L>(cl.key, bs ,cl.expirationTime);
			FutureSupplier supplier = (FutureSupplier<OUT>) cart.getValue();
			CompletableFuture<Boolean> cartFuture = place(  cart );		
			if(cartFuture.isCancelled()) {
				supplier.getFuture().cancel(true);
			}
			return supplier.getFuture();
		});
	}

	@Override
	public FutureLoader<K, OUT> future() {
		return new FutureLoader<K, OUT> (cl -> {
			CompletableFuture<OUT> future = new CompletableFuture<OUT>();
			FutureCart<K, OUT, L> cart = new FutureCart<K, OUT, L>(cl.key,future,cl.expirationTime);
			CompletableFuture<Boolean> cartFuture = this.place( cart );
			if(cartFuture.isCancelled()) {
				future.cancel(true);
			}
			return future;
		});
	}

	
	/**
	 * Instantiates a new parallel conveyor.
	 *
	 * @param <V> the value type
	 * @param cart the cart
	 * @return the completable future
	 */
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(com.aegisql.conveyor.Cart)
	 */
	@Override
	public abstract <V> CompletableFuture<Boolean> addCommand(GeneralCommand<K, V> cart);
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, long)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long expirationTime) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, expirationTime ){ private static final long serialVersionUID = 1L;} );
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, long ttl, TimeUnit unit) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, ttl, unit){ private static final long serialVersionUID = 1L;} );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, java.time.Duration)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Duration duration) {
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, duration){ private static final long serialVersionUID = 1L;} );		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCommand(java.lang.Object, java.lang.Object, com.aegisql.conveyor.CommandLabel, java.time.Instant)
	 */
	@Override
	public <V> CompletableFuture<Boolean> addCommand(K key, V value, CommandLabel label, Instant instant) {		
		return this.addCommand( new GeneralCommand<K,V>(key, value, label, instant){ private static final long serialVersionUID = 1L;} );
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#add(com.aegisql.conveyor.Cart)
	 */
	@Override
	public abstract <V> CompletableFuture<Boolean> place(Cart<K,V,L> cart);

	/**
	 * Creates the build future.
	 *
	 * @param cartSupplier the cart supplier
	 * @return the completable future
	 */
	protected CompletableFuture<OUT> createBuildFuture(Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier) {
		return createBuildFutureWithCart(cartSupplier,builderSupplier);
	}
	
	
	/**
	 * Creates the build with cart.
	 *
	 * @param <V> the value type
	 * @param cart the cart
	 * @return the completable future
	 */
	protected abstract <V> CompletableFuture<Boolean> createBuildWithCart(Cart<K,V,L> cart);

	/**
	 * Creates the build future with cart.
	 *
	 * @param cartSupplier the cart supplier
	 * @param builderSupplier the builder supplier
	 * @return the completable future
	 */
	protected abstract CompletableFuture<OUT> createBuildFutureWithCart(Function<BuilderAndFutureSupplier<OUT>, CreatingCart<K, OUT, L>> cartSupplier, BuilderSupplier<OUT> builderSupplier);

	/**
	 * Gets the future by cart.
	 *
	 * @param futureCart the future cart
	 * @return the future by cart
	 */
	protected abstract CompletableFuture<OUT> getFutureByCart(FutureCart<K,OUT,L> futureCart);
		
	/**
	 * Gets the number of conveyors.
	 *
	 * @return the number of conveyors
	 */
	public int getNumberOfConveyors() {
		return conveyors.size();
	}
	
	/**
	 * Gets the collector size.
	 *
	 * @param idx the idx
	 * @return the collector size
	 */
	public int getCollectorSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getCollectorSize();
		}
	}

	/**
	 * Gets the input queue size.
	 *
	 * @param idx the idx
	 * @return the input queue size
	 */
	public int getInputQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getInputQueueSize();
		}
	}

	/**
	 * Gets the delayed queue size.
	 *
	 * @param idx the idx
	 * @return the delayed queue size
	 */
	public int getDelayedQueueSize(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getDelayedQueueSize();
		}
	}

	/**
	 * Sets the scrap consumer.
	 *
	 * @param scrapConsumer the scrap consumer
	 */
	public void setScrapConsumer(Consumer<ScrapBin<?, ?>> scrapConsumer) {
		this.conveyors.forEach(conv->conv.setScrapConsumer(scrapConsumer));
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		this.conveyors.forEach(conv->conv.stop());
	}

	/**
	 * Gets the expiration collection interval.
	 *
	 * @return the expiration collection interval
	 */
	public long getExpirationCollectionIdleInterval() {
		return expirationCollectionInterval;
	}
	
	/**
	 * Gets the expiration collection idle time unit.
	 *
	 * @return the expiration collection idle time unit
	 */
	public TimeUnit getExpirationCollectionIdleTimeUnit() {
		return expirationCollectionUnit;
	}

	/**
	 * Sets the expiration collection interval.
	 *
	 * @param expirationCollectionInterval the expiration collection interval
	 * @param unit the unit
	 */
	public void setIdleHeartBeat(long expirationCollectionInterval, TimeUnit unit) {
		this.expirationCollectionInterval = expirationCollectionInterval;
		this.expirationCollectionUnit = unit;
		this.conveyors.forEach(conv->conv.setIdleHeartBeat(expirationCollectionInterval,unit));
	}

	/**
	 * Gets the builder timeout.
	 *
	 * @return the builder timeout
	 */
	public long getDefaultBuilderTimeout() {
		return builderTimeout;
	}

	/**
	 * Sets the builder timeout.
	 *
	 * @param builderTimeout the builder timeout
	 * @param unit the unit
	 */
	public void setDefaultBuilderTimeout(long builderTimeout, TimeUnit unit) {
		this.builderTimeout = unit.toMillis(builderTimeout);
		this.conveyors.forEach(conv->conv.setDefaultBuilderTimeout(builderTimeout,unit));
	}

	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param timeout the timeout
	 * @param unit the unit
	 */
	public void rejectUnexpireableCartsOlderThan(long timeout, TimeUnit unit) {
		this.conveyors.forEach(conv->conv.rejectUnexpireableCartsOlderThan(timeout,unit));
	}

	
	/**
	 * Checks if is on timeout action.
	 *
	 * @return true, if is on timeout action
	 */
	public boolean isOnTimeoutAction() {
		return timeoutAction != null;
	}

	/**
	 * Sets the on timeout action.
	 *
	 * @param timeoutAction the new on timeout action
	 */
	public void setOnTimeoutAction(Consumer<Supplier<? extends OUT>> timeoutAction) {
		this.timeoutAction = timeoutAction;
		this.conveyors.forEach(conv->conv.setOnTimeoutAction(timeoutAction));
	}

	/**
	 * Sets the result consumer.
	 *
	 * @param resultConsumer the new result consumer
	 */
	public void setResultConsumer(Consumer<ProductBin<K,OUT>> resultConsumer) {
		this.conveyors.forEach(conv->conv.setResultConsumer(resultConsumer));
	}

	/**
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public void setDefaultCartConsumer(LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer) {
		this.conveyors.forEach(conv->conv.setDefaultCartConsumer(cartConsumer));
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param ready the ready
	 */
	public void setReadinessEvaluator(BiPredicate<State<K,L>, Supplier<? extends OUT>> ready) {
		this.conveyors.forEach(conv->conv.setReadinessEvaluator(ready));
	}

	/**
	 * Sets the readiness evaluator.
	 *
	 * @param readiness the ready
	 */
	public void setReadinessEvaluator(Predicate<Supplier<? extends OUT>> readiness) {
		this.conveyors.forEach(conv->conv.setReadinessEvaluator(readiness));
	}

	/**
	 * Sets the builder supplier.
	 *
	 * @param builderSupplier the new builder supplier
	 */
	public void setBuilderSupplier(BuilderSupplier<OUT> builderSupplier) {
		this.builderSupplier = builderSupplier;
		this.conveyors.forEach(conv->conv.setBuilderSupplier(builderSupplier));
	}

	/**
	 * Sets the name.
	 *
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
		this.setMbean(name);
		int i = 0;
		for(Conveyor<K,L,OUT> conv: conveyors) {
			conv.setName(name+" ["+i+"]");
			i++;
		}
	}
	
	/**
	 * Checks if is running.
	 *
	 * @return true, if is running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Checks if is running.
	 *
	 * @param idx the idx
	 * @return true, if is running
	 */
	public boolean isRunning(int idx) {
		if(idx < 0 || idx >= pf) {
			return false;
		} else {
			return conveyors.get(idx).isRunning();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addCartBeforePlacementValidator(java.util.function.Consumer)
	 */
	//Think about consequences
	public void addCartBeforePlacementValidator(Consumer<Cart<K, ?, L>> cartBeforePlacementValidator) {
		if(cartBeforePlacementValidator != null) {
			this.conveyors.forEach(conv->conv.addCartBeforePlacementValidator(cartBeforePlacementValidator));
		}
	}


	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyEvictionAction(java.util.function.Consumer)
	 */
	public void addBeforeKeyEvictionAction(Consumer<K> keyBeforeEviction) {
		if(keyBeforeEviction != null) {
			this.conveyors.forEach(conv->conv.addBeforeKeyEvictionAction(keyBeforeEviction));
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#addBeforeKeyReschedulingAction(java.util.function.BiConsumer)
	 */
	public void addBeforeKeyReschedulingAction(BiConsumer<K,Long> keyBeforeRescheduling) {
		if(keyBeforeRescheduling != null) {
			this.conveyors.forEach(conv->conv.addBeforeKeyReschedulingAction(keyBeforeRescheduling));
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getExpirationTime(java.lang.Object)
	 */
	public long getExpirationTime(K key) {
		if(!lBalanced) {
			return this.conveyors.get(0).getExpirationTime(key);
		} else {
			throw new RuntimeException("Method cannot be called for L-Balanced conveyor '"+name+"'. Use getExpirationTime(K key, L label)");
		}
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getCollectorSize()
	 */
	@Override
	public int getCollectorSize() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getInputQueueSize()
	 */
	@Override
	public int getInputQueueSize() {
		return -1;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getDelayedQueueSize()
	 */
	@Override
	public int getDelayedQueueSize() {
		return -1;
	}

	/**
	 * Sets the balancing command algorithm.
	 *
	 * @param balancingCommand the balancing command
	 */
	public void setBalancingCommandAlgorithm(Function<GeneralCommand<K, ?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand) {
		this.balancingCommand = balancingCommand;
	}

	/**
	 * Sets the balancing cart algorithm.
	 *
	 * @param balancingCart the balancing cart
	 */
	public void setBalancingCartAlgorithm(Function<Cart<K, ?, L>, List<? extends Conveyor<K, L, OUT>>> balancingCart) {
		this.balancingCart = balancingCart;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isLBalanced()
	 */
	@Override
	public boolean isLBalanced() {
		return lBalanced;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getAcceptedLabels()
	 */
	@Override
	public Set<L> getAcceptedLabels() {
		return acceptedLabels ;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#acceptLabels(java.lang.Object[])
	 */
	@Override
	public void acceptLabels(L... labels) {
		if(labels != null && labels.length > 0) {
			for(L l:labels) {
				acceptedLabels.add(l);
			}
			this.addCartBeforePlacementValidator(cart->{
				if( ! acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException("Parallel Conveyor '"+this.name+"' cannot process label '"+cart.getLabel()+"'");					
				}
			});
			
			lBalanced = true;
			
		}		
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ParallelConveyor [name=" + name + ", pf=" + pf + ", lBalanced=" + lBalanced + "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#forwardPartialResultTo(java.lang.Object, com.aegisql.conveyor.Conveyor)
	 */
	public void forwardPartialResultTo(L partial, Conveyor<K,L,OUT> conv) {
		this.forwardingResults  = true;
		this.setResultConsumer(bin->{
			LOG.debug("Forward {} from {} to {} {}",partial,this.name,conv.getName(),bin.product);
			Cart<K,OUT,L> partialResult = new ShoppingCart<>(bin.key, bin.product, partial, bin.remainingDelayMsec,TimeUnit.MILLISECONDS);
			conv.place( partialResult );
		});
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpiration(boolean)
	 */
	@Override
	public void enablePostponeExpiration(boolean flag) {
		this.conveyors.forEach(conv -> conv.enablePostponeExpiration(flag));
	}

	
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#enablePostponeExpirationOnTimeout(boolean)
	 */
	@Override
	public void enablePostponeExpirationOnTimeout(boolean flag) {
		this.conveyors.forEach(conv -> conv.enablePostponeExpirationOnTimeout(flag));
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#setExpirationPostponeTime(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public void setExpirationPostponeTime(long time, TimeUnit unit) {
		this.conveyors.forEach(conv -> conv.setExpirationPostponeTime(time, unit));	
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Conveyor#isForwardingResults()
	 */
	@Override
	public boolean isForwardingResults() {
		return forwardingResults;
	}

	/**
	 * Sets the mbean.
	 *
	 * @param name the new mbean
	 */
	protected void setMbean(String name) {
		try {
			final ParallelConveyor<K,L,OUT> thisConv = this;

			Object mbean = new StandardMBean(new ParallelConveyorMBean() {
				@Override
				public String getName() {
					return name;
				}
				@Override
				public String getType() {
					return thisConv.getClass().getSimpleName();
				}
				@Override
				public int getInnerConveyorsCount() {
					return conveyors.size();
				}
				@Override
				public boolean isRunning() {
					return thisConv.running;
				}
			}, ParallelConveyorMBean.class, false);
			
			ObjectName newObjectName = new ObjectName("com.aegisql.conveyor:type="+name);
			synchronized(mBeanServer) {
				if(this.objectName == null) {
					this.objectName = newObjectName;
					this.setMbean(name);
				}
				if(mBeanServer.isRegistered(this.objectName)) {
					mBeanServer.unregisterMBean(objectName);
					this.objectName = newObjectName;
					this.setMbean(name);
				} else {
					mBeanServer.registerMBean(mbean, newObjectName);
					this.objectName = newObjectName;
				}
			}
		} catch (Exception e) {
			LOG.error("MBEAN error",e);
			throw new RuntimeException(e);
		}
	}
	

}
