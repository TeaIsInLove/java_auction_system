package com.example.bai_tap_lon.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-client auction server. Each connecting JavaFX client gets its own
 * ClientHandler thread. Any message sent by one client is broadcast to all
 * connected clients so every UI refreshes in real-time.
 *
 * Run standalone:  java -cp <classpath> com.example.bai_tap_lon.network.AuctionServer
 * Or started embedded by Launcher when no server is detected on the port.
 */
public class AuctionServer {

    public static final int PORT = 9999;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private ServerSocket serverSocket;
    private volatile boolean running;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        System.out.println("[AuctionServer] Started on port " + PORT);

        ExecutorService pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AuctionServer-Worker");
            t.setDaemon(true);
            return t;
        });

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                clients.add(handler);
                pool.submit(handler);
                System.out.println("[AuctionServer] Client connected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                if (running) {
                    System.err.println("[AuctionServer] Accept error: " + e.getMessage());
                }
            }
        }
        pool.shutdown();
    }

    /** Broadcast a message to every connected client. */
    public void broadcast(NetworkMessage message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("[AuctionServer] Client disconnected. Active clients: " + clients.size());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[AuctionServer] Error on stop: " + e.getMessage());
        }
    }

    /** Entry point to run server as a standalone process. */
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "ServerShutdown"));
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[AuctionServer] Fatal: " + e.getMessage());
        }
    }
}
