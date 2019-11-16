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
def omims = [:]
new File(A_FILE).text.split('\n').each { 
  if(it.indexOf('OMIM') != -1) { 
    it = it.split('\t')
    if(!omims.containsKey(it[1])) { omims[it[1]] = [] }
    omims[it[1]] << it[4]
  } 
}
def ids = omims.keySet().collect()

// Generate patients
def simulator = new AnnotationSimulator(O_FILE, A_FILE, OntologyProjectType.HPO);

results = [:]
(0..dCount).each { 
  def idx = rnd.nextInt(ids.size())
  def id = ids[idx]

  results[id] = simulator.simulatePatients(ItemDatabase.OMIM, id, pCount, 0.2, 0.4, 2, 10);
}

def c = 0
println "ptid\tannotation\tdisease"
results.each { id, pts ->
  pts.each { hpos ->
    hpos.each { hp ->
      println "$c\t$hp.id\tOMIM:${id}_PATIENT_$c"
    }

    c++
  }
}


println 'Done'
