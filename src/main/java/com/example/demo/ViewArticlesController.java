package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import javafx.scene.input.KeyEvent;
import java.io.*;

public class ViewArticlesController {



    private String loggedInUser;

    public void setUsername(String username) {
        this.loggedInUser = username;
    }

    @FXML
    private TextField searchBar;

    @FXML
    private VBox articlesVBox;

    private List<String> articleTitles = new ArrayList<>();

    // Load all article titles when the window is opened
    public void initialize() {
        loadArticleTitles();
    }

    private void loadArticleTitles() {
        File folder = new File("data");
        File[] listOfFiles = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    String title = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:")) {
                            title = line.substring(7).trim();
                            articleTitles.add(title);
                            addArticleButton(title); // Create button for each article
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Add a button for each article to view it
    private void addArticleButton(String title) {
        Button articleButton = new Button(title);
        articleButton.setOnAction(event -> openArticleWindow(title));
        articlesVBox.getChildren().add(articleButton);
    }

    // Search function to filter articles based on keyword
    @FXML
    private void handleSearch(KeyEvent event) {
        String keyword = searchBar.getText().toLowerCase(); // Get the search text
        articlesVBox.getChildren().clear();  // Clear previous results

        List<String> keywordTokens = Arrays.asList(keyword.split("\\s+"));

        for (String title : articleTitles) {
            List<String> titleTokens = Arrays.asList(title.toLowerCase().split("\\s+"));

            if (titleTokens.stream().anyMatch(keywordTokens::contains)) {
                addArticleButton(title); // Add button if a match is found
            }
        }
    }

    // Open a new window to display the full article content
    private void openArticleWindow(String title) {
        Stage newStage = new Stage();
        VBox vbox = new VBox();
        TextArea articleContentArea = new TextArea();
        articleContentArea.setEditable(false);
        articleContentArea.setWrapText(true);

        // Find article content by title
        String articleContent = loadArticleContent(title);
        articleContentArea.setText(articleContent);

        // Log the article view
        saveArticleView(title);

        // Like Button
        Button likeButton = new Button("Like Article 👍");
        likeButton.setOnAction(event -> savePreference(title, "like"));

        // Dislike Button
        Button dislikeButton = new Button("Dislike Article 👎");
        dislikeButton.setOnAction(event -> savePreference(title, "dislike"));

        vbox.getChildren().addAll(articleContentArea, likeButton, dislikeButton);
        Scene scene = new Scene(vbox, 500, 400);
        newStage.setScene(scene);
        newStage.setTitle(title);
        newStage.show();
    }

    private void saveArticleView(String articleTitle) {
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            showAlert("Error", "User is not logged in. Unable to save article view.");
            return;
        }

        String fileName = "data/preferences_" + loggedInUser + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            // Write header if file is new
            File file = new File(fileName);
            if (file.length() == 0) {
                writer.write("Title,Action,Preference\n");
            }

            // Write the article view entry with "NA" for the preference, followed by a new line
            writer.write(String.format("\"%s\",viewed,NA\n", articleTitle));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePreference(String articleTitle, String preference) {
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            showAlert("Error", "User is not logged in. Unable to save preference.");
            return;
        }

        String fileName = "data/preferences_" + loggedInUser + ".csv";
        try {
            File tempFile = new File(fileName + ".tmp");
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, false)); // 'false' to overwrite the temp file
            String line;
            boolean updated = false;

            // Read all lines and update the preference
            while ((line = reader.readLine()) != null) {
                if (line.contains(articleTitle) && line.contains("viewed")) {
                    // If the article is already viewed, update its preference
                    line = String.format("\"%s\",viewed,%s", articleTitle, preference);
                    updated = true;
                }

                // Avoid writing empty lines
                if (!line.trim().isEmpty()) {
                    writer.write(line + "\n");
                }
            }

            // If the article was not found, add a new entry
            if (!updated) {
                writer.write(String.format("\"%s\",viewed,%s\n", articleTitle, preference));
            }

            reader.close();
            writer.close();

            // Delete original file and rename the temp file to the original
            if (new File(fileName).delete()) {
                tempFile.renameTo(new File(fileName));
            }

            showAlert("Success", "Your preference for the article has been recorded.");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to save your preference.");
        }
    }




    // Load article content from file based on title
    private String loadArticleContent(String title) {
        File folder = new File("data");
        File[] listOfFiles = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));
        StringBuilder content = new StringBuilder();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    boolean articleFound = false;

                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:") && line.substring(7).trim().equals(title)) {
                            articleFound = true;
                        }
                        if (articleFound) {
                            content.append(line).append("\n");
                        }
                        if (line.startsWith("=====================================") && articleFound) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return content.toString();
    }

    // Show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}