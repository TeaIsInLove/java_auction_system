//─────────────────────────────────────────────────────────────
package com.auction.network.protocol;

import java.io.Serializable;

/**
 * Generic serializable message passed over sockets.
 * The {@code payload} can be any Serializable domain object.
 */
public final class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final Object payload;   // Cast on receiver based on MessageType
    private final String sessionToken; // Simple auth token (userId)

    public Message(MessageType type, Object payload, String sessionToken) {
        this.type = type;
        this.payload = payload;
        this.sessionToken = sessionToken;
    }

    /** Convenience constructor for messages without a payload. */
    public Message(MessageType type, String sessionToken) {
        this(type, null, sessionToken);
    }

    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
    public String getSessionToken() { return sessionToken; }

    @Override
    public String toString() {
        return "Message[" + type + " | token=" + sessionToken + "]";
    }
}
