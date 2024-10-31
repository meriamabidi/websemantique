package org.example.service;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
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
    private static final String RDF_FILE_PATH = "C:/Users/Asus/Desktop/websemantique/rdffile.rdf";
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

    public void update(CollectDechet collecte) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + collecte.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#quantite"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#etat"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#date"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#lieu"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#quantite"), String.valueOf(collecte.getQuantite()));
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#etat"), collecte.getEtat());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#date"), collecte.getDate().toString());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#lieu"), collecte.getLieu());

            saveRdfModel();
        }
    }

    public Optional<CollectDechet> findById(String id) {
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToCollecte(ind) : null);
    }

    public void save(CollectDechet collecte) {
        String generatedId = UUID.randomUUID().toString();
        collecte.setId(generatedId); // Update this line based on how you want to manage IDs

        Resource collecteClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#collecte_dechet");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, collecteClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#quantite"), String.valueOf(collecte.getQuantite()));
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#etat"), collecte.getEtat());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#date"), collecte.getDate().toString());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#lieu"), collecte.getLieu());

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
    }}