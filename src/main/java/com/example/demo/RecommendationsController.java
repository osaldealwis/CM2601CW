package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

public class RecommendationsController {

    private String loggedInUser;

    public void setUsername(String username) {
        this.loggedInUser = username;
    }

    @FXML
    private VBox recommendationsVBox;

    public void loadRecommendations() {
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            return;
        }

        RecommendationEngine recommendationEngine = new RecommendationEngine();
        recommendationEngine.setUsername(loggedInUser); // Set the username for the engine
        List<String> recommendedArticles = recommendationEngine.getRecommendationsForUser(loggedInUser);

        recommendationsVBox.getChildren().clear();

        if (recommendedArticles.isEmpty()) {
            recommendationsVBox.getChildren().add(new Button("No recommendations available."));
        } else {
            for (String articleTitle : recommendedArticles) {
                Button articleButton = new Button(articleTitle);
                articleButton.setOnAction(event -> recommendationEngine.openArticleWindow(articleTitle)); // Link to openArticleWindow
                recommendationsVBox.getChildren().add(articleButton);
            }
        }
    }

    @FXML
    private void handleClose() {
        recommendationsVBox.getScene().getWindow().hide();
    }
}

