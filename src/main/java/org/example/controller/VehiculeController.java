package org.example.controller;

import org.example.model.Vehicule;
import org.example.service.VehiculeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/CentreRecyclage")
@CrossOrigin(origins = "http://localhost:4200")
public class VehiculeController {
    @Autowired
    private VehiculeService vehiculeService;

    @GetMapping
    public List<Vehicule> getAllVehicules() {
        return vehiculeService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<Vehicule> getVehiculeById(@PathVariable String id) {
        return vehiculeService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createVehicule(@RequestBody Vehicule blog) {
        vehiculeService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVehicule(@PathVariable String id, @RequestBody Vehicule blog) {
        blog.setId(id); // Assuming the id is a Long
        vehiculeService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicule

            (@PathVariable String id) {
        vehiculeService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
