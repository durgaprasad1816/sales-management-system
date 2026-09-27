package com.hardware.app;

import com.hardware.app.config.SchemaInitializer;
import com.hardware.app.server.HardwareHttpServer;

public class Main {

    public static void main(String[] args) {

        try {

            int port = Integer.parseInt(
                    System.getenv().getOrDefault("PORT", "8080")
            );

            SchemaInitializer.ensure();

            HardwareHttpServer server =
                    new HardwareHttpServer(port);

            server.start();

        } catch (Exception e) {

            System.err.println(
                    "Failed to start Hardware Management Server."
            );

            e.printStackTrace();
        }
    }
}