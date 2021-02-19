package socketdemo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(2000);

        System.out.println("server ready");
        System.out.println("服务器信息：" + serverSocket.getInetAddress() + " port:" + serverSocket.getLocalPort());

        for (;;) {
            // wait client connect
            Socket client = serverSocket.accept();

            // construct client thread
            ClientHandler clientHandler = new ClientHandler(client);
            // start thread
            clientHandler.start();
        }
    }


    private static class ClientHandler extends Thread {
        private Socket socket;
        private boolean flag = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("new client info:" + socket.getInetAddress() + " port:" + socket.getPort());

            try {
                // get printStream, for data output
                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                // get input stream, for data receive
                BufferedReader socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                do {
                    //get data
                    String str = socketInput.readLine();
                    if ("bye".equalsIgnoreCase(str)) {
                        flag = false;
                        // send bye
                        socketOutput.println("bye");
                    } else {
                        System.out.println(str);
                        socketOutput.println("send:" + str.length());
                    }
                } while(flag);
                socketInput.close();
                socketOutput.close();

            } catch (Exception e) {
                System.out.println("connect exit");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("client closed: " + socket.getInetAddress() + " port:" + socket.getPort());
        }
    }
}
