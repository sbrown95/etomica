package etomica.potential;

import etomica.api.IAtomLeaf;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialAtomic;
import etomica.api.IPotentialMolecular;
import etomica.atom.AtomPair;

public class P2MoleculeMonatomic implements IPotentialMolecular {
	public P2MoleculeMonatomic(IPotentialAtomic potential) {
		wrappedPotential = potential;
		leafAtoms = new AtomPair();
	}

	public double energy(IMoleculeList atoms) {
		leafAtoms.atom0 = atoms.getMolecule(0).getChildList().getAtom(0); 
		leafAtoms.atom1 = atoms.getMolecule(1).getChildList().getAtom(0); 
		return wrappedPotential.energy(leafAtoms); 
	}

	public double getRange() {
		return wrappedPotential.getRange();
	}

	public int nBody() {
		return 2;
	}

	@Override
	public void setBox(IBox box) {
		wrappedPotential.setBox(box); 

	}

	public IPotentialAtomic getWrappedPotential() { 
		return wrappedPotential; 
		} 
	public void setWrappedPotential(IPotentialAtomic newWrappedPotential) { 
		wrappedPotential = newWrappedPotential; 
		} 
	private static final long serialVersionUID = 1L; 
	protected final AtomPair leafAtoms;
	protected IPotentialAtomic wrappedPotential; 

}
