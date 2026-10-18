package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only aggregate queries for the dashboard. */
@Repository
public class DashboardDao {
    private final Db db;
    public DashboardDao(Db db) { this.db = db; }

    private List<Map<String, Object>> grouped(String sql) {
        return db.query(sql, rs -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", rs.getString("label"));
            m.put("value", rs.getInt("value"));
            return m;
        });
    }

    public Map<String, Object> summary() {
        Map<String, Object> out = new LinkedHashMap<>();

        out.put("totals", db.queryOne(
                "SELECT (SELECT COUNT(*) FROM baggage) AS bags, (SELECT COUNT(*) FROM passenger) AS passengers, "
              + "(SELECT COUNT(*) FROM flight) AS flights, (SELECT COUNT(*) FROM country) AS countries, "
              + "(SELECT COUNT(*) FROM baggage WHERE Baggage_Status='Lost') AS lost, "
              + "(SELECT COUNT(*) FROM baggage_claim WHERE Claim_Status='Pending') AS pendingClaims, "
              + "(SELECT COUNT(*) FROM baggage WHERE Security_status='Failed') AS securityFailed, "
              + "(SELECT COALESCE(ROUND(AVG(Weight),1),0) FROM baggage) AS avgWeight, "
              + "(SELECT COALESCE(ROUND(SUM(Weight),1),0) FROM baggage) AS totalWeight",
                rs -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    for (String k : List.of("bags", "passengers", "flights", "countries", "lost", "pendingClaims", "securityFailed"))
                        m.put(k, rs.getInt(k));
                    m.put("avgWeight", rs.getBigDecimal("avgWeight"));
                    m.put("totalWeight", rs.getBigDecimal("totalWeight"));
                    return m;
                }).orElse(Map.of()));

        out.put("byStatus", grouped("SELECT COALESCE(Baggage_Status,'Unknown') AS label, COUNT(*) AS value FROM baggage GROUP BY Baggage_Status ORDER BY value DESC"));
        out.put("bySecurity", grouped("SELECT COALESCE(Security_status,'Unknown') AS label, COUNT(*) AS value FROM baggage GROUP BY Security_status ORDER BY value DESC"));
        out.put("flightsByStatus", grouped("SELECT COALESCE(Flight_Status,'Unknown') AS label, COUNT(*) AS value FROM flight GROUP BY Flight_Status ORDER BY value DESC"));
        out.put("flightsByType", grouped("SELECT Flight_Type AS label, COUNT(*) AS value FROM flight GROUP BY Flight_Type"));
        out.put("topFlights", grouped("SELECT f.Flight_Number AS label, COUNT(b.Baggage_ID) AS value FROM flight f "
                + "JOIN baggage b ON b.Flight_ID = f.Flight_ID GROUP BY f.Flight_ID, f.Flight_Number ORDER BY value DESC, f.Flight_Number LIMIT 6"));
        out.put("recent", db.query(
                "SELECT b.Baggage_ID, b.RFID_Tag, b.Baggage_Status, f.Flight_Number, "
              + "TRIM(CONCAT(COALESCE(p.First_Name,''),' ',COALESCE(p.Last_Name,''))) AS pname "
              + "FROM baggage b LEFT JOIN flight f ON f.Flight_ID=b.Flight_ID LEFT JOIN passenger p ON p.Passenger_ID=b.Passenger_ID "
              + "ORDER BY b.Baggage_ID DESC LIMIT 8",
                rs -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("baggageId", rs.getInt("Baggage_ID"));
                    m.put("rfidTag", rs.getString("RFID_Tag"));
                    m.put("status", rs.getString("Baggage_Status"));
                    m.put("flightNumber", rs.getString("Flight_Number"));
                    m.put("passengerName", rs.getString("pname"));
                    return m;
                }));
        return out;
    }
}
