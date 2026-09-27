
/* =========================================================
HARDWARE MANAGEMENT SYSTEM
Frontend JavaScript
HTML + CSS + Java HttpServer + JDBC + MySQL
========================================================= */
let allProducts = [];
let dashboardProducts = [];
let saleItems = [];
let showAllDashboardProducts = false;
let notificationTimer = null;
/* PDF PURCHASE IMPORT STATE */
let pdfPurchaseData = null;
let pdfReviewProducts = [];
let pdfImportInProgress = false;
let pdfImportExistingProducts = [];
let pdfImportNewProducts = [];
let pdfImportSelectedProducts = [];
/* =========================================================
PAGE INFORMATION
========================================================= */
const pageInformation = {
dashboard: {
title: "Dashboard",
description: "Overview of your hardware business"
},
products: {
title: "Products",
description: "Manage your hardware inventory"
},
sales: {
title: "New Sale",
description: "Create a customer sales invoice"
},
purchase: {
title: "Purchase",
description: "Purchase products from suppliers"
},
supplier: {
title: "Suppliers",
description: "Supplier directory and purchase history"
},
customer: {
title: "Customers",
description: "Customer directory and sales history"
}
};
/* =========================================================
PAGE NAVIGATION
========================================================= */
function showPage(pageName, clickedButton = null) {
document.querySelectorAll(".page").forEach(page => {
page.classList.remove("active-page");
});
const selectedPage =
document.getElementById(pageName + "Page");
if (selectedPage) {
selectedPage.classList.add("active-page");
}
document.querySelectorAll(".nav-item").forEach(button => {
button.classList.remove("active");
});
if (clickedButton) {
clickedButton.classList.add("active");
}
if (pageInformation[pageName]) {
const title = document.getElementById("pageTitle");
const description = document.getElementById("pageDescription");
if (title) {
title.textContent =
pageInformation[pageName].title;
}
if (description) {
description.textContent =
pageInformation[pageName].description;
}
}
if (pageName === "dashboard") {
loadDashboard();
}
if (pageName === "products") {
loadProducts();
}
if (pageName === "sales") {
prepareSalesPage();
}
if (pageName === "supplier") {
loadSuppliers();
}
if (pageName === "customer") {
loadCustomers();
}
}
function showPageByName(pageName) {
showPage(pageName);
}
/* =========================================================
DATE
========================================================= */

function updateDate() {
const dateElement =
document.getElementById("currentDate");
if (!dateElement) {
return;
}
const today = new Date();
dateElement.textContent =
today.toLocaleDateString("en-IN", {
weekday: "short",
day: "2-digit",
month: "short",
year: "numeric"
});
}
/* =========================================================
API REQUEST
========================================================= */
async function apiRequest(url, options = {}) {

    // Render Java backend
    const API_BASE_URL =
        "https://sales-management-system-rs5b.onrender.com";

    try {

        // If the URL is already absolute, use it directly.
        // Otherwise, connect it to the Render backend.
        const requestUrl =
            url.startsWith("http://") ||
            url.startsWith("https://")
                ? url
                : API_BASE_URL + url;

        const response =
            await fetch(requestUrl, options);

        if (!response.ok) {
            throw new Error(
                "HTTP Error " + response.status
            );
        }

        const contentType =
            response.headers.get("content-type");

        if (
            contentType &&
            contentType.includes("application/json")
        ) {
            return await response.json();
        }

        return await response.text();

    } catch (error) {

        console.error(
            "API Error:",
            error
        );

        showNotification(
            "Backend connection failed. Please try again.",
            "error"
        );

        throw error;
    }
}
/* =========================================================
NOTIFICATION
========================================================= */
function showNotification(
message,
type = "success"
) {
const notification =
document.getElementById("notification");
const messageElement =
document.getElementById("notificationMessage");
if (!notification || !messageElement) {
alert(message);
return;
}
messageElement.textContent = message;
if (type === "error") {
notification.style.background =
"#dc2626";
} else if (type === "warning") {
notification.style.background =
"#d97706";
} else {
notification.style.background =

"#111827";
}
notification.classList.add("show");
clearTimeout(notificationTimer);

notificationTimer =
setTimeout(() => {
notification.classList.remove("show");
}, 3500);
}
/* =========================================================
MONEY FORMAT
========================================================= */
function formatMoney(value) {
const number =
Number(value) || 0;
return number.toLocaleString(
"en-IN",
{
style: "currency",
currency: "INR",
minimumFractionDigits: 2
}
);
}
function formatShortMoney(value) {
value = Number(value) || 0;
if (value >= 10000000) {
return "n" +
(value / 10000000).toFixed(1) +
"Cr";
}
if (value >= 100000) {
return "n" +
(value / 100000).toFixed(1) +
"L";
}
if (value >= 1000) {
return "n" +
(value / 1000).toFixed(1) +
"K";
}
return "n" +
value.toFixed(0);
}
/* =========================================================
ESCAPE HTML
========================================================= */
function escapeHtml(value) {
if (
value === null ||
value === undefined
) {
return "";
}
return String(value)
.replace(/&/g, "&amp;")
.replace(/</g, "&lt;")
.replace(/>/g, "&gt;")
.replace(/"/g, "&quot;")
.replace(/'/g, "&#039;");
}
/* =========================================================
DASHBOARD
========================================================= */
async function loadDashboard() {
try {
const data =
await apiRequest(
"/data/dashboard"
);
if (!data || data.ok === false) {
throw new Error(
data?.error ||
"Dashboard loading failed"
);
}
const totalSales =

Number(data.totalSales || 0);
const totalPurchase =
Number(data.totalPurchase || 0);
const profit =
Number(

data.profit ??
(totalSales - totalPurchase)
);
const totalProducts =
Number(data.totalProducts || 0);
const salesElement =
document.getElementById("totalSales");
const purchaseElement =
document.getElementById("totalPurchase");
const profitElement =
document.getElementById("totalProfit");
const productsElement =
document.getElementById("totalProducts");
if (salesElement) {
salesElement.textContent =
formatMoney(totalSales);
}
if (purchaseElement) {
purchaseElement.textContent =
formatMoney(totalPurchase);
}
if (profitElement) {
profitElement.textContent =
formatMoney(profit);
}
if (productsElement) {
productsElement.textContent =
totalProducts;
}
if (Array.isArray(data.products)) {
dashboardProducts = data.products;
renderDashboardProducts(dashboardProducts);
}
drawProfitLossChart(
totalSales,
totalPurchase,
profit
);
} catch (error) {
console.error(
"Dashboard Error:",
error
);
}
}
/* =========================================================
DASHBOARD PRODUCTS
========================================================= */
function renderDashboardProducts(products = allProducts) {
const table =
document.getElementById(
"dashboardProducts"
);
if (!table) {
return;
}
const dashboardSort = document.getElementById("dashboardProductSort")?.value || "none";
const dashboardFilter = document.getElementById("dashboardProductFilter")?.value || "all";
let dashboardRows = [...products].filter(product => {
const stock = Number(product.quantity || 0);
return dashboardFilter === "all" ||
(dashboardFilter === "in" && stock > 10) ||
(dashboardFilter === "low" && stock > 0 && stock <= 10) ||
(dashboardFilter === "out" && stock <= 0);
});
if (dashboardSort !== "none") {
dashboardRows.sort((a, b) => {
if (dashboardSort === "name") return String(a.product_name || "").localeCompare(String(b.product_name || ""));
if (dashboardSort === "stock") return Number(b.quantity || 0) - Number(a.quantity || 0);
if (dashboardSort === "purchase") return Number(b.purchase_price || 0) - Number(a.purchase_price || 0);
if (dashboardSort === "sale") return Number(b.sale_price || 0) - Number(a.sale_price || 0);
return Number(a.product_id || 0) - Number(b.product_id || 0);
});
}
if (!dashboardRows.length) {
table.innerHTML = `
<tr>
<td colspan="6">
<div class="empty-state">
<div class="empty-state-icon">n</div>
<h3>No Products</h3>
<p>Add your first product.</p>
</div>
</td>
</tr>
`;
return;
}
const productsToShow =
showAllDashboardProducts
? dashboardRows
: dashboardRows.slice(0, 5);
table.innerHTML =
productsToShow

.map(product => {
const stock =
Number(
product.quantity || 0
);
return `

<tr>
<td>
<strong>
${escapeHtml(
product.product_name
)}
</strong>
</td>
<td>
${escapeHtml(
product.product_type || "-"
)}
</td>
<td>
${stock}
</td>
<td>
${formatMoney(
product.purchase_price
)}
</td>
<td>
${formatMoney(
product.sale_price
)}
</td>
<td>
${getStockStatus(stock)}
</td>
</tr>
`;
})
.join("");
const button =
document.getElementById(
"showProductsBtn"
);
if (button) {
button.textContent =
showAllDashboardProducts
? "Show Less"
: "Show More";
}
}
/* =========================================================
SHOW ALL PRODUCTS
========================================================= */
function toggleAllProducts() {
showAllDashboardProducts =
!showAllDashboardProducts;
renderDashboardProducts(dashboardProducts);
}
function resetDashboardProductControls() {
const sort = document.getElementById("dashboardProductSort");
const filter = document.getElementById("dashboardProductFilter");
if (sort) sort.value = "none";
if (filter) filter.value = "all";
renderDashboardProducts(dashboardProducts);
}
/* =========================================================
STOCK STATUS
========================================================= */
function getStockStatus(stock) {
stock =
Number(stock) || 0;
if (stock <= 0) {
return `
<span class="stock-status stock-out">
Out of Stock
</span>
`;
}
if (stock <= 10) {
return `
<span class="stock-status stock-low">
Low Stock
</span>
`;
}
return `
<span class="stock-status stock-good">
In Stock
</span>
`;

}
/* =========================================================
PROFIT & LOSS CHART
========================================================= */
function drawProfitLossChart(

sales,
purchase,
profit
) {
const canvas =
document.getElementById(
"profitLossChart"
);
if (!canvas) {
return;
}
const container =
canvas.parentElement;
const width =
container?.clientWidth || 500;
const height =
container?.clientHeight || 275;
const ratio =
window.devicePixelRatio || 1;
canvas.width =
width * ratio;
canvas.height =
height * ratio;
canvas.style.width =
width + "px";
canvas.style.height =
height + "px";
const ctx =
canvas.getContext("2d");
ctx.setTransform(
ratio,
0,
0,
ratio,
0,
0
);
ctx.clearRect(
0,
0,
width,
height
);
const values = [
Number(sales) || 0,
Number(purchase) || 0,
Math.max(Number(profit) || 0, 0)
];
const labels = [
"Sales",
"Purchase",
"Profit"
];
const maxValue =
Math.max(...values, 100);
const padding = 45;
const chartWidth =
width - padding * 2;
const chartHeight =
height - padding * 2;
/* Grid */
ctx.strokeStyle =
"#e5e7eb";
ctx.lineWidth = 1;
for (
let i = 0;
i <= 4;
i++
) {
const y =
padding +
(chartHeight / 4) * i;
ctx.beginPath();
ctx.moveTo(
padding,
y

);
ctx.lineTo(
width - padding,
y
);
ctx.stroke();

}
/* Bars */
const barWidth =
Math.min(
80,
chartWidth / 5
);
const positions = [
width * 0.25,
width * 0.50,
width * 0.75
];
values.forEach(
(value, index) => {
const barHeight =
(value / maxValue) *
chartHeight;
const x =
positions[index] -
barWidth / 2;
const y =
height -
padding -
barHeight;
if (index === 0) {
ctx.fillStyle =
"#2563eb";
} else if (index === 1) {
ctx.fillStyle =
"#f59e0b";
} else {
ctx.fillStyle =
"#16a34a";
}
ctx.fillRect(
x,
y,
barWidth,
barHeight
);
/* Value */
ctx.fillStyle =
"#374151";
ctx.font =
"600 11px Arial";
ctx.textAlign =
"center";
ctx.fillText(
formatShortMoney(value),
positions[index],
Math.max(y - 8, 15)
);
/* Label */
ctx.fillStyle =
"#6b7280";
ctx.font =
"12px Arial";
ctx.fillText(
labels[index],
positions[index],
height - 15
);
}
);
}
/* =========================================================
PRODUCTS
========================================================= */
async function loadProducts() {
const table =
document.getElementById(
"productsTable"
);
if (!table) {

return;
}
table.innerHTML = `
<tr>
<td colspan="8" class="loading">
Loading products...
</td>

</tr>
`;
try {
const data =
await apiRequest(
"/data/products"
);
if (!data || data.ok === false) {
throw new Error(
data?.error ||
"Could not load products"
);
}
allProducts =
Array.isArray(data.products)
? data.products
: [];
renderProductsTable();
if (typeof renderSaleItems === "function") {
renderSaleItems();
}
} catch (error) {
console.error(
"Products Error:",
error
);
table.innerHTML = `
<tr>
<td colspan="8">
<div class="empty-state">
Unable to load products.
</div>
</td>
</tr>
`;
}
}
/* =========================================================
RENDER PRODUCTS TABLE
========================================================= */
function renderProductsTable(
products = allProducts
) {
const table =
document.getElementById(
"productsTable"
);
if (!table) {
return;
}
if (!products.length) {
table.innerHTML = `
<tr>
<td colspan="8">
<div class="empty-state">
<div class="empty-state-icon">
n
</div>
<h3>
No Products Found
</h3>
<p>
Add a product to your inventory.
</p>
</div>
</td>
</tr>
`;
return;
}
table.innerHTML =
products
.map((product, index) => {
return `
<tr>
<td>
${index + 1}
</td>
<td>
<strong>
${escapeHtml(
product.product_name

)}
</strong>
</td>
<td>
${escapeHtml(
product.product_type || "-"
)}

</td>
<td>
${Number(
product.quantity || 0
)}
</td>
<td>
${formatMoney(
product.purchase_price
)}
</td>
<td>
${formatMoney(
product.sale_price
)}
</td>
<td>
${formatMoney(
product.discount
)}
</td>
<td>
<button
class="delete-btn"
onclick="deleteProduct(${product.product_id})">
Delete
</button>
</td>
</tr>
`;
})
.join("");
}
/* =========================================================
SEARCH PRODUCTS
========================================================= */
function searchProducts() {
    applyProductSortFilter();
}

function applyProductSortFilter() {
    const input = document.getElementById("productSearch");
    const search = String(input?.value || "").trim().toLowerCase();
    const filter = document.getElementById("productFilter")?.value || "all";
    const sort = document.getElementById("productSort")?.value || "none";

    let filtered = allProducts.filter(product => {
        const name = String(product.product_name || "").toLowerCase();
        const type = String(product.product_type || "").toLowerCase();
        const matchesSearch = !search || name.includes(search) || type.includes(search);
        const stock = Number(product.quantity || 0);
        const matchesFilter = filter === "all" ||
            (filter === "in" && stock > 10) ||
            (filter === "low" && stock > 0 && stock <= 10) ||
            (filter === "out" && stock <= 0);
        return matchesSearch && matchesFilter;
    });

    if (sort !== "none") {
        filtered.sort((a, b) => {
            if (sort === "name") return String(a.product_name || "").localeCompare(String(b.product_name || ""));
            if (sort === "stock") return Number(b.quantity || 0) - Number(a.quantity || 0);
            if (sort === "purchase") return Number(b.purchase_price || 0) - Number(a.purchase_price || 0);
            if (sort === "sale") return Number(b.sale_price || 0) - Number(a.sale_price || 0);
            if (sort === "discount") return Number(b.discount || 0) - Number(a.discount || 0);
            return Number(a.product_id || 0) - Number(b.product_id || 0);
        });
    }

    renderProductsTable(filtered);
}
function resetProductSortFilter() {
    const search = document.getElementById("productSearch");
    const sort = document.getElementById("productSort");
    const filter = document.getElementById("productFilter");
    if (search) search.value = "";
    if (sort) sort.value = "none";
    if (filter) filter.value = "all";
    applyProductSortFilter();
}

/* =========================================================
PRODUCT MODAL
========================================================= */
function resetProductForm() {
const form = document.getElementById("productForm");

if (form) {
form.reset();
}
// Restore the default quantity value after reset.
const quantity = document.getElementById("productQuantity");
if (quantity) {
quantity.value = "0";

}
}
function openProductModal() {
// Always open a fresh form. Cancelled/previous values must not remain.
resetProductForm();
const modal = document.getElementById("productModal");
if (modal) {
modal.classList.add("show");
}
}
function closeProductModal() {
// Cancel means discard everything that was entered.
resetProductForm();
const modal = document.getElementById("productModal");
if (modal) {
modal.classList.remove("show");
}
}
/* =========================================================
ADD PRODUCT
========================================================= */
async function addProduct(event) {
if (event) {
event.preventDefault();
}
const product = {
productName:
document.getElementById(
"productName"
)?.value.trim() || "",
productType:
document.getElementById(
"productType"
)?.value.trim() || "",
quantity:
Number(
document.getElementById(
"productQuantity"
)?.value
) || 0,
purchasePrice:
Number(
document.getElementById(
"productPurchasePrice"
)?.value
) || 0,
salePrice:
Number(
document.getElementById(
"productSalePrice"
)?.value
) || 0,
discount:
Number(
document.getElementById(
"productDiscount"
)?.value
) || 0
};
if (!product.productName) {
showNotification(
"Product name is required.",
"warning"
);
return;
}
if (product.quantity < 0) {
showNotification(
"Quantity cannot be negative.",
"warning"
);
return;
}
if (
product.purchasePrice < 0 ||
product.salePrice < 0 ||
product.discount < 0
) {

showNotification(
"Price and discount cannot be negative.",
"warning"
);
return;
}

// Prevent duplicate product names (case/space insensitive).
const normalizedName = product.productName.trim().toLowerCase();
const duplicateProduct = allProducts.find(
existing =>
String(existing.product_name || "")
.trim()
.toLowerCase() === normalizedName
);
if (duplicateProduct) {
showNotification(
`Product already exists: ${duplicateProduct.product_name}`,
"warning"
);
return;
}
try {
const result =
await apiRequest(
"/data/products/add",
{
method: "POST",
headers: {
"Content-Type":
"application/json"
},
body:
JSON.stringify(product)
}
);
if (!result || !result.ok) {
throw new Error(
result?.error ||
"Product could not be added."
);
}
showNotification(
"Product added successfully."
);
const form =
document.getElementById(
"productForm"
);
if (form) {
form.reset();
}
closeProductModal();
await loadProducts();
await loadDashboard();
} catch (error) {
console.error(
"Add Product Error:",
error
);
showNotification(
error?.message || "Product could not be added.",
"warning"
);
}
}
/* =========================================================
DELETE PRODUCT
========================================================= */
async function deleteProduct(productId) {
const confirmed =
confirm(
"Are you sure you want to delete this product?"
);
if (!confirmed) {
return;
}
try {
const result =
await apiRequest(
"/data/products/delete?id=" +
encodeURIComponent(productId),
{
method: "POST"
}

);
if (!result || !result.ok) {
throw new Error(
result?.error ||
"Product could not be deleted."

);
}
showNotification(
"Product deleted successfully."
);
await loadProducts();
renderSaleItems();
await loadDashboard();
} catch (error) {
console.error(
"Delete Product Error:",
error
);
showNotification(
error?.message || "Product could not be deleted.",
"warning"
);
}
}
/* =========================================================
SALES PAGE
========================================================= */
async function prepareSalesPage() {

// Clear customer details when opening a new sale.
const customerName = document.getElementById("customerName");
const customerPhone = document.getElementById("customerPhone");
const customerEmail = document.getElementById("customerEmail");
const customerGst = document.getElementById("customerGst");
const suggestionBox = document.getElementById("salesCustomerSuggestions");

if (customerName) customerName.value = "";
if (customerPhone) customerPhone.value = "";
if (customerEmail) customerEmail.value = "";
if (customerGst) customerGst.value = "";
if (suggestionBox) suggestionBox.innerHTML = "";

await loadProducts();
if (!saleItems.length) {
addSaleRow();
} else {
renderSaleItems();
}
await loadSalesHistory();
}
/* =========================================================
ADD SALE ROW
========================================================= */
function addSaleRow() {
const rowId =
Date.now() +
Math.random();
saleItems.push({
rowId,
productId: "",
quantity: 1,
unitPrice: 0,
discount: 0,
total: 0
});
renderSaleItems();
}
/* =========================================================
RENDER SALE ITEMS
========================================================= */
function renderSaleItems() {
const table =
document.getElementById(
"saleItems"
);
if (!table) {
return;
}
table.innerHTML =
saleItems
.map(item => {
return `
<tr data-row-id="${item.rowId}">
<td>
<select
class="sale-product-select"
onchange="selectSaleProduct('${item.rowId}', this.value)">
<option value="">
Select Product
</option>
${allProducts
.map(product => {
const selected =
String(
product.product_id

) ===
String(
item.productId
)
? "selected"
: "";
return `

<option
value="${product.product_id}"
${selected}>
${escapeHtml(
product.product_name
)}
</option>
`;
})
.join("")}
</select>
</td>
<td>
<span
id="available-${item.rowId}">
${getAvailableStock(
item.productId
)}
</span>
</td>
<td>
<input
type="number"
class="sale-quantity-input"
min="1"
value="${item.quantity}"
onchange="updateSaleQuantity('${item.rowId}', this.value)"
>
</td>
<td>
<span
id="unit-price-${item.rowId}">
${formatMoney(
item.unitPrice
)}
</span>
</td>
<td>
<span
id="discount-${item.rowId}">
${formatMoney(
item.discount
)}
</span>
</td>
<td>
<strong
id="sale-total-${item.rowId}">
${formatMoney(
item.total
)}
</strong>
</td>
<td>
<button
class="delete-btn"
onclick="removeSaleRow('${item.rowId}')">
Remove
</button>
</td>
</tr>
`;
})
.join("");
calculateSaleTotal();
}
/* =========================================================
SELECT SALE PRODUCT
========================================================= */
function selectSaleProduct(
rowId,
productId
) {
const item =

saleItems.find(
x =>
String(x.rowId) ===
String(rowId)
);
if (!item) {
return;

}
const product =
allProducts.find(
x =>
String(x.product_id) ===
String(productId)
);
item.productId =
productId;
if (product) {
item.unitPrice =
Number(
product.sale_price || 0
);
item.discount =
Number(
product.discount || 0
);
} else {
item.unitPrice = 0;
item.discount = 0;
}
item.quantity = 1;
renderSaleItems();
}
/* =========================================================
AVAILABLE STOCK
========================================================= */
function getAvailableStock(productId) {
if (!productId) {
return "-";
}
const product =
allProducts.find(
p =>
String(p.product_id) ===
String(productId)
);
if (!product) {
return "-";
}
return Number(
product.quantity || 0
);
}
/* =========================================================
UPDATE SALE QUANTITY
========================================================= */
function updateSaleQuantity(
rowId,
quantity
) {
const item =
saleItems.find(
x =>
String(x.rowId) ===
String(rowId)
);
if (!item) {
return;
}
quantity =
Math.max(
1,
Number(quantity) || 1
);
const available =
getAvailableStock(
item.productId
);
if (
available !== "-" &&
quantity > Number(available)
) {
showNotification(
"Only " +

available +
" units are available.",
"warning"
);
quantity =
Number(available);

if (quantity <= 0) {
quantity = 1;
}
}
item.quantity =
quantity;
updateSaleRowTotal(item);
calculateSaleTotal();
}
/* =========================================================
UPDATE SALE ROW TOTAL
========================================================= */
function updateSaleRowTotal(item) {
const gross =
Number(item.unitPrice || 0) *
Number(item.quantity || 0);
const discount =
Number(item.discount || 0) *
Number(item.quantity || 0);
item.total =
Math.max(
0,
gross - discount
);
const unitPriceElement =
document.getElementById(
"unit-price-" + item.rowId
);
const discountElement =
document.getElementById(
"discount-" + item.rowId
);
const totalElement =
document.getElementById(
"sale-total-" + item.rowId
);
if (unitPriceElement) {
unitPriceElement.textContent =
formatMoney(
item.unitPrice
);
}
if (discountElement) {
discountElement.textContent =
formatMoney(
item.discount
);
}
if (totalElement) {
totalElement.textContent =
formatMoney(
item.total
);
}
}
/* =========================================================
REMOVE SALE ROW
========================================================= */
function removeSaleRow(rowId) {
saleItems =
saleItems.filter(
item =>
String(item.rowId) !==
String(rowId)
);
if (!saleItems.length) {
addSaleRow();
} else {
renderSaleItems();
}
}
/* =========================================================
SALE TOTAL
========================================================= */

function calculateSaleTotal() {
let subtotal = 0;
let discount = 0;
saleItems.forEach(item => {

const gross =
Number(item.unitPrice || 0) *
Number(item.quantity || 0);
const itemDiscount =
Number(item.discount || 0) *
Number(item.quantity || 0);
subtotal += gross;
discount += itemDiscount;
item.total =
Math.max(
0,
gross - itemDiscount
);
});
const taxableAmount =
Math.max(
0,
subtotal - discount
);
/* 18% GST */
const gst =
taxableAmount * 0.18;
const finalTotal =
taxableAmount + gst;
const subtotalElement =
document.getElementById(
"saleSubtotal"
);
const discountElement =
document.getElementById(
"saleDiscount"
);
const gstElement =
document.getElementById(
"saleGST"
);
const finalElement =
document.getElementById(
"saleFinalTotal"
);
if (subtotalElement) {
subtotalElement.textContent =
formatMoney(subtotal);
}
if (discountElement) {
discountElement.textContent =
formatMoney(discount);
}
if (gstElement) {
gstElement.textContent =
formatMoney(gst);
}
if (finalElement) {
finalElement.textContent =
formatMoney(finalTotal);
}
saleItems.forEach(
updateSaleRowTotal
);
return {
subtotal,
discount,
gst,
finalTotal
};
}
/* =========================================================
SAVE SALE
========================================================= */
async function saveSale() {
const customerName =
document.getElementById(
"customerName"
)?.value.trim() || "";
const customerType =

document.getElementById(
"customerType"
)?.value || "DIRECT";
const customerPhone =
document.getElementById(
"customerPhone"

)?.value.trim() || "";
const customerEmail =
document.getElementById(
"customerEmail"
)?.value.trim() || "";
const customerGst =
document.getElementById(
"customerGst"
)?.value.trim() || "";
if (!customerName) {
showNotification(
"Enter customer name.",
"warning"
);
return;
}
const validItems =
saleItems.filter(
item =>
item.productId &&
Number(item.quantity) > 0
);
if (!validItems.length) {
showNotification(
"Add at least one product.",
"warning"
);
return;
}
/* Validate stock */
for (const item of validItems) {
const available =
Number(
getAvailableStock(
item.productId
)
);
if (
available <= 0 ||
Number(item.quantity) > available
) {
const product =
allProducts.find(
p =>
String(
p.product_id
) ===
String(
item.productId
)
);
showNotification(
"Insufficient stock for " +
(
product?.product_name ||
"product"
),
"warning"
);
return;
}
}
const totals =
calculateSaleTotal();
const saleData = {
customerName,
customerType,
customerPhone,
customerEmail,
customerGst,
subtotal:
totals.subtotal,
discount:
totals.discount,
gst:
totals.gst,
finalTotal:
totals.finalTotal,
items:
validItems.map(item => {
return {
productId:
Number(
item.productId
),
quantity:

Number(
item.quantity
),
unitPrice:
Number(
item.unitPrice

),
discount:
Number(
item.discount
),
total:
Number(
item.total
)
};
})
};
try {
const result =
await apiRequest(
"/data/sales/add",
{
method: "POST",
headers: {
"Content-Type":
"application/json"
},
body:
JSON.stringify(
saleData
)
}
);
if (!result || !result.ok) {
throw new Error(
result?.error ||
"Sale could not be created."
);
}
showNotification(
"Sale created successfully."
);
generateInvoiceFromSale(
saleData,
result
);
clearSaleForm();
await loadProducts();
await loadDashboard();
await loadSalesHistory();
} catch (error) {
console.error(
"Save Sale Error:",
error
);
}
}
/* =========================================================
CLEAR SALE
========================================================= */
function clearSaleForm() {
const customerName =
document.getElementById(
"customerName"
);
const customerPhone = document.getElementById("customerPhone");
const customerEmail = document.getElementById("customerEmail");
const customerGst = document.getElementById("customerGst");
if (customerName) customerName.value = "";
if (customerPhone) customerPhone.value = "";
if (customerEmail) customerEmail.value = "";
if (customerGst) customerGst.value = "";
const suggestionBox = document.getElementById("salesCustomerSuggestions");
if (suggestionBox) suggestionBox.innerHTML = "";
saleItems = [];
addSaleRow();
}
/* =========================================================
SALES HISTORY
========================================================= */
function ensureSalesHistorySection() {
    const salesPage = document.getElementById("salesPage");
    if (!salesPage) {
        return null;
    }

    let section = document.getElementById("salesHistorySection");
    if (!section) {
        section = document.createElement("section");
        section.id = "salesHistorySection";
        section.style.marginTop = "30px";
        salesPage.appendChild(section);
    }

    // Do not rebuild the section every time history is loaded.
    // Rebuilding it would clear the date selected by the user before
    // the request is made.
    if (document.getElementById("salesHistoryContent")) {
        return section;
    }

    section.innerHTML = `
        <div style="background:#fff;border:1px solid #e5e7eb;border-radius:14px;padding:24px;box-shadow:0 4px 14px rgba(0,0,0,.04);">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:15px;flex-wrap:wrap;margin-bottom:18px;">
                <div>
                    <h2 style="margin:0 0 5px;font-size:20px;">Sales History</h2>
                    <p style="margin:0;color:#6b7280;font-size:13px;">View, open and manage previously generated sales.</p>
                </div>
                <div style="display:flex;gap:10px;align-items:center;flex-wrap:wrap;">
                    <input id="salesHistorySearch" type="text" placeholder="Customer search..." oninput="renderSalesHistory(currentSalesHistoryRows)" style="padding:9px 12px;border:1px solid #d1d5db;border-radius:8px;min-width:180px;">
                    <select id="salesHistorySort" onchange="renderSalesHistory(currentSalesHistoryRows)" style="padding:9px 12px;border:1px solid #d1d5db;border-radius:8px;">
                        <option value="none">None</option>
                        <option value="date">Newest First</option>
                        <option value="customer">Customer Name</option>
                        <option value="amount">Amount High to Low</option>
                    </select>
                    <input id="salesHistoryDate" type="date" onchange="loadSalesHistory()" style="padding:9px 12px;border:1px solid #d1d5db;border-radius:8px;">
                    <button type="button" class="secondary-btn" onclick="clearSalesHistoryFilter();">Clear</button>
                </div>
            </div>
            <div id="salesHistoryContent">
                <div style="padding:25px;text-align:center;color:#6b7280;">Loading sales history...</div>
            </div>
        </div>
    `;

    return section;
}

let currentSalesHistoryRows = [];
function clearSalesHistoryFilter() {
    const input = document.getElementById("salesHistoryDate");
    if (input) input.value = "";
    const search = document.getElementById("salesHistorySearch");
    if (search) search.value = "";
    const sort = document.getElementById("salesHistorySort");
    if (sort) sort.value = "none";
    loadSalesHistory();
}

async function loadSalesHistory() {
    const section = ensureSalesHistorySection();
    if (!section) {
        return;
    }

    const content = document.getElementById("salesHistoryContent");
    if (!content) {
        return;
    }

    const dateInput = document.getElementById("salesHistoryDate");
    const date = dateInput?.value || "";

    content.innerHTML = `
        <div style="padding:25px;text-align:center;color:#6b7280;">Loading sales history...</div>
    `;

    try {
        const url = date
            ? "/data/sales/history?date=" + encodeURIComponent(date)
            : "/data/sales/history";

        const data = await apiRequest(url);

        if (!data || data.ok === false) {
            throw new Error(data?.error || "Could not load sales history.");
        }

        currentSalesHistoryRows = Array.isArray(data.sales) ? data.sales : [];
        renderSalesHistory(currentSalesHistoryRows);
    } catch (error) {
        console.error("Sales History Error:", error);
        content.innerHTML = `
            <div style="padding:25px;text-align:center;color:#dc2626;">
                Could not load sales history.
            </div>
        `;
    }
}

function renderSalesHistory(sales) {
    const content = document.getElementById("salesHistoryContent");
    const search = String(document.getElementById("salesHistorySearch")?.value || "").trim().toLowerCase();
    const sort = document.getElementById("salesHistorySort")?.value || "date";
    sales = [...(Array.isArray(sales) ? sales : [])].filter(sale => !search || String(sale.customerName || "").toLowerCase().includes(search) || String(sale.customerPhone || "").includes(search));
    if (sort !== "none") {
        sales.sort((a,b) => sort === "customer" ? String(a.customerName || "").localeCompare(String(b.customerName || "")) : sort === "amount" ? Number(b.finalTotal || 0) - Number(a.finalTotal || 0) : String(b.saleDate || "").localeCompare(String(a.saleDate || "")));
    }
    if (!content) {
        return;
    }

    if (!sales.length) {
        content.innerHTML = `
            <div style="padding:35px;text-align:center;color:#6b7280;">
                <strong>No data available</strong>
                <div style="margin-top:5px;font-size:13px;">No sales were generated on the selected date.</div>
            </div>
        `;
        return;
    }

    content.innerHTML = `
        <div style="overflow-x:auto;">
            <table style="width:100%;border-collapse:collapse;min-width:720px;">
                <thead>
                    <tr style="border-bottom:1px solid #e5e7eb;text-align:left;">
                        <th style="padding:12px 10px;">#</th>
                        <th style="padding:12px 10px;">Customer</th>
                        <th style="padding:12px 10px;">Phone</th>
                        <th style="padding:12px 10px;">Amount</th>
                        <th style="padding:12px 10px;">Date</th>
                        <th style="padding:12px 10px;">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${sales.map((sale, index) => `
                        <tr style="border-bottom:1px solid #f1f5f9;">
                            <td style="padding:12px 10px;">${index + 1}</td>
                            <td style="padding:12px 10px;">
                                <strong>${escapeHtml(sale.customerName || "Walk-in Customer")}</strong>
                                <div style="font-size:12px;color:#6b7280;">${escapeHtml(sale.customerType || "DIRECT")}</div>
                            </td>
                            <td style="padding:12px 10px;">${escapeHtml(sale.customerPhone || "-")}</td>
                            <td style="padding:12px 10px;"><strong>${formatMoney(sale.finalTotal)}</strong></td>
                            <td style="padding:12px 10px;">${formatSaleDate(sale.saleDate)}</td>
                            <td style="padding:12px 10px;">
                                <div style="display:flex;gap:7px;flex-wrap:wrap;">
                                    <button type="button" class="secondary-btn" onclick="viewSaleDetails(${Number(sale.saleId)})">Details</button>
                                    <button type="button" class="delete-btn" onclick="deleteSale(${Number(sale.saleId)})">Delete</button>
                                </div>
                            </td>
                        </tr>
                    `).join("")}
                </tbody>
            </table>
        </div>
    `;
}

function formatSaleDate(value) {
    if (!value) {
        return "-";
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return escapeHtml(String(value));
    }

    return parsed.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

async function viewSaleDetails(saleId) {
    try {
        const data = await apiRequest(
            "/data/sales/details?id=" + encodeURIComponent(saleId)
        );

        if (!data || data.ok === false) {
            throw new Error(data?.error || "Could not load sale details.");
        }

        showSaleDetailsModal(data);
    } catch (error) {
        console.error("Sale Details Error:", error);
        showNotification(
            error?.message || "Could not load sale details.",
            "warning"
        );
    }
}

function showSaleDetailsModal(sale) {
    if (!sale) {
        return;
    }

    let modal = document.getElementById("saleDetailsModal");
    if (!modal) {
        modal = document.createElement("div");
        modal.id = "saleDetailsModal";
        modal.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;padding:20px;z-index:9999;";
        document.body.appendChild(modal);
    }

    const items = Array.isArray(sale.items) ? sale.items : [];

    modal.innerHTML = `
        <div style="background:#fff;width:min(900px,100%);max-height:90vh;overflow:auto;border-radius:14px;padding:25px;box-shadow:0 20px 50px rgba(0,0,0,.2);">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:15px;margin-bottom:18px;">
                <div>
                    <h2 style="margin:0 0 5px;">Sale #${Number(sale.saleId)}</h2>
                    <div style="font-size:13px;color:#6b7280;">${formatSaleDate(sale.saleDate)}</div>
                </div>
                <button type="button" class="secondary-btn" onclick="closeSaleDetailsModal()">Close</button>
            </div>

            <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:15px;margin-bottom:22px;">
                <div><strong>Customer</strong><div>${escapeHtml(sale.customerName || "Walk-in Customer")}</div></div>
                <div><strong>Type</strong><div>${escapeHtml(sale.customerType || "DIRECT")}</div></div>
                <div><strong>Phone</strong><div>${escapeHtml(sale.customerPhone || "-")}</div></div>
            </div>

            <div style="overflow-x:auto;">
                <table style="width:100%;border-collapse:collapse;min-width:650px;">
                    <thead>
                        <tr style="border-bottom:1px solid #e5e7eb;text-align:left;">
                            <th style="padding:10px;">#</th>
                            <th style="padding:10px;">Product</th>
                            <th style="padding:10px;">Qty</th>
                            <th style="padding:10px;">Unit Price</th>
                            <th style="padding:10px;">Discount</th>
                            <th style="padding:10px;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${items.map((item, index) => `
                            <tr style="border-bottom:1px solid #f1f5f9;">
                                <td style="padding:10px;">${index + 1}</td>
                                <td style="padding:10px;">${escapeHtml(item.productName || "Product")}</td>
                                <td style="padding:10px;">${Number(item.quantity || 0)}</td>
                                <td style="padding:10px;">${formatMoney(item.unitPrice)}</td>
                                <td style="padding:10px;">${formatMoney(item.discount)}</td>
                                <td style="padding:10px;"><strong>${formatMoney(item.totalPrice)}</strong></td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>

            <div style="margin-top:20px;margin-left:auto;max-width:320px;">
                <div style="display:flex;justify-content:space-between;padding:6px 0;"><span>Subtotal</span><strong>${formatMoney(sale.subtotal)}</strong></div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;"><span>Discount</span><strong>${formatMoney(sale.discount)}</strong></div>
                <div style="display:flex;justify-content:space-between;padding:6px 0;"><span>GST</span><strong>${formatMoney(sale.gst)}</strong></div>
                <div style="display:flex;justify-content:space-between;padding:10px 0;border-top:1px solid #e5e7eb;font-size:17px;"><span>Final Total</span><strong>${formatMoney(sale.finalTotal)}</strong></div>
            </div>

            <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:20px;">
                <button type="button" class="primary-btn" onclick="generateInvoiceFromHistory(${Number(sale.saleId)})">Generate Invoice</button>
                <button type="button" class="secondary-btn" onclick="closeSaleDetailsModal()">Close</button>
            </div>
        </div>
    `;

    modal.style.display = "flex";
}

function closeSaleDetailsModal() {
    const modal = document.getElementById("saleDetailsModal");
    if (modal) {
        modal.remove();
    }
}

async function generateInvoiceFromHistory(saleId) {
    try {
        const data = await apiRequest(
            "/data/sales/details?id=" + encodeURIComponent(saleId)
        );

        if (!data || data.ok === false) {
            throw new Error(data?.error || "Could not load sale details.");
        }

        const sale = data;
        const items = Array.isArray(sale.items) ? sale.items : [];

        const saleData = {
            customerName: sale.customerName || "Walk-in Customer",
            customerType: sale.customerType || "DIRECT",
            customerPhone: sale.customerPhone || "-",
            subtotal: Number(sale.subtotal || 0),
            discount: Number(sale.discount || 0),
            gst: Number(sale.gst || 0),
            finalTotal: Number(sale.finalTotal || 0),
            items: items.map(item => ({
                productId: Number(item.productId),
                productName: item.productName || "Product",
                quantity: Number(item.quantity || 0),
                unitPrice: Number(item.unitPrice || 0),
                discount: Number(item.discount || 0),
                total: Number(item.totalPrice || 0)
            }))
        };

        closeSaleDetailsModal();
        generateInvoiceFromSaleWithNames(saleData, sale.saleId, sale.saleDate);
    } catch (error) {
        console.error("Historical Invoice Error:", error);
        showNotification(
            error?.message || "Could not generate invoice.",
            "warning"
        );
    }
}

function generateInvoiceFromSaleWithNames(saleData, saleId, saleDate) {
    const invoiceNumber = saleId || Date.now();
    const invoiceDate = saleDate
        ? formatSaleDate(saleDate)
        : new Date().toLocaleDateString("en-IN");

    const itemRows = saleData.items.map((item, index) => {
        const product = allProducts.find(
            p => String(p.product_id) === String(item.productId)
        );
        const productName = item.productName || product?.product_name || "Product";
        const total = Number(item.total || 0);

        return `
            <tr>
                <td>${index + 1}</td>
                <td>${escapeHtml(productName)}</td>
                <td>${item.quantity}</td>
                <td>${formatMoney(item.unitPrice)}</td>
                <td>${formatMoney(item.discount)}</td>
                <td>${formatMoney(total)}</td>
            </tr>
        `;
    }).join("");

    const invoiceWindow = window.open("", "_blank", "width=900,height=700");
    if (!invoiceWindow) {
        showNotification("Please allow pop-ups to generate invoice.", "warning");
        return;
    }

    invoiceWindow.document.write(`
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>HardwarePro Invoice</title>
<style>
*{box-sizing:border-box}
body{font-family:Arial,sans-serif;margin:0;padding:35px;color:#111827}
.invoice{max-width:800px;margin:auto;border:1px solid #e5e7eb;padding:35px}
.header{display:flex;justify-content:space-between;border-bottom:2px solid #2563eb;padding-bottom:20px;margin-bottom:25px}
.logo{font-size:25px;font-weight:bold;color:#2563eb}.title{font-size:28px;font-weight:bold}
.info{display:flex;justify-content:space-between;margin-bottom:25px}.info-box{width:48%}
.info-box h4{margin:0 0 7px;color:#6b7280;font-size:12px;text-transform:uppercase}.info-box p{margin:4px 0;font-size:13px}
table{width:100%;border-collapse:collapse;margin-top:20px}th,td{border-bottom:1px solid #e5e7eb;padding:10px;text-align:left;font-size:13px}
th{background:#f8fafc}.total{margin-top:25px;margin-left:auto;width:320px}.total-row{display:flex;justify-content:space-between;padding:7px 0}.grand-total{border-top:2px solid #111827;font-size:17px;margin-top:5px;padding-top:12px}.footer{text-align:center;margin-top:35px;color:#6b7280;font-size:12px}.print-btn{padding:10px 15px;margin-bottom:15px}
@media print{.print-btn{display:none}body{padding:0}.invoice{border:none}}
</style>
</head>
<body>
<button class="print-btn" onclick="window.print()">Print Invoice</button>
<div class="invoice">
<div class="header"><div><div class="logo">HardwarePro</div><p>Hardware Management System</p></div><div><div class="title">INVOICE</div><p>Invoice #${invoiceNumber}</p><p>Date: ${escapeHtml(invoiceDate)}</p></div></div>
<div class="info"><div class="info-box"><h4>Customer</h4><p><strong>${escapeHtml(saleData.customerName)}</strong></p><p>Type: ${escapeHtml(saleData.customerType)}</p><p>Phone: ${escapeHtml(saleData.customerPhone)}</p></div><div class="info-box"><h4>Business</h4><p><strong>HardwarePro</strong></p><p>Hardware & Electrical Store</p><p>India</p></div></div>
<table><thead><tr><th>#</th><th>Product</th><th>Qty</th><th>Unit Price</th><th>Discount</th><th>Total</th></tr></thead><tbody>${itemRows}</tbody></table>
<div class="total"><div class="total-row"><span>Subtotal</span><strong>${formatMoney(saleData.subtotal)}</strong></div><div class="total-row"><span>Discount</span><strong>${formatMoney(saleData.discount)}</strong></div><div class="total-row"><span>GST</span><strong>${formatMoney(saleData.gst)}</strong></div><div class="total-row grand-total"><span>Final Total</span><strong>${formatMoney(saleData.finalTotal)}</strong></div></div>
<div class="footer">Thank you for your business.<br>HardwarePro — Hardware Management System</div>
</div>
</body>
</html>
`);
    invoiceWindow.document.close();
}

async function deleteSale(saleId) {
    const confirmed = confirm(
        "Are you sure you want to delete this sale? Product stock will be restored."
    );

    if (!confirmed) {
        return;
    }

    try {
        const result = await apiRequest(
            "/data/sales/delete?id=" + encodeURIComponent(saleId),
            { method: "POST" }
        );

        if (!result || !result.ok) {
            throw new Error(result?.error || "Sale could not be deleted.");
        }

        closeSaleDetailsModal();
        showNotification("Sale deleted successfully. Stock restored.");
        await loadProducts();
        await loadDashboard();
        await loadSalesHistory();
    } catch (error) {
        console.error("Delete Sale Error:", error);
        showNotification(
            error?.message || "Sale could not be deleted.",
            "warning"
        );
    }
}

/* =========================================================
PURCHASE TOTAL
========================================================= */
function calculatePurchaseTotal() {
    const rows = document.querySelectorAll(
        "#purchaseProductRows .purchase-product-row"
    );

    let grandTotal = 0;

    rows.forEach(row => {
        const quantity = Number(
            row.querySelector(".purchase-quantity")?.value
        ) || 0;
        const unitPrice = Number(
            row.querySelector(".purchase-unit-price")?.value
        ) || 0;
        const total = quantity * unitPrice;

        const totalInput = row.querySelector(".purchase-total-price");
        if (totalInput) {
            totalInput.value = total.toFixed(2);
        }

        grandTotal += total;
    });

    const count = document.getElementById("purchaseProductCount");
    if (count) {
        count.textContent = rows.length;
    }

    const grandTotalElement = document.getElementById("purchaseGrandTotal");
    if (grandTotalElement) {
        grandTotalElement.textContent = formatMoney(grandTotal);
    }

    return grandTotal;
}

function addPurchaseProductRow(product = null) {
    const container = document.getElementById("purchaseProductRows");
    if (!container) return;

    const quantity = Number(product?.quantity || 1);
    const unitPrice = Number(product?.unitPrice || 0);
    const total = quantity * unitPrice;

    const row = document.createElement("div");
    row.className = "purchase-product-row";
    row.innerHTML = `
        <div class="form-grid">
            <div class="form-group">
                <label>Product Name</label>
                <input type="text" class="purchase-product-name"
                    placeholder="Product name"
                    value="${escapeHtml(product?.productName || "")}">
            </div>

            <div class="form-group">
                <label>Product Type</label>
                <input type="text" class="purchase-product-type"
                    placeholder="Electrical / Plumbing / Tools"
                    value="${escapeHtml(product?.productType || "")}">
            </div>

            <div class="form-group">
                <label>Quantity</label>
                <input type="number" class="purchase-quantity"
                    min="1" value="${quantity}"
                    oninput="calculatePurchaseTotal()">
            </div>

            <div class="form-group">
                <label>Unit Purchase Price</label>
                <input type="number" class="purchase-unit-price"
                    min="0" step="0.01" placeholder="0.00"
                    value="${unitPrice}"
                    oninput="calculatePurchaseTotal()">
            </div>

            <div class="form-group">
                <label>Total Purchase Price</label>
                <input type="number" class="purchase-total-price"
                    readonly value="${total.toFixed(2)}">
            </div>
        </div>

        <div class="form-actions">
            <button type="button" class="secondary-btn"
                onclick="this.closest('.purchase-product-row').remove(); calculatePurchaseTotal();">
                Remove Product
            </button>
        </div>
    `;

    container.appendChild(row);
    calculatePurchaseTotal();
}

/* =========================================================
SAVE PURCHASE
========================================================= */
async function savePurchase() {
    const ids = ["supplierName","supplierPhone","supplierLandline","supplierEmail","supplierFax","supplierGst","supplierStreet","supplierDoorNumber","supplierVillage","supplierDistrict","supplierCity","supplierPincode","supplierCountry"];
    const requiredIds = ["supplierName","supplierPhone","supplierEmail","supplierGst","supplierStreet","supplierDoorNumber","supplierDistrict","supplierCity","supplierPincode","supplierCountry"];
    const supplier = {};
    ids.forEach(id => supplier[id] = document.getElementById(id)?.value.trim() || "");
    const invoiceNumber = document.getElementById("purchaseInvoiceNumber")?.value.trim() || "";

    const missing = requiredIds.find(id => !supplier[id]);
    if (!invoiceNumber) {
        showNotification("Enter Invoice Number.", "warning");
        document.getElementById("purchaseInvoiceNumber")?.focus();
        return;
    }
    if (missing) {
        const label = document.querySelector(`label[for="${missing}"]`)?.textContent || missing.replace("supplier", "");
        showNotification(`Enter ${label.replace("*", "").trim()}.`, "warning");
        document.getElementById(missing)?.focus();
        return;
    }

    const rows = document.querySelectorAll("#purchaseProductRows .purchase-product-row");
    if (!rows.length) { showNotification("Add at least one product.", "warning"); return; }
    const products = [];
    for (const row of rows) {
        const productName = row.querySelector(".purchase-product-name")?.value.trim() || "";
        const productType = row.querySelector(".purchase-product-type")?.value.trim() || "";
        const quantity = Number(row.querySelector(".purchase-quantity")?.value) || 0;
        const unitPrice = Number(row.querySelector(".purchase-unit-price")?.value) || 0;
        if (!productName) { showNotification("Enter product name for all products.", "warning"); return; }
        if (quantity <= 0) { showNotification(`Enter a valid quantity for ${productName}.`, "warning"); return; }
        if (unitPrice < 0 || !Number.isFinite(unitPrice)) { showNotification(`Enter a valid purchase price for ${productName}.`, "warning"); return; }
        products.push({ productName, productType, quantity, unitPrice, total: quantity * unitPrice });
    }

    const payload = {
        supplierName: supplier.supplierName, supplierPhone: supplier.supplierPhone, supplierLandline: supplier.supplierLandline,
        supplierEmail: supplier.supplierEmail, supplierFax: supplier.supplierFax, supplierGst: supplier.supplierGst,
        supplierStreet: supplier.supplierStreet, supplierDoorNumber: supplier.supplierDoorNumber, supplierVillage: supplier.supplierVillage,
        supplierDistrict: supplier.supplierDistrict, supplierCity: supplier.supplierCity, supplierPincode: supplier.supplierPincode, supplierCountry: supplier.supplierCountry,
        invoiceNumber, products
    };
    try {
        const result = await apiRequest("/data/purchases/import", { method:"POST", headers:{"Content-Type":"application/json"}, body:JSON.stringify(payload) });
        if (!result || !result.ok) throw new Error(result?.error || "Purchase could not be saved.");
        showNotification("Purchase saved and stock updated.");
        clearPurchaseForm(); await loadProducts(); await loadDashboard();
        if (typeof loadSuppliers === "function") loadSuppliers();
    } catch (error) {
        console.error("Purchase Error:", error);
        showNotification(error?.message || "Purchase could not be saved.", "warning");
    }
}

/* =========================================================
CLEAR PURCHASE
========================================================= */
function clearPurchaseForm() {
    const container = document.getElementById("purchaseProductRows");

    if (container) {
        container.innerHTML = `
            <div class="purchase-product-row" data-row-index="0">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Product Name</label>
                        <input type="text" id="purchaseProductName"
                            class="purchase-product-name"
                            placeholder="Product name">
                    </div>

                    <div class="form-group">
                        <label>Product Type</label>
                        <input type="text" id="purchaseProductType"
                            class="purchase-product-type"
                            placeholder="Electrical / Plumbing / Tools">
                    </div>

                    <div class="form-group">
                        <label>Quantity</label>
                        <input type="number" id="purchaseQuantity"
                            class="purchase-quantity" min="1" value="1"
                            oninput="calculatePurchaseTotal()">
                    </div>

                    <div class="form-group">
                        <label>Unit Purchase Price</label>
                        <input type="number" id="purchaseUnitPrice"
                            class="purchase-unit-price" min="0" step="0.01"
                            placeholder="0.00"
                            oninput="calculatePurchaseTotal()">
                    </div>

                    <div class="form-group">
                        <label>Total Purchase Price</label>
                        <input type="number" id="purchaseTotalPrice"
                            class="purchase-total-price" readonly value="0.00">
                    </div>
                </div>
            </div>
        `;
    }

    ["supplierName","supplierPhone","supplierLandline","supplierEmail","supplierFax","supplierGst","supplierStreet","supplierDoorNumber","supplierVillage","supplierDistrict","supplierCity","supplierPincode","supplierCountry","purchaseInvoiceNumber"].forEach(id => {
        const element = document.getElementById(id);
        if (element) element.value = "";
    });

    const pdfResult = document.getElementById("pdfResult");
    if (pdfResult) pdfResult.style.display = "none";

    closeNewProductsModal();
    closePurchaseImportModal();

    pdfPurchaseData = null;
    pdfReviewProducts = [];
    pdfImportExistingProducts = [];
    pdfImportNewProducts = [];
    pdfImportSelectedProducts = [];
    pdfImportInProgress = false;

    const fileInput = document.getElementById("supplierPdf");
    if (fileInput) fileInput.value = "";

    calculatePurchaseTotal();
}

/* =========================================================
READ SUPPLIER PDF
========================================================= */
async function readSupplierFile() {
const fileInput =
document.getElementById(
"supplierPdf"
);
if (
!fileInput ||
!fileInput.files ||
!fileInput.files.length
) {
showNotification(
"Please select a supplier invoice file.",
"warning"
);
return;
}

const file =
fileInput.files[0];

const fileName =
String(file.name || "").toLowerCase();

const supported =
fileName.endsWith(".pdf") ||
fileName.endsWith(".xlsx") ||
fileName.endsWith(".xls") ||
fileName.endsWith(".csv") ||
fileName.endsWith(".txt");

if (!supported) {
showNotification(
"Unsupported file. Please select PDF, Excel, CSV or TXT.",
"warning"
);
return;
}

const formData =
new FormData();

formData.append(
"file",
file
);

try {
showNotification(
"Reading " + file.name + "...",
"success"
);

let result;
try {
const response = await fetch(
"https://sales-management-system-rs5b.onrender.com/data/purchases/read-file",
{
method: "POST",
body: formData
}
);

const contentType =
response.headers.get("content-type") || "";

if (contentType.includes("application/json")) {
result = await response.json();
} else {
const responseText = await response.text();
throw new Error(
responseText ||
("HTTP Error " + response.status)
);
}

if (!response.ok || !result || !result.ok) {
throw new Error(
result?.error ||
("HTTP Error " + response.status)
);
}
} catch (requestError) {
console.error("Purchase file request failed:", requestError);
throw new Error(
requestError?.message ||
"Could not connect to the purchase file reader. Make sure the Java server is running and rebuilt."
);
}

const text =
String(result.text || "").trim();

const pdfResult =
document.getElementById(
"pdfResult"
);

const pdfText =
document.getElementById(
"pdfText"
);

if (pdfResult) {
pdfResult.style.display =
"block";
}

if (pdfText) {
pdfText.textContent =
text ||
"No readable data found in the selected file.";
}

if (!text) {
pdfPurchaseData = null;
pdfReviewProducts = [];
renderPDFPurchaseReview();

showNotification(
"File was read, but no readable data was extracted.",
"warning"
);
return;
}

autoFillFromPDF(text);

const fileType =
String(result.fileType || "").toUpperCase();

/*
 * CSV files are converted to tab-separated rows by the backend.
 * Excel files are also returned as tab-separated rows.
 * PDF/TXT text can go directly through the existing invoice parser.
 */
const parsed =
parsePurchaseInvoiceText(text);

pdfPurchaseData = parsed;

pdfReviewProducts =
Array.isArray(parsed.products)
? parsed.products
: [];

if (!pdfReviewProducts.length) {
renderPDFPurchaseReview();

showNotification(
fileType
? `${fileType} read successfully, but no product rows were detected. Please check the extracted data or enter the purchase manually.`
: "File was read successfully, but no product rows were detected.",
"warning"
);

return;
}

renderPDFPurchaseReview();

/* Refresh the master product list before comparing imported products.
 * Do this without the normal loadProducts() UI flow so a temporary
 * /data/products failure cannot block the import popup.
 */
try {
const productResponse = await fetch(
    "https://sales-management-system-rs5b.onrender.com/data/products"
);
if (productResponse.ok) {
const productData = await productResponse.json();
if (productData && productData.ok !== false) {
allProducts = Array.isArray(productData.products)
? productData.products
: [];
}
}
} catch (productError) {
console.warn("Could not refresh products before purchase comparison:", productError);
}

/* Always continue to the comparison popup. If the product list could not
 * be refreshed, imported products are treated as new instead of silently
 * stopping with a fetch error.
 */
preparePDFProductComparison();

showNotification(
`${pdfReviewProducts.length} product${pdfReviewProducts.length === 1 ? "" : "s"} detected from ${file.name}.`
);

} catch (error) {
console.error(
"Supplier File Import Error:",
error
);

showNotification(
error?.message ||
"Could not read the selected file.",
"warning"
);
}
}

/*
 * Keep the old function name available so existing HTML or bookmarks
 * that still call readSupplierPDF() continue to work.
 */
async function readSupplierPDF() {
return readSupplierFile();
}

/* =========================================================
PDF INVOICE PARSER
========================================================= */
function cleanPdfNumber(value) {
if (
value === null ||
value === undefined
) {
return 0;
}
const cleaned =
String(value)
.replace(/[n$€£]/g, "")
.replace(/Rs\.?/gi, "")
.replace(/INR/gi, "")
.replace(/,/g, "")
.trim();
const number =
Number(cleaned);
return Number.isFinite(number)
? number
: 0;
}
function normalizePurchaseDate(value) {
if (!value) {
return "";
}
const text =
String(value).trim();
let match =
text.match(/^(\d{1,2})[-\/.](\d{1,2})[-\/.](\d{4})$/);
if (match) {
const day =
match[1].padStart(2, "0");
const month =
match[2].padStart(2, "0");
return `${match[3]}-${month}-${day}`;

}
match =
text.match(/^(\d{4})[-\/.](\d{1,2})[-\/.](\d{1,2})$/);
if (match) {
return `${match[1]}-${match[2].padStart(2, "0")}-${match[3].padStart(2, "0")}`;

}
const parsed =
new Date(text);
if (!Number.isNaN(parsed.getTime())) {
return parsed
.toISOString()
.slice(0, 10);
}
return "";
}
function extractPDFHeader(text) {
const result = {
invoiceNumber: "",
supplierName: "",
supplierPhone: "",
supplierLandline: "",
supplierEmail: "",
supplierFax: "",
supplierGst: "",
supplierStreet: "",
supplierDoorNumber: "",
supplierVillage: "",
supplierDistrict: "",
supplierCity: "",
supplierPincode: "",
supplierCountry: "",
purchaseDate: ""
};
const lines =
String(text || "")
.split(/\r?\n/)
.map(line => line.trim())
.filter(Boolean);
/* Invoice / bill number */
const invoicePatterns = [
/(?:invoice|bill)\s*(?:no\.?|number|#)\s*[:\-]?\s*([A-Za-z0-9][A-Za-z0-9\-\/_.]*)/i,
/(?:invoice|bill)\s*[:\-]\s*([A-Za-z0-9][A-Za-z0-9\-\/_.]*)/i
];
for (const pattern of invoicePatterns) {
const match =
text.match(pattern);
if (match) {
result.invoiceNumber =
match[1].trim();
break;
}
}
/* Supplier */
for (let i = 0; i < lines.length; i++) {
const match =
lines[i].match(
/^supplier(?:\s+name)?\s*[:\-]\s*(.+)$/i
);
if (match) {
result.supplierName =
match[1].trim();
break;
}
}
if (!result.supplierName) {
const vendorMatch =
text.match(
/(?:vendor|seller)\s*(?:name)?\s*[:\-]\s*(.+)/i
);
if (vendorMatch) {
result.supplierName =
vendorMatch[1].trim();
}
}
/* Phone */
const phoneMatch =
text.match(
/(?:phone|mobile|contact|tel(?:ephone)?)\s*[:\-]?\s*(\+?\d[\d\s()\-]{8,}\d)/i
);
if (phoneMatch) {
result.supplierPhone =
phoneMatch[1]
.replace(/[\s()\-]/g, "")
.trim();
}
/* Additional supplier details */
const fieldPatterns = {
    supplierLandline: /(?:landline|land\s*line|telephone)\s*[:\-]?\s*(.+)/i,
    supplierEmail: /(?:email|e-mail)\s*[:\-]?\s*([^\s]+)/i,
    supplierFax: /fax\s*[:\-]?\s*(.+)/i,
    supplierGst: /(?:gst|gstin)\s*[:\-]?\s*(.+)/i,
    supplierStreet: /street\s*[:\-]?\s*(.+)/i,
    supplierDoorNumber: /(?:door\s*(?:no|number)|house\s*(?:no|number))\s*[:\-]?\s*(.+)/i,
    supplierVillage: /village\s*[:\-]?\s*(.+)/i,
    supplierDistrict: /district\s*[:\-]?\s*(.+)/i,
    supplierCity: /city\s*[:\-]?\s*(.+)/i,
    supplierPincode: /(?:pin|pincode|postal\s*code)\s*[:\-]?\s*(.+)/i,
    supplierCountry: /country\s*[:\-]?\s*(.+)/i
};
Object.entries(fieldPatterns).forEach(([key, pattern]) => {
    const m = text.match(pattern);
    if (m) result[key] = m[1].trim();
});

/* Date */
const dateMatch =
text.match(
/(?:invoice\s*)?date\s*[:\-]?\s*(\d{1,2}[-\/.]\d{1,2}[-\/.]\d{4}|\d{4}[-\/.]\d{1,2}[-\/.]\d{1,2}|[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})/i
);
if (dateMatch) {

result.purchaseDate =
normalizePurchaseDate(
dateMatch[1]
);
}

return result;
}
function isPDFSummaryLine(line) {
return /^(subtotal|sub\s*total|tax|gst|cgst|sgst|igst|discount|grand\s*total|total|amount\s*due|net\s*amount|round\s*off|balance|terms|thank|notes?)/i.test(line.trim());
}
function looksLikePDFProductHeader(line) {
return /\b(product|item|description)\b/i.test(line) &&
/\b(qty|quantity)\b/i.test(line) &&
/\b(price|rate|unit)\b/i.test(line);
}
function parseProductColumns(line) {
let columns =
line
.split(/\t+|\s{2,}/)
.map(value => value.trim())
.filter(Boolean);

if (columns.length < 4) {
return null;
}

/* Remove a serial number column. */
if (/^\d{1,4}[.)]?$/.test(columns[0])) {
columns.shift();
}

if (columns.length < 4) {
return null;
}

const numericIndexes = [];
for (let i = 0; i < columns.length; i++) {
if (/^(?:n|Rs\.?|INR)?\s*[\d,]+(?:\.\d+)?$/.test(columns[i])) {
numericIndexes.push(i);
}
}

if (numericIndexes.length < 3) {
return null;
}

const qtyIndex = numericIndexes[numericIndexes.length - 3];
const unitIndex = numericIndexes[numericIndexes.length - 2];
const totalIndex = numericIndexes[numericIndexes.length - 1];
const quantity = cleanPdfNumber(columns[qtyIndex]);
const unitPrice = cleanPdfNumber(columns[unitIndex]);
const total = cleanPdfNumber(columns[totalIndex]);

if (quantity <= 0 || unitPrice < 0) {
return null;
}

const prefixColumns =
columns.slice(0, qtyIndex);

if (!prefixColumns.length) {
return null;
}

let productName = prefixColumns.join(" ").trim();
let productType = "";

/*
 * Excel commonly has: Product Name | Product Type | Quantity | Unit Price | Total
 * When two text columns exist before the numeric columns, preserve them separately.
 */
if (prefixColumns.length >= 2 && line.includes("\t")) {
productType = prefixColumns[prefixColumns.length - 1].trim();
productName = prefixColumns.slice(0, -1).join(" ").trim();
}

/*
 * Some supplier PDFs place current stock immediately after the product type.
 * Example: PVC Pipe 1/2 Inch Plumbing 100 85 8 680
 * Here 100 is current stock, not part of the product name.
 */
productName =
productName.replace(/\s+\d{1,8}(?:\.\d+)?\s*$/, "").trim();

/* Existing PDF behavior: detect a known type at the end of the product text. */
if (!productType) {
const typeMatch =
productName.match(
/^(.*?)(?:\s+)(Plumbing|Electrical|Tools?|Steel|Cement|Fasteners?|Hardware|Paint|Safety|Construction|Sanitary|PVC|Adhesives?|Automotive|Keyboard|Mouse|USB\s*Hub|SSD|RAM|Router|Monitor|UPS|Printer|Network\s*Switch)$/i
);
if (typeMatch) {
productName = typeMatch[1].trim();
productType = typeMatch[2].trim();
}
}

return {
productName: productName.replace(/^\d{1,4}[.)]?\s+/, "").trim(),
productType,
quantity,
unitPrice,
total: total || quantity * unitPrice
};
}

function parseProductByTrailingNumbers(line) {

/*
Handles PDF extraction where table columns collapse into spaces.
Example:
1 PVC Pipe 1/2 Inch Plumbing 100 85 8500
*/
const match =
line.match(
/^(.*?)[\s|]+(?:n|Rs\.?|INR)?\s*([\d,]+(?:\.\d+)?)\s+(?:n|Rs\.?|INR)?\s*([\d,]+(?:\.\d+)?)\s+(?:n|Rs\.?|INR)?\s*([\d,]+(?:\.\d+)?)\s*$/i
);
if (!match) {
return null;
}
const prefix =
match[1].trim();
const quantity =
cleanPdfNumber(match[2]);
const unitPrice =
cleanPdfNumber(match[3]);
const total =
cleanPdfNumber(match[4]);
if (
!prefix ||
quantity <= 0 ||
unitPrice < 0
) {
return null;
}
let productName =
prefix.replace(/^\d{1,4}[.)]?\s+/, "").trim();
let productType = "";

/*
 * Some supplier PDFs include current stock between the product type
 * and purchase quantity. Do not merge that stock value into the name.
 */
productName =
productName.replace(/\s+\d{1,8}(?:\.\d+)?\s*$/, "").trim();

const typeMatch =
productName.match(
/^(.*?)(?:\s+)(Plumbing|Electrical|Tools?|Steel|Cement|Fasteners?|Hardware|Paint|Safety|Construction|Sanitary|PVC|Adhesives?|Automotive|Keyboard|Mouse|USB\s*Hub|SSD|RAM|Router|Monitor|UPS|Printer|Network\s*Switch)$/i
);
if (typeMatch) {
productName =
typeMatch[1].trim();
productType =
typeMatch[2].trim();
}
return {
productName,
productType,
quantity,
unitPrice,
total: total || quantity * unitPrice
};
}

function normalizeImportedTableText(text) {
const source =
String(text || "");

const lines =
source
.split(/\r?\n/)
.map(line => line.trim())
.filter(Boolean);

return lines
.map(line => {
if (line.includes("\t")) {
return line;
}

/*
 * Convert simple CSV rows to tab-separated rows.
 * Excel rows are already returned as tab-separated text by the backend.
 */
if (line.includes(",")) {
const values = [];
let current = "";
let inQuotes = false;

for (let i = 0; i < line.length; i++) {
const character = line[i];

if (character === '"') {
if (inQuotes && line[i + 1] === '"') {
current += '"';
i++;
} else {
inQuotes = !inQuotes;
}
} else if (character === "," && !inQuotes) {
values.push(current.trim());
current = "";
} else {
current += character;
}
}

values.push(current.trim());
return values.join("\t");
}

return line;
})
.join("\n");
}

function parseStructuredPurchaseRows(text) {
    const lines = String(text || "").split(/\r?\n/).map(v => v.trim()).filter(Boolean);
    const headerIndex = lines.findIndex(line => /product\s*name|product|item/i.test(line) && /quantity|qty/i.test(line) && /unit\s*(price|rate)|price|rate/i.test(line) && /supplier/i.test(line));
    if (headerIndex < 0) return null;
    const header = lines[headerIndex].split("\t").map(v => v.trim().toLowerCase());
    const idx = (patterns) => header.findIndex(h => patterns.some(p => p.test(h)));
    const ix = {
        supplierName:idx([/supplier\s*name/,/^supplier$/]), supplierPhone:idx([/supplier.*phone/,/^phone$/]), supplierLandline:idx([/landline/,/telephone/]),
        supplierEmail:idx([/supplier.*email/,/^email$/]), supplierFax:idx([/fax/]), supplierGst:idx([/gst/]), supplierStreet:idx([/street/]),
        supplierDoorNumber:idx([/door/,/house.*no/]), supplierVillage:idx([/village/]), supplierDistrict:idx([/district/]), supplierCity:idx([/city/]),
        supplierPincode:idx([/pincode/,/postal/]), supplierCountry:idx([/country/]), invoiceNumber:idx([/invoice/]), purchaseDate:idx([/^date$/, /purchase\s*date/]),
        productName:idx([/product\s*name/,/^product$/, /item/,/description/]), productType:idx([/product\s*type/,/^type$/]),
        quantity:idx([/^qty$/, /quantity/]), unitPrice:idx([/unit\s*price/,/^price$/, /rate/]), total:idx([/^total$/, /amount/])
    };
    if (ix.productName < 0 || ix.quantity < 0 || ix.unitPrice < 0) return null;
    const products = [];
    for (let i = headerIndex + 1; i < lines.length; i++) {
        const cols = lines[i].split("\t").map(v => v.trim());
        if (!cols.some(Boolean)) continue;
        const productName = cols[ix.productName] || "";
        const quantity = cleanPdfNumber(cols[ix.quantity]);
        const unitPrice = cleanPdfNumber(cols[ix.unitPrice]);
        if (!productName || quantity <= 0 || unitPrice < 0) continue;
        const product = {
            productName, productType: ix.productType >= 0 ? (cols[ix.productType] || "") : "",
            quantity, unitPrice, total: ix.total >= 0 ? (cleanPdfNumber(cols[ix.total]) || quantity * unitPrice) : quantity * unitPrice
        };
        ["supplierName","supplierPhone","supplierLandline","supplierEmail","supplierFax","supplierGst","supplierStreet","supplierDoorNumber","supplierVillage","supplierDistrict","supplierCity","supplierPincode","supplierCountry","invoiceNumber","purchaseDate"].forEach(key => {
            if (ix[key] >= 0) product[key] = cols[ix[key]] || "";
        });
        products.push(product);
    }
    if (!products.length) return null;
    const first = products[0];
    return { invoiceNumber:first.invoiceNumber || "", supplierName:first.supplierName || "", supplierPhone:first.supplierPhone || "", supplierLandline:first.supplierLandline || "", supplierEmail:first.supplierEmail || "", supplierFax:first.supplierFax || "", supplierGst:first.supplierGst || "", supplierStreet:first.supplierStreet || "", supplierDoorNumber:first.supplierDoorNumber || "", supplierVillage:first.supplierVillage || "", supplierDistrict:first.supplierDistrict || "", supplierCity:first.supplierCity || "", supplierPincode:first.supplierPincode || "", supplierCountry:first.supplierCountry || "", purchaseDate:normalizePurchaseDate(first.purchaseDate || ""), products, suppliers:[...new Set(products.map(p => p.supplierName).filter(Boolean))] };
}

function parsePurchaseInvoiceText(text) {
    const normalizedText = normalizeImportedTableText(text);
    const structured = parseStructuredPurchaseRows(normalizedText);
    if (structured) return structured;

    const sourceLines = String(normalizedText || "")
        .split(/\r?\n/)
        .map(line => line.replace(/\u00a0/g, " ").trim())
        .filter(Boolean);

    /*
     * PDF invoices can contain more than one supplier.  When the file has
     * repeated Supplier/Vendor/Seller headings, split it into supplier blocks
     * so every product keeps the correct supplier details.
     */
    const supplierHeaderRegex = /^(?:supplier(?:\s+name)?|vendor(?:\s+name)?|seller(?:\s+name)?)\s*[:\-]\s*(.+)$/i;
    const supplierBlocks = [];
    let currentBlock = [];

    for (const line of sourceLines) {
        if (supplierHeaderRegex.test(line) && currentBlock.some(Boolean)) {
            supplierBlocks.push(currentBlock);
            currentBlock = [];
        }
        currentBlock.push(line);
    }
    if (currentBlock.length) supplierBlocks.push(currentBlock);

    const hasMultipleSupplierBlocks = supplierBlocks.length > 1 &&
        supplierBlocks.filter(block => block.some(line => supplierHeaderRegex.test(line))).length > 1;

    const parseBlock = (block) => {
        const blockText = block.join("\n");
        const header = extractPDFHeader(blockText);
        const products = [];
        let tableStarted = false;

        for (const line of block) {
            if (isPDFSummaryLine(line)) continue;
            if (looksLikePDFProductHeader(line)) {
                tableStarted = true;
                continue;
            }

            let product = parseProductColumns(line);
            if (!product) product = parseProductByTrailingNumbers(line);
            if (!product) continue;
            if (product.productName.length < 2 || /^(product|item|description|qty|quantity|price|rate|amount)$/i.test(product.productName)) continue;
            if (/^(invoice|bill|supplier|vendor|seller|date|phone|mobile|contact|email|gst|fax|country|city|district|village|street|pincode|pin)\b/i.test(product.productName)) continue;

            Object.assign(product, header);
            products.push(product);
            tableStarted = true;
        }

        return { header, products, tableDetected: tableStarted };
    };

    if (hasMultipleSupplierBlocks) {
        const allProducts = [];
        let firstHeader = null;
        const suppliers = [];

        supplierBlocks.forEach(block => {
            const parsedBlock = parseBlock(block);
            if (!firstHeader) firstHeader = parsedBlock.header;
            if (parsedBlock.header.supplierName) suppliers.push(parsedBlock.header.supplierName);
            allProducts.push(...parsedBlock.products);
        });

        const uniqueProducts = [];
        const seen = new Set();
        for (const product of allProducts) {
            const key = [
                product.supplierName,
                product.invoiceNumber,
                product.productName,
                product.productType,
                product.quantity,
                product.unitPrice
            ].join("|").toLowerCase();
            if (seen.has(key)) continue;
            seen.add(key);
            uniqueProducts.push(product);
        }

        return {
            ...(firstHeader || extractPDFHeader(normalizedText)),
            products: uniqueProducts,
            tableDetected: uniqueProducts.length > 0,
            suppliers: [...new Set(suppliers.filter(Boolean))]
        };
    }

    /* Preserve the original single-supplier PDF/TXT parsing behavior. */
    const header = extractPDFHeader(normalizedText);
    const products = [];
    let tableStarted = false;

    for (const line of sourceLines) {
        if (isPDFSummaryLine(line)) continue;
        if (looksLikePDFProductHeader(line)) {
            tableStarted = true;
            continue;
        }
        let product = parseProductColumns(line);
        if (!product) product = parseProductByTrailingNumbers(line);
        if (!product) continue;
        if (product.productName.length < 2 || /^(product|item|description|qty|quantity|price|rate|amount)$/i.test(product.productName)) continue;
        if (/^(invoice|bill|supplier|vendor|seller|date|phone|mobile|contact|email|gst|fax|country|city|district|village|street|pincode|pin)\b/i.test(product.productName)) continue;
        Object.assign(product, header);
        tableStarted = true;
        products.push(product);
    }

    const uniqueProducts = [];
    const seen = new Set();
    for (const product of products) {
        const key = [product.productName, product.productType, product.quantity, product.unitPrice].join("|").toLowerCase();
        if (seen.has(key)) continue;
        seen.add(key);
        uniqueProducts.push(product);
    }

    return {
        ...header,
        products: uniqueProducts,
        tableDetected: tableStarted,
        suppliers: header.supplierName ? [header.supplierName] : []
    };
}

/* =========================================================
APPLY PDF HEADER DATA TO PURCHASE FORM
========================================================= */
function autoFillFromPDF(text) {
if (!text) {
return;
}
const header =
extractPDFHeader(text);
const invoiceInput =
document.getElementById(
"purchaseInvoiceNumber"
);
if (
invoiceInput &&
header.invoiceNumber
) {
invoiceInput.value =
header.invoiceNumber;
}
const supplierInput =
document.getElementById(
"supplierName"
);
if (
supplierInput &&
header.supplierName
) {
supplierInput.value =
header.supplierName;
}
const phoneInput =
document.getElementById(
"supplierPhone"

);
if (phoneInput && header.supplierPhone) phoneInput.value = header.supplierPhone;
const headerFieldMap = {
 supplierLandline:"supplierLandline", supplierEmail:"supplierEmail", supplierFax:"supplierFax", supplierGst:"supplierGst",
 supplierStreet:"supplierStreet", supplierDoorNumber:"supplierDoorNumber", supplierVillage:"supplierVillage", supplierDistrict:"supplierDistrict",
 supplierCity:"supplierCity", supplierPincode:"supplierPincode", supplierCountry:"supplierCountry"
};
Object.entries(headerFieldMap).forEach(([sourceId, targetId]) => {
 const el = document.getElementById(targetId);
 if (el && header[sourceId]) el.value = header[sourceId];
});
}
/* =========================================================
PDF PRODUCT COMPARISON / IMPORT MODALS
========================================================= */
function preparePDFProductComparison() {
pdfImportExistingProducts = [];
pdfImportNewProducts = [];

const projectedStocks = new Map();
const newProductKeys = new Set();

pdfReviewProducts.forEach(product => {
const normalized = String(product.productName || "").trim().toLowerCase();
if (!normalized) return;

const existing = allProducts.find(item =>
String(item.product_name || "").trim().toLowerCase() === normalized
);

if (existing) {
const baseStock = projectedStocks.has(normalized)
? projectedStocks.get(normalized)
: Number(existing.quantity || 0);
const currentStock = baseStock;
const finalStock = currentStock + Number(product.quantity || 0);
projectedStocks.set(normalized, finalStock);

pdfImportExistingProducts.push({
...product,
productId: existing.product_id,
currentStock,
finalStock
});
return;
}

/* If the same new product occurs more than once in the imported file,
 * keep it in the new-product list while projecting its stock correctly. */
const previousNewStock = projectedStocks.has(normalized)
? projectedStocks.get(normalized)
: 0;
const finalStock = previousNewStock + Number(product.quantity || 0);
projectedStocks.set(normalized, finalStock);
newProductKeys.add(normalized);
pdfImportNewProducts.push({
...product,
currentStock: previousNewStock,
finalStock
});
});

pdfImportSelectedProducts = [
...pdfImportExistingProducts,
...pdfImportNewProducts
];
showPurchaseImportModal();
}
function showNewProductsModal() {
const modal = document.getElementById("newProductsModal");
const list = document.getElementById("newProductsList");
if (!modal || !list) {
showPurchaseImportModal();
return;
}
list.innerHTML = `
<div style="padding:12px;">
<table style="width:100%;border-collapse:collapse;">
<thead>
<tr style="background:#f3f4f6;">
<th style="padding:10px;text-align:left;">#</th>
<th style="padding:10px;text-align:left;">Supplier</th>
<th style="padding:10px;text-align:left;">Product</th>
<th style="padding:10px;text-align:left;">Type</th>
<th style="padding:10px;text-align:right;">Quantity</th>
<th style="padding:10px;text-align:right;">Unit Price</th>
</tr>
</thead>
<tbody>
${pdfImportNewProducts.map((product, index) => `
<tr>
<td style="padding:10px;">${index + 1}</td>
<td style="padding:10px;font-weight:600;">${escapeHtml(product.productName)}</td>
<td style="padding:10px;">${escapeHtml(product.productType || "-")}</td>
<td style="padding:10px;text-align:right;">${Number(product.quantity || 0)}</td>
<td style="padding:10px;text-align:right;">${formatMoney(product.unitPrice)}</td>
</tr>
`).join("")}
</tbody>
</table>
</div>
`;
modal.classList.add("show");
}
function closeNewProductsModal() {
const modal = document.getElementById("newProductsModal");
if (modal) modal.classList.remove("show");
}
function continueWithExistingProducts() {
pdfImportSelectedProducts = [
...pdfImportExistingProducts
];
closeNewProductsModal();
if (!pdfImportSelectedProducts.length) {

showNotification("No existing products were found in the PDF.", "warning");
return;
}
showPurchaseImportModal();
}

function createNewPDFProducts() {
pdfImportSelectedProducts = [
...pdfImportExistingProducts,
...pdfImportNewProducts
];
closeNewProductsModal();
showPurchaseImportModal();
}
function showPurchaseImportModal() {
const modal = document.getElementById("purchaseImportModal");
const list = document.getElementById("purchaseImportList");
if (!modal || !list) {
importPDFPurchase();
return;
}

const existingProducts = Array.isArray(pdfImportExistingProducts) ? pdfImportExistingProducts : [];
const newProducts = Array.isArray(pdfImportNewProducts) ? pdfImportNewProducts : [];
const products = Array.isArray(pdfImportSelectedProducts) ? pdfImportSelectedProducts : [];
const total = products.reduce(
(sum, product) => sum + Number(product.quantity || 0) * Number(product.unitPrice || 0),
0
);

const renderRows = (rows, isNew) => rows.map((product, index) => {
const current = Number(product.currentStock || 0);
const quantity = Number(product.quantity || 0);
const finalStock = isNew ? quantity : current + quantity;
return `
<tr style="border-top:1px solid #e5e7eb;">
<td style="padding:10px;">${index + 1}</td>
<td style="padding:10px;font-weight:600;">${escapeHtml(product.supplierName || pdfPurchaseData?.supplierName || "-")}</td>
<td style="padding:10px;font-weight:600;">${escapeHtml(product.productName || "-")}</td>
<td style="padding:10px;">${escapeHtml(product.productType || "-")}</td>
<td style="padding:10px;text-align:right;">${isNew ? "New Product" : current}</td>
<td style="padding:10px;text-align:right;">${quantity}</td>
<td style="padding:10px;text-align:right;font-weight:600;">${finalStock}</td>
<td style="padding:10px;text-align:right;">${formatMoney(product.unitPrice)}</td>
<td style="padding:10px;text-align:right;">${formatMoney(quantity * Number(product.unitPrice || 0))}</td>
</tr>`;
}).join("");

const renderTable = (title, rows, isNew) => rows.length ? `
<div style="margin-top:16px;border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;">
<div style="padding:11px 14px;background:${isNew ? "#fff7ed" : "#f0fdf4"};font-weight:700;">
${escapeHtml(title)} (${rows.length})
</div>
<div style="overflow:auto;">
<table style="width:100%;border-collapse:collapse;min-width:950px;">
<thead><tr style="background:#f3f4f6;">
<th style="padding:10px;text-align:left;">#</th>
<th style="padding:10px;text-align:left;">Supplier</th>
<th style="padding:10px;text-align:left;">Product</th>
<th style="padding:10px;text-align:left;">Type</th>
<th style="padding:10px;text-align:right;">Current Stock</th>
<th style="padding:10px;text-align:right;">Purchase Qty</th>
<th style="padding:10px;text-align:right;">Final Stock</th>
<th style="padding:10px;text-align:right;">Unit Price</th>
<th style="padding:10px;text-align:right;">Total</th>
</tr></thead>
<tbody>${renderRows(rows, isNew)}</tbody>
</table>
</div>
</div>` : "";

list.innerHTML = `
<div style="padding:12px;">
<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px;margin-bottom:12px;">
<div style="padding:12px;border:1px solid #e5e7eb;border-radius:8px;background:#f8fafc;"><small style="color:#6b7280;">Suppliers</small><div style="font-size:20px;font-weight:700;">${new Set(products.map(p => String(p.supplierName || "").trim()).filter(Boolean)).size}</div></div>
<div style="padding:12px;border:1px solid #e5e7eb;border-radius:8px;background:#f0fdf4;"><small style="color:#6b7280;">Existing Products</small><div style="font-size:20px;font-weight:700;">${existingProducts.length}</div></div>
<div style="padding:12px;border:1px solid #e5e7eb;border-radius:8px;background:#fff7ed;"><small style="color:#6b7280;">New Products</small><div style="font-size:20px;font-weight:700;">${newProducts.length}</div></div>
<div style="padding:12px;border:1px solid #e5e7eb;border-radius:8px;background:#f8fafc;"><small style="color:#6b7280;">Import Total</small><div style="font-size:20px;font-weight:700;">${formatMoney(total)}</div></div>
</div>
<p style="margin:8px 0 14px;color:#4b5563;">All detected suppliers and products are listed below. Existing products will increase their stock; new products will be created. Click <strong>Final Import</strong> to save everything.</p>
${renderTable("Already Existing Products — Stock Will Increase", existingProducts, false)}
${renderTable("New Products — Will Be Created", newProducts, true)}
</div>`;

const count = document.getElementById("importProductCount");
if (count) count.textContent = products.length;
const grand = document.getElementById("importGrandTotal");
if (grand) grand.textContent = formatMoney(total);
modal.classList.add("show");
}
function closePurchaseImportModal() {
const modal = document.getElementById("purchaseImportModal");
if (modal) modal.classList.remove("show");
}
async function finalImportPurchase() {
closePurchaseImportModal();
if (!pdfImportSelectedProducts.length) {
showNotification("There are no products selected for import.", "warning");
return;
}
await importPDFPurchase();
}
/* =========================================================
PURCHASE FILE REVIEW UI
========================================================= */

function renderPDFPurchaseReview() {
const pdfResult =
document.getElementById(
"pdfResult"
);

if (!pdfResult) {
return;
}
let review =
document.getElementById(
"pdfPurchaseReview"
);
if (!review) {
review =
document.createElement("div");
review.id =
"pdfPurchaseReview";
pdfResult.appendChild(review);
}
const data =
pdfPurchaseData || {};
const products =
Array.isArray(pdfImportSelectedProducts) && pdfImportSelectedProducts.length
? pdfImportSelectedProducts
: (Array.isArray(pdfReviewProducts) ? pdfReviewProducts : []);
review.innerHTML = `
<div style="margin-top:20px;padding:18px;border:1px solid #dbeafe;border-radius:12px;background:#f8fbff;">
<div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:14px;">
<div>
<h3 style="margin:0 0 4px;">Purchase Import Review</h3>
<p style="margin:0;color:#6b7280;font-size:13px;">
Review the extracted products before adding them to inventory.
</p>
</div>
<span style="font-size:13px;font-weight:600;color:#2563eb;">
${products.length} product${products.length === 1 ? "" : "s"} detected
</span>
</div>
<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:10px;margin-bottom:15px;">
<div style="padding:10px 12px;background:white;border:1px solid #e5e7eb;border-radius:8px;">
<small style="color:#6b7280;">Supplier</small>
<div style="font-weight:600;">${data.suppliers && data.suppliers.length > 1 ? `${data.suppliers.length} suppliers detected` : escapeHtml(data.supplierName || "Not detected")}</div>
</div>
<div style="padding:10px 12px;background:white;border:1px solid #e5e7eb;border-radius:8px;">
<small style="color:#6b7280;">Invoice</small>
<div style="font-weight:600;">${escapeHtml(data.invoiceNumber || "Not detected")}</div>
</div>
<div style="padding:10px 12px;background:white;border:1px solid #e5e7eb;border-radius:8px;">
<small style="color:#6b7280;">Date</small>
<div style="font-weight:600;">${escapeHtml(data.purchaseDate || "Not detected")}</div>
</div>
</div>
${products.length ? `
<div style="overflow:auto;border:1px solid #e5e7eb;border-radius:8px;background:white;">
<table style="width:100%;border-collapse:collapse;min-width:700px;">
<thead>
<tr style="background:#f3f4f6;">
<th style="padding:10px;text-align:left;">#</th>
<th style="padding:10px;text-align:left;">Product</th>
<th style="padding:10px;text-align:left;">Type</th>
<th style="padding:10px;text-align:right;">Qty</th>
<th style="padding:10px;text-align:right;">Unit Price</th>
<th style="padding:10px;text-align:right;">Total</th>
<th style="padding:10px;text-align:center;">Action</th>
</tr>
</thead>
<tbody>
${products.map((product, index) => `
<tr data-pdf-index="${index}" style="border-top:1px solid #e5e7eb;">
<td style="padding:8px;">${index + 1}</td>
<td style="padding:8px;font-weight:600;">${escapeHtml(product.supplierName || data.supplierName || "-")}</td>
<td style="padding:8px;">
<input
type="text"
value="${escapeHtml(product.productName)}"
onchange="updatePDFReviewProduct(${index}, 'productName', this.value)"
style="width:100%;min-width:180px;padding:7px;border:1px solid #d1d5db;border-radius:6px;"
>
</td>
<td style="padding:8px;">
<input
type="text"
value="${escapeHtml(product.productType || "")}"
onchange="updatePDFReviewProduct(${index}, 'productType', this.value)"
style="width:100%;min-width:120px;padding:7px;border:1px solid #d1d5db;border-radius:6px;"
>
</td>
<td style="padding:8px;text-align:right;">
<input
type="number"
min="1"
step="1"
value="${Number(product.quantity) || 0}"
onchange="updatePDFReviewProduct(${index}, 'quantity', this.value)"
style="width:85px;padding:7px;border:1px solid #d1d5db;border-radius:6px;text-align:right;"
>

</td>
<td style="padding:8px;text-align:right;">
<input
type="number"
min="0"
step="0.01"
value="${Number(product.unitPrice) || 0}"
onchange="updatePDFReviewProduct(${index}, 'unitPrice', this.value)"

style="width:105px;padding:7px;border:1px solid #d1d5db;border-radius:6px;text-align:right;"
>
</td>
<td style="padding:8px;text-align:right;font-weight:600;" id="pdf-product-total-${index}">
${formatMoney(Number(product.quantity || 0) * Number(product.unitPrice || 0))}
</td>
<td style="padding:8px;text-align:center;">
<button
type="button"
class="delete-btn"
onclick="removePDFReviewProduct(${index})">
Remove
</button>
</td>
</tr>
`).join("")}
</tbody>
</table>
</div>
<div style="display:flex;justify-content:flex-end;gap:10px;align-items:center;flex-wrap:wrap;margin-top:15px;">
<strong>
Purchase Total:
${formatMoney(
products.reduce(
(sum, item) =>
sum + Number(item.quantity || 0) * Number(item.unitPrice || 0),
0
)
)}
</strong>
<button
type="button"
class="primary-btn"
id="importPurchaseBtn"
onclick="importPDFPurchase()">
Import Purchase
</button>
</div>
` : `
<div style="padding:16px;background:white;border:1px solid #e5e7eb;border-radius:8px;color:#6b7280;">
No product rows were detected from this file.
</div>
`}
</div>
`;
}
function updatePDFReviewProduct(index, field, value) {
const product =
pdfReviewProducts[index];
if (!product) {
return;
}
if (field === "productName" || field === "productType") {
product[field] =
String(value || "").trim();
} else if (field === "quantity") {
product.quantity =
Math.max(0, Number(value) || 0);
} else if (field === "unitPrice") {
product.unitPrice =
Math.max(0, Number(value) || 0);
}
product.total =
Number(product.quantity || 0) *
Number(product.unitPrice || 0);
const totalElement =
document.getElementById(
`pdf-product-total-${index}`
);
if (totalElement) {
totalElement.textContent =
formatMoney(product.total);
}
if (pdfPurchaseData) {
pdfPurchaseData.products =
pdfReviewProducts;
}
updatePDFReviewGrandTotal();
}
function updatePDFReviewGrandTotal() {
const review =
document.getElementById(
"pdfPurchaseReview"
);
if (!review) {
return;

}
const total =
pdfReviewProducts.reduce(
(sum, item) =>
sum +
Number(item.quantity || 0) *
Number(item.unitPrice || 0),

0
);
const strongElements =
review.querySelectorAll("strong");
for (const element of strongElements) {
if (element.textContent.includes("Purchase Total:")) {
element.innerHTML =
`Purchase Total: ${formatMoney(total)}`;
break;
}
}
}
function removePDFReviewProduct(index) {
if (!Array.isArray(pdfReviewProducts)) {
return;
}
pdfReviewProducts.splice(index, 1);
if (pdfPurchaseData) {
pdfPurchaseData.products =
pdfReviewProducts;
}
renderPDFPurchaseReview();
}
/* =========================================================
IMPORT PURCHASE FILE
========================================================= */
async function importPDFPurchase() {
    if (pdfImportInProgress) return;
    const products = Array.isArray(pdfImportSelectedProducts) && pdfImportSelectedProducts.length ? pdfImportSelectedProducts : (Array.isArray(pdfReviewProducts) ? pdfReviewProducts : []);
    if (!products.length) { showNotification("There are no products to import.", "warning"); return; }

    const fallbackIds = ["supplierName","supplierPhone","supplierLandline","supplierEmail","supplierFax","supplierGst","supplierStreet","supplierDoorNumber","supplierVillage","supplierDistrict","supplierCity","supplierPincode","supplierCountry"];
    const requiredSupplierIds = ["supplierName","supplierPhone","supplierEmail","supplierGst","supplierStreet","supplierDoorNumber","supplierDistrict","supplierCity","supplierPincode","supplierCountry"];
    const fallback = {}; fallbackIds.forEach(id => fallback[id] = document.getElementById(id)?.value.trim() || "");
    const invoiceNumber = document.getElementById("purchaseInvoiceNumber")?.value.trim() || pdfPurchaseData?.invoiceNumber || "";
    if (!invoiceNumber) {
        showNotification("Invoice Number is required before import.", "warning");
        document.getElementById("purchaseInvoiceNumber")?.focus();
        return;
    }

    const groups = new Map();
    products.forEach(product => {
        const name = String(product.supplierName || fallback.supplierName || pdfPurchaseData?.supplierName || "").trim();
        const key = name.toLowerCase();
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(product);
    });
    if ([...groups.keys()].some(k => !k)) { showNotification("Supplier name is required for every imported supplier.", "warning"); return; }

    const payloads = [];
    for (const groupProducts of groups.values()) {
        const first = groupProducts[0];
        const supplier = {};
        fallbackIds.forEach(id => { const productKey = id; supplier[id] = String(first[productKey] || fallback[id] || pdfPurchaseData?.[id] || "").trim(); });

        /* If the file identifies an already-known supplier but does not repeat
         * all of its details on every row, use the saved supplier record. */
        if (supplier.supplierName) {
            const hasMissingRequired = requiredSupplierIds.some(id => !supplier[id]);
            if (hasMissingRequired) {
                try {
                    const supplierSearch = await apiRequest("/data/suppliers?search=" + encodeURIComponent(supplier.supplierName));
                    const matches = Array.isArray(supplierSearch.suppliers) ? supplierSearch.suppliers : [];
                    const match = matches.find(item => String(item.supplierName || "").trim().toLowerCase() === supplier.supplierName.toLowerCase()) || matches[0];
                    if (match?.supplierId) {
                        const detailResult = await apiRequest("/data/suppliers/details?id=" + encodeURIComponent(match.supplierId));
                        const saved = detailResult?.supplier || detailResult?.supplierDetails || detailResult;
                        if (saved && typeof saved === "object") {
                            const map = {
                                supplierName: "supplierName", supplierPhone: "phone", supplierLandline: "landline",
                                supplierEmail: "email", supplierFax: "fax", supplierGst: "gst", supplierStreet: "street",
                                supplierDoorNumber: "doorNumber", supplierVillage: "village", supplierDistrict: "district",
                                supplierCity: "city", supplierPincode: "pincode", supplierCountry: "country"
                            };
                            Object.entries(map).forEach(([target, source]) => {
                                if (!supplier[target] && saved[source] != null) supplier[target] = String(saved[source]).trim();
                            });
                        }
                    }
                } catch (lookupError) {
                    console.warn("Could not load saved supplier details for import:", lookupError);
                }
            }
        }

        const missing = requiredSupplierIds.find(id => !supplier[id]);
        if (missing) { showNotification(`Supplier details are incomplete. Missing ${missing.replace("supplier", "")}.`, "warning"); return; }
        payloads.push({ supplierName:supplier.supplierName, supplierPhone:supplier.supplierPhone, supplierLandline:supplier.supplierLandline, supplierEmail:supplier.supplierEmail, supplierFax:supplier.supplierFax, supplierGst:supplier.supplierGst, supplierStreet:supplier.supplierStreet, supplierDoorNumber:supplier.supplierDoorNumber, supplierVillage:supplier.supplierVillage, supplierDistrict:supplier.supplierDistrict, supplierCity:supplier.supplierCity, supplierPincode:supplier.supplierPincode, supplierCountry:supplier.supplierCountry, invoiceNumber:String(first.invoiceNumber || invoiceNumber || "").trim(), purchaseDate:normalizePurchaseDate(first.purchaseDate || pdfPurchaseData?.purchaseDate || ""), products:groupProducts.map(product => ({productName:String(product.productName||"").trim(),productType:String(product.productType||"").trim(),quantity:Number(product.quantity),unitPrice:Number(product.unitPrice),total:Number(product.quantity)*Number(product.unitPrice)})) });
    }
    for (const payload of payloads) {
        const invalid = payload.products.find(p => !p.productName || p.quantity <= 0 || !Number.isFinite(p.quantity) || p.unitPrice < 0 || !Number.isFinite(p.unitPrice));
        if (invalid) { showNotification("Please correct all imported product names, quantities and prices.", "warning"); return; }
    }
    if (!confirm(`Import ${products.length} product${products.length === 1 ? "" : "s"} from ${payloads.length} supplier${payloads.length === 1 ? "" : "s"}?\n\nExisting products will have their stock increased.`)) return;

    const button=document.getElementById("importPurchaseBtn"); pdfImportInProgress=true; if(button){button.disabled=true;button.textContent="Importing...";}
    try {
        for (const payload of payloads) {
            const result=await apiRequest("/data/purchases/import",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
            if(!result || !result.ok) throw new Error(result?.error || "Purchase import failed.");
        }
        showNotification("Purchase import completed. Stock updated.");
        await loadProducts(); await loadDashboard(); if(typeof loadSuppliers === "function") await loadSuppliers();
        if (currentSupplierDetailsData?.supplier?.supplierId && typeof viewSupplierDetails === "function") {
            try { await viewSupplierDetails(currentSupplierDetailsData.supplier.supplierId); } catch (refreshError) { console.warn("Supplier purchase details refresh failed:", refreshError); }
        }
        if (document.getElementById("purchasePage")?.classList.contains("active-page")) {
            calculatePurchaseTotal();
        }
        pdfPurchaseData=null; pdfReviewProducts=[]; pdfImportExistingProducts=[]; pdfImportNewProducts=[]; pdfImportSelectedProducts=[]; clearPurchaseForm();
        const fileInput=document.getElementById("supplierPdf"); if(fileInput) fileInput.value=""; showPurchaseImportSuccess({ok:true});
    } catch(error){ console.error("Purchase File Import Error:",error); showNotification(error?.message || "Purchase import failed.","warning"); if(button){button.disabled=false;button.textContent="Import Purchase";} } finally { pdfImportInProgress=false; }
}

function showPurchaseImportSuccess(result) {
const pdfResult =
document.getElementById(
"pdfResult"
);
if (!pdfResult) {
return;
}
let successBox =
document.getElementById(
"purchaseImportSuccess"
);
if (!successBox) {
successBox =
document.createElement("div");
successBox.id =
"purchaseImportSuccess";
pdfResult.appendChild(successBox);
}
successBox.innerHTML = `
<div style="margin-top:15px;padding:16px;border:1px solid #bbf7d0;border-radius:10px;background:#f0fdf4;">
<strong style="color:#166534;">Purchase imported successfully.</strong>
<p style="margin:6px 0 12px;color:#166534;font-size:13px;">
Inventory stock and dashboard values have been refreshed.
</p>
<div style="display:flex;gap:10px;flex-wrap:wrap;">
<button type="button" class="primary-btn" onclick="showPageByName('products');loadProducts();">
View Products
</button>
<button type="button" class="primary-btn" onclick="showPageByName('dashboard');loadDashboard();">
View Dashboard
</button>
</div>
</div>
`;
}
/* =========================================================
GENERATE INVOICE
========================================================= */
function generateInvoice() {
if (!saleItems.length) {
showNotification(
"Create a sale before generating an invoice.",
"warning"
);
return;
}
const totals =
calculateSaleTotal();
const customerName =
document.getElementById(
"customerName"
)?.value.trim() ||
"Walk-in Customer";
const customerType =
document.getElementById(
"customerType"
)?.value ||
"DIRECT";
const customerPhone =
document.getElementById(
"customerPhone"
)?.value.trim() ||
"-";
const saleData = {
customerName,
customerType,

customerPhone,
subtotal:
totals.subtotal,
discount:

totals.discount,
gst:
totals.gst,
finalTotal:
totals.finalTotal,
items:
saleItems.filter(
item =>
item.productId
)
};
generateInvoiceFromSale(
saleData,
{}
);
}
/* =========================================================
INVOICE
========================================================= */
function generateInvoiceFromSale(
saleData,
result
) {
const invoiceNumber =
result?.saleId ||
result?.sale_id ||
Date.now();
const invoiceDate =
new Date()
.toLocaleDateString(
"en-IN"
);
const itemRows =
saleData.items
.map(
(item, index) => {
const product =
allProducts.find(
p =>
String(
p.product_id
) ===
String(
item.productId
)
);
const productName =
product?.product_name ||
"Product";
const total =
Number(
item.total ||
(
Number(
item.unitPrice
) *
Number(
item.quantity
) -
Number(
item.discount || 0
) *
Number(
item.quantity
)
)
);
return `
<tr>
<td>
${index + 1}
</td>
<td>
${escapeHtml(
productName
)}
</td>
<td>
${item.quantity}
</td>
<td>
${formatMoney(
item.unitPrice
)}

</td>
<td>
${formatMoney(
item.discount
)}
</td>

<td>
${formatMoney(
total
)}
</td>
</tr>
`;
}
)
.join("");
const invoiceWindow =
window.open(
"",
"_blank",
"width=900,height=700"
);
if (!invoiceWindow) {
showNotification(
"Please allow pop-ups to generate invoice.",
"warning"
);
return;
}
invoiceWindow.document.write(`
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>
HardwarePro Invoice
</title>
<style>
* {
box-sizing: border-box;
}
body {
font-family:
Arial,
sans-serif;
margin: 0;
padding: 35px;
color: #111827;
}
.invoice {
max-width: 800px;
margin: auto;
border: 1px solid #e5e7eb;
padding: 35px;
}
.header {
display: flex;
justify-content:
space-between;
border-bottom:
2px solid #2563eb;
padding-bottom: 20px;
margin-bottom: 25px;
}
.logo {
font-size: 25px;
font-weight: bold;
color: #2563eb;
}
.title {
font-size: 28px;
font-weight: bold;
}

.info {
display: flex;
justify-content:
space-between;

margin-bottom: 25px;
}
.info-box {
width: 48%;
}
.info-box h4 {
margin:
0 0 7px;
color: #6b7280;
font-size: 12px;
text-transform:
uppercase;
}
.info-box p {
margin:
4px 0;
font-size: 13px;
}
table {
width: 100%;
border-collapse:
collapse;
margin-top: 20px;
}
th {
background: #f3f4f6;
padding: 11px;
text-align: left;
font-size: 12px;
}
td {
padding: 11px;
border-bottom:
1px solid #e5e7eb;
font-size: 12px;
}
.total {
width: 320px;
margin-left: auto;
margin-top: 25px;
}
.total-row {
display: flex;
justify-content:
space-between;
padding: 7px 0;
font-size: 13px;
}
.grand-total {
border-top:
2px solid #111827;
margin-top: 8px;
padding-top: 12px;
font-size: 18px;
font-weight: bold;
}
.footer {
margin-top: 50px;
padding-top: 15px;
border-top:

1px solid #e5e7eb;
text-align: center;
font-size: 11px;
color: #6b7280;
}

.print-btn {
position: fixed;
right: 25px;
top: 20px;
padding:
10px 18px;
border: none;
background: #2563eb;
color: white;
border-radius: 7px;
cursor: pointer;
}
@media print {
.print-btn {
display: none;
}
body {
padding: 0;
}
.invoice {
border: none;
}
}
</style>
</head>
<body>
<button
class="print-btn"
onclick="window.print()">
Print Invoice
</button>
<div class="invoice">
<div class="header">
<div>
<div class="logo">
HardwarePro
</div>
<p>
Hardware Management System
</p>
</div>
<div>
<div class="title">
INVOICE
</div>
<p>
Invoice #${invoiceNumber}
</p>
<p>
Date: ${invoiceDate}
</p>
</div>
</div>
<div class="info">
<div class="info-box">
<h4>
Customer
</h4>
<p>
<strong>
${escapeHtml(
saleData.customerName
)}
</strong>
</p>
<p>
Type:

${escapeHtml(
saleData.customerType
)}
</p>
<p>
Phone:

${escapeHtml(
saleData.customerPhone
)}
</p>
</div>
<div class="info-box">
<h4>
Business
</h4>
<p>
<strong>
HardwarePro
</strong>
</p>
<p>
Hardware & Electrical Store
</p>
<p>
India
</p>
</div>
</div>
<table>
<thead>
<tr>
<th>#</th>
<th>
Product
</th>
<th>
Qty
</th>
<th>
Unit Price
</th>
<th>
Discount
</th>
<th>
Total
</th>
</tr>
</thead>
<tbody>
${itemRows}
</tbody>
</table>
<div class="total">
<div class="total-row">
<span>
Subtotal
</span>
<strong>
${formatMoney(
saleData.subtotal
)}
</strong>
</div>
<div class="total-row">
<span>
Discount
</span>
<strong>
${formatMoney(
saleData.discount
)}
</strong>

</div>
<div class="total-row">
<span>
GST

</span>
<strong>
${formatMoney(
saleData.gst
)}
</strong>
</div>
<div class="total-row grand-total">
<span>
Final Total
</span>
<strong>
${formatMoney(
saleData.finalTotal
)}
</strong>
</div>
</div>
<div class="footer">
Thank you for your business.
<br>
HardwarePro —
Hardware Management System
</div>
</div>
</body>
</html>
`);
invoiceWindow.document.close();
}
/* =========================================================
MODAL CLICK OUTSIDE
========================================================= */
window.addEventListener(
"click",
function(event) {
const modal =
document.getElementById(
"productModal"
);
if (
modal &&
event.target === modal
) {
closeProductModal();
closeNewProductsModal();
closePurchaseImportModal();
}
const newProductsModal = document.getElementById("newProductsModal");
if (newProductsModal && event.target === newProductsModal) {
closeNewProductsModal();
}
const purchaseImportModal = document.getElementById("purchaseImportModal");
if (purchaseImportModal && event.target === purchaseImportModal) {
closePurchaseImportModal();
}
}
);
/* =========================================================
ESC KEY
========================================================= */
document.addEventListener(
"keydown",
function(event) {
if (
event.key ===
"Escape"
) {
closeProductModal();
}
}
);

/* =========================================================
PRODUCT FORM SUBMIT
========================================================= */
document.addEventListener(
"DOMContentLoaded",

function() {
const productForm =
document.getElementById(
"productForm"
);
if (productForm) {
productForm.addEventListener(
"submit",
addProduct
);
}
}
);
/* =========================================================
PURCHASE INPUT EVENTS
========================================================= */
document.addEventListener(
"DOMContentLoaded",
function() {
const quantity =
document.getElementById(
"purchaseQuantity"
);
const price =
document.getElementById(
"purchaseUnitPrice"
);
if (quantity) {
quantity.addEventListener(
"input",
calculatePurchaseTotal
);
}
if (price) {
price.addEventListener(
"input",
calculatePurchaseTotal
);
}
}
);
/* =========================================================
WINDOW RESIZE
========================================================= */
window.addEventListener(
"resize",
function() {
const dashboard =
document.getElementById(
"dashboardPage"
);
if (
dashboard &&
dashboard.classList.contains(
"active-page"
)
) {
loadDashboard();
}
}
);
/* =========================================================
INITIALIZATION
========================================================= */
document.addEventListener(
"DOMContentLoaded",
function() {
console.log(
"Hardware Management System started."
);
populateSupplierCountryList();
updateDate();
saleItems = [];
addSaleRow();
loadDashboard()
.catch(error => {
console.error(

"Dashboard failed to load:",
error
);
});
}
);

/* =========================================================
MAKE FUNCTIONS AVAILABLE TO HTML onclick=""
========================================================= */
window.showPage =
showPage;
window.showPageByName =
showPageByName;
window.toggleAllProducts =
toggleAllProducts;
window.openProductModal =
openProductModal;
window.closeProductModal =
closeProductModal;
window.addProduct =
addProduct;
window.deleteProduct =
deleteProduct;
window.searchProducts =
searchProducts;
window.addSaleRow =
addSaleRow;
window.selectSaleProduct =
selectSaleProduct;
window.updateSaleQuantity =
updateSaleQuantity;
window.removeSaleRow =
removeSaleRow;
window.calculateSaleTotal =
calculateSaleTotal;
window.saveSale =
saveSale;
window.generateInvoice =
generateInvoice;
window.loadSalesHistory =
loadSalesHistory;
window.clearSalesHistoryFilter =
clearSalesHistoryFilter;
window.viewSaleDetails =
viewSaleDetails;
window.closeSaleDetailsModal =
closeSaleDetailsModal;
window.generateInvoiceFromHistory =
generateInvoiceFromHistory;
window.deleteSale =
deleteSale;
window.savePurchase =
savePurchase;
window.calculatePurchaseTotal =
calculatePurchaseTotal;
window.addPurchaseProductRow =
addPurchaseProductRow;
window.closeNewProductsModal =
closeNewProductsModal;
window.continueWithExistingProducts =
continueWithExistingProducts;
window.createNewPDFProducts =
createNewPDFProducts;
window.closePurchaseImportModal =
closePurchaseImportModal;
window.finalImportPurchase =
finalImportPurchase;
window.readSupplierFile =
readSupplierFile;
window.readSupplierPDF =
readSupplierPDF;
window.importPDFPurchase =
importPDFPurchase;
window.updatePDFReviewProduct =
updatePDFReviewProduct;
window.removePDFReviewProduct =
removePDFReviewProduct;
window.clearSaleForm =
clearSaleForm;
window.clearPurchaseForm =
clearPurchaseForm;
/* =========================================================
SUPPLIER DIRECTORY
========================================================= */
let supplierRows = [];
let currentSupplierDetailsData = null;
async function loadSuppliers() {
    const table = document.getElementById("suppliersTable");
    if (!table) return;
    const search = document.getElementById("supplierSearch")?.value.trim() || "";
    try {
        const data = await apiRequest("/data/suppliers" + (search ? "?search=" + encodeURIComponent(search) : ""));
        supplierRows = Array.isArray(data.suppliers) ? data.suppliers : [];
        const list = document.getElementById("supplierSuggestionList");
        if (list) list.innerHTML = supplierRows.map(s => `<option value="${escapeHtml(s.supplierName || "")}"></option>`).join("");
        renderSupplierList();
    } catch (e) { table.innerHTML = `<tr><td colspan="7" style="padding:25px;text-align:center;color:#dc2626;">Could not load suppliers.</td></tr>`; }
}
function renderSupplierList() {
    const table=document.getElementById("suppliersTable"); if(!table) return;
    let rows=[...supplierRows]; const sort=document.getElementById("supplierSort")?.value || "none";
    if (sort !== "none") {
        rows.sort((a,b)=> sort==="purchases" ? Number(b.purchaseCount||0)-Number(a.purchaseCount||0) : sort==="city" ? String(a.city||"").localeCompare(String(b.city||"")) : String(a.supplierName||"").localeCompare(String(b.supplierName||"")));
    }
    if(!rows.length){table.innerHTML='<tr><td colspan="7" style="padding:30px;text-align:center;color:#6b7280;">No suppliers found.</td></tr>';return;}
    table.innerHTML=rows.map((s,i)=>`<tr><td>${i+1}</td><td><button type="button" onclick="viewSupplierDetails(${Number(s.supplierId)})" style="border:0;background:none;padding:0;font-weight:700;cursor:pointer;text-align:left;">${escapeHtml(s.supplierName||"-")}</button></td><td>${escapeHtml(s.phone||"-")}</td><td>${escapeHtml(s.email||"-")}</td><td>${escapeHtml(s.city||"-")}</td><td>${Number(s.purchaseCount||0)}</td><td><button class="secondary-btn" type="button" onclick="viewSupplierDetails(${Number(s.supplierId)})">Details</button></td></tr>`).join("");
}
function resetSupplierControls(){ const search=document.getElementById("supplierSearch"); const sort=document.getElementById("supplierSort"); if(search)search.value=""; if(sort)sort.value="none"; loadSuppliers(); }
function closeDirectoryDetailsModal(){ const modal=document.getElementById("directoryDetailsModal"); if(modal)modal.classList.remove("show"); }
function ensureDirectoryDetailsModal(){
    let modal=document.getElementById("directoryDetailsModal");
    if(modal)return modal;
    modal=document.createElement("div"); modal.id="directoryDetailsModal"; modal.className="modal";
    modal.innerHTML=`<div class="modal-content" style="max-width:1000px;max-height:85vh;overflow:auto;"><div style="display:flex;justify-content:space-between;align-items:center;gap:15px;margin-bottom:20px;"><div><h2 id="directoryDetailsTitle" style="margin:0;"></h2><p id="directoryDetailsSubtitle" style="margin:5px 0 0;color:#6b7280;"></p></div><button type="button" class="secondary-btn" onclick="closeDirectoryDetailsModal()">Close</button></div><div id="directoryDetailsBody"></div></div>`;
    document.body.appendChild(modal); return modal;
}
async function viewSupplierDetails(id){
    try {
        const data=await apiRequest("/data/suppliers/details?id="+encodeURIComponent(id));
        if(!data||data.ok===false){showNotification(data?.error||"Could not load supplier details.","warning");return;}
        currentSupplierDetailsData={supplier:data.supplier||{}, purchases:Array.isArray(data.purchases)?data.purchases:[]};
        renderSupplierDetailsHome();
    } catch(e){ showNotification(e?.message||"Could not load supplier details.","warning"); }
}

function supplierPurchaseDateKey(value){
    const text=String(value||"").trim();
    const match=text.match(/^(\\d{4}-\\d{2}-\\d{2})/);
    if(match) return match[1];
    const parsed=new Date(value);
    return Number.isNaN(parsed.getTime()) ? text : parsed.toISOString().slice(0,10);
}

function supplierPurchaseDateLabel(dateKey){
    if(!dateKey) return "-";
    const parsed=new Date(dateKey+"T00:00:00");
    if(Number.isNaN(parsed.getTime())) return dateKey;
    return parsed.toLocaleDateString("en-IN",{day:"2-digit",month:"short",year:"numeric"});
}

function renderSupplierDetailsHome(){
    const state=currentSupplierDetailsData;
    if(!state) return;
    const s=state.supplier||{};
    const purchases=Array.isArray(state.purchases)?state.purchases:[];
    const modal=ensureDirectoryDetailsModal();
    document.getElementById("directoryDetailsTitle").textContent=s.supplierName||"Supplier Details";
    document.getElementById("directoryDetailsSubtitle").textContent="Supplier details and date-wise purchase history";

    const grouped={};
    purchases.forEach(p=>{
        const key=supplierPurchaseDateKey(p.purchaseDate);
        if(!grouped[key]) grouped[key]=[];
        grouped[key].push(p);
    });
    const dates=Object.keys(grouped).sort((a,b)=>b.localeCompare(a));

    document.getElementById("directoryDetailsBody").innerHTML=`
        <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px 28px;">
            <div><b>Name</b><br>${escapeHtml(s.supplierName||"-")}</div>
            <div><b>Phone</b><br>${escapeHtml(s.phone||"-")}</div>
            <div><b>Landline</b><br>${escapeHtml(s.landline||"-")}</div>
            <div><b>Email</b><br>${escapeHtml(s.email||"-")}</div>
            <div><b>Fax</b><br>${escapeHtml(s.fax||"-")}</div>
            <div><b>GST</b><br>${escapeHtml(s.gst||"-")}</div>
            <div style="grid-column:1/-1;"><b>Address</b><br>${escapeHtml([s.street,s.doorNumber,s.village,s.district,s.city,s.pincode,s.country].filter(Boolean).join(", ")||"-")}</div>
        </div>
        <div style="margin-top:24px;padding-top:18px;border-top:1px solid #e5e7eb;">
            <h3 style="margin:0 0 12px;">Purchase Dates</h3>
            ${dates.length ? `<div style="display:flex;flex-direction:column;gap:9px;">${dates.map(dateKey=>{
                const dayPurchases=grouped[dateKey];
                const dayTotal=dayPurchases.reduce((sum,p)=>sum+Number(p.totalAmount||0),0);
                return `<div style="display:flex;justify-content:space-between;align-items:center;gap:12px;padding:11px 12px;border:1px solid #e5e7eb;border-radius:8px;flex-wrap:wrap;">
                    <div><b>${escapeHtml(supplierPurchaseDateLabel(dateKey))}</b><div style="font-size:12px;color:#6b7280;margin-top:3px;">${dayPurchases.length} purchase${dayPurchases.length===1?"":"s"} · Total ${formatMoney(dayTotal)}</div></div>
                    <button class="secondary-btn" type="button" onclick="showSupplierPurchaseDateDetails('${escapeHtml(dateKey)}')">Details</button>
                </div>`;
            }).join("")}</div>` : '<span style="color:#6b7280;">No purchase dates available.</span>'}
        </div>`;
    modal.classList.add("show");
}

function showSupplierPurchaseDateDetails(dateKey){
    const state=currentSupplierDetailsData;
    if(!state) return;
    const purchases=(state.purchases||[]).filter(p=>supplierPurchaseDateKey(p.purchaseDate)===dateKey);
    const s=state.supplier||{};
    const modal=ensureDirectoryDetailsModal();
    const grandTotal=purchases.reduce((sum,p)=>sum+Number(p.totalAmount||0),0);
    document.getElementById("directoryDetailsTitle").textContent=s.supplierName||"Supplier Details";
    document.getElementById("directoryDetailsSubtitle").textContent=`Purchase details for ${supplierPurchaseDateLabel(dateKey)}`;

    const purchaseSections=purchases.map((purchase,pIndex)=>{
        const items=Array.isArray(purchase.items)?purchase.items:[];
        return `<div style="margin-top:${pIndex?"18px":"0"};border:1px solid #e5e7eb;border-radius:10px;overflow:hidden;">
            <div style="padding:12px 14px;background:#f8fafc;display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;">
                <div><b>Invoice:</b> ${escapeHtml(purchase.invoiceNumber||"-")}</div>
                <div><b>Purchase Total:</b> ${formatMoney(purchase.totalAmount)}</div>
            </div>
            <div style="overflow:auto;">
                <table style="width:100%;border-collapse:collapse;min-width:760px;">
                    <thead><tr style="background:#f3f4f6;">
                        <th style="padding:9px;text-align:left;">#</th>
                        <th style="padding:9px;text-align:left;">Product</th>
                        <th style="padding:9px;text-align:left;">Product Type</th>
                        <th style="padding:9px;text-align:right;">Quantity</th>
                        <th style="padding:9px;text-align:right;">Unit Cost</th>
                        <th style="padding:9px;text-align:right;">Total Cost</th>
                    </tr></thead>
                    <tbody>${items.length?items.map((item,i)=>`<tr style="border-top:1px solid #e5e7eb;">
                        <td style="padding:9px;">${i+1}</td>
                        <td style="padding:9px;font-weight:600;">${escapeHtml(item.productName||"-")}</td>
                        <td style="padding:9px;">${escapeHtml(item.productType||"-")}</td>
                        <td style="padding:9px;text-align:right;">${Number(item.quantity||0)}</td>
                        <td style="padding:9px;text-align:right;">${formatMoney(item.unitPrice)}</td>
                        <td style="padding:9px;text-align:right;font-weight:600;">${formatMoney(item.totalPrice)}</td>
                    </tr>`).join(""):`<tr><td colspan="6" style="padding:18px;text-align:center;color:#6b7280;">No product details available.</td></tr>`}</tbody>
                </table>
            </div>
        </div>`;
    }).join("");

    document.getElementById("directoryDetailsBody").innerHTML=`
        <button type="button" class="secondary-btn" onclick="renderSupplierDetailsHome()" style="margin-bottom:16px;">← Back to Purchase Dates</button>
        <div style="padding:12px 14px;background:#eff6ff;border:1px solid #dbeafe;border-radius:9px;margin-bottom:14px;display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;">
            <b>${escapeHtml(supplierPurchaseDateLabel(dateKey))}</b>
            <b>Date Total: ${formatMoney(grandTotal)}</b>
        </div>
        ${purchaseSections || '<div style="padding:20px;text-align:center;color:#6b7280;">No purchase details available for this date.</div>'}`;
    modal.classList.add("show");
}

/* =========================================================
PURCHASE SUPPLIER SUGGESTIONS
========================================================= */
let purchaseSupplierSuggestionRows = [];
async function suggestPurchaseSuppliers() {
    const input = document.getElementById("supplierName");
    const box = document.getElementById("purchaseSupplierSuggestions");
    if (!input || !box) return;
    const name = input.value.trim();
    if (name.length < 2) { box.innerHTML = ""; return; }
    try {
        const data = await apiRequest("/data/suppliers?search=" + encodeURIComponent(name));
        const suppliers = Array.isArray(data.suppliers) ? data.suppliers : [];
        purchaseSupplierSuggestionRows = suppliers;
        box.innerHTML = suppliers.length ? `<div style="position:absolute;left:0;right:0;top:2px;background:#fff;border:1px solid #d1d5db;border-radius:8px;box-shadow:0 8px 20px rgba(0,0,0,.12);overflow:hidden;z-index:60;">${suppliers.map((supplier, index) => `<button type="button" style="display:block;width:100%;text-align:left;padding:10px 12px;border:0;background:#fff;cursor:pointer;border-bottom:1px solid #f1f5f9;" onclick="selectExistingPurchaseSupplier(${index})"><b>${escapeHtml(supplier.supplierName || "")}</b><br><small>${escapeHtml(supplier.phone || "")} · ${escapeHtml(supplier.city || "")}</small></button>`).join("")}</div>` : "";
    } catch (e) {
        box.innerHTML = "";
    }
}
async function selectExistingPurchaseSupplier(index) {
    const supplier = purchaseSupplierSuggestionRows[index];
    const box = document.getElementById("purchaseSupplierSuggestions");
    if (!supplier) return;
    try {
        const data = await apiRequest("/data/suppliers/details?id=" + encodeURIComponent(supplier.supplierId));
        if (!data || data.ok === false) throw new Error(data?.error || "Could not load supplier details.");
        const s = data.supplier || {};
        const values = {
            supplierName: s.supplierName,
            supplierPhone: s.phone,
            supplierLandline: s.landline,
            supplierEmail: s.email,
            supplierFax: s.fax,
            supplierGst: s.gst,
            supplierStreet: s.street,
            supplierDoorNumber: s.doorNumber,
            supplierVillage: s.village,
            supplierDistrict: s.district,
            supplierCity: s.city,
            supplierPincode: s.pincode,
            supplierCountry: s.country
        };
        Object.entries(values).forEach(([id, value]) => {
            const element = document.getElementById(id);
            if (element && value !== null && value !== undefined) element.value = value;
        });
        if (box) box.innerHTML = "";
        showNotification("Existing supplier details filled.");
    } catch (e) {
        showNotification(e?.message || "Could not load supplier details.", "warning");
    }
}

const SUPPLIER_COUNTRIES = [
"Afghanistan","Albania","Algeria","Andorra","Angola","Antigua and Barbuda","Argentina","Armenia","Australia","Austria","Azerbaijan",
"Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bhutan","Bolivia","Bosnia and Herzegovina","Botswana","Brazil","Brunei","Bulgaria","Burkina Faso","Burundi",
"Cabo Verde","Cambodia","Cameroon","Canada","Central African Republic","Chad","Chile","China","Colombia","Comoros","Congo","Costa Rica","Croatia","Cuba","Cyprus","Czechia",
"Denmark","Djibouti","Dominica","Dominican Republic","Ecuador","Egypt","El Salvador","Equatorial Guinea","Eritrea","Estonia","Eswatini","Ethiopia",
"Fiji","Finland","France","Gabon","Gambia","Georgia","Germany","Ghana","Greece","Grenada","Guatemala","Guinea","Guinea-Bissau","Guyana",
"Haiti","Honduras","Hungary","Iceland","India","Indonesia","Iran","Iraq","Ireland","Israel","Italy","Jamaica","Japan","Jordan",
"Kazakhstan","Kenya","Kiribati","Kuwait","Kyrgyzstan","Laos","Latvia","Lebanon","Lesotho","Liberia","Libya","Liechtenstein","Lithuania","Luxembourg",
"Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Marshall Islands","Mauritania","Mauritius","Mexico","Micronesia","Moldova","Monaco","Mongolia","Montenegro","Morocco","Mozambique","Myanmar",
"Namibia","Nauru","Nepal","Netherlands","New Zealand","Nicaragua","Niger","Nigeria","North Korea","North Macedonia","Norway","Oman","Pakistan","Palau","Palestine","Panama","Papua New Guinea","Paraguay","Peru","Philippines","Poland","Portugal",
"Qatar","Romania","Russia","Rwanda","Saint Kitts and Nevis","Saint Lucia","Saint Vincent and the Grenadines","Samoa","San Marino","Sao Tome and Principe","Saudi Arabia","Senegal","Serbia","Seychelles","Sierra Leone","Singapore","Slovakia","Slovenia","Solomon Islands","Somalia","South Africa","South Korea","South Sudan","Spain","Sri Lanka","Sudan","Suriname","Sweden","Switzerland","Syria",
"Taiwan","Tajikistan","Tanzania","Thailand","Timor-Leste","Togo","Tonga","Trinidad and Tobago","Tunisia","Turkey","Turkmenistan","Tuvalu","Uganda","Ukraine","United Arab Emirates","United Kingdom","United States","Uruguay","Uzbekistan","Vanuatu","Vatican City","Venezuela","Vietnam","Yemen","Zambia","Zimbabwe"
];
function populateSupplierCountryList() {
    const list = document.getElementById("supplierCountryList");
    if (!list) return;
    list.innerHTML = SUPPLIER_COUNTRIES.map(country => `<option value="${escapeHtml(country)}"></option>`).join("");
}

/* =========================================================
CUSTOMER DIRECTORY
========================================================= */
let customerRows=[];
let salesCustomerSuggestionRows=[];
async function loadCustomers(){
    const table=document.getElementById("customersTable"); if(!table)return;
    const search=document.getElementById("customerSearch")?.value.trim()||"";
    try{const data=await apiRequest("/data/customers"+(search?"?search="+encodeURIComponent(search):"")); customerRows=Array.isArray(data.customers)?data.customers:[]; const list=document.getElementById("customerSuggestionList"); if(list)list.innerHTML=customerRows.map(c=>`<option value="${escapeHtml(c.customerName||"")}"></option>`).join(""); renderCustomerList();}catch(e){table.innerHTML='<tr><td colspan="7" style="padding:25px;text-align:center;color:#dc2626;">Could not load customers.</td></tr>';}
}
function renderCustomerList(){
    const table=document.getElementById("customersTable"); if(!table)return; let rows=[...customerRows]; const sort=document.getElementById("customerSort")?.value||"none"; const filter=document.getElementById("customerFilter")?.value||"all"; rows=rows.filter(c=>filter==="all"||String(c.customerType||"").toUpperCase()===filter); if(sort!=="none") rows.sort((a,b)=>sort==="purchases"?Number(b.purchaseCount||0)-Number(a.purchaseCount||0):sort==="date"?String(b.lastPurchase||"").localeCompare(String(a.lastPurchase||"")):String(a.customerName||"").localeCompare(String(b.customerName||""))); if(!rows.length){table.innerHTML='<tr><td colspan="7" style="padding:30px;text-align:center;color:#6b7280;">No customers found.</td></tr>';return;} table.innerHTML=rows.map((c,i)=>`<tr><td>${i+1}</td><td><button type="button" onclick="viewCustomerDetails(${Number(c.customerId)})" style="border:0;background:none;padding:0;font-weight:700;cursor:pointer;text-align:left;">${escapeHtml(c.customerName||"-")}</button></td><td>${escapeHtml(c.phone||"-")}</td><td>${escapeHtml(c.customerType||"-")}</td><td>${Number(c.purchaseCount||0)}</td><td>${formatSaleDate(c.lastPurchase)}</td><td><button class="secondary-btn" type="button" onclick="viewCustomerDetails(${Number(c.customerId)})">Details</button></td></tr>`).join("");
}
function resetCustomerControls(){ const search=document.getElementById("customerSearch"); const sort=document.getElementById("customerSort"); const filter=document.getElementById("customerFilter"); if(search)search.value=""; if(sort)sort.value="none"; if(filter)filter.value="all"; loadCustomers(); }
async function viewCustomerDetails(id){
    try {
        const data=await apiRequest("/data/customers/details?id="+encodeURIComponent(id)); if(!data||data.ok===false){showNotification(data?.error||"Could not load customer details.","warning");return;} const c=data.customer||{}; const sales=Array.isArray(data.sales)?data.sales:[]; const modal=ensureDirectoryDetailsModal();
        document.getElementById("directoryDetailsTitle").textContent=c.customerName||"Customer Details";
        document.getElementById("directoryDetailsSubtitle").textContent="Customer details";
        const dates=[...new Set(sales.map(s=>formatSaleDate(s.saleDate)).filter(Boolean))];
        document.getElementById("directoryDetailsBody").innerHTML=`<div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px 28px;"><div><b>Name</b><br>${escapeHtml(c.customerName||"-")}</div><div><b>Phone</b><br>${escapeHtml(c.phone||"-")}</div><div><b>Email</b><br>${escapeHtml(c.email||"-")}</div><div><b>Customer Type</b><br>${escapeHtml(c.customerType||"-")}</div><div><b>GST</b><br>${escapeHtml(c.gst||"-")}</div></div><div style="margin-top:24px;padding-top:18px;border-top:1px solid #e5e7eb;"><h3 style="margin:0 0 12px;">Purchase Dates</h3><div style="display:flex;flex-direction:column;gap:9px;">${sales.length?sales.map(s=>`<div style="display:flex;justify-content:space-between;align-items:center;gap:12px;padding:10px 12px;border:1px solid #e5e7eb;border-radius:8px;flex-wrap:wrap;"><span>${escapeHtml(formatSaleDate(s.saleDate))}</span><button class="secondary-btn" type="button" onclick="generateInvoiceFromHistory(${Number(s.saleId)})">Generate Invoice</button></div>`).join(""):'<span style="color:#6b7280;">No purchase dates available.</span>'}</div></div>`;
        modal.classList.add("show");
    } catch(e){ showNotification(e?.message||"Could not load customer details.","warning"); }
}

/* =========================================================
SALES CUSTOMER PHONE SUGGESTIONS
========================================================= */
async function suggestSalesCustomers(){
    const phone=document.getElementById("customerPhone")?.value.trim()||""; const box=document.getElementById("salesCustomerSuggestions"); if(!box)return; if(phone.length<3){box.innerHTML="";return;}
    try{const data=await apiRequest("/data/customers?phone="+encodeURIComponent(phone)); const customers=Array.isArray(data.customers)?data.customers:[]; salesCustomerSuggestionRows=customers; box.innerHTML=customers.length?`<div style="position:absolute;left:0;right:0;top:2px;background:#fff;border:1px solid #d1d5db;border-radius:8px;box-shadow:0 8px 20px rgba(0,0,0,.12);overflow:hidden;z-index:50;">${customers.map((c,i)=>`<button type="button" style="display:block;width:100%;text-align:left;padding:10px 12px;border:0;background:#fff;cursor:pointer;border-bottom:1px solid #f1f5f9;" onclick="selectExistingSalesCustomerById(${i})"><b>${escapeHtml(c.customerName||"")}</b><br><small>${escapeHtml(c.phone||"")} · ${escapeHtml(c.customerType||"")}</small></button>`).join("")}</div>`:"";}catch(e){box.innerHTML="";}
}
function selectExistingSalesCustomer(customer){
    document.getElementById("customerName").value=customer.customerName||""; document.getElementById("customerType").value=customer.customerType||"DIRECT"; document.getElementById("customerPhone").value=customer.phone||""; document.getElementById("customerEmail").value=customer.email||""; document.getElementById("customerGst").value=customer.gst||""; const box=document.getElementById("salesCustomerSuggestions"); if(box)box.innerHTML="";
}
function selectExistingSalesCustomerById(index){
    const customer=salesCustomerSuggestionRows[index];
    if(customer) selectExistingSalesCustomer(customer);
}

window.loadSuppliers = loadSuppliers;
window.renderSupplierList = renderSupplierList;
window.viewSupplierDetails = viewSupplierDetails;
window.showSupplierPurchaseDateDetails = showSupplierPurchaseDateDetails;
window.renderSupplierDetailsHome = renderSupplierDetailsHome;
window.loadCustomers = loadCustomers;
window.renderCustomerList = renderCustomerList;
window.viewCustomerDetails = viewCustomerDetails;
window.suggestSalesCustomers = suggestSalesCustomers;
window.selectExistingSalesCustomer = selectExistingSalesCustomer;
window.applyProductSortFilter = applyProductSortFilter;
window.resetProductSortFilter = resetProductSortFilter;
window.resetDashboardProductControls = resetDashboardProductControls;
window.resetSupplierControls = resetSupplierControls;
window.resetCustomerControls = resetCustomerControls;
window.closeDirectoryDetailsModal = closeDirectoryDetailsModal;

window.selectExistingSalesCustomerById = selectExistingSalesCustomerById;
window.suggestPurchaseSuppliers = suggestPurchaseSuppliers;
window.selectExistingPurchaseSupplier = selectExistingPurchaseSupplier;
window.populateSupplierCountryList = populateSupplierCountryList;
