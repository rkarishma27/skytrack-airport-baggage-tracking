# SkyTrack – Airport Baggage Management System

A complete web application for your `airportbaggagems` MySQL database.

| Layer     | Tech |
|-----------|------|
| Frontend  | HTML + CSS + vanilla JavaScript single-page app (dashboard, CRUD, bag tracker, dark/light theme, responsive) |
| Backend   | Java 17 + Spring Boot 3 REST API |
| Database  | MySQL via **plain JDBC** (`Connection` / `PreparedStatement` / `ResultSet`, pooled with HikariCP) |

## Features
- **Dashboard** – live totals, baggage-status donut, security screening, flight status, busiest flights, latest check-ins
- **Baggage** – search, status filter, sort, add / edit / delete, one-click status change
- **Track a bag** – look up by RFID tag or ID: owner, flight, claim belt and a journey timeline
- **Flights, Passengers, Countries** – full CRUD with validation
- **Baggage claim** – *Collect* and *Report lost* actions (each is a JDBC **transaction** that updates claim + baggage together)
- Friendly errors: duplicate RFID/passport, deleting a row that is still referenced, bad input, etc.

## Run it

**Prerequisites:** JDK 17+, Maven 3.8+, MySQL 8 with your `airportbaggagems` database.

1. *(Optional)* load demo data: `mysql -u root -p airportbaggagems < database/sample_data.sql`
   (`database/schema.sql` is only a reference if you ever need to recreate the tables.)
2. Set your MySQL login – either edit `src/main/resources/application.properties`, or use env variables:

   ```bash
   # Linux / macOS
   export DB_USER=root DB_PASSWORD=your_password
   # Windows PowerShell
   $env:DB_USER="root"; $env:DB_PASSWORD="your_password"
   ```
   (The URL defaults to `jdbc:mysql://localhost:3306/airportbaggagems`; override with `DB_URL`.)
3. Start:
   ```bash
   mvn spring-boot:run
   ```
4. Open **http://localhost:8080**  (the sidebar shows a green "MySQL connected" light when JDBC works).

To build a runnable jar: `mvn clean package` then `java -jar target/baggage-system-1.0.0.jar`.
You can also open the folder in IntelliJ / Eclipse / VS Code as a Maven project and run `BaggageApplication`.

## Project layout
```
src/main/java/com/airport/baggage/
  BaggageApplication.java      entry point
  db/Db.java, DbException.java JDBC helper: query / update / transaction
  dao/*.java                   one DAO per table - all SQL lives here (PreparedStatements)
  model/*.java                 records: Baggage, Passenger, Flight, Claim, Country
  web/*.java                   REST controllers, validation, global error handler
src/main/resources/
  application.properties       DB + server config
  static/                      the website (index.html, css/style.css, js/app.js)
database/                      schema.sql, sample_data.sql
```

## REST API
| Method & path | Purpose |
|---|---|
| `GET /api/dashboard` | aggregate stats for the dashboard |
| `GET /api/baggage?q=&status=` · `POST` · `PUT /{id}` · `DELETE /{id}` | baggage CRUD (search + status filter) |
| `PATCH /api/baggage/{id}/status` | change status, body `{"baggageStatus":"Arrived"}` |
| `GET /api/baggage/track/{rfidTag or id}` | full journey: bag + flight + passenger + claim |
| `/api/passengers`, `/api/flights`, `/api/claims` | CRUD, `?q=` search (claims also `?status=`) |
| `POST /api/claims/{id}/collect` · `/report-lost` | transactional claim actions |
| `/api/countries` (key = 2-letter code) | CRUD |

## Notes
- Primary keys are generated as `MAX(id)+1` inside the DAO, so it works whether or not your ID columns are `AUTO_INCREMENT`.
- The normalisation tables (`passenger_2nf`, `passenger_3nf`, `passenger_mobile`) are not used by the app.
- There is no login; add Spring Security before exposing this beyond a lab / demo.
