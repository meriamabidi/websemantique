package org.example.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
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
        ?centre a ns:Centre_Recyclage .
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

                // Set properties if they exist
                if (soln.contains("capacite") && soln.get("capacite").isLiteral()) {
                    centre.setCapacite(soln.get("capacite").asLiteral().getInt());
                }
                if (soln.contains("localisation") && soln.get("localisation").isLiteral()) {
                    centre.setLocalisation(soln.get("localisation").asLiteral().getString());
                }
                if (soln.contains("nom") && soln.get("nom").isLiteral()) {
                    centre.setNom(soln.get("nom").asLiteral().getString());
                }

                centreMap.put(centreUri, centre); // Store the centre in the map
            }
        } catch (Exception e) {
            logger.error("Error retrieving centres: ", e);
        }

        centres.addAll(centreMap.values()); // Collect all CentreRecyclage instances into the final list
        logger.info("Total centres retrieved: " + centres.size());
        return centres;
    }

    public void update(CentreRecyclage centre) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + centre.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#capacite"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#capacite"), String.valueOf(centre.getCapacite()));
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation"), centre.getLocalisation());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), centre.getNom());

            saveRdfModel();
        }
    }

    public Optional<CentreRecyclage> findById(String id) { // Changed to String
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToCentre(ind) : null);
    }

    public void save(CentreRecyclage centre) {
        String generatedId = UUID.randomUUID().toString();
        centre.setId(generatedId); // Now set ID as String

        Resource centreClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#Centre_Recyclage");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, centreClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#capacite"), String.valueOf(centre.getCapacite()));
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation"), centre.getLocalisation());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), centre.getNom());

        saveRdfModel();
    }

    public void deleteById(String id) { // Changed to String
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        if (individual != null) {
            ontModel.removeAll(individual, null, null);
            ontModel.removeAll(null, null, individual);
            saveRdfModel();
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
