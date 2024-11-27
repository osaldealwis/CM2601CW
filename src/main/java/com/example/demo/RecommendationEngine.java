package com.example.demo;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationEngine {

    private static final String DATA_FOLDER = "data";
    private static final String POS_MODEL_PATH = "models/opennlp-en-ud-ewt-pos-1.1-2.4.0.bin";
    private static final String DOC_CAT_MODEL_PATH = "models/en-doccat.bin";

    private POSTaggerME posTagger;

    public RecommendationEngine() {
        try {
            // Load the POS model
            InputStream posModelIn = getClass().getClassLoader().getResourceAsStream(POS_MODEL_PATH);
            if (posModelIn == null) {
                throw new IOException("POS model not found in resources.");
            }
            POSModel posModel = new POSModel(posModelIn);
            posTagger = new POSTaggerME(posModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to get recommendations for a user
    public List<String> getRecommendationsForUser(String username) {
        String preferencesFilePath = DATA_FOLDER + "/preferences_" + username + ".csv";
        Map<String, String> userPreferences = loadUserPreferences(preferencesFilePath);

        if (userPreferences.isEmpty()) {
            System.out.println("No preferences found for user: " + username);
            return Collections.emptyList();
        }

        // Get liked articles, giving priority to those that were added last
        List<String> likedArticles = getArticlesByPreference(userPreferences, "like");

        if (likedArticles.isEmpty()) {
            // If no liked articles, consider viewed ones
            likedArticles = getArticlesByPreference(userPreferences, "viewed");
        }

        if (likedArticles.isEmpty()) {
            System.out.println("No liked or viewed articles found for user: " + username);
            return Collections.emptyList();
        }

        // Determine categories of liked/viewed articles
        Set<String> preferredCategories = getArticleCategories(likedArticles);

        // Fetch articles from those categories, prioritizing new ones based on the latest preferences
        List<String> recommendedArticles = fetchArticlesByCategories(preferredCategories, likedArticles);

        return recommendedArticles;
    }

    // Load user preferences (liked, viewed, etc.) from the CSV file
    private Map<String, String> loadUserPreferences(String filePath) {
        Map<String, String> preferences = new LinkedHashMap<>(); // LinkedHashMap preserves order
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("Title")) { // Skip header
                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (parts.length >= 3) {
                        String title = parts[0].replaceAll("^\"|\"$", ""); // Remove quotes
                        String preference = parts[2].trim().toLowerCase();
                        preferences.put(title, preference);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return preferences;
    }

    // Get articles based on the given preference (like or viewed), with priority for newer entries
    private List<String> getArticlesByPreference(Map<String, String> userPreferences, String preferenceType) {
        // Filter articles based on the preference type ("like" or "viewed")
        return userPreferences.entrySet().stream()
                .filter(entry -> preferenceType.equalsIgnoreCase(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Get categories of the articles based on titles using NLP
    private Set<String> getArticleCategories(List<String> articleTitles) {
        Set<String> categories = new HashSet<>();
        File folder = new File(DATA_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    String currentCategory = file.getName().replace("Articles_", "").replace(".txt", "");
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Title:")) {
                            String title = line.substring(7).trim();
                            if (articleTitles.contains(title)) {
                                // Use NLP to categorize the article based on its content
                                String articleContent = getArticleContent(file, title); // Assuming article content is in the file
                                String articleCategory = categorizeArticleWithNLP(articleContent);
                                categories.add(articleCategory);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return categories;
    }

    // Fetch articles based on the categories and exclude already viewed/liked articles
    private List<String> fetchArticlesByCategories(Set<String> categories, List<String> excludeTitles) {
        List<String> recommendations = new ArrayList<>();
        File folder = new File(DATA_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.startsWith("Articles_") && name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                String currentCategory = file.getName().replace("Articles_", "").replace(".txt", "");
                if (categories.contains(currentCategory)) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("Title:")) {
                                String title = line.substring(7).trim();
                                // Avoid recommending the same article again
                                if (!excludeTitles.contains(title) && !recommendations.contains(title)) {
                                    recommendations.add(title);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return recommendations;
    }

    // Method to categorize article content using OpenNLP
    private String categorizeArticleWithNLP(String articleContent) {
        try (InputStream modelIn = getClass().getClassLoader().getResourceAsStream(DOC_CAT_MODEL_PATH)) {
            // Load the pre-trained model
            if (modelIn == null) {
                throw new IOException("Document categorization model not found in resources.");
            }
            DoccatModel model = new DoccatModel(modelIn);
            DocumentCategorizerME categorizer = new DocumentCategorizerME(model);

            // Manually tokenize the article content (split by whitespace)
            String[] tokens = articleContent.split("\\s+");

            // Apply POS tagging (even though the tokenizer is not used)
            String[] posTags = posTagger.tag(tokens);

            // Classify the article content
            double[] outcome = categorizer.categorize(tokens);
            String category = categorizer.getBestCategory(outcome);
            return category;
        } catch (IOException e) {
            e.printStackTrace();
            return "Unknown"; // Return a default category if an error occurs
        }
    }

    // Helper method to get the article content from a file
    private String getArticleContent(File file, String title) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isTitleFound = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Title:") && line.substring(7).trim().equals(title)) {
                    isTitleFound = true;
                }
                if (isTitleFound) {
                    content.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
}

