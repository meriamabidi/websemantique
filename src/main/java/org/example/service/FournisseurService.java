package org.example.service;

import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.example.model.Fournisseur;
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

public class FournisseurService {
    // Define RDF path and load model
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    public FournisseurService() {
        loadRdfModel();
    }

    private static final Logger logger = LoggerFactory.getLogger(FournisseurService.class);

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    // Retrieve all Fournisseurs
    public List<Fournisseur> findAll() {
        List<Fournisseur> fournisseurs = new ArrayList<>();
        Map<String, Fournisseur> fournisseurMap = new HashMap<>();

        // Log du modèle RDF
        logger.info("Modèle RDF contenant : " + ontModel.listStatements().toList());

        // Requête SPARQL pour récupérer tous les Fournisseurs_de_Dechet
        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?fournisseur ?contact ?nom WHERE {
            ?fournisseur a ns:Fournisseur_de_Dechet .
            OPTIONAL { ?fournisseur ns:contact ?contact . }
            OPTIONAL { ?fournisseur ns:nom ?nom . }
            OPTIONAL { ?fournisseur ns:adresse ?adresse . }
        }
    """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.warn("Aucun fournisseur de déchets trouvé.");
            }

            // Traiter les résultats
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String fournisseurUri = soln.get("fournisseur").toString();
                String idValue = fournisseurUri.contains("#") ?
                        fournisseurUri.substring(fournisseurUri.lastIndexOf('#') + 1) :
                        fournisseurUri.substring(fournisseurUri.lastIndexOf('/') + 1);

                // Créer ou récupérer l'objet FournisseurDeDechet
                Fournisseur fournisseur = fournisseurMap.getOrDefault(fournisseurUri, new Fournisseur());
                fournisseur.setId(idValue); // Définir l'ID extrait

                // Définir les propriétés si elles existent
                if (soln.contains("contact") && soln.get("contact").isLiteral()) {
                    fournisseur.setContact(soln.get("contact").asLiteral().getString());
                }
                if (soln.contains("nom") && soln.get("nom").isLiteral()) {
                    fournisseur.setNom(soln.get("nom").asLiteral().getString());
                }

                fournisseurMap.put(fournisseurUri, fournisseur); // Stocker le fournisseur dans la map
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des fournisseurs : ", e);
        }

        fournisseurs.addAll(fournisseurMap.values()); // Collecter tous les FournisseurDeDechet dans la liste finale
        logger.info("Total fournisseurs récupérés : " + fournisseurs.size());

        return fournisseurs;
    }

    public void save(Fournisseur fournisseur) {
        String generatedId = UUID.randomUUID().toString();
        fournisseur.setId(generatedId);

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Fournisseur ; " +
                        "        ns:nom \"%s\" ; " +
                        "        ns:adresse \"%s\" ; " +
                        "        ns:contact \"%s\" . " +
                        "}",
                baseUri, generatedId, fournisseur.getNom(), fournisseur.getAdresse(), fournisseur.getContact()
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            Dataset dataset = DatasetFactory.create(ontModel);
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, dataset);
            qexec.execute();
            saveRdfModel();
        } catch (Exception e) {
            logger.error("Error saving Fournisseur: ", e);
        }
    }
    public void update(Fournisseur fournisseur) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + fournisseur.getId();
        Logger logger = LoggerFactory.getLogger(FournisseurService.class);

        String sparqlDelete = String.format(
                "PREFIX ns: <%s> " +
                        "DELETE { " +
                        "    <%s> ns:nom ?nom ; " +
                        "           ns:adresse ?adresse ; " +
                        "           ns:contact ?contact . " +
                        "} WHERE { " +
                        "    <%s> ns:nom ?nom . " +
                        "    <%s> ns:adresse ?adresse . " +
                        "    <%s> ns:contact ?contact . " +
                        "}",
                baseUri, individualUri, individualUri, individualUri, individualUri
        );

        String sparqlInsert = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    <%s> ns:nom \"%s\" ; " +
                        "           ns:adresse \"%s\" ; " +
                        "           ns:contact \"%s\" . " +
                        "}",
                baseUri, individualUri, fournisseur.getNom(), fournisseur.getAdresse(), fournisseur.getContact()
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
            logger.error("Error updating Fournisseur: ", e);
        }
    }
    // Find Fournisseur by ID
    public Optional<Fournisseur> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?nom ?adresse ?contact WHERE { " +
                        "    ex:%s a ex:Fournisseur_de_Dechet . " +
                        "    OPTIONAL { ex:%s ex:nom ?nom . } " +
                        "    OPTIONAL { ex:%s ex:adresse ?adresse . } " +
                        "    OPTIONAL { ex:%s ex:contact ?contact . } " +
                        "}",
                baseUri, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Fournisseur fournisseur = new Fournisseur();
                fournisseur.setId(id);
                if (solution.contains("nom")) {
                    fournisseur.setNom(solution.getLiteral("nom").getString());
                }
                if (solution.contains("adresse")) {
                    fournisseur.setAdresse(solution.getLiteral("adresse").getString());
                }
                if (solution.contains("contact")) {
                    fournisseur.setContact(solution.getLiteral("contact").getString());
                }
                return Optional.of(fournisseur);
            }
        } catch (Exception e) {
            logger.error("Error retrieving Fournisseur by ID: ", e);
        }
        return Optional.empty();
    }

    // Delete Fournisseur by ID
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


    // Helper method to map Individual to Fournisseur entity
    private Fournisseur mapIndividualToFournisseur(Individual ind) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(ind.getLocalName());
        fournisseur.setNom(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom")).toString());
        fournisseur.setAdresse(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse")).toString());
        fournisseur.setContact(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact")).toString());
        return fournisseur;
    }

    // Save RDF model to file
    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }

}
