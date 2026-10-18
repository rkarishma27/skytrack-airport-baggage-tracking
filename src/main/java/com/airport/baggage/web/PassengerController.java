package com.airport.baggage.web;

import com.airport.baggage.dao.PassengerDao;
import com.airport.baggage.model.Passenger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {
    private final PassengerDao dao;
    public PassengerController(PassengerDao dao) { this.dao = dao; }

    @GetMapping
    public List<Passenger> list(@RequestParam(required = false) String q) { return dao.findAll(q); }

    @GetMapping("/{id}")
    public Passenger get(@PathVariable int id) {
        return dao.findById(id).orElseThrow(() -> ApiException.notFound("Passenger not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Passenger create(@RequestBody Passenger in) {
        int id = dao.insert(clean(in));
        return dao.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Passenger update(@PathVariable int id, @RequestBody Passenger in) {
        if (!dao.update(id, clean(in))) throw ApiException.notFound("Passenger not found");
        return dao.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        if (!dao.delete(id)) throw ApiException.notFound("Passenger not found");
    }

    private Passenger clean(Passenger p) {
        String email = Check.optional(p.email(), "Email", 100);
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw ApiException.badRequest("Email is not valid");
        return new Passenger(null, Check.optional(p.passportNumber(), "Passport number", 20),
                Check.required(p.firstName(), "First name", 50), Check.optional(p.lastName(), "Last name", 50),
                Check.optional(p.mobileNumber(), "Mobile number", 30), email,
                Check.optional(p.nationality(), "Nationality", 50));
    }
}
