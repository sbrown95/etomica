package etomica.virial.cluster2.graph.isomorphism;

import java.util.ArrayList;

import etomica.virial.cluster2.graph.EdgeAttributes;
import etomica.virial.cluster2.graph.Edges;
import etomica.virial.cluster2.graph.Graph;
import etomica.virial.cluster2.graph.NodeAttributes;
import etomica.virial.cluster2.graph.Nodes;

public abstract class AbstractSearchState implements SearchState {

  private Graph firstGraph;
  private Graph secondGraph;
  protected int core_len;
  protected int n1, n2;
  protected int[] core_1, core_2;

  protected AbstractSearchState() {

    // default constructor for use by descendant classes
  }

  public AbstractSearchState(Graph g1, Graph g2) {

    assert (g1 != null);
    assert (g2 != null);
    firstGraph = g1;
    secondGraph = g2;
  }

  public void backTrack() {

    // backtracking does nothing by default
  }

  public int getCoreLen() {

    return core_len;
  }

  public NodePair[] getCoreSet() {

    ArrayList<NodePair> pairList = new ArrayList<NodePair>();
    for (int i = 0; i < n1; i++) {
      if (core_1[i] != NULL_NODE) {
        pairList.add(new NodePair(i, core_1[i]));
      }
    }
    return pairList.toArray(new NodePair[] {});
  }

  public Edges getE1() {

    return getG1().getEdges();
  }

  public EdgeAttributes getE1Attrs(int fromNodeID, int toNodeID) {

    return getE1().getAttributes(fromNodeID, toNodeID);
  }

  public Edges getE2() {

    return getG2().getEdges();
  }

  public EdgeAttributes getE2Attrs(int fromNodeID, int toNodeID) {

    return getE2().getAttributes(fromNodeID, toNodeID);
  }

  public Graph getG1() {

    return firstGraph;
  }

  public Graph getG2() {

    return secondGraph;
  }

  public Nodes getN1() {

    return getG1().getNodes();
  }

  public NodeAttributes getN1Attrs(int nodeID) {

    return getN1().getAttributes(nodeID);
  }

  public Nodes getN2() {

    return getG2().getNodes();
  }

  public NodeAttributes getN2Attrs(int nodeID) {

    return getN2().getAttributes(nodeID);
  }

  protected void setG1(Graph g1) {

    firstGraph = g1;
  }

  protected void setG2(Graph g2) {

    secondGraph = g2;
  }
}