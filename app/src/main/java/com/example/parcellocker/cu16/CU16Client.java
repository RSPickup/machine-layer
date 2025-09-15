package com.example.parcellocker.cu16;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

///
//        * Low-level TCP client to talk to the BU/simulator.
//        * Use from background thread only.
//        */
public class CU16Client {
    private final String ip;
    private final int port;
    private int timeoutMs = 3000;

    public CU16Client(String ip, int port) {
        this.ip = ip; this.port = port;
    }

    public void setTimeoutMs(int ms) { this.timeoutMs = ms; }

//    / Send packet and return raw response bytes (may be multiple frames) */
    public synchronized byte[] sendAndReceive(byte[] packet) throws Exception {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(ip, port), timeoutMs);
            s.setSoTimeout(timeoutMs);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write(packet);
            out.flush();

            byte[] buf = new byte[256];
            int n = in.read(buf);
            if (n <= 0) return null;
            byte[] outb = new byte[n];
            System.arraycopy(buf, 0, outb, 0, n);
            return outb;
        }
    }
}