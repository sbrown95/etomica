package etomica.potential;

import etomica.atom.AtomSet;
import etomica.phase.Phase;
import etomica.space.Space;
import etomica.units.Dimension;

public interface IPotential {

    public Space getSpace();

    /**
     * Returns the range over which the potential applies.  IAtoms with a
     * greater separation do not interact.
     */
    public double getRange();

    /**
     * The Dimension for range (Length)
     */
    public Dimension getRangeDimension();

    /**
     * Informs the potential of the phase on which it acts. Typically this
     * requires at least that it update the nearestImageTransformer of its
     * coordinatePair (if it uses one), e.g.:
     * cPair.setNearestImageTransformer(phase.boundary());
     */
    public void setPhase(Phase phase);

    /**
     * The number of atoms on which the potential depends.
     */
    public int nBody();

    /**
     * Returns the interaction energy between the given atoms.  There might be
     * 0, 1, 2 or more atoms in the AtomSet.
     */
    public abstract double energy(AtomSet atoms);

}