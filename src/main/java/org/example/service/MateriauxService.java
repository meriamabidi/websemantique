package org.example.service;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.example.model.Materiaux;
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
public class MateriauxService {
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private static final Logger logger = LoggerFactory.getLogger(MateriauxService.class);

    private OntModel ontModel;

    public MateriauxService() {
        loadRdfModel();
    }

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<Materiaux> findAll() {
        List<Materiaux> materiauxList = new ArrayList<>();
        Map<String, Materiaux> materiauxMap = new HashMap<>();

        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?materiaux ?type ?poids ?nom ?description ?categorie WHERE {
            ?materiaux a ns:Materiaux_Recycles .
            OPTIONAL { ?materiaux ns:type ?type . }
            OPTIONAL { ?materiaux ns:poids ?poids . }
            OPTIONAL { ?materiaux ns:nom ?nom . }
            OPTIONAL { ?materiaux ns:description ?description . }
            OPTIONAL { ?materiaux ns:categorie ?categorie . }
        }
        """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String materiauxUri = soln.get("materiaux").toString();
                String idValue = materiauxUri.contains("#") ? materiauxUri.substring(materiauxUri.lastIndexOf('#') + 1)
                        : materiauxUri.substring(materiauxUri.lastIndexOf('/') + 1);

                Materiaux materiaux = materiauxMap.getOrDefault(materiauxUri, new Materiaux());
                materiaux.setId(idValue);

                if (soln.contains("type")) {
                    materiaux.setType(soln.get("type").toString());
                }
                if (soln.contains("poids")) {
                    materiaux.setPoids(soln.get("poids").asLiteral().getDouble());
                }
                if (soln.contains("nom")) {
                    materiaux.setNom(soln.get("nom").toString());
                }
                if (soln.contains("description")) {
                    materiaux.setDescription(soln.get("description").toString());
                }
                if (soln.contains("categorie")) {
                    materiaux.setCategorie(soln.get("categorie").toString());
                }

                materiauxMap.put(materiauxUri, materiaux);
            }
        } catch (Exception e) {
            logger.error("Error retrieving materiaux: ", e);
        }

        materiauxList.addAll(materiauxMap.values());
        logger.info("Total materiaux retrieved: " + materiauxList.size());
        return materiauxList;
    }

    public Optional<Materiaux> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?type ?poids ?nom ?description ?categorie WHERE { " +
                        "    ex:%s a ex:Materiaux_Recycles . " +
                        "    OPTIONAL { ex:%s ex:type ?type . } " +
                        "    OPTIONAL { ex:%s ex:poids ?poids . } " +
                        "    OPTIONAL { ex:%s ex:nom ?nom . } " +
                        "    OPTIONAL { ex:%s ex:description ?description . } " +
                        "    OPTIONAL { ex:%s ex:categorie ?categorie . } " +
                        "}",
                baseUri, id, id, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Materiaux materiaux = new Materiaux();
                materiaux.setId(id);
                if (solution.contains("type")) {
                    materiaux.setType(solution.getLiteral("type").getString());
                }
                if (solution.contains("poids")) {
                    materiaux.setPoids(solution.getLiteral("poids").getDouble());
                }
                if (solution.contains("nom")) {
                    materiaux.setNom(solution.getLiteral("nom").getString());
                }
                if (solution.contains("description")) {
                    materiaux.setDescription(solution.getLiteral("description").getString());
                }
                if (solution.contains("categorie")) {
                    materiaux.setCategorie(solution.getLiteral("categorie").getString());
                }
                return Optional.of(materiaux);
            }
        } catch (Exception e) {
            logger.error("Error retrieving Materiaux by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(Materiaux materiaux) {
        String generatedId = UUID.randomUUID().toString();
        materiaux.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Materiaux_Recycles ; " +
                        "        ns:type \"%s\" ; " +
                        "        ns:poids \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "        ns:nom \"%s\" ; " +
                        "        ns:description \"%s\" ; " +
                        "        ns:categorie \"%s\" . " +
                        "}",
                baseUri, generatedId, materiaux.getType(), materiaux.getPoids(), materiaux.getNom(), materiaux.getDescription(), materiaux.getCategorie()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving Materiaux: ", e);
        }
    }
    public void update(Materiaux materiaux) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + materiaux.getId();
        Logger logger = LoggerFactory.getLogger(MateriauxService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:type ?type ; " +
                        "           ns:poids ?poids ; " +
                        "           ns:nom ?nom ; " +
                        "           ns:description ?description ; " +
                        "           ns:categorie ?categorie . " +
                        "} WHERE { " +
                        "    <%s> ns:type ?type . " +
                        "    <%s> ns:poids ?poids . " +
                        "    <%s> ns:nom ?nom . " +
                        "    <%s> ns:description ?description . " +
                        "    <%s> ns:categorie ?categorie . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:type \"%s\" ; " +
                        "           ns:poids \"%s\"^^<http://www.w3.org/2001/XMLSchema#double> ; " +
                        "           ns:nom \"%s\" ; " +
                        "           ns:description \"%s\" ; " +
                        "           ns:categorie \"%s\" . " +
                        "}",
                baseUri, individualUri, materiaux.getType(), materiaux.getPoids(), materiaux.getNom(), materiaux.getDescription(), materiaux.getCategorie()
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
            logger.error("Error updating Materiaux: ", e);
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

    private Materiaux mapIndividualToMateriaux(Individual ind) {
    Materiaux materiaux = new Materiaux();
    materiaux.setId(ind.getLocalName());
    materiaux.setType(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type")).toString());
    materiaux.setPoids(Double.parseDouble(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#poids")).toString()));
    materiaux.setNom(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom")).toString());
    materiaux.setDescription(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description")).toString());
    materiaux.setCategorie(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#categorie")).toString());
    return materiaux;
}
    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }
}
