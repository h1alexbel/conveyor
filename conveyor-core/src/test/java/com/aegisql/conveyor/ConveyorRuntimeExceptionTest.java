package com.aegisql.conveyor;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConveyorRuntimeExceptionTest {

    @Test
    public void testConstructors() {
        ConveyorRuntimeException e1 = new ConveyorRuntimeException();
        ConveyorRuntimeException e2 = new ConveyorRuntimeException("test");
        ConveyorRuntimeException e3 = new ConveyorRuntimeException(e1);
        ConveyorRuntimeException e4 = new ConveyorRuntimeException("test",e1);
        ConveyorRuntimeException e5 = new ConveyorRuntimeException("test",e1,true,false);
    }

}