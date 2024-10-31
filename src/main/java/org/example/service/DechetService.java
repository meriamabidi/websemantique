package org.example.service;


import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
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

    private static final String RDF_FILE_PATH = "C:/Users/Asus/Desktop/websemantique/rdffile.rdf";
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
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + dechet.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#poids"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#dateCollecte"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type"), dechet.getType());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#poids"), String.valueOf(dechet.getPoids()));
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#dateCollecte"), dechet.getDateCollecte().toString());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"), dechet.getDescription());

            saveRdfModel();
        }
    }

    public Optional<Dechet> findById(String id) {
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToDechet(ind) : null);
    }

    public void save(Dechet dechet) {
        String generatedId = UUID.randomUUID().toString();
        dechet.setId(generatedId); // Update this line based on how you want to manage IDs

        Resource dechetClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#Dechet");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, dechetClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type"), dechet.getType());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#poids"), String.valueOf(dechet.getPoids()));
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#dateCollecte"), dechet.getDateCollecte().toString());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"), dechet.getDescription());

        saveRdfModel();
    }

    public void deleteById(String id) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        if (individual != null) {
            ontModel.removeAll(individual, null, null);
            ontModel.removeAll(null, null, individual);
            saveRdfModel();
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