package com.aegisql.conveyor.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.ReadinessTester;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.consumers.result.ForwardResult;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.parallel.KBalancedParallelConveyor;
import com.aegisql.conveyor.parallel.LBalancedParallelConveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorBuilder.
 */
@SuppressWarnings("rawtypes")
public class ConveyorBuilder implements Supplier<Conveyor>, Testing {

	/** The all files read. */
	//readiness
	private boolean allFilesRead = false;
	
	/** The l parallel. */
	private Set<String> lParallel    = new LinkedHashSet<>();
	
	/** The dependencies. */
	private Set<String> dependencies = new HashSet<>();
	
	/** The completed. */
	private Set<String> completed    = new HashSet<>();
	
	/** The Constant serialVersionUID. */
	//setters
	private static final long serialVersionUID = 1L;
	
	/** The constructor. */
	private Supplier<Conveyor> constructor                        = AssemblingConveyor::new;
	
	/** The idle heart beat. */
	private Duration idleHeartBeat                                = null;
	
	/** The default builder timeout. */
	private Duration defaultBuilderTimeout                        = null;
	
	/** The reject unexpireable carts older than. */
	private Duration rejectUnexpireableCartsOlderThan             = null;
	
	/** The expiration postpone time. */
	private Duration expirationPostponeTime                       = null;
	
	/** The static parts. */
	private Collection<Pair> staticParts                          = new LinkedList<>();
	
	/** The first result consumer. */
	private ResultConsumer firstResultConsumer                    = null;
	
	/** The next result consumers. */
	private Collection<ResultConsumer> nextResultConsumers        = new LinkedList<>();
	
	/** The first scrap consumer. */
	private ScrapConsumer firstScrapConsumer                      = null;
	
	/** The next scrap consumers. */
	private Collection<ScrapConsumer> nextScrapConsumers          = new LinkedList<>();
	
	/** The timeout action. */
	private Consumer timeoutAction                                = null;
	
	/** The default cart consumer. */
	private LabeledValueConsumer defaultCartConsumer              = null;
	
	/** The readiness evaluator bi P. */
	private BiPredicate readinessEvaluatorBiP                     = null;
	
	/** The readiness evaluator P. */
	private Predicate readinessEvaluatorP                         = null;
	
	/** The builder supplier. */
	private BuilderSupplier builderSupplier                       = null;
	
	/** The add cart before placement validator. */
	private Collection<Consumer> addCartBeforePlacementValidator  = new LinkedList<>();
	
	/** The add before key eviction action. */
	private Collection<Consumer> addBeforeKeyEvictionAction       = new LinkedList<>();
	
	/** The add before key rescheduling action. */
	private Collection<BiConsumer> addBeforeKeyReschedulingAction = new LinkedList<>();
	
	/** The accepted labels. */
	private Set acceptedLabels                                    = new HashSet<>();
	
	private ReadinessTester readinessTester                       = null;

	/** The ready labels. */
	private Map<Object,Integer> readyLabels                       = new HashMap<>();

	/** The enable postpone expiration. */
	private Boolean enablePostponeExpiration                      = null;
	
	/** The enable postpone expiration on timeout. */
	private Boolean enablePostponeExpirationOnTimeout             = null;
	
	/** The auto acknowledge. */
	private Boolean autoAcknowledge                               = null;
	
	/** The acknowledge action. */
	private Consumer acknowledgeAction                            = null;
	
	/** The auto acknowledge on status. */
	private Status[] autoAcknowledgeOnStatus                      = null;
	
	/** The cart payload accessor. */
	private Function cartPayloadAccessor                          = null;
	
	/** The forward. */
	private Collection<Trio> forward                              = new LinkedList<>();
	
	/** The parallel factor. */
	private int parallelFactor                                    = 1;
	
	/** The persistence. */
	private String persistence                                    = null;
	
	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ConveyorBuilder.class);
		
	private Map<String,Map<String,Object>> persistenceProperties  = new HashMap<>();
	
	/**
	 * Sets the if not null.
	 *
	 * @param <T> the generic type
	 * @param value the value
	 * @param consumer the consumer
	 */
	private <T> void setIfNotNull(T value,Consumer<T> consumer) {
		if(value != null) {
			consumer.accept(value);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.util.function.Supplier#get()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Conveyor get() {
		try {
		LOG.debug("{}",this);
		Conveyor instance = null;
		
		if(parallelFactor > 1) {
			instance = new KBalancedParallelConveyor(constructor, parallelFactor);
			LOG.info("Instantiate K-Balanced conveyor with parallelizm={}",parallelFactor);
		} else if(lParallel.size() > 1){
			LOG.info("Instantiate L-Balanced conveyor with parallelizm={}",lParallel);
			String[] lConveyors = new String[lParallel.size()];
			lConveyors = lParallel.toArray(lConveyors);
			instance = new LBalancedParallelConveyor<>(lConveyors);
		} else {
			instance = constructor.get();
			LOG.info("Instantiate {}",instance.getClass().getName());
		}
		
		if(persistence != null) {
			Persistence p = Persistence.byName(persistence);
			instance = new PersistentConveyor(p,instance);
		}
		
		final Conveyor c = instance;
		
		setIfNotNull(builderSupplier, c::setBuilderSupplier);
		setIfNotNull(idleHeartBeat, c::setIdleHeartBeat);
		setIfNotNull(defaultBuilderTimeout, c::setDefaultBuilderTimeout);
		setIfNotNull(rejectUnexpireableCartsOlderThan, c::rejectUnexpireableCartsOlderThan);
		setIfNotNull(expirationPostponeTime, c::setExpirationPostponeTime);
		setIfNotNull(firstResultConsumer, rc -> c.resultConsumer(rc).set() );
		setIfNotNull(firstScrapConsumer, rc -> c.scrapConsumer(rc).set() );
		setIfNotNull(timeoutAction, c::setOnTimeoutAction);
		setIfNotNull(defaultCartConsumer, c::setDefaultCartConsumer);
		setIfNotNull(enablePostponeExpiration, c::enablePostponeExpiration);
		setIfNotNull(enablePostponeExpirationOnTimeout, c::enablePostponeExpirationOnTimeout);
		setIfNotNull(autoAcknowledge, c::setAutoAcknowledge);
		setIfNotNull(acknowledgeAction, c::setAcknowledgeAction);
		setIfNotNull(cartPayloadAccessor, c::setCartPayloadAccessor);
		
		if(readinessTester != null) {
			for(Object label:readyLabels.keySet()) {
				if(label == null) {
					LOG.debug("Ready when accept count {}",readyLabels.get(null));
					readinessTester = readinessTester.accepted(readyLabels.get(null));
				} else {
					LOG.debug("Ready when accept {} times {}",label,readyLabels.get(label));
					readinessTester = readinessTester.accepted(label, readyLabels.get(label));
				}
			}
			if(readinessEvaluatorP != null) {
				readinessTester = readinessTester.andThen(readinessEvaluatorP);
			}
			if(readinessEvaluatorBiP != null) {
				readinessTester = readinessTester.andThen(readinessEvaluatorBiP);
			}
			c.setReadinessEvaluator(readinessTester);
		} else {
			setIfNotNull(readinessEvaluatorP, c::setReadinessEvaluator);
			setIfNotNull(readinessEvaluatorBiP, c::setReadinessEvaluator);
		}
		
		if(autoAcknowledgeOnStatus != null && autoAcknowledgeOnStatus.length != 0) {
			Status first  = autoAcknowledgeOnStatus[0];
			Status[] more = null;
			if(autoAcknowledgeOnStatus.length > 1) {
				more = new Status[autoAcknowledgeOnStatus.length-1];
				for(int i = 1; i < autoAcknowledgeOnStatus.length; i++) {
					more[i-1] = autoAcknowledgeOnStatus[i];
				}
			}
			c.autoAcknowledgeOnStatus(first, more);
		}		
		nextResultConsumers.forEach(rc -> c.resultConsumer().andThen(rc).set() );
		nextScrapConsumers.forEach(rc -> c.scrapConsumer().andThen(rc).set() );
		staticParts.forEach(pair -> c.staticPart().label(pair.label).value(pair.value).place() );
		addCartBeforePlacementValidator.forEach(pv -> c.addCartBeforePlacementValidator(pv) );
		addBeforeKeyEvictionAction.forEach(pv -> c.addBeforeKeyEvictionAction(pv) );
		addBeforeKeyReschedulingAction.forEach(ra -> c.addBeforeKeyReschedulingAction(ra) );
		forward.forEach(f->{
			ForwardResult fr = ForwardResult.from(c).to((String)f.value1).label(f.label);
			if(f.value2 != null) {
				fr = fr.transformKey((Function) f.value2);
			}
			fr.bind();			
		});
		if(acceptedLabels.size() > 0) {
			Object[] acceptLabels = new Object[acceptedLabels.size()];
			acceptLabels = acceptedLabels.toArray(acceptLabels);
			c.acceptLabels(acceptLabels);
		}

		
		return c;
		} catch (Exception e) {
			LOG.error("Error constructing Conveyor",e);
			throw e;
		}
	}
	
	/**
	 * Idle heart beat.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void idleHeartBeat(ConveyorBuilder b, String s) {
		LOG.debug("Applying idleHeartBeat={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.idleHeartBeat = Duration.ofMillis(value.longValue());
	}
	
	/**
	 * Default builder timeout.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void defaultBuilderTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultBuilderTimeout={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.defaultBuilderTimeout = Duration.ofMillis(value.longValue());
	}
	
	/**
	 * Reject unexpireable carts older than.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void rejectUnexpireableCartsOlderThan(ConveyorBuilder b, String s) {
		LOG.debug("Applying rejectUnexpireableCartsOlderThan={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.rejectUnexpireableCartsOlderThan = Duration.ofMillis(value.longValue());
	}
	
	/**
	 * Expiration postpone time.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void expirationPostponeTime(ConveyorBuilder b, String s) {
		LOG.debug("Applying expirationPostponeTime={}",s);
		Long value = (Long) ConfigUtils.timeToMillsConverter.apply(s);
		b.expirationPostponeTime = Duration.ofMillis(value.longValue());
	}
	
	/**
	 * Static part.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void staticPart(ConveyorBuilder b, String s) {
		LOG.debug("Applying staticPart={}",s);
		Pair value = (Pair) ConfigUtils.stringToLabelValuePairSupplier.apply(s);
		b.staticParts.add(value);
	}

	/**
	 * First result consumer.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void firstResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstResultConsumer={}",s);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
		b.firstResultConsumer = value;
	}

	/**
	 * Next result consumer.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void nextResultConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextResultConsumer={}",s);
		ResultConsumer value = (ResultConsumer) ConfigUtils.stringToResultConsumerSupplier.apply(s);
		b.nextResultConsumers.add(value);
	}

	/**
	 * First scrap consumer.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void firstScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying firstScrapConsumer={}",s);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
		b.firstScrapConsumer = value;
	}

	/**
	 * Next scrap consumer.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void nextScrapConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying nextScrapConsumer={}",s);
		ScrapConsumer value = (ScrapConsumer) ConfigUtils.stringToScrapConsumerSupplier.apply(s);
		b.nextScrapConsumers.add(value);
	}

	/**
	 * Timeout action.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void timeoutAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying timeoutAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.timeoutAction = value;
	}

	/**
	 * Acknowledge action.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void acknowledgeAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying acknowledgeAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.acknowledgeAction = value;
	}

	/**
	 * Adds the cart before placement validator.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void addCartBeforePlacementValidator(ConveyorBuilder b, String s) {
		LOG.debug("Applying addCartBeforePlacementValidator={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addCartBeforePlacementValidator.add(value);
	}
	
	/**
	 * Adds the before key eviction action.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void addBeforeKeyEvictionAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyEvictionAction={}",s);
		Consumer value = (Consumer) ConfigUtils.stringToConsumerSupplier.apply(s);
		b.addBeforeKeyEvictionAction.add(value);
	}

	/**
	 * Default cart consumer.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void defaultCartConsumer(ConveyorBuilder b, String s) {
		LOG.debug("Applying defaultCartConsumer={}",s);
		LabeledValueConsumer value = (LabeledValueConsumer) ConfigUtils.stringToLabeledValueConsumerSupplier.apply(s);
		b.defaultCartConsumer = value;
	}
	
	/**
	 * Readiness evaluator.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void readinessEvaluator(ConveyorBuilder b, String s) {
		LOG.debug("Applying readinessEvaluator={}",s);
		Object obj = ConfigUtils.stringToReadinessEvaluatorSupplier.apply(s);
		if(obj instanceof BiPredicate) {
			BiPredicate re = (BiPredicate) obj;
			b.readinessEvaluatorBiP = re;
			b.readinessEvaluatorP   = null;
		} else if(obj instanceof Predicate) {
			Predicate re = (Predicate) obj;
			b.readinessEvaluatorBiP = null;
			b.readinessEvaluatorP   = re;
		} else {
			throw new ConveyorConfigurationException("Unexpected readinessEvaluator type "+obj.getClass());
		}
	}
	
	public static void readyWhen(ConveyorBuilder b, String s) {
		LOG.debug("Applying readyWhen={}",s);
		String[] parts = s.trim().split("\\s+");
		if(parts.length == 0) {
			return;
		}
		if(b.readinessTester == null) {
			b.readinessTester = new ReadinessTester<>();
		}
		int count = 1;
		try {
			count = Integer.parseInt( parts[0] ); 
		} catch(Exception e) {
			Object[] labels = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(parts[0]);
			for(Object label:labels) {
				b.readyLabels.put(label, count);
			}
			return;
		}
		
		if(parts.length == 1) {
			b.readyLabels.put(null, count);
		} else {
			Object[] labels = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(parts[1]);
			for(Object label:labels) {
				b.readyLabels.put(label, count);
			}
		}
	}


	/**
	 * Builder supplier.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void builderSupplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying builderSupplier={}",s);
		BuilderSupplier value = (BuilderSupplier) ConfigUtils.stringToBuilderSupplier.apply(s);
		b.builderSupplier = value;
	}

	/**
	 * Adds the before key rescheduling action.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void addBeforeKeyReschedulingAction(ConveyorBuilder b, String s) {
		LOG.debug("Applying addBeforeKeyReschedulingAction={}",s);
		BiConsumer value = (BiConsumer) ConfigUtils.stringToBiConsumerSupplier.apply(s);
		b.addBeforeKeyReschedulingAction.add(value);
	}
	
	/**
	 * Accept labels.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void acceptLabels(ConveyorBuilder b, String s) {
		LOG.debug("Applying acceptLabels={}",s);
		Object[] value = (Object[]) ConfigUtils.stringToLabelArraySupplier.apply(s);
		b.acceptedLabels.addAll(Arrays.asList(value));
	}
	
	/**
	 * Enable postpone expiration.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void enablePostponeExpiration(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpiration={}",s);
		Boolean value = Boolean.valueOf(s);
		b.enablePostponeExpiration = value;
	}

	/**
	 * Enable postpone expiration on timeout.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void enablePostponeExpirationOnTimeout(ConveyorBuilder b, String s) {
		LOG.debug("Applying enablePostponeExpirationOnTimeout={}",s);
		Boolean value = Boolean.valueOf(s);
		b.enablePostponeExpirationOnTimeout = value;
	}

	/**
	 * Auto acknowledge.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void autoAcknowledge(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledge={}",s);
		Boolean value = Boolean.valueOf(s);
		b.autoAcknowledge = value;
	}
	
	/**
	 * Auto acknowledge on status.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void autoAcknowledgeOnStatus(ConveyorBuilder b, String s) {
		LOG.debug("Applying autoAcknowledgeOnStatus={}",s);
		Status[] value = (Status[]) ConfigUtils.stringToStatusConverter.apply(s);
		b.autoAcknowledgeOnStatus = value;
	}

	/**
	 * Cart payload accessor.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void cartPayloadAccessor(ConveyorBuilder b, String s) {
		LOG.debug("Applying cartPayloadAccessor={}",s);
		Function value = (Function) ConfigUtils.stringToFunctionSupplier.apply(s);
		b.cartPayloadAccessor = value;
	}

	/**
	 * Forward.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void forward(ConveyorBuilder b, String s) {
		LOG.debug("Applying forward={}",s);
		Trio value = (Trio) ConfigUtils.stringToForwardTrioSupplier.apply(s);
		b.forward.add(value);
	}
	
	/**
	 * Supplier.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void supplier(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor supplier={}",s);
		Supplier<Conveyor> value = ConfigUtils.stringToConveyorSupplier.apply(s);
		b.constructor = value;
	}

	/**
	 * Persitence.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void persitence(ConveyorBuilder b, String s) {
		LOG.debug("Applying conveyor persitence={}",s);
		b.persistence = s;
	}

	/**
	 * All files read successfully.
	 *
	 * @param b the b
	 * @param readOk the read ok
	 */
	//Readiness management
	public static void allFilesReadSuccessfully(ConveyorBuilder b, Boolean readOk) {
		LOG.debug("Applying allFilesReadSuccessfully={}",readOk);
		if(readOk) {
			b.allFilesRead = readOk;
		} else {
			throw new ConveyorConfigurationException("Conveyor initialization terminated because of file reading issue");
		}
	}

	/**
	 * Dependency.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void dependency(ConveyorBuilder b, String s) {
		LOG.debug("Applying dependency={}",s);
		String[] parts = s.split(",");
		for(String p:parts) {
			String clean = p.trim();
			if( ! "".equals(clean) ) {
				b.dependencies.add(clean);
			}
		}
	}

	/**
	 * Completed.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void completed(ConveyorBuilder b, String s) {
		LOG.debug("Applying completed={}",s);
		if( b.dependencies.remove(s) ) {
			b.completed.add(s);
		}
	}
	
	/**
	 * Parallel.
	 *
	 * @param b the b
	 * @param s the s
	 */
	public static void parallel(ConveyorBuilder b, String s) {
		LOG.debug("Applying parallel={}",s);
		try {
			Integer pf = Integer.parseInt(s.split("\\s+")[0]);
			b.parallelFactor = pf;
			b.lParallel.clear();
		} catch (Exception e) {
			String[] parts = s.split(",");
			if(parts.length > 0) {
				b.parallelFactor = 1;
				for(String part:parts) {
					String trimmed = part.trim();
					b.dependencies.add(trimmed);
					b.lParallel.add(trimmed);
				}
			} else {
				throw e;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ConveyorBuilder [" + (dependencies != null ? "dependencies=" + dependencies + ", " : "")
				+ "allFilesRead=" + allFilesRead + ", " + (lParallel != null ? "lParallel=" + lParallel + ", " : "")
				+ (completed != null ? "completed=" + completed + ", " : "")
				+ (constructor != null ? "constructor=" + constructor + ", " : "")
				+ (idleHeartBeat != null ? "idleHeartBeat=" + idleHeartBeat + ", " : "")
				+ (defaultBuilderTimeout != null ? "defaultBuilderTimeout=" + defaultBuilderTimeout + ", " : "")
				+ (rejectUnexpireableCartsOlderThan != null
						? "rejectUnexpireableCartsOlderThan=" + rejectUnexpireableCartsOlderThan + ", "
						: "")
				+ (expirationPostponeTime != null ? "expirationPostponeTime=" + expirationPostponeTime + ", " : "")
				+ (staticParts != null ? "staticParts=" + staticParts + ", " : "")
				+ (firstResultConsumer != null ? "firstResultConsumer=" + firstResultConsumer + ", " : "")
				+ (nextResultConsumers != null ? "nextResultConsumers=" + nextResultConsumers + ", " : "")
				+ (firstScrapConsumer != null ? "firstScrapConsumer=" + firstScrapConsumer + ", " : "")
				+ (nextScrapConsumers != null ? "nextScrapConsumers=" + nextScrapConsumers + ", " : "")
				+ (timeoutAction != null ? "timeoutAction=" + timeoutAction + ", " : "")
				+ (defaultCartConsumer != null ? "defaultCartConsumer=" + defaultCartConsumer + ", " : "")
				+ (readinessEvaluatorBiP != null ? "readinessEvaluatorBiP=" + readinessEvaluatorBiP + ", " : "")
				+ (readinessEvaluatorP != null ? "readinessEvaluatorP=" + readinessEvaluatorP + ", " : "")
				+ (builderSupplier != null ? "builderSupplier=" + builderSupplier + ", " : "")
				+ (addCartBeforePlacementValidator != null
						? "addCartBeforePlacementValidator=" + addCartBeforePlacementValidator + ", "
						: "")
				+ (addBeforeKeyEvictionAction != null
						? "addBeforeKeyEvictionAction=" + addBeforeKeyEvictionAction + ", "
						: "")
				+ (addBeforeKeyReschedulingAction != null
						? "addBeforeKeyReschedulingAction=" + addBeforeKeyReschedulingAction + ", "
						: "")
				+ (acceptedLabels != null ? "acceptLabels=" + acceptedLabels + ", " : "")
				+ (enablePostponeExpiration != null ? "enablePostponeExpiration=" + enablePostponeExpiration + ", "
						: "")
				+ (enablePostponeExpirationOnTimeout != null
						? "enablePostponeExpirationOnTimeout=" + enablePostponeExpirationOnTimeout + ", "
						: "")
				+ (autoAcknowledge != null ? "autoAcknowledge=" + autoAcknowledge + ", " : "")
				+ (acknowledgeAction != null ? "acknowledgeAction=" + acknowledgeAction + ", " : "")
				+ (autoAcknowledgeOnStatus != null
						? "autoAcknowledgeOnStatus=" + Arrays.toString(autoAcknowledgeOnStatus) + ", "
						: "")
				+ (cartPayloadAccessor != null ? "cartPayloadAccessor=" + cartPayloadAccessor + ", " : "")
				+ (forward != null ? "forward=" + forward + ", " : "") + "parallelFactor=" + parallelFactor + ", "
				+ (persistence != null ? "persistence=" + persistence : "") + "]";
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Testing#test()
	 */
	@Override
	public boolean test() {
		return allFilesRead && dependencies.size() == 0;
	}
	
	
	
}