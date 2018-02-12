package com.aegisql.conveyor.consumers.result;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.serial.SerializablePredicate;

public class ForwardResult<K2, L2> {

	private final static Logger LOG = LoggerFactory.getLogger(ForwardResult.class);

	public static class ForwardingConsumer<K2, L> implements ResultConsumer<Object, Object> {

		private static final long serialVersionUID = 1L;

		private String fromName;
		private String toConvName;
		private Conveyor<K2, L, ?> toConv;
		private L label;
		private Function<ProductBin, K2> keyTransformer;
		private SerializablePredicate<K2> filter;

		public ForwardingConsumer(L label, Function<ProductBin, K2> keyTransformer, String toName, String fromName,
				SerializablePredicate<K2> filter) {
			this.fromName = fromName;
			this.toConvName = toName;
			this.label = label;
			this.keyTransformer = keyTransformer;
			this.filter = filter;
		}

		public ForwardingConsumer(L label, Function<ProductBin, K2> keyTransformer, Conveyor<K2, L, ?> toConv,
				String fromName, SerializablePredicate<K2> filter) {
			this.fromName = fromName;
			this.toConv = toConv;
			this.toConvName = toConv.getName();
			this.label = label;
			this.keyTransformer = keyTransformer;
			this.filter = filter;
		}

		private Conveyor<K2, L, ?> getToConv() {
			if (toConv == null) {
				toConv = Conveyor.byName(toConvName);
			}
			return toConv;
		}

		@Override
		public void accept(ProductBin bin) {
			LOG.debug("Forward {} from {} to {} {}", label, fromName, getToConv().getName(), bin);
			String forwardedProperty;
			if (!bin.properties.containsKey("FORWARDED")) {
				forwardedProperty = fromName;
			} else {
				forwardedProperty = (String) bin.properties.get("FORWARDED");
			}
			if (filter == null) {
				getToConv().part().label(label).id(keyTransformer.apply(bin)).value(bin.product)
						.ttl(bin.remainingDelayMsec, TimeUnit.MILLISECONDS).addProperty("FORWARDED", forwardedProperty)
						.place();
			} else {
				getToConv().part().label(label).foreach(filter).value(bin.product)
				.ttl(bin.remainingDelayMsec, TimeUnit.MILLISECONDS).addProperty("FORWARDED", forwardedProperty)
				.place();
			}
		}

		public String getToConvName() {
			return toConvName;
		}

	}

	private Conveyor fromConv;
	private String toConvName;
	private Conveyor<K2, L2, ?> toConv;
	private L2 label;
	private Function<ProductBin, K2> keyTransformer = bin -> (K2) bin.key;
	private SerializablePredicate<K2> filter = null;

	private ForwardResult(Conveyor fromConv) {
		this.fromConv = fromConv;
	}

	private ForwardResult(Conveyor fromConv, Conveyor<K2, L2, ?> toConv, L2 label,
			Function<ProductBin, K2> keyTransformer, String toName, SerializablePredicate<K2> filter) {
		this.fromConv = fromConv;
		this.toConv = toConv;
		this.label = label;
		this.keyTransformer = keyTransformer;
		this.toConvName = toName;
		this.filter = filter;
	}

	public static ForwardResult from(Conveyor fromConv) {
		Objects.requireNonNull(fromConv, "From Conveyor must not be null");
		return new ForwardResult<>(fromConv);
	}

	public ForwardResult<K2, L2> to(Conveyor<K2, L2, ?> tc) {
		return new ForwardResult<>(fromConv, tc, label, keyTransformer, null, filter);
	}

	public ForwardResult<K2, L2> to(String toConv) {
		return new ForwardResult<>(fromConv, null, label, keyTransformer, toConv, filter);
	}

	public ForwardResult<K2, L2> label(L2 l) {
		return new ForwardResult<>(fromConv, toConv, l, keyTransformer, toConvName, filter);
	}

	public ForwardResult<K2, L2> transformKey(Function<ProductBin, K2> t) {
		return new ForwardResult<>(fromConv, toConv, label, t, toConvName, filter);
	}

	public ForwardResult<K2, L2> foreach() {
		return new ForwardResult<>(fromConv, toConv, label, keyTransformer, toConvName, k -> true);
	}

	public ForwardResult<K2, L2> foreach(SerializablePredicate<K2> f) {
		return new ForwardResult<>(fromConv, toConv, label, keyTransformer, toConvName, f);
	}

	public void bind() {
		if (toConv != null) {
			fromConv.resultConsumer()
					.andThen(new ForwardingConsumer<K2, L2>(label, keyTransformer, toConv, fromConv.getName(), filter))
					.set();
		} else if (toConvName != null) {
			fromConv.resultConsumer().andThen(
					new ForwardingConsumer<K2, L2>(label, keyTransformer, toConvName, fromConv.getName(), filter))
					.set();
		} else {
			throw new RuntimeException("Either toConveyor or toConveyor name must not be null");
		}
	}
}
