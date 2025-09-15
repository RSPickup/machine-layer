package com.example.parcellocker.cu16;


import java.util.HashMap;
import java.util.Map;

public class HardwareMock {
    private final Map<Integer, boolean[]> cuBoards = new HashMap<>();

    public HardwareMock() {
        // Create 1 CU board with 16 doors, all locked initially
        boolean[] doors = new boolean[16];
        for (int i = 0; i < 16; i++) {
            doors[i] = true; // true = locked
        }
        cuBoards.put(0, doors); // CU ID 0
    }

    public synchronized void unlockDoor(int cuId, int doorIndex) {
        boolean[] doors = cuBoards.get(cuId);
        if (doors != null && doorIndex >= 0 && doorIndex < doors.length) {
            doors[doorIndex] = false;
        }
    }

    public synchronized void unlockAll(int cuId) {
        boolean[] doors = cuBoards.get(cuId);
        if (doors != null) {
            for (int i = 0; i < doors.length; i++) {
                doors[i] = false;
            }
        }
    }

    public synchronized boolean[] getDoorStates(int cuId) {
        return cuBoards.getOrDefault(cuId, new boolean[0]);
    }
}