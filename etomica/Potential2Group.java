package etomica;

/**
 * Collection of potentials that act between the atoms contained in
 * two groups of atoms.  This group iterates over all such atom-group
 * pairs assigned to it.  For each pair it iterates over the potentials it
 * contains, instructing these sub-potentials to perform their calculations
 * over the atoms relevant to them in the two groups.
 *
 * @author David Kofke
 */

public class Potential2Group extends Potential2 implements PotentialGroup {
    
    private final IteratorDirective localDirective = new IteratorDirective();
//    private final PotentialCalculation.EnergySum energy = new PotentialCalculation.EnergySum();
    private PotentialLinker first;
    private final PotentialTruncation potentialTruncation;
    
    public Potential2Group() {
        this(Simulation.instance.hamiltonian.potential);
    }
    public Potential2Group(PotentialGroup parent) {
        this(parent, null);
    }
    public Potential2Group(PotentialGroup parent, PotentialTruncation truncation) {
        super(parent);
        potentialTruncation = truncation;
        //overwrite iterator with one that doesn't force looping over leaf atoms
        iterator = new AtomPairIterator(parentSimulation().space(),
                new AtomIteratorSequential(false), new AtomIteratorSequential(false));
    }
    /**
     * Performs the specified calculation over the iterates of this potential
     * that comply with the iterator directive.
     */
    public void calculate(IteratorDirective id, PotentialCalculation pc) {
        localDirective.copy(id);//copy the iteratordirective to define the directive sent to the subpotentials
        iterator.reset(id);  //reset for iteration over pairs of atom groups
        while(iterator.hasNext()) {
            AtomPair pair = iterator.next();
            
            //apply truncation if in effect
            if(potentialTruncation != null && potentialTruncation.isZero(pair.r2())) continue;                
            
            //if the atom of the pair is the one specified for calculation, then
            //it becomes the basis for the sub-potential iterations, and is no longer
            //specified to them via the iterator directive
            if(pair.atom1 == id.atom1()) localDirective.set();
            
            //loop over sub-potentials
            for(PotentialLinker link=first; link!=null; link=link.next) {
                if(id.excludes(link.potential)) continue; //see if potential is ok with iterator directive
                link.potential.set(pair.atom1, pair.atom2).calculate(localDirective, pc);
            }//end for
        }//end while
    }//end calculate

    /**
     * Convenient reformulation of the calculate method, applicable if the potential calculation
     * performs a sum.  The method returns the potential calculation object, so that the sum
     * can be accessed in-line with the method call.
     */
    public final PotentialCalculation.Sum calculate(IteratorDirective id, PotentialCalculation.Sum pa) {
        this.calculate(id, (PotentialCalculation)pa);
        return pa;
    }
    
    /**
     * Adds the given potential to this group.  Part of the PotentialGroup interface.
     */
    public void addPotential(Potential potential) {
        first = new PotentialLinker(potential, first);
    }

    
    public double energy(AtomPair pair) {
        System.out.println("energy method not implemented in Potential2Group");
        System.exit(1);
        return 0.0;
//        return calculate(localDirective.set().set(IteratorDirective.BOTH), energy).sum();
    }

}//end Potential2Group
    