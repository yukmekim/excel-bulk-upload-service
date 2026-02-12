# Excel Bulk Upload Service

This is a Spring Boot application designed for bulk uploading massive amounts of data from Excel files into a database.

## Technologies
- **Java 21**
- **Spring Boot 3.4.2**
- **Spring Batch**: For processing large datasets.
- **Spring Data JPA**: For database interaction.
- **H2 Database**: In-memory database for development/testing.
- **Apache POI**: For reading Excel files (.xlsx).
- **Lombok**: To reduce boilerplate code.

## Setup & Run
1. Make sure you have Java 21 installed.
2. Run the application:
   ```bash
   ./gradlew bootRun
   ```
3. The application will start on port 8080.
4. H2 Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, User: `sa`, Password: `password`).

## Configuration
- Default database is H2 (configured in `src/main/resources/application.properties`).
- File upload limits are set to 50MB.

## Project Structure
- `src/main/java/com/yukmekim/excelbulkuploadservice`: Main application code.
- `src/main/resources`: Configuration files.
- `src/test/java`: Unit tests.

## Next Steps
- Implement `BatchConfig` to define Spring Batch jobs.
- Create a Controller to accept file uploads.
- Define Entity classes for your data.
