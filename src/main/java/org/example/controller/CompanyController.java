package org.example.controller;

import org.example.model.Company;
import org.example.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/CollectDechet")
@CrossOrigin(origins = "http://localhost:4200")
public class CompanyController {
    @Autowired
    private CompanyService companyService;

    @GetMapping
    public List<Company> getAllCompanys() {
        return companyService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable String id) {
        return companyService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createCompany(@RequestBody Company blog) {
        companyService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCompany(@PathVariable String id, @RequestBody Company blog) {
        blog.setId(id); // Assuming the id is a Long
        companyService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany

            (@PathVariable String id) {
        companyService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
