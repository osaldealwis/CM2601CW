package com.example.demo;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;

public class FileManager {

    public static void saveCredentials(String username, String password) {
        try {
            // Create the "data" folder if it doesn't exist
            File directory = new File("data");
            if (!directory.exists()) {
                directory.mkdir();  // Create the folder
            }

            // Save the credentials in the "data/users.txt" file
            FileWriter writer = new FileWriter("data/users.txt", true);
            writer.write(username + ":" + password + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
