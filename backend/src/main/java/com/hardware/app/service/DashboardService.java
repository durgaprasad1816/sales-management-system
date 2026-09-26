package com.hardware.app.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hardware.app.config.DatabaseConfig;

public class DashboardService {

    // =====================================================
    // GET DASHBOARD DATA
    // =====================================================

    public String getDashboardJson() {

        double totalSales = 0;
        double totalPurchase = 0;
        int totalProducts = 0;

        StringBuilder productsJson =
                new StringBuilder();

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            // =================================================
            // TOTAL SALES
            // =================================================

            String salesSql =
                    "SELECT COALESCE(SUM(final_total), 0) " +
                    "FROM sales";

            try (PreparedStatement statement =
                         connection.prepareStatement(salesSql);

                 ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {
                    totalSales =
                            result.getDouble(1);
                }
            }

            // =================================================
            // TOTAL PURCHASE
            // =================================================

            String purchaseSql =
                    "SELECT COALESCE(SUM(total_amount), 0) " +
                    "FROM purchases";

            try (PreparedStatement statement =
                         connection.prepareStatement(purchaseSql);

                 ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {
                    totalPurchase =
                            result.getDouble(1);
                }
            }

            // =================================================
            // TOTAL PRODUCTS
            // =================================================

            String productCountSql =
                    "SELECT COUNT(*) FROM products";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 productCountSql
                         );

                 ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {
                    totalProducts =
                            result.getInt(1);
                }
            }

            // =================================================
            // PRODUCT DETAILS
            // =================================================

            String productSql =
                    "SELECT product_id, " +
                    "product_name, " +
                    "product_type, " +
                    "quantity, " +
                    "purchase_price, " +
                    "sale_price, " +
                    "discount " +
                    "FROM products " +
                    "ORDER BY product_id DESC";

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 productSql
                         );

                 ResultSet result =
                         statement.executeQuery()) {

                boolean first = true;

                while (result.next()) {

                    if (!first) {
                        productsJson.append(",");
                    }

                    first = false;

                    productsJson.append("{");

                    productsJson.append(
                            "\"product_id\":"
                    );

                    productsJson.append(
                            result.getInt(
                                    "product_id"
                            )
                    );

                    productsJson.append(",");

                    productsJson.append(
                            "\"product_name\":\""
                    );

                    productsJson.append(
                            escapeJson(
                                    result.getString(
                                            "product_name"
                                    )
                            )
                    );

                    productsJson.append("\",");

                    productsJson.append(
                            "\"product_type\":\""
                    );

                    productsJson.append(
                            escapeJson(
                                    result.getString(
                                            "product_type"
                                    )
                            )
                    );

                    productsJson.append("\",");

                    productsJson.append(
                            "\"quantity\":"
                    );

                    productsJson.append(
                            result.getInt(
                                    "quantity"
                            )
                    );

                    productsJson.append(",");

                    productsJson.append(
                            "\"purchase_price\":"
                    );

                    productsJson.append(
                            result.getDouble(
                                    "purchase_price"
                            )
                    );

                    productsJson.append(",");

                    productsJson.append(
                            "\"sale_price\":"
                    );

                    productsJson.append(
                            result.getDouble(
                                    "sale_price"
                            )
                    );

                    productsJson.append(",");

                    productsJson.append(
                            "\"discount\":"
                    );

                    productsJson.append(
                            result.getDouble(
                                    "discount"
                            )
                    );

                    productsJson.append("}");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            return
                    "{"
                    + "\"ok\":false,"
                    + "\"error\":\""
                    + escapeJson(
                            e.getMessage()
                    )
                    + "\""
                    + "}";
        }

        // =====================================================
        // PROFIT
        // =====================================================

        double profit =
                totalSales - totalPurchase;

        // =====================================================
        // FINAL JSON
        // =====================================================

        return
                "{"

                + "\"ok\":true,"

                + "\"totalSales\":"
                + totalSales
                + ","

                + "\"totalPurchase\":"
                + totalPurchase
                + ","

                + "\"profit\":"
                + profit
                + ","

                + "\"totalProducts\":"
                + totalProducts
                + ","

                + "\"products\":["
                + productsJson
                + "]"

                + "}";
    }

    // =====================================================
    // ESCAPE JSON
    // =====================================================

    private String escapeJson(
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
}