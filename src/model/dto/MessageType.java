═══════════════════════════════════════════════════════════
package com.auction.network.protocol;

/** All message types exchanged between client and server. */
public enum MessageType {
    // ── Auth ──────────────────────────────────────────────
    LOGIN_REQUEST,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    REGISTER_REQUEST,
    REGISTER_SUCCESS,
    REGISTER_FAILURE,

    // ── Auction ───────────────────────────────────────────
    GET_AUCTIONS,
    AUCTION_LIST,
    GET_AUCTION_DETAIL,
    AUCTION_DETAIL,
    CREATE_AUCTION,
    AUCTION_CREATED,
    CANCEL_AUCTION,

    // ── Bidding ───────────────────────────────────────────
    PLACE_BID,
    BID_SUCCESS,
    BID_FAILURE,
    AUTO_BID,

    // ── Server → Client push (realtime) ──────────────────
    AUCTION_UPDATE,   // pushed to all clients watching an auction
    ERROR,
    DISCONNECT
}
