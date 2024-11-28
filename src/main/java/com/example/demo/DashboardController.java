package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.File;
import java.io.IOException;

public class DashboardController {

    private String loggedInUser;

    public void setUsername(String username) {
        this.loggedInUser = username;
    }


    // Handle viewing articles
    @FXML
    public void handleViewArticles(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ViewArticles.fxml"));
            Parent root = loader.load();

            // Set the username in ViewArticlesController
            ViewArticlesController controller = loader.getController();
            controller.setUsername(loggedInUser);

            Stage viewArticlesStage = new Stage();
            viewArticlesStage.setTitle("View Articles");
            viewArticlesStage.setScene(new Scene(root));
            viewArticlesStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Handle getting recommendations
    @FXML
    public void handleGetRecommendations(ActionEvent event) {
        // Define the path for the user's preference file
        String preferenceFilePath = "data/preferences_" + loggedInUser + ".csv";
        File preferenceFile = new File(preferenceFilePath);

        // Check if the preference file does not exist
        if (!preferenceFile.exists()) {
            // Show an alert prompting the user to view or rate articles
            showAlert("No Recommendations",
                    "Please view or rate articles to get personalized recommendations.");
            return; // Exit the method if no preference file exists
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Recommendations.fxml"));
            Parent root = loader.load();

            // Set the username in RecommendationsController
            RecommendationsController recommendationsController = loader.getController();
            recommendationsController.setUsername(loggedInUser);
            recommendationsController.loadRecommendations(); // Load recommendations for the logged-in user

            // Create the stage and set the scene with specified dimensions
            Stage recommendationsStage = new Stage();
            recommendationsStage.setTitle("Recommended Articles");
            recommendationsStage.setScene(new Scene(root, 600, 300)); // Adjust width and height here

            // Optionally, set minimum size for the window
            recommendationsStage.setMinWidth(600);
            recommendationsStage.setMinHeight(300);

            recommendationsStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to load recommendations view.");
        }
    }


    // Handle updating profile
    @FXML
    public void handleUpdateProfile(ActionEvent event) {
        try {
            // Load the UpdateProfile.fxml file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UpdateProfile.fxml"));
            Parent root = loader.load();

            // Set up the UpdateProfileController if needed to pass data
            UpdateProfileController updateProfileController = loader.getController();
            // Optionally pass data if needed
            // updateProfileController.setUserData(currentUser);

            // Create a new stage for the Update Profile window
            Stage updateProfileStage = new Stage();
            updateProfileStage.setTitle("Update Profile");
            updateProfileStage.initModality(Modality.APPLICATION_MODAL); // Block input to other windows
            updateProfileStage.setScene(new Scene(root));

            // Show the Update Profile window and wait until it's closed
            updateProfileStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // Utility method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
