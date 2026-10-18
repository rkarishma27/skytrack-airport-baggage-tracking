package com.airport.baggage.web;

import com.airport.baggage.dao.FlightDao;
import com.airport.baggage.model.Flight;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightDao dao;
    public FlightController(FlightDao dao) { this.dao = dao; }

    @GetMapping
    public List<Flight> list(@RequestParam(required = false) String q) { return dao.findAll(q); }

    @GetMapping("/{id}")
    public Flight get(@PathVariable int id) {
        return dao.findById(id).orElseThrow(() -> ApiException.notFound("Flight not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Flight create(@RequestBody Flight in) {
        int id = dao.insert(clean(in));
        return dao.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Flight update(@PathVariable int id, @RequestBody Flight in) {
        if (!dao.update(id, clean(in))) throw ApiException.notFound("Flight not found");
        return dao.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        if (!dao.delete(id)) throw ApiException.notFound("Flight not found");
    }

    private Flight clean(Flight f) {
        return new Flight(null, Check.required(f.flightNumber(), "Flight number", 10).toUpperCase(),
                Check.required(f.airline(), "Airline", 50),
                Check.oneOf(f.flightType(), "Flight type", Set.copyOf(Flight.TYPES), null),
                Check.required(f.departure(), "Departure", 50), Check.required(f.arrival(), "Arrival", 50),
                Check.required(f.departureTime(), "Departure time"),
                Check.optional(f.flightStatus(), "Flight status", 20));
    }
}
