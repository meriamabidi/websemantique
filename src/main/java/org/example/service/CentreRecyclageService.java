package org.example.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.example.model.CentreRecyclage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import java.util.*;

@Service
public class CentreRecyclageService {
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    private static final Logger logger = LoggerFactory.getLogger(CentreRecyclageService.class);

    public CentreRecyclageService() {
        loadRdfModel();
    }

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }
    public List<CentreRecyclage> findAll() {
        List<CentreRecyclage> centres = new ArrayList<>();
        Map<String, CentreRecyclage> centreMap = new HashMap<>();

        // SPARQL query to retrieve all Centre_Recyclage individuals with their properties
        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?centre ?capacite ?localisation ?nom WHERE {
            ?centre a ns:Centre_de_Recyclage .
            OPTIONAL { ?centre ns:capacite ?capacite . }
            OPTIONAL { ?centre ns:localisation ?localisation . }
            OPTIONAL { ?centre ns:nom ?nom . }
        }
    """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            // Process results and map them to CentreRecyclage objects
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String centreUri = soln.get("centre").toString();
                String idValue = centreUri.contains("#") ?
                        centreUri.substring(centreUri.lastIndexOf('#') + 1) :
                        centreUri.substring(centreUri.lastIndexOf('/') + 1);

                // Retrieve or create the CentreRecyclage object
                CentreRecyclage centre = centreMap.getOrDefault(centreUri, new CentreRecyclage());
                centre.setId(idValue); // Set the extracted ID

                // Log the current centre being processed
                logger.info("Processing centre URI: " + centreUri);

                // Set properties if they exist
                if (soln.contains("capacite") && soln.get("capacite").isLiteral()) {
                    centre.setCapacite(soln.get("capacite").asLiteral().getInt());
                    logger.info("Capacité found: " + centre.getCapacite());
                } else {
                    logger.warn("Capacité not found for centre: " + centreUri);
                }

                if (soln.contains("localisation") && soln.get("localisation").isLiteral()) {
                    centre.setLocalisation(soln.get("localisation").asLiteral().getString());
                    logger.info("Localisation found: " + centre.getLocalisation());
                } else {
                    logger.warn("Localisation not found for centre: " + centreUri);
                }

                if (soln.contains("nom") && soln.get("nom").isLiteral()) {
                    centre.setNom(soln.get("nom").asLiteral().getString());
                    logger.info("Nom found: " + centre.getNom());
                } else {
                    logger.warn("Nom not found for centre: " + centreUri);
                }

                centreMap.put(centreUri, centre); // Store the centre in the map
            }
        } catch (Exception e) {
            logger.error("Error retrieving centres: ", e);
        }

        centres.addAll(centreMap.values()); // Collect all CentreRecyclage instances into the final list
        logger.info("Total centres retrieved: " + centres.size());

        // Log the details of the retrieved centres
        for (CentreRecyclage centre : centres) {
            logger.info("Centre retrieved: ID=" + centre.getId() + ", Nom=" + centre.getNom() +
                    ", Localisation=" + centre.getLocalisation() + ", Capacité=" + centre.getCapacite());
        }

        return centres;
    }


    public void update(CentreRecyclage centreRecyclage) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + centreRecyclage.getId();
        Logger logger = LoggerFactory.getLogger(CentreRecyclageService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:capacite ?capacite ; " +
                        "           ns:localisation ?localisation ; " +
                        "           ns:nom ?nom . " +
                        "} WHERE { " +
                        "    <%s> ns:capacite ?capacite . " +
                        "    <%s> ns:localisation ?localisation . " +
                        "    <%s> ns:nom ?nom . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:capacite \"%d\"^^<http://www.w3.org/2001/XMLSchema#int> ; " +
                        "           ns:localisation \"%s\" ; " +
                        "           ns:nom \"%s\" . " +
                        "}",
                baseUri, individualUri, centreRecyclage.getCapacite(), centreRecyclage.getLocalisation(), centreRecyclage.getNom()
        );

        // Wrap the ontModel in a Dataset
        Dataset dataset = DatasetFactory.create(ontModel);

        try {
            UpdateRequest deleteRequest = UpdateFactory.create(sparqlDelete);
            UpdateExecutionFactory.create(deleteRequest, dataset).execute();

            UpdateRequest insertRequest = UpdateFactory.create(sparqlInsert);
            UpdateExecutionFactory.create(insertRequest, dataset).execute();

            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error updating CentreRecyclage: ", e);
        }
    }
    public Optional<CentreRecyclage> findById(String id) {
        String sparqlQueryString = String.format("""
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?capacite ?localisation ?nom WHERE {
            ns:%s a ns:Centre_de_Recyclage .
            OPTIONAL { ns:%s ns:capacite ?capacite . }
            OPTIONAL { ns:%s ns:localisation ?localisation . }
            OPTIONAL { ns:%s ns:nom ?nom . }
        }
        """, id, id, id, id);

        Query query = QueryFactory.create(sparqlQueryString);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                CentreRecyclage centre = new CentreRecyclage();
                centre.setId(id);
                if (soln.contains("capacite")) {
                    centre.setCapacite(soln.get("capacite").asLiteral().getInt());
                }
                if (soln.contains("localisation")) {
                    centre.setLocalisation(soln.get("localisation").toString());
                }
                if (soln.contains("nom")) {
                    centre.setNom(soln.get("nom").toString());
                }
                return Optional.of(centre);
            }
        } catch (Exception e) {
            logger.error("Error retrieving centre by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(CentreRecyclage centreRecyclage) {
        String generatedId = UUID.randomUUID().toString();
        centreRecyclage.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Centre_de_Recyclage ; " +
                        "        ns:capacite \"%d\"^^<http://www.w3.org/2001/XMLSchema#int> ; " +
                        "        ns:localisation \"%s\" ; " +
                        "        ns:nom \"%s\" . " +
                        "}",
                baseUri, generatedId, centreRecyclage.getCapacite(), centreRecyclage.getLocalisation(), centreRecyclage.getNom()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving CentreRecyclage: ", e);
        }
    }
    public void deleteById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + id;

        // Requête SPARQL pour supprimer toutes les propriétés liées à l'individu
        String sparqlUpdate = String.format(
                "PREFIX ex: <%s> " +
                        "DELETE WHERE { " +
                        "    <%s> ?p ?o . " +
                        "}",
                baseUri, individualUri
        );

        try {
            // Création de la requête de mise à jour SPARQL
            UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);

            // Utilisation du modèle pour créer un Dataset
            Dataset dataset = DatasetFactory.create(ontModel);

            // Logs de débogage
            logger.info("Tentative de suppression de la Company avec l'ID : " + id);

            // Exécution de la requête de suppression
            UpdateExecutionFactory.create(updateRequest, dataset).execute();

            // Log de confirmation de suppression
            logger.info("Suppression réussie de la Company avec l'ID : " + id);

        } catch (Exception e) {
            // Log d'erreur et propagation de l'exception si nécessaire
            logger.error("Erreur lors de la suppression de la Company avec l'ID : " + id, e);
            throw new RuntimeException("L'opération de suppression a échoué", e);
        }
    }


    private CentreRecyclage mapIndividualToCentre(Individual ind) {
        CentreRecyclage centre = new CentreRecyclage();
        centre.setId(ind.getLocalName()); // Changed to String
        centre.setCapacite(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#capacite")).asLiteral().getInt());
        centre.setLocalisation(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation")).toString());
        centre.setNom(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom")).toString());
        return centre;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }
}