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
    private static final String RDF_FILE_PATH = "C:/Users/Asus/Desktop/websemantique/rdffile.rdf";
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


    // Save new Fournisseur
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
            logger.error("Error saving CollectDechet: ", e);
        }
    }

    // Update existing Fournisseur
    public void update(Fournisseur fournisseur) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + fournisseur.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), fournisseur.getNom());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"), fournisseur.getAdresse());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"), fournisseur.getContact());

            saveRdfModel();
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
        String sparqlUpdate = String.format(
                "PREFIX ex: <%s> " +
                        "DELETE WHERE { " +
                        "    ex:%s ?p ?o . " +
                        "    ?s ?p2 ex:%s . " +
                        "}",
                baseUri, id, id
        );

        UpdateRequest updateRequest = UpdateFactory.create(sparqlUpdate);
        try {
            UpdateExecution qexec = UpdateExecutionFactory.create(updateRequest, (Dataset) ontModel);
            qexec.execute();
        } catch (Exception e) {
            logger.error("Error deleting CollectDechet by ID: ", e);
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
