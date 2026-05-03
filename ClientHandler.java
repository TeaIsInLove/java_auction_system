package com.auction.network;

import com.auction.exception.AuctionClosedException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.InvalidBidException;
import com.auction.manager.AuctionManager;
import com.auction.model.Auction;
import com.auction.model.Bid;
import com.auction.model.User;
import com.auction.model.item.Item;
import com.auction.pattern.AuctionObserver;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * TUẦN 9+10 - ClientHandler: Runnable xử lý 1 client kết nối
 *
 * Mỗi client kết nối → Server tạo 1 ClientHandler chạy trong ThreadPool.
 * ClientHandler:
 *   1. Đọc Request từ client (ObjectInputStream)
 *   2. Gọi AuctionManager xử lý nghiệp vụ
 *   3. Trả Response về client (ObjectOutputStream)
 *
 * AuctionObserver: ClientHandler implements AuctionObserver.
 * Khi có bid mới → onBidPlaced() được gọi → push Response đến client này.
 * → Đây là cơ chế REALTIME UPDATE qua Socket.
 *
 * Thread-safety:
 *   - out (ObjectOutputStream) cần synchronized khi push vì
 *     push (từ auction thread) và reply (từ handler thread) có thể đồng thời.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket       socket;
    private ObjectInputStream  in;
    private ObjectOutputStream out;
    private final Object       outLock = new Object(); // lock cho việc ghi ra socket

    private final AuctionManager manager = AuctionManager.getInstance();
    private String sessionToken = null; // null = chưa đăng nhập

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // ── Main loop ────────────────────────────────────────────────────────────
    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        System.out.println("[Handler] Client connected: " + clientAddr);

        try {
            // QUAN TRỌNG: tạo ObjectOutputStream TRƯỚC ObjectInputStream
            // Nếu ngược lại → cả 2 phía chờ nhau → deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // flush header ngay
            in  = new ObjectInputStream(socket.getInputStream());

            // Vòng lặp xử lý request
            while (!socket.isClosed()) {
                Request request = (Request) in.readObject();
                Response response = dispatch(request);
                sendResponse(response);
            }

        } catch (EOFException | java.net.SocketException e) {
            // Client ngắt kết nối bình thường
            System.out.println("[Handler] Client disconnected: " + clientAddr);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Handler] Error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ── Request dispatcher ────────────────────────────────────────────────────
    /**
     * Phân loại Request và gọi handler tương ứng.
     * Mọi exception nghiệp vụ → Response.error() thay vì crash handler.
     */
    private Response dispatch(Request request) {
        System.out.println("[Handler] Received: " + request);
        try {
            switch (request.getType()) {
                case LOGIN:             return handleLogin(request);
                case LOGOUT:            return handleLogout(request);
                case REGISTER:          return handleRegister(request);
                case GET_ALL_AUCTIONS:  return handleGetAllAuctions(request);
                case GET_AUCTION_BY_ID: return handleGetAuctionById(request);
                case PLACE_BID:         return handlePlaceBid(request);
                case CREATE_AUCTION:    return handleCreateAuction(request);
                case SUBSCRIBE_AUCTION: return handleSubscribe(request);
                case UNSUBSCRIBE_AUCTION: return handleUnsubscribe(request);
                default:
                    return Response.error("Unknown request type: " + request.getType());
            }
        } catch (AuthenticationException e) {
            return Response.unauthorized(e.getMessage());
        } catch (AuctionClosedException | InvalidBidException e) {
            return Response.error(e.getMessage());
        } catch (Exception e) {
            System.err.println("[Handler] Unexpected error: " + e);
            return Response.error("Server error: " + e.getMessage());
        }
    }

    // ── Request handlers ──────────────────────────────────────────────────────

    private Response handleLogin(Request req) throws AuthenticationException {
        LoginPayload p = (LoginPayload) req.getPayload();
        String token = manager.login(p.getUsername(), p.getPasswordHash());
        this.sessionToken = token; // lưu token cho session này
        return Response.ok(token); // trả token về client
    }

    private Response handleLogout(Request req) {
        manager.logout(sessionToken);
        sessionToken = null;
        return Response.ok("Logged out.");
    }

    private Response handleRegister(Request req) throws AuthenticationException {
        RegisterPayload p = (RegisterPayload) req.getPayload();
        User user = manager.register(p.getUsername(), p.getPasswordHash(),
                                     p.getEmail(), p.getRole());
        return Response.ok(user);
    }

    private Response handleGetAllAuctions(Request req) {
        List<Auction> list = manager.getAllAuctions();
        return Response.ok(list);
    }

    private Response handleGetAuctionById(Request req) throws InvalidBidException {
        String auctionId = (String) req.getPayload();
        Auction auction = manager.getAuction(auctionId);
        if (auction == null) return Response.error("Auction not found: " + auctionId);
        return Response.ok(auction);
    }

    private Response handlePlaceBid(Request req)
            throws AuthenticationException, AuctionClosedException, InvalidBidException {
        BidPayload p = (BidPayload) req.getPayload();
        // Dùng token từ Request (client gửi kèm) hoặc session token của handler
        String token = req.getSessionToken() != null ? req.getSessionToken() : sessionToken;
        Bid bid = manager.placeBid(token, p.getAuctionId(), p.getAmount());
        return Response.ok(bid);
    }

    private Response handleCreateAuction(Request req)
            throws AuthenticationException {
        // payload = CreateAuctionPayload (tạo tương tự BidPayload nếu cần)
        // Simplified: client gửi Auction object trực tiếp (thực tế nên dùng DTO)
        Object payload = req.getPayload();
        return Response.error("Use CreateAuctionPayload - implement when needed.");
    }

    private Response handleSubscribe(Request req) throws AuthenticationException {
        String auctionId = (String) req.getPayload();
        manager.subscribeToAuction(auctionId, this); // đăng ký this làm observer
        return Response.ok("Subscribed to auction: " + auctionId);
    }

    private Response handleUnsubscribe(Request req) {
        String auctionId = (String) req.getPayload();
        manager.unsubscribeFromAuction(auctionId, this);
        return Response.ok("Unsubscribed from: " + auctionId);
    }

    // ── AuctionObserver: REALTIME PUSH ────────────────────────────────────────

    /**
     * Được gọi bởi Auction khi có bid mới.
     * Chạy trên auction thread → phải synchronized với out.
     */
    @Override
    public void onBidPlaced(Auction auction, Bid newBid) {
        sendResponse(Response.pushBidUpdate(auction));
    }

    /**
     * Được gọi khi phiên đấu giá đóng.
     */
    @Override
    public void onAuctionClosed(Auction auction) {
        sendResponse(Response.pushAuctionClose(auction));
    }

    // ── I/O helpers ───────────────────────────────────────────────────────────

    /**
     * Gửi response về client.
     * synchronized(outLock): tránh 2 thread ghi đồng thời vào ObjectOutputStream
     * (reply thread + push thread có thể đồng thời).
     */
    private void sendResponse(Response response) {
        synchronized (outLock) {
            try {
                out.writeObject(response);
                out.flush();
                // reset(): tránh ObjectOutputStream cache reference cũ
                // (quan trọng khi gửi object đã thay đổi)
                out.reset();
            } catch (IOException e) {
                System.err.println("[Handler] Send failed: " + e.getMessage());
            }
        }
    }

    /** Dọn dẹp khi client ngắt kết nối */
    private void cleanup() {
        // Hủy tất cả subscription
        manager.unsubscribeFromAll(this);
        // Logout session
        if (sessionToken != null) {
            manager.logout(sessionToken);
        }
        // Đóng socket
        try { socket.close(); } catch (IOException ignored) {}
    }
}
