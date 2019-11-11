@Grapes([
  @GrabResolver(name='compbio', root='http://compbio.charite.de/tl_files/maven'),
  @Grab(group='annotation-simulation', module='annotation-simulation', version='0.0.1-SNAPSHOT'),
  @Grab(group='phenologizer', module='phenologizer', version='0.0.4-SNAPSHOT'),
  @GrabConfig(systemClassLoader=true)
])

import drseb.*
import ontologizer.ontology.*
import hpo.ItemId.ItemDatabase;
import groovy.json.*

// Parameters 
def ontologyFile = './hp.obo'
def annotationFile = './phenotype_annotation.tab'
def id = "114030";

// Generate patients
def simulator = new AnnotationSimulator(ontologyFile, annotationFile, OntologyProjectType.HPO);
def patients = simulator.simulatePatients(ItemDatabase.OMIM, id, 1000, 0.2, 0.4, 2, 10);

// Output
def json = JsonOutput.toJson(patients)
new File('patients.json').write(json)

println 'done'
