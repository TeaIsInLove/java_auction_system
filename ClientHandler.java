package com.auction.network;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.model.Auction;
import com.auction.model.Bid;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.User;
import com.auction.network.protocol.Message;
import com.auction.network.protocol.MessageType;
import com.auction.pattern.observer.AuctionEvent;
import com.auction.pattern.singleton.AuctionManager;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import com.auction.service.UserService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all communication with a single connected client.
 * Runs on its own thread (submitted by {@link AuctionServer}).
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final AuctionServer server;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private User authenticatedUser;

    private final UserService    userService    = new UserService();
    private final AuctionService auctionService = new AuctionService();
    private final BidService     bidService     = new BidService();

    public ClientHandler(Socket socket, AuctionServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            Message msg;
            while ((msg = (Message) in.readObject()) != null) {
                handle(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            LOG.info("Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            server.removeClient(this);
            close();
        }
    }

    // ── Dispatch ─────────────────────────────────────────────────────────
    private void handle(Message msg) {
        LOG.fine("Received: " + msg);
        try {
            switch (msg.getType()) {
                case LOGIN_REQUEST    -> handleLogin(msg);
                case REGISTER_REQUEST -> handleRegister(msg);
                case GET_AUCTIONS     -> handleGetAuctions(msg);
                case GET_AUCTION_DETAIL -> handleGetDetail(msg);
                case PLACE_BID        -> handlePlaceBid(msg);
                case AUTO_BID         -> handleAutoBid(msg);
                case CREATE_AUCTION   -> handleCreateAuction(msg);
                case CANCEL_AUCTION   -> handleCancelAuction(msg);
                case DISCONNECT       -> close();
                default -> send(new Message(MessageType.ERROR, "Unknown message type", null));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error handling message", e);
            send(new Message(MessageType.ERROR, e.getMessage(), null));
        }
    }

    private void handleLogin(Message msg) throws IOException {
        String[] creds = (String[]) msg.getPayload(); // [username, password]
        try {
            authenticatedUser = userService.login(creds[0], creds[1]);
            send(new Message(MessageType.LOGIN_SUCCESS, authenticatedUser, authenticatedUser.getId()));
        } catch (AuthenticationException e) {
            send(new Message(MessageType.LOGIN_FAILURE, e.getMessage(), null));
        }
    }

    private void handleRegister(Message msg) throws IOException {
        Object[] data = (Object[]) msg.getPayload(); // [role, username, password, email, ...]
        String role = (String) data[0];
        try {
            User user = switch (role) {
                case "BIDDER" -> userService.registerBidder(
                        (String) data[1], (String) data[2], (String) data[3],
                        Double.parseDouble((String) data[4]));
                case "SELLER" -> userService.registerSeller(
                        (String) data[1], (String) data[2], (String) data[3]);
                default -> throw new IllegalArgumentException("Unknown role: " + role);
            };
            send(new Message(MessageType.REGISTER_SUCCESS, user, user.getId()));
        } catch (Exception e) {
            send(new Message(MessageType.REGISTER_FAILURE, e.getMessage(), null));
        }
    }

    private void handleGetAuctions(Message msg) throws IOException {
        send(new Message(MessageType.AUCTION_LIST,
                auctionService.getAllAuctions(), msg.getSessionToken()));
    }

    private void handleGetDetail(Message msg) throws IOException {
        String auctionId = (String) msg.getPayload();
        Auction auction = auctionService.findById(auctionId).orElse(null);
        send(new Message(MessageType.AUCTION_DETAIL, auction, msg.getSessionToken()));
    }

    private void handlePlaceBid(Message msg) throws IOException {
        Object[] data = (Object[]) msg.getPayload(); // [auctionId, amount]
        String auctionId = (String) data[0];
        double amount    = (Double) data[1];

        if (!(authenticatedUser instanceof Bidder bidder)) {
            send(new Message(MessageType.BID_FAILURE, "Only bidders can place bids.", null));
            return;
        }
        try {
            Bid bid = bidService.placeBid(auctionId, bidder, amount);
            send(new Message(MessageType.BID_SUCCESS, bid, msg.getSessionToken()));
            // Broadcast updated auction to all clients
            auctionService.findById(auctionId).ifPresent(a ->
                    server.broadcast(new AuctionEvent(AuctionEvent.Type.BID_PLACED, a, bid)));
        } catch (AuctionClosedException | InvalidBidException e) {
            send(new Message(MessageType.BID_FAILURE, e.getMessage(), null));
        }
    }

    private void handleAutoBid(Message msg) throws IOException {
        Object[] data = (Object[]) msg.getPayload(); // [auctionId, maxBid]
        String auctionId = (String) data[0];
        double maxBid    = (Double) data[1];

        if (!(authenticatedUser instanceof Bidder bidder)) {
            send(new Message(MessageType.BID_FAILURE, "Only bidders can auto-bid.", null));
            return;
        }
        try {
            Bid bid = bidService.autoBid(auctionId, bidder, maxBid);
            if (bid != null) {
                send(new Message(MessageType.BID_SUCCESS, bid, msg.getSessionToken()));
                auctionService.findById(auctionId).ifPresent(a ->
                        server.broadcast(new AuctionEvent(AuctionEvent.Type.BID_PLACED, a, bid)));
            } else {
                send(new Message(MessageType.BID_FAILURE, "Max bid already exceeded.", null));
            }
        } catch (AuctionClosedException | InvalidBidException e) {
            send(new Message(MessageType.BID_FAILURE, e.getMessage(), null));
        }
    }

    private void handleCreateAuction(Message msg) throws IOException {
        if (!(authenticatedUser instanceof Seller seller)) {
            send(new Message(MessageType.ERROR, "Only sellers can create auctions.", null));
            return;
        }
        // Payload: Auction object already built on the client side
        Auction auction = (Auction) msg.getPayload();
        // Re-register through service so scheduling is applied
        auctionService.createAuction(
                seller,
                auction.getItem(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getMinimumIncrement());
        send(new Message(MessageType.AUCTION_CREATED, auction, msg.getSessionToken()));
    }

    private void handleCancelAuction(Message msg) throws IOException {
        String auctionId = (String) msg.getPayload();
        auctionService.cancelAuction(auctionId);
        send(new Message(MessageType.AUCTION_UPDATE, auctionId, msg.getSessionToken()));
    }

    // ── I/O helpers ──────────────────────────────────────────────────────
    public synchronized void sendEvent(AuctionEvent event) {
        send(new Message(MessageType.AUCTION_UPDATE, event, null));
    }

    private synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // Prevent caching of mutable objects
        } catch (IOException e) {
            LOG.warning("Failed to send message: " + e.getMessage());
        }
    }

    private void close() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
