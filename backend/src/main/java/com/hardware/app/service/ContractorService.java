package com.hardware.app.service;

import com.hardware.app.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contractor and worker management. This service is additive and does not
 * modify the existing product/customer/sales workflow.
 */
public class ContractorService {

    public String getWorkers(String search, String workType, String status, String sort) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"workers\":[");
        try (Connection c = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT w.worker_id,w.worker_name,w.phone,w.work_type,w.address,w.salary,w.aadhar_number,w.aadhar_image,w.active, " +
                    "CASE WHEN EXISTS (SELECT 1 FROM contractor_sale_workers csw " +
                    "WHERE csw.worker_id=w.worker_id AND csw.assignment_status='IN_PROGRESS') " +
                    "THEN 'IN_PROGRESS' ELSE 'AVAILABLE' END current_status " +
                    "FROM workers w WHERE 1=1 ");
            List<String> params = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                sql.append("AND (w.worker_name LIKE ? OR w.phone LIKE ?) ");
                params.add("%" + search + "%");
                params.add("%" + search + "%");
            }
            if (workType != null && !workType.isBlank() && !workType.equalsIgnoreCase("ALL")) {
                sql.append("AND CONCAT(',',REPLACE(COALESCE(w.work_type,''),' ',''),',') LIKE ? ");
                params.add("%," + workType.replace(" ", "") + ",%");
            }
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                if (status.equalsIgnoreCase("ACTIVE")) sql.append("AND w.active=1 ");
                else if (status.equalsIgnoreCase("INACTIVE")) sql.append("AND w.active=0 ");
                else if (status.equalsIgnoreCase("IN_PROGRESS")) sql.append("AND EXISTS (SELECT 1 FROM contractor_sale_workers x WHERE x.worker_id=w.worker_id AND x.assignment_status='IN_PROGRESS') ");
                else if (status.equalsIgnoreCase("AVAILABLE")) sql.append("AND w.active=1 AND NOT EXISTS (SELECT 1 FROM contractor_sale_workers x WHERE x.worker_id=w.worker_id AND x.assignment_status='IN_PROGRESS') ");
            }
            String order = "w.worker_name ASC";
            if ("salary".equalsIgnoreCase(sort)) order = "w.salary DESC, w.worker_name ASC";
            else if ("worktype".equalsIgnoreCase(sort)) order = "w.work_type ASC, w.worker_name ASC";
            else if ("status".equalsIgnoreCase(sort)) order = "current_status ASC, w.worker_name ASC";
            else if ("phone".equalsIgnoreCase(sort)) order = "w.phone ASC";
            sql.append("ORDER BY ").append(order);

            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
                try (ResultSet r = ps.executeQuery()) {
                    boolean first = true;
                    while (r.next()) {
                        if (!first) json.append(',');
                        first = false;
                        json.append('{')
                                .append("\"workerId\":").append(r.getInt("worker_id")).append(',')
                                .append("\"workerName\":\"").append(e(r.getString("worker_name"))).append("\",")
                                .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                                .append("\"workType\":\"").append(e(r.getString("work_type"))).append("\",")
                                .append("\"address\":\"").append(e(r.getString("address"))).append("\",")
                                .append("\"salary\":").append(r.getDouble("salary")).append(',')
                                .append("\"aadharNumber\":\"").append(e(r.getString("aadhar_number"))).append("\",")
                                .append("\"hasAadharImage\":").append(r.getString("aadhar_image") != null && !r.getString("aadhar_image").isBlank()).append(',')
                                .append("\"active\":").append(r.getBoolean("active")).append(',')
                                .append("\"status\":\"").append(e(r.getString("current_status"))).append("\"}");
                    }
                }
            }
        } catch (Exception ex) {
            return err(ex.getMessage());
        }
        return json.append("]}").toString();
    }

    public String getWorkerDetails(int id) {
        if (id <= 0) return err("Invalid worker id.");
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT worker_id,worker_name,phone,work_type,address,salary,aadhar_number,aadhar_image,active FROM workers WHERE worker_id=?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) return err("Worker not found.");
                String image = r.getString("aadhar_image");
                return "{\"ok\":true,\"worker\":{" +
                        "\"workerId\":" + r.getInt("worker_id") + "," +
                        "\"workerName\":\"" + e(r.getString("worker_name")) + "\"," +
                        "\"phone\":\"" + e(r.getString("phone")) + "\"," +
                        "\"workType\":\"" + e(r.getString("work_type")) + "\"," +
                        "\"address\":\"" + e(r.getString("address")) + "\"," +
                        "\"salary\":" + r.getDouble("salary") + "," +
                        "\"aadharNumber\":\"" + e(r.getString("aadhar_number")) + "\"," +
                        "\"aadharImage\":\"" + e(image) + "\"," +
                        "\"active\":" + r.getBoolean("active") + "}}";
            }
        } catch (Exception ex) { return err(ex.getMessage()); }
    }

    public String addWorkerFromJson(String json) {
        String name = getString(json, "workerName");
        String phone = getString(json, "phone");
        String workType = getString(json, "workType");
        String address = getString(json, "address");
        String aadharNumber = getString(json, "aadharNumber");
        String aadharImage = getString(json, "aadharImage");
        double salary = getDouble(json, "salary");
        boolean active = getBoolean(json, "active", true);
        if (name.isBlank() || phone.isBlank() || workType.isBlank() || address.isBlank() || aadharNumber.isBlank() || aadharImage.isBlank() || salary < 0) return err("All worker fields including work type, address, Aadhar number and Aadhar image are required.");
        try (Connection c = DatabaseConfig.getConnection()) {
            try (PreparedStatement check = c.prepareStatement("SELECT worker_id FROM workers WHERE phone=? LIMIT 1")) {
                check.setString(1, phone);
                try (ResultSet r = check.executeQuery()) {
                    if (r.next()) return err("A worker with this phone number already exists.");
                }
            }
            String sql = "INSERT INTO workers(worker_name,phone,work_type,address,salary,aadhar_number,aadhar_image,active) VALUES(?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name); ps.setString(2, phone); ps.setString(3, workType); ps.setString(4, address);
                ps.setDouble(5, salary); ps.setString(6, aadharNumber); ps.setString(7, aadharImage); ps.setBoolean(8, active); ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    int id = k.next() ? k.getInt(1) : 0;
                    return "{\"ok\":true,\"workerId\":" + id + "}";
                }
            }
        } catch (Exception ex) { return err(ex.getMessage()); }
    }

    public String updateWorkerActive(int id, boolean active) {
        try (Connection c = DatabaseConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE workers SET active=? WHERE worker_id=?")) {
            ps.setBoolean(1, active); ps.setInt(2, id);
            if (ps.executeUpdate() == 0) return err("Worker not found.");
            return "{\"ok\":true}";
        } catch (Exception ex) { return err(ex.getMessage()); }
    }

    public String completeWorkerAssignment(int workerId) {
        try (Connection c = DatabaseConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                int assignmentId = 0;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT assignment_id FROM contractor_sale_workers WHERE worker_id=? AND assignment_status='IN_PROGRESS' ORDER BY assignment_id DESC LIMIT 1")) {
                    ps.setInt(1, workerId);
                    try (ResultSet r = ps.executeQuery()) { if (r.next()) assignmentId = r.getInt(1); }
                }
                if (assignmentId == 0) { c.rollback(); return err("No in-progress work found for this worker."); }
                try (PreparedStatement ps = c.prepareStatement("UPDATE contractor_sale_workers SET assignment_status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE assignment_id=?")) {
                    ps.setInt(1, assignmentId); ps.executeUpdate();
                }
                c.commit();
                return "{\"ok\":true}";
            } catch (Exception ex) { c.rollback(); return err(ex.getMessage()); }
            finally { c.setAutoCommit(true); }
        } catch (Exception ex) { return err(ex.getMessage()); }
    }

    public String getInProgressAssignments(String search) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"assignments\":[");
        try (Connection c = DatabaseConfig.getConnection()) {
            String sql = "SELECT cs.sale_id,c.contractor_id,c.contractor_name,c.phone,c.address,c.purpose,cs.sale_date, " +
                    "csw.assignment_id,csw.worker_id,csw.worker_name,csw.work_type,csw.salary,csw.assignment_status,w.phone worker_phone " +
                    "FROM contractor_sales cs JOIN contractors c ON c.contractor_id=cs.contractor_id " +
                    "JOIN contractor_sale_workers csw ON csw.sale_id=cs.sale_id " +
                    "JOIN workers w ON w.worker_id=csw.worker_id " +
                    "WHERE csw.assignment_status='IN_PROGRESS' " +
                    "AND (?='' OR c.contractor_name LIKE ? OR c.phone LIKE ?) " +
                    "ORDER BY c.contractor_name ASC,cs.sale_id DESC,csw.assignment_id ASC";
            try (PreparedStatement ps=c.prepareStatement(sql)) {
                String q=search==null?"":search.trim(); ps.setString(1,q); ps.setString(2,"%"+q+"%"); ps.setString(3,"%"+q+"%");
                try(ResultSet r=ps.executeQuery()) { boolean first=true; while(r.next()) {
                    if(!first)json.append(','); first=false;
                    json.append('{')
                        .append("\"saleId\":").append(r.getInt("sale_id")).append(',')
                        .append("\"contractorId\":").append(r.getInt("contractor_id")).append(',')
                        .append("\"contractorName\":\"").append(e(r.getString("contractor_name"))).append("\",")
                        .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                        .append("\"address\":\"").append(e(r.getString("address"))).append("\",")
                        .append("\"purpose\":\"").append(e(r.getString("purpose"))).append("\",")
                        .append("\"saleDate\":\"").append(e(String.valueOf(r.getTimestamp("sale_date")))).append("\",")
                        .append("\"assignmentId\":").append(r.getInt("assignment_id")).append(',')
                        .append("\"workerId\":").append(r.getInt("worker_id")).append(',')
                        .append("\"workerName\":\"").append(e(r.getString("worker_name"))).append("\",")
                        .append("\"workType\":\"").append(e(r.getString("work_type"))).append("\",")
                        .append("\"salary\":").append(r.getDouble("salary")).append(',')
                        .append("\"status\":\"").append(e(r.getString("assignment_status"))).append("\"}");
                }}
            }
        } catch(Exception ex){return err(ex.getMessage());}
        return json.append("]}").toString();
    }

    public String completeContractorAssignments(String json) {
        int saleId=getInt(json,"saleId");
        String workersJson=getArray(json,"workers");
        if(saleId<=0)return err("Invalid contractor service.");
        try(Connection c=DatabaseConfig.getConnection()){
            c.setAutoCommit(false);
            try{
                if(workersJson.isBlank()){
                    try(PreparedStatement ps=c.prepareStatement("UPDATE contractor_sale_workers SET assignment_status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE sale_id=? AND assignment_status='IN_PROGRESS'")){ps.setInt(1,saleId);ps.executeUpdate();}
                } else {
                    String[] workers=splitObjects(workersJson); Set<Integer> ids=new HashSet<>();
                    for(String wj:workers){int workerId=getInt(wj,"workerId");if(workerId>0)ids.add(workerId);}
                    if(ids.isEmpty())throw new Exception("Select at least one worker.");
                    try(PreparedStatement ps=c.prepareStatement("UPDATE contractor_sale_workers SET assignment_status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE sale_id=? AND worker_id=? AND assignment_status='IN_PROGRESS'")){for(Integer id:ids){ps.setInt(1,saleId);ps.setInt(2,id);ps.addBatch();}ps.executeBatch();}
                }
                c.commit(); return "{\"ok\":true}";
            }catch(Exception ex){c.rollback();return err(ex.getMessage());}finally{c.setAutoCommit(true);}
        }catch(Exception ex){return err(ex.getMessage());}
    }

    public String getContractors(String search, String sort) {
        return getContractors(search, sort, "");
    }

    public String getContractors(String search, String sort, String range) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"contractors\":[");
        try (Connection c = DatabaseConfig.getConnection()) {
            StringBuilder sql = new StringBuilder(
                    "SELECT c.contractor_id,c.contractor_name,c.phone,c.address,c.purpose,c.relation_start_date,c.created_at,COUNT(cs.sale_id) service_count, " +
                    "COALESCE(SUM(cs.total_amount),0) total_amount,MAX(cs.sale_date) last_service " +
                    "FROM contractors c LEFT JOIN contractor_sales cs ON cs.contractor_id=c.contractor_id ");
            List<String> params = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                sql.append("WHERE (c.contractor_name LIKE ? OR c.phone LIKE ?) ");
                params.add("%" + search + "%"); params.add("%" + search + "%");
            }
            if (range != null && !range.isBlank() && !range.equalsIgnoreCase("ALL")) {
                if ("0-1".equals(range)) sql.append(searchOrWhere(sql) + "TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) < 1 ");
                else if ("1-3".equals(range)) sql.append(searchOrWhere(sql) + "TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) >= 1 AND TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) < 3 ");
                else if ("3-5".equals(range)) sql.append(searchOrWhere(sql) + "TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) >= 3 AND TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) < 5 ");
                else if ("5+".equals(range)) sql.append(searchOrWhere(sql) + "TIMESTAMPDIFF(YEAR, COALESCE(c.relation_start_date, DATE(c.created_at)), CURRENT_DATE) >= 5 ");
            }
            sql.append("GROUP BY c.contractor_id ");
            if ("services".equalsIgnoreCase(sort)) sql.append("ORDER BY service_count DESC, c.contractor_name ASC");
            else if ("amount".equalsIgnoreCase(sort)) sql.append("ORDER BY total_amount DESC, c.contractor_name ASC");
            else if ("date".equalsIgnoreCase(sort)) sql.append("ORDER BY last_service DESC, c.contractor_name ASC");
            else if ("relation".equalsIgnoreCase(sort)) sql.append("ORDER BY COALESCE(c.relation_start_date, DATE(c.created_at)) ASC, c.contractor_name ASC");
            else sql.append("ORDER BY c.contractor_name ASC");
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                for (int i=0;i<params.size();i++) ps.setString(i+1,params.get(i));
                try (ResultSet r=ps.executeQuery()) {
                    boolean first=true;
                    while(r.next()) {
                        if(!first) json.append(','); first=false;
                        json.append('{')
                                .append("\"contractorId\":").append(r.getInt("contractor_id")).append(',')
                                .append("\"contractorName\":\"").append(e(r.getString("contractor_name"))).append("\",")
                                .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                                .append("\"address\":\"").append(e(r.getString("address"))).append("\",")
                                .append("\"purpose\":\"").append(e(r.getString("purpose"))).append("\",")
                                .append("\"relationStartDate\":\"").append(e(String.valueOf(r.getDate("relation_start_date") != null ? r.getDate("relation_start_date") : new java.sql.Date(r.getTimestamp("created_at").getTime())))).append("\",")
                                .append("\"relationYears\":").append(relationYears(r.getDate("relation_start_date"), r.getTimestamp("created_at"))).append(',')
                                .append("\"relationRange\":\"").append(e(relationRange(r.getDate("relation_start_date"), r.getTimestamp("created_at")))).append("\",")
                                .append("\"serviceCount\":").append(r.getInt("service_count")).append(',')
                                .append("\"totalAmount\":").append(r.getDouble("total_amount")).append(',')
                                .append("\"lastService\":\"").append(e(String.valueOf(r.getTimestamp("last_service")))).append("\"}");
                    }
                }
            }
        } catch(Exception ex){return err(ex.getMessage());}
        return json.append("]}").toString();
    }

    public String addContractor(String json) {
        String name = getString(json, "contractorName");
        String phone = getString(json, "phone");
        String address = getString(json, "address");
        String purpose = getString(json, "purpose");
        String relationStartDate = getString(json, "relationStartDate");
        if (name.isBlank() || phone.isBlank()) return err("Contractor name and phone are required.");
        if (relationStartDate.isBlank()) relationStartDate = LocalDate.now().toString();
        try (Connection c = DatabaseConfig.getConnection()) {
            try (PreparedStatement check = c.prepareStatement("SELECT contractor_id FROM contractors WHERE phone=? LIMIT 1")) {
                check.setString(1, phone);
                try (ResultSet r = check.executeQuery()) { if (r.next()) return err("A contractor with this phone number already exists."); }
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO contractors(contractor_name,phone,address,purpose,relation_start_date) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name); ps.setString(2, phone); ps.setString(3, address); ps.setString(4, purpose); ps.setString(5, relationStartDate); ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { int id = k.next() ? k.getInt(1) : 0; return "{\"ok\":true,\"contractorId\":" + id + "}"; }
            }
        } catch (Exception ex) { return err(ex.getMessage()); }
    }

    public String getContractorDetails(int id) {
        try (Connection c = DatabaseConfig.getConnection()) {
            StringBuilder json = new StringBuilder("{\"ok\":true,\"contractor\":");
            try (PreparedStatement ps=c.prepareStatement("SELECT contractor_id,contractor_name,phone,address,purpose,relation_start_date,created_at FROM contractors WHERE contractor_id=?")) {
                ps.setInt(1,id);
                try(ResultSet r=ps.executeQuery()) {
                    if(!r.next()) return err("Contractor not found.");
                    json.append('{').append("\"contractorId\":").append(r.getInt(1)).append(',')
                            .append("\"contractorName\":\"").append(e(r.getString(2))).append("\",")
                            .append("\"phone\":\"").append(e(r.getString(3))).append("\",")
                            .append("\"address\":\"").append(e(r.getString(4))).append("\",")
                            .append("\"purpose\":\"").append(e(r.getString(5))).append("\",")
                            .append("\"relationStartDate\":\"").append(e(String.valueOf(r.getDate(6) != null ? r.getDate(6) : new java.sql.Date(r.getTimestamp(7).getTime())))).append("\",")
                            .append("\"relationYears\":").append(relationYears(r.getDate(6), r.getTimestamp(7))).append(',')
                            .append("\"relationRange\":\"").append(e(relationRange(r.getDate(6), r.getTimestamp(7)))).append("\"},\"sales\":[");
                }
            }
            try(PreparedStatement ps=c.prepareStatement("SELECT sale_id,sale_date,total_workers,total_amount,purpose FROM contractor_sales WHERE contractor_id=? ORDER BY sale_date DESC,sale_id DESC")) {
                ps.setInt(1,id); try(ResultSet r=ps.executeQuery()) { boolean first=true; while(r.next()){ if(!first)json.append(',');first=false; json.append('{').append("\"saleId\":").append(r.getInt(1)).append(',').append("\"saleDate\":\"").append(e(String.valueOf(r.getTimestamp(2)))).append("\",").append("\"totalWorkers\":").append(r.getInt(3)).append(',').append("\"totalAmount\":").append(r.getDouble(4)).append(',').append("\"purpose\":\"").append(e(r.getString(5))).append("\"}"); } }
            }
            return json.append("]}").toString();
        } catch(Exception ex){return err(ex.getMessage());}
    }

    public String getContractorSalesHistory(String date, String search) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"history\":[");
        try(Connection c=DatabaseConfig.getConnection()) {
            StringBuilder sql=new StringBuilder("SELECT DATE(cs.sale_date) sale_day,c.contractor_id,c.contractor_name,c.phone,COUNT(*) service_count,SUM(cs.total_amount) total_amount,MAX(cs.sale_date) last_time FROM contractor_sales cs JOIN contractors c ON c.contractor_id=cs.contractor_id WHERE 1=1 ");
            List<String> params=new ArrayList<>();
            if(date!=null&&!date.isBlank()){sql.append("AND DATE(cs.sale_date)=? ");params.add(date);}
            if(search!=null&&!search.isBlank()){sql.append("AND (c.contractor_name LIKE ? OR c.phone LIKE ?) ");params.add("%"+search+"%");params.add("%"+search+"%");}
            sql.append("GROUP BY DATE(cs.sale_date),c.contractor_id,c.contractor_name,c.phone ORDER BY sale_day DESC,c.contractor_name ASC");
            try(PreparedStatement ps=c.prepareStatement(sql.toString())){for(int i=0;i<params.size();i++)ps.setString(i+1,params.get(i));try(ResultSet r=ps.executeQuery()){boolean first=true;while(r.next()){if(!first)json.append(',');first=false;json.append('{').append("\"date\":\"").append(e(String.valueOf(r.getDate("sale_day")))).append("\",").append("\"contractorId\":").append(r.getInt("contractor_id")).append(',').append("\"contractorName\":\"").append(e(r.getString("contractor_name"))).append("\",").append("\"phone\":\"").append(e(r.getString("phone"))).append("\",").append("\"serviceCount\":").append(r.getInt("service_count")).append(',').append("\"totalAmount\":").append(r.getDouble("total_amount")).append("}");}}}
        }catch(Exception ex){return err(ex.getMessage());}
        return json.append("]}").toString();
    }

    public String getContractorSaleDetails(int contractorId, String date) {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"sales\":[");
        try (Connection c = DatabaseConfig.getConnection()) {
            String sql = "SELECT cs.sale_id,cs.sale_date,cs.total_workers,cs.total_amount,cs.purpose," +
                    "c.contractor_name,c.phone,c.address FROM contractor_sales cs " +
                    "JOIN contractors c ON c.contractor_id=cs.contractor_id " +
                    "WHERE cs.contractor_id=? AND DATE(cs.sale_date)=? " +
                    "ORDER BY cs.sale_id DESC";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contractorId);
                ps.setString(2, date);
                try (ResultSet r = ps.executeQuery()) {
                    boolean first = true;
                    while (r.next()) {
                        if (!first) json.append(',');
                        first = false;
                        int saleId = r.getInt("sale_id");
                        json.append('{')
                                .append("\"saleId\":").append(saleId).append(',')
                                .append("\"saleDate\":\"").append(e(String.valueOf(r.getTimestamp("sale_date")))).append("\",")
                                .append("\"totalWorkers\":").append(r.getInt("total_workers")).append(',')
                                .append("\"totalAmount\":").append(r.getDouble("total_amount")).append(',')
                                .append("\"purpose\":\"").append(e(r.getString("purpose"))).append("\",")
                                .append("\"contractorName\":\"").append(e(r.getString("contractor_name"))).append("\",")
                                .append("\"phone\":\"").append(e(r.getString("phone"))).append("\",")
                                .append("\"address\":\"").append(e(r.getString("address"))).append("\",\"workers\":[");

                        String itemSql = "SELECT worker_id,worker_name,work_type,salary,assignment_status,completed_at " +
                                "FROM contractor_sale_workers WHERE sale_id=? ORDER BY assignment_id";
                        try (PreparedStatement iw = c.prepareStatement(itemSql)) {
                            iw.setInt(1, saleId);
                            try (ResultSet w = iw.executeQuery()) {
                                boolean wf = true;
                                while (w.next()) {
                                    if (!wf) json.append(',');
                                    wf = false;
                                    json.append('{')
                                            .append("\"workerId\":").append(w.getInt("worker_id")).append(',')
                                            .append("\"workerName\":\"").append(e(w.getString("worker_name"))).append("\",")
                                            .append("\"workType\":\"").append(e(w.getString("work_type"))).append("\",")
                                            .append("\"salary\":").append(w.getDouble("salary")).append(',')
                                            .append("\"status\":\"").append(e(w.getString("assignment_status"))).append("\",")
                                            .append("\"completedAt\":\"").append(e(String.valueOf(w.getTimestamp("completed_at")))).append("\"}");
                                }
                            }
                        }
                        json.append("]}");
                    }
                }
            }
        } catch (Exception ex) {
            return err(ex.getMessage());
        }
        return json.append("]}").toString();
    }

    public String createContractorSale(String json) {
        int contractorIdInput=getInt(json,"contractorId");
        String name=getString(json,"contractorName"), phone=getString(json,"phone"), address=getString(json,"address"), purpose=getString(json,"purpose");
        String workersJson=getArray(json,"workers");
        if(contractorIdInput<=0 && (name.isBlank()||phone.isBlank())) return err("Contractor name, phone and at least one worker are required.");
        if(workersJson.isBlank()) return err("Contractor and at least one worker are required.");
        String[] workers=splitObjects(workersJson); if(workers.length==0)return err("Add at least one worker.");
        try(Connection c=DatabaseConfig.getConnection()){
            c.setAutoCommit(false);
            try{
                int contractorId=contractorIdInput;
                if(contractorId>0){
                    try(PreparedStatement ps=c.prepareStatement("SELECT contractor_name,phone,address,purpose FROM contractors WHERE contractor_id=?")){
                        ps.setInt(1,contractorId); try(ResultSet r=ps.executeQuery()){
                            if(!r.next()) throw new Exception("Contractor not found.");
                            name=r.getString(1); phone=r.getString(2); address=r.getString(3); purpose=r.getString(4);
                        }
                    }
                } else {
                    try(PreparedStatement ps=c.prepareStatement("SELECT contractor_id FROM contractors WHERE phone=? LIMIT 1")){ps.setString(1,phone);try(ResultSet r=ps.executeQuery()){if(r.next())contractorId=r.getInt(1);}}
                    if(contractorId==0){try(PreparedStatement ps=c.prepareStatement("INSERT INTO contractors(contractor_name,phone,address,purpose,relation_start_date) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setString(1,name);ps.setString(2,phone);ps.setString(3,address);ps.setString(4,purpose);ps.setString(5,LocalDate.now().toString());ps.executeUpdate();try(ResultSet k=ps.getGeneratedKeys()){if(k.next())contractorId=k.getInt(1);}}}
                    else {try(PreparedStatement ps=c.prepareStatement("UPDATE contractors SET contractor_name=?,address=?,purpose=? WHERE contractor_id=?")){ps.setString(1,name);ps.setString(2,address);ps.setString(3,purpose);ps.setInt(4,contractorId);ps.executeUpdate();}}
                }
                Set<Integer> seen=new HashSet<>(); double total=0;
                List<Integer> workerIds=new ArrayList<>(); List<String> names=new ArrayList<>(); List<String> types=new ArrayList<>(); List<Double> salaries=new ArrayList<>();
                for(String wj:workers){int workerId=getInt(wj,"workerId");if(workerId<=0||!seen.add(workerId))throw new Exception("Select a valid unique worker for every row.");
                    try(PreparedStatement ps=c.prepareStatement("SELECT worker_name,work_type,salary,active FROM workers WHERE worker_id=?")){ps.setInt(1,workerId);try(ResultSet r=ps.executeQuery()){if(!r.next())throw new Exception("Worker not found: "+workerId);if(!r.getBoolean("active"))throw new Exception("Selected worker is inactive.");names.add(r.getString(1));types.add(r.getString(2));salaries.add(r.getDouble(3));workerIds.add(workerId);total+=r.getDouble(3);}}
                    try(PreparedStatement ps=c.prepareStatement("SELECT assignment_id FROM contractor_sale_workers WHERE worker_id=? AND assignment_status='IN_PROGRESS' LIMIT 1")){ps.setInt(1,workerId);try(ResultSet r=ps.executeQuery()){if(r.next())throw new Exception("Worker "+workerId+" is already assigned to another job.");}}
                }
                int saleId=0;try(PreparedStatement ps=c.prepareStatement("INSERT INTO contractor_sales(contractor_id,total_workers,total_amount,purpose) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){ps.setInt(1,contractorId);ps.setInt(2,workers.length);ps.setDouble(3,total);ps.setString(4,purpose);ps.executeUpdate();try(ResultSet k=ps.getGeneratedKeys()){if(k.next())saleId=k.getInt(1);}}
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO contractor_sale_workers(sale_id,worker_id,worker_name,work_type,salary,assignment_status) VALUES(?,?,?,?,?,'IN_PROGRESS')")){for(int i=0;i<workerIds.size();i++){ps.setInt(1,saleId);ps.setInt(2,workerIds.get(i));ps.setString(3,names.get(i));ps.setString(4,types.get(i));ps.setDouble(5,salaries.get(i));ps.addBatch();}ps.executeBatch();}
                c.commit(); return "{\"ok\":true,\"saleId\":"+saleId+",\"totalAmount\":"+total+"}";
            }catch(Exception ex){c.rollback();return err(ex.getMessage());}finally{c.setAutoCommit(true);}
        }catch(Exception ex){return err(ex.getMessage());}
    }

    private static String searchOrWhere(StringBuilder sql) {
        return sql.indexOf("WHERE") >= 0 ? "AND " : "WHERE ";
    }

    private static LocalDate relationStart(java.sql.Date relationStartDate, Timestamp createdAt) {
        if (relationStartDate != null) return relationStartDate.toLocalDate();
        if (createdAt != null) return createdAt.toLocalDateTime().toLocalDate();
        return LocalDate.now();
    }

    private static long relationYears(java.sql.Date relationStartDate, Timestamp createdAt) {
        LocalDate start = relationStart(relationStartDate, createdAt);
        return java.time.Period.between(start, LocalDate.now()).getYears();
    }

    private static String relationRange(java.sql.Date relationStartDate, Timestamp createdAt) {
        LocalDate start = relationStart(relationStartDate, createdAt);
        java.time.Period p = java.time.Period.between(start, LocalDate.now());
        int years = p.getYears();
        int months = p.getMonths();
        if (years <= 0) return months + (months == 1 ? " month" : " months");
        return years + (years == 1 ? " year" : " years") + (months > 0 ? " " + months + " month" + (months == 1 ? "" : "s") : "");
    }

    private static String getString(String json,String key){String marker="\""+key+"\"";int p=json.indexOf(marker);if(p<0)return "";p=json.indexOf(':',p+marker.length());if(p<0)return "";p++;while(p<json.length()&&Character.isWhitespace(json.charAt(p)))p++;if(p>=json.length()||json.charAt(p)!='\"')return "";p++;StringBuilder s=new StringBuilder();boolean esc=false;for(;p<json.length();p++){char ch=json.charAt(p);if(esc){s.append(ch);esc=false;}else if(ch=='\\')esc=true;else if(ch=='\"')break;else s.append(ch);}return s.toString();}
    private static int getInt(String json,String key){String v=getNumber(json,key);try{return Integer.parseInt(v);}catch(Exception e){return 0;}}
    private static double getDouble(String json,String key){String v=getNumber(json,key);try{return Double.parseDouble(v);}catch(Exception e){return 0;}}
    private static String getNumber(String json,String key){String marker="\""+key+"\"";int p=json.indexOf(marker);if(p<0)return "0";p=json.indexOf(':',p+marker.length());if(p<0)return "0";p++;while(p<json.length()&&Character.isWhitespace(json.charAt(p)))p++;int s=p;while(p<json.length()&&"-+.0123456789".indexOf(json.charAt(p))>=0)p++;return json.substring(s,p);}
    private static boolean getBoolean(String json,String key,boolean fallback){String marker="\""+key+"\"";int p=json.indexOf(marker);if(p<0)return fallback;p=json.indexOf(':',p+marker.length());if(p<0)return fallback;String tail=json.substring(p+1).trim();return tail.startsWith("true")||(!tail.startsWith("false")&&fallback);}
    private static String getArray(String json,String key){String marker="\""+key+"\"";int p=json.indexOf(marker);if(p<0)return "";p=json.indexOf('[',p+marker.length());if(p<0)return "";int end=findClosing(json,p,'[',']');return end<0?"":json.substring(p+1,end);}
    private static int findClosing(String text,int start,char open,char close){int depth=0;boolean inside=false,esc=false;for(int i=start;i<text.length();i++){char c=text.charAt(i);if(esc){esc=false;continue;}if(c=='\\'&&inside){esc=true;continue;}if(c=='\"'){inside=!inside;continue;}if(inside)continue;if(c==open)depth++;else if(c==close&&--depth==0)return i;}return -1;}
    private static String[] splitObjects(String content){List<String> out=new ArrayList<>();int start=-1,depth=0;boolean inside=false,esc=false;for(int i=0;i<content.length();i++){char c=content.charAt(i);if(esc){esc=false;continue;}if(c=='\\'&&inside){esc=true;continue;}if(c=='\"'){inside=!inside;continue;}if(inside)continue;if(c=='{'){if(depth==0)start=i;depth++;}else if(c=='}'){depth--;if(depth==0&&start>=0){out.add(content.substring(start,i+1));start=-1;}}}return out.toArray(new String[0]);}
    private static String e(String s){if(s==null)return "";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");}
    private static String err(String s){return "{\"ok\":false,\"error\":\""+e(s==null?"Unknown error":s)+"\"}";}
}
