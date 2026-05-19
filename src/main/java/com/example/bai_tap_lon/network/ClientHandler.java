package com.example.bai_tap_lon.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

/** Handles one connected client on the server side. */
class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;
    private ObjectOutputStream out;

    ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            NetworkMessage message;
            while ((message = (NetworkMessage) in.readObject()) != null) {
                // Relay every incoming message to all connected clients
                server.broadcast(message);
            }
        } catch (EOFException | SocketException ignored) {
            // Client disconnected cleanly
        } catch (Exception e) {
            System.err.println("[ClientHandler] Error: " + e.getMessage());
        } finally {
            server.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    void send(NetworkMessage message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
                out.reset(); // Prevent stale object graph caching
            }
        } catch (IOException e) {
            server.removeClient(this);
        }
    }
}
