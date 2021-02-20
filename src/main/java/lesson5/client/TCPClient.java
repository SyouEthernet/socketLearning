package lesson5.client;

import lesson5.client.bean.ServerInfo;
import lesson5.clink.utils.CloseUtils;
import lesson5.server.handler.ClientHandler;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPClient {
    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        // set read timeout
        socket.setSoTimeout(3000);

        //connect server
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        System.out.println("start to connect server~");
        System.out.println("client info: " + socket.getLocalAddress() + "\tport:" + socket.getLocalPort());
        System.out.println("server info: " + socket.getInetAddress() + "\tport:" + socket.getPort());

        try {
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();

            // send data
            write(socket);

            // exit
            readHandler.exit();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("exit in exception.");
        }

        socket.close();
        System.out.println("client is exited.");
    }

    private static void write(Socket client) throws IOException {
        // read from keyboard
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        // get socket outputStream and trans to printStream
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        while (true) {
            // read from keyboard
            String str = input.readLine();
            // send to server
            socketPrintStream.println(str);
            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        }

        // release resource
        socketPrintStream.close();
    }

    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        public ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();
            try {
                // get inputStream for data receive
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    // get one line data
                    String str;
                    try {
                        str = socketInput.readLine();
                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        System.out.println("connection closed,cannot read data!");
                        break;
                    }
                    System.out.println(str);
                } while (!done);
                socketInput.close();
            } catch (Exception e) {
                e.printStackTrace();
                if (!done) {
                    System.out.println("exception disconnect" + e.getMessage());
                }
            } finally {
                CloseUtils.close(inputStream);
            }
        }

        void exit() {
            done = true;
            CloseUtils.close(inputStream);
        }
    }
}
