package com.hardware.app.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.hardware.app.config.DatabaseConfig;

public class CustomerService {
    public String getCustomers(String search,String phone){
        StringBuilder json=new StringBuilder("{\"ok\":true,\"customers\":[");
        try(Connection c=DatabaseConfig.getConnection()){
            String sql="SELECT c.customer_id,c.customer_name,c.customer_type,c.phone,c.email,c.gst,COUNT(s.sale_id) purchase_count,MAX(s.sale_date) last_purchase FROM customers c LEFT JOIN sales s ON s.customer_id=c.customer_id WHERE 1=1 ";
            if(search!=null&&!search.isBlank())sql+="AND c.customer_name LIKE ? ";
            if(phone!=null&&!phone.isBlank())sql+="AND c.phone LIKE ? ";
            sql+="GROUP BY c.customer_id ORDER BY c.customer_name";
            try(PreparedStatement ps=c.prepareStatement(sql)){int n=1;if(search!=null&&!search.isBlank())ps.setString(n++,"%"+search+"%");if(phone!=null&&!phone.isBlank())ps.setString(n++,"%"+phone+"%");try(ResultSet r=ps.executeQuery()){boolean first=true;while(r.next()){if(!first)json.append(',');first=false;json.append('{').append("\"customerId\":").append(r.getInt("customer_id")).append(',').append("\"customerName\":\"").append(e(r.getString("customer_name"))).append("\",").append("\"customerType\":\"").append(e(r.getString("customer_type"))).append("\",").append("\"phone\":\"").append(e(r.getString("phone"))).append("\",").append("\"email\":\"").append(e(r.getString("email"))).append("\",").append("\"gst\":\"").append(e(r.getString("gst"))).append("\",").append("\"purchaseCount\":").append(r.getInt("purchase_count")).append(',').append("\"lastPurchase\":\"").append(e(String.valueOf(r.getTimestamp("last_purchase")))).append("\"}");}}}
        }catch(Exception ex){return err(ex.getMessage());}
        return json.append("]}").toString();
    }
    public String getCustomerDetails(int id){
        try(Connection c=DatabaseConfig.getConnection()){
            StringBuilder json=new StringBuilder("{\"ok\":true,\"customer\":");
            try(PreparedStatement ps=c.prepareStatement("SELECT customer_id,customer_name,customer_type,phone,email,gst FROM customers WHERE customer_id=?")){ps.setInt(1,id);try(ResultSet r=ps.executeQuery()){if(!r.next())return err("Customer not found");json.append('{').append("\"customerId\":").append(r.getInt("customer_id")).append(',').append("\"customerName\":\"").append(e(r.getString("customer_name"))).append("\",").append("\"customerType\":\"").append(e(r.getString("customer_type"))).append("\",").append("\"phone\":\"").append(e(r.getString("phone"))).append("\",").append("\"email\":\"").append(e(r.getString("email"))).append("\",").append("\"gst\":\"").append(e(r.getString("gst"))).append("\"},\"sales\":[");}}
            String sql="SELECT s.sale_id,s.sale_date,s.final_total,GROUP_CONCAT(CONCAT(p.product_name,' x ',si.quantity) ORDER BY p.product_name SEPARATOR ', ') products FROM sales s LEFT JOIN sale_items si ON si.sale_id=s.sale_id LEFT JOIN products p ON p.product_id=si.product_id WHERE s.customer_id=? GROUP BY s.sale_id ORDER BY s.sale_date DESC,s.sale_id DESC";
            boolean first=true;try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,id);try(ResultSet r=ps.executeQuery()){while(r.next()){if(!first)json.append(',');first=false;json.append('{').append("\"saleId\":").append(r.getInt("sale_id")).append(',').append("\"saleDate\":\"").append(e(String.valueOf(r.getTimestamp("sale_date")))).append("\",").append("\"finalTotal\":").append(r.getDouble("final_total")).append(',').append("\"products\":\"").append(e(r.getString("products"))).append("\"}");}}}
            return json.append("]}").toString();
        }catch(Exception ex){return err(ex.getMessage());}
    }
    private static String e(String s){if(s==null)return "";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");}
    private static String err(String s){return "{\"ok\":false,\"error\":\""+e(s==null?"Unknown error":s)+"\"}";}
}
