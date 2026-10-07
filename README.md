# Employee Data Management Portal

A full-stack, schema-agnostic enterprise web application built using **Spring Boot 3.2.4**, **React 18 (Vite)**, and **MySQL 8**. The portal allows administrators (Deans) to upload **any flat structured file** (`.xlsx`, `.csv`, `.json`, `.xml`) with arbitrary columns (up to 10 MB, 100,000 rows, 60 columns), automatically converting them through an internal XML pipeline and creating a dedicated dynamic database table for lightning-fast querying, filtering, pagination, CRUD, and dual exports.

---

> [!WARNING]
> ### DATABASE RESET INSTRUCTION
> Before the first start of this upgraded version, execute the following command in MySQL:
> ```sql
> DROP DATABASE employee_portal;
> ```
> Spring Boot will automatically recreate the database schema and initialize the required tables (`users`, `datasets`, `dataset_columns`) on startup.

---

## 1. Two Separate Portals (Frontend)

The application provides two completely separated portals with distinct visual themes and role boundaries:

- **Landing Page (`/`):** Two cards directing users to "Dean Portal" and "Employee Portal".
- **Dean Portal (`/dean`):**
  - Theme: **Indigo / Purple** with a `"DEAN PORTAL"` badge.
  - Login / Register tabs at `/dean/login` (Dean registration requires the Dean Access Code `DEAN2026`).
  - Dashboard at `/dean/dashboard`: Upload & atomic replace of structured files, Download Standard XML, Schema-driven workforce directory table, Dynamic Filter Row, Global Search, Add Record, Edit Record, Delete Record (with confirm), Export Excel, Export CSV.
- **Employee Portal (`/employee`):**
  - Theme: **Teal / Green** with an `"EMPLOYEE PORTAL"` badge.
  - Login / Register tabs at `/employee/login`.
  - Dashboard at `/employee/dashboard`: View-only directory, Dynamic Filter Row, Global Search, Row Inspection Modal, Export Excel, Export CSV. All mutations, uploads, and XML download endpoints return `403 Forbidden` for Employee users.
- **Route Protection (`RoleProtectedRoute`):**
  - Unauthenticated access redirects to the portal's login page.
  - Attempting to access the wrong portal's dashboard redirects to the user's appropriate portal.
  - Logging out returns to that portal's login page.

---

## 2. Ingestion & Conversion Pipeline (Everything Becomes XML)

Every uploaded structured file passes through an explicit, visible 5-step conversion pipeline:

```
[ Upload: .xlsx, .csv, .json, .xml ]
              │
              ▼
    Step A: FileReaderService (Plain Table: Headers + Rows)
              │
              ▼
    Step B: XmlConverterService (Standard Portal XML)
              │
              ▼
    Step C: XmlParserService (DOM Parser with XXE Protection)
              │
              ▼
    Step D: SchemaInferenceService (Type & Filter Inference)
              │
              ▼
    Step E: DynamicTableService (MySQL ds_<id> Dynamic Table)
```

### Step A: `FileReaderService`
Reads any uploaded file format into an in-memory plain table structure (headers + string rows):
- **`.xlsx`:** Parsed using Apache POI (`WorkbookFactory`). Reads the first sheet; row 0 is treated as headers, rows 1..N as data. Blank rows are skipped.
- **`.csv`:** Native RFC 4180 compliant parser. Strips UTF-8 BOM if present. Auto-detects delimiters (`,`, `;`, or `\t`). Handles escaped quotes, commas, and newlines inside quoted fields.
- **`.json`:** Parsed via Jackson `ObjectMapper`. Accepts either a top-level JSON array of flat objects, or an object containing a `"data"` or `"records"` array. Nested objects or arrays are strictly rejected.
- **`.xml`:** If already in standard portal format, preserved; if arbitrary repeating XML tags (e.g., `<employees><employee>...`), lifts child elements and attributes to tabular columns.
- **Validation Limits:** Files exceeding 10 MB, 100,000 rows, or 60 columns, or files with 0 data rows are immediately rejected with HTTP 400 and a descriptive message.

### Step B: `XmlConverterService`
Converts the plain table into the standardized XML representation:
```xml
<dataset>
  <columns>
    <column key="no" label="No"/>
    <column key="first_name" label="First Name"/>
    ...
  </columns>
  <records>
    <record>
      <no>1</no>
      <first_name>Ghadir</first_name>
      ...
    </record>
  </records>
</dataset>
```
- **Column Key Normalization:** Sanitized to lowercase ASCII letters, digits, and underscores (`[a-z0-9_]`). Keys starting with a digit are prefixed with `c_`. Duplicate names are suffixed with `_2`, `_3`.

### Step C: `XmlParserService`
Parses the standard XML into Java domain objects (`ParsedDataset`) using a secure `DocumentBuilderFactory` configured with XXE protection (disabling external DTDs and entity expansion).

### Step D: `SchemaInferenceService`
Scans all parsed column values across all records to infer data types and UI filter behaviors:
- **`type`:**
  - `NUMBER`: `INTEGER` (if all non-empty values parse as Long) or `DECIMAL` (if values contain decimals or parse as Double).
  - `DATE`: Matches ISO-8601 `YYYY-MM-DD` or standard date patterns (`DD/MM/YYYY`, `MM/DD/YYYY`, `DD-MM-YYYY`, `YYYY/MM/DD`). Automatically normalized to ISO `YYYY-MM-DD`.
  - `TEXT`: Any strings that do not parse as numbers or dates.
- **`filterType`:**
  - `RANGE` for `NUMBER`.
  - `DATE_RANGE` for `DATE`.
  - `CATEGORY` for `TEXT` with $\le 30$ distinct values (renders dropdown select in the filter row).
  - `TEXT_SEARCH` for `TEXT` with $> 30$ distinct values (renders text input in the filter row).
- **`sequential`:** Flagged `true` if an integer column is labeled like an ID (`no`, `id`, `#`, `sl no`) and values form a continuous sequence $1..N$ with no gaps.
- **`nullable`:** Flagged `true` if any row contains an empty or null value.

### Step E: `DynamicTableService`
- Dynamically generates and executes MySQL DDL to create a dedicated table `ds_<datasetId>`:
  - `id BIGINT PRIMARY KEY AUTO_INCREMENT`
  - Columns mapped to: `BIGINT NULL`, `DECIMAL(15, 4) NULL`, `DATE NULL`, `VARCHAR(255) NULL` (or `TEXT NULL` if long text).
  - Creates database indexes on all `CATEGORY` columns and on `id`.
- Batch-inserts all records in chunks of 500 using `JdbcTemplate.batchUpdate`.
- **Atomic Rollback:** If any error occurs during ingestion, the table `ds_<id>` is immediately dropped via `DROP TABLE IF EXISTS`, and the transaction rolls back cleanly.
- **Atomic Replacement:** Replacing an active dataset creates `ds_<newId>`, drops `ds_<oldId>`, and marks the new dataset active.

---

## 3. Dynamic UI & Filter Row

The frontend table and filters adapt dynamically to whatever schema is active:
- **Dynamic Table:** Columns rendered in schema order, with horizontal scrolling for wide datasets. Number columns are right-aligned, text and dates left-aligned. Dates formatted as `DD Mon YYYY` (e.g. `04 Apr 2018`).
- **Dynamic Filter Row:** Embedded directly underneath table headers:
  - `CATEGORY`: Dropdown populated via `GET /api/records/filter-options`.
  - `TEXT_SEARCH`: Text input with 400ms debouncing.
  - `RANGE`: Dual inputs for **Min** and **Max** with placeholders showing dataset min/max values.
  - `DATE_RANGE`: Dual date pickers for **From** and **To**.
  - **Reset Filters Button:** Resets all filters and returns to page 0.
- **Add / Edit Record Modals:**
  - Form fields generated dynamically from active dataset columns.
  - Date fields render `<input type="date">`.
  - Sequential ID columns are rendered read-only or auto-populated.
  - Per-field validation errors returned as HTTP 400 `{ "errors": { "<fieldKey>": "message" } }` and highlighted beneath the input.

---

## 4. Mandatory Pagination & APIs

Every directory query enforces server-side pagination:
- Endpoint: `GET /api/records?page=0&size=10`
- `page` is clamped to $\ge 0$.
- `size` is clamped to between $10$ and $100$ (default 10).
- **Zero full-table dumps:** Queries always execute with `COUNT(*)` for total records and `SELECT ... LIMIT size OFFSET page * size`.

### Dynamic Query Parameters
- Global text search: `?search=<text>`
- Exact match on category: `?eq_<colKey>=<value>`
- Substring match: `?like_<colKey>=<value>`
- Numeric ranges: `?min_<colKey>=<num>&max_<colKey>=<num>`
- Date ranges: `?from_<colKey>=<date>&to_<colKey>=<date>`

---

## 5. Dual File Exports

Both portals support two unpaginated, filter-scoped export options:

1. **📊 Export Excel (`GET /api/records/export/excel`):**
   - Streams an authentic `.xlsx` spreadsheet using Apache POI **`SXSSFWorkbook`** (100-row memory flush window).
   - Styled dark blue header (`#1F4E79`, bold white text).
   - Frozen top header row (`createFreezePane(0, 1)`).
   - Excel AutoFilter applied to all columns.
   - Native numeric and date cell types.
   - Attachment filename: `employee_data.xlsx`.

2. **📄 Export CSV (`GET /api/records/export`):**
   - Streams a standard `.csv` file.
   - Prepends the UTF-8 Byte Order Mark (`\uFEFF`) so Microsoft Excel opens special characters seamlessly.
   - RFC 4180 escaping for quotes and commas.
   - Attachment filename: `employee_data.csv`.

---

## 6. Security & Role-Based Access Control (RBAC)

The application uses token-based authentication with HMAC-SHA256 signatures and strict interceptor guards (`AuthInterceptor`):

| Endpoint | Method | DEAN | EMPLOYEE | Unauthenticated |
|---|---|:---:|:---:|:---:|
| `/api/auth/**` | POST | Allowed | Allowed | Allowed |
| `/api/dataset/active` | GET | Allowed | Allowed | 401 |
| `/api/dataset/upload` | POST | Allowed | 403 | 401 |
| `/api/dataset/replace` | POST | Allowed | 403 | 401 |
| `/api/dataset/xml` | GET | Allowed | 403 | 401 |
| `/api/records` | GET | Allowed | Allowed | 401 |
| `/api/records/filter-options` | GET | Allowed | Allowed | 401 |
| `/api/records/{id}` | GET | Allowed | Allowed | 401 |
| `/api/records` | POST | Allowed | 403 | 401 |
| `/api/records/{id}` | PUT | Allowed | 403 | 401 |
| `/api/records/{id}` | DELETE | Allowed | 403 | 401 |
| `/api/records/export/excel` | GET | Allowed | Allowed | 401 |
| `/api/records/export` | GET | Allowed | Allowed | 401 |

---

## 7. Demo Credentials

The database comes pre-seeded with two demo accounts (passwords hashed via BCrypt):

| Role | Username / Identifier | Password | Access Code | Portal URL |
|---|---|---|---|---|
| **DEAN** | `dean` (or `dean@portal.com`) | `dean123` | `DEAN2026` | `http://localhost:5173/dean/login` |
| **EMPLOYEE** | `employee` (or `employee@portal.com`) | `emp123` | N/A | `http://localhost:5173/employee/login` |

---

## 8. Sample Datasets

Reference files are provided in `sample-data/`:
- **`Employees.xlsx`:** Primary reference file containing 689 employee records across 15 columns (`No`, `First Name`, `Last Name`, `Gender`, `Start Date`, `Years`, `Department`, `Country`, `Center`, `Monthly Salary`, `Annual Salary`, `Job Rate`, `Sick Leaves`, `Unpaid Leaves`, `Overtime Hours`).
- **`sample-employees.xml`:** 15 records in XML format.
- **`sample-students.xml`:** Dynamic dataset showcasing student grades, departments, and roll numbers.
- **`sample-products.xml`:** Dynamic dataset showcasing e-commerce catalog items with prices and stock.

---

## 9. Quick Start Instructions

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL 8.x

### Step 1: Initialize Database
In your MySQL client:
```sql
DROP DATABASE IF EXISTS employee_portal;
CREATE DATABASE employee_portal;
```

Verify `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:employee_portal}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.jpa.hibernate.ddl-auto=update
app.dean.access-code=DEAN2026
```

### Step 2: Start Backend (Port 8080)
```bash
cd backend
mvn spring-boot:run
```

### Step 3: Start Frontend (Port 5173)
```bash
cd frontend
npm install
npm run dev
```

Visit [http://localhost:5173](http://localhost:5173).

---

## 10. Automated Tests & Verification

- **Backend Integration Tests:**
  ```bash
  cd backend
  mvn test
  ```
  Runs 12 comprehensive integration tests covering file uploads, XML pipeline conversion, schema inference, dynamic table queries, RBAC security, POI Excel streaming, and CSV streaming. Tests run in an isolated in-memory H2 environment.

- **Frontend Build Verification:**
  ```bash
  cd frontend
  npm run build
  ```
  Vite compiles production assets with zero syntax or bundle errors.

- **HTTP Client Tests:**
  Open [`api-tests.http`](api-tests.http) in VS Code (with REST Client extension) or IntelliJ to execute every endpoint interactively.

---

## 11. Troubleshooting

### 1. Database Connection & Password Configuration
By default, `application.properties` is configured for local MySQL with user `root` and an **empty password** using environment variable placeholders:
```properties
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:employee_portal}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
```

If your MySQL server requires a password (e.g., in an interviewer's or production environment), you can supply it easily without changing source code:
- **Via Environment Variables:**
  - **PowerShell (Windows):**
    ```powershell
    $env:DB_PASSWORD="your_mysql_password"
    mvn spring-boot:run
    ```
  - **Bash (Linux/macOS):**
    ```bash
    DB_PASSWORD=your_mysql_password mvn spring-boot:run
    ```
- **Via Command-Line Arguments:**
  ```bash
  mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.password=your_mysql_password"
  ```
- **Or directly in `backend/src/main/resources/application.properties`:**
  ```properties
  spring.datasource.password=your_mysql_password
  ```

### 2. Error: `java.sql.SQLException: Access denied for user 'root'@'localhost'`
- **Root Cause:** The password configured in `application.properties` (or `DB_PASSWORD`) does not match your MySQL server's actual password.
  - If MySQL has no password set (insecure initialization), `spring.datasource.password` must be empty (`${DB_PASSWORD:}`).
  - If MySQL has a password set, you must pass `DB_PASSWORD` or set `spring.datasource.password`.
- **Note on Dialect Error:** The log message `Unable to determine Dialect without JDBC metadata` is merely a secondary symptom that occurs when Hibernate cannot query the database metadata due to the primary connection failure. Fixing the credentials resolves both messages.

### 3. Error: `Communications link failure / Connection refused`
- **Root Cause:** MySQL Server is not running or is not listening on port 3306.
- **Verification:** Before starting Spring Boot, verify MySQL connectivity:
  ```bash
  mysql -u root -e "SELECT 1"
  ```
  Ensure the MySQL service is started before launching the backend.
