package com.hardware.app.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.hardware.app.config.DatabaseConfig;

public class ProductService {

    // =====================================================
    // GET ALL PRODUCTS
    // =====================================================

    public String getAllProductsJson() {

        StringBuilder json =
                new StringBuilder();

        json.append("{\"ok\":true,\"products\":[");

        boolean first = true;

        String sql =
                "SELECT product_id, " +
                "product_name, " +
                "product_type, " +
                "quantity, " +
                "purchase_price, " +
                "sale_price, " +
                "discount " +
                "FROM products " +
                "ORDER BY product_id DESC";

        try (Connection connection =
                     DatabaseConfig.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                if (!first) {
                    json.append(",");
                }

                first = false;

                json.append("{");

                json.append("\"product_id\":");
                json.append(
                        result.getInt("product_id")
                );

                json.append(",");

                json.append("\"product_name\":\"");
                json.append(
                        escapeJson(
                                result.getString(
                                        "product_name"
                                )
                        )
                );
                json.append("\"");

                json.append(",");

                json.append("\"product_type\":\"");
                json.append(
                        escapeJson(
                                result.getString(
                                        "product_type"
                                )
                        )
                );
                json.append("\"");

                json.append(",");

                json.append("\"quantity\":");
                json.append(
                        result.getInt("quantity")
                );

                json.append(",");

                json.append("\"purchase_price\":");
                json.append(
                        result.getDouble(
                                "purchase_price"
                        )
                );

                json.append(",");

                json.append("\"sale_price\":");
                json.append(
                        result.getDouble(
                                "sale_price"
                        )
                );

                json.append(",");

                json.append("\"discount\":");
                json.append(
                        result.getDouble(
                                "discount"
                        )
                );

                json.append("}");
            }

            json.append("]}");

            return json.toString();

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // ADD PRODUCT
    // =====================================================

    public String addProductFromJson(
            String json
    ) {

        try {

            String productName =
                    getString(
                            json,
                            "productName"
                    );

            String productType =
                    getString(
                            json,
                            "productType"
                    );

            int quantity =
                    getInt(
                            json,
                            "quantity"
                    );

            double purchasePrice =
                    getDouble(
                            json,
                            "purchasePrice"
                    );

            double salePrice =
                    getDouble(
                            json,
                            "salePrice"
                    );

            double discount =
                    getDouble(
                            json,
                            "discount"
                    );

            if (productName == null
                    || productName.isBlank()) {

                return errorJson(
                        "Product name is required"
                );
            }

            if (quantity < 0) {

                return errorJson(
                        "Quantity cannot be negative"
                );
            }

            if (purchasePrice < 0
                    || salePrice < 0
                    || discount < 0) {

                return errorJson(
                        "Price and discount cannot be negative"
                );
            }

            // Product names are unique ignoring case and surrounding spaces.
            String duplicateSql =
                    "SELECT product_name FROM products " +
                    "WHERE LOWER(TRIM(product_name)) = LOWER(TRIM(?)) " +
                    "LIMIT 1";

            try (Connection connection =
                         DatabaseConfig.getConnection();
                 PreparedStatement duplicateStatement =
                         connection.prepareStatement(duplicateSql)) {

                duplicateStatement.setString(1, productName);

                try (ResultSet resultSet =
                             duplicateStatement.executeQuery()) {

                    if (resultSet.next()) {
                        return errorJson(
                                "Product already exists: " +
                                resultSet.getString("product_name")
                        );
                    }
                }
            }

            String sql =
                    "INSERT INTO products " +
                    "(product_name, product_type, " +
                    "quantity, purchase_price, " +
                    "sale_price, discount) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection connection =
                         DatabaseConfig.getConnection();

                 PreparedStatement statement =
                         connection.prepareStatement(
                                 sql
                         )) {

                statement.setString(
                        1,
                        productName
                );

                statement.setString(
                        2,
                        productType
                );

                statement.setInt(
                        3,
                        quantity
                );

                statement.setDouble(
                        4,
                        purchasePrice
                );

                statement.setDouble(
                        5,
                        salePrice
                );

                statement.setDouble(
                        6,
                        discount
                );

                int rows =
                        statement.executeUpdate();

                if (rows > 0) {

                    return
                            "{\"ok\":true," +
                            "\"message\":" +
                            "\"Product added successfully\"}";
                }

                return errorJson(
                        "Product was not added"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    public String deleteProduct(
            int productId
    ) {

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // =================================================
                // REMOVE SALES ITEMS
                // =================================================

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "DELETE FROM sale_items WHERE product_id = ?"
                             )) {

                    statement.setInt(1, productId);
                    statement.executeUpdate();
                }

                // =================================================
                // FIND AFFECTED PURCHASES
                // AND CALCULATE AMOUNT TO REMOVE
                // =================================================

                java.util.List<Integer> affectedPurchaseIds =
                        new java.util.ArrayList<>();

                java.util.Map<Integer, Double> purchaseAmounts =
                        new java.util.HashMap<>();

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "SELECT purchase_id, " +
                                     "COALESCE(SUM(total_price), 0) AS deleted_amount " +
                                     "FROM purchase_items " +
                                     "WHERE product_id = ? " +
                                     "GROUP BY purchase_id"
                             )) {

                    statement.setInt(1, productId);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        while (result.next()) {

                            int purchaseId =
                                    result.getInt("purchase_id");

                            double deletedAmount =
                                    result.getDouble("deleted_amount");

                            affectedPurchaseIds.add(
                                    purchaseId
                            );

                            purchaseAmounts.put(
                                    purchaseId,
                                    deletedAmount
                            );
                        }
                    }
                }

                // =================================================
                // REMOVE PRODUCT FROM PURCHASE ITEMS
                // =================================================

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "DELETE FROM purchase_items " +
                                     "WHERE product_id = ?"
                             )) {

                    statement.setInt(1, productId);
                    statement.executeUpdate();
                }

                // =================================================
                // UPDATE PURCHASE TOTAL
                // OR DELETE EMPTY PURCHASE
                // =================================================

                try (PreparedStatement updatePurchaseStatement =
                             connection.prepareStatement(
                                     "UPDATE purchases " +
                                     "SET total_amount = total_amount - ? " +
                                     "WHERE purchase_id = ?"
                             );

                     PreparedStatement countStatement =
                             connection.prepareStatement(
                                     "SELECT COUNT(*) AS item_count " +
                                     "FROM purchase_items " +
                                     "WHERE purchase_id = ?"
                             );

                     PreparedStatement deletePurchaseStatement =
                             connection.prepareStatement(
                                     "DELETE FROM purchases " +
                                     "WHERE purchase_id = ?"
                             )) {

                    for (Integer purchaseId :
                            affectedPurchaseIds) {

                        // Check whether the purchase still
                        // contains other products.
                        countStatement.setInt(
                                1,
                                purchaseId
                        );

                        try (ResultSet result =
                                     countStatement.executeQuery()) {

                            if (result.next()) {

                                int itemCount =
                                        result.getInt(
                                                "item_count"
                                        );

                                if (itemCount == 0) {

                                    // No products remain in this
                                    // purchase, so remove the
                                    // purchase header too.
                                    deletePurchaseStatement.setInt(
                                            1,
                                            purchaseId
                                    );

                                    deletePurchaseStatement
                                            .executeUpdate();

                                } else {

                                    // Other products remain.
                                    // Reduce the purchase total
                                    // by the deleted product amount.
                                    double deletedAmount =
                                            purchaseAmounts.getOrDefault(
                                                    purchaseId,
                                                    0.0
                                            );

                                    updatePurchaseStatement.setDouble(
                                            1,
                                            deletedAmount
                                    );

                                    updatePurchaseStatement.setInt(
                                            2,
                                            purchaseId
                                    );

                                    updatePurchaseStatement
                                            .executeUpdate();
                                }
                            }
                        }
                    }
                }

                // =================================================
                // DELETE PRODUCT
                // =================================================

                int rows;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "DELETE FROM products " +
                                     "WHERE product_id = ?"
                             )) {

                    statement.setInt(1, productId);

                    rows = statement.executeUpdate();
                }

                if (rows == 0) {

                    connection.rollback();

                    return errorJson(
                            "Product not found"
                    );
                }

                // =================================================
                // COMMIT EVERYTHING
                // =================================================

                connection.commit();

                return
                        "{\"ok\":true," +
                        "\"message\":" +
                        "\"Product deleted successfully\"}";

            } catch (Exception e) {

                connection.rollback();

                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // GET SINGLE PRODUCT
    // =====================================================

    public String getProductById(
            int productId
    ) {

        String sql =
                "SELECT product_id, " +
                "product_name, " +
                "product_type, " +
                "quantity, " +
                "purchase_price, " +
                "sale_price, " +
                "discount " +
                "FROM products " +
                "WHERE product_id = ?";

        try (Connection connection =
                     DatabaseConfig.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    productId
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                if (!result.next()) {

                    return errorJson(
                            "Product not found"
                    );
                }

                return
                        "{"
                        + "\"ok\":true,"
                        + "\"product\":{"

                        + "\"product_id\":"
                        + result.getInt(
                                "product_id"
                        )
                        + ","

                        + "\"product_name\":\""
                        + escapeJson(
                                result.getString(
                                        "product_name"
                                )
                        )
                        + "\","

                        + "\"product_type\":\""
                        + escapeJson(
                                result.getString(
                                        "product_type"
                                )
                        )
                        + "\","

                        + "\"quantity\":"
                        + result.getInt(
                                "quantity"
                        )
                        + ","

                        + "\"purchase_price\":"
                        + result.getDouble(
                                "purchase_price"
                        )
                        + ","

                        + "\"sale_price\":"
                        + result.getDouble(
                                "sale_price"
                        )
                        + ","

                        + "\"discount\":"
                        + result.getDouble(
                                "discount"
                        )

                        + "}"
                        + "}";
            }

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(
                    e.getMessage()
            );
        }
    }

    // =====================================================
    // JSON HELPER - STRING
    // =====================================================

    private String getString(
            String json,
            String key
    ) {

        String pattern =
                "\"" + key + "\"";

        int keyPosition =
                json.indexOf(pattern);

        if (keyPosition == -1) {
            return "";
        }

        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );

        if (colonPosition == -1) {
            return "";
        }

        int firstQuote =
                json.indexOf(
                        "\"",
                        colonPosition + 1
                );

        if (firstQuote == -1) {
            return "";
        }

        int secondQuote =
                json.indexOf(
                        "\"",
                        firstQuote + 1
                );

        if (secondQuote == -1) {
            return "";
        }

        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }

    // =====================================================
    // JSON HELPER - INT
    // =====================================================

    private int getInt(
            String json,
            String key
    ) {

        String value =
                getNumberValue(
                        json,
                        key
                );

        if (value.isBlank()) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    // =====================================================
    // JSON HELPER - DOUBLE
    // =====================================================

    private double getDouble(
            String json,
            String key
    ) {

        String value =
                getNumberValue(
                        json,
                        key
                );

        if (value.isBlank()) {
            return 0;
        }

        return Double.parseDouble(value);
    }

    // =====================================================
    // JSON HELPER - NUMBER
    // =====================================================

    private String getNumberValue(
            String json,
            String key
    ) {

        String pattern =
                "\"" + key + "\"";

        int keyPosition =
                json.indexOf(pattern);

        if (keyPosition == -1) {
            return "";
        }

        int colonPosition =
                json.indexOf(
                        ":",
                        keyPosition
                );

        if (colonPosition == -1) {
            return "";
        }

        int start =
                colonPosition + 1;

        while (
                start < json.length()
                && Character.isWhitespace(
                        json.charAt(start)
                )
        ) {
            start++;
        }

        int end = start;

        while (
                end < json.length()
                && (
                    Character.isDigit(
                            json.charAt(end)
                    )
                    || json.charAt(end) == '.'
                    || json.charAt(end) == '-'
                )
        ) {
            end++;
        }

        return json.substring(
                start,
                end
        );
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

    // =====================================================
    // ERROR JSON
    // =====================================================

    private String errorJson(
            String message
    ) {

        if (message == null) {
            message = "Unknown error";
        }

        return
                "{\"ok\":false,\"error\":\""
                + escapeJson(message)
                + "\"}";
    }
}