package com.aegisql.conveyor.persistence.converters.arrays;

import java.nio.ByteBuffer;

public class ShortsToBytesConverter implements ObjectArrayToByteArrayConverter<Short> {

	@Override
	public byte[] toPersistence(Short[] obj) {
		if(obj == null) {
			return null;
		}
		byte[] res = new byte[2*obj.length];
		
		ByteBuffer bb = ByteBuffer.wrap(res);
		
		for(int i = 0; i < obj.length; i++) {
			bb.putShort(2*i, obj[i].shortValue());
		}
		
		return res;
	}

	@Override
	public Short[] fromPersistence(byte[] p) {
		if(p == null) {
			return null;
		}
		Short[] res = new Short[p.length/2];
		ByteBuffer bb = ByteBuffer.wrap(p);
		
		for(int i = 0; i < res.length; i++) {
			res[i] = bb.getShort(2*i);
		}

		return res;
	}

}