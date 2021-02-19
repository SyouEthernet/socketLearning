package udpdemo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.UUID;

public class UDPProvider {
    public static void main(String[] args) throws IOException {
        // create a provider with uuid
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();

        // read any character and exit
        System.in.read();
        provider.exit();
    }

    private static class Provider extends Thread {
        private final String sn;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Provider(String sn) {
            super();
            this.sn = sn;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider started.");

            try {
                // as a receiver , set a port to receive
                ds = new DatagramSocket(20000);

                while (!done) {

                    final byte[] buffer = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                    // receive
                    ds.receive(receivePacket);

                    // get sender information
                    String ip = receivePacket.getAddress().getHostAddress();
                    int port = receivePacket.getPort();
                    int dataLen = receivePacket.getLength();
                    String data = new String(receivePacket.getData(), 0, dataLen);
                    System.out.println("UDPProvider receive data from ip:" + ip + "\tport:" + port + "\tdata:" + data);

                    // 获取到返回对端口号
                    int responsePort = MessageCreator.parsePort(data);
                    if (responsePort != -1) {

                        // construct data to send back
                        String responseData = MessageCreator.buildWithSn(sn);
                        byte[] responseDataByte = responseData.getBytes();

                        // send back
                        DatagramPacket responsePacket = new DatagramPacket(responseDataByte, responseDataByte.length, receivePacket.getAddress(), responsePort);

                        ds.send(responsePacket);
                    }
                }
            } catch (Exception e) {

            } finally {
                close();
            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        void exit() {
            done = true;
            close();
        }
    }
}
