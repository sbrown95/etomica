package etomica.mappedDensity;

import etomica.atom.IAtomList;
import etomica.box.Box;
import etomica.potential.IPotentialAtomic;
import etomica.potential.PotentialSoft;
import etomica.space.Space;
import etomica.space.Tensor;
import etomica.space.Vector;

public class P1EXP implements IPotentialAtomic, PotentialSoft {

    private double L;
    private final Vector[] gradient;

    public P1EXP(Space space) {
        gradient = new Vector[]{space.makeVector()};
    }

    @Override
    public double virial(IAtomList atoms) {
        return 0;
    }

    @Override
    public Vector[] gradient(IAtomList atoms) {
        double z = atoms.get(0).getPosition().getX(2);
        gradient[0].setX(2, -2*z*Math.exp(-z*z));
//        System.out.println(z+" "+energy(atoms)+" "+gradient[0].getX(2));
        return gradient;
    }

    @Override
    public Vector[] gradient(IAtomList atoms, Tensor pressureTensor) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public double energy(IAtomList atoms) {
        double z = atoms.get(0).getPosition().getX(2);
        return Math.exp(-z*z);
    }

    @Override
    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void setBox(Box box) {
        L = box.getBoundary().getBoxSize().getX(2);
    }

    @Override
    public int nBody() {
        return 1;
    }


}
