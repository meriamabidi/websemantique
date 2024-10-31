package org.example.model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class CentreRecyclage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int capacite;
    private String localisation;
    private String nom;

    public CentreRecyclage() {
    }

    public CentreRecyclage(int capacite, String localisation, String nom) {
        this.capacite = capacite;
        this.localisation = localisation;
        this.nom = nom;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    @Override
    public String toString() {
        return "CentreRecyclage{" +
                "id=" + id +
                ", capacite=" + capacite +
                ", localisation='" + localisation + '\'' +
                ", nom='" + nom + '\'' +
                '}';
    }
}
