package etomica.lattice.crystal;

import etomica.Space;
import etomica.lattice.Basis;

/**
 * Single-atom basis with the coordinate at the origin.
 */
 
public class BasisMonatomic implements Basis {
    
    /**
     * Creates a single-atom basis with the coordinate at the origin.
     * @param D the spatial dimension of the crystal
     */
    public BasisMonatomic(int D) {
        coordinates = new Space.Vector[] {Space.makeVector(D)};
    }
    	
	/**
	 * Number of atoms in the basis.
	 * @return int
	 */
	public int size() {
        return 1;
    }
    
    /**
     * Returns an array with a single vector at the origin.
     */
    public Space.Vector[] positions() {
        return coordinates;
    }

    private Space.Vector[] coordinates;
}