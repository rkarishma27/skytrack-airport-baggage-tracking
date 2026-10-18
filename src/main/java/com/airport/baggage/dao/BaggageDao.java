package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import com.airport.baggage.model.Baggage;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class BaggageDao {
    private static final String SELECT =
            "SELECT b.Baggage_ID, b.Flight_ID, b.RFID_Tag, b.Passenger_ID, b.Weight, b.Color, b.Security_status, b.Baggage_Status, "
          + "f.Flight_Number, TRIM(CONCAT(COALESCE(p.First_Name,''), ' ', COALESCE(p.Last_Name,''))) AS Passenger_Name "
          + "FROM baggage b LEFT JOIN flight f ON f.Flight_ID = b.Flight_ID LEFT JOIN passenger p ON p.Passenger_ID = b.Passenger_ID";

    private final Db db;
    public BaggageDao(Db db) { this.db = db; }

    static Baggage map(ResultSet rs) throws SQLException {
        return new Baggage(rs.getInt("Baggage_ID"), rs.getInt("Flight_ID"), rs.getString("RFID_Tag"), rs.getInt("Passenger_ID"),
                rs.getBigDecimal("Weight"), rs.getString("Color"), rs.getString("Security_status"), rs.getString("Baggage_Status"),
                rs.getString("Flight_Number"), rs.getString("Passenger_Name"));
    }

    public List<Baggage> findAll(String q, String status) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (status != null && !status.isBlank()) { where.add("b.Baggage_Status = ?"); args.add(status); }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            where.add("(b.RFID_Tag LIKE ? OR b.Color LIKE ? OR f.Flight_Number LIKE ? OR p.First_Name LIKE ? OR p.Last_Name LIKE ? OR CAST(b.Baggage_ID AS CHAR) = ?)");
            args.addAll(List.of(like, like, like, like, like, q.trim()));
        }
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY b.Baggage_ID DESC");
        return db.query(sql.toString(), BaggageDao::map, args.toArray());
    }

    public Optional<Baggage> findById(int id) {
        return db.queryOne(SELECT + " WHERE b.Baggage_ID = ?", BaggageDao::map, id);
    }

    public Optional<Baggage> findByRfid(String rfid) {
        return db.queryOne(SELECT + " WHERE b.RFID_Tag = ?", BaggageDao::map, rfid);
    }

    public synchronized int insert(Baggage b) {
        int id = db.queryOne("SELECT COALESCE(MAX(Baggage_ID),0)+1 AS n FROM baggage", rs -> rs.getInt("n")).orElse(1);
        db.update("INSERT INTO baggage (Baggage_ID, Flight_ID, RFID_Tag, Passenger_ID, Weight, Color, Security_status, Baggage_Status) VALUES (?,?,?,?,?,?,?,?)",
                id, b.flightId(), b.rfidTag(), b.passengerId(), b.weight(), b.color(), b.securityStatus(), b.baggageStatus());
        return id;
    }

    public boolean update(int id, Baggage b) {
        db.update("UPDATE baggage SET Flight_ID=?, RFID_Tag=?, Passenger_ID=?, Weight=?, Color=?, Security_status=?, Baggage_Status=? WHERE Baggage_ID=?",
                b.flightId(), b.rfidTag(), b.passengerId(), b.weight(), b.color(), b.securityStatus(), b.baggageStatus(), id);
        return findById(id).isPresent();
    }

    /** Changes the baggage status and keeps the matching claim row consistent - one transaction. */
    public boolean updateStatus(int id, String status) {
        return db.transaction(tx -> {
            int n = tx.update("UPDATE baggage SET Baggage_Status = ? WHERE Baggage_ID = ?", status, id);
            if (n == 0 && tx.query("SELECT 1 AS x FROM baggage WHERE Baggage_ID = ?", rs -> 1, id).isEmpty()) return false;
            if ("Lost".equals(status)) {
                tx.update("UPDATE baggage_claim SET Lost_Status = 'Yes' WHERE Baggage_ID = ?", id);
            } else if ("Collected".equals(status)) {
                tx.update("UPDATE baggage_claim SET Claim_Status = 'Collected', Lost_Status = 'No', "
                        + "Collection_Time = COALESCE(Collection_Time, NOW()) WHERE Baggage_ID = ?", id);
            } else {
                tx.update("UPDATE baggage_claim SET Lost_Status = 'No' WHERE Baggage_ID = ?", id);
            }
            return true;
        });
    }

    public boolean delete(int id) {
        return db.update("DELETE FROM baggage WHERE Baggage_ID = ?", id) > 0;
    }
}
