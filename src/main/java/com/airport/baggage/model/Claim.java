package com.airport.baggage.model;

import java.time.LocalDateTime;
import java.util.List;

/** rfidTag / passengerName are read-only values joined in for display. */
public record Claim(Integer claimId, Integer baggageId, String beltNumber, LocalDateTime collectionTime,
                    String claimStatus, String lostStatus, String currentLocation,
                    String rfidTag, String passengerName) {
    public static final List<String> CLAIM_STATUS = List.of("Pending", "Collected");
    public static final List<String> LOST = List.of("Yes", "No");
}
