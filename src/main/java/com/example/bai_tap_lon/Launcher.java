package com.example.bai_tap_lon;

import com.example.bai_tap_lon.Controllers.AuctionWorkspace;
import com.example.bai_tap_lon.network.AuctionClient;
import com.example.bai_tap_lon.network.AuctionServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Launcher extends Application {

    /** Reference kept so the embedded server's sync handler can be registered. */
    private AuctionServer embeddedServer;

    @Override
    public void start(Stage stage) throws Exception {
        AuctionWorkspace.getInstance().startPendingApprovalCleaner();
        connectToServer();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/auctionsystem/Views/fxml/login.fxml")
        );

        Scene scene = new Scene(loader.load(), 600, 400);
        stage.setScene(scene);
        stage.setTitle("Auction App");
        stage.show();
    }

    /**
     * Resolve the server host:
     *  1. Read {@code ~/.auctionsystem/server.properties} — key {@code server.host}
     *  2. Fallback to {@code localhost}
     *
     * <p>To run as a CLIENT on a different machine, create the file with:
     * <pre>server.host=192.168.x.x</pre>
     * where the IP is the machine running the first instance (the embedded server host).
     */
    private String resolveServerHost() {
        Path configFile = Paths.get(System.getProperty("user.home"), ".auctionsystem", "server.properties");
        if (Files.exists(configFile)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
                String host = props.getProperty("server.host", "").trim();
                if (!host.isEmpty()) {
                    System.out.println("[Launcher] Using server host from config: " + host);
                    return host;
                }
            } catch (IOException ignored) {}
        }
        return "localhost";
    }

    /**
     * Connection strategy:
     * <ol>
     *   <li>Resolve server host (config file or localhost).</li>
     *   <li>Try to connect. If successful → send SYNC_REQUEST so this instance's
     *       local DB is populated from the host's data.</li>
     *   <li>If host is remote and unreachable → log error, run standalone.</li>
     *   <li>If host is localhost and no server is running → start an embedded
     *       AuctionServer in a daemon thread, then connect to it.</li>
     * </ol>
     */
    private void connectToServer() {
        String host = resolveServerHost();
        AuctionClient client = AuctionClient.getInstance();

        // Register DB-sync handler for ALL incoming messages (before UI callbacks)
        client.setDbSyncHandler(msg -> AuctionWorkspace.getInstance().handleNetworkSync(msg));

        if (client.connect(host, AuctionServer.PORT)) {
            if (!"localhost".equals(host)) {
                // Connected to a remote server — request full auction list
                client.send(new com.example.bai_tap_lon.network.NetworkMessage(
                        com.example.bai_tap_lon.network.NetworkMessage.Type.SYNC_REQUEST,
                        "", "", 0, ""));
            }
            return;
        }

        if (!"localhost".equals(host)) {
            System.err.println("[Launcher] Could not connect to remote server " + host + ":" + AuctionServer.PORT
                    + ". Running in standalone mode.");
            return;
        }

        // localhost — no server running yet → start an embedded one
        embeddedServer = new AuctionServer();

        // Register the sync handler BEFORE the server starts accepting connections
        AuctionWorkspace.getInstance().registerSyncHandler(embeddedServer);

        Thread serverThread = new Thread(() -> {
            try {
                embeddedServer.start();
            } catch (Exception e) {
                System.err.println("[Launcher] Embedded server error: " + e.getMessage());
            }
        }, "EmbeddedAuctionServer");
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server a moment to bind the port
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        if (!client.connect("localhost", AuctionServer.PORT)) {
            System.err.println("[Launcher] Could not connect to embedded server. Running in standalone mode.");
        }
        // No SYNC_REQUEST needed — we ARE the server host, local DB is already current
    }

    @Override
    public void stop() {
        AuctionClient.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch();
    }
}
