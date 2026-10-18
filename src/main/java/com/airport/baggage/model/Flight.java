package com.airport.baggage.model;

import java.time.LocalDateTime;
import java.util.List;

public record Flight(Integer flightId, String flightNumber, String airline, String flightType,
                     String departure, String arrival, LocalDateTime departureTime, String flightStatus) {
    public static final List<String> TYPES = List.of("Domestic", "International");
}
