package com.example.parcellocker.cu16;

public final class CU16Commands {
    public static final byte STX = 0x02;
    public static final byte ETX = 0x03;

    public static final byte CMD_GET_STATUS = 0x30;
    public static final byte CMD_UNLOCK_ONE = 0x31;
    public static final byte CMD_GET_ALL_BUS = 0x32;
    public static final byte CMD_UNLOCK_ALL = 0x33;
    public static final byte CMD_SET_UNLOCK_TIME = 0x37;
    public static final byte CMD_SET_DELAY_OR_BAUD = 0x39;

    public static final byte RESP_STATUS_SINGLE = 0x35;
    public static final byte RESP_STATUS_ALL = 0x36;
    public static final byte RESP_UNLOCK_TIME = 0x38;
    // ...
    private CU16Commands(){}

    
}
