package com.example.demo;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationEngine {

    private static final String DATA_FOLDER = "data";
    private Map<String, String> allArticles = new HashMap<>(); // Stores article title -> content
    private Map<String, Integer> globalVocabulary = new HashMap<>(); // Global vocabulary for consistent vectorization

    // Get recommendations for a user based on preferences
    public List<String> getRecommendationsForUser(String username) {
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

        // Calculate similarities
        return calculateSimilarities(candidateArticles, categoryPreferences.get(preferredCategory));
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

                if (preference.equalsIgnoreCase("like") || action.equalsIgnoreCase("viewed")) {
                    String category = detectArticleCategory(title);
                    categoryPreferences.computeIfAbsent(category, k -> new ArrayList<>()).add(title);
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
