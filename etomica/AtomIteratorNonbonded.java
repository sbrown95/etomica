package etomica;

/**
 * Loops over all the atoms in a basis which are not bonded to 
 * an atom specified in the iterator directive.
 */

public class AtomIteratorNonbonded implements AtomIterator {
    
    private final AtomIterator iterator;
    
    private boolean hasNext;
    protected IteratorDirective.Direction direction;
    private Atom nonbondAtom;
    private Atom nextAtom;
    
    public AtomIteratorNonbonded(Simulation sim) {
        iterator = sim.iteratorFactory.makeIntragroupIterator();
    }
    
    public boolean hasNext() {return hasNext;}

    public boolean contains(Atom atom) {
        return(iterator.contains(atom) && !Bond.areBonded(nonbondAtom, atom));
    }
    
    public Atom reset(IteratorDirective id) {
        nonbondAtom = id.atom1();
        if(nonbondAtom != null) setBasis(nonbondAtom.node.parentGroup());
        iterator.reset(id);
        next();
        return nextAtom;
    }
    
    public Atom reset() {
        iterator.reset();
        next();
        return nextAtom;
    }
    
    public Atom next() {
        Atom next = nextAtom;
        hasNext = false; nextAtom = null;
        while(iterator.hasNext()) {//loop until another nonbonded atom is found, or iterator expires
            nextAtom = iterator.next();
            if(!Bond.areBonded(nextAtom, nonbondAtom)) {
                hasNext = true;
                break;
            }
        }
        return next;
    }
    
    //not implemented
    public void allAtoms(AtomAction act) {}
    
    public void setBasis(Atom atom) {
        iterator.setBasis(atom);
    }
    
    public Atom getBasis() {return iterator.getBasis();}
    
    /**
     * Do not use: not correctly implemented.
     */
    public int size() {
        throw new RuntimeException("AtomIteratorNonbonded.size() not implemented");
    }   
}//end of AtomIteratorNonbonded