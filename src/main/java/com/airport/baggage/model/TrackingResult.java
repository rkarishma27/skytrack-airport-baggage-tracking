package com.airport.baggage.model;

public record TrackingResult(Baggage baggage, Flight flight, Passenger passenger, Claim claim) {}
