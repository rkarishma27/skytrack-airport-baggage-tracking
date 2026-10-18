package com.airport.baggage.model;

public record Passenger(Integer passengerId, String passportNumber, String firstName, String lastName,
                        String mobileNumber, String email, String nationality) {}
