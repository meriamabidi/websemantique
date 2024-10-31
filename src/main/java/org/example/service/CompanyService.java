package org.example.service;


import org.apache.jena.query.Query;
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
    private static final String RDF_FILE_PATH = "C:/Users/MSI/Desktop/websemantique/rdffile.rdf";
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
            ?company a ns:Company .
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
        Individual ind = ontModel.getIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + id);
        return Optional.ofNullable(ind != null ? mapIndividualToCompany(ind) : null);
    }

    public void save(Company company) {
        String generatedId = UUID.randomUUID().toString();
        company.setId(generatedId); // Update this line based on how you want to manage IDs

        Resource companyClass = ontModel.getOntClass("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#Company");
        Individual individual = ontModel.createIndividual("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#" + generatedId, companyClass);

        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#nom"), company.getNom());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#adresse"), company.getAdresse());
        individual.addProperty(ontModel.getProperty("http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#contact"), company.getContact());

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
