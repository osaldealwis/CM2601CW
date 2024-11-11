package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

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

    public void handleLogout(ActionEvent actionEvent) {

    }
}
