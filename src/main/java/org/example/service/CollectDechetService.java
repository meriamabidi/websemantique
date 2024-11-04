package org.example.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.example.model.CollectDechet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CollectDechetService {
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    public CollectDechetService() {
        loadRdfModel();
    }

    private static final Logger logger = LoggerFactory.getLogger(CollectDechetService.class);

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<CollectDechet> findAll() {
        List<CollectDechet> collectes = new ArrayList<>();
        Map<String, CollectDechet> collecteMap = new HashMap<>();

        String sparqlQueryString = """
    PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
    SELECT ?collecte ?quantite ?etat ?date ?lieu WHERE {
        ?collecte a ns:collecte_dechet .
        OPTIONAL { ?collecte ns:quantite ?quantite . }
        OPTIONAL { ?collecte ns:etat ?etat . }
        OPTIONAL { ?collecte ns:date ?date . }
        OPTIONAL { ?collecte ns:lieu ?lieu . }
    }
    """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String collecteUri = soln.get("collecte").toString();
                String idValue = collecteUri.contains("#") ? collecteUri.substring(collecteUri.lastIndexOf('#') + 1)
                        : collecteUri.substring(collecteUri.lastIndexOf('/') + 1);

                CollectDechet collecte = collecteMap.getOrDefault(collecteUri, new CollectDechet());
                collecte.setId(idValue); // Assuming ID is numeric

                if (soln.contains("quantite")) {
                    collecte.setQuantite(soln.get("quantite").asLiteral().getDouble());
                }
                if (soln.contains("etat")) {
                    collecte.setEtat(soln.get("etat").toString());
                }
                if (soln.contains("date")) {
                    collecte.setDate(soln.get("date").toString());
                }
                if (soln.contains("lieu")) {
                    collecte.setLieu(soln.get("lieu").toString());
                }

                collecteMap.put(collecteUri, collecte);
            }
        } catch (Exception e) {
            logger.error("Error retrieving collectes: ", e);
        }

        collectes.addAll(collecteMap.values());
        logger.info("Total collectes retrieved: " + collectes.size());
        return collectes;
    }

    public void update(CollectDechet collectDechet) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + collectDechet.getId();
        Logger logger = LoggerFactory.getLogger(CollectDechetService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:quantite ?quantite ; " +
                        "           ns:etat ?etat ; " +
                        "           ns:date ?date ; " +
                        "           ns:lieu ?lieu . " +
                        "} WHERE { " +
                        "    <%s> ns:quantite ?quantite . " +
                        "    <%s> ns:etat ?etat . " +
                        "    <%s> ns:date ?date . " +
                        "    <%s> ns:lieu ?lieu . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:quantite \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "           ns:etat \"%s\" ; " +
                        "           ns:date \"%s\" ; " +
                        "           ns:lieu \"%s\" . " +
                        "}",
                baseUri, individualUri, collectDechet.getQuantite(), collectDechet.getEtat(), collectDechet.getDate(), collectDechet.getLieu()
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
            logger.error("Error updating CollectDechet: ", e);
        }
    }
    public Optional<CollectDechet> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?quantite ?etat ?date ?lieu WHERE { " +
                        "    ex:%s a ex:collecte_dechet . " +
                        "    OPTIONAL { ex:%s ex:quantite ?quantite . } " +
                        "    OPTIONAL { ex:%s ex:etat ?etat . } " +
                        "    OPTIONAL { ex:%s ex:date ?date . } " +
                        "    OPTIONAL { ex:%s ex:lieu ?lieu . } " +
                        "}",
                baseUri, id, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                CollectDechet collecte = new CollectDechet();
                collecte.setId(id);
                if (solution.contains("quantite")) {
                    collecte.setQuantite(solution.getLiteral("quantite").getDouble());
                }
                if (solution.contains("etat")) {
                    collecte.setEtat(solution.getLiteral("etat").getString());
                }
                if (solution.contains("date")) {
                    collecte.setDate(solution.getLiteral("date").getString());
                }
                if (solution.contains("lieu")) {
                    collecte.setLieu(solution.getLiteral("lieu").getString());
                }
                return Optional.of(collecte);
            }
        } catch (Exception e) {
            logger.error("Error retrieving CollectDechet by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(CollectDechet collecte) {
        String generatedId = UUID.randomUUID().toString();
        collecte.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ex: <%s> " +
                        "INSERT DATA { " +
                        "    ex:%s a ex:collecte_dechet ; " +
                        "        ex:quantite \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "        ex:etat \"%s\" ; " +
                        "        ex:date \"%s\" ; " +
                        "        ex:lieu \"%s\" . " +
                        "}",
                baseUri, generatedId, collecte.getQuantite(), collecte.getEtat(), collecte.getDate(), collecte.getLieu()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving CollectDechet: ", e);
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


    private CollectDechet mapIndividualToCollecte(Individual ind) {
        CollectDechet collecte = new CollectDechet();
        collecte.setId(ind.getLocalName());
        collecte.setEtat(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#etat")).toString());

        collecte.setQuantite(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#quantite")).asLiteral().getDouble());
        collecte.setDate(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#date")).toString());
        collecte.setLieu(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#lieu")).toString());
        return collecte;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }

    public List<CollectDechet> searchCollectDechets(String keyword) {
        List<CollectDechet> collectDechets = new ArrayList<>();
        Map<String, CollectDechet> collectDechetMap = new HashMap<>();

        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?collectDechet ?quantite ?etat ?date ?lieu WHERE {
            ?collectDechet a ns:CollectDechet .
            OPTIONAL { ?collectDechet ns:quantite ?quantite . }
            OPTIONAL { ?collectDechet ns:etat ?etat . }
            OPTIONAL { ?collectDechet ns:date ?date . }
            OPTIONAL { ?collectDechet ns:lieu ?lieu . }
            FILTER(CONTAINS(LCASE(?etat), LCASE(?keyword)) || CONTAINS(LCASE(?lieu), LCASE(?keyword)))
        }
    """;

        Query query = QueryFactory.create(sparqlQueryString.replace("?keyword", "\"" + keyword + "\""));

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String collectDechetUri = soln.get("collectDechet").toString();
                String idValue = collectDechetUri.contains("#") ? collectDechetUri.substring(collectDechetUri.lastIndexOf('#') + 1)
                        : collectDechetUri.substring(collectDechetUri.lastIndexOf('/') + 1);

                CollectDechet collectDechet = collectDechetMap.getOrDefault(collectDechetUri, new CollectDechet());
                collectDechet.setId(idValue);

                if (soln.contains("quantite")) {
                    collectDechet.setQuantite(soln.get("quantite").asLiteral().getDouble());
                }
                if (soln.contains("etat")) {
                    collectDechet.setEtat(soln.get("etat").toString());
                }
                if (soln.contains("date")) {
                    collectDechet.setDate(soln.get("date").toString());
                }
                if (soln.contains("lieu")) {
                    collectDechet.setLieu(soln.get("lieu").toString());
                }

                collectDechetMap.put(collectDechetUri, collectDechet);
            }
        } catch (Exception e) {
            logger.error("Error retrieving collectDechets: ", e);
        }

        collectDechets.addAll(collectDechetMap.values());
        logger.info("Total collectDechets found for keyword '" + keyword + "': " + collectDechets.size());
        return collectDechets;
    }




}

