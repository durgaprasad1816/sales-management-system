package com.hardware.app;

import com.hardware.app.server.HardwareHttpServer;
import com.hardware.app.config.SchemaInitializer;

public class Main {

    public static void main(String[] args) {

        try {

            int port = 8080;

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