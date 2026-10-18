-- Reference schema for the 5 core tables (matches the structure of your airportbaggagems database).
-- Only needed if you want to rebuild the database from scratch; your existing tables work as they are.
CREATE DATABASE IF NOT EXISTS airportbaggagems;
USE airportbaggagems;

CREATE TABLE IF NOT EXISTS country (
  Country_Code CHAR(2)     NOT NULL PRIMARY KEY,
  Nationality  VARCHAR(30)
);

CREATE TABLE IF NOT EXISTS passenger (
  Passenger_ID    INT         NOT NULL PRIMARY KEY,
  Passport_Number VARCHAR(20) UNIQUE,
  First_Name      VARCHAR(50),
  Last_Name       VARCHAR(50),
  Mobile_Number   VARCHAR(30),
  Email           VARCHAR(100),
  Nationality     VARCHAR(50),
  INDEX idx_passenger_first_name (First_Name)
);

CREATE TABLE IF NOT EXISTS flight (
  Flight_ID      INT         NOT NULL PRIMARY KEY,
  Flight_Number  VARCHAR(10) NOT NULL UNIQUE,
  Airline        VARCHAR(50) NOT NULL,
  Flight_Type    ENUM('Domestic','International') NOT NULL,
  Departure      VARCHAR(50) NOT NULL,
  Arrival        VARCHAR(50) NOT NULL,
  Departure_Time DATETIME    NOT NULL,
  Flight_Status  VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS baggage (
  Baggage_ID      INT         NOT NULL PRIMARY KEY,
  Flight_ID       INT         NOT NULL,
  RFID_Tag        VARCHAR(30) NOT NULL UNIQUE,
  Passenger_ID    INT         NOT NULL,
  Weight          DECIMAL(5,2),
  Color           VARCHAR(30),
  Security_status ENUM('Pending','Cleared','Failed'),
  Baggage_Status  ENUM('Checked_In','In Transit','Arrived','Collected','Lost'),
  FOREIGN KEY (Flight_ID)    REFERENCES flight(Flight_ID),
  FOREIGN KEY (Passenger_ID) REFERENCES passenger(Passenger_ID)
);

CREATE TABLE IF NOT EXISTS baggage_claim (
  Claim_ID         INT NOT NULL PRIMARY KEY,
  Baggage_ID       INT UNIQUE,
  Belt_Number      VARCHAR(10),
  Collection_Time  DATETIME,
  Claim_Status     ENUM('Pending','Collected'),
  Lost_Status      ENUM('Yes','No'),
  Current_Location VARCHAR(100),
  FOREIGN KEY (Baggage_ID) REFERENCES baggage(Baggage_ID)
);
