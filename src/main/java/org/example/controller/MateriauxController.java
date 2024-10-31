package org.example.controller;

import org.example.model.Materiaux;
import org.example.service.MateriauxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/Materiaux")
@CrossOrigin(origins = "http://localhost:4200")
public class MateriauxController {
    @Autowired
    private MateriauxService materiauxService;

    @GetMapping
    public List<Materiaux> getAllMateriauxs() {
        return materiauxService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Materiaux> getMateriauxById(@PathVariable String id) {
        return materiauxService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createMateriaux(@RequestBody Materiaux blog) {
        materiauxService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMateriaux(@PathVariable String id, @RequestBody Materiaux blog) {
        blog.setId(id); // Assuming the id is a Long
        materiauxService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMateriaux

            (@PathVariable String id) {
        materiauxService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
