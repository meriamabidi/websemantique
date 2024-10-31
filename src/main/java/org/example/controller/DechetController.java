package org.example.controller;

import org.example.model.Dechet;
import org.example.service.DechetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Dechet")
@CrossOrigin(origins = "http://localhost:4200")  // Enable CORS for this controller
public class DechetController {
    @Autowired
    private DechetService dechetService;

    @GetMapping
    public List<Dechet> getAllDechets() {
        return dechetService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Dechet> getDechetById(@PathVariable String id) {
        return dechetService.findById(Long.valueOf(id))
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createDechet(@RequestBody Dechet blog) {
        dechetService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDechet(@PathVariable String id, @RequestBody Dechet blog) {
        blog.setId(Long.valueOf(id)); // Assuming the id is a Long
        dechetService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDechet

            (@PathVariable String id) {
        dechetService.deleteById(Long.valueOf(id));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
