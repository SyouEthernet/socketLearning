package socketdemo;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();

        // 超时时间
        socket.setSoTimeout(3000);

        // 连接本地端口2000， 超时时间3000
        socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 2000), 3000);

        System.out.println("发起服务器连接，并且进入后续流程");
        System.out.println("客户端信息：" + socket.getLocalAddress() + " port: "+ socket.getLocalPort());
        System.out.println("服务器信息：" + socket.getInetAddress() + "port:" + socket.getPort());

        try {
            // send data
            todo(socket);
        } catch (Exception e) {
            System.out.println("exception closed.");
        }

        socket.close();
        System.out.println("client exit");
    }

    private static void todo(Socket client) throws IOException {
        // construct input stream
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        // get socket output stream, and trans to printStream
        OutputStream outputStream = client.getOutputStream();
        PrintStream socketPrintStream = new PrintStream(outputStream);

        // get socket input stream
        InputStream inputStream = client.getInputStream();
        BufferedReader socketBufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        boolean flag  = true;

        do {
            // read keyboard
            String str = input.readLine();
            // send to server
            socketPrintStream.println(str);

            // read from server
            String echo = socketBufferedReader.readLine();
            if ("bye".equalsIgnoreCase(echo)) {
                flag = false;
            } else {
                System.out.println(echo);
            }
        } while(flag);
    }
}
