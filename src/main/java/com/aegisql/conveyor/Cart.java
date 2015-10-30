package com.aegisql.conveyor;

public class Cart<K,V,L> {
	
	private final K k;
	private final V v;
	private final L label;
	
	private final long created = System.currentTimeMillis();
	private final long expiration;
	
	
	public Cart(K k, V v, L label, long expiration) {
		super();
		this.k = k;
		this.v = v;
		this.label = label;
		this.expiration = expiration;
	}
	
	public Cart(K k, V v, L label) {
		super();
		this.k = k;
		this.v = v;
		this.label = label;
		this.expiration = 0;
	}
	
	public K getKey() {
		return k;
	}
	public V getValue() {
		return v;
	}
	public L getLabel() {
		return label;
	}
	public long getCreationTime() {
		return created;
	}
	public long getExpirationTime() {
		return expiration;
	}

	@Override
	public String toString() {
		return "Cart [k=" + k + ", v=" + v + ", label=" + label + ", created=" + created + ", expiration=" + expiration
				+ "]";
	}
	
}
