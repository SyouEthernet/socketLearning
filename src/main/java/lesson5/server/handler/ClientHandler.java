package lesson5.server.handler;

import lesson5.clink.utils.CloseUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private final Socket client;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final CloseNotify closeNotify;

    public ClientHandler(Socket client, CloseNotify closeNotify) throws IOException {
        this.client = client;
        this.readHandler = new ClientReadHandler(client.getInputStream());
        this.writeHandler = new ClientWriteHandler(client.getOutputStream());
        this.closeNotify = closeNotify;

        System.out.println("new client connected:" + client.getInetAddress() + "\tport:" + client.getPort());
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(client);
        System.out.println("client finished:" + client.getInetAddress() + "\tport:" + client.getPort());
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        closeNotify.onSelfClosed(this);
    }

    public interface CloseNotify{
        void onSelfClosed(ClientHandler handler);
    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        public ClientReadHandler(InputStream inputStream) {
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
                    String str = socketInput.readLine();
                    if (str == null) {
                        System.out.println("client cannot read data!");
                        // exit client
                        ClientHandler.this.exitBySelf();
                        break;
                    }
                    System.out.println(str);
                } while (!done);
                socketInput.close();
            } catch (Exception e) {
                e.printStackTrace();
                if (!done) {
                    System.out.println("exception disconnect");
                    ClientHandler.this.exitBySelf();
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

    class ClientWriteHandler {
        private boolean done = false;
        private final PrintStream printStream;
        private final ExecutorService executorService;

        public ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }

        void exit() {
            done = true;
            CloseUtils.close(printStream);
            executorService.shutdownNow();
        }

        void send(String str) {
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final String msg;

            public WriteRunnable(String msg) {
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                try {
                    ClientWriteHandler.this.printStream.println(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
