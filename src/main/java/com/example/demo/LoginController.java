package com.example.demo;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    // Admin credentials
    private static final String ADMIN_USERNAME = "newsrecadmin";
    private static final String ADMIN_PASSWORD = "admin@123";

    // Handle login logic
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Check for empty fields
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Login Failed", "Username and password cannot be empty.");
            return;
        }

        // Check if the login is for admin
        if (username.equals(ADMIN_USERNAME) && password.equals(ADMIN_PASSWORD)) {
            loadAdminDashboard(event);
        } else if (validateCredentials(username, password)) {
            loadUserDashboard(event);
        } else {
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    // Load the Admin Dashboard
    private void loadAdminDashboard(ActionEvent event) {
        try {
            Parent adminRoot = FXMLLoader.load(getClass().getResource("AdminDashboard.fxml"));
            Scene adminScene = new Scene(adminRoot);
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(adminScene);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to load Admin Dashboard.");
        }
    }

    // Load the User Dashboard
    private void loadUserDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashboard.fxml"));
            Parent userRoot = loader.load();

            // Pass username to the next controller
            DashboardController dashboardController = loader.getController();
            dashboardController.setUsername(usernameField.getText().trim());

            // Set scene with specified dimensions
            Scene userScene = new Scene(userRoot, 400, 300); // Adjust width and height here

            // Get the current stage and update its properties
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(userScene);

            // Optionally, set minimum size or other properties for the stage
            currentStage.setMinWidth(400);
            currentStage.setMinHeight(300);

            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Unable to load User Dashboard.");
        }
    }



    // Handle registration logic
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Check for empty fields
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Registration Failed", "Username and password cannot be empty.");
            return;
        }

        // Prevent registration with the admin username
        if (username.equals(ADMIN_USERNAME)) {
            showAlert("Registration Failed", "The username '" + ADMIN_USERNAME + "' is reserved for admin use. Please choose a different username.");
        } else if (isUsernameTaken(username)) {
            showAlert("Registration Failed", "Username already exists. Please choose a different username.");
        } else {
            // Save credentials using FileManager
            FileManager.saveCredentials(username, password);
            showAlert("Registration Successful", "You have successfully registered, " + username + "!");
        }
    }

    // Validate credentials by checking if they exist in users.txt
    private boolean validateCredentials(String username, String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader("data/users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] credentials = line.split(":");
                if (credentials.length >= 2 && credentials[0].equals(username) && credentials[1].equals(password)) {
                    return true; // Found matching username and password
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // No match found
    }

    // Check if the username is already taken
    private boolean isUsernameTaken(String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader("data/users.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] credentials = line.split(":");
                if (credentials.length >= 1 && credentials[0].equals(username)) {
                    return true; // Username already exists
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false; // Username is available
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
