# MySQL Setup Instructions

## 1. Application Properties Configuration
Add the following to your `/Users/pooja/Desktop/Projects/loanapp/src/main/resources/application.properties` file:

```properties
# Server
server.port=8080

# MySQL Connection Details
spring.datasource.url=jdbc:mysql://localhost:3306/loan_db
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate Properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```
> **Note:** Replace `YOUR_MYSQL_USERNAME` and `YOUR_MYSQL_PASSWORD` with your actual MySQL credentials.

---

## 2. Database Schema

You only need to run the `CREATE DATABASE` command if you have set `spring.jpa.hibernate.ddl-auto=update` in your properties, as Hibernate will generate the tables for you automatically!

```sql
CREATE DATABASE IF NOT EXISTS loan_db;
```

**Optional:** If you prefer to disable auto-generation (`spring.jpa.hibernate.ddl-auto=none`) and create the tables manually, here is the schema:

```sql
USE loan_db;

CREATE TABLE loan_evaluations (
    id BINARY(16) NOT NULL PRIMARY KEY,
    applicant_name VARCHAR(255) NOT NULL,
    applicant_age INT NOT NULL,
    monthly_income DECIMAL(19, 2) NOT NULL,
    employment_type VARCHAR(50) NOT NULL,
    credit_score INT NOT NULL,
    
    requested_amount DECIMAL(19, 2) NOT NULL,
    requested_tenure_months INT NOT NULL,
    loan_purpose VARCHAR(50) NOT NULL,
    
    status VARCHAR(50) NOT NULL,
    risk_band VARCHAR(50),
    
    offer_interest_rate DECIMAL(19, 2),
    offer_tenure_months INT,
    offer_emi DECIMAL(19, 2),
    offer_total_payable DECIMAL(19, 2)
);

CREATE TABLE loan_evaluations_rejection_reasons (
    loan_evaluation_result_id BINARY(16) NOT NULL,
    rejection_reasons VARCHAR(255),
    FOREIGN KEY (loan_evaluation_result_id) REFERENCES loan_evaluations(id)
);
```
