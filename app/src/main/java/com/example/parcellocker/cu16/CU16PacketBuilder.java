package com.example.parcellocker.cu16;
public final class CU16PacketBuilder {

    private CU16PacketBuilder() {}

    private static byte checksum(byte[] buf, int len) {
        int s = 0;
        for (int i = 0; i < len; i++) s += (buf[i] & 0xFF);
        return (byte) (s & 0xFF);
    }

//    / Make ADDR byte from cuId (0..15) and doorIndex (0..15) */
    public static byte makeAddr(int cuId, int doorIndex) {
        return (byte) (((cuId & 0x0F) << 4) | (doorIndex & 0x0F));
    }

//    */ Build minimal frame: STX ADDR CMD ETX SUM  (5 bytes) */
    public static byte[] buildSimple(byte addr, byte cmd) {
        byte[] b = new byte[5];
        b[0] = CU16Commands.STX;
        b[1] = addr;
        b[2] = cmd;
        b[3] = CU16Commands.ETX;
        b[4] = checksum(b, 4);
        return b;
    }

//    / Build frame with one data byte: STX ADDR CMD DATA ETX SUM (6 bytes) */
    public static byte[] buildOneData(byte addr, byte cmd, byte data) {
        byte[] b = new byte[6];
        b[0] = CU16Commands.STX;
        b[1] = addr;
        b[2] = cmd;
        b[3] = data;
        b[4] = CU16Commands.ETX;
        b[5] = checksum(b, 5);
        return b;
    }

    /** Build frame with two data bytes (low, high): STX ADDR CMD LO HI ETX SUM (7 bytes) */
    public static byte[] buildTwoData(byte addr, byte cmd, int value) {
        byte low = (byte) (value & 0xFF);
        byte high = (byte) ((value >> 8) & 0xFF);
        byte[] b = new byte[7];
        b[0] = CU16Commands.STX;
        b[1] = addr;
        b[2] = cmd;
        b[3] = low;
        b[4] = high;
        b[5] = CU16Commands.ETX;
        b[6] = checksum(b, 6);
        return b;
    }
}