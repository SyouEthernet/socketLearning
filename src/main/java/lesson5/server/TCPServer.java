package lesson5.server;

import lesson5.server.handler.ClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class TCPServer {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();

    public TCPServer(int port) {
        this.port = port;
    }

    public boolean start() {
        try {
            ClientListener clientListener = new ClientListener(port);
            mListener = clientListener;
            clientListener.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
        }

        for (ClientHandler clientHandler :
                clientHandlerList) {
            clientHandler.exit();
        }

        clientHandlerList.clear();
    }

    public void broadcast(String str) {
        for (ClientHandler clientHandler : clientHandlerList) {
            clientHandler.send(str);
        }
    }

    private class ClientListener extends Thread {
        private boolean done = false;
        private ServerSocket serverSocket;

        public ClientListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            System.out.println("Server info: " + serverSocket.getInetAddress() + "\tport:" + serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("server ready!");

            Socket client;
            while (!done) {
                try {
                    client = serverSocket.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                try {
                    // async clienthandler construct
                    ClientHandler clientHandler = new ClientHandler(client, handler -> clientHandlerList.remove(handler));
                    // start to read and print
                    clientHandler.readToPrint();
                    clientHandlerList.add(clientHandler);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("client connect exception" + e.getMessage());
                }
            }

            System.out.println("server closed!");
        }

        void close() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                serverSocket = null;
            }
        }

        public void exit() {
            done = true;
            close();
            System.out.println("ClientListener exited.");
        }
    }
}
