package org.example.service;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
@Service

public class CentreRecyclageService {
    private static final String RDF_FILE_PATH = "C:/Users/MSI/Desktop/websemantique/rdffile.rdf";
    private OntModel ontModel;
    public CentreRecyclageService() {
        loadRdfModel();
    }

    private static final Logger logger = LoggerFactory.getLogger(CentreRecyclageService.class);

    private void loadRdfModel() {
        ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        InputStream in = FileManager.get().open(RDF_FILE_PATH);
        if (in != null) {
            ontModel.read(in, null);
        }
    }


}
