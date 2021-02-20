package lesson5.server;

import lesson5.clink.utils.ByteUtils;
import lesson5.constants.UDPConstants;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class UDPProvider {
    private static Provider PROVIDER_INSTANCE;

    static void start(int port) {
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn.getBytes(), port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }


    private static class Provider extends Thread {
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;
        // message buffer
        final byte[] buffer = new byte[128];

        public Provider(byte[] sn, int port) {
            this.sn = sn;
            this.port = port;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPProvider started.");

            try {
                // listen port
                ds = new DatagramSocket(UDPConstants.PORT_SERVER);
                // receive packet
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    // receive
                    ds.receive(receivePacket);

                    // print received message and sender info
                    String clientIp = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int clientDataLen = receivePacket.getLength();
                    byte[] clientData = receivePacket.getData();
                    boolean isValid = clientDataLen >= (UDPConstants.HEADER.length + 2 + 4) && ByteUtils.startsWith(clientData, UDPConstants.HEADER);

                    System.out.println("ServerProvider receive from ip:" + clientIp + "\tport:" + clientPort + "\tdataValid:" + isValid);

                    if (!isValid) {
                        continue;
                    }

                    // parse cmd and send back
                    int index = UDPConstants.HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int responsePort = (((clientData[index++]) << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            ((clientData[index] & 0xff)));

                    // check isValid
                    if (cmd == 1 && responsePort > 0) {
                        // construct data to send back
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);
                        int len = byteBuffer.position();
                        // directly send back message according to sender
                        DatagramPacket responsPacket = new DatagramPacket(buffer, len, receivePacket.getAddress(), responsePort);
                        ds.send(responsPacket);
                        System.out.println("ServerProvider response to:" + clientIp + "\tresponsePort:" + responsePort + "\tdatalen:" + len);
                    } else {
                        System.out.println("ServerProvider cmd nonsupport; cmd: " + cmd + "\tresponsePort:" + responsePort);
                    }
                }
            } catch (Exception e) {

            } finally {
                System.out.println("UDPProvider finish.");
                close();
            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        // exit
        void exit() {
            done = true;
            close();
        }
    }
}
