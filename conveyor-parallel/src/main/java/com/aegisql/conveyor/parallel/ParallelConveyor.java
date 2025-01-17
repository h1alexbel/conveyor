/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.parallel;

import com.aegisql.conveyor.*;
import com.aegisql.conveyor.cart.*;
import com.aegisql.conveyor.cart.command.GeneralCommand;
import com.aegisql.conveyor.consumers.result.ForwardResult.ForwardingConsumer;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.loaders.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.*;

import static com.aegisql.conveyor.cart.LoadType.STATIC_PART;

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
	
	protected volatile boolean suspended = false;
	
	protected volatile CompletableFuture<Boolean> conveyorFuture = null;
	
	private final Object conveyorFutureLock = new Object();
	

	/** The conveyors. */
	protected final List<Conveyor<K, L, OUT>> conveyors = new ArrayList<>();

	/** The pf. */
	protected int pf;
	
	/** The balancing command. */
	protected Function<GeneralCommand<K,?>, List<? extends Conveyor<K, L, OUT>>> balancingCommand;
	
	/** The balancing cart. */
	protected Function<Cart<K,?,L>, List<? extends Conveyor<K, L, OUT>>> balancingCart;

	/** The name. */
	protected String name = "ParallelConveyor"+Thread.currentThread().getId();

	/** The l balanced. */
	protected boolean lBalanced = false;

	/** The accepted labels. */
	private final Set<L> acceptedLabels = new HashSet<>();

	/** The forwarding results. */
	protected boolean forwardingResults = false;
	
	/** The object name. */
	private ObjectName objectName;
	
	/** The builder supplier. */
	protected BuilderSupplier<OUT> builderSupplier = () -> {
		throw new IllegalStateException("Builder Supplier is not set");
	};
	
	/** The result consumer. */
	protected ResultConsumer <K,OUT> resultConsumer = null;

	private ScrapConsumer<K,?> scrapConsumer = null;
	
	/**
	 * Instantiates a new parallel conveyor.
	 */
	protected ParallelConveyor() {
		this.setMbean(name);
	}
	
	
	@Override
	public PartLoader<K, L> part() {
		return new PartLoader<>(cl -> {
            ShoppingCart<K, Object, L> cart = new ShoppingCart<>(cl.key, cl.partValue, cl.label, cl.creationTime, cl.expirationTime, cl.priority);
            cl.getAllProperties().forEach(cart::addProperty);
            return place(cart);
        });
	}
	
	@Override
	public StaticPartLoader<L> staticPart() {
		return new StaticPartLoader<>(cl -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("CREATE", cl.create);
            Cart<K, ?, L> staticPart = new ShoppingCart<>(null, cl.staticPartValue, cl.label, System.currentTimeMillis(), 0, properties, STATIC_PART, cl.priority);
            cl.getAllProperties().forEach(staticPart::addProperty);
            return place(staticPart);
        });
	}

	
	@Override
	public BuilderLoader<K, OUT> build() {
		return new BuilderLoader<>(cl -> {
            BuilderSupplier<OUT> bs = cl.value;
            if (bs == null) {
                bs = builderSupplier;
            }
            CreatingCart<K, OUT, L> cart = new CreatingCart<>(cl.key, bs, cl.creationTime, cl.expirationTime, cl.priority);
            cl.getAllProperties().forEach(cart::addProperty);
            return createBuildWithCart(cart);
        }, cl -> {
            BuilderSupplier<OUT> bs = cl.value;
            if (bs == null) {
                bs = builderSupplier;
            }
            return createBuildFutureWithCart(supplier -> new CreatingCart<>(cl.key, supplier, cl.creationTime, cl.expirationTime, cl.getAllProperties(), cl.priority), bs);//builderSupplier);
        });
	}

	@Override
	public FutureLoader<K, OUT> future() {
		return new FutureLoader<>(cl -> getFutureByCart(new FutureCart<>(cl.key, new CompletableFuture<>(), cl.creationTime, cl.expirationTime, cl.getAllProperties(), cl.priority)));
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
	public abstract <V> CompletableFuture<Boolean> command(GeneralCommand<K, V> cart);
	
	@Override
	public CommandLoader<K, OUT> command() {
		return new CommandLoader<>(this::command);
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
	 * Stop.
	 */
	public void stop() {
		this.running = false;
		this.conveyors.forEach(Conveyor::stop);
	}

	/**
	 * Complete tasks and stop.
	 */
	@Override
	public CompletableFuture<Boolean> completeAndStop() {
		if(this.conveyorFuture == null) {
			synchronized (this.conveyorFutureLock) {
				if(this.conveyorFuture == null) {
					this.conveyorFuture = new CompletableFuture<>();
					this.conveyorFuture.complete(true);
					for(Conveyor<K, L, OUT> c:conveyors) {
						CompletableFuture<Boolean> f = c.completeAndStop();
						this.conveyorFuture = this.conveyorFuture.thenCombine(f, (a,b)-> a && b);
					}
				}
			}
		}
		return this.conveyorFuture;
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
	
	@Override
	public void setDefaultBuilderTimeout(Duration duration) {
		this.setDefaultBuilderTimeout(duration.toMillis(), TimeUnit.MILLISECONDS);
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
	 * Sets the cart consumer.
	 *
	 * @param cartConsumer the cart consumer
	 */
	public <B extends Supplier<? extends OUT>> void setDefaultCartConsumer(LabeledValueConsumer<L, ?, B> cartConsumer) {
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
	@Override
	public void addBeforeKeyEvictionAction(Consumer<AcknowledgeStatus<K>> keyBeforeEviction) {
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
			acceptedLabels.addAll(Arrays.asList(labels));
			acceptedLabels.add(null);
			this.addCartBeforePlacementValidator(cart->{
				if( ! acceptedLabels.contains(cart.getLabel())) {
					throw new IllegalStateException("Parallel Conveyor '"+this.name+"' cannot process label '"+cart.getLabel()+"'");					
				}
			});
			conveyors.forEach(c->c.acceptLabels(labels));
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
			final ParallelConveyor<K,L,OUT> thisConv = this;

			Conveyor.register(this, new ParallelConveyorMBean() {
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
				@Override
				public <K, L, OUT> Conveyor<K, L, OUT> conveyor() {
					return (Conveyor<K, L, OUT>) thisConv;
				}
				@Override
				public void stop() {
					thisConv.stop();
				}
				@Override
				public void completeAndStop() {
					thisConv.completeAndStop();
				}
				@Override
				public void idleHeartBeatMsec(long msec) {
					if(msec > 0) {
						thisConv.setIdleHeartBeat(msec, TimeUnit.MILLISECONDS);
					}					
				}

				@Override
				public void defaultBuilderTimeoutMsec(long msec) {
					if(msec > 0) {
						thisConv.setDefaultBuilderTimeout(msec, TimeUnit.MILLISECONDS);
					}
				}

				@Override
				public void rejectUnexpireableCartsOlderThanMsec(long msec) {
					if(msec > 0) {
						thisConv.rejectUnexpireableCartsOlderThan(msec, TimeUnit.MILLISECONDS);
					}
				}

				@Override
				public void expirationPostponeTimeMsec(long msec) {
					if(msec > 0) {
						thisConv.setExpirationPostponeTime(msec, TimeUnit.MILLISECONDS);
					}					
				}
				@Override
				public boolean isSuspended() {
					return thisConv.suspended;
				}
				@Override
				public void suspend() {
					thisConv.suspend();
				}
				@Override
				public void resume() {
					thisConv.resume();
				}
			});
	}
	
	@Override
	public long getCartCounter() {
		long counter = 0;
		for(Conveyor<K, L, OUT> c: conveyors) {
			counter += c.getCartCounter();
		}
		return counter;
	}

	public long getCartCounter(int idx) {
		if(idx < 0 || idx >= pf) {
			return 0;
		} else {
			return conveyors.get(idx).getCartCounter();
		}
	}

	@Override
	public void setIdleHeartBeat(Duration duration) {
		this.setIdleHeartBeat(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	@Override
	public void rejectUnexpireableCartsOlderThan(Duration duration) {
		this.rejectUnexpireableCartsOlderThan(duration.toMillis(),TimeUnit.MILLISECONDS);
	}

	@Override
	public void setExpirationPostponeTime(Duration duration) {
		this.setExpirationPostponeTime(duration.toMillis(),TimeUnit.MILLISECONDS);
	}
	
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer() {
		return new ResultConsumerLoader<>(rcl->{
			Cart<K,?,L> cart;
			if(rcl.key != null) {
				cart = new ResultConsumerCart<>(rcl.key, rcl.consumer, rcl.creationTime, rcl.expirationTime, rcl.priority);
			} else {
				cart = new MultiKeyCart<>(rcl.filter, rcl.consumer, null, rcl.creationTime, rcl.expirationTime,  LoadType.RESULT_CONSUMER,rcl.priority);
			}
			
			CompletableFuture<Boolean> f = new CompletableFuture<>();
			f.complete(true);
			
			for(Conveyor<K, L, OUT> conv: conveyors) {
				f = f.thenCombine( conv.place(cart.copy()), (a,b) -> a && b );
			}

			return f;
		}, 
		rc->{
			this.resultConsumer = rc;
			for(Conveyor<K, L, OUT> conv: conveyors) {
				conv.resultConsumer().first(this.resultConsumer).set();
			}
			if(rc instanceof ForwardingConsumer) {
				this.forwardingResults = true;
			}
		}, 
		this.resultConsumer);
	}
	
	@Override
	public ResultConsumerLoader<K, OUT> resultConsumer(ResultConsumer<K,OUT> consumer) {
		return this.resultConsumer().first(consumer);
	}
	
	public ScrapConsumerLoader<K> scrapConsumer() {
		return new ScrapConsumerLoader<>(sc -> {
            this.scrapConsumer = sc;
            for (Conveyor<K, L, OUT> conv : conveyors) {
                conv.scrapConsumer().first(this.scrapConsumer).set();
            }

        }, scrapConsumer);
	}

	@Override
	public ScrapConsumerLoader<K> scrapConsumer(ScrapConsumer<K,?> scrapConsumer) {
		return scrapConsumer().first(scrapConsumer);
	}

	@Override
	public void setAutoAcknowledge(boolean auto) {
		for(Conveyor<K, L, OUT> conv: conveyors) {
			conv.setAutoAcknowledge(auto);
		}
	}

	@Override
	public void autoAcknowledgeOnStatus(Status first, Status... other) {
		for(Conveyor<K, L, OUT> conv: conveyors) {
			conv.autoAcknowledgeOnStatus(first,other);
		}
	}


	@Override
	public ResultConsumer<K, OUT> getResultConsumer() {
		return resultConsumer;
	}


	@Override
	public void interrupt(final String conveyorName) {
		conveyors.forEach(c->c.interrupt(conveyorName));
		
	}

	@Override
	public void setCartPayloadAccessor(Function<Cart<K, ?, L>, Object> payloadFunction) {
		conveyors.forEach(c->c.setCartPayloadAccessor(payloadFunction));
	}


	@Override
	public void suspend() {
		this.suspended = true;
		conveyors.forEach(Conveyor::suspend);
	}


	@Override
	public void resume() {
		this.suspended = false;
		conveyors.forEach(Conveyor::resume);
	}


	@Override
	public boolean isSuspended() {
		return suspended;
	}

	@Override
	public Class<?> mBeanInterface() {
		return ParallelConveyorMBean.class;
	}
}
