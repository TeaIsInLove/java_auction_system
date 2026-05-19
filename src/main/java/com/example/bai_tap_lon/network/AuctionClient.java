package com.example.bai_tap_lon.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.Consumer;

/**
 * Singleton client that connects to AuctionServer.
 * The JavaFX app uses this to send auction events and receive real-time updates.
 * A background daemon thread reads incoming messages and invokes the registered callback
 * on whichever thread delivers it — callers must wrap in Platform.runLater() for UI updates.
 */
public class AuctionClient {

    private static final AuctionClient INSTANCE = new AuctionClient();

    private Socket socket;
    private ObjectOutputStream out;
    private volatile boolean connected;
    private Consumer<NetworkMessage> onMessageReceived;

    private AuctionClient() {}

    public static AuctionClient getInstance() {
        return INSTANCE;
    }

    /**
     * Attempt to connect to the server.
     * @return true if connection succeeded
     */
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            connected = true;

            Thread reader = new Thread(this::receiveLoop, "AuctionClient-Reader");
            reader.setDaemon(true);
            reader.start();

            System.out.println("[AuctionClient] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            connected = false;
            System.out.println("[AuctionClient] Could not connect to server: " + e.getMessage());
            return false;
        }
    }

    private void receiveLoop() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            while (connected) {
                NetworkMessage msg = (NetworkMessage) in.readObject();
                if (msg != null && onMessageReceived != null) {
                    onMessageReceived.accept(msg);
                }
            }
        } catch (EOFException | SocketException ignored) {
            // Server closed connection
        } catch (Exception e) {
            if (connected) {
                System.err.println("[AuctionClient] Receive error: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }

    /** Send a message to the server (which will broadcast it to all clients). */
    public void send(NetworkMessage message) {
        if (!connected || out == null) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            connected = false;
            System.err.println("[AuctionClient] Send failed: " + e.getMessage());
        }
    }

    /**
     * Register a callback invoked on every incoming message.
     * For JavaFX UI updates, wrap the body in Platform.runLater().
     */
    public void setOnMessageReceived(Consumer<NetworkMessage> handler) {
        this.onMessageReceived = handler;
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}
