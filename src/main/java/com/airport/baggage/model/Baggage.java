package com.airport.baggage.model;

import java.math.BigDecimal;
import java.util.List;

/** flightNumber / passengerName are read-only values joined in for display. */
public record Baggage(Integer baggageId, Integer flightId, String rfidTag, Integer passengerId,
                      BigDecimal weight, String color, String securityStatus, String baggageStatus,
                      String flightNumber, String passengerName) {
    public static final List<String> SECURITY = List.of("Pending", "Cleared", "Failed");
    public static final List<String> STATUS = List.of("Checked_In", "In Transit", "Arrived", "Collected", "Lost");
}
