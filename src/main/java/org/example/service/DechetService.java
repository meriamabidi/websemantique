package org.example.service;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.example.model.Company;
import org.example.model.Dechet;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
public class DechetService {

    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    private static final Logger logger = LoggerFactory.getLogger(DechetService.class);

    public DechetService() {
        loadRdfModel();
    }

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<Dechet> findAll() {
        List<Dechet> dechets = new ArrayList<>();
        Map<String, Dechet> dechetMap = new HashMap<>();

        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?dechet ?type ?poids ?dateCollecte ?description WHERE {
            ?dechet a ns:Dechet .
            OPTIONAL { ?dechet ns:type ?type . }
            OPTIONAL { ?dechet ns:poids ?poids . }
            OPTIONAL { ?dechet ns:dateCollecte ?dateCollecte . }
            OPTIONAL { ?dechet ns:description ?description . }
        }
        """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String dechetUri = soln.get("dechet").toString();
                String idValue = dechetUri.contains("#") ? dechetUri.substring(dechetUri.lastIndexOf('#') + 1)
                        : dechetUri.substring(dechetUri.lastIndexOf('/') + 1);

                Dechet dechet = dechetMap.getOrDefault(dechetUri, new Dechet());
                dechet.setId(idValue); // Assuming ID is numeric

                if (soln.contains("type")) {
                    dechet.setType(soln.get("type").toString());
                }
                if (soln.contains("poids")) {
                    dechet.setPoids(soln.get("poids").asLiteral().getDouble());
                }
                if (soln.contains("dateCollecte")) {
                    dechet.setDateCollecte(soln.get("dateCollecte").toString());
                }
                if (soln.contains("description")) {
                    dechet.setDescription(soln.get("description").toString());
                }

                dechetMap.put(dechetUri, dechet);
            }
        } catch (Exception e) {
            logger.error("Error retrieving dechets: ", e);
        }

        dechets.addAll(dechetMap.values());
        logger.info("Total dechets retrieved: " + dechets.size());
        return dechets;
    }
    public void update(Dechet dechet) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + dechet.getId();
        Logger logger = LoggerFactory.getLogger(DechetService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:type ?type ; " +
                        "           ns:poids ?poids ; " +
                        "           ns:dateCollecte ?dateCollecte ; " +
                        "           ns:description ?description . " +
                        "} WHERE { " +
                        "    <%s> ns:type ?type . " +
                        "    <%s> ns:poids ?poids . " +
                        "    <%s> ns:dateCollecte ?dateCollecte . " +
                        "    <%s> ns:description ?description . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:type \"%s\" ; " +
                        "           ns:poids \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "           ns:dateCollecte \"%s\"^^<http://www.w3.org/2001/XMLSchema#date> ; " +
                        "           ns:description \"%s\" . " +
                        "}",
                baseUri, individualUri, dechet.getType(), dechet.getPoids(), dechet.getDateCollecte(), dechet.getDescription()
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
            logger.error("Error updating Dechet: ", e);
        }
    }
    public Optional<Dechet> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?type ?poids ?dateCollecte ?description WHERE { " +
                        "    ex:%s a ex:Dechet . " +
                        "    OPTIONAL { ex:%s ex:type ?type . } " +
                        "    OPTIONAL { ex:%s ex:poids ?poids . } " +
                        "    OPTIONAL { ex:%s ex:dateCollecte ?dateCollecte . } " +
                        "    OPTIONAL { ex:%s ex:description ?description . } " +
                        "}",
                baseUri, id, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Dechet dechet = new Dechet();
                dechet.setId(id);
                if (solution.contains("type")) {
                    dechet.setType(solution.getLiteral("type").getString());
                }
                if (solution.contains("poids")) {
                    dechet.setPoids(solution.getLiteral("poids").getDouble());
                }
                if (solution.contains("dateCollecte")) {
                    dechet.setDateCollecte(solution.getLiteral("dateCollecte").getString());
                }
                if (solution.contains("description")) {
                    dechet.setDescription(solution.getLiteral("description").getString());
                }
                return Optional.of(dechet);
            }
        } catch (Exception e) {
            logger.error("Error retrieving Dechet by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(Dechet dechet) {
        String generatedId = UUID.randomUUID().toString();
        dechet.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Dechet ; " +
                        "        ns:type \"%s\" ; " +
                        "        ns:poids \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "        ns:dateCollecte \"%s\"^^<http://www.w3.org/2001/XMLSchema#date> ; " +
                        "        ns:description \"%s\" . " +
                        "}",
                baseUri, generatedId, dechet.getType(), dechet.getPoids(), dechet.getDateCollecte(), dechet.getDescription()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving Dechet: ", e);
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




    private Dechet mapIndividualToDechet(Individual ind) {
        Dechet dechet = new Dechet();
        dechet.setId(ind.getLocalName());
        dechet.setType(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type")).toString());
        dechet.setPoids(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#poids")).asLiteral().getDouble());
        dechet.setDateCollecte(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#dateCollecte")).toString());
        dechet.setDescription(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description")).toString());
        return dechet;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }
}