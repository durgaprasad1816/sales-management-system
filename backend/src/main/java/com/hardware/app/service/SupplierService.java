package com.hardware.app.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.hardware.app.config.DatabaseConfig;

public class SupplierService {

    public String getSuppliers(String search) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"suppliers\":[");
        try (Connection c = DatabaseConfig.getConnection()) {
            String sql = "SELECT s.supplier_id,s.supplier_name,s.phone,s.email,s.city," +
                    "COUNT(p.purchase_id) purchase_count,MAX(p.purchase_date) last_purchase " +
                    "FROM suppliers s LEFT JOIN purchases p ON p.supplier_id=s.supplier_id ";
            if (search != null && !search.isBlank()) sql += "WHERE s.supplier_name LIKE ? ";
            sql += "GROUP BY s.supplier_id ORDER BY s.supplier_name";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (search != null && !search.isBlank()) ps.setString(1, "%" + search + "%");
                try (ResultSet r = ps.executeQuery()) {
                    boolean first = true;
                    while (r.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append('{')
                                .append("\"supplierId\":").append(r.getInt("supplier_id")).append(',')
                                .append("\"supplierName\":\"").append(e(r.getString("supplier_name"))).append("\",")
                                .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                                .append("\"email\":\"").append(e(r.getString("email"))).append("\",")
                                .append("\"city\":\"").append(e(r.getString("city"))).append("\",")
                                .append("\"purchaseCount\":").append(r.getInt("purchase_count")).append(',')
                                .append("\"lastPurchase\":\"").append(e(String.valueOf(r.getTimestamp("last_purchase")))).append("\"}");
                    }
                }
            }
        } catch (Exception ex) {
            return err(ex.getMessage());
        }
        return json.append("]}").toString();
    }

    public String getSupplierDetails(int id) {
        try (Connection c = DatabaseConfig.getConnection()) {
            StringBuilder json = new StringBuilder("{\"ok\":true,\"supplier\":");
            String supplierSql = "SELECT supplier_id,supplier_name,phone,landline,email,fax,gst,street,door_number,village,district,city,pincode,country FROM suppliers WHERE supplier_id=?";
            try (PreparedStatement ps = c.prepareStatement(supplierSql)) {
                ps.setInt(1, id);
                try (ResultSet r = ps.executeQuery()) {
                    if (!r.next()) return err("Supplier not found");
                    json.append('{')
                            .append("\"supplierId\":").append(r.getInt("supplier_id")).append(',')
                            .append("\"supplierName\":\"").append(e(r.getString("supplier_name"))).append("\",")
                            .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                            .append("\"landline\":\"").append(e(r.getString("landline"))).append("\",")
                            .append("\"email\":\"").append(e(r.getString("email"))).append("\",")
                            .append("\"fax\":\"").append(e(r.getString("fax"))).append("\",")
                            .append("\"gst\":\"").append(e(r.getString("gst"))).append("\",")
                            .append("\"street\":\"").append(e(r.getString("street"))).append("\",")
                            .append("\"doorNumber\":\"").append(e(r.getString("door_number"))).append("\",")
                            .append("\"village\":\"").append(e(r.getString("village"))).append("\",")
                            .append("\"district\":\"").append(e(r.getString("district"))).append("\",")
                            .append("\"city\":\"").append(e(r.getString("city"))).append("\",")
                            .append("\"pincode\":\"").append(e(r.getString("pincode"))).append("\",")
                            .append("\"country\":\"").append(e(r.getString("country"))).append("\"},\"purchases\":[");
                }
            }
            String sql = "SELECT p.purchase_id,p.invoice_number,p.purchase_date,p.total_amount," +
                    "GROUP_CONCAT(CONCAT(pr.product_name,' x ',pi.quantity) ORDER BY pr.product_name SEPARATOR ', ') products " +
                    "FROM purchases p LEFT JOIN purchase_items pi ON pi.purchase_id=p.purchase_id " +
                    "LEFT JOIN products pr ON pr.product_id=pi.product_id WHERE p.supplier_id=? " +
                    "GROUP BY p.purchase_id ORDER BY p.purchase_date DESC,p.purchase_id DESC";
            boolean first = true;
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet r = ps.executeQuery()) {
                    while (r.next()) {
                        if (!first) json.append(',');
                        first = false;
                        int purchaseId = r.getInt("purchase_id");
                        json.append('{')
                                .append("\"purchaseId\":").append(purchaseId).append(',')
                                .append("\"invoiceNumber\":\"").append(e(r.getString("invoice_number"))).append("\",")
                                .append("\"purchaseDate\":\"").append(e(String.valueOf(r.getTimestamp("purchase_date")))).append("\",")
                                .append("\"totalAmount\":").append(r.getDouble("total_amount")).append(',')
                                .append("\"products\":\"").append(e(r.getString("products"))).append("\",\"items\":[");

                        String itemSql = "SELECT pi.product_id,pr.product_name,pr.product_type,pi.quantity,pi.unit_price,pi.total_price " +
                                "FROM purchase_items pi JOIN products pr ON pr.product_id=pi.product_id " +
                                "WHERE pi.purchase_id=? ORDER BY pr.product_name";
                        boolean itemFirst = true;
                        try (PreparedStatement itemPs = c.prepareStatement(itemSql)) {
                            itemPs.setInt(1, purchaseId);
                            try (ResultSet item = itemPs.executeQuery()) {
                                while (item.next()) {
                                    if (!itemFirst) json.append(',');
                                    itemFirst = false;
                                    json.append('{')
                                            .append("\"productId\":").append(item.getInt("product_id")).append(',')
                                            .append("\"productName\":\"").append(e(item.getString("product_name"))).append("\",")
                                            .append("\"productType\":\"").append(e(item.getString("product_type"))).append("\",")
                                            .append("\"quantity\":").append(item.getInt("quantity")).append(',')
                                            .append("\"unitPrice\":").append(item.getDouble("unit_price")).append(',')
                                            .append("\"totalPrice\":").append(item.getDouble("total_price"))
                                            .append('}');
                                }
                            }
                        }
                        json.append("]}");
                    }
                }
            }
            return json.append("]}").toString();
        } catch (Exception ex) {
            return err(ex.getMessage());
        }
    }

    private static String e(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static String err(String s) {
        return "{\"ok\":false,\"error\":\"" + e(s == null ? "Unknown error" : s) + "\"}";
    }
}
