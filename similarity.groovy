@Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.3')
@Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.5')
@Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.5')
@Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.5')
@Grab(group='com.github.sharispe', module='slib-sml', version='0.9.1')

import org.openrdf.model.URI;
import slib.graph.algo.accessor.GraphAccessor;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.validator.dag.ValidatorDAG;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.conf.GraphConf;
import slib.graph.io.loader.GraphLoaderGeneric;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Exception;
import slib.utils.impl.Timer;

import groovyx.gpars.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

def aMap = [:]
def cList = []
new File('patients.tsv').splitEachLine('\t') {
  if(!aMap.containsKey(it[2])) { aMap[it[2]] = [] }
  aMap[it[2]] << 'http://purl.obolibrary.org/obo/' + it[1].replace(':', '_')
}
new File('phenotype_annotation.tab').splitEachLine('\t') {
  if(it[0] != 'OMIM') { return; }
  if(!aMap.containsKey(it[5])) { aMap[it[5]] = [] }

  def cls = 'http://purl.obolibrary.org/obo/' + it[4].replace(':', '_')
  aMap[it[5]] << cls
  if(!cList.contains(cls)) {
    cList << cls
  }
}

println aMap

def ontoFile = 'hp_merged.owl'

def factory = URIFactoryMemory.getSingleton()
def graphURI = factory.getURI('http://graph/')
def g = new GraphMemory(graphURI)

def dataConf = new GDataConf(GFormat.RDF_XML, ontoFile)

def actionRerootConf = new GAction(GActionType.REROOTING)

def gConf = new GraphConf()
gConf.addGDataConf(dataConf)
gConf.addGAction(actionRerootConf)

GraphLoaderGeneric.load(gConf, g)

println g.toString()

def roots = new ValidatorDAG().getTaxonomicRoots(g)
println roots

def icConf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SANCHEZ_2011)
def smConf = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995, icConf)

def engine = new SM_Engine(g)

def i = 0
ConcurrentHashMap cSim = new ConcurrentHashMap()
GParsPool.withPool(4) { p ->
  cList.eachParallel { u1 ->
    i++
    println "${i}/${cList.size()}"

    cSim[u1] = [:]
    cList.each { u2 ->
      cSim[u1][u2] = engine.compare(smConf, factory.getURI(u1), factory.getURI(u2))
    }
  }
}

def out = [] 
GParsPool.withPool(4) { p ->
  aMap.eachParallel { g1, u1 ->
    def aList = []
    aMap.each { g2, u2 ->
      aList << [
        g2,
        ((u1.inject(0) { sum, uri ->
          sum += u2.collect { uri2 -> cSim[uri][uri2] }.max()
        } + u2.inject(0) { sum, uri ->
          sum += u1.collect { uri2 -> cSim[uri][uri2] }.max()
        }) / (u1.size() + u2.size()))
      ]
    }
    aList = aList.toSorted { it[1] }.reverse()
    aList.each { 
      out << g1 + ',' + it[0] + ',' + it[1]
    }
  }
}

new File('sim_matrix.lst').text = out.join('\n')

//sum += u2.inject(0) { max, uri2 -> (cSim[uri][uri2] > max) ? cSim[uri][uri2] : max }
//sum += u1.inject(0) { max, uri2 -> (cSim[uri][uri2] > max) ? cSim[uri][uri2] : max }
