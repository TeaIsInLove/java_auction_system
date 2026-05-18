// ═══════════════════════════════════════════════════════════
package com.auction.network.server;

import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.observer.AuctionObserver;
import com.auction.pattern.singleton.AuctionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multi-threaded auction server.
 *
 * Each connected client gets its own {@link ClientHandler} on a thread-pool thread.
 * When an auction event occurs, the server broadcasts it to all active handlers
 * that are watching the same auction (Observer push over socket).
 */
public class AuctionServer {

    private static final Logger LOG = Logger.getLogger(AuctionServer.class.getName());
    public static final int DEFAULT_PORT = 9090;

    private final int port;
    private final ExecutorService threadPool;
    private final Set<ClientHandler> connectedClients = ConcurrentHashMap.newKeySet();
    private volatile boolean running;

    public AuctionServer(int port) {
        this.port = port;
        this.threadPool = Executors.newCachedThreadPool();
    }

    /** Starts listening for connections. Blocks the calling thread. */
    public void start() {
        running = true;
        LOG.info("Auction server started on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this);
                connectedClients.add(handler);
                threadPool.submit(handler);
                LOG.info("New client connected: " + clientSocket.getRemoteSocketAddress());
            }
        } catch (IOException e) {
            if (running) LOG.log(Level.SEVERE, "Server error", e);
        }
    }

    /** Broadcasts an auction event to all connected clients watching that auction. */
    public void broadcast(AuctionEvent event) {
        for (ClientHandler handler : connectedClients) {
            handler.sendEvent(event);
        }
    }

    public void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        LOG.info("Client disconnected. Active clients: " + connectedClients.size());
    }

    public void stop() {
        running = false;
        threadPool.shutdownNow();
    }

    // ── Entry point for server process ───────────────────────────────────
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new AuctionServer(port).start();
    }
}
