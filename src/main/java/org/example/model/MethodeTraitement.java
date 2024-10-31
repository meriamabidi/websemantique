package org.example.model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class MethodeTraitement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String description;
    private int temps; // en minutes
    private double couts; // en euros

    public MethodeTraitement() {
    }

    public MethodeTraitement(String nom, String description, int temps, double couts) {
        this.nom = nom;
        this.description = description;
        this.temps = temps;
        this.couts = couts;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTemps() {
        return temps;
    }

    public void setTemps(int temps) {
        this.temps = temps;
    }

    public double getCouts() {
        return couts;
    }

    public void setCouts(double couts) {
        this.couts = couts;
    }
}
