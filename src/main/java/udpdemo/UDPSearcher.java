package udpdemo;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class UDPSearcher {
    private static final int LISTEN_PORT = 30000;

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("UDPSearcher started.");

        Listener listener = listen();
        sendBroadCast();



        // read any character and exit
        System.in.read();
        List<Device> devices = listener.getDevicesAndClose();

        for (Device device :
                devices) {
            System.out.println("Device:" + device.toString());
        }

        //close provider
        System.out.println("UDPSearcher close.");
    }

    private static Listener listen() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, countDownLatch);
        listener.start();

        countDownLatch.await();
        return listener;
    }

    private static void sendBroadCast() throws IOException {

        System.out.println("UDPSearcher sendBroadcast started.");

        // as a searcher , unnecessary to set port, system will set one
        DatagramSocket datagramSocket = new DatagramSocket();

        // construct data to request
        String requestData = MessageCreator.buildWithPort(LISTEN_PORT);
        byte[] requestDataBytes = requestData.getBytes();

        // send
        DatagramPacket requestPacket = new DatagramPacket(requestDataBytes, requestDataBytes.length);

        // set broadcast address
        requestPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPacket.setPort(20000);

        datagramSocket.send(requestPacket);

        //close provider
        System.out.println("UDPSearcher sendBroadcast close.");
        datagramSocket.close();
    }

    private static class Device {
        final int port;
        final String ip;
        final String sn;

        public Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> devices = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenPort, CountDownLatch countDownLatch) {
            super();
            this.listenPort = listenPort;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            super.run();
            // notify allready start
            countDownLatch.countDown();

            try {
                ds  = new DatagramSocket(listenPort);

                while(!done) {

                    final byte[] buffer = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    // receive
                    ds.receive(receivePacket);

                    // get provider information
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    String data = new String(receivePacket.getData(), 0, dataLen);
                    System.out.println("UDPProvider receive data from ip:" + ip + "\tport:" + port + "\tdata:" + data);

                    String sn = MessageCreator.parseSn(data);
                    if (sn!=null) {
                        Device device = new Device(port, ip, sn);
                        devices.add(device);
                    }
                }
            }catch (Exception e) {
            }finally {
                close();
            }
            System.out.println("UDPSearcher listener finished");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }


        List<Device> getDevicesAndClose() {
            done = true;
            close();
            return devices;
        }
    }
}
