package etomica.modules.droplet;

import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IDataSource;
import etomica.api.IMolecule;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomFilterCollective;
import etomica.space.ISpace;

public class AtomFilterLiquid implements AtomFilterCollective {
    
    public AtomFilterLiquid(ISpace space, IDataSource meterDeformation) {
        axis = space.makeVector();
        work = space.makeVector();
        meter = meterDeformation;
    }

    public void setCutoff(double newCutoff) {
        cutoff = newCutoff;
        cutoffSq = cutoff*cutoff;
    }
    
    public double getCutoff() {
        return cutoff;
    }
    
    public void resetFilter() {
        double deformation = meter.getData().getValue(1);
        double factor = (1+deformation) / (1-deformation);
        axis.E(Math.pow(factor, -1.0/3.0));
        axis.setX(2,1.0/(axis.getX(0)*axis.getX(0)));
    }
    
    public boolean accept(IAtom a) {
        IVector p = ((IAtomPositioned)a).getPosition();
        work.E(p);
        work.DE(axis);
        double r2 = work.squared();
        return r2 < cutoffSq;
    }

    public boolean accept(IMolecule m) {
        throw new RuntimeException("go away");
    }

    private static final long serialVersionUID = 1L;
    protected double cutoff, cutoffSq;
    protected final IDataSource meter;
    protected final IVectorMutable axis;
    protected final IVectorMutable work;
}