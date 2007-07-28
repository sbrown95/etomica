package etomica.virial.cluster;

import etomica.atom.AtomSet;
import etomica.potential.IPotential;
import etomica.virial.MayerFunction;


public class FTilde implements MayerFunction, java.io.Serializable {
	private final MayerFunction fWrapped;
	public FTilde(MayerFunction f) {
		fWrapped = f;
	}
	public double f(AtomSet aPair, double beta) {
		return fWrapped.f(aPair,beta) + 1.0;
	}
	public IPotential getPotential() {
	    return fWrapped.getPotential();
	}
	public String toString() {return "f~  ";}
}
