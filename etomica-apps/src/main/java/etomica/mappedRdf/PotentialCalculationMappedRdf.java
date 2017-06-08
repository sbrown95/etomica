package etomica.mappedRdf;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IPotentialAtomic;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomPair;
import etomica.atom.iterator.IteratorDirective;
import etomica.box.Box;
import etomica.data.DataSourceUniform;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.Potential2SoftSpherical;
import etomica.potential.PotentialCalculation;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Space3D;
import etomica.units.Length;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by aksharag on 5/16/17.
 */
public class PotentialCalculationMappedRdf implements PotentialCalculation{
    protected final Vector dr;
    protected long[] gSum;
    protected IBoundary boundary;
    protected final DataSourceUniform xDataSource;
    protected double xMax;
    protected double vol;
    protected double q;
    protected double qp;
    protected double sum, R;
    protected double x0, vCut;
    protected double vShift;
    protected double beta;
    protected double c1;
    protected final int nbins;
    protected final Space space;
    protected final double[] cumint;
    protected final AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent> forceManager;


    public PotentialCalculationMappedRdf(Space space, Box box, int nbins, AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent> forceManager) {
        dr = space.makeVector();
        xDataSource = new DataSourceUniform("r", Length.DIMENSION);
        xDataSource.setTypeMax(DataSourceUniform.LimitType.HALF_STEP);
        xDataSource.setTypeMin(DataSourceUniform.LimitType.HALF_STEP);

        gSum = new long[xDataSource.getData().getLength()];

        this.boundary = box.getBoundary();
        this.nbins = nbins;
        this.space = space;

        cumint = new double[nbins+1];

        this.forceManager = forceManager;
    }

    public DataSourceUniform getXDataSource() {
        return xDataSource;
    }

    public void setBox(Box box) {
        boundary = box.getBoundary();
    }

    public double getX0() {
        return x0;
    }

    public double getq() {
        return q;
    }

    public void setVCut(double newVCut) {
        vCut = newVCut;
    }

    /**
     * Sets volume to an arbitrary value (instead of the box volume).  This is
     * used for 1/(1+q/V) terms.
     */
    public void setVolume(double newVol) {
        vol = newVol;
    }

    public void setTemperature(double T, Potential2SoftSpherical p2) {
        beta = 1/T;
        double rc = p2.getRange();
        x0 = rc;
        q = 0;

        if (vCut==0) vCut = x0;
        vShift = -p2.u(vCut*vCut);
        c1 = Math.log(rc+1)/nbins;
        int D = space.D();
        for (int i=1; i<=nbins; i++) {
            double r = Math.exp(c1*i)-1;
            double r2 = r*r;
            if (r >= p2.getRange() ) {
                if (i<nbins) throw new RuntimeException("oops "+i+" "+nbins+" "+r+" "+p2.getRange());
                r = Math.exp(c1*i*0.9999)-1;
                r2 = r*r;
            }
            double u = p2.u(r2);
            double evm1 = 0;
            double v = calcV(r,u);
            evm1 = Math.exp(-beta*v);
            q += (D==2 ? r : r2)*(evm1-1)*c1*(r+1);

        }

        q *= (D==2?2:4)*Math.PI*vol;
        q += vol*vol;

        for (int i=1; i<=nbins; i++) {
            double r = Math.exp(c1*i)-1;
            double r2 = r*r;
            if (r >= p2.getRange()) {
                if (i<nbins) throw new RuntimeException("oops "+i+" "+nbins+" "+r+" "+p2.getRange());
                r = Math.exp(c1*i*0.9999)-1;
                r2 = r*r;
            }
            double u = p2.u(r2);
            double evm1 = 0;
            double v = calcV(r,u);
            evm1 = Math.exp(-beta*v);
            cumint[i] = cumint[i-1] + (D==2 ? r : r2)*(evm1)*c1*(r+1);

        }

    }

    protected double calcXu(double r, double u, int R, Potential2SoftSpherical p2) {

        double y = cumint(r);
        double v = calcV(r,u);
        double uR = p2.u(R);
        double vR = calcV(R,uR);
        double evm1 = Math.exp(-beta*v);
        double evmR = Math.exp(-beta*vR);

        if(r<R)
            return y*(evmR/evm1)*(R/r)*(R/r)*beta*4*Math.PI/q*-1;
        else
            return beta*(R/r)*(R/r)*(evmR/evm1)*(1-(4*Math.PI/q*y));

    }

    protected double calcV(double r,double u){

        if(r>vCut)
            return 0;
        return u+vShift;
    }

    protected double cumint( double r) {
        // r = (exp(c1*i) - 1)
        double i = Math.log(r+1)/c1;
        int ii = (int)i;
        double y = cumint[ii] + (cumint[ii+1]-cumint[ii])*(i-ii);
        return y;
    }

    /**
     * Zero's out the RDF sum tracked by this meter.
     */
    public void reset() {
        xMax = xDataSource.getXMax();
        gSum = new long[xDataSource.getData().getLength()];
    }

    public void doCalculation(IAtomList atoms, IPotentialAtomic potential) {
        if (!(potential instanceof Potential2SoftSpherical)) return;
        Potential2SoftSpherical p2 = (Potential2SoftSpherical)potential;
        IAtom atom0 = atoms.getAtom(0);
        IAtom atom1 = atoms.getAtom(1);
        dr.Ev1Mv2(atom0.getPosition(), atom1.getPosition());

        double xMaxSquared = xMax*xMax;
        boundary.nearestImage(dr);
        double r2 = dr.squared();       //compute pair separation
        double r = Math.sqrt(r2);
        if (r > p2.getRange()) return;
        double fij = p2.du(r2);
        double up = fij/r;
        double vp = up;
        double u= p2.u(r2);
        if(r2 < xMaxSquared) {
            int index = xDataSource.getIndex(Math.sqrt(r2));  //determine histogram index

            if (index<x0) {
                Vector fi = forceManager.getAgent(atom0).force;
                Vector fj = forceManager.getAgent(atom1).force;
                //  System.out.println(u+" "+r);
                double fifj = (fi.dot(dr) - fj.dot(dr))/r;
                double xu = calcXu(r, u, index, p2);
                double wp = 0.5*fifj;
                gSum[index] += xu*(vp-wp);               //add once for each atom

            }
        }
    }

    public long[] getGSum() {
        return gSum;
    }
}
