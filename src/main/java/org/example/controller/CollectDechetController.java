package org.example.controller;

import org.example.model.CollectDechet;
import org.example.service.CollectDechetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/CollectDechet")
@CrossOrigin(origins = "http://localhost:4200")
public class CollectDechetController {
    @Autowired
    private CollectDechetService collectDechetService;

    @GetMapping
    public List<CollectDechet> getAllCollectDechets() {
        return collectDechetService.findAll();
    }
    @GetMapping("/{id}")
    public ResponseEntity<CollectDechet> getCollectDechetById(@PathVariable String id) {
        return collectDechetService.findById(id)
                .map(hebergment -> new ResponseEntity<>(hebergment, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
    @PostMapping
    public ResponseEntity<Void> createCollectDechet(@RequestBody CollectDechet blog) {
        collectDechetService.save(blog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCollectDechet(@PathVariable String id, @RequestBody CollectDechet blog) {
        blog.setId(id); // Assuming the id is a Long
        collectDechetService.update(blog);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollectDechet

            (@PathVariable String id) {
        collectDechetService.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @GetMapping("/search/{keyword}")
    public ResponseEntity<List<CollectDechet>> searchCollectDechets(@PathVariable String keyword) {
        List<CollectDechet> foundCentreRecyclages = collectDechetService.searchCollectDechets(keyword);
        if (foundCentreRecyclages.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(foundCentreRecyclages, HttpStatus.OK);
    }
}
