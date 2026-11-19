# Utang na Loob: Micro-Lending & Transparent Repayment Platform

Project Development Increment 1: User Accounts, Authentication, and Role-Based Dashboards.

---

## 1. System Architecture

- **Frontend**: ReactJS, Tailwind CSS, Context API for state and session management
- **Backend**: Java 17+, Spring Boot 3, Spring Security 6, JWT, BCrypt Password Hashing, Spring Data JPA
- **Database**: PostgreSQL (with embedded/H2 in-memory profile for standalone demonstration)

---

## 2. Seed Accounts for Demonstration

The database automatically seeds the following operational user accounts with pre-hashed BCrypt credentials:

| Role | Email | Password | Primary Capabilities |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin@utangnaloob.ph` | `Admin@123` | Portfolio analytics, User role management, System financial parameter configuration, Security audit trail |
| **Credit Underwriter** | `underwriter@utangnaloob.ph` | `Underwriter@123` | Pending KYC applicant verification queue, Document inspector, Revolving credit limit allocation |
| **Active Borrower** | `borrower@utangnaloob.ph` | `Borrower@123` | Active revolving credit limit, Available spending balance, Installment calculator |
| **Pending Applicant** | `applicant@utangnaloob.ph` | `Borrower@123` | Borrower with submitted KYC documents ready for underwriter review |

---

## 3. How to Run

### Backend (Spring Boot)
Open a terminal in `backend/` and execute:

```bash
# Run with default PostgreSQL database (configured in application.properties)
mvn spring-boot:run

# Or run with the standalone H2/embedded PostgreSQL-compatible profile
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

The REST API will be available at: `http://localhost:8080/api`

### Frontend (React Application)
- Open `frontend/standalone.html` directly in any modern web browser for an immediate zero-configuration interactive demonstration.
- Or run the Vite development server in `frontend/` using `npm run dev`.
