package org.example.service;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.example.model.Dechet;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DechetService {

    private static final String SPARQL_ENDPOINT = "http://localhost:3030/dataset/sparql"; // remplacer par l'URL de votre endpoint SPARQL

    public List<Dechet> getDechetsByType(String typeDechet) {
        List<Dechet> dechets = new ArrayList<>();

        String sparqlQuery = "PREFIX ex: <http://example.com/schema#> " +
                "SELECT ?poids ?dateCollecte ?description WHERE { " +
                "?dechet ex:type \"" + typeDechet + "\" . " +
                "?dechet ex:poids ?poids . " +
                "?dechet ex:dateCollecte ?dateCollecte . " +
                "?dechet ex:description ?description . " +
                "}";

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qExec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qExec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Dechet dechet = new Dechet();
                dechet.setType(typeDechet);
                dechet.setPoids(solution.getLiteral("poids").getDouble());
                dechet.setDateCollecte(LocalDate.parse(solution.getLiteral("dateCollecte").getString()));
                dechet.setDescription(solution.getLiteral("description").getString());

                dechets.add(dechet);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return dechets;
    }

    // Méthode pour ajouter un déchet en RDF
    public void addDechet(Dechet dechet) {
        Model model = ModelFactory.createDefaultModel();
        String uri = "http://example.com/dechet/" + dechet.getId();

        Resource dechetResource = model.createResource(uri)
                .addProperty(model.createProperty("http://example.com/schema#type"), dechet.getType())
                .addProperty(model.createProperty("http://example.com/schema#poids"), model.createTypedLiteral(dechet.getPoids()))
                .addProperty(model.createProperty("http://example.com/schema#dateCollecte"), dechet.getDateCollecte().toString())
                .addProperty(model.createProperty("http://example.com/schema#description"), dechet.getDescription());

        // Code pour envoyer le modèle à un endpoint SPARQL (ou autre mécanisme de stockage RDF)
    }
}
