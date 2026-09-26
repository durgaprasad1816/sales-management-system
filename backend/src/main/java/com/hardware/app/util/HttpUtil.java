package com.hardware.app.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpUtil {

    // =====================================================
    // READ FRONTEND FILE
    // =====================================================

    public static String readFrontendFile(
            String fileName
    ) throws IOException {

        Path path =
                Path.of(
                        "../frontend",
                        fileName
                ).toAbsolutePath()
                        .normalize();

        if (!Files.exists(path)) {

            throw new IOException(
                    "Frontend file not found: "
                            + path
            );
        }

        return Files.readString(
                path,
                StandardCharsets.UTF_8
        );
    }

    // =====================================================
    // ESCAPE JSON
    // =====================================================

    public static String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =====================================================
    // JSON ERROR
    // =====================================================

    public static String errorJson(
            String message
    ) {

        return
                "{\"ok\":false,\"error\":\""
                + escape(message)
                + "\"}";
    }
}