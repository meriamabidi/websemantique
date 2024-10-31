package org.example.controller;

import org.example.model.MethodeTraitement;
import org.example.service.MethodeTraitementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Traitement")
@CrossOrigin(origins = "http://localhost:4200")
public class MethodeTraitementController {
    @Autowired
    private MethodeTraitementService methodeTraitementService;

    @GetMapping
    public List<MethodeTraitement> getAllMethodeTraitements() {
        return methodeTraitementService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<MethodeTraitement> getMethodeTraitementById(@PathVariable String id) {
        return methodeTraitementService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createMethodeTraitement(@RequestBody MethodeTraitement blog) {
        methodeTraitementService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMethodeTraitement(@PathVariable String id, @RequestBody MethodeTraitement blog) {
        blog.setId(id); // Assuming the id is a Long
        methodeTraitementService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMethodeTraitement

            (@PathVariable String id) {
        methodeTraitementService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
