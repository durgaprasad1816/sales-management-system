package com.hardware.app.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/** Adds only the optional columns required by the extended UI. Existing data is preserved. */
public final class SchemaInitializer {
    private SchemaInitializer() {}

    public static void ensure() {
        try (Connection c = DatabaseConfig.getConnection()) {
            ensureColumn(c, "suppliers", "landline", "VARCHAR(50)");
            ensureColumn(c, "suppliers", "email", "VARCHAR(150)");
            ensureColumn(c, "suppliers", "fax", "VARCHAR(50)");
            ensureColumn(c, "suppliers", "gst", "VARCHAR(50)");
            ensureColumn(c, "suppliers", "street", "VARCHAR(150)");
            ensureColumn(c, "suppliers", "door_number", "VARCHAR(50)");
            ensureColumn(c, "suppliers", "village", "VARCHAR(100)");
            ensureColumn(c, "suppliers", "district", "VARCHAR(100)");
            ensureColumn(c, "suppliers", "city", "VARCHAR(100)");
            ensureColumn(c, "suppliers", "pincode", "VARCHAR(20)");
            ensureColumn(c, "suppliers", "country", "VARCHAR(100)");
            ensureColumn(c, "purchases", "purchase_date", "DATETIME DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(c, "customers", "email", "VARCHAR(150)");
            ensureColumn(c, "customers", "gst", "VARCHAR(50)");
            System.out.println("Extended HardwarePro schema checked successfully.");
        } catch (Exception e) {
            System.err.println("Schema check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean hasColumn(Connection c, String table, String column) throws Exception {
        DatabaseMetaData md = c.getMetaData();
        try (ResultSet rs = md.getColumns(c.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }

    private static void ensureColumn(Connection c, String table, String column, String definition) throws Exception {
        if (hasColumn(c, table, column)) return;
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
        try (Statement st = c.createStatement()) { st.executeUpdate(sql); }
    }
}
