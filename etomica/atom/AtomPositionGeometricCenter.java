package etomica.atom;

import java.io.Serializable;

import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IMolecule;
import etomica.api.IVectorMutable;
import etomica.api.IVector;
import etomica.space.ISpace;

/**
 * Calculates the geometric center over a set of atoms. The position of the
 * atom or child atoms are accumulated and used to compute their
 * center (unweighted by mass). Calculated center is obtained via the getPosition
 * method.
 * 
 * @author David Kofke
 */
public class AtomPositionGeometricCenter implements IAtomPositionDefinition, Serializable {

    public AtomPositionGeometricCenter(ISpace space) {
        center = space.makeVector();
    }

    public IVector position(IMolecule atom) {
        center.E(0.0);
        IAtomList children = atom.getChildList();
        int nAtoms = children.getAtomCount();
        for (int i=0; i<nAtoms; i++) {
            center.PE(((IAtomPositioned)children.getAtom(i)).getPosition());
        }
        center.TE(1.0 / nAtoms);
        return center;
    }

    private static final long serialVersionUID = 1L;
    private final IVectorMutable center;
}
 