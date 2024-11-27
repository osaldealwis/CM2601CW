package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboardController {

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField articleTitleField;

    @FXML
    private TextArea articleContentArea;

    @FXML
    private javafx.scene.layout.AnchorPane articleAddSection;

    @FXML
    private javafx.scene.layout.AnchorPane articleRemoveSection;

    @FXML
    private ComboBox<String> articleToRemoveComboBox;

    private static final String[] CATEGORIES = {"Technology", "Health", "Sports", "AI"};

    public void initialize() {
        categoryComboBox.getItems().addAll(CATEGORIES);
        articleAddSection.setVisible(false);
        articleRemoveSection.setVisible(false);
        populateArticleRemoveComboBox();
    }

    public void handleAddArticle(ActionEvent event) {
        articleAddSection.setVisible(true);
        articleRemoveSection.setVisible(false);
    }

    public void handleRemoveArticle(ActionEvent event) {
        articleAddSection.setVisible(false);
        articleRemoveSection.setVisible(true);
    }

    public void handleSubmitArticle(ActionEvent event) {
        String category = categoryComboBox.getValue();
        String title = articleTitleField.getText();
        String content = articleContentArea.getText();

        if (category == null || title.isEmpty() || content.isEmpty()) {
            showAlert("Error", "Please select a category, and fill in both title and content.");
            return;
        }

        saveArticleToFile(title, category, content);
        showAlert("Success", "Article '" + title + "' added successfully under '" + category + "' category!");
    }

    private void saveArticleToFile(String title, String category, String content) {
        String filename = "data/Articles_" + category + ".txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            writer.println("Title: " + title);
            writer.println("Content: " + content);
            writer.println("=====================================");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save the article.");
        }
    }

    private void populateArticleRemoveComboBox() {
        File folder = new File("data");
        File[] listOfFiles = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));
        List<String> articleTitles = new ArrayList<>();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    String title = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:")) {
                            title = line.substring(7).trim();
                            articleTitles.add(title);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        articleToRemoveComboBox.getItems().setAll(articleTitles);
    }

    public void handleRemoveArticleSubmit(ActionEvent event) {
        String articleToRemove = articleToRemoveComboBox.getValue();

        if (articleToRemove == null || articleToRemove.isEmpty()) {
            showAlert("Error", "Please select an article to remove.");
            return;
        }

        removeArticleFromFile(articleToRemove);
        showAlert("Success", "Article '" + articleToRemove + "' removed successfully!");
    }

    private void removeArticleFromFile(String articleTitle) {
        File folder = new File("data");
        File[] listOfFiles = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        for (File file : listOfFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                List<String> lines = new ArrayList<>();
                boolean articleFound = false;

                while ((line = reader.readLine()) != null) {
                    // Check if line starts with the article title to remove
                    if (line.startsWith("Title:") && line.substring(7).trim().equals(articleTitle)) {
                        articleFound = true;
                    }
                    // Check for the separator indicating the end of the article
                    else if (articleFound && line.startsWith("=====================================")) {
                        articleFound = false;
                        continue;  // Skip adding this line to remove the article entirely
                    }

                    // Add lines only if they do not belong to the article being removed
                    if (!articleFound) {
                        lines.add(line);
                    }
                }

                // Rewrite the file with the updated content (excluding the removed article)
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    for (String lineToWrite : lines) {
                        writer.println(lineToWrite);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to remove the article.");
            }
        }

        // Re-populate the ComboBox after removing the article
        populateArticleRemoveComboBox();
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        showAlert("Logout", "Logging out...");
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("login.fxml"));
            Scene loginScene = new Scene(loginRoot);
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(loginScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
