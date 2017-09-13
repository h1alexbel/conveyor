package com.aegisql.conveyor.persistence.converters;

import java.nio.ByteBuffer;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class ShortToBytesConverter implements ObjectConverter<Short, byte[]> {

	@Override
	public byte[] toPersistence(Short obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[2];
		ByteBuffer.wrap(bytes).putShort(obj.shortValue());
		return bytes;
	}

	@Override
	public Short fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return ByteBuffer.wrap(p).getShort();
	}

}
