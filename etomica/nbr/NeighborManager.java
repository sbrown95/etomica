/*
 * History
 * Created on Sep 22, 2004 by kofke
 */
package etomica.nbr;

import etomica.Atom;
import etomica.AtomIteratorTree;
import etomica.AtomsetActive;
import etomica.Integrator;
import etomica.IteratorDirective;
import etomica.Integrator.IntervalEvent;
import etomica.Integrator.IntervalListener;
import etomica.Phase;

/**
 * @author kofke
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class NeighborManager implements IntervalListener {

	/**
	 * 
	 */
	public NeighborManager() {
		super();
		setUpdateInterval(1);
		iieCount = updateInterval;
		iterator = new AtomIteratorTree();
		iterator.setDoAllNodes(true);
		neighborCheck = new NeighborCheck();
	}

	/* (non-Javadoc)
	 * @see etomica.Integrator.IntervalListener#intervalAction(etomica.Integrator.IntervalEvent)
	 */
	public void intervalAction(IntervalEvent evt) {
		if (--iieCount == 0) {
			updateNbrsIfNeeded(((Integrator)evt.getSource()).getPhases());
		}
	}

	public void updateNbrsIfNeeded(Phase[] phase) {
		for (int i=0; i<phase.length; i++) {
			neighborCheck.reset();
			for (int j=0; j<criteria.length; i++) {
				criteria[j].setPhase(phase[i]);
			}
			iterator.setRoot(phase[i].speciesMaster());
			iterator.allAtoms(neighborCheck);
			if (neighborCheck.needUpdate) {
				if (neighborCheck.unsafe) {
					System.err.println("Atoms exceeded the safe neighbor limit");
				}
				phase[i].simulation().potentialMaster.calculate(phase[i],id,potentialCalculationNbrSetup);
			}
		}
	}
	
	public int getUpdateInterval() {
		return updateInterval;
	}
	
	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}
	
	public void addCriterion(NeighborCriterion criterion) {
		NeighborCriterion[] newCriteria = new NeighborCriterion[criteria.length+1];
		System.arraycopy(criteria, 0, newCriteria, 0, criteria.length);
		newCriteria[criteria.length] = criterion;
		criteria = newCriteria;
	}

    public boolean removeCriterion(NeighborCriterion criterion) {
    	for (int i=0; i<criteria.length; i++) {
    		if (criteria[i] == criterion) {
    	    	NeighborCriterion[] newCriteria = new NeighborCriterion[criteria.length-1];
    	    	System.arraycopy(criteria,0,newCriteria,0,i);
    	    	System.arraycopy(criteria,i+1,newCriteria,i,criteria.length-i-1);
    	    	criteria = newCriteria;
    	    	return true;
    		}
    	}
    	return false;
    }

	private NeighborCriterion[] criteria;
	private int updateInterval;
	private int iieCount;
	private final AtomIteratorTree iterator;
	private final NeighborCheck neighborCheck;
	private final IteratorDirective id = new IteratorDirective();
	private final PotentialCalculationNbrSetup potentialCalculationNbrSetup = new PotentialCalculationNbrSetup();

	private static class NeighborCheck implements AtomsetActive {
		private boolean needUpdate = false, unsafe = false;
		public void actionPerformed(Atom[] atom) {
			NeighborCriterion criterion = atom[0].type.getNbrManagerAgent().getCriterion();
			if (criterion != null && criterion.needUpdate(atom[0])) {
				needUpdate = true;
				if (criterion.unsafe(atom[0])) {
					unsafe = true;
				}
			}
		}
		
		public void reset() {
			needUpdate = false;
			unsafe = false;
		}
		
		
	}
}
