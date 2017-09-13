package com.aegisql.conveyor.persistence.converters.sql;

import java.nio.ByteBuffer;
import java.sql.Date;

import com.aegisql.conveyor.persistence.core.ObjectConverter;

public class SqlDateToBytesConverter implements ObjectConverter<Date, byte[]> {

	@Override
	public byte[] toPersistence(Date obj) {
		if(obj==null) {
			return null;
		}
		byte[] bytes = new byte[8];
		ByteBuffer.wrap(bytes).putLong(obj.getTime());
		return bytes;
	}

	@Override
	public Date fromPersistence(byte[] p) {
		if(p == null || p.length == 0) {
			return null;
		}
		return new Date(ByteBuffer.wrap(p).getLong());
	}

}
