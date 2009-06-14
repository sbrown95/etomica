package etomica.virial.cluster2.graph.isomorphism;

import etomica.virial.cluster2.graph.Graph;

public class VF2SearchState extends AbstractSearchState {

  private int orig_core_len;
  private int added_node1;
  private int t1both_len, t2both_len;
  private int t1in_len, t1out_len;
  private int t2in_len, t2out_len;
  private int[] in_1, in_2;
  private int[] out_1, out_2;

  public VF2SearchState(Graph g1, Graph g2) {

    super(g1, g2);
    n1 = getN1().count();
    n2 = getN2().count();
    core_len = 0;
    orig_core_len = 0;
    t1both_len = 0;
    t1in_len = 0;
    t1out_len = 0;
    t2both_len = 0;
    t2in_len = 0;
    t2out_len = 0;
    added_node1 = NULL_NODE;
    core_1 = new int[n1];
    core_2 = new int[n2];
    in_1 = new int[n1];
    in_2 = new int[n2];
    out_1 = new int[n1];
    out_2 = new int[n2];
    for (int i = 0; i < n1; i++) {
      core_1[i] = NULL_NODE;
      in_1[i] = 0;
      out_1[i] = 0;
    }
    for (int i = 0; i < n2; i++) {
      core_2[i] = NULL_NODE;
      in_2[i] = 0;
      out_2[i] = 0;
    }
  }

  public VF2SearchState(VF2SearchState state) {

    copy(state);
  }

  public void addPair(NodePair pair) {

    assert (pair != null);
    int node1 = pair.getN1();
    int node2 = pair.getN2();
    assert (node1 < n1);
    assert (node2 < n2);
    assert (core_len < n1);
    assert (core_len < n2);
    // we are adding node1 to the core
    core_len++;
    added_node1 = node1;
    if (in_1[node1] == 0) {
      in_1[node1] = core_len;
      t1in_len++;
      if (out_1[node1] != 0) {
        t1both_len++;
      }
    }
    if (out_1[node1] == 0) {
      out_1[node1] = core_len;
      t1out_len++;
      if (in_1[node1] != 0) {
        t1both_len++;
      }
    }
    if (in_2[node2] == 0) {
      in_2[node2] = core_len;
      t2in_len++;
      if (out_2[node2] != 0) {
        t2both_len++;
      }
    }
    if (out_2[node2] == 0) {
      out_2[node2] = core_len;
      t2out_len++;
      if (in_2[node2] != 0) {
        t2both_len++;
      }
    }
    // update the matched core set
    core_1[node1] = node2;
    core_2[node2] = node1;
    int i, other;
    for (i = 0; i < getE1().getInDegree(node1); i++) {
      other = getE1().getInNode(node1, i);
      if (in_1[other] == 0) {
        in_1[other] = core_len;
        t1in_len++;
        if (out_1[other] != 0) {
          t1both_len++;
        }
      }
    }
    for (i = 0; i < getE1().getOutDegree(node1); i++) {
      other = getE1().getOutNode(node1, i);
      if (out_1[other] == 0) {
        out_1[other] = core_len;
        t1out_len++;
        if (in_1[other] != 0) {
          t1both_len++;
        }
      }
    }
    for (i = 0; i < getE2().getInDegree(node2); i++) {
      other = getE2().getInNode(node2, i);
      if (in_2[other] == 0) {
        in_2[other] = core_len;
        t2in_len++;
        if (out_2[other] != 0) {
          t2both_len++;
        }
      }
    }
    for (i = 0; i < getE2().getOutDegree(node2); i++) {
      other = getE2().getOutNode(node2, i);
      if (out_2[other] == 0) {
        out_2[other] = core_len;
        t2out_len++;
        if (in_2[other] != 0) {
          t2both_len++;
        }
      }
    }
  }

  public void backTrack() {

    assert (core_len - orig_core_len <= 1);
    assert (added_node1 != NULL_NODE);
    if (orig_core_len < core_len) {
      int i, node2;
      if (in_1[added_node1] == core_len) {
        in_1[added_node1] = 0;
      }
      for (i = 0; i < getE1().getInDegree(added_node1); i++) {
        int other = getE1().getInNode(added_node1, i);
        if (in_1[other] == core_len) {
          in_1[other] = 0;
        }
      }
      if (out_1[added_node1] == core_len) {
        out_1[added_node1] = 0;
      }
      for (i = 0; i < getE1().getOutDegree(added_node1); i++) {
        int other = getE1().getOutNode(added_node1, i);
        if (out_1[other] == core_len) {
          out_1[other] = 0;
        }
      }
      node2 = core_1[added_node1];
      if (in_2[node2] == core_len) {
        in_2[node2] = 0;
      }
      for (i = 0; i < getE2().getInDegree(node2); i++) {
        int other = getE2().getInNode(node2, i);
        if (in_2[other] == core_len) {
          in_2[other] = 0;
        }
      }
      if (out_2[node2] == core_len) {
        out_2[node2] = 0;
      }
      for (i = 0; i < getE2().getOutDegree(node2); i++) {
        int other = getE2().getOutNode(node2, i);
        if (out_2[other] == core_len) {
          out_2[other] = 0;
        }
      }
      core_1[added_node1] = NULL_NODE;
      core_2[node2] = NULL_NODE;
      core_len = orig_core_len;
      added_node1 = NULL_NODE;
    }
  }

  public SearchState copy() {

    return new VF2SearchState(this);
  }

  public void copy(SearchState fromState) {

    assert (fromState != null);
    assert (fromState instanceof VF2SearchState);
    VF2SearchState state = (VF2SearchState) fromState;
    setG1(fromState.getG1());
    setG2(fromState.getG2());
    n1 = state.n1;
    n2 = state.n2;
    core_len = state.core_len;
    orig_core_len = state.core_len;
    t1in_len = state.t1in_len;
    t1out_len = state.t1out_len;
    t1both_len = state.t1both_len;
    t2in_len = state.t2in_len;
    t2out_len = state.t2out_len;
    t2both_len = state.t2both_len;
    added_node1 = NULL_NODE;
    core_1 = state.core_1;
    core_2 = state.core_2;
    in_1 = state.in_1;
    in_2 = state.in_2;
    out_1 = state.out_1;
    out_2 = state.out_2;
  }

  public boolean isDead() {

    return (n1 != n2) || (t1both_len != t2both_len) || (t1out_len != t2out_len)
        || (t1in_len != t2in_len);
  }

  public boolean isFeasiblePair(NodePair pair) {

    assert (pair != null);
    int node1 = pair.getN1();
    int node2 = pair.getN2();
    assert (node1 < n1);
    assert (node2 < n2);
    assert (core_1[node1] == NULL_NODE);
    assert (core_2[node2] == NULL_NODE);
    if (!getN1Attrs(node1).isCompatible(getN2Attrs(node2))) {
      return false;
    }
    int i, other1, other2;
    int termout1 = 0, termout2 = 0, termin1 = 0, termin2 = 0, new1 = 0, new2 = 0;
    // Check the 'out' edges of node1
    for (i = 0; i < getE1().getOutDegree(node1); i++) {
      other1 = getE1().getOutNode(node1, i);
      if (core_1[other1] != NULL_NODE) {
        other2 = core_1[other1];
        if (!getE2().hasEdge(node2, other2)
            || !getE1Attrs(node1, other1).isCompatible(
                getE2Attrs(node2, other2))) {
          return false;
        }
      }
      else {
        if (in_1[other1] != 0) {
          termin1++;
        }
        if (out_1[other1] != 0) {
          termout1++;
        }
        if (in_1[other1] == 0 && out_1[other1] == 0) {
          new1++;
        }
      }
    }
    // Check the 'in' edges of node1
    for (i = 0; i < getE1().getInDegree(node1); i++) {
      other1 = getE1().getInNode(node1, i);
      if (core_1[other1] != NULL_NODE) {
        other2 = core_1[other1];
        if (!getE2().hasEdge(other2, node2)
            || !getE1Attrs(other1, node1).isCompatible(
                getE2Attrs(other2, node2))) {
          return false;
        }
      }
      else {
        if (in_1[other1] != 0) {
          termin1++;
        }
        if (out_1[other1] != 0) {
          termout1++;
        }
        if (in_1[other1] == 0 && out_1[other1] == 0) {
          new1++;
        }
      }
    }
    // Check the 'out' edges of node2
    for (i = 0; i < getE2().getOutDegree(node2); i++) {
      other2 = getE2().getOutNode(node2, i);
      if (core_2[other2] != NULL_NODE) {
        other1 = core_2[other2];
        if (!getE1().hasEdge(node1, other1)) {
          return false;
        }
      }
      else {
        if (in_2[other2] != 0) {
          termin2++;
        }
        if (out_2[other2] != 0) {
          termout2++;
        }
        if (in_2[other2] == 0 && out_2[other2] == 0) {
          new2++;
        }
      }
    }
    // Check the 'in' edges of node2
    for (i = 0; i < getE2().getInDegree(node2); i++) {
      other2 = getE2().getInNode(node2, i);
      if (core_2[other2] != NULL_NODE) {
        other1 = core_2[other2];
        if (!getE1().hasEdge(other1, node1)) {
          return false;
        }
      }
      else {
        if (in_2[other2] != 0) {
          termin2++;
        }
        if (out_2[other2] != 0) {
          termout2++;
        }
        if (in_2[other2] == 0 && out_2[other2] == 0) {
          new2++;
        }
      }
    }
    return termin1 == termin2 && termout1 == termout2 && new1 == new2;
  }

  public boolean isGoal() {

    return (core_len == n1) && (core_len == n2);
  }

  public NodePair nextPair(NodePair prev) {

    assert (prev != null);
    int prev_n1 = prev.getN1();
    int prev_n2 = prev.getN2();
    if (prev_n1 == NULL_NODE) {
      prev_n1 = 0;
    }
    if (prev_n2 == NULL_NODE) {
      prev_n2 = 0;
    }
    else {
      prev_n2++;
    }
    if (t1both_len > core_len && t2both_len > core_len) {
      while (prev_n1 < n1
          && (core_1[prev_n1] != NULL_NODE || out_1[prev_n1] == 0 || in_1[prev_n1] == 0)) {
        prev_n1++;
        prev_n2 = 0;
      }
    }
    else if (t1out_len > core_len && t2out_len > core_len) {
      while (prev_n1 < n1
          && (core_1[prev_n1] != NULL_NODE || out_1[prev_n1] == 0)) {
        prev_n1++;
        prev_n2 = 0;
      }
    }
    else if (t1in_len > core_len && t2in_len > core_len) {
      while (prev_n1 < n1
          && (core_1[prev_n1] != NULL_NODE || in_1[prev_n1] == 0)) {
        prev_n1++;
        prev_n2 = 0;
      }
    }
// else if (prev_n1==0 && order!=NULL)
// { int i=0;
// while (i<n1 && core_1[prev_n1=order[i]]!=NULL_NODE)
// i++;
// if (i==n1)
// prev_n1=n1;
// }
    else {
      while (prev_n1 < n1 && core_1[prev_n1] != NULL_NODE) {
        prev_n1++;
        prev_n2 = 0;
      }
    }
    if (t1both_len > core_len && t2both_len > core_len) {
      while (prev_n2 < n2
          && (core_2[prev_n2] != NULL_NODE || out_2[prev_n2] == 0 || in_2[prev_n2] == 0)) {
        prev_n2++;
      }
    }
    else if (t1out_len > core_len && t2out_len > core_len) {
      while (prev_n2 < n2
          && (core_2[prev_n2] != NULL_NODE || out_2[prev_n2] == 0)) {
        prev_n2++;
      }
    }
    else if (t1in_len > core_len && t2in_len > core_len) {
      while (prev_n2 < n2
          && (core_2[prev_n2] != NULL_NODE || in_2[prev_n2] == 0)) {
        prev_n2++;
      }
    }
    else {
      while (prev_n2 < n2 && core_2[prev_n2] != NULL_NODE) {
        prev_n2++;
      }
    }
    if (prev_n1 < n1 && prev_n2 < n2) {
      return new NodePair(prev_n1, prev_n2);
    }
    return null;
  }
}