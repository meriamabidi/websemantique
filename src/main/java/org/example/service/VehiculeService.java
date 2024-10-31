package org.example.service;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.example.model.Vehicule;
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
public class VehiculeService {
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;

    public VehiculeService() {
        loadRdfModel();
    }

    private static final Logger logger = LoggerFactory.getLogger(VehiculeService.class);

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<Vehicule> findAll() {
        List<Vehicule> vehicules = new ArrayList<>();
        Map<String, Vehicule> vehiculeMap = new HashMap<>();

        String sparqlQueryString = """
    PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
    SELECT ?vehicule ?type ?localisation ?description WHERE {
        ?vehicule a ns:Vehicule .
        OPTIONAL { ?vehicule ns:type ?type . }
        OPTIONAL { ?vehicule ns:localisation ?localisation . }
        OPTIONAL { ?vehicule ns:description ?description . }
    }
    """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String vehiculeUri = soln.get("vehicule").toString();
                String idValue = vehiculeUri.contains("#") ? vehiculeUri.substring(vehiculeUri.lastIndexOf('#') + 1)
                        : vehiculeUri.substring(vehiculeUri.lastIndexOf('/') + 1);

                Vehicule vehicule = vehiculeMap.getOrDefault(vehiculeUri, new Vehicule());
                vehicule.setId(idValue);

                if (soln.contains("type")) {
                    vehicule.setType(soln.get("type").toString());
                }
                if (soln.contains("localisation")) {
                    vehicule.setLocalisation(soln.get("localisation").toString());
                }
                if (soln.contains("description")) {
                    vehicule.setDescription(soln.get("description").toString());
                }

                vehiculeMap.put(vehiculeUri, vehicule);
            }
        } catch (Exception e) {
            logger.error("Error retrieving vehicules: ", e);
        }

        vehicules.addAll(vehiculeMap.values());
        logger.info("Total vehicules retrieved: " + vehicules.size());
        return vehicules;
    }

    public void update(Vehicule vehicule) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + vehicule.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type"), vehicule.getType());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation"), vehicule.getLocalisation());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description"), vehicule.getDescription());

            saveRdfModel();
        }
    }

    public Optional<Vehicule> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?type ?localisation ?description WHERE { " +
                        "    ex:%s a ex:Vehicule . " +
                        "    OPTIONAL { ex:%s ex:type ?type . } " +
                        "    OPTIONAL { ex:%s ex:localisation ?localisation . } " +
                        "    OPTIONAL { ex:%s ex:description ?description . } " +
                        "}",
                baseUri, id, id, id, id
        );

        Query query = QueryFactory.create(sparqlQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                Vehicule vehicule = new Vehicule();
                vehicule.setId(id);
                if (solution.contains("type")) {
                    vehicule.setType(solution.getLiteral("type").getString());
                }
                if (solution.contains("localisation")) {
                    vehicule.setLocalisation(solution.getLiteral("localisation").getString());
                }
                if (solution.contains("description")) {
                    vehicule.setDescription(solution.getLiteral("description").getString());
                }
                return Optional.of(vehicule);
            }
        } catch (Exception e) {
            logger.error("Error retrieving Vehicule by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(Vehicule vehicule) {
        String generatedId = UUID.randomUUID().toString();
        vehicule.setId(generatedId);

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Vehicule ; " +
                        "        ns:type \"%s\" ; " +
                        "        ns:localisation \"%s\" ; " +
                        "        ns:description \"%s\" . " +
                        "}",
                baseUri, generatedId, vehicule.getType(), vehicule.getLocalisation(), vehicule.getDescription()
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

    private Vehicule mapIndividualToVehicule(Individual ind) {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(ind.getLocalName());
        vehicule.setType(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#type")).toString());
        vehicule.setLocalisation(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#localisation")).toString());
        vehicule.setDescription(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#description")).toString());
        return vehicule;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }

}
