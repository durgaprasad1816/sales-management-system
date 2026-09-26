package com.hardware.app.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.hardware.app.config.DatabaseConfig;
import com.sun.net.httpserver.HttpExchange;

public class PurchaseService {

    // =====================================================
    // SAVE PURCHASE
    // =====================================================

    public String createPurchaseFromJson(String json) {

        String supplierName =
                getString(json, "supplierName");

        String supplierPhone =
                getString(json, "supplierPhone");
        String supplierLandline = getString(json, "supplierLandline");
        String supplierEmail = getString(json, "supplierEmail");
        String supplierFax = getString(json, "supplierFax");
        String supplierGst = getString(json, "supplierGst");
        String supplierStreet = getString(json, "supplierStreet");
        String supplierDoorNumber = getString(json, "supplierDoorNumber");
        String supplierVillage = getString(json, "supplierVillage");
        String supplierDistrict = getString(json, "supplierDistrict");
        String supplierCity = getString(json, "supplierCity");
        String supplierPincode = getString(json, "supplierPincode");
        String supplierCountry = getString(json, "supplierCountry");

        String invoiceNumber =
                getString(json, "invoiceNumber");
        String purchaseDate = getString(json, "purchaseDate");

        String productName =
                getString(json, "productName");

        String productType =
                getString(json, "productType");

        int quantity =
                getInt(json, "quantity");

        double unitPrice =
                getDouble(json, "unitPrice");

        double total =
                getDouble(json, "total");

        if (supplierName == null ||
                supplierName.isBlank()) {

            return errorJson(
                    "Supplier name is required"
            );
        }

        if (productName == null ||
                productName.isBlank()) {

            return errorJson(
                    "Product name is required"
            );
        }

        if (quantity <= 0) {

            return errorJson(
                    "Quantity must be greater than 0"
            );
        }

        if (unitPrice < 0) {

            return errorJson(
                    "Purchase price cannot be negative"
            );
        }

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // =========================================
                // SUPPLIER
                // =========================================

                int supplierId =
                        findOrCreateSupplier(
                                connection, supplierName, supplierPhone, supplierLandline, supplierEmail, supplierFax,
                                supplierGst, supplierStreet, supplierDoorNumber, supplierVillage, supplierDistrict, supplierCity, supplierPincode, supplierCountry
                        );

                // =========================================
                // PRODUCT
                // =========================================

                int productId =
                        findProduct(
                                connection,
                                productName
                        );

                if (productId == -1) {

                    productId =
                            createProduct(
                                    connection,
                                    productName,
                                    productType,
                                    quantity,
                                    unitPrice
                            );

                } else {

                    updateProductStock(
                            connection,
                            productId,
                            quantity,
                            unitPrice,
                            productType
                    );
                }

                // =========================================
                // PURCHASE
                // =========================================

                String purchaseSql =
                        "INSERT INTO purchases " +
                        "(supplier_id, invoice_number, total_amount, purchase_date) " +
                        "VALUES (?, ?, ?, COALESCE(STR_TO_DATE(?, '%Y-%m-%d'), CURRENT_TIMESTAMP))";

                int purchaseId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     purchaseSql,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setInt(
                            1,
                            supplierId
                    );

                    statement.setString(
                            2,
                            invoiceNumber
                    );

                    statement.setDouble(
                            3,
                            total
                    );
                    statement.setString(4, purchaseDate);

                    statement.executeUpdate();

                    try (ResultSet keys =
                                 statement.getGeneratedKeys()) {

                        if (!keys.next()) {

                            throw new SQLException(
                                    "Could not create purchase"
                            );
                        }

                        purchaseId =
                                keys.getInt(1);
                    }
                }

                // =========================================
                // PURCHASE ITEM
                // =========================================

                String itemSql =
                        "INSERT INTO purchase_items " +
                        "(purchase_id, product_id, " +
                        "quantity, unit_price, total_price) " +
                        "VALUES (?, ?, ?, ?, ?)";

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     itemSql
                             )) {

                    statement.setInt(
                            1,
                            purchaseId
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
                            total
                    );

                    statement.executeUpdate();
                }

                // =========================================
                // COMMIT
                // =========================================

                connection.commit();

                return
                        "{"
                        + "\"ok\":true,"
                        + "\"purchaseId\":"
                        + purchaseId
                        + ","
                        + "\"message\":"
                        + "\"Purchase saved successfully\""
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
    // FIND OR CREATE SUPPLIER
    // =====================================================

    private int findOrCreateSupplier(
            Connection connection,
            String supplierName,
            String supplierPhone
    ) throws SQLException {
        try {
            return findOrCreateSupplier(connection, supplierName, supplierPhone, "", "", "", "", "", "", "", "", "", "", "");
        } catch (Exception e) {
            if (e instanceof SQLException) throw (SQLException)e;
            throw new SQLException(e.getMessage(), e);
        }
    }

    private int findOrCreateSupplier(
            Connection connection,
            String supplierName,
            String supplierPhone,
            String landline,
            String email,
            String fax,
            String gst,
            String street,
            String doorNumber,
            String village,
            String district,
            String city,
            String pincode,
            String country
    ) throws Exception {
        String findSql = "SELECT supplier_id FROM suppliers WHERE supplier_name = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setString(1, supplierName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    int id = result.getInt("supplier_id");
                    String updateSql = "UPDATE suppliers SET phone=?, landline=?, email=?, fax=?, gst=?, street=?, door_number=?, village=?, district=?, city=?, pincode=?, country=? WHERE supplier_id=?";
                    try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                        update.setString(1,supplierPhone); update.setString(2,landline); update.setString(3,email); update.setString(4,fax); update.setString(5,gst);
                        update.setString(6,street); update.setString(7,doorNumber); update.setString(8,village); update.setString(9,district); update.setString(10,city); update.setString(11,pincode); update.setString(12,country); update.setInt(13,id); update.executeUpdate();
                    }
                    return id;
                }
            }
        }
        String insertSql = "INSERT INTO suppliers (supplier_name, phone, landline, email, fax, gst, street, door_number, village, district, city, pincode, country) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1,supplierName); statement.setString(2,supplierPhone); statement.setString(3,landline); statement.setString(4,email); statement.setString(5,fax); statement.setString(6,gst);
            statement.setString(7,street); statement.setString(8,doorNumber); statement.setString(9,village); statement.setString(10,district); statement.setString(11,city); statement.setString(12,pincode); statement.setString(13,country); statement.executeUpdate();
            try(ResultSet keys=statement.getGeneratedKeys()){ if(keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("Could not create supplier");
    }

    // =====================================================
    // FIND PRODUCT
    // =====================================================

    private int findProduct(
            Connection connection,
            String productName
    ) throws SQLException {

        String sql =
                "SELECT product_id " +
                "FROM products " +
                "WHERE product_name = ? " +
                "LIMIT 1";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    productName
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(
                            "product_id"
                    );
                }
            }
        }

        return -1;
    }

    // =====================================================
    // CREATE PRODUCT
    // =====================================================

    private int createProduct(
            Connection connection,
            String productName,
            String productType,
            int quantity,
            double purchasePrice
    ) throws SQLException {

        String sql =
                "INSERT INTO products " +
                "(product_name, product_type, " +
                "quantity, purchase_price, " +
                "sale_price, discount) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
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
                    purchasePrice
            );

            statement.setDouble(
                    6,
                    0
            );

            statement.executeUpdate();

            try (ResultSet keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {

                    return keys.getInt(1);
                }
            }
        }

        throw new SQLException(
                "Could not create product"
        );
    }

    // =====================================================
    // UPDATE PRODUCT STOCK
    // =====================================================

    private void updateProductStock(
            Connection connection,
            int productId,
            int quantity,
            double purchasePrice,
            String productType
    ) throws SQLException {

        String sql =
                "UPDATE products " +
                "SET quantity = quantity + ?, " +
                "purchase_price = ?, " +
                "product_type = ? " +
                "WHERE product_id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(
                    1,
                    quantity
            );

            statement.setDouble(
                    2,
                    purchasePrice
            );

            statement.setString(
                    3,
                    productType
            );

            statement.setInt(
                    4,
                    productId
            );

            statement.executeUpdate();
        }
    }

    // =====================================================
    // IMPORT MULTI-PRODUCT PURCHASE
    // =====================================================

    public String importPurchaseFromJson(String json) {

        String supplierName =
                getString(
                        json,
                        "supplierName"
                );

        String supplierPhone =
                getString(
                        json,
                        "supplierPhone"
                );
        String supplierLandline = getString(json, "supplierLandline");
        String supplierEmail = getString(json, "supplierEmail");
        String supplierFax = getString(json, "supplierFax");
        String supplierGst = getString(json, "supplierGst");
        String supplierStreet = getString(json, "supplierStreet");
        String supplierDoorNumber = getString(json, "supplierDoorNumber");
        String supplierVillage = getString(json, "supplierVillage");
        String supplierDistrict = getString(json, "supplierDistrict");
        String supplierCity = getString(json, "supplierCity");
        String supplierPincode = getString(json, "supplierPincode");
        String supplierCountry = getString(json, "supplierCountry");

        String invoiceNumber =
                getString(
                        json,
                        "invoiceNumber"
                );

        String purchaseDate = getString(json, "purchaseDate");

        if (supplierName == null ||
                supplierName.isBlank()) {

            return errorJson(
                    "Supplier name is required"
            );
        }

        String productsJson =
                getArray(
                        json,
                        "products"
                );

        if (productsJson == null ||
                productsJson.isBlank()) {

            return errorJson(
                    "No products found for import"
            );
        }

        java.util.List<String> productObjects =
                splitJsonObjects(
                        productsJson
                );

        if (productObjects.isEmpty()) {

            return errorJson(
                    "No valid products found for import"
            );
        }

        try (Connection connection =
                     DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // =========================================
                // SUPPLIER
                // =========================================

                int supplierId =
                        findOrCreateSupplier(
                                connection, supplierName, supplierPhone, supplierLandline, supplierEmail, supplierFax,
                                supplierGst, supplierStreet, supplierDoorNumber, supplierVillage, supplierDistrict, supplierCity, supplierPincode, supplierCountry
                        );

                // =========================================
                // CALCULATE TOTAL
                // =========================================

                double purchaseTotal = 0;

                for (
                        String productJson :
                        productObjects
                ) {

                    String productName =
                            getString(
                                    productJson,
                                    "productName"
                            );

                    int quantity =
                            getInt(
                                    productJson,
                                    "quantity"
                            );

                    double unitPrice =
                            getDouble(
                                    productJson,
                                    "unitPrice"
                            );

                    if (
                            productName == null ||
                            productName.isBlank()
                    ) {

                        throw new IllegalArgumentException(
                                "Product name is required"
                        );
                    }

                    if (quantity <= 0) {

                        throw new IllegalArgumentException(
                                "Quantity must be greater than 0"
                        );
                    }

                    if (unitPrice < 0) {

                        throw new IllegalArgumentException(
                                "Purchase price cannot be negative"
                        );
                    }

                    purchaseTotal +=
                            quantity * unitPrice;
                }

                // =========================================
                // CREATE PURCHASE
                // =========================================

                int purchaseId;

                String purchaseSql =
                        "INSERT INTO purchases " +
                        "(supplier_id, invoice_number, total_amount, purchase_date) " +
                        "VALUES (?, ?, ?, COALESCE(STR_TO_DATE(?, '%Y-%m-%d'), CURRENT_TIMESTAMP))";

                try (
                        PreparedStatement statement =
                                connection.prepareStatement(
                                        purchaseSql,
                                        Statement.RETURN_GENERATED_KEYS
                                )
                ) {

                    statement.setInt(
                            1,
                            supplierId
                    );

                    statement.setString(
                            2,
                            invoiceNumber
                    );

                    statement.setDouble(
                            3,
                            purchaseTotal
                    );
                    statement.setString(4, purchaseDate);

                    statement.executeUpdate();

                    try (
                            ResultSet keys =
                                    statement.getGeneratedKeys()
                    ) {

                        if (!keys.next()) {

                            throw new SQLException(
                                    "Could not create purchase"
                            );
                        }

                        purchaseId =
                                keys.getInt(1);
                    }
                }

                // =========================================
                // PURCHASE ITEMS
                // =========================================

                String itemSql =
                        "INSERT INTO purchase_items " +
                        "(purchase_id, product_id, " +
                        "quantity, unit_price, total_price) " +
                        "VALUES (?, ?, ?, ?, ?)";

                for (
                        String productJson :
                        productObjects
                ) {

                    String productName =
                            getString(
                                    productJson,
                                    "productName"
                            );

                    String productType =
                            getString(
                                    productJson,
                                    "productType"
                            );

                    int quantity =
                            getInt(
                                    productJson,
                                    "quantity"
                            );

                    double unitPrice =
                            getDouble(
                                    productJson,
                                    "unitPrice"
                            );

                    double total =
                            quantity * unitPrice;

                    // =====================================
                    // FIND PRODUCT
                    // =====================================

                    int productId =
                            findProduct(
                                    connection,
                                    productName
                            );

                    // =====================================
                    // NEW PRODUCT
                    // =====================================

                    if (productId == -1) {

                        productId =
                                createProduct(
                                        connection,
                                        productName,
                                        productType,
                                        quantity,
                                        unitPrice
                                );

                    }

                    // =====================================
                    // EXISTING PRODUCT
                    // =====================================

                    else {

                        updateProductStock(
                                connection,
                                productId,
                                quantity,
                                unitPrice,
                                productType
                        );
                    }

                    // =====================================
                    // INSERT PURCHASE ITEM
                    // =====================================

                    try (
                            PreparedStatement statement =
                                    connection.prepareStatement(
                                            itemSql
                                    )
                    ) {

                        statement.setInt(
                                1,
                                purchaseId
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
                                total
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
                        + "\"purchaseId\":"
                        + purchaseId
                        + ","
                        + "\"productCount\":"
                        + productObjects.size()
                        + ","
                        + "\"total\":"
                        + purchaseTotal
                        + ","
                        + "\"message\":"
                        + "\"Purchase imported successfully. Stock updated.\""
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

        int depth = 0;

        boolean inString = false;

        boolean escaped = false;

        for (
                int i = start;
                i < json.length();
                i++
        ) {

            char c =
                    json.charAt(i);

            if (inString) {

                if (escaped) {

                    escaped = false;

                } else if (c == '\\') {

                    escaped = true;

                } else if (c == '"') {

                    inString = false;
                }

                continue;
            }

            if (c == '"') {

                inString = true;

            } else if (c == '[') {

                depth++;

            } else if (c == ']') {

                depth--;

                if (depth == 0) {

                    return json.substring(
                            start + 1,
                            i
                    );
                }
            }
        }

        return "";
    }

    // =====================================================
    // SPLIT JSON OBJECTS FROM ARRAY
    // =====================================================

    private java.util.List<String> splitJsonObjects(
            String arrayContent
    ) {

        java.util.List<String> objects =
                new java.util.ArrayList<>();

        int depth = 0;

        int objectStart = -1;

        boolean inString = false;

        boolean escaped = false;

        for (
                int i = 0;
                i < arrayContent.length();
                i++
        ) {

            char c =
                    arrayContent.charAt(i);

            if (inString) {

                if (escaped) {

                    escaped = false;

                } else if (c == '\\') {

                    escaped = true;

                } else if (c == '"') {

                    inString = false;
                }

                continue;
            }

            if (c == '"') {

                inString = true;

            } else if (c == '{') {

                if (depth == 0) {

                    objectStart = i;
                }

                depth++;

            } else if (c == '}') {

                depth--;

                if (
                        depth == 0 &&
                        objectStart >= 0
                ) {

                    objects.add(
                            arrayContent.substring(
                                    objectStart,
                                    i + 1
                            )
                    );

                    objectStart = -1;
                }
            }
        }

        return objects;
    }

    // =====================================================
    // READ GENERIC SUPPLIER FILE
    // =====================================================

    public String readSupplierFile(
            HttpExchange exchange
    ) throws IOException {

        MultipartFileData file =
                readMultipartFile(exchange);

        if (
                file == null ||
                file.bytes == null ||
                file.bytes.length == 0
        ) {
            return errorJson(
                    "Supplier file not received"
            );
        }

        String fileName =
                file.fileName == null
                        ? "supplier-file"
                        : file.fileName;

        String lowerName =
                fileName.toLowerCase();

        try {

            String text;
            String fileType;

            if (lowerName.endsWith(".pdf")) {

                fileType = "PDF";

                try (
                        PDDocument document =
                                Loader.loadPDF(file.bytes)
                ) {

                    PDFTextStripper stripper =
                            new PDFTextStripper();

                    text =
                            stripper.getText(
                                    document
                            );
                }

            } else if (
                    lowerName.endsWith(".xlsx") ||
                    lowerName.endsWith(".xls")
            ) {

                fileType =
                        lowerName.endsWith(".xlsx")
                                ? "XLSX"
                                : "XLS";

                text =
                        readExcelFile(
                                file.bytes
                        );

            } else if (lowerName.endsWith(".csv")) {

                fileType = "CSV";

                text =
                        new String(
                                file.bytes,
                                StandardCharsets.UTF_8
                        );

            } else if (lowerName.endsWith(".txt")) {

                fileType = "TXT";

                text =
                        new String(
                                file.bytes,
                                StandardCharsets.UTF_8
                        );

            } else {

                return errorJson(
                        "Unsupported file type. Supported files: PDF, XLSX, XLS, CSV and TXT."
                );
            }

            return
                    "{"
                    + "\"ok\":true,"
                    + "\"fileType\":\""
                    + escapeJson(fileType)
                    + "\","
                    + "\"fileName\":\""
                    + escapeJson(fileName)
                    + "\","
                    + "\"text\":\""
                    + escapeJson(text)
                    + "\""
                    + "}";

        } catch (Throwable e) {

            e.printStackTrace();

            return errorJson(
                    "Could not read "
                    + fileName
                    + ": "
                    + (e.getMessage() == null ? e.getClass().getName() : e.getMessage())
            );
        }
    }

    // =====================================================
    // READ EXCEL WORKBOOK
    // =====================================================

    private String readExcelFile(
            byte[] bytes
    ) throws IOException {

        StringBuilder text =
                new StringBuilder();

        try (
                Workbook workbook =
                        WorkbookFactory.create(
                                new ByteArrayInputStream(bytes)
                        )
        ) {

            DataFormatter formatter =
                    new DataFormatter();

            for (
                    int sheetIndex = 0;
                    sheetIndex < workbook.getNumberOfSheets();
                    sheetIndex++
            ) {

                Sheet sheet =
                        workbook.getSheetAt(
                                sheetIndex
                        );

                if (sheetIndex > 0) {
                    text.append("\n");
                }

                text.append(
                        "Sheet: "
                ).append(
                        sheet.getSheetName()
                ).append(
                        "\n"
                );

                for (Row row : sheet) {

                    boolean hasValue = false;

                    for (
                            int cellIndex =
                                    0;
                            cellIndex <
                                    row.getLastCellNum();
                            cellIndex++
                    ) {

                        if (cellIndex > 0) {
                            text.append("\t");
                        }

                        Cell cell =
                                row.getCell(
                                        cellIndex,
                                        Row.MissingCellPolicy
                                                .CREATE_NULL_AS_BLANK
                                );

                        String value =
                                formatter.formatCellValue(
                                        cell
                                ).trim();

                        if (!value.isBlank()) {
                            hasValue = true;
                        }

                        text.append(value);
                    }

                    if (hasValue) {
                        text.append("\n");
                    }
                }
            }
        } catch (Exception e) {

            throw new IOException(
                    "Could not read Excel file: "
                    + e.getMessage(),
                    e
            );
        }

        return text.toString();
    }

    // =====================================================
    // READ SUPPLIER PDF
    // =====================================================

    public String readSupplierPdf(
            HttpExchange exchange
    ) throws IOException {

        byte[] pdfBytes =
                readMultipartPdf(
                        exchange
                );

        if (
                pdfBytes == null ||
                pdfBytes.length == 0
        ) {

            return errorJson(
                    "PDF file not received"
            );
        }

        try {

            PDDocument document =
                    Loader.loadPDF(
                            pdfBytes
                    );

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text =
                    stripper.getText(
                            document
                    );

            document.close();

            return
                    "{"
                    + "\"ok\":true,"
                    + "\"text\":\""
                    + escapeJson(text)
                    + "\""
                    + "}";

        } catch (Exception e) {

            e.printStackTrace();

            return errorJson(
                    "Could not read PDF: "
                    + e.getMessage()
            );
        }
    }

    // =====================================================
    // SIMPLE MULTIPART FILE READER
    // =====================================================

    private static class MultipartFileData {

        private final String fileName;
        private final byte[] bytes;

        private MultipartFileData(
                String fileName,
                byte[] bytes
        ) {
            this.fileName = fileName;
            this.bytes = bytes;
        }
    }

    private MultipartFileData readMultipartFile(
            HttpExchange exchange
    ) throws IOException {

        String contentType =
                exchange.getRequestHeaders()
                        .getFirst(
                                "Content-Type"
                        );

        if (
                contentType == null ||
                !contentType.contains(
                        "multipart/form-data"
                )
        ) {
            throw new IOException(
                    "Request is not multipart/form-data"
            );
        }

        String boundary =
                getBoundary(
                        contentType
                );

        if (boundary == null) {
            throw new IOException(
                    "Multipart boundary not found"
            );
        }

        byte[] body =
                exchange.getRequestBody()
                        .readAllBytes();

        byte[] boundaryBytes =
                (
                        "--" + boundary
                ).getBytes(
                        StandardCharsets.ISO_8859_1
                );

        int start =
                indexOf(
                        body,
                        boundaryBytes,
                        0
                );

        if (start < 0) {
            return null;
        }

        int headerEnd =
                indexOf(
                        body,
                        new byte[]{
                                '\r',
                                '\n',
                                '\r',
                                '\n'
                        },
                        start
                );

        if (headerEnd < 0) {
            return null;
        }

        String headers =
                new String(
                        body,
                        start,
                        headerEnd - start,
                        StandardCharsets.ISO_8859_1
                );

        String fileName =
                extractMultipartFileName(
                        headers
                );

        int dataStart =
                headerEnd + 4;

        int nextBoundary =
                indexOf(
                        body,
                        boundaryBytes,
                        dataStart
                );

        if (nextBoundary < 0) {
            return null;
        }

        int dataEnd =
                nextBoundary - 2;

        if (dataEnd <= dataStart) {
            return null;
        }

        byte[] fileBytes =
                new byte[
                        dataEnd - dataStart
                ];

        System.arraycopy(
                body,
                dataStart,
                fileBytes,
                0,
                fileBytes.length
        );

        return new MultipartFileData(
                fileName,
                fileBytes
        );
    }

    private String extractMultipartFileName(
            String headers
    ) {

        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        "filename=\"([^\"]*)\"",
                        java.util.regex.Pattern.CASE_INSENSITIVE
                );

        java.util.regex.Matcher matcher =
                pattern.matcher(headers);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "supplier-file";
    }

    // =====================================================
    // SIMPLE MULTIPART PDF READER
    // =====================================================

    private byte[] readMultipartPdf(
            HttpExchange exchange
    ) throws IOException {

        String contentType =
                exchange.getRequestHeaders()
                        .getFirst(
                                "Content-Type"
                        );

        if (
                contentType == null ||
                !contentType.contains(
                        "multipart/form-data"
                )
        ) {

            throw new IOException(
                    "Request is not multipart/form-data"
            );
        }

        String boundary =
                getBoundary(
                        contentType
                );

        if (boundary == null) {

            throw new IOException(
                    "Multipart boundary not found"
            );
        }

        byte[] body =
                exchange.getRequestBody()
                        .readAllBytes();

        byte[] boundaryBytes =
                (
                    "--" + boundary
                ).getBytes(
                        StandardCharsets.ISO_8859_1
                );

        int start =
                indexOf(
                        body,
                        boundaryBytes,
                        0
                );

        if (start < 0) {

            return null;
        }

        int headerEnd =
                indexOf(
                        body,
                        new byte[]{
                                '\r',
                                '\n',
                                '\r',
                                '\n'
                        },
                        start
                );

        if (headerEnd < 0) {

            return null;
        }

        int dataStart =
                headerEnd + 4;

        int nextBoundary =
                indexOf(
                        body,
                        boundaryBytes,
                        dataStart
                );

        if (nextBoundary < 0) {

            return null;
        }

        int dataEnd =
                nextBoundary - 2;

        if (dataEnd <= dataStart) {

            return null;
        }

        byte[] pdf =
                new byte[
                        dataEnd - dataStart
                ];

        System.arraycopy(
                body,
                dataStart,
                pdf,
                0,
                pdf.length
        );

        return pdf;
    }

    // =====================================================
    // GET MULTIPART BOUNDARY
    // =====================================================

    private String getBoundary(
            String contentType
    ) {

        String marker =
                "boundary=";

        int index =
                contentType.indexOf(
                        marker
                );

        if (index < 0) {

            return null;
        }

        String boundary =
                contentType.substring(
                        index + marker.length()
                ).trim();

        if (
                boundary.startsWith("\"") &&
                boundary.endsWith("\"")
        ) {

            boundary =
                    boundary.substring(
                            1,
                            boundary.length() - 1
                    );
        }

        return boundary;
    }

    // =====================================================
    // BYTE ARRAY SEARCH
    // =====================================================

    private int indexOf(
            byte[] source,
            byte[] target,
            int start
    ) {

        outer:

        for (
                int i = start;
                i <= source.length - target.length;
                i++
        ) {

            for (
                    int j = 0;
                    j < target.length;
                    j++
            ) {

                if (
                        source[i + j]
                                != target[j]
                ) {

                    continue outer;
                }
            }

            return i;
        }

        return -1;
    }

    // =====================================================
    // JSON STRING
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
    // JSON INTEGER
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

        return Integer.parseInt(
                value
        );
    }

    // =====================================================
    // JSON DOUBLE
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

        return Double.parseDouble(
                value
        );
    }

    // =====================================================
    // JSON NUMBER
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
                &&
                Character.isWhitespace(
                        json.charAt(start)
                )
        ) {

            start++;
        }

        int end =
                start;

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
    // ESCAPE JSON
    // =====================================================

    private String escapeJson(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\t",
                        "\\t"
                );
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