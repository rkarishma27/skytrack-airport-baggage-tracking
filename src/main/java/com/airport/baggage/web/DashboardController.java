package com.airport.baggage.web;

import com.airport.baggage.dao.DashboardDao;
import com.airport.baggage.model.Baggage;
import com.airport.baggage.model.Claim;
import com.airport.baggage.model.Flight;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardDao dao;
    public DashboardController(DashboardDao dao) { this.dao = dao; }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() { return dao.summary(); }

    /** Allowed enum values, so the UI dropdowns always match the database. */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return Map.of("baggageStatus", Baggage.STATUS, "securityStatus", Baggage.SECURITY,
                "claimStatus", Claim.CLAIM_STATUS, "lostStatus", Claim.LOST, "flightType", Flight.TYPES);
    }
}
