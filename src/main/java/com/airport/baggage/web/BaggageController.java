package com.airport.baggage.web;

import com.airport.baggage.dao.*;
import com.airport.baggage.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/baggage")
public class BaggageController {
    private final BaggageDao dao;
    private final FlightDao flights;
    private final PassengerDao passengers;
    private final ClaimDao claims;

    public BaggageController(BaggageDao dao, FlightDao flights, PassengerDao passengers, ClaimDao claims) {
        this.dao = dao; this.flights = flights; this.passengers = passengers; this.claims = claims;
    }

    @GetMapping
    public List<Baggage> list(@RequestParam(required = false) String q, @RequestParam(required = false) String status) {
        return dao.findAll(q, status);
    }

    @GetMapping("/{id}")
    public Baggage get(@PathVariable int id) {
        return dao.findById(id).orElseThrow(() -> ApiException.notFound("Baggage not found"));
    }

    /** Full journey of one bag, looked up by its RFID tag (or numeric baggage ID). */
    @GetMapping("/track/{tag}")
    public TrackingResult track(@PathVariable String tag) {
        Baggage b = dao.findByRfid(tag).or(() -> tag.matches("\\d+") ? dao.findById(Integer.parseInt(tag)) : java.util.Optional.empty())
                .orElseThrow(() -> ApiException.notFound("No baggage found for \"" + tag + "\""));
        return new TrackingResult(b,
                flights.findById(b.flightId()).orElse(null),
                passengers.findById(b.passengerId()).orElse(null),
                claims.findByBaggageId(b.baggageId()).orElse(null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Baggage create(@RequestBody Baggage in) {
        int id = dao.insert(clean(in));
        return dao.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Baggage update(@PathVariable int id, @RequestBody Baggage in) {
        if (!dao.update(id, clean(in))) throw ApiException.notFound("Baggage not found");
        return dao.findById(id).orElseThrow();
    }

    @PatchMapping("/{id}/status")
    public Baggage updateStatus(@PathVariable int id, @RequestBody Map<String, String> body) {
        String status = Check.oneOf(body.get("baggageStatus"), "Baggage status", Set.copyOf(Baggage.STATUS), null);
        if (!dao.updateStatus(id, status)) throw ApiException.notFound("Baggage not found");
        return dao.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        if (!dao.delete(id)) throw ApiException.notFound("Baggage not found");
    }

    private Baggage clean(Baggage b) {
        return new Baggage(null, Check.required(b.flightId(), "Flight"), Check.required(b.rfidTag(), "RFID tag", 30),
                Check.required(b.passengerId(), "Passenger"), Check.weight(b.weight()), Check.optional(b.color(), "Color", 30),
                Check.oneOf(b.securityStatus(), "Security status", Set.copyOf(Baggage.SECURITY), "Pending"),
                Check.oneOf(b.baggageStatus(), "Baggage status", Set.copyOf(Baggage.STATUS), "Checked_In"), null, null);
    }
}
