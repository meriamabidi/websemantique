package org.example.service;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.example.model.MethodeTraitement;
import org.springframework.stereotype.Service;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jena.query.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
@Service
public class MethodeTraitementService {
    // Path to the RDF file
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    public MethodeTraitementService() {
        loadRdfModel();
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodeTraitementService.class);

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<MethodeTraitement> findAll() {
        List<MethodeTraitement> methodes = new ArrayList<>();
        Map<String, MethodeTraitement> methodeMap = new HashMap<>();

        String sparqlQueryString = """
    PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
    SELECT ?methode ?nom ?description ?temps ?couts WHERE {
        ?methode a ns:Methode_de_traitement .
        OPTIONAL { ?methode ns:nom ?nom . }
        OPTIONAL { ?methode ns:description ?description . }
        OPTIONAL { ?methode ns:temps ?temps . }
        OPTIONAL { ?methode ns:couts ?couts . }
    }
    """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String methodeUri = soln.get("methode").toString();
                String idValue = methodeUri.contains("#") ? methodeUri.substring(methodeUri.lastIndexOf('#') + 1)
                        : methodeUri.substring(methodeUri.lastIndexOf('/') + 1);

                MethodeTraitement methode = methodeMap.getOrDefault(methodeUri, new MethodeTraitement());
                methode.setId(idValue);

                if (soln.contains("nom")) {
                    methode.setNom(soln.get("nom").toString());
                }
                if (soln.contains("description")) {
                    methode.setDescription(soln.get("description").toString());
                }
                if (soln.contains("temps")) {
                    methode.setTemps(soln.get("temps").asLiteral().getInt());
                }
                if (soln.contains("couts")) {
                    methode.setCouts(soln.get("couts").asLiteral().getDouble());
                }

                methodeMap.put(methodeUri, methode);
            }
        } catch (Exception e) {
            logger.error("Error retrieving methodes: ", e);
        }

        methodes.addAll(methodeMap.values());
        logger.info("Total methodes retrieved: " + methodes.size());
        return methodes;
    }
    public void update(MethodeTraitement methodeTraitement) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + methodeTraitement.getId();
        Logger logger = LoggerFactory.getLogger(MethodeTraitementService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:nom ?nom ; " +
                        "           ns:description ?description ; " +
                        "           ns:temps ?temps ; " +
                        "           ns:couts ?couts . " +
                        "} WHERE { " +
                        "    <%s> ns:nom ?nom . " +
                        "    <%s> ns:description ?description . " +
                        "    <%s> ns:temps ?temps . " +
                        "    <%s> ns:couts ?couts . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:nom \"%s\" ; " +
                        "           ns:description \"%s\" ; " +
                        "           ns:temps \"%d\"^^<http://www.w3.org/2001/XMLSchema#int> ; " +
                        "           ns:couts \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> . " +
                        "}",
                baseUri, individualUri, methodeTraitement.getNom(), methodeTraitement.getDescription(), methodeTraitement.getTemps(), methodeTraitement.getCouts()
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
            logger.error("Error updating MethodeTraitement: ", e);
        }
    }
    public Optional<MethodeTraitement> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?nom ?description ?temps ?couts WHERE { " +
                        "    ex:%s a ex:Methode_de_traitement . " +
                        "    OPTIONAL { ex:%s ex:nom ?nom . } " +
                        "    OPTIONAL { ex:%s ex:description ?description . } " +
                        "    OPTIONAL { ex:%s ex:temps ?temps . } " +
                        "    OPTIONAL { ex:%s ex:couts ?couts . } " +
                        "}",
                baseUri, id, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                MethodeTraitement methode = new MethodeTraitement();
                methode.setId(id);
                if (solution.contains("nom")) {
                    methode.setNom(solution.getLiteral("nom").getString());
                }
                if (solution.contains("description")) {
                    methode.setDescription(solution.getLiteral("description").getString());
                }
                if (solution.contains("temps")) {
                    methode.setTemps(solution.getLiteral("temps").getInt());
                }
                if (solution.contains("couts")) {
                    methode.setCouts(solution.getLiteral("couts").getDouble());
                }
                return Optional.of(methode);
            }
        } catch (Exception e) {
            logger.error("Error retrieving MethodeTraitement by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(MethodeTraitement methodeTraitement) {
        String generatedId = UUID.randomUUID().toString();
        methodeTraitement.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Methode_de_traitement ; " +
                        "        ns:nom \"%s\" ; " +
                        "        ns:description \"%s\" ; " +
                        "        ns:temps \"%d\"^^<http://www.w3.org/2001/XMLSchema#int> ; " +
                        "        ns:couts \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> . " +
                        "}",
                baseUri, generatedId, methodeTraitement.getNom(), methodeTraitement.getDescription(), methodeTraitement.getTemps(), methodeTraitement.getCouts()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving MethodeTraitement: ", e);
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


    private MethodeTraitement mapIndividualToMethode(Individual ind) {
        MethodeTraitement methode = new MethodeTraitement();
        methode.setId(ind.getLocalName());
        methode.setNom(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom")).toString());
        methode.setDescription(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description")).toString());
        methode.setTemps(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#temps")).asLiteral().getInt());
        methode.setCouts(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#couts")).asLiteral().getDouble());
        return methode;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }

}
