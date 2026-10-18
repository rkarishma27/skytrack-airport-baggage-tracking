package com.airport.baggage.db;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Thin plain-JDBC helper: Connection -> PreparedStatement -> ResultSet.
 * Every DAO goes through here, so all SQL is parameterised (no SQL injection)
 * and every resource is closed with try-with-resources.
 */
@Component
public class Db {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private final DataSource dataSource;

    public Db(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ---------- single statements (auto-commit) ----------

    public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = dataSource.getConnection()) {
            return run(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public int update(String sql, Object... params) {
        try (Connection c = dataSource.getConnection()) {
            return exec(c, sql, params);
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    // ---------- transactions ----------

    /** Runs several statements on ONE connection; commits on success, rolls back on any failure. */
    public <T> T transaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new Tx(c));
                c.commit();
                return result;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DbException(e);
        }
    }

    public static final class Tx {
        private final Connection c;
        private Tx(Connection c) { this.c = c; }

        public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
            try { return run(c, sql, mapper, params); } catch (SQLException e) { throw new DbException(e); }
        }
        public int update(String sql, Object... params) {
            try { return exec(c, sql, params); } catch (SQLException e) { throw new DbException(e); }
        }
    }

    // ---------- internals ----------

    private static <T> List<T> run(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        }
    }

    private static int exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object v = params[i];
            if (v == null) ps.setNull(i + 1, Types.NULL);
            else if (v instanceof LocalDateTime t) ps.setTimestamp(i + 1, Timestamp.valueOf(t));
            else ps.setObject(i + 1, v);
        }
    }

    // ---------- null-safe column readers for mappers ----------

    public static Integer getInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public static LocalDateTime getDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }
}
