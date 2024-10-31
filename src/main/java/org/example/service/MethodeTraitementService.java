package org.example.service;
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
    private static final String RDF_FILE_PATH = "C:/Users/Asus/Desktop/websemantique/rdffile.rdf";
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

    public void update(MethodeTraitement methode) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + methode.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#temps"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#couts"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), methode.getNom());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"), methode.getDescription());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#temps"), String.valueOf(methode.getTemps()));
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#couts"), String.valueOf(methode.getCouts()));

            saveRdfModel();
        }
    }

    public Optional<MethodeTraitement> findById(String id) {
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToMethode(ind) : null);
    }

    public void save(MethodeTraitement methode) {
        String generatedId = UUID.randomUUID().toString();
        methode.setId(generatedId);

        Resource methodeClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#Methode_de_traitement");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, methodeClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), methode.getNom());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"), methode.getDescription());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#temps"), String.valueOf(methode.getTemps()));
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#couts"), String.valueOf(methode.getCouts()));

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
