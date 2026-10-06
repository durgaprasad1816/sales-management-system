package com.hardware.app.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.hardware.app.service.ContractorService;
import com.hardware.app.service.CustomerService;
import com.hardware.app.service.DashboardService;
import com.hardware.app.service.ProductService;
import com.hardware.app.service.PurchaseService;
import com.hardware.app.service.SalesService;
import com.hardware.app.service.SupplierService;
import com.hardware.app.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class HardwareHttpServer {

    private final HttpServer server;

    private final ProductService productService;
    private final DashboardService dashboardService;
    private final PurchaseService purchaseService;
    private final SalesService salesService;
    private final SupplierService supplierService;
    private final CustomerService customerService;
    private final ContractorService contractorService;

    public HardwareHttpServer(int port) throws IOException {

        server = HttpServer.create(
                new InetSocketAddress(port),
                0
        );

        productService = new ProductService();
        dashboardService = new DashboardService();
        purchaseService = new PurchaseService();
        salesService = new SalesService();
        supplierService = new SupplierService();
        customerService = new CustomerService();
        contractorService = new ContractorService();

        createRoutes();
    }

    private void createRoutes() {

        // ==============================
        // FRONTEND
        // ==============================

        server.createContext(
                "/",
                this::handleFrontend
        );

        // ==============================
        // PRODUCTS
        // ==============================

        server.createContext(
                "/data/products",
                this::handleProducts
        );

        server.createContext(
                "/data/products/add",
                this::handleAddProduct
        );

        server.createContext(
                "/data/products/delete",
                this::handleDeleteProduct
        );

        // ==============================
        // DASHBOARD
        // ==============================

        server.createContext(
                "/data/dashboard",
                this::handleDashboard
        );

        // ==============================
        // SALES
        // ==============================

        server.createContext(
                "/data/sales/add",
                this::handleAddSale
        );

        server.createContext(
                "/data/sales/history",
                this::handleSalesHistory
        );

        server.createContext(
                "/data/sales/details",
                this::handleSaleDetails
        );

        server.createContext(
                "/data/sales/delete",
                this::handleDeleteSale
        );

        // ==============================
        // PURCHASE
        // ==============================

        server.createContext(
                "/data/purchases/add",
                this::handleAddPurchase
        );

        server.createContext(
                "/data/purchases/read-pdf",
                this::handleReadPdf
        );

        server.createContext(
                "/data/purchases/read-file",
                this::handleReadPurchaseFile
        );

        server.createContext(
                "/data/purchases/import",
                this::handleImportPurchase
        );

        // ==============================
        // SUPPLIERS
        // ==============================

        server.createContext(
                "/data/suppliers",
                this::handleSuppliers
        );

        server.createContext(
                "/data/suppliers/details",
                this::handleSupplierDetails
        );

        // ==============================
        // CUSTOMERS
        // ==============================

        server.createContext(
                "/data/customers",
                this::handleCustomers
        );

        server.createContext(
                "/data/customers/details",
                this::handleCustomerDetails
        );

        // ==============================
        // CONTRACTORS / WORKERS
        // ==============================

        server.createContext("/data/workers", this::handleWorkers);
        server.createContext("/data/workers/details", this::handleWorkerDetails);
        server.createContext("/data/workers/add", this::handleAddWorker);
        server.createContext("/data/workers/active", this::handleWorkerActive);
        server.createContext("/data/workers/complete", this::handleWorkerComplete);
        server.createContext("/data/workers/assign", this::handleWorkerAssign);
        server.createContext("/data/contractor-assignments", this::handleContractorAssignments);
        server.createContext("/data/contractor-assignments/complete", this::handleContractorAssignmentsComplete);
        server.createContext("/data/contractors", this::handleContractors);
        server.createContext("/data/contractors/add", this::handleAddContractor);
        server.createContext("/data/contractors/details", this::handleContractorDetails);
        server.createContext("/data/contractor-sales/add", this::handleAddContractorSale);
        server.createContext("/data/contractor-sales/history", this::handleContractorSalesHistory);
        server.createContext("/data/contractor-sales/details", this::handleContractorSaleDetails);
    }

    // =====================================================
    // FRONTEND
    // =====================================================

    private void handleFrontend(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        String path =
                exchange.getRequestURI()
                        .getPath();

        // Serve index.html
        if (path.equals("/")
                || path.equals("/index.html")) {

            String html =
                    HttpUtil.readFrontendFile(
                            "index.html"
                    );

            sendResponse(
                    exchange,
                    200,
                    "text/html; charset=UTF-8",
                    html
            );

            return;
        }

        // Serve CSS
        if (path.equals("/style.css")) {

            String css =
                    HttpUtil.readFrontendFile(
                            "style.css"
                    );

            sendResponse(
                    exchange,
                    200,
                    "text/css; charset=UTF-8",
                    css
            );

            return;
        }

        // Serve JavaScript
        if (path.equals("/app.js")) {

            String js =
                    HttpUtil.readFrontendFile(
                            "app.js"
                    );

            sendResponse(
                    exchange,
                    200,
                    "application/javascript; charset=UTF-8",
                    js
            );

            return;
        }

        sendJson(
                exchange,
                404,
                "{\"ok\":false,\"error\":\"Page Not Found\"}"
        );
    }

    // =====================================================
    // PRODUCTS
    // =====================================================

    private void handleProducts(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String json =
                    productService.getAllProductsJson();

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // ADD PRODUCT
    // =====================================================

    private void handleAddProduct(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String body =
                    readRequestBody(exchange);

            String result =
                    productService.addProductFromJson(
                            body
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // DELETE PRODUCT
    // =====================================================

    private void handleDeleteProduct(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String query =
                    exchange.getRequestURI()
                            .getQuery();

            int productId =
                    getQueryInt(
                            query,
                            "id"
                    );

            String result =
                    productService.deleteProduct(
                            productId
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // DASHBOARD
    // =====================================================

    private void handleDashboard(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String json =
                    dashboardService.getDashboardJson();

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // ADD SALE
    // =====================================================

    private void handleAddSale(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String body =
                    readRequestBody(exchange);

            String result =
                    salesService.createSaleFromJson(
                            body
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // SALES HISTORY
    // =====================================================

    private void handleSalesHistory(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String query =
                    exchange.getRequestURI()
                            .getQuery();

            String date =
                    getQueryParameter(
                            query,
                            "date"
                    );

            String result =
                    salesService.getSalesHistory(
                            date
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // SALE DETAILS
    // =====================================================

    private void handleSaleDetails(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String query =
                    exchange.getRequestURI()
                            .getQuery();

            int saleId =
                    getQueryInt(
                            query,
                            "id"
                    );

            String result =
                    salesService.getSaleDetails(
                            saleId
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // DELETE SALE
    // =====================================================

    private void handleDeleteSale(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String query =
                    exchange.getRequestURI()
                            .getQuery();

            int saleId =
                    getQueryInt(
                            query,
                            "id"
                    );

            String result =
                    salesService.deleteSale(
                            saleId
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // GET QUERY PARAMETER
    // =====================================================

    private String getQueryParameter(
            String query,
            String parameter
    ) {

        if (query == null
                || query.isBlank()) {

            return null;
        }

        String[] parameters =
                query.split("&");

        for (String item : parameters) {

            String[] pair =
                    item.split("=", 2);

            if (pair.length == 2
                    && pair[0].equals(parameter)) {

                try {
                    return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    return pair[1];
                }
            }
        }

        return null;
    }

    // =====================================================
    // ADD PURCHASE
    // =====================================================

    private void handleAddPurchase(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String body =
                    readRequestBody(exchange);

            String result =
                    purchaseService.createPurchaseFromJson(
                            body
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // READ SUPPLIER PDF
    // =====================================================

    private void handleReadPdf(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String result =
                    purchaseService.readSupplierPdf(
                            exchange
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // READ GENERIC PURCHASE FILE
    // =====================================================

    private void handleReadPurchaseFile(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String result =
                    purchaseService.readSupplierFile(
                            exchange
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // IMPORT PURCHASE
    // =====================================================

    private void handleImportPurchase(
            HttpExchange exchange
    ) throws IOException {

        // CORS preflight
        if (handleCorsPreflight(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("POST")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String body =
                    readRequestBody(exchange);

            String result =
                    purchaseService.importPurchaseFromJson(
                            body
                    );

            sendJson(
                    exchange,
                    200,
                    result
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // SUPPLIERS
    // =====================================================

    private void handleSuppliers(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String q =
                    exchange.getRequestURI()
                            .getQuery();

            String search =
                    getQueryParameter(
                            q,
                            "search"
                    );

            sendJson(
                    exchange,
                    200,
                    supplierService.getSuppliers(
                            search
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // SUPPLIER DETAILS
    // =====================================================

    private void handleSupplierDetails(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            int id =
                    getQueryInt(
                            exchange.getRequestURI()
                                    .getQuery(),
                            "id"
                    );

            sendJson(
                    exchange,
                    200,
                    supplierService.getSupplierDetails(
                            id
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // CUSTOMERS
    // =====================================================

    private void handleCustomers(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            String q =
                    exchange.getRequestURI()
                            .getQuery();

            String search =
                    getQueryParameter(
                            q,
                            "search"
                    );

            String phone =
                    getQueryParameter(
                            q,
                            "phone"
                    );

            sendJson(
                    exchange,
                    200,
                    customerService.getCustomers(
                            search,
                            phone
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // CUSTOMER DETAILS
    // =====================================================

    private void handleCustomerDetails(
            HttpExchange exchange
    ) throws IOException {

        if (!exchange.getRequestMethod()
                .equalsIgnoreCase("GET")) {

            sendJson(
                    exchange,
                    405,
                    "{\"ok\":false,\"error\":\"Method Not Allowed\"}"
            );

            return;
        }

        try {

            int id =
                    getQueryInt(
                            exchange.getRequestURI()
                                    .getQuery(),
                            "id"
                    );

            sendJson(
                    exchange,
                    200,
                    customerService.getCustomerDetails(
                            id
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    errorJson(e)
            );
        }
    }

    // =====================================================
    // CONTRACTORS / WORKERS
    // =====================================================

    private void handleWorkers(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendJson(exchange, 405, "{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return;
        }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.getWorkers(getQueryParameter(q,"search"),getQueryParameter(q,"workType"),getQueryParameter(q,"status"),getQueryParameter(q,"sort"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleWorkerDetails(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.getWorkerDetails(getQueryInt(exchange.getRequestURI().getQuery(),"id"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleAddWorker(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.addWorkerFromJson(readRequestBody(exchange))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleWorkerActive(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.updateWorkerActive(getQueryInt(q,"id"),"true".equalsIgnoreCase(getQueryParameter(q,"active")))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleWorkerComplete(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.completeWorkerAssignment(getQueryInt(exchange.getRequestURI().getQuery(),"id"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleWorkerAssign(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.createContractorSale(readRequestBody(exchange))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleContractorAssignments(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.getInProgressAssignments(getQueryParameter(q,"search"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleContractorAssignmentsComplete(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.completeContractorAssignments(readRequestBody(exchange))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

private void handleAddContractor(HttpExchange exchange) throws IOException {

    // CORS preflight
    if (handleCorsPreflight(exchange)) {
        return;
    }

    // Only POST is allowed
    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {

        sendJson(
                exchange,
                405,
                "{\"ok\":false,\"error\":\"Method not allowed.\"}"
        );

        return;
    }

    try {

        String body = readRequestBody(exchange);

        String result =
                contractorService.addContractor(body);

        sendJson(
                exchange,
                200,
                result
        );

    } catch (Exception e) {

        e.printStackTrace();

        sendJson(
                exchange,
                500,
                errorJson(e)
        );
    }
}
    private void handleContractors(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.getContractors(getQueryParameter(q,"search"),getQueryParameter(q,"sort"),getQueryParameter(q,"range"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleContractorDetails(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.getContractorDetails(getQueryInt(exchange.getRequestURI().getQuery(),"id"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleAddContractorSale(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) return;
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { sendJson(exchange,200,contractorService.createContractorSale(readRequestBody(exchange))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleContractorSalesHistory(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.getContractorSalesHistory(getQueryParameter(q,"date"),getQueryParameter(q,"search"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    private void handleContractorSaleDetails(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange,405,"{\"ok\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try { String q=exchange.getRequestURI().getQuery(); sendJson(exchange,200,contractorService.getContractorSaleDetails(getQueryInt(q,"contractorId"),getQueryParameter(q,"date"))); }
        catch(Exception e){e.printStackTrace();sendJson(exchange,500,errorJson(e));}
    }

    // =====================================================
    // CORS PREFLIGHT
    // =====================================================

    private boolean handleCorsPreflight(
            HttpExchange exchange
    ) throws IOException {

        if ("OPTIONS".equalsIgnoreCase(
                exchange.getRequestMethod()
        )) {

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Origin",
                    "*"
            );

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Methods",
                    "GET, POST, OPTIONS"
            );

            exchange.getResponseHeaders().set(
                    "Access-Control-Allow-Headers",
                    "Content-Type"
            );

            exchange.sendResponseHeaders(
                    204,
                    -1
            );

            exchange.close();

            return true;
        }

        return false;
    }

    // =====================================================
    // READ REQUEST BODY
    // =====================================================

    private String readRequestBody(
            HttpExchange exchange
    ) throws IOException {

        return new String(
                exchange.getRequestBody()
                        .readAllBytes(),
                StandardCharsets.UTF_8
        );
    }

    // =====================================================
    // GET QUERY INTEGER
    // =====================================================

    private int getQueryInt(
            String query,
            String parameter
    ) {

        if (query == null
                || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Missing query parameter: "
                            + parameter
            );
        }

        String[] parameters =
                query.split("&");

        for (String item : parameters) {

            String[] pair =
                    item.split("=", 2);

            if (pair.length == 2
                    && pair[0].equals(parameter)) {

                return Integer.parseInt(
                        pair[1]
                );
            }
        }

        throw new IllegalArgumentException(
                "Missing query parameter: "
                        + parameter
        );
    }

    // =====================================================
    // JSON RESPONSE
    // =====================================================

    private void sendJson(
            HttpExchange exchange,
            int status,
            String json
    ) throws IOException {

        sendResponse(
                exchange,
                status,
                "application/json; charset=UTF-8",
                json
        );
    }

    // =====================================================
    // NORMAL RESPONSE
    // =====================================================

    private void sendResponse(
            HttpExchange exchange,
            int status,
            String contentType,
            String response
    ) throws IOException {

        byte[] bytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Origin",
                        "*"
                );

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Methods",
                        "GET, POST, OPTIONS"
                );

        exchange.getResponseHeaders()
                .set(
                        "Access-Control-Allow-Headers",
                        "Content-Type"
                );

        exchange.sendResponseHeaders(
                status,
                bytes.length
        );

        try {

            exchange.getResponseBody()
                    .write(bytes);

        } finally {

            exchange.close();
        }
    }

    // =====================================================
    // ERROR JSON
    // =====================================================

    private String errorJson(
            Exception e
    ) {

        String message =
                e.getMessage();

        if (message == null) {

            message =
                    e.getClass()
                            .getSimpleName();
        }

        message =
                message
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");

        return
                "{\"ok\":false,\"error\":\""
                        + message
                        + "\"}";
    }

    // =====================================================
    // START SERVER
    // =====================================================

    public void start() {

        server.start();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "Hardware Management Server Started"
        );

        System.out.println(
                "URL: http://localhost:8080"
        );

        System.out.println(
                "======================================"
        );
    }

    // =====================================================
    // STOP SERVER
    // =====================================================

    public void stop() {

        server.stop(0);

        System.out.println(
                "Hardware Management Server Stopped."
        );
    }
}