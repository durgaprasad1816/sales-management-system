package com.hardware.app.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    // =========================================================
    // AIVEN MYSQL DATABASE CONFIGURATION
    // =========================================================

    private static final String HOST =
            "mysql-6d0f1ed-project-9a40.b.aivencloud.com";

    private static final String PORT =
            "15342";

    private static final String DATABASE =
            "hardware_store";

    private static final String USER =
            "avnadmin";

    /*
     * IMPORTANT:
     * Do NOT write the Aiven password directly here.
     *
     * The password will come from the DB_PASSWORD
     * environment variable.
     */
    private static final String PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    // =========================================================
    // MYSQL CONNECTION URL
    // =========================================================

    private static final String URL =
            "jdbc:mysql://"
                    + HOST
                    + ":"
                    + PORT
                    + "/"
                    + DATABASE
                    + "?useSSL=true"
                    + "&serverTimezone=Asia/Kolkata"
                    + "&allowPublicKeyRetrieval=true";

    // =========================================================
    // GET DATABASE CONNECTION
    // =========================================================

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    // =========================================================
    // TEST DATABASE CONNECTION
    // =========================================================

    public static void main(String[] args) {

        System.out.println(
                "Attempting to connect to Aiven MySQL..."
        );

        System.out.println(
                "Host: " + HOST
        );

        System.out.println(
                "Port: " + PORT
        );

        System.out.println(
                "Database: " + DATABASE
        );

        System.out.println(
                "User: " + USER
        );

        try (Connection connection = getConnection()) {

            if (connection != null && !connection.isClosed()) {

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "Aiven MySQL Database Connected Successfully!"
                );

                System.out.println(
                        "======================================"
                );
            }

        } catch (SQLException e) {

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "Aiven MySQL Database Connection Failed!"
            );

            System.out.println(
                    "======================================"
            );

            e.printStackTrace();
        }
    }
}