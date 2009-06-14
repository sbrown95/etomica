package etomica.virial.cluster2.graph.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import etomica.virial.cluster2.graph.Edges;
import etomica.virial.cluster2.graph.EdgesSetVisitor;
import etomica.virial.cluster2.graph.GraphSet;
import etomica.virial.cluster2.graph.Nodes;

public class SimpleGraphSet implements GraphSet {

  private Nodes nodes;
  private List<Edges> edgesList;
  private Set<String> tags;

  public SimpleGraphSet(Nodes nodes, List<Edges> edgesList) {

    this.nodes = nodes;
    this.edgesList = edgesList;
  }

  public void addComplements() {

    List<Edges> complements = new ArrayList<Edges>(edgesList.size());
    for (int i = 0; i < edgesList.size(); i++) {
      complements.add(edgesList.get(i).complement());
    }
    edgesList.addAll(complements);
  }

  public Set<Edges> getEdgesSet() {

    return new HashSet<Edges>(edgesList);
  }

  public int getSize() {

    return edgesList.size();
  }

  public Nodes getNodes() {

    return nodes;
  }

  @Override
  public String toString() {

    String result = "";
    for (int i = 0; i < getSize(); i++) {
      result += i + ": " + edgesList.get(i).toString() + "\n";
    }
    return result;
  }

  public void visitEdgesSet(EdgesSetVisitor visitor) {

    for (int i = 0; i < getSize(); i++) {
      if (!visitor.visit(edgesList.get(i))) {
        break;
      }
    }
  }

  public void setTags(Set<String> tags) {

    this.tags = tags;
  }

  public Set<String> getTags() {

    return tags;
  }
}