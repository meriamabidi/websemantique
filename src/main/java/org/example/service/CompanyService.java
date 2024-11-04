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
    private static final String RDF_FILE_PATH = "C:/Users/user/Desktop/websemantique/rdffile.rdf";
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
        String baseUri = "http://www.semanticweb.org/basou/ontologies/2024/9/untitled-ontology-5#";
        String individualUri = baseUri + company.getId();
        Logger logger = LoggerFactory.getLogger(CompanyService.class);

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
                baseUri, individualUri, company.getNom(), company.getAdresse(), company.getContact()
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
            logger.error("Error updating Company: ", e);
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