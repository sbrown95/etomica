/*
 * History
 * Created on Sep 20, 2004 by kofke
 */
package etomica.nbr;

import etomica.Atom;
import etomica.AtomArrayList;
import etomica.AtomSequencer;
import etomica.AtomTreeNodeGroup;
import etomica.Potential;
import etomica.utility.Arrays;

/**
 * Sequencer used to maintain neighbor lists.  Holds lists of atoms
 * that were elsewhere deemed to be neighbors of the sequencer's atom.
 */
public class AtomSequencerNbr extends AtomSequencer {

	protected AtomArrayList[] upList, downList;
	
	/**
	 * @param a
	 */
	public AtomSequencerNbr(Atom a) {
		super(a);
		upList = new AtomArrayList[0];
		downList = new AtomArrayList[0];
	}
	
	public void addUpNbr(Atom a, Potential potential) {
		int index = 0;
		try {
			index = atom.type.getNbrManagerAgent().getPotentialIndex(potential);
			upList[index].add(a);
		} catch(ArrayIndexOutOfBoundsException e) {
			index = addPotential(potential);
			upList[index].add(a);
		}
	}
	public void addDownNbr(Atom a, Potential potential) {
		int index = 0;
		try {
			index = atom.type.getNbrManagerAgent().getPotentialIndex(potential);
			downList[index].add(a);
		} catch(ArrayIndexOutOfBoundsException e) {
			index = addPotential(potential);
			downList[index].add(a);
		}
	}
	
	public AtomArrayList[] getUpList() {
		return upList;
	}
	
	public AtomArrayList[] getDownList() {
		return downList;
	}
	
	public int addPotential(Potential p) {
		int index = atom.type.getNbrManagerAgent().addPotential(p);
		if (index > upList.length-1) {
            upList = (AtomArrayList[])Arrays.addObject(upList, new AtomArrayList());
            downList = (AtomArrayList[])Arrays.addObject(downList, new AtomArrayList());
		}
		return index;
	}
	
	public void removePotential(Potential p) {
		atom.type.getNbrManagerAgent().removePotential(p);
		if (upList.length == 0) throw new RuntimeException("potential list empty in removePotential");
		upList = new AtomArrayList[upList.length-1];
		downList = new AtomArrayList[downList.length-1];
		for (int i=0; i<upList.length; i++) {
			upList[i] = new AtomArrayList();
			downList[i] = new AtomArrayList();
		}
	}
	
	public void clearNbrs() {
		int length = upList.length;
		for (int i=0; i<length; i++) {
			upList[i].clear();
			downList[i].clear();
		}
	}

    public static final AtomSequencer.Factory FACTORY = new AtomSequencer.Factory() {
        public AtomSequencer makeSequencer(Atom atom) {
            return new AtomSequencerNbr(atom);
        }
        public Class sequencerClass() {return AtomSequencerNbr.class;}
    };
	public boolean preceeds(Atom a) {
		// TODO Auto-generated method stub
		return false;
	}
	public void setParentNotify(AtomTreeNodeGroup newParent) {
		// TODO Auto-generated method stub

	}
}
