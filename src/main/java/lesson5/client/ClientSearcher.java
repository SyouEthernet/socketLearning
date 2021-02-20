package lesson5.client;

import lesson5.client.bean.ServerInfo;
import lesson5.clink.utils.ByteUtils;
import lesson5.constants.UDPConstants;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientSearcher {
    private static final int LISTEN_PORT = UDPConstants.PORT_CLIENT_RESPONSE;

    public static ServerInfo searchServer(int timeout) {
        System.out.println("ClientSearcher Started.");

        CountDownLatch receiveLatch = new CountDownLatch(1);
        Listener listener = null;
        try {
            listener = listen(receiveLatch);
            sendBroadCast();
            receiveLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // finished
        System.out.println("ClientSearcher finished.");
        if (listener == null) {
            return null;
        }

        List<ServerInfo> devices = listener.getServerAndClose();
        if (devices.size() > 0) {
            return devices.get(0);
        }
        return null;
    }

    private static void sendBroadCast() throws IOException {
        System.out.println("ClientSearcher sendBroadCast started.");

        DatagramSocket ds = new DatagramSocket();

        // construct request data
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // header
        byteBuffer.put(UDPConstants.HEADER);
        // cmd
        byteBuffer.putShort((short)1);
        //sendback port info
        byteBuffer.putInt(LISTEN_PORT);
        // construct datagram packet
        DatagramPacket requestPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);
        // broadcast address
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        // set server port
        requestPacket.setPort(UDPConstants.PORT_SERVER);

        //send
        ds.send(requestPacket);
        ds.close();

        System.out.println("ClientSearcher sendBroadCast finished.");
    }

    public static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        System.out.println("UDPSearcher listen start.");
        CountDownLatch startDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startDownLatch, receiveLatch);
        listener.start();
        startDownLatch.await();
        return listener;
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final byte[] buffer = new byte[128];
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();
            // notify listener has started
            startDownLatch.countDown();
            try {
                // listen sendback port
                ds = new DatagramSocket(listenPort);
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while(!done) {
                    ds.receive(receivePacket);

                    //print sender info
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    byte[] data = receivePacket.getData();
                    boolean isValid = dataLen > minLen && ByteUtils.startsWith(data, UDPConstants.HEADER);

                    System.out.println("UDPSearcher receive from ip:" + ip + "\tport:" + port + "\tdataValid:" + isValid);

                    if (!isValid) {
                        //not valid, continue
                        continue;
                    }

                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, dataLen);
                    final short cmd = byteBuffer.getShort();
                    final int serverPort = byteBuffer.getInt();
                    if (cmd != 2 || serverPort <= 0) {
                        System.out.println("UDPSearcher receive cmd:" + cmd + "\tserverPort:" + serverPort);
                    }

                    String sn = new String(buffer, minLen, dataLen - minLen);
                    ServerInfo info = new ServerInfo(sn, serverPort, ip);
                    serverInfoList.add(info);
                    // receive success and finish
                    receiveDownLatch.countDown();
                }
            } catch (Exception e) {

            } finally {
                close();
                System.out.println("ClientSearcher listener closed.");
            }
            System.out.println("ClientSearcher listener finished.");
        }

        void close() {
            if (ds!= null) {
                ds.close();
                ds = null;
            }
        }


        public List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }
}
