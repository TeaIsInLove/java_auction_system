package com.example.bai_tap_lon.network;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        BID_PLACED, AUCTION_CREATED, AUCTION_STARTED,
        AUCTION_ENDED, AUCTION_CANCELLED, PING
    }

    private final Type type;
    private final String auctionId;
    private final String username;
    private final double amount;
    private final String payload;

    public NetworkMessage(Type type, String auctionId, String username, double amount, String payload) {
        this.type = type;
        this.auctionId = auctionId;
        this.username = username;
        this.amount = amount;
        this.payload = payload;
    }

    public Type getType()      { return type; }
    public String getAuctionId() { return auctionId; }
    public String getUsername()  { return username; }
    public double getAmount()    { return amount; }
    public String getPayload()   { return payload; }

    @Override
    public String toString() {
        return "NetworkMessage{type=" + type + ", auctionId='" + auctionId + "', user='" + username + "', amount=" + amount + "}";
    }
}
