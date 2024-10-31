package org.example.controller;

import org.example.model.CentreRecyclage;
import org.example.service.CentreRecyclageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/CentreRecyclage")
@CrossOrigin(origins = "http://localhost:4200")
public class CentreRecyclageController {
    @Autowired
    private CentreRecyclageService centreRecyclageService;

    @GetMapping
    public List<CentreRecyclage> getAllCentreRecyclages() {
        return centreRecyclageService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<CentreRecyclage> getCentreRecyclageById(@PathVariable String id) {
        return centreRecyclageService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createCentreRecyclage(@RequestBody CentreRecyclage blog) {
        centreRecyclageService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCentreRecyclage(@PathVariable String id, @RequestBody CentreRecyclage blog) {
        blog.setId(id); // Assuming the id is a Long
        centreRecyclageService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCentreRecyclage

            (@PathVariable String id) {
        centreRecyclageService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  /*  @GetMapping("/search")
    public ResponseEntity<List<CentreRecyclage>> searchCentreRecyclages(@RequestParam String query) {
        List<CentreRecyclage> foundCentreRecyclages = centreRecyclageService.searchCentreRecyclages(query);
        if (foundCentreRecyclages.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(foundCentreRecyclages, HttpStatus.OK);
    }*/
}