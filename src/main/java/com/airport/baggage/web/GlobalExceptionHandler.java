package com.airport.baggage.web;

import com.airport.baggage.db.DbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Turns exceptions into clean JSON: {"error": "..."} so the UI can show a friendly toast. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DbException.class)
    public ResponseEntity<Map<String, String>> db(DbException e) {
        switch (e.getSqlErrorCode()) {
            case 1062: return err(HttpStatus.CONFLICT, "Duplicate value: a record with the same unique field (ID / RFID tag / passport / flight number) already exists.");
            case 1451: return err(HttpStatus.CONFLICT, "Cannot delete: other records (baggage, claims...) still reference this one.");
            case 1452: return err(HttpStatus.BAD_REQUEST, "Invalid reference: the selected flight / passenger / baggage does not exist.");
            case 1048: return err(HttpStatus.BAD_REQUEST, "A required field is missing.");
            case 1265: case 1366: case 1292: case 1406:
                return err(HttpStatus.BAD_REQUEST, "A value is too long or in the wrong format.");
            default:
                log.error("Database error", e);
                return err(HttpStatus.INTERNAL_SERVER_ERROR, "Database error. Check the server log.");
        }
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return err(HttpStatus.BAD_REQUEST, "Malformed request body (check numbers and date formats).");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception e) {
        log.error("Unexpected error", e);
        return err(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");
    }

    private static ResponseEntity<Map<String, String>> err(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("error", m));
    }
}
