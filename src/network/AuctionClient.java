// ═══════════════════════════════════════════════════════════
package com.auction.network.client;

import com.auction.network.protocol.Message;
import com.auction.network.protocol.MessageType;
import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.observer.AuctionObserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client-side network layer.
 *
 * Sends requests to the server and dispatches incoming push messages
 * (AUCTION_UPDATE) to registered {@link AuctionObserver}s on the
 * JavaFX Application Thread via the provided callback.
 */
public class AuctionClient {

    private static final Logger LOG = Logger.getLogger(AuctionClient.class.getName());

    private final String host;
    private final int port;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String sessionToken;
    private boolean connected;

    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    private final List<Consumer<Message>> responseListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService listenerThread = Executors.newSingleThreadExecutor();

    public AuctionClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // ── Connection ───────────────────────────────────────────────────────
    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;
        startListening();
        LOG.info("Connected to server at " + host + ":" + port);
    }

    public void disconnect() {
        connected = false;
        send(new Message(MessageType.DISCONNECT, sessionToken));
        listenerThread.shutdownNow();
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ── Background listener ──────────────────────────────────────────────
    private void startListening() {
        listenerThread.submit(() -> {
            while (connected) {
                try {
                    Message msg = (Message) in.readObject();
                    dispatch(msg);
                } catch (IOException | ClassNotFoundException e) {
                    if (connected) LOG.log(Level.WARNING, "Connection lost", e);
                    connected = false;
                }
            }
        });
    }

    private void dispatch(Message msg) {
        if (msg.getType() == MessageType.AUCTION_UPDATE
                && msg.getPayload() instanceof AuctionEvent event) {
            // Push to all registered observers (e.g., UI controllers)
            for (AuctionObserver obs : observers) {
                obs.onAuctionEvent(event);
            }
        } else {
            // Response to a specific request
            for (Consumer<Message> listener : responseListeners) {
                listener.accept(msg);
            }
        }
    }

    // ── Observer management ──────────────────────────────────────────────
    public void addObserver(AuctionObserver obs) { observers.add(obs); }
    public void removeObserver(AuctionObserver obs) { observers.remove(obs); }

    /** Register a one-time response listener (e.g., for login callback). */
    public void addResponseListener(Consumer<Message> listener) {
        responseListeners.add(listener);
    }
    public void removeResponseListener(Consumer<Message> listener) {
        responseListeners.remove(listener);
    }

    // ── Requests ─────────────────────────────────────────────────────────
    public void login(String username, String password) {
        send(new Message(MessageType.LOGIN_REQUEST,
                new String[]{username, password}, null));
    }

    public void register(String role, String username, String password,
                         String email, String extra) {
        send(new Message(MessageType.REGISTER_REQUEST,
                new Object[]{role, username, password, email, extra}, null));
    }

    public void getAuctions() {
        send(new Message(MessageType.GET_AUCTIONS, sessionToken));
    }

    public void getAuctionDetail(String auctionId) {
        send(new Message(MessageType.GET_AUCTION_DETAIL, auctionId, sessionToken));
    }

    public void placeBid(String auctionId, double amount) {
        send(new Message(MessageType.PLACE_BID,
                new Object[]{auctionId, amount}, sessionToken));
    }

    public void autoBid(String auctionId, double maxBid) {
        send(new Message(MessageType.AUTO_BID,
                new Object[]{auctionId, maxBid}, sessionToken));
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to send", e);
        }
    }

    public void setSessionToken(String token) { this.sessionToken = token; }
    public String getSessionToken() { return sessionToken; }
    public boolean isConnected() { return connected; }
