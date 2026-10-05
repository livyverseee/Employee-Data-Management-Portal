# Employee Data Management Portal

A full-stack enterprise web portal built for technical evaluation using **Spring Boot 3.2.4**, **React 18 (Vite)**, and **MySQL 8**. It provides two dedicated, role-tailored portals (**Dean Portal** and **Employee Portal**) for managing employee datasets, performing multi-column filtering, updating records, inspecting employee details, and streaming filtered records to CSV.

---

> [!WARNING]
> ### SCHEMA MIGRATION NOTICE
> If you previously ran an earlier version of this application, you **MUST** drop the existing database before starting the backend due to primary key and entity schema updates (`Dataset` entity added, `Employee` PK migrated to auto-incrementing `Long id` with dataset-scoped unique constraints):
> ```sql
> DROP DATABASE employee_portal;
> ```
> Spring Boot will automatically recreate the database and all updated tables on startup.

---

## 1. Overview & Architecture

### Two Dedicated Portals
- **Landing Page (`/`):** Clean portal selector with dedicated access cards for the **Dean Portal** and **Employee Portal**.
- **Dean Portal (`/dean/login`, `/dean/dashboard`):**
  - **Indigo/Purple Theme** with `DEAN PORTAL` badge.
  - **Dataset Lifecycle Management:** Initial dataset onboarding card when empty; active dataset metadata info bar + **"Replace Dataset"** modal.
  - **Workforce Management:** Full CRUD capabilities — per-column dynamic filtering, global search, **Edit** employee modal, **Delete** employee with confirmation, view details popup, and CSV export.
- **Employee Portal (`/employee/login`, `/employee/dashboard`):**
  - **Teal/Green Theme** with `EMPLOYEE PORTAL` badge.
  - **Read-Only Directory:** View active dataset metadata, friendly empty state if no dataset has been uploaded yet, dynamic per-column filtering, age range filtering, detail inspection modal, and CSV export.
  - Upload, replace, edit, and delete operations are completely absent and blocked with `403 Forbidden` if attempted.

### Dataset Ownership & Scoping
- At most **one active dataset** exists in the system at any time.
- All employee queries (`GET /api/employees`, `/filter-options`, single record lookup, and CSV export) are **strictly scoped** to the active dataset ID.
- **Initial Upload (`POST /api/dataset/upload`):** Allowed only when no dataset exists; rejected with `409 Conflict` if a dataset is already active.
- **Dataset Replace (`POST /api/dataset/replace`):** Validates and parses the new XML file **first**; upon successful validation, existing employee records are atomically replaced within a single `@Transactional` method so that invalid files never destroy existing data.

### Dual-Tab Authentication & Registration
- Reusable `AuthCard` parameterized by portal with **Sign In** and **Register** tabs.
- Login accepts either **Username or Email** (`identifier`). Cross-portal logins (e.g., Dean logging into Employee portal) are rejected with `401 Unauthorized`.
- Dean registration requires a valid Dean Access Code (`DEAN2026`).
- Registration returns `201 Created` without auto-logging in, switching to the Sign In tab with a green confirmation alert.

---

## 2. Tech Stack

- **Backend:** Spring Boot 3.2.4, Java 17, Maven, Lombok, Spring Data JPA, Hibernate, MySQL Connector/J, Jackson XML (`jackson-dataformat-xml`), Spring Security Crypto (BCrypt for password hashing).
- **Database:** MySQL 8 (auto-creates `employee_portal` database and tables on startup).
- **Frontend:** React 18, Vite 5, Axios, React Router DOM v6, Plain CSS with theme variables (no heavy UI frameworks).
- **Network Ports:** Backend `8080`, Frontend `5173`.

---

## 3. Prerequisites

Before running the application:
1. **Java 17** (or higher)
2. **Maven 3.8+** (or use `./mvnw` / `mvnw.cmd`)
3. **Node.js 18+** and **npm**
4. **MySQL 8** running on `localhost:3306`

---

## 4. Setup & Running Instructions

### Step 1: Database Setup
1. Ensure your local MySQL 8 server is running.
2. If upgrading from a previous version, run in your MySQL shell:
   ```sql
   DROP DATABASE employee_portal;
   ```
3. Verify your MySQL credentials in `backend/src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=root          # Change to your MySQL password
   app.dean.access-code=DEAN2026            # Default Dean access code for registration
   ```

### Step 2: Start the Backend (Port 8080)
```bash
cd backend
mvn spring-boot:run
```
*(Or on Windows: `mvnw.cmd spring-boot:run`)*

### Step 3: Start the Frontend (Port 5173)
```bash
cd frontend
npm install
npm run dev
```

### Step 4: Open Application
Navigate to [http://localhost:5173](http://localhost:5173) in your browser.

---

## 5. Pre-Seeded Accounts

The application automatically seeds two test accounts on startup via `DataSeeder`:

| Role | Username / Identifier | Password | Access Code | Portal URL |
|---|---|---|---|---|
| **DEAN** | `dean` (or `dean@portal.com`) | `dean123` | `DEAN2026` | `http://localhost:5173/dean/login` |
| **EMPLOYEE** | `employee` (or `emp@portal.com`) | `emp123` | N/A | `http://localhost:5173/employee/login` |

---

## 6. Endpoints Reference

### Authentication Endpoints
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/dean/login` | Public | Dean login with username or email + password. Returns token & full details. |
| `POST` | `/api/auth/employee/login` | Public | Employee login with username or email + password. |
| `POST` | `/api/auth/dean/register` | Public | Dean registration requiring `fullName`, `username`, `email`, `password`, and `accessCode`. Returns 201. |
| `POST` | `/api/auth/employee/register` | Public | Employee registration requiring `fullName`, `username`, `email`, and `password`. Returns 201. |

### Dataset Lifecycle Endpoints
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/dataset/active` | DEAN, EMPLOYEE | Retrieves metadata of current active dataset (`{ exists, fileName, uploadedBy, uploadedAt, recordCount }`). |
| `POST` | `/api/dataset/upload` | **DEAN** only | Uploads initial XML dataset. Returns 409 if a dataset is already active. |
| `POST` | `/api/dataset/replace` | **DEAN** only | Atomically validates and replaces active dataset with a new XML file. |

### Employee Management Endpoints
| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/api/employees` | DEAN, EMPLOYEE | Paginated employee list scoped to active dataset. Supports `search`, `employeeId`, `education`, `city`, `joiningYear`, `paymentTier`, `ageMin`, `ageMax`, `gender`, `everBenched`, `leaveOrNot`. |
| `GET` | `/api/employees/filter-options` | DEAN, EMPLOYEE | Distinct filter dropdown values aggregated from the active dataset. |
| `GET` | `/api/employees/{id}` | DEAN, EMPLOYEE | Retrieves complete details for an employee by database ID. |
| `PUT` | `/api/employees/{id}` | **DEAN** only | Updates employee fields. Validates constraints and checks duplicate `employeeId` (409 Conflict). |
| `DELETE` | `/api/employees/{id}` | **DEAN** only | Deletes employee record by database ID (204 No Content). |
| `GET` | `/api/employees/export` | DEAN, EMPLOYEE | Streams all records matching current active filter specifications as a CSV attachment. |

---

## 7. Step-by-Step Testing & Evaluation Guide

1. **Portal Selection Landing Page (`/`):**
   - Visit `http://localhost:5173`.
   - Observe the two cards: **Dean Portal** (indigo) and **Employee Portal** (teal).
2. **Dean Onboarding Flow:**
   - Click **Enter Dean Portal** (`/dean/login`).
   - Sign in with `dean` / `dean123`.
   - If no dataset is uploaded yet, an onboarding card appears with a drag-and-drop XML file dropzone.
   - Select `sample-data/sample-employees.xml` (15 records) and click **Upload & Initialize Dataset**.
   - Notice the green notification banner, the **Active Dataset Info Bar** displaying metadata, and the loaded employee table.
3. **Multi-Column Filtering & Age Range:**
   - In the filter row directly below the table headers:
     - Filter by City (e.g., `Bangalore`).
     - Filter by Age Range: enter `Min: 20`, `Max: 30`.
     - Filter by Gender: select `Female`.
   - Click **Reset** to clear all column filters.
4. **Edit Employee Record (DEAN only):**
   - Click the **Edit** button on any row.
   - Notice that the detail view modal does not open (due to `e.stopPropagation()`).
   - Modify fields (e.g. increase experience or change city) and click **Save Changes**.
   - The table refreshes with a success toast notification.
   - If you set an `employeeId` that already exists on another row, a clear `409 Conflict` message appears.
5. **Delete Employee Record (DEAN only):**
   - Click **Delete** on a row. Confirm the browser prompt.
   - The record is removed and a success toast appears.
6. **Replace Dataset Atomically:**
   - Click **↻ Replace Dataset** in the Active Dataset Info Bar.
   - Select `sample-data/employees-full.xml` (4,653 records) and confirm.
   - The dataset is replaced cleanly; pagination updates to reflect `4,653` total records.
7. **Export Filtered CSV:**
   - Apply any filter and click **📥 Export CSV**.
   - A CSV file downloads containing only records matching the active filter.
8. **Employee Portal Read-Only Flow:**
   - Log out from the Dean Portal (redirects to `/dean/login`).
   - Navigate to `/employee/login` and sign in with `employee` / `emp123`.
   - Notice the distinct teal theme, the `EMPLOYEE PORTAL` badge, and read-only access:
     - Active Dataset Info Bar has no "Replace" button.
     - Table Action column shows only a **View** button (no Edit or Delete buttons).
     - Full searching, dynamic filtering, detail inspection modal, and CSV export remain fully functional.

---

## 8. Running Automated Tests

To run the backend integration test suite covering Dean & Employee authentication, access code validation, duplicate checks, atomic dataset replacement, and role-based permissions:

```bash
cd backend
mvn test
```
All 12 integration tests will execute against an in-memory H2 database with 100% pass rate.
