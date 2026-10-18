package com.airport.baggage.db;

import java.sql.SQLException;

public class DbException extends RuntimeException {
    private final int sqlErrorCode;

    public DbException(SQLException cause) {
        super(cause.getMessage(), cause);
        this.sqlErrorCode = cause.getErrorCode();
    }

    /** MySQL error code, e.g. 1062 duplicate key, 1451 child rows exist, 1452 parent row missing. */
    public int getSqlErrorCode() { return sqlErrorCode; }
}
