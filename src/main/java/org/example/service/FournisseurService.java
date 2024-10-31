package org.example.service;

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


    // Save new Fournisseur
    public void save(Fournisseur fournisseur) {
        String generatedId = UUID.randomUUID().toString();
        fournisseur.setId(generatedId);

        Resource fournisseurClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#Fournisseur_de_Dechet");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, fournisseurClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), fournisseur.getNom());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"), fournisseur.getAdresse());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"), fournisseur.getContact());

        saveRdfModel();
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
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToFournisseur(ind) : null);
    }

    // Delete Fournisseur by ID
    public void deleteById(String id) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        if (individual != null) {
            ontModel.removeAll(individual, null, null);
            ontModel.removeAll(null, null, individual);
            saveRdfModel();
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
