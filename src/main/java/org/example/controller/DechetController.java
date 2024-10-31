package org.example.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/Blog")
@CrossOrigin(origins = "http://localhost:4200")  // Enable CORS for this controller
public class DechetController {
}
