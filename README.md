# Employee Data Management Portal

A full-stack enterprise web portal built for technical evaluation. It allows organizations to upload employee records via XML, store them in MySQL 8 using Spring Data JPA, perform dynamic multi-field search and pagination, view detailed employee modals, delete records, and stream filtered results to CSV.

---

## 1. Overview & Features

- **Role-Based Access Control (RBAC):** Distinct dashboards for **DEAN** (Admin) and **EMPLOYEE** roles with route guards.
- **XML Ingestion:** Dedicated `XmlParserService` leveraging Jackson `XmlMapper` to deserialize XML datasets into JPA entities with bulk upsert (`saveAll()`).
- **Dynamic Filter & Search:** Real-time search across Employee ID, City, and Education with wildcard escaping, alongside exact matching for City and Gender using JPA `Specification`.
- **Server-Side Pagination:** Sorted pagination (10 records/page) by `employeeId ASC`.
- **Interactive Details Modal:** Click-to-view modal with friendly labels and multi-modal close (X button, Close button, backdrop click, Escape key).
- **Protected Actions:** DEAN-only XML upload and record deletion with confirmation dialogs and stopPropagation to prevent modal popups.
- **Direct CSV Streaming:** Export all records matching the current active filters directly as a downloadable CSV.
- **Lightweight Authentication:** HMAC-SHA256 signed stateless tokens carrying username and role without the overhead of heavy security filters.

---

## 2. Tech Stack

- **Backend:** Spring Boot 3.2.4, Java 17, Maven, Lombok, Spring Data JPA, Hibernate, MySQL Connector/J, Jackson XML (`jackson-dataformat-xml`), Spring Security Crypto (BCrypt).
- **Database:** MySQL 8 (auto-creates database `employee_portal` and tables on startup).
- **Frontend:** React.js 18, Vite 5, Axios, React Router DOM v6, Plain CSS with CSS variables (no heavy UI frameworks).
- **Network Ports:** Backend `8080`, Frontend `5173`.

---

## 3. Prerequisites

Before running the project, ensure you have installed:
- **Java 17** (or higher)
- **Maven 3.8+** (or use the included `./mvnw` / `mvnw.cmd` wrapper)
- **Node.js 18+** and **npm**
- **MySQL 8** running locally on port 3306

---

## 4. Setup & Running Instructions

### Step 1: Clone and Configure Database
1. Make sure your local MySQL 8 server is running.
2. Open `backend/src/main/resources/application.properties` and verify your MySQL credentials:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=root          # Change to your MySQL password
   ```
   *(Note: The database `employee_portal` and all tables will be automatically created on first startup thanks to `createDatabaseIfNotExist=true` and `hibernate.ddl-auto=update`.)*

### Step 2: Start the Backend (Port 8080)
```bash
cd backend
mvn spring-boot:run
```
*(Or on Windows without global Maven: `.\mvnw.cmd spring-boot:run`)*

### Step 3: Start the Frontend (Port 5173)
```bash
cd frontend
npm install
npm run dev
```

### Step 4: Open the Application
Navigate to [http://localhost:5173](http://localhost:5173) in your web browser.

---

## 5. Login Credentials & Role Permissions

| Username | Password | Role | Permissions |
|---|---|---|---|
| `dean` | `dean123` | **DEAN** | Full Access: Upload XML, Search & Filter, View Details Modal, Delete Employee, Export CSV |
| `employee` | `emp123` | **EMPLOYEE** | Read-Only: Search & Filter, View Details Modal, Export CSV (Upload & Delete hidden and blocked) |

---

## 6. How to Test

1. **Login as DEAN:**
   - Go to `http://localhost:5173/login` and sign in with `dean` / `dean123`.
   - You will be redirected to `/dean/dashboard`.
2. **Upload Sample XML:**
   - Under **Import Employee XML Dataset**, choose `sample-data/sample-employees.xml` (15 records) and click **Upload XML**.
   - A success banner will confirm `15` records saved, and the table will automatically refresh.
3. **Upload Full XML (Pagination Demo):**
   - Upload `sample-data/employees-full.xml` (4,653 records).
   - Notice the pagination bar update to `Total Records: 4,653`, with Prev/Next buttons navigating between pages.
4. **Test Search & Filters:**
   - In the search bar, type `Bangalore` or `Pune` (input is automatically debounced ~400ms).
   - Select City: `Pune` and Gender: `Female`.
   - Click **Reset** to restore default view.
5. **View Modal:**
   - Click on any table row to open the details popup modal.
   - Verify friendly labels (`Left company: Yes/No`, `Tier 3`).
   - Close using the `X` button, `Close` button, clicking the backdrop, or pressing the `Esc` key.
6. **Test Delete (DEAN only):**
   - Click **Delete** on an employee row.
   - Confirm the prompt (`window.confirm`).
   - Notice that the details modal does **not** open (due to `e.stopPropagation()`), and the record is deleted.
7. **Export CSV:**
   - Filter by City: `Bangalore`, then click **Export CSV**.
   - An `employees.csv` file will download containing all records matching that filter.
8. **Login as EMPLOYEE:**
   - Click **Logout** in the header.
   - Sign in as `employee` / `emp123`.
   - You will be redirected to `/employee/dashboard`.
   - Notice that the XML Upload card and the table Action/Delete column are completely absent.
   - Any manual API calls to upload or delete return `403 Forbidden` with `"You don't have permission"`.

---

## 7. API Endpoints Table

| Method | Endpoint | Allowed Roles | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticates credentials and returns signed token carrying user role. |
| `POST` | `/api/employees/upload` | **DEAN** | Ingests multipart `.xml` file, parses records with XmlMapper, and upserts to MySQL. |
| `GET` | `/api/employees` | **DEAN**, **EMPLOYEE** | Retrieves paginated employees sorted by ID ascending, supporting `search`, `city`, and `gender` query params. |
| `GET` | `/api/employees/{id}` | **DEAN**, **EMPLOYEE** | Retrieves complete details for a single employee ID (404 if not found). |
| `DELETE` | `/api/employees/{id}` | **DEAN** | Permanently deletes an employee record by ID (204 No Content, 404 if not found). |
| `GET` | `/api/employees/export` | **DEAN**, **EMPLOYEE** | Streams all records matching active filter specifications as a CSV attachment. |

---

## 8. XML -> Java Object -> MySQL Flow

```
+------------------+         +--------------------+         +---------------------+         +-------------------+
|  1. Client POST  |  -----> |   2. XmlMapper     |  -----> | 3. List<Employee>   |  -----> | 4. Spring Data    |
|  multipart/xml   |         |   Jackson Parsing  |         |    Java Entities    |         |    saveAll()      |
+------------------+         +--------------------+         +---------------------+         +-------------------+
                                                                                                      |
                                                                                                      v
                                                                                            +-------------------+
                                                                                            | 5. MySQL Database |
                                                                                            |    employees      |
                                                                                            +-------------------+
```

1. **Receive Multipart File:** The client sends a `multipart/form-data` request with key `"file"`.
2. **Validation:** `XmlParserService` verifies that the file is not empty and has an `.xml` extension.
3. **Jackson XML Deserialization:** Jackson's `XmlMapper` reads the `InputStream` into the wrapper DTO `EmployeeListWrapper`.
   - `@JacksonXmlElementWrapper(useWrapping = false)` maps repeating `<employee>` tags directly into `List<Employee>`.
4. **Entity Mapping:** Because the XML tags match the `Employee` entity fields (`employeeId`, `education`, `city`, etc.), they map 1-to-1 without manual boilerplate.
5. **Database Upsert:** `employeeRepository.saveAll(employees)` is executed inside a `@Transactional` block. Because `employeeId` is the primary key (`@Id`), Hibernate automatically updates existing rows on re-upload rather than generating duplicate entries.

---

## 9. Future Enhancements & Possible Improvements

- **Spring Security 6 JWT:** Transition from lightweight HMAC tokens to full Spring Security 6 with asymmetric RSA key pairs and refresh token rotation.
- **Excel & PDF Export:** Add Apache POI / iText to export report summaries in `.xlsx` and `.pdf` formats.
- **Bean Validation:** Add Hibernate Validator (`@NotNull`, `@Min`, `@Max`, `@Pattern`) on entity fields and request DTOs.
- **CI/CD Pipeline:** Add GitHub Actions workflow for automated `mvn test` and `npm run build` on push.
