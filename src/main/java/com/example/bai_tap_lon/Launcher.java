package com.example.bai_tap_lon;

import com.example.bai_tap_lon.Controllers.AuctionWorkspace;
import com.example.bai_tap_lon.network.AuctionClient;
import com.example.bai_tap_lon.network.AuctionServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {

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
     * Tries to connect to a running AuctionServer.
     * If none is running on localhost, starts an embedded server in a daemon thread
     * then connects to it — so the first launched instance becomes the server host
     * and subsequent instances join as additional clients.
     */
    private void connectToServer() {
        AuctionClient client = AuctionClient.getInstance();

        // First, try connecting to an already-running server
        if (client.connect("localhost", AuctionServer.PORT)) {
            return;
        }

        // No server running — start an embedded one
        Thread serverThread = new Thread(() -> {
            try {
                new AuctionServer().start();
            } catch (Exception e) {
                System.err.println("[Launcher] Embedded server error: " + e.getMessage());
            }
        }, "EmbeddedAuctionServer");
        serverThread.setDaemon(true);
        serverThread.start();

        // Give the server a moment to bind
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        if (!client.connect("localhost", AuctionServer.PORT)) {
            System.err.println("[Launcher] Could not connect to embedded server. Running in standalone mode.");
        }
    }

    @Override
    public void stop() {
        AuctionClient.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch();
    }
}
