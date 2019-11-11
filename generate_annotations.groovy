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

// setup ...
def O_FILE = './hp.obo'
def A_FILE = './phenotype_annotation.tab'

def dCount = Integer.parseInt(args[0]) // number of diseases
def pCount = Integer.parseInt(args[1]) // number of patients per disease

def rnd = new Random()

// Get IDs
def ids = new File(A_FILE).text.split('\n').collect { if(it.indexOf('OMIM') != -1) { it.split('\t')[1] } }
ids.removeAll([null])
println ids

// Generate patients
def simulator = new AnnotationSimulator(O_FILE, A_FILE, OntologyProjectType.HPO);

results = []
(1..dCount).each { 
  def idx = rnd.nextInt(ids.size())
  def id = ids[idx]

  results += simulator.simulatePatients(ItemDatabase.OMIM, id, pCount, 0.2, 0.4, 2, 10);
}

results = results.flatten()

// Output
def json = JsonOutput.toJson(results)
new File('results.json').write(json)

println 'Done'
