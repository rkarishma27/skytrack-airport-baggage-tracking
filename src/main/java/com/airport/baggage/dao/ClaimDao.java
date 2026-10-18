package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import com.airport.baggage.model.Claim;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ClaimDao {
    private static final String SELECT =
            "SELECT c.Claim_ID, c.Baggage_ID, c.Belt_Number, c.Collection_Time, c.Claim_Status, c.Lost_Status, c.Current_Location, "
          + "b.RFID_Tag, TRIM(CONCAT(COALESCE(p.First_Name,''), ' ', COALESCE(p.Last_Name,''))) AS Passenger_Name "
          + "FROM baggage_claim c LEFT JOIN baggage b ON b.Baggage_ID = c.Baggage_ID "
          + "LEFT JOIN passenger p ON p.Passenger_ID = b.Passenger_ID";

    private final Db db;
    public ClaimDao(Db db) { this.db = db; }

    static Claim map(ResultSet rs) throws SQLException {
        return new Claim(rs.getInt("Claim_ID"), Db.getInt(rs, "Baggage_ID"), rs.getString("Belt_Number"),
                Db.getDateTime(rs, "Collection_Time"), rs.getString("Claim_Status"), rs.getString("Lost_Status"),
                rs.getString("Current_Location"), rs.getString("RFID_Tag"), rs.getString("Passenger_Name"));
    }

    public List<Claim> findAll(String q, String status) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>();
        List<String> where = new ArrayList<>();
        if (status != null && !status.isBlank()) { where.add("c.Claim_Status = ?"); args.add(status); }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim() + "%";
            where.add("(b.RFID_Tag LIKE ? OR c.Belt_Number LIKE ? OR c.Current_Location LIKE ? OR p.First_Name LIKE ? OR p.Last_Name LIKE ? OR CAST(c.Claim_ID AS CHAR) = ?)");
            args.addAll(List.of(like, like, like, like, like, q.trim()));
        }
        if (!where.isEmpty()) sql.append(" WHERE ").append(String.join(" AND ", where));
        sql.append(" ORDER BY c.Claim_ID DESC");
        return db.query(sql.toString(), ClaimDao::map, args.toArray());
    }

    public Optional<Claim> findById(int id) {
        return db.queryOne(SELECT + " WHERE c.Claim_ID = ?", ClaimDao::map, id);
    }

    public Optional<Claim> findByBaggageId(int baggageId) {
        return db.queryOne(SELECT + " WHERE c.Baggage_ID = ?", ClaimDao::map, baggageId);
    }

    public synchronized int insert(Claim c) {
        int id = db.queryOne("SELECT COALESCE(MAX(Claim_ID),0)+1 AS n FROM baggage_claim", rs -> rs.getInt("n")).orElse(1);
        db.update("INSERT INTO baggage_claim (Claim_ID, Baggage_ID, Belt_Number, Collection_Time, Claim_Status, Lost_Status, Current_Location) VALUES (?,?,?,?,?,?,?)",
                id, c.baggageId(), c.beltNumber(), c.collectionTime(), c.claimStatus(), c.lostStatus(), c.currentLocation());
        return id;
    }

    public boolean update(int id, Claim c) {
        db.update("UPDATE baggage_claim SET Baggage_ID=?, Belt_Number=?, Collection_Time=?, Claim_Status=?, Lost_Status=?, Current_Location=? WHERE Claim_ID=?",
                c.baggageId(), c.beltNumber(), c.collectionTime(), c.claimStatus(), c.lostStatus(), c.currentLocation(), id);
        return findById(id).isPresent();
    }

    /** Passenger collects the bag: claim + baggage updated together. */
    public boolean markCollected(int claimId) {
        return db.transaction(tx -> {
            if (tx.query("SELECT 1 AS x FROM baggage_claim WHERE Claim_ID = ?", rs -> 1, claimId).isEmpty()) return false;
            tx.update("UPDATE baggage_claim SET Claim_Status='Collected', Lost_Status='No', Collection_Time=NOW() WHERE Claim_ID=?", claimId);
            tx.update("UPDATE baggage SET Baggage_Status='Collected' WHERE Baggage_ID = (SELECT Baggage_ID FROM (SELECT Baggage_ID FROM baggage_claim WHERE Claim_ID=?) t)", claimId);
            return true;
        });
    }

    /** Bag could not be found: claim flagged lost and baggage marked Lost. */
    public boolean markLost(int claimId) {
        return db.transaction(tx -> {
            if (tx.query("SELECT 1 AS x FROM baggage_claim WHERE Claim_ID = ?", rs -> 1, claimId).isEmpty()) return false;
            tx.update("UPDATE baggage_claim SET Lost_Status='Yes', Claim_Status='Pending' WHERE Claim_ID=?", claimId);
            tx.update("UPDATE baggage SET Baggage_Status='Lost' WHERE Baggage_ID = (SELECT Baggage_ID FROM (SELECT Baggage_ID FROM baggage_claim WHERE Claim_ID=?) t)", claimId);
            return true;
        });
    }

    public boolean delete(int id) {
        return db.update("DELETE FROM baggage_claim WHERE Claim_ID = ?", id) > 0;
    }
}
