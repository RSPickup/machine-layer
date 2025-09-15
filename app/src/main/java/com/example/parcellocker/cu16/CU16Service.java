package com.example.parcellocker.cu16;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * High level API for app modules. Uses background executor for network calls.
 */
public class CU16Service {
    private final CU16Client client;
    private final int cuId;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public CU16Service(CU16Client client, int cuId) {
        this.client = client;
        this.cuId = cuId;
    }

    public Future<CU16Parser.Status> unlockDoorAsync(int doorIndex) {
        return exec.submit(() -> {
            byte addr = CU16PacketBuilder.makeAddr(cuId, doorIndex);
            byte[] req = CU16PacketBuilder.buildSimple(addr, CU16Commands.CMD_UNLOCK_ONE);
            byte[] resp = client.sendAndReceive(req);
            if (resp == null) throw new IllegalStateException("no response");
            // response expected to be 9-byte status; if multiple frames, take first 9
            if (resp.length >= 9) {
                byte[] first = new byte[9];
                System.arraycopy(resp, 0, first, 0, 9);
                return CU16Parser.parseStatus9(first);
            } else {
                throw new IllegalStateException("invalid response length");
            }
        });
    }

    public Future<CU16Parser.Status> getStatusSingleAsync(int doorIndex) {
        return exec.submit(() -> {
            byte addr = CU16PacketBuilder.makeAddr(cuId, doorIndex);
            byte[] req = CU16PacketBuilder.buildSimple(addr, CU16Commands.CMD_GET_STATUS);
            byte[] resp = client.sendAndReceive(req);
            if (resp == null) throw new IllegalStateException("no response");
            if (resp.length >= 9) {
                byte[] first = new byte[9];
                System.arraycopy(resp, 0, first, 0, 9);
                return CU16Parser.parseStatus9(first);
            } else {
                throw new IllegalStateException("invalid response length");
            }
        });
    }

    public Future<CU16Parser.Status> unlockAllAsync() {
        return exec.submit(() -> {
            byte addr = CU16PacketBuilder.makeAddr(cuId, 0);
            byte[] req = CU16PacketBuilder.buildSimple(addr, CU16Commands.CMD_UNLOCK_ALL);
            byte[] resp = client.sendAndReceive(req);
            if (resp == null) throw new IllegalStateException("no response");
            if (resp.length >= 9) {
                byte[] first = new byte[9];
                System.arraycopy(resp, 0, first, 0, 9);
                return CU16Parser.parseStatus9(first);
            } else {
                throw new IllegalStateException("invalid response length");
            }
        });
    }
}