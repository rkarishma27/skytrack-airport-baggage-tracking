package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import com.airport.baggage.model.Passenger;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PassengerDao {
    private static final String SELECT = "SELECT Passenger_ID, Passport_Number, First_Name, Last_Name, Mobile_Number, Email, Nationality FROM passenger";
    private final Db db;
    public PassengerDao(Db db) { this.db = db; }

    static Passenger map(ResultSet rs) throws SQLException {
        return new Passenger(rs.getInt("Passenger_ID"), rs.getString("Passport_Number"), rs.getString("First_Name"),
                rs.getString("Last_Name"), rs.getString("Mobile_Number"), rs.getString("Email"), rs.getString("Nationality"));
    }

    public List<Passenger> findAll(String q) {
        if (q == null || q.isBlank()) return db.query(SELECT + " ORDER BY Passenger_ID DESC", PassengerDao::map);
        String like = "%" + q.trim() + "%";
        return db.query(SELECT + " WHERE First_Name LIKE ? OR Last_Name LIKE ? OR Passport_Number LIKE ? OR Email LIKE ? "
                        + "OR Mobile_Number LIKE ? OR Nationality LIKE ? OR CAST(Passenger_ID AS CHAR) = ? ORDER BY Passenger_ID DESC",
                PassengerDao::map, like, like, like, like, like, like, q.trim());
    }

    public Optional<Passenger> findById(int id) {
        return db.queryOne(SELECT + " WHERE Passenger_ID = ?", PassengerDao::map, id);
    }

    /** IDs are generated as MAX+1 so this works whether or not the column is AUTO_INCREMENT. */
    public synchronized int insert(Passenger p) {
        int id = db.queryOne("SELECT COALESCE(MAX(Passenger_ID),0)+1 AS n FROM passenger", rs -> rs.getInt("n")).orElse(1);
        db.update("INSERT INTO passenger (Passenger_ID, Passport_Number, First_Name, Last_Name, Mobile_Number, Email, Nationality) VALUES (?,?,?,?,?,?,?)",
                id, p.passportNumber(), p.firstName(), p.lastName(), p.mobileNumber(), p.email(), p.nationality());
        return id;
    }

    public boolean update(int id, Passenger p) {
        db.update("UPDATE passenger SET Passport_Number=?, First_Name=?, Last_Name=?, Mobile_Number=?, Email=?, Nationality=? WHERE Passenger_ID=?",
                p.passportNumber(), p.firstName(), p.lastName(), p.mobileNumber(), p.email(), p.nationality(), id);
        return findById(id).isPresent();
    }

    public boolean delete(int id) {
        return db.update("DELETE FROM passenger WHERE Passenger_ID = ?", id) > 0;
    }
}
