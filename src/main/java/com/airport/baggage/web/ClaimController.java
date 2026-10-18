package com.airport.baggage.web;

import com.airport.baggage.dao.ClaimDao;
import com.airport.baggage.model.Claim;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimDao dao;
    public ClaimController(ClaimDao dao) { this.dao = dao; }

    @GetMapping
    public List<Claim> list(@RequestParam(required = false) String q, @RequestParam(required = false) String status) {
        return dao.findAll(q, status);
    }

    @GetMapping("/{id}")
    public Claim get(@PathVariable int id) {
        return dao.findById(id).orElseThrow(() -> ApiException.notFound("Claim not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Claim create(@RequestBody Claim in) {
        int id = dao.insert(clean(in));
        return dao.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Claim update(@PathVariable int id, @RequestBody Claim in) {
        if (!dao.update(id, clean(in))) throw ApiException.notFound("Claim not found");
        return dao.findById(id).orElseThrow();
    }

    @PostMapping("/{id}/collect")
    public Claim collect(@PathVariable int id) {
        if (!dao.markCollected(id)) throw ApiException.notFound("Claim not found");
        return dao.findById(id).orElseThrow();
    }

    @PostMapping("/{id}/report-lost")
    public Claim reportLost(@PathVariable int id) {
        if (!dao.markLost(id)) throw ApiException.notFound("Claim not found");
        return dao.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        if (!dao.delete(id)) throw ApiException.notFound("Claim not found");
    }

    private Claim clean(Claim c) {
        return new Claim(null, Check.required(c.baggageId(), "Baggage"), Check.optional(c.beltNumber(), "Belt number", 10),
                c.collectionTime(),
                Check.oneOf(c.claimStatus(), "Claim status", Set.copyOf(Claim.CLAIM_STATUS), "Pending"),
                Check.oneOf(c.lostStatus(), "Lost status", Set.copyOf(Claim.LOST), "No"),
                Check.optional(c.currentLocation(), "Current location", 100), null, null);
    }
}
