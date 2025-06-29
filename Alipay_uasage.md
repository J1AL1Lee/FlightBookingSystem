## Alipay Payment Integration

### Overview
- The project now integrates Alipay payment functionality using the `AlipayService` class.
- Supports payment creation, status checking, and notifications.

### API Endpoints
- **POST /api/payments/create**
    - Description: Creates a payment order and generates a QR code.
    - Request Body: `{"orderId": "string", "amount": "double"}`
    - Response: `{"success": boolean, "message": "string", "payId": "string", "qrCode": "string"}`
- **GET /api/payments/status**
    - Description: Checks the payment status by payId.
    - Query Parameter: `?payId=string`
    - Response: `{"success": boolean, "status": "string", "qrCode": "string|null"}`
- **POST /api/payments/notify**
    - Description: Handles Alipay asynchronous notifications (TBD).

### Implementation Details
- **AlipayService**: Located in `src/main/java/service/AlipayService.java`.
    - Manages Alipay API calls and database interactions.
    - Loads configuration from `src/main/resources/application.properties`.
- **Integration**: Added `PaymentCreateHandler` and `PaymentStatusHandler` to `SimpleHttpServer.java`.
- **Dependencies**: Requires `alipay-sdk-java` and `gson` (configured in `pom.xml`).

### Setup
- Ensure `application.properties` contains valid Alipay credentials.
- Run `mvn clean install` to build the project.
- Start the server with `java -jar target/flight-booking-system-1.0.0.jar`.

### Testing
- Test payment creation: `curl -X POST http://localhost:8080/api/payments/create -H "Content-Type: application/json" -d '{"orderId":"OR123456","amount":"10.50"}'`
- Check status: `curl http://localhost:8080/api/payments/status?payId=ORDER_...`

### Notes
- Payment amount is dynamic, supporting any positive value.
- QR code refreshes via status polling; implement `PaymentNotifyHandler` for real-time updates.