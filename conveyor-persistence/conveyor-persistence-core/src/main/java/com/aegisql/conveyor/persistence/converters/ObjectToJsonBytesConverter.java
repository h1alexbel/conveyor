package com.aegisql.conveyor.persistence.converters;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectToJsonBytesConverter <O> implements ObjectToByteArrayConverter<O> {

	private final Class<O> valueType;
	
	public ObjectToJsonBytesConverter(Class<O> valueType) {
		this.valueType = valueType;
	}
	
	
	@Override
	public byte[] toPersistence(O obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.writeValueAsBytes(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed "+valueType.getCanonicalName()+" -> JSON conversion: "+obj,e);
		}
	}

	@Override
	public O fromPersistence(byte[] p) {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.readValue(p, valueType);
		} catch (IOException e) {
			throw new RuntimeException("Failed JSON -> "+valueType.getCanonicalName()+" conversion: ",e);
		}
	}


	@Override
	public String conversionHint() {
		return "JSON<"+valueType.getCanonicalName()+">:byte[]";
	}

}