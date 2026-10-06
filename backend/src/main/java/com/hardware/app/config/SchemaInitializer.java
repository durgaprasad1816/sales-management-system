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
            ensureContractorTables(c);
            System.out.println("Extended HardwarePro schema checked successfully.");
        } catch (Exception e) {
            System.err.println("Schema check failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureContractorTables(Connection c) throws Exception {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS contractors (" +
                    "contractor_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "contractor_name VARCHAR(150) NOT NULL," +
                    "phone VARCHAR(50) NOT NULL," +
                    "address VARCHAR(300)," +
                    "purpose VARCHAR(200)," +
                    "relation_start_date DATE NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_contractors_phone(phone), INDEX idx_contractors_name(contractor_name)" +
                    ") ENGINE=InnoDB");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS workers (" +
                    "worker_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "worker_name VARCHAR(150) NOT NULL," +
                    "phone VARCHAR(50) NOT NULL," +
                    "work_type VARCHAR(100) NOT NULL," +
                    "skills TEXT," +
                    "address VARCHAR(300)," +
                    "salary DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "aadhar_number VARCHAR(30)," +
                    "aadhar_image LONGTEXT," +
                    "active TINYINT(1) NOT NULL DEFAULT 1," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_workers_phone(phone), INDEX idx_workers_type(work_type), INDEX idx_workers_active(active), INDEX idx_workers_aadhar(aadhar_number)" +
                    ") ENGINE=InnoDB");
            ensureColumn(c, "workers", "skills", "TEXT");
            ensureColumn(c, "workers", "aadhar_number", "VARCHAR(30)");
            ensureColumn(c, "workers", "aadhar_image", "LONGTEXT");
            ensureColumn(c, "contractors", "relation_start_date", "DATE NULL");
            try (Statement fix = c.createStatement()) {
                fix.executeUpdate("UPDATE contractors SET relation_start_date=DATE(created_at) WHERE relation_start_date IS NULL");
            }

            st.executeUpdate("CREATE TABLE IF NOT EXISTS contractor_sales (" +
                    "sale_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "contractor_id INT NOT NULL," +
                    "total_workers INT NOT NULL DEFAULT 0," +
                    "total_amount DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "purpose VARCHAR(200)," +
                    "sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_contractor_sales_contractor(contractor_id)," +
                    "INDEX idx_contractor_sales_date(sale_date)," +
                    "CONSTRAINT fk_contractor_sales_contractor FOREIGN KEY (contractor_id) REFERENCES contractors(contractor_id)" +
                    ") ENGINE=InnoDB");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS contractor_sale_workers (" +
                    "assignment_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "sale_id INT NOT NULL," +
                    "worker_id INT NOT NULL," +
                    "worker_name VARCHAR(150) NOT NULL," +
                    "work_type VARCHAR(100) NOT NULL," +
                    "salary DECIMAL(12,2) NOT NULL DEFAULT 0," +
                    "assignment_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'," +
                    "assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "completed_at TIMESTAMP NULL," +
                    "INDEX idx_csw_worker_status(worker_id,assignment_status)," +
                    "INDEX idx_csw_sale(sale_id)," +
                    "CONSTRAINT fk_csw_sale FOREIGN KEY (sale_id) REFERENCES contractor_sales(sale_id)," +
                    "CONSTRAINT fk_csw_worker FOREIGN KEY (worker_id) REFERENCES workers(worker_id)" +
                    ") ENGINE=InnoDB");
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
