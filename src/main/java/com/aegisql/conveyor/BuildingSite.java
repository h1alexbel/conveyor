/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.cart.Cart;

// TODO: Auto-generated Javadoc
/**
 * The Class BuildingSite.
 *
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 * @param <K> the key type
 * @param <L> the generic type
 * @param <C> the generic type
 * @param <OUT> the generic type
 */
public class BuildingSite <K, L, C extends Cart<K, ?, L>, OUT> implements Expireable, Delayed {

	private final static Logger LOG = LoggerFactory.getLogger(BuildingSite.class);
	/**
	 * The Enum Status.
	 */
	public static enum Status{
		WAITING_DATA,
		TIMED_OUT,
		READY,
		CANCELED,
		INVALID;
	}
	
	/** The builder. */
	final Supplier<? extends OUT> builder;
	
	/** The value consumer. */
	private final LabeledValueConsumer<L, Object, Supplier<? extends OUT>> valueConsumer;
	
	/** The readiness. */
	private final BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness;
	
	private Consumer<Supplier<? extends OUT>> timeoutAction;
	
	private boolean saveCarts      = false;

	private List<C> allCarts = new ArrayList<>();
	
	/** The initial cart. */
	private final  C initialCart;

	private  C lastCart = null;

	/** The accept count. */
	private int acceptCount = 0;
	
	/** The builder created. */
	private final long builderCreated;
	
	/** The builder expiration. */
	long builderExpiration;
	
	/** The status. */
	private Status status = Status.WAITING_DATA;
	
	/** The last error. */
	private Throwable lastError;
	
	/** The event history. */
	private Map<L,AtomicInteger> eventHistory = new LinkedHashMap<>();
	
	/** The delay keeper. */
	Delayed delayKeeper;
	
	private final Lock lock;

	private final long ttl;
	
	private final TimeUnit unit;
	
	private final boolean synchronizeBuilder;
	
	private final Supplier<Supplier<? extends OUT>> builderSupplier;
	
	/**
	 * Instantiates a new building site.
	 *
	 * @param cart the cart
	 * @param builderSupplier the builder supplier
	 * @param cartConsumer the cart consumer
	 * @param readiness the ready
	 * @param timeoutAction the timeoutAction
	 * @param ttl the ttl
	 * @param unit the unit
	 * @param synchronizeBuilder the synchronizeBuilder
	 */
	public BuildingSite( 
			C cart, 
			Supplier<Supplier<? extends OUT>> builderSupplier, 
			LabeledValueConsumer<L, ?, Supplier<? extends OUT>> cartConsumer, 
			BiPredicate<State<K,L>, Supplier<? extends OUT>> readiness, 
			Consumer<Supplier<? extends OUT>> timeoutAction,
			long ttl, TimeUnit unit, boolean synchronizeBuilder, boolean saveCarts) {
		this.initialCart        = cart;
		this.lastCart           = cart;
		this.builder            = builderSupplier.get() ;
		this.builderSupplier    = builderSupplier;
		this.timeoutAction      = timeoutAction;
		this.saveCarts          = saveCarts;
		this.ttl                = ttl;
		this.unit               = unit;
		this.synchronizeBuilder = synchronizeBuilder;
		this.valueConsumer = (LabeledValueConsumer<L, Object, Supplier<? extends OUT>>) cartConsumer;
		if(synchronizeBuilder) {
			lock = new ReentrantLock();
		} else {
			lock = new Lock() {
				public void lock() {}
				public void lockInterruptibly() throws InterruptedException {}
				public boolean tryLock() {
					return true;
				}
				public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
					return true;
				}
				public void unlock() {}
				public Condition newCondition() {
					return null;
				}
			};
		}	
		if(builder instanceof TestingState) {
			this.readiness = (state,builder) -> {
				lock.lock();
				try {
					return ((TestingState<K,L>)builder).test(state);
				} finally {
					lock.unlock();
				}
			};
		}else if(builder instanceof Testing) {
			this.readiness = (state,builder) -> {
				lock.lock();
				try {
					return ((Testing)builder).test();
				} finally {
					lock.unlock();
				}
			};
		} else {
			this.readiness = readiness;
		}
		this.eventHistory.put(cart.getLabel(), new AtomicInteger(0));
		if(builder instanceof Expireable) {
			Expireable expireable = (Expireable)builder;
			builderCreated    = System.currentTimeMillis();
			builderExpiration = expireable.getExpirationTime();
			delayKeeper       = Expireable.toDelayed((Expireable)builder);
		} else if ( cart.getExpirationTime() > 0 ) {
			builderCreated    = cart.getCreationTime();
			builderExpiration = cart.getExpirationTime();
			delayKeeper       = Expireable.toDelayed(cart);
		} else {
			builderCreated = System.currentTimeMillis();
			if(ttl == 0) {
				builderExpiration = 0;
			} else {
				builderExpiration = builderCreated + TimeUnit.MILLISECONDS.convert(ttl, unit);
			}
			delayKeeper = Expireable.toDelayed(this);
		}
	}

	/**
	 * Accept the data.
	 *
	 * @param cart the cart
	 */
	public void accept(C cart) {
		this.lastCart = cart;
		if( saveCarts) {
			allCarts.add(cart);
		}
		L label = cart.getLabel();
		Object value = cart.getValue();
		
			if( (label == null) || ! (label instanceof SmartLabel) ) {
				lock.lock();
				try {
					valueConsumer.accept(label, value, builder);
				} finally {
					lock.unlock();
				}
			} else {
				lock.lock();
				try {
					((SmartLabel)label).get().accept(builder,value);
				} finally {
					lock.unlock();
				}
			}
		if(label != null) {
			acceptCount++;
			if(eventHistory.containsKey(label)) {
				eventHistory.get(label).incrementAndGet();
			} else {
				eventHistory.put(label, new AtomicInteger(1));
			}
		}
	}

	public void timeout(C cart) {
		this.lastCart = cart;
			if (builder instanceof TimeoutAction ){
				lock.lock();
				try {
					((TimeoutAction)builder).onTimeout();
				} finally {
					lock.unlock();
				}
			} else if( timeoutAction != null ) {
				lock.lock();
				try {
					timeoutAction.accept(builder);
				} finally {
					lock.unlock();
				}
			}
	}

	/**
	 * Builds the Product when it is ready.
	 *
	 * @return the out
	 */
	public OUT build() {
		if( ! ready() ) {
			throw new IllegalStateException("Builder is not ready!");
		}
		lock.lock();
		try {
			return builder.get();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Try to build the product without checking for it's readiness condition.
	 * This method should not be used to produce actual results, but it can be useful
	 * when processing the ScrapBin - the only place where this interface is accessible.
	 *
	 * @return the out
	 */
	public OUT unsafeBuild() {
		lock.lock();
		try {
			return builder.get();
		} finally {
			lock.unlock();
		}
	}

	private Supplier<? extends OUT> productSupplier = null;
	
	public Supplier<? extends OUT> getProductSupplier() {
		if(productSupplier == null ) {
			productSupplier = new Supplier<OUT>() {
				@Override
				public OUT get() {
					if( ! getStatus().equals(Status.WAITING_DATA)) {
						throw new IllegalStateException("Supplier is in a wrong state: " + getStatus());
					}
					OUT res = null;
					lock.lock();
					try {
						res = builder.get();
					} finally {
						lock.unlock();
					}
					return res;
				}
			};
		}
		return productSupplier;
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		boolean res = false;
		final Map<L,Integer> history = new LinkedHashMap<>();
		eventHistory.forEach((k,v)->history.put(k, v.get()));
		State<K,L> state = new State<>(
				initialCart.getKey(),
				builderCreated,
				builderExpiration,
				initialCart.getCreationTime(),
				initialCart.getExpirationTime(),
				acceptCount,
				Collections.unmodifiableMap( history ),
				Collections.unmodifiableList(allCarts)
				);
		lock.lock();
		try {
			res = readiness.test(state, builder);
		} finally {
			lock.unlock();
		}
		if( res ) {
			status = Status.READY;
		}
		return res;
	}

	/**
	 * Gets the accept count.
	 *
	 * @return the accept count
	 */
	public int getAcceptCount() {
		return acceptCount;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed o) {
		return delayKeeper.compareTo(o);
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return delayKeeper.getDelay(unit);	}

	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return getDelay(TimeUnit.MILLISECONDS) < 0;
	}

	/**
	 * Gets the builder expiration.
	 *
	 * @return the builder expiration
	 */
	@Override
	public long getExpirationTime() {
		return builderExpiration;
	}

	/**
	 * Gets the cart.
	 *
	 * @return the cart
	 */
	public C getCreatingCart() {
		return initialCart;
	}

	public List<C> getAcceptedCarts() {
		return Collections.unmodifiableList( allCarts );
	}

	/**
	 * Gets the key.
	 *
	 * @return the key
	 */
	public K getKey() {
		return initialCart.getKey();
	}
	
	/**
	 * Gets the last error.
	 *
	 * @return the last error
	 */
	public Throwable getLastError() {
		return lastError;
	}

	/**
	 * Sets the last error.
	 *
	 * @param lastError the new last error
	 */
	public void setLastError(Throwable lastError) {
		this.lastError = lastError;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BuildingSite [" + (builder != null ? "builder=" + builder + ", " : "")
				+ (initialCart != null ? "initialCart=" + initialCart + ", " : "") + "acceptCount=" + acceptCount
				+ ", builderCreated=" + builderCreated + ", builderExpiration=" + builderExpiration + ", "
				+ (status != null ? "status=" + status + ", " : "")
				+ (lastError != null ? "lastError=" + lastError + ", " : "")
				+ (eventHistory != null ? "eventHistory=" + eventHistory : "") + "]";
	}

	/**
	 * Gets the status.
	 *
	 * @return the status
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Sets the status.
	 *
	 * @param status the new status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	public C getLastCart() {
		return lastCart;
	}

	public void setLastCart(C lastCart) {
		this.lastCart = lastCart;
	}
	
	/**
	 * package access methods
	 * */
	Supplier<? extends OUT> getBuilder() {
		return builder;
	}

	BuildingSite <K, L, C, OUT> newInstance(long expirationTime) {
		BuildingSite <K, L, C, OUT> newSite =  new BuildingSite<>(
				initialCart, 
				builderSupplier, 
				valueConsumer , 
				readiness, 
				timeoutAction, 
				ttl, 
				unit, 
				synchronizeBuilder, 
				saveCarts );
		newSite.builderExpiration = expirationTime;
		newSite.acceptCount = acceptCount;
		newSite.eventHistory = eventHistory;
		newSite.allCarts = allCarts;
		return newSite;
	}
	
}
