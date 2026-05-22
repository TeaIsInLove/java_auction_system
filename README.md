# Java Auction System

A real-time online auction platform built with **JavaFX** and **SQLite**, developed as a university assignment for *Lập Trình Nâng Cao 2026*.

---

## Features

### Core (Bắt buộc)
| # | Feature | Status |
|---|---------|--------|
| 1 | User registration & login with SHA-256 password hashing | ✅ |
| 2 | Role-based access: **ADMIN** and **USER** | ✅ |
| 3 | Create, start, and end auction sessions | ✅ |
| 4 | Real-time bidding with thread-safe `ReentrantLock` | ✅ |
| 5 | Bid history per auction | ✅ |
| 6 | Payment tracking (PAID / FINISHED / CANCELED) | ✅ |
| 7 | Item types: Electronics, Art, Vehicle (Factory pattern) | ✅ |
| 8 | Persistent storage in SQLite via JDBC | ✅ |
| 9 | Unit tests (JUnit 5) with CI via GitHub Actions | ✅ |
| 10 | Admin user management (view, promote/demote, delete) | ✅ |

### Optional (Tùy chọn)
| Feature | Status |
|---------|--------|
| **Anti-snipe** — bids placed within 5 min of end time extend the auction by 5 min | ✅ |
| **Auto-bidding** — register a max price; the system bids automatically on your behalf | ✅ |
| **Bid trend chart** — live LineChart showing price progression per auction | ✅ |

---

## Design Patterns Used

| Pattern | Where |
|---------|-------|
| **Singleton** | `AuctionManager`, `AuctionWorkspace`, `SessionManager`, `AuctionClient`, `AutoBidManager` |
| **Observer** | `AuctionSubject` / `AuctionObserver` — views subscribe to auction events |
| **Factory** | `ItemFactory.createItem(type, ...)` — creates Electronics / Art / Vehicle |
| **MVC** | JavaFX FXML controllers + model classes + repository layer |

---

## Architecture

```
Launcher (JavaFX Application)
│
├── Controllers/          UI controllers (JavaFX FXML)
│   ├── AuctionWorkspace  Central business-logic facade (Singleton)
│   ├── AutoBidManager    Auto-bid registrations (Singleton)
│   └── ...
│
├── model/
│   ├── auction/          Auction, AuctionManager, Observer interfaces
│   └── entity/           Item (Art/Electronics/Vehicle), BidTransaction, User types
│
├── auth/                 Repositories (SQLite JDBC), AuthService, PasswordUtil
├── session/              SessionManager — holds the currently logged-in user
├── network/              AuctionServer, AuctionClient, NetworkMessage (real-time sync)
└── exception/            Custom exceptions (AuctionException hierarchy)
```

### Real-time Updates
The app uses a lightweight embedded TCP server (`AuctionServer` on port 9999).  
- The first JVM instance starts the server automatically.  
- Subsequent instances connect as clients.  
- When a bid is placed, an `NetworkMessage` is broadcast to all connected clients.  
- Each client reloads the affected auction from SQLite so that stale in-memory state is never displayed.

---

## Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 21 LTS or 25+ |
| Maven | 3.9+ (or use the included `mvnw` wrapper) |

---

## Running the Application

```bash
# Clone
git clone https://github.com/<your-org>/java_auction_system.git
cd java_auction_system

# Build and run (Windows)
mvnw.cmd javafx:run

# Build and run (macOS / Linux)
./mvnw javafx:run
```

On first run, the SQLite database is created automatically at `~/.auctionsystem/app.db`.

### Default Admin Account
| Field | Value |
|-------|-------|
| Email | `admin@local` |
| Password | `admin123` |

---

## Running Tests

```bash
./mvnw clean test
```

Tests run headless (no JavaFX display required) via `maven-surefire-plugin` with `useModulePath=false`.

---

## CI / CD

GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push and pull request to `main` / `dev`:
1. Checks out code
2. Sets up JDK 21 (Temurin)
3. Runs `mvn clean test`
4. Uploads Surefire test reports as build artifacts

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/bai_tap_lon/
│   │   ├── Controllers/        JavaFX controllers + workspace facade
│   │   ├── auth/               Repositories, AuthService, DB manager
│   │   ├── exception/          Custom exception hierarchy
│   │   ├── model/              Domain model (Auction, Item, User, …)
│   │   ├── network/            TCP server/client for real-time sync
│   │   └── session/            SessionManager
│   └── resources/
│       └── com/example/auctionsystem/Views/
│           ├── fxml/           FXML layout files
│           └── css/            Stylesheets
└── test/
    └── java/com/example/bai_tap_lon/
        ├── AuctionTest.java
        ├── AuthServiceTest.java
        ├── ItemFactoryTest.java
        └── PasswordUtilTest.java
```

---

## License

This project is for educational purposes only.
