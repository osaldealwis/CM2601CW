package com.example.demo;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class RecommendationEngine {

    private static final String DATA_FOLDER = "data";
    private Map<String, String> allArticles = new HashMap<>(); // Stores article title -> content
    private Map<String, Integer> globalVocabulary = new HashMap<>(); // Global vocabulary for consistent vectorization
    private String loggedInUser; // To track the user

    public void setUsername(String username) {
        this.loggedInUser = username;
    }

    // Get recommendations for a user based on preferences
    public List<String> getRecommendationsForUser(String username) {
        setUsername(username); // Set the logged-in user
        loadAllArticles(); // Load all articles from files
        buildGlobalVocabulary(allArticles.values()); // Build global vocabulary

        String preferencesFilePath = DATA_FOLDER + "/preferences_" + username + ".csv";

        // Load user preferences
        Map<String, List<String>> categoryPreferences = analyzeUserPreferences(preferencesFilePath);

        // Find the category with the highest "liked" and "viewed" counts
        String preferredCategory = categoryPreferences.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (preferredCategory == null) {
            System.out.println("No preferences found for user: " + username);
            return Collections.emptyList();
        }

        // Fetch articles from the preferred category
        List<String> candidateArticles = fetchArticlesFromCategory(preferredCategory, categoryPreferences.get(preferredCategory));

        // Skip disliked articles
        List<String> dislikedArticles = getDislikedArticles(username);
        candidateArticles = candidateArticles.stream()
                .filter(article -> !dislikedArticles.contains(article))
                .collect(Collectors.toList());

        // Calculate similarities
        return calculateSimilarities(candidateArticles, categoryPreferences.get(preferredCategory));
    }

    // Open an article in a separate window
    public void openArticleWindow(String title) {
        Stage newStage = new Stage();
        VBox vbox = new VBox();
        vbox.setSpacing(10); // Add some spacing between elements

        // Create a heading label for the article title
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // Style for heading

        // Fetch the article content and clean it by removing "Content:" if present
        String rawContent = allArticles.getOrDefault(title, "Article content not found.");
        String cleanedContent = rawContent.replaceFirst("(?i)Content:\\s*", ""); // Remove "Content:" prefix (case-insensitive)

        // Create a TextArea for the article content
        TextArea articleContentArea = new TextArea();
        articleContentArea.setEditable(false);
        articleContentArea.setWrapText(true);
        articleContentArea.setText(cleanedContent); // Set the cleaned content

        // Log the article view
        saveArticleView(title);

        // Like Button
        Button likeButton = new Button("Like Article 👍");
        likeButton.setOnAction(event -> savePreference(title, "like"));

        // Dislike Button
        Button dislikeButton = new Button("Dislike Article 👎");
        dislikeButton.setOnAction(event -> savePreference(title, "dislike"));

        // Add all components to the VBox
        vbox.getChildren().addAll(titleLabel, articleContentArea, likeButton, dislikeButton);

        // Create and set the scene
        Scene scene = new Scene(vbox, 500, 400);
        newStage.setScene(scene);
        newStage.setTitle(title); // Set the window title
        newStage.show();
    }


    private void saveArticleView(String articleTitle) {
        if (loggedInUser == null || loggedInUser.isEmpty()) {
            showAlert("Error", "User is not logged in. Unable to save article view.");
            return;
        }

        String fileName = DATA_FOLDER + "/preferences_" + loggedInUser + ".csv";
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

        String fileName = DATA_FOLDER + "/preferences_" + loggedInUser + ".csv";
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Analyze user preferences from the CSV file
    private Map<String, List<String>> analyzeUserPreferences(String filePath) {
        Map<String, List<String>> categoryPreferences = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();

            for (String[] record : records) {
                if (record[0].equalsIgnoreCase("Title")) continue; // Skip header
                String title = record[0];
                String action = record[1];
                String preference = record[2];

                // Only consider liked and viewed actions, and filter out dislikes
                if ("like".equalsIgnoreCase(preference)) {
                    String category = detectArticleCategory(title);
                    categoryPreferences.computeIfAbsent(category, k -> new ArrayList<>()).add(title); // Add liked articles
                }
            }

            // Second pass to add viewed articles, but only if there are no liked articles in that category
            for (String[] record : records) {
                if (record[0].equalsIgnoreCase("Title")) continue; // Skip header
                String title = record[0];
                String action = record[1];
                String preference = record[2];

                // Only add viewed articles if no liked articles have been added for that category
                if ("viewed".equalsIgnoreCase(action) && !"like".equalsIgnoreCase(preference)) {
                    String category = detectArticleCategory(title);
                    // Only add if this category hasn't already received liked articles
                    if (!categoryPreferences.containsKey(category) || categoryPreferences.get(category).isEmpty()) {
                        categoryPreferences.computeIfAbsent(category, k -> new ArrayList<>()).add(title);
                    }
                }
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
        return categoryPreferences;
    }


    // Detect category based on article title
    private String detectArticleCategory(String title) {
        // Match article titles to category files
        File folder = new File(DATA_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:") && line.substring(7).trim().equalsIgnoreCase(title)) {
                            return file.getName().replace("Articles_", "").replace(".txt", "");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "Unknown";
    }

    // Fetch articles from a specific category excluding already interacted titles
    private List<String> fetchArticlesFromCategory(String category, List<String> excludeTitles) {
        List<String> recommendations = new ArrayList<>();
        String categoryFilePath = DATA_FOLDER + "/Articles_" + category + ".txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(categoryFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Title:")) {
                    String title = line.substring(7).trim();
                    if (!excludeTitles.contains(title)) {
                        recommendations.add(title);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return recommendations;
    }

    // Get disliked articles for the logged-in user
    private List<String> getDislikedArticles(String username) {
        List<String> dislikedArticles = new ArrayList<>();
        String preferencesFilePath = DATA_FOLDER + "/preferences_" + username + ".csv";

        try (CSVReader reader = new CSVReader(new FileReader(preferencesFilePath))) {
            List<String[]> records = reader.readAll();

            for (String[] record : records) {
                if (record[0].equalsIgnoreCase("Title")) continue; // Skip header
                if ("dislike".equalsIgnoreCase(record[2])) {
                    dislikedArticles.add(record[0]);
                }
            }
        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        return dislikedArticles;
    }

    // Build a global vocabulary from all articles
    private void buildGlobalVocabulary(Collection<String> articles) {
        int index = 0;
        for (String article : articles) {
            String[] words = article.split("\\s+");
            for (String word : words) {
                if (!globalVocabulary.containsKey(word)) {
                    globalVocabulary.put(word, index++);
                }
            }
        }
    }

    // Convert article content into a vector using the global vocabulary
    private RealVector toVector(String content) {
        String[] words = content.split("\\s+");
        double[] vector = new double[globalVocabulary.size()];
        for (String word : words) {
            if (globalVocabulary.containsKey(word)) {
                int index = globalVocabulary.get(word);
                vector[index]++;
            }
        }
        return new ArrayRealVector(vector);
    }

    // Calculate cosine similarity between articles
    private double calculateCosineSimilarity(RealVector vector1, RealVector vector2) {
        return vector1.dotProduct(vector2) / (vector1.getNorm() * vector2.getNorm());
    }

    // Calculate similarities and rank articles
    private List<String> calculateSimilarities(List<String> candidateArticles, List<String> userLikedArticles) {
        List<String> recommendations = new ArrayList<>();
        RealVector userProfileVector = new ArrayRealVector(globalVocabulary.size());

        // Aggregate vectors for user liked articles
        for (String likedArticle : userLikedArticles) {
            String content = allArticles.getOrDefault(likedArticle, "");
            userProfileVector = userProfileVector.add(toVector(content));
        }

        // Rank candidates based on similarity
        Map<String, Double> scores = new HashMap<>();
        for (String candidate : candidateArticles) {
            String content = allArticles.getOrDefault(candidate, "");
            RealVector candidateVector = toVector(content);
            double similarity = calculateCosineSimilarity(userProfileVector, candidateVector);
            scores.put(candidate, similarity);
        }

        // Sort by similarity, limit to top 5
        return scores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by similarity
                .map(Map.Entry::getKey)
                .limit(5) // Limit to top 5
                .collect(Collectors.toList());
    }

    // Load all articles into memory
    private void loadAllArticles() {
        File folder = new File(DATA_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line, title = null, content = "";
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:")) {
                            if (title != null) {
                                allArticles.put(title, content.trim());
                            }
                            title = line.substring(7).trim();
                            content = "";
                        } else {
                            content += line + " ";
                        }
                    }
                    if (title != null) {
                        allArticles.put(title, content.trim());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

