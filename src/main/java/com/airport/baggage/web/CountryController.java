package com.airport.baggage.web;

import com.airport.baggage.dao.CountryDao;
import com.airport.baggage.model.Country;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
public class CountryController {
    private final CountryDao dao;
    public CountryController(CountryDao dao) { this.dao = dao; }

    @GetMapping
    public List<Country> list(@RequestParam(required = false) String q) { return dao.findAll(q); }

    @GetMapping("/{code}")
    public Country get(@PathVariable String code) {
        return dao.findByCode(code).orElseThrow(() -> ApiException.notFound("Country not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Country create(@RequestBody Country in) {
        Country c = clean(in.countryCode(), in);
        dao.insert(c);
        return c;
    }

    @PutMapping("/{code}")
    public Country update(@PathVariable String code, @RequestBody Country in) {
        Country c = clean(code, in);
        if (!dao.update(code, c)) throw ApiException.notFound("Country not found");
        return c;
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String code) {
        if (!dao.delete(code)) throw ApiException.notFound("Country not found");
    }

    private Country clean(String code, Country in) {
        String cc = Check.required(code, "Country code", 2).toUpperCase();
        if (cc.length() != 2) throw ApiException.badRequest("Country code must be exactly 2 letters (e.g. IN)");
        return new Country(cc, Check.optional(in.nationality(), "Nationality", 30));
    }
}
