# Development Notes - Loan Application Service

## Overall Approach
The app is built using **Spring Boot 4.0.3**.
The core logic part is broke down into 3 major service logic (considering the Single Responsibility Principle):

- **`LoanApplicationService`**: The main start of business logic containing multiple sub-service calls and methods (user persistence, eligibility check, offer generation).
- **`EligibilityRulesEngine`**: The rule engine that focuses strictly on rejection rules based upon the input data (Age, Credit Score, EMI ratios).
- **`FinancialCalculatorService`**: Handles the mathematical computations (Risk bands, Interest rates, EMI formulas using `BigDecimal`).

## Key Design Decisions
1.  **Precision Handling**: Used `BigDecimal` with `RoundingMode.HALF_UP` and a scale of 2 for all currency and percentage calculations to avoid floating-point inaccuracies.
2.  **Stateless Rules Engine**: The `EligibilityRulesEngine` is designed to be stateless, making it easy to test and potentially extract into a standalone business rules service.
3.  **Domain-Driven Validation**: Leveraged `Jakarta Bean Validation` (@Valid, @Min, @Max) at the DTO level to catch malformed or out-of-range requests before they reach the business logic.
4.  **Database Integration**: Switched to database-native auto-incrementing IDs for `UserEntity` to optimize MySQL performance and simplify entity management compared to manual UUID generation.

## Trade-offs Considered
- **Standard Validation vs. Custom Logic**: Chose to use Bean Validation for range/format checks (Age 21-60, Score 300-900) while keeping business rules (Age + Tenure ≤ 65) in a dedicated service. This separates "well-formed data" from "eligible data".
- **Primary Key Strategy**: Opted for `Long id` (Auto-increment) over `UUID` to align with MySQL best practices for clustered index performance, despite UUIDs being better for distributed systems.

## Assumptions Made
- **Base Interest Rate**: Fixed at 12.00% as per requirements.
- **Tenure Unit**: Assumed to be in months for EMI calculation and converted to years for the `Age + Tenure > 65` rule.
- **Risk Bands**: Defined as LOW (≥750), MEDIUM (650-749), and HIGH (<650) based on credit score.

## Future Improvements
1.  **Rule Engine Integration**: For more complex or frequently changing rules, integrating a tool like **Drools** would allow business users to update eligibility criteria without code changes.
2.  **Audit Logging**: Implement an interceptor or Aspect (AOP) to log every decision path (rejection reason details) into a separate audit table for regulatory compliance.
3.  **Dynamic Pricing**: Refactor interest rates and premiums to be fetchable from a database or configuration server rather than being hardcoded constants.
4.  **Error Handling**: Enhance the `GlobalExceptionHandler` to return a unique `correlationId` in error responses to help developers trace issues in logs via ELK/Splunk.
