package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import com.airport.baggage.model.Flight;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class FlightDao {
    private static final String SELECT = "SELECT Flight_ID, Flight_Number, Airline, Flight_Type, Departure, Arrival, Departure_Time, Flight_Status FROM flight";
    private final Db db;
    public FlightDao(Db db) { this.db = db; }

    static Flight map(ResultSet rs) throws SQLException {
        return new Flight(rs.getInt("Flight_ID"), rs.getString("Flight_Number"), rs.getString("Airline"),
                rs.getString("Flight_Type"), rs.getString("Departure"), rs.getString("Arrival"),
                Db.getDateTime(rs, "Departure_Time"), rs.getString("Flight_Status"));
    }

    public List<Flight> findAll(String q) {
        if (q == null || q.isBlank()) return db.query(SELECT + " ORDER BY Departure_Time DESC", FlightDao::map);
        String like = "%" + q.trim() + "%";
        return db.query(SELECT + " WHERE Flight_Number LIKE ? OR Airline LIKE ? OR Departure LIKE ? OR Arrival LIKE ? OR Flight_Status LIKE ? OR Flight_Type LIKE ? ORDER BY Departure_Time DESC",
                FlightDao::map, like, like, like, like, like, like);
    }

    public Optional<Flight> findById(int id) {
        return db.queryOne(SELECT + " WHERE Flight_ID = ?", FlightDao::map, id);
    }

    public synchronized int insert(Flight f) {
        int id = db.queryOne("SELECT COALESCE(MAX(Flight_ID),0)+1 AS n FROM flight", rs -> rs.getInt("n")).orElse(1);
        db.update("INSERT INTO flight (Flight_ID, Flight_Number, Airline, Flight_Type, Departure, Arrival, Departure_Time, Flight_Status) VALUES (?,?,?,?,?,?,?,?)",
                id, f.flightNumber(), f.airline(), f.flightType(), f.departure(), f.arrival(), f.departureTime(), f.flightStatus());
        return id;
    }

    public boolean update(int id, Flight f) {
        db.update("UPDATE flight SET Flight_Number=?, Airline=?, Flight_Type=?, Departure=?, Arrival=?, Departure_Time=?, Flight_Status=? WHERE Flight_ID=?",
                f.flightNumber(), f.airline(), f.flightType(), f.departure(), f.arrival(), f.departureTime(), f.flightStatus(), id);
        return findById(id).isPresent();
    }

    public boolean delete(int id) {
        return db.update("DELETE FROM flight WHERE Flight_ID = ?", id) > 0;
    }
}
