package com.hardware.app.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import com.hardware.app.config.DatabaseConfig;

public class SalesService {

    // =====================================================
    // CREATE SALE
    // =====================================================

    public String createSaleFromJson(String json) {

        String customerName =
                getString(json, "customerName");

        String customerType =
                getString(json, "customerType");

        String customerPhone =
                getString(json, "customerPhone");
        String customerEmail =
                getString(json, "customerEmail");
        String customerGst =
                getString(json, "customerGst");

        double subtotal =
                getDouble(json, "subtotal");

        double discount =
                getDouble(json, "discount");

        double gst =
                getDouble(json, "gst");

        double finalTotal =
                getDouble(json, "finalTotal");

        if (customerName == null ||
                customerName.isBlank()) {

            return errorJson(
                    "Customer name is required"
            );
        }

        if (customerType == null ||
                customerType.isBlank()) {

            customerType = "DIRECT";
        }

        if (finalTotal < 0) {

            return errorJson(
                    "Invalid final total"
            );
        }

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // =========================================
                // CUSTOMER
                // =========================================

                int customerId =
                        findOrCreateCustomer(
                                connection,
                                customerName,
                                customerType,
                                customerPhone,
                                customerEmail,
                                customerGst
                        );

                // =========================================
                // CREATE SALE
                // =========================================

                String saleSql =
                        "INSERT INTO sales " +
                        "(customer_id, subtotal, discount, gst, final_total) " +
                        "VALUES (?, ?, ?, ?, ?)";

                int saleId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     saleSql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setInt(
                            1,
                            customerId
                    );

                    statement.setDouble(
                            2,
                            subtotal
                    );

                    statement.setDouble(
                            3,
                            discount
                    );

                    statement.setDouble(
                            4,
                            gst
                    );

                    statement.setDouble(
                            5,
                            finalTotal
                    );

                    statement.executeUpdate();

                    try (ResultSet keys =
                                 statement.getGeneratedKeys()) {

                        if (!keys.next()) {

                            throw new Exception(
                                    "Could not create sale"
                            );
                        }

                        saleId =
                                keys.getInt(1);
                    }
                }

                // =========================================
                // GET ITEMS FROM JSON
                // =========================================

                String itemsJson =
                        getArray(
                                json,
                                "items"
                        );

                if (itemsJson == null ||
                        itemsJson.isBlank()) {

                    throw new Exception(
                            "No sale items provided"
                    );
                }

                // =========================================
                // PARSE ITEMS
                // =========================================

                String[] items =
                        splitObjects(
                                itemsJson
                        );

                if (items.length == 0) {

                    throw new Exception(
                            "No valid sale items found"
                    );
                }

                // =========================================
                // INSERT SALE ITEMS
                // =========================================

                String itemSql =
                        "INSERT INTO sale_items " +
                        "(sale_id, product_id, quantity, " +
                        "unit_price, discount, total_price) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

                String stockSql =
                        "UPDATE products " +
                        "SET quantity = quantity - ? " +
                        "WHERE product_id = ? " +
                        "AND quantity >= ?";

                for (String itemJson : items) {

                    int productId =
                            getInt(
                                    itemJson,
                                    "productId"
                            );

                    int quantity =
                            getInt(
                                    itemJson,
                                    "quantity"
                            );

                    double unitPrice =
                            getDouble(
                                    itemJson,
                                    "unitPrice"
                            );

                    double itemDiscount =
                            getDouble(
                                    itemJson,
                                    "discount"
                            );

                    double itemTotal =
                            getDouble(
                                    itemJson,
                                    "total"
                            );

                    if (productId <= 0) {

                        throw new Exception(
                                "Invalid product ID"
                        );
                    }

                    if (quantity <= 0) {

                        throw new Exception(
                                "Quantity must be greater than 0"
                        );
                    }

                    if (unitPrice < 0) {

                        throw new Exception(
                                "Unit price cannot be negative"
                        );
                    }

                    if (itemDiscount < 0) {

                        throw new Exception(
                                "Discount cannot be negative"
                        );
                    }

                    // =====================================
                    // REDUCE STOCK
                    // =====================================

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         stockSql
                                 )) {

                        statement.setInt(
                                1,
                                quantity
                        );

                        statement.setInt(
                                2,
                                productId
                        );

                        statement.setInt(
                                3,
                                quantity
                        );

                        int updated =
                                statement.executeUpdate();

                        if (updated == 0) {

                            throw new Exception(
                                    "Insufficient stock for product ID: "
                                            + productId
                            );
                        }
                    }

                    // =====================================
                    // SAVE SALE ITEM
                    // =====================================

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         itemSql
                                 )) {

                        statement.setInt(
                                1,
                                saleId
                        );

                        statement.setInt(
                                2,
                                productId
                        );

                        statement.setInt(
                                3,
                                quantity
                        );

                        statement.setDouble(
                                4,
                                unitPrice
                        );

                        statement.setDouble(
                                5,
                                itemDiscount
                        );

                        statement.setDouble(
                                6,
                                itemTotal
                        );

                        statement.executeUpdate();
                    }
                }

                // =========================================
                // COMMIT
                // =========================================

                connection.commit();

                return
                        "{"
                        + "\"ok\":true,"
                        + "\"saleId\":"
                        + saleId
                        + ","
                        + "\"message\":"
                        + "\"Sale created successfully\""
                        + "}";

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
    // GET SALES HISTORY
    // =====================================================

    public String getSalesHistory(String date) {

        StringBuilder json = new StringBuilder();

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            String sql =
                    "SELECT s.sale_id, c.customer_name, " +
                    "c.customer_type, c.phone, c.email, c.gst, " +
                    "s.subtotal, s.discount, s.gst, " +
                    "s.final_total, s.sale_date " +
                    "FROM sales s " +
                    "LEFT JOIN customers c " +
                    "ON s.customer_id = c.customer_id ";

            if (date != null && !date.isBlank()) {
                sql += "WHERE DATE(s.sale_date) = ? ";
            }

            sql += "ORDER BY s.sale_date DESC, s.sale_id DESC";

            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                if (date != null && !date.isBlank()) {
                    statement.setString(1, date);
                }

                try (ResultSet result =
                             statement.executeQuery()) {

                    boolean first = true;

                    while (result.next()) {

                        if (!first) {
                            json.append(",");
                        }

                        first = false;

                        json.append("{");

                        json.append("\"saleId\":")
                                .append(result.getInt("sale_id"))
                                .append(",");

                        json.append("\"customerName\":\"")
                                .append(escapeJson(
                                        result.getString("customer_name")))
                                .append("\",");

                        json.append("\"customerType\":\"")
                                .append(escapeJson(
                                        result.getString("customer_type")))
                                .append("\",");

                        json.append("\"customerPhone\":\"")
                                .append(escapeJson(
                                        result.getString("phone")))
                                .append("\",");

                        json.append("\"subtotal\":")
                                .append(result.getDouble("subtotal"))
                                .append(",");

                        json.append("\"discount\":")
                                .append(result.getDouble("discount"))
                                .append(",");

                        json.append("\"gst\":")
                                .append(result.getDouble("gst"))
                                .append(",");

                        json.append("\"finalTotal\":")
                                .append(result.getDouble("final_total"))
                                .append(",");

                        json.append("\"saleDate\":\"")
                                .append(escapeJson(
                                        String.valueOf(
                                                result.getTimestamp("sale_date"))))
                                .append("\"");

                        json.append("}");
                    }
                }
            }

            return "{\"ok\":true,\"sales\":[" + json + "]}";

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(e.getMessage());
        }
    }

    // =====================================================
    // GET SALE DETAILS
    // =====================================================

    public String getSaleDetails(int saleId) {

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            String saleSql =
                    "SELECT s.sale_id, c.customer_name, " +
                    "c.customer_type, c.phone, c.email, c.gst, " +
                    "s.subtotal, s.discount, s.gst, " +
                    "s.final_total, s.sale_date " +
                    "FROM sales s " +
                    "LEFT JOIN customers c " +
                    "ON s.customer_id = c.customer_id " +
                    "WHERE s.sale_id = ?";

            try (PreparedStatement statement =
                         connection.prepareStatement(saleSql)) {

                statement.setInt(1, saleId);

                try (ResultSet result =
                             statement.executeQuery()) {

                    if (!result.next()) {
                        return errorJson("Sale not found");
                    }

                    StringBuilder items =
                            new StringBuilder();

                    String itemSql =
                            "SELECT si.product_id, " +
                            "p.product_name, si.quantity, " +
                            "si.unit_price, si.discount, " +
                            "si.total_price " +
                            "FROM sale_items si " +
                            "LEFT JOIN products p " +
                            "ON si.product_id = p.product_id " +
                            "WHERE si.sale_id = ? " +
                            "ORDER BY si.product_id";

                    try (PreparedStatement itemStatement =
                                 connection.prepareStatement(itemSql)) {

                        itemStatement.setInt(1, saleId);

                        try (ResultSet itemResult =
                                     itemStatement.executeQuery()) {

                            boolean first = true;

                            while (itemResult.next()) {

                                if (!first) {
                                    items.append(",");
                                }

                                first = false;

                                items.append("{");

                                items.append("\"productId\":")
                                        .append(itemResult.getInt("product_id"))
                                        .append(",");

                                items.append("\"productName\":\"")
                                        .append(escapeJson(
                                                itemResult.getString("product_name")))
                                        .append("\",");

                                items.append("\"quantity\":")
                                        .append(itemResult.getInt("quantity"))
                                        .append(",");

                                items.append("\"unitPrice\":")
                                        .append(itemResult.getDouble("unit_price"))
                                        .append(",");

                                items.append("\"discount\":")
                                        .append(itemResult.getDouble("discount"))
                                        .append(",");

                                items.append("\"totalPrice\":")
                                        .append(itemResult.getDouble("total_price"));

                                items.append("}");
                            }
                        }
                    }

                    return "{"
                            + "\"ok\":true,"
                            + "\"saleId\":" + result.getInt("sale_id") + ","
                            + "\"customerName\":\""
                            + escapeJson(result.getString("customer_name"))
                            + "\","
                            + "\"customerType\":\""
                            + escapeJson(result.getString("customer_type"))
                            + "\","
                            + "\"customerPhone\":\""
                            + escapeJson(result.getString("phone"))
                            + "\","
                            + "\"customerEmail\":\""
                            + escapeJson(result.getString("email"))
                            + "\","
                            + "\"customerGst\":\""
                            + escapeJson(result.getString("gst"))
                            + "\","
                            + "\"subtotal\":" + result.getDouble("subtotal") + ","
                            + "\"discount\":" + result.getDouble("discount") + ","
                            + "\"gst\":" + result.getDouble("gst") + ","
                            + "\"finalTotal\":" + result.getDouble("final_total") + ","
                            + "\"saleDate\":\""
                            + escapeJson(String.valueOf(
                                    result.getTimestamp("sale_date")))
                            + "\","
                            + "\"items\":[" + items + "]"
                            + "}";
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(e.getMessage());
        }
    }

    // =====================================================
    // DELETE SALE AND RESTORE STOCK
    // =====================================================

    public String deleteSale(int saleId) {

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                String stockSql =
                        "SELECT product_id, quantity " +
                        "FROM sale_items " +
                        "WHERE sale_id = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(stockSql)) {

                    statement.setInt(1, saleId);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        while (result.next()) {

                            int productId =
                                    result.getInt("product_id");

                            int quantity =
                                    result.getInt("quantity");

                            String restoreSql =
                                    "UPDATE products " +
                                    "SET quantity = quantity + ? " +
                                    "WHERE product_id = ?";

                            try (PreparedStatement restore =
                                         connection.prepareStatement(
                                                 restoreSql)) {

                                restore.setInt(1, quantity);
                                restore.setInt(2, productId);

                                restore.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "DELETE FROM sale_items " +
                                     "WHERE sale_id = ?")) {

                    statement.setInt(1, saleId);

                    statement.executeUpdate();
                }

                int deleted;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     "DELETE FROM sales " +
                                     "WHERE sale_id = ?")) {

                    statement.setInt(1, saleId);

                    deleted = statement.executeUpdate();
                }

                if (deleted == 0) {
                    connection.rollback();
                    return errorJson("Sale not found");
                }

                connection.commit();

                return "{"
                        + "\"ok\":true,"
                        + "\"message\":\"Sale deleted successfully\""
                        + "}";

            } catch (Exception e) {

                connection.rollback();

                throw e;
            }

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(e.getMessage());
        }
    }

    // =====================================================
    // FIND OR CREATE CUSTOMER
    // =====================================================

    private int findOrCreateCustomer(
            Connection connection,
            String customerName,
            String customerType,
            String customerPhone,
            String customerEmail,
            String customerGst
    ) throws Exception {

        // A phone number is the unique revisitation key for sales.
        if (customerPhone != null && !customerPhone.isBlank()) {
            String phoneSql =
                    "SELECT customer_id FROM customers WHERE phone = ? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(phoneSql)) {
                statement.setString(1, customerPhone);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        int id = result.getInt("customer_id");
                        String updateSql =
                                "UPDATE customers SET customer_name=?, customer_type=?, email=?, gst=? WHERE customer_id=?";
                        try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                            update.setString(1, customerName);
                            update.setString(2, customerType);
                            update.setString(3, customerEmail);
                            update.setString(4, customerGst);
                            update.setInt(5, id);
                            update.executeUpdate();
                        }
                        return id;
                    }
                }
            }
        }

        String findSql =
                "SELECT customer_id FROM customers WHERE customer_name = ? AND customer_type = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, customerName);
            statement.setString(2, customerType);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getInt("customer_id");
            }
        }

        String insertSql =
                "INSERT INTO customers (customer_name, customer_type, phone, email, gst) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, customerName);
            statement.setString(2, customerType);
            statement.setString(3, customerPhone);
            statement.setString(4, customerEmail);
            statement.setString(5, customerGst);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new Exception("Could not create customer");
    }

    // =====================================================
    // GET JSON STRING
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
    // GET INTEGER
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
    // GET DOUBLE
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
    // GET NUMBER
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
                &&
                (
                    Character.isDigit(
                            json.charAt(end)
                    )
                    ||
                    json.charAt(end) == '.'
                    ||
                    json.charAt(end) == '-'
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
    // GET JSON ARRAY
    // =====================================================

    private String getArray(
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
                json.indexOf(
                        "[",
                        colonPosition
                );

        if (start == -1) {
            return "";
        }

        int end =
                findMatchingBracket(
                        json,
                        start,
                        '[',
                        ']'
                );

        if (end == -1) {
            return "";
        }

        return json.substring(
                start + 1,
                end
        );
    }

    // =====================================================
    // FIND MATCHING BRACKET
    // =====================================================

    private int findMatchingBracket(
            String text,
            int start,
            char opening,
            char closing
    ) {

        int depth = 0;

        boolean insideString = false;
        boolean escaped = false;

        for (
                int i = start;
                i < text.length();
                i++
        ) {

            char c =
                    text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' &&
                    insideString) {

                escaped = true;
                continue;
            }

            if (c == '"') {

                insideString =
                        !insideString;

                continue;
            }

            if (insideString) {
                continue;
            }

            if (c == opening) {
                depth++;
            }

            if (c == closing) {

                depth--;

                if (depth == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    // =====================================================
    // SPLIT JSON OBJECTS
    // =====================================================

    private String[] splitObjects(
            String arrayContent
    ) {

        if (arrayContent == null ||
                arrayContent.isBlank()) {

            return new String[0];
        }

        java.util.List<String> objects =
                new java.util.ArrayList<>();

        int start = -1;
        int depth = 0;

        boolean insideString = false;
        boolean escaped = false;

        for (
                int i = 0;
                i < arrayContent.length();
                i++
        ) {

            char c =
                    arrayContent.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' &&
                    insideString) {

                escaped = true;
                continue;
            }

            if (c == '"') {

                insideString =
                        !insideString;

                continue;
            }

            if (insideString) {
                continue;
            }

            if (c == '{') {

                if (depth == 0) {
                    start = i;
                }

                depth++;
            }

            if (c == '}') {

                depth--;

                if (depth == 0 &&
                        start != -1) {

                    objects.add(
                            arrayContent.substring(
                                    start,
                                    i + 1
                            )
                    );

                    start = -1;
                }
            }
        }

        return objects.toArray(
                new String[0]
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
            message =
                    "Unknown error";
        }

        return
                "{\"ok\":false,\"error\":\""
                + escapeJson(message)
                + "\"}";
    }
}