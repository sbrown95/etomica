package etomica.mappedRdf2;

import etomica.api.IAtom;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.iterator.IteratorDirective;
import etomica.box.Box;
import etomica.data.*;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialMaster;
import etomica.space.Space;
import etomica.units.Null;

import java.io.Serializable;

public class MeterMappedRdf2 implements IEtomicaDataSource, DataSourceIndependent, Serializable, AtomLeafAgentManager.AgentSource<IntegratorVelocityVerlet.MyAgent> {

    protected final PotentialCalculationForceSum pcForce;
    protected final AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent> forceManager;

    public MeterMappedRdf2(Space space, PotentialMaster potentialMaster, Box box, int nbins) {
        this.space = space;
        this.box = box;

        this.potentialMaster = potentialMaster;

        pcForce = new PotentialCalculationForceSum();
        if (box != null) {
            forceManager = new AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent>(this, box, IntegratorVelocityVerlet.MyAgent.class);
            pcForce.setAgentManager(forceManager);
        } else {
            forceManager = null;
        }

        pc = new PotentialCalculationMappedRdf(space, box, nbins, forceManager);

        xDataSource = pc.getXDataSource();

        rData = (DataDoubleArray) xDataSource.getData();
        data = new DataFunction(new int[]{rData.getLength()});
        dataInfo = new DataFunction.DataInfoFunction("g(r)", Null.DIMENSION, this);

        allAtoms = new IteratorDirective();

        tag = new DataTag();
        dataInfo.addTag(tag);
    }

    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }

    /**
     * Zero's out the RDF sum tracked by this meter.
     */
    public void reset() {
        rData = (DataDoubleArray) xDataSource.getData();
        xMax = xDataSource.getXMax();
        pc.getXDataSource().setNValues(rData.getLength());
        pc.getXDataSource().setXMax(xMax);
        data = new DataFunction(new int[]{rData.getLength()});
        dataInfo = new DataFunction.DataInfoFunction("g(r)", Null.DIMENSION, this);
        dataInfo.addTag(tag);
    }


    /**
     * Returns the RDF, averaged over the calls to actionPerformed since the
     * meter was reset or had some parameter changed (xMax or # of bins).
     */
    public IData getData() {
        if (rData != xDataSource.getData() ||
                data.getLength() != rData.getLength() ||
                xDataSource.getXMax() != xMax) {
            reset();
        }
        pcForce.reset();
        pc.reset();
        potentialMaster.calculate(box, allAtoms, pcForce);

        potentialMaster.calculate(box, allAtoms, pc);


        final double[] y = data.getData();

        long numAtomPairs = 0;
        long numAtoms = box.getLeafList().getAtomCount();
        numAtomPairs = numAtoms * (numAtoms - 1)/2 ;
        double norm = numAtomPairs  / box.getBoundary().volume();
        double[] r = rData.getData();
        double dx2 = 0.5 * (xMax - xDataSource.getXMin()) / r.length;
        double[] gSum2 = pc.getGSum2();
        double[] gR = pc.gR();

  //      System.out.println("metervol " + box.getBoundary().volume());

        for (int i = 0; i < r.length; i++) {
//            double vShell = space.sphereVolume(r[i]+dx2)-space.sphereVolume(r[i]-dx2);
            // y[i] = (gR[i]*callCount+gSum[i])*01/(norm*vShell);

              y[i] = 1 + (gSum2[i] / (norm)) ;
        //    y[i] =  (gSum[i] / (norm)) ;

        }
 //       System.out.println(y[10]);
        return data;
    }

    public DataSourceUniform getXDataSource() {
        return xDataSource;
    }

    public DataDoubleArray getIndependentData(int i) {
        return (DataDoubleArray) xDataSource.getData();
    }

    public DataDoubleArray.DataInfoDoubleArray getIndependentDataInfo(int i) {
        return (DataDoubleArray.DataInfoDoubleArray) xDataSource.getDataInfo();
    }

    public DataTag getIndependentTag() {
        return xDataSource.getTag();
    }

    public int getIndependentArrayDimension() {
        return 1;
    }

    /**
     * @return Returns the box.
     */
    public Box getBox() {
        return box;
    }

    /**
     * @param box The box to set.
     */
    public void setBox(Box box) {
        pc.setBox(box);
        this.box = box;
    }

    public String getName() {
        return name;
    }

    public PotentialCalculationMappedRdf getPotentialCalculation() {
        return pc;
    }

    public void setName(String name) {
        this.name = name;
    }


    protected Box box;
    protected final Space space;
    protected DataFunction data;
    private IEtomicaDataInfo dataInfo;
    protected DataDoubleArray rData;
    protected IteratorDirective allAtoms;
    protected final DataSourceUniform xDataSource;
    protected double xMax;
    private String name;
    protected final DataTag tag;
    protected final PotentialMaster potentialMaster;
    protected final PotentialCalculationMappedRdf pc;

    public IntegratorVelocityVerlet.MyAgent makeAgent(IAtom a, Box agentBox) {
        return new IntegratorVelocityVerlet.MyAgent(space);
    }

    public void releaseAgent(IntegratorVelocityVerlet.MyAgent agent, IAtom atom, Box agentBox) {
    }

}
