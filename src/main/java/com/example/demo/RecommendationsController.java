package com.example.demo;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecommendationsController {

    private String loggedInUser;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void setUsername(String username) {
        this.loggedInUser = username;
    }

    @FXML
    private VBox recommendationsVBox;

    public void loadRecommendations() {
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            return;
        }

        // Clear the UI and add a loading indicator
        recommendationsVBox.getChildren().clear();
        recommendationsVBox.getChildren().add(new Label("Loading recommendations..."));

        // Run recommendation retrieval in a background thread
        executorService.submit(() -> {
            RecommendationEngine recommendationEngine = new RecommendationEngine();
            recommendationEngine.setUsername(loggedInUser);
            List<String> recommendedArticles = recommendationEngine.getRecommendationsForUser(loggedInUser);

            // Update the UI on the JavaFX Application Thread
            Platform.runLater(() -> {
                recommendationsVBox.getChildren().clear();

                if (recommendedArticles.isEmpty()) {
                    recommendationsVBox.getChildren().add(new Label("No recommendations available."));
                } else {
                    for (String articleTitle : recommendedArticles) {
                        Button articleButton = new Button(articleTitle);
                        articleButton.setOnAction(event -> recommendationEngine.openArticleWindow(articleTitle));
                        recommendationsVBox.getChildren().add(articleButton);
                    }
                }
            });
        });
    }

    @FXML
    private void handleClose() {
        recommendationsVBox.getScene().getWindow().hide();
        shutdownExecutor(); // Ensure executor is shut down when the window is closed
    }

    private void shutdownExecutor() {
        executorService.shutdown();
    }
}


