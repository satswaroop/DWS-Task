## DWS-Task

This task is a Transfer Management System built using Spring Boot 3 and Java 17. It provides functionalities to manage account transfers between users, ensuring validations and handling concurrency.

### Features

- **Transfer Money:** Transfer funds between accounts with validations.
- **Concurrency Handling:** Ensure thread-safe transactions.
- **Error Handling:** Informative error messages and validation checks.

### Technologies Used

- **Java 17**
- **Spring Boot 3**
- **Gradle**
- **JUnit 5**
- **Mockito**
- **Spring MockMvc**

### Prerequisites

- Java 17
- Gradle
- Git

### Setup and Running the Project

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/satswaroop/DWS-Task.git
   cd DWS-Task
   ```

2. **Build the Project:**

   ```bash
   ./gradlew clean build
   ```

3. **Run the Application:**

   ```bash
   ./gradlew bootRun
   ```

4. **Run Tests:**

   ```bash
   ./gradlew clean test
   ```

### API Endpoints

#### Transfer Money

- **URL:** `/v1/accounts/transfer`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Request Body:**

  ```json
  {
    "accountFromId": "Id-123",
    "accountToId": "Id-456",
    "amount": 300.00
  }
  ```

- **Response:**
    - `200 OK`: If the transfer is successful.
    - `400 Bad Request`: If any validation fails.

### Test Cases

- **Successful Transfer:** Ensure balances are updated correctly.
- **Insufficient Funds:** Handle transfers with insufficient balance.
- **Invalid Amount Format:** Validate and handle incorrect amount formats.
- **Negative Transfer Amount:** Ensure negative amounts are not allowed.
- **Concurrent Transfers:** Test multiple concurrent transfers for consistency.

### Future Improvements

Recommended Improvements Before Production Release

**Enhanced Validation and Error Handling:**
- Implement more detailed validation for request parameters to ensure they meet all necessary constraints (e.g., valid UUID format for account IDs).
Provide more informative error messages and comprehensive error handling to cover all potential edge cases.

**Security Enhancements:**
- Implement authentication and authorization mechanisms to ensure only authorized users can perform transfers.
Use HTTPS for secure data transmission.
Add input sanitization to prevent SQL injection and other security vulnerabilities.

**Logging and Monitoring:**
- Integrate logging for all significant events and errors for better traceability.
Set up monitoring and alerting for key metrics (e.g., number of transactions, failed transactions).

**Performance Optimization:**
- Conduct performance testing to identify bottlenecks and optimize the application's performance under load.
Implement caching strategies where appropriate to reduce database load.

**Scalability:**
- Design the system to be scalable to handle increasing loads, including database scaling and horizontal scaling of application servers.

**Database Management:**
- Implement database migration tools for seamless schema changes.

**Comprehensive Testing:**
- Expand test coverage to include integration tests, end-to-end tests, and performance tests.
Automate testing as part of a continuous integration/continuous deployment (CI/CD) pipeline.

**Documentation:**
- Provide thorough documentation for API endpoints, including usage examples and response formats.
Document the overall system architecture and setup instructions for developers and operators.
---
