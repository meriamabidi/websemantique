package org.example.service;


import org.apache.jena.query.Query;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.example.model.Company;
import org.springframework.stereotype.Service;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.ontology.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.*;
import java.util.*;
@Service
public class CompanyService {
    private static final String RDF_FILE_PATH = "C:/Users/Asus/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;
    private static final Logger logger = LoggerFactory.getLogger(CompanyService.class);

    public CompanyService() {
        loadRdfModel();
    }

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }

    public List<Company> findAll() {
        List<Company> companies = new ArrayList<>();
        Map<String, Company> companyMap = new HashMap<>();

        String sparqlQueryString = """
        PREFIX ns: <http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#>
        SELECT ?company ?nom ?adresse ?contact WHERE {
            ?company a ns:Compagnie .
            OPTIONAL { ?company ns:nom ?nom . }
            OPTIONAL { ?company ns:adresse ?adresse . }
            OPTIONAL { ?company ns:contact ?contact . }
        }
        """;

        Query query = QueryFactory.create(sparqlQueryString);

        try (QueryExecution qexec = QueryExecutionFactory.create(query, ontModel)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String companyUri = soln.get("company").toString();
                String idValue = companyUri.contains("#") ? companyUri.substring(companyUri.lastIndexOf('#') + 1)
                        : companyUri.substring(companyUri.lastIndexOf('/') + 1);

                Company company = companyMap.getOrDefault(companyUri, new Company());
                company.setId(idValue); // Assuming ID is numeric

                if (soln.contains("nom")) {
                    company.setNom(soln.get("nom").toString());
                }
                if (soln.contains("adresse")) {
                    company.setAdresse(soln.get("adresse").toString());
                }
                if (soln.contains("contact")) {
                    company.setContact(soln.get("contact").toString());
                }

                companyMap.put(companyUri, company);
            }
        } catch (Exception e) {
            logger.error("Error retrieving companies: ", e);
        }

        companies.addAll(companyMap.values());
        logger.info("Total companies retrieved: " + companies.size());
        return companies;
    }

    public void update(Company company) {
        Individual individual = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + company.getId());
        if (individual != null) {
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"));
            individual.removeAll(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"));

            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), company.getNom());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"), company.getAdresse());
            individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"), company.getContact());

            saveRdfModel();
        }
    }

    public Optional<Company> findById(String id) {
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlQuery = String.format(
                "PREFIX ex: <%s> " +
                        "SELECT ?nom ?adresse ?contact WHERE { " +
                        "    ex:%s a ex:Compagnie . " +
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
                Company company = new Company();
                company.setId(id);
                if (solution.contains("nom")) {
                    company.setNom(solution.getLiteral("nom").getString());
                }
                if (solution.contains("adresse")) {
                    company.setAdresse(solution.getLiteral("adresse").getString());
                }
                if (solution.contains("contact")) {
                    company.setContact(solution.getLiteral("contact").getString());
                }
                return Optional.of(company);
            }
        } catch (Exception e) {
            logger.error("Error retrieving Company by ID: ", e);
        }
        return Optional.empty();
    }

    public void save(Company company) {
        String generatedId = UUID.randomUUID().toString();
        company.setId(generatedId); // Update this line based on how you want to manage IDs

        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String sparqlUpdate = String.format(
                "PREFIX ns: <%s> " +
                        "INSERT DATA { " +
                        "    ns:%s a ns:Compagnie ; " +
                        "        ns:nom \"%s\" ; " +
                        "        ns:adresse \"%s\" ; " +
                        "        ns:contact \"%s\" . " +
                        "}",
                baseUri, generatedId, company.getNom(), company.getAdresse(), company.getContact()
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


    private Company mapIndividualToCompany(Individual ind) {
        Company company = new Company();
        company.setId(ind.getLocalName());
        company.setNom(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom")).toString());
        company.setAdresse(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse")).toString());
        company.setContact(ind.getPropertyValue(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact")).toString());
        return company;
    }

    private void saveRdfModel() {
        try (FileOutputStream out = new FileOutputStream(RDF_FILE_PATH)) {
            ontModel.write(out, "RDF/XML");
        } catch (Exception e) {
            logger.error("Error saving RDF model: ", e);
        }
    }
}