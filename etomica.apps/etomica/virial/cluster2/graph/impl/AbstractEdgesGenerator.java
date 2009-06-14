package etomica.virial.cluster2.graph.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;

import etomica.virial.cluster2.graph.Edges;
import etomica.virial.cluster2.graph.EdgesFilter;
import etomica.virial.cluster2.graph.EdgesGenerator;

public abstract class AbstractEdgesGenerator implements EdgesGenerator {

  private EdgesFilter edgesFilter = null;
  private Set<String> tags = new HashSet<String>();
  private boolean started = false;
  private Stack<Edges> stack = new Stack<Edges>();

  protected AbstractEdgesGenerator(boolean computeTags, EdgesFilter filter) {

    edgesFilter = filter;
    if (computeTags) {
      computeTags();
    }
  }

  protected AbstractEdgesGenerator(boolean computeTags) {

    this(computeTags, null);
  }

  public AbstractEdgesGenerator(EdgesFilter filter) {

    this(true, filter);
  }

  public AbstractEdgesGenerator() {

    this(true, null);
  }

  public final Set<String> getTags() {

    return Collections.unmodifiableSet(tags);
  }

  public final Edges next(List<Edges> edgesList) throws NoSuchElementException {

    smartPush(edgesList);
    if (stack.empty()) {
      return null;
    }
    return stack.pop();
  }

  protected final void smartPush(List<Edges> edgesList) {

    // do we need to push anything at this time?
    if (!stack.empty()) {
      return;
    }
    // the last pop emptied the stack so push one or more set of edges
    Edges e = push();
    setStarted(true);
    // nothing to push
    if (e == null) {
      return;
    }
    // make sure the set of edges satisfies the chain of filters
    if (getEdgesFilter() != null) {
//      if (e.toString().equals("<(1,3)>")) {
//        System.out.println("here you are!");
//      }
//      if (e.toString().equals("<(2,3)>")) {
//        System.out.println("no, here you are!");
//      }
      while (e != null
          && (!getEdgesFilter().preAccept(e, edgesList) || !getEdgesFilter().accept(e, edgesList))) {
        e = push();
      }
      // nothing to push
      if (e == null) {
        return;
      }
    }
    stack.push(e);
  }

  protected final EdgesFilter getEdgesFilter() {

    return edgesFilter;
  }

  protected abstract String getTag();

  protected Set<String> getInternalTags() {

    return tags;
  }

  protected final boolean isStarted() {

    return started;
  }

  protected abstract Edges push();

  protected final void setStarted(boolean value) {

    started = value;
  }

  protected void computeTags() {

    getInternalTags().add(getTag());
    if (getEdgesFilter() != null) {
      getInternalTags().addAll(getEdgesFilter().getTags());
    }
  }
}