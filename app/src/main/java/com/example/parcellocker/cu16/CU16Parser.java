package com.example.parcellocker.cu16;


public final class CU16Parser {

    private CU16Parser() {}

    public static boolean validateChecksum(byte[] packet) {
        if (packet == null || packet.length < 5) return false;
        int sum = 0;
        for (int i = 0; i < packet.length - 1; i++) sum += (packet[i] & 0xFF);
        return (byte) (sum & 0xFF) == packet[packet.length - 1];
    }

//    / Parse a 9-byte status frame and return Status object. Throws IllegalArgumentException on invalid. */
    public static Status parseStatus9(byte[] p) {
        if (p == null || p.length < 9) throw new IllegalArgumentException("packet too short");
        if (p[0] != CU16Commands.STX || p[7] != CU16Commands.ETX) throw new IllegalArgumentException("bad frame markers");
        if (!validateChecksum(p)) throw new IllegalArgumentException("checksum mismatch");

        byte cmd = p[2];
        if (cmd != CU16Commands.RESP_STATUS_SINGLE && cmd != CU16Commands.RESP_STATUS_ALL) {
            throw new IllegalArgumentException("not a status response");
        }

        byte d1 = p[3], d2 = p[4], d3 = p[5], d4 = p[6];
        boolean[] locked = new boolean[16];
        boolean[] occupied = new boolean[16];

        for (int i = 0; i < 8; i++) {
            locked[i] = ((d1 >> i) & 1) == 1;
            locked[i + 8] = ((d2 >> i) & 1) == 1;
            occupied[i] = ((d3 >> i) & 1) == 1;
            occupied[i + 8] = ((d4 >> i) & 1) == 1;
        }
        return new Status(p[1], cmd, locked, occupied);
    }

    public static class Status {
        public final byte addr;
        public final byte cmd;
        public final boolean[] locked;   // size 16
        public final boolean[] occupied; // size 16

        public Status(byte addr, byte cmd, boolean[] locked, boolean[] occupied) {
            this.addr = addr; this.cmd = cmd; this.locked = locked; this.occupied = occupied;
        }
    }
}