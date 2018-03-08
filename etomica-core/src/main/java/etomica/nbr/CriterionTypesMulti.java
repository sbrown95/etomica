/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package etomica.nbr;

import etomica.atom.AtomType;
import etomica.atom.IAtom;

/**
 * Filters AtomSets to match a given set of AtomTypes.  CriterionType and
 * CriterionTypePair should be used for single and pairs of Atoms.
 * 
 * @author Andrew Schultz
 */
public class CriterionTypesMulti extends CriterionAdapter {

    private static final long serialVersionUID = 1L;
    private final AtomType[] types;

    public CriterionTypesMulti(NeighborCriterion criterion,
                               AtomType[] types) {
        super(criterion);
        this.types = types.clone();
    }
    
    /**
     * Returns true if the AtomTypes for the pair of atoms match the AtomTypes
     * given at construction (without regard to the order of the AtomSet), and
     * if the wrapped criterion also accepts the AtomSet.
     * @param atom1
     * @param atom2
     */
    public boolean accept(IAtom atom1, IAtom atom2) {
        for (AtomType type : types) {
            boolean accepted = false;
            if (atom1.getType() == type || atom2.getType() == type) {
                accepted = true;
            }
            if (!accepted) {
                return false;
            }
        }
        return subCriterion.accept(atom1, atom2);
    }

    /**
     * Returns the AtomTypes accepted by this NeighborCriterion
     */
    public AtomType[] getTypes() {
        return types;
    }
}
