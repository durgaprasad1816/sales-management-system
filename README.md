# Hardware Management Application

Frontend: HTML + CSS + JavaScript
Backend: Core Java using Java's built-in HttpServer
Database: MySQL
Database access: JDBC

No Spring Boot, Servlets, or REST framework.

## Features
- Dashboard with ONE Profit & Loss graph
- Dashboard shows all products with stock; Show More/Show Less
- Products: add/search/delete with name, type, quantity, purchase price, sale price, discount
- Sales: product, quantity, automatic unit price and total price, direct/indirect customer, discount, GST, final price, printable invoice
- Purchase: supplier, product, type, quantity, purchase price, supplier PDF upload and PDF text extraction using PDFBox, purchase invoice
- Stock automatically decreases after sales and increases after purchases

## Setup
1. Install JDK 17+, MySQL 8+, Maven 3.9+.
2. Run database/schema.sql in MySQL.
3. Edit DatabaseConfig.java and set your MySQL username/password.
4. From backend run: mvn clean package
5. Run: mvn exec:java
6. Open http://localhost:8080

Important: the browser still communicates with Java over HTTP. This is not Spring Boot/Servlet/REST framework; it is Java's built-in HTTP server.

PDF note: text-based supplier PDFs can be extracted. Scanned/image PDFs need OCR and are not automatically parsed in this version.
