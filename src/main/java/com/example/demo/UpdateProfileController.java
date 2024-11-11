package com.example.demo;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class UpdateProfileController {

    @FXML
    private TextField currentUsernameField;
    @FXML
    private TextField newUsernameField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button updateButton;

    private final String FILE_PATH = "data/users.txt";

    @FXML
    private void initialize() {
        updateButton.setOnAction(event -> updateProfile());
    }

    private void updateProfile() {
        String currentUsername = currentUsernameField.getText();
        String newUsername = newUsernameField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!newPassword.equals(confirmPassword)) {
            showAlert(Alert.AlertType.ERROR, "Password Mismatch", "The passwords do not match. Please try again.");
            return;
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_PATH));
            boolean userExists = false;
            for (String line : lines) {
                if (line.split(":")[0].equals(currentUsername)) {
                    userExists = true;
                    break;
                }
            }

            if (!userExists) {
                showAlert(Alert.AlertType.ERROR, "User Not Found", "The current username is not registered.");
                return;
            }

            for (String line : lines) {
                if (line.split(":")[0].equals(newUsername) && !newUsername.equals(currentUsername)) {
                    showAlert(Alert.AlertType.ERROR, "Username Taken", "The new username is already taken. Please choose another.");
                    return;
                }
            }

            updateFile(lines, currentUsername, newUsername, newPassword);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully.");

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while updating the profile.");
        }
    }

    private void updateFile(List<String> lines, String currentUsername, String newUsername, String newPassword) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String line : lines) {
                String[] parts = line.split(":");
                if (parts[0].equals(currentUsername)) {
                    String updatedUsername = newUsername.isEmpty() ? currentUsername : newUsername;
                    String updatedPassword = newPassword.isEmpty() ? parts[1] : newPassword;
                    writer.write(updatedUsername + ":" + updatedPassword + "\n");
                } else {
                    writer.write(line + "\n");
                }
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
