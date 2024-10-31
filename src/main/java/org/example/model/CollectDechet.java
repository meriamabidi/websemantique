package org.example.model;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class CollectDechet {

    @Id
    private String id;

    private double quantite; // en kg
    private String etat; // e.g., "collecté", "en attente", etc.
    private String date;
    private String lieu;

    public CollectDechet() {
    }

    public CollectDechet(double quantite, String etat, String date, String lieu) {
        this.quantite = quantite;
        this.etat = etat;
        this.date = date;
        this.lieu = lieu;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }
}
