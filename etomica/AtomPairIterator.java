package etomica;

/**
 * Basic interface for iterating over pairs of atoms.
 *
 * @author David Kofke
 */
public interface AtomPairIterator {
    
    public void setBasis(Atom a1, Atom a2);
    
    /**
     * Returns the number of pairs capable of being given by this iterator
     * (that is, if no restrictions are specified in an iteratorDirective).
     */
    public int size();        
    
    public boolean hasNext();
    
    public void reset(IteratorDirective id);
    
    /**
     * Resets the iterator, so that it is ready to go through all of its pairs.
     */
    public void reset();
        
    /**
     * Resets the iterator so that it iterates over all pairs formed with the 
     * given atom in the most recently specified iterator directive (default UP is
     * if none previously specified.
     */
    public void reset(Atom atom);
    
        
    public AtomPair next();

    /**
     * Performs the given action on all pairs returned by this iterator.
     */
    public void allPairs(AtomPairAction act);
    
    public static final AtomPairIterator NULL = new Null();
    static final class Null implements AtomPairIterator {
        public void setBasis(Atom a1, Atom a2) {}
        public int size() {return 0;}                
        public boolean hasNext() {return false;}       
        public void reset(IteratorDirective id) {}
        public void reset() {}
        public void reset(Atom atom) {}
        public AtomPair next() {return null;}
        public void allPairs(AtomPairAction act) {}
    }//end of Null
    
}  //end of class AtomPairIterator
    
