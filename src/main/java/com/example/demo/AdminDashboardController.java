package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminDashboardController {

    @FXML
    private TextField articleTitleField;

    // Add an article
    public void handleAddArticle(ActionEvent event) {
        String articleTitle = articleTitleField.getText();
        // Logic to save the article (e.g., to a database or file) would go here
        showAlert("Success", "Article '" + articleTitle + "' added successfully!");
    }

    // Remove an article
    public void handleRemoveArticle(ActionEvent event) {
        String articleTitle = articleTitleField.getText();
        // Logic to remove the article would go here
        showAlert("Success", "Article '" + articleTitle + "' removed successfully!");
    }

    // Utility method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    // Handle logout
    @FXML
    public void handleLogout(ActionEvent event) {
        showAlert("Logout", "Logging out...");

        // Redirect back to the login screen
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("login.fxml"));
            Scene loginScene = new Scene(loginRoot);

            // Get current stage and set login scene
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(loginScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
