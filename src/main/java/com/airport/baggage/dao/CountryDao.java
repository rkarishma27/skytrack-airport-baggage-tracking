package com.airport.baggage.dao;

import com.airport.baggage.db.Db;
import com.airport.baggage.model.Country;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CountryDao {
    private final Db db;
    public CountryDao(Db db) { this.db = db; }

    private static Country map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Country(rs.getString("Country_Code"), rs.getString("Nationality"));
    }

    public List<Country> findAll(String q) {
        if (q == null || q.isBlank())
            return db.query("SELECT Country_Code, Nationality FROM country ORDER BY Country_Code", CountryDao::map);
        String like = "%" + q.trim() + "%";
        return db.query("SELECT Country_Code, Nationality FROM country WHERE Country_Code LIKE ? OR Nationality LIKE ? ORDER BY Country_Code",
                CountryDao::map, like, like);
    }

    public Optional<Country> findByCode(String code) {
        return db.queryOne("SELECT Country_Code, Nationality FROM country WHERE Country_Code = ?", CountryDao::map, code);
    }

    public void insert(Country c) {
        db.update("INSERT INTO country (Country_Code, Nationality) VALUES (?, ?)", c.countryCode(), c.nationality());
    }

    public boolean update(String code, Country c) {
        return db.update("UPDATE country SET Nationality = ? WHERE Country_Code = ?", c.nationality(), code) > 0
                || findByCode(code).isPresent();
    }

    public boolean delete(String code) {
        return db.update("DELETE FROM country WHERE Country_Code = ?", code) > 0;
    }
}
