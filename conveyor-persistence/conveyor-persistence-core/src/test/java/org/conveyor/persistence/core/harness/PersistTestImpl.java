package org.conveyor.persistence.core.harness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.conveyor.persistence.core.Persist;

import com.aegisql.conveyor.cart.Cart;

public class PersistTestImpl implements Persist<Integer, Integer> {

	ConcurrentHashMap<Integer,Cart<Integer,?,?>> carts = new ConcurrentHashMap<>();
	ConcurrentHashMap<Integer,List<Integer>> cartIds   = new ConcurrentHashMap<>();
	Set<Integer> ack = new HashSet<>();
	
	AtomicInteger idGen = new AtomicInteger(0);
	
	public PersistTestImpl() {
		
	}
	
	public PersistTestImpl(PersistTestImpl old, PersistTestImpl... more) {
		
		old.carts.forEach((oldId,cart)->{
			cart.addProperty("_RECOVERY_CART", Boolean.TRUE);
			Integer newId = getUniqueId();
			cart.addProperty("CART_ID", newId );
			this.saveCart(newId, cart);
		});
		
		old.archiveAckKeys(this.carts.keySet());
		old.archiveKeys(this.carts.keySet());
		this.cartIds.forEach((id,list)->old.archiveData(list));
		
		ack.addAll(old.ack);
		if(more != null) {
			for(PersistTestImpl p:more) {
				p.carts.forEach((oldId,cart)->this.saveCart(getUniqueId(), cart));
				ack.addAll(p.ack);
				p.archiveAckKeys(this.carts.keySet());
				p.archiveKeys(this.carts.keySet());
				p.cartIds.forEach((id,list)->old.archiveData(list));
			}
		}
	}
	

	@Override
	public Integer getUniqueId() {
		return idGen.incrementAndGet();
	}

	@Override
	public void saveCart(Integer id, Cart<Integer, ?, ?> cart) {
		carts.put(id, cart);
		saveCartId(cart.getKey(), id);
	}

	@Override
	public void saveCartId(Integer key, Integer cartId) {
		getAllCartIds(key).add(cartId);
	}

	@Override
	public void saveAcknowledge(Integer key) {
		ack.add(key);
	}

	@Override
	public Cart<Integer, ?, ?> getCart(Integer id) {
		return carts.get(id);
	}

	@Override
	public Collection<Integer> getAllCartIds(Integer key) {
		ArrayList<Integer> newList = new ArrayList<>();
		
		List<Integer> ids = cartIds.putIfAbsent(key, newList);
		if(ids == null) {
			ids = newList;
		}
		return ids;
	}

	@Override
	public void archiveData(Collection<Integer> ids) {
		ids.forEach(id->carts.remove(id));
	}

	@Override
	public void archiveKeys(Collection<Integer> key) {
		key.forEach(k->cartIds.remove(k));
	}

	@Override
	public void archiveAckKeys(Collection<Integer> keys) {
		ack.removeAll(keys);
	}

	@Override
	public String toString() {
		return "Persistence [carts=" + carts + ", cartIds=" + cartIds + ", ack=" + ack + "]";
	}

	@Override
	public Collection<Cart<Integer, ?, ?>> getAllCarts() {
		return carts.values();
	}

	public boolean isEmpty() {
		return carts.isEmpty() && cartIds.isEmpty() && ack.isEmpty();
	}
	
}
