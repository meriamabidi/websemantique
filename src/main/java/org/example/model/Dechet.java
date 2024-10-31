package org.example.model;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Dechet {

    @Id
    private String id;

    private String type;

    private double poids;

    private String dateCollecte;

    private String description;

    // Constructeurs
    public Dechet() {}

    public Dechet(String type, double poids, String dateCollecte, String description) {
        this.type = type;
        this.poids = poids;
        this.dateCollecte = dateCollecte;
        this.description = description;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPoids() {
        return poids;
    }

    public void setPoids(double poids) {
        this.poids = poids;
    }

    public String getDateCollecte() {
        return dateCollecte;
    }

    public void setDateCollecte(String dateCollecte) {
        this.dateCollecte = dateCollecte;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Dechet{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", poids=" + poids +
                ", dateCollecte=" + dateCollecte +
                ", description='" + description + '\'' +
                '}';
    }
}
