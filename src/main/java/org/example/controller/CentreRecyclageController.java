package org.example.controller;

import org.example.model.CentreRecyclage;
import org.example.service.CentreRecyclageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.apache.jena.rdf.model.impl.RDFDefaultErrorHandler.logger;

@RestController
@RequestMapping("/CentreRecyclage")
@CrossOrigin(origins = "http://localhost:4200")
public class CentreRecyclageController {
    @Autowired
    private CentreRecyclageService centreRecyclageService;

    @GetMapping
    public List<CentreRecyclage> getAllCentreRecyclages() {
        List<CentreRecyclage> centres = centreRecyclageService.findAll();
        if (centres.isEmpty()) {
            logger.warn("Aucun centre de recyclage trouvé.");
        }
        return centres;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CentreRecyclage> getCentreRecyclageById(@PathVariable String id) {
        return centreRecyclageService.findById(id) // Using String id
                .map(centre -> new ResponseEntity<>(centre, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Void> createCentreRecyclage(@RequestBody CentreRecyclage centre) {
        centreRecyclageService.save(centre);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCentreRecyclage(@PathVariable String id, @RequestBody CentreRecyclage centre) {
        centre.setId(id); // Set ID as String
        centreRecyclageService.update(centre);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCentreRecyclage(@PathVariable String id) {
        centreRecyclageService.deleteById(id); // Using String id
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /* @GetMapping("/search")
    public ResponseEntity<List<CentreRecyclage>> searchCentreRecyclages(@RequestParam String query) {
        List<CentreRecyclage> foundCentreRecyclages = centreRecyclageService.searchCentreRecyclages(query);
        if (foundCentreRecyclages.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(foundCentreRecyclages, HttpStatus.OK);
    } */
}
