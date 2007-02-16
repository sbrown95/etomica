package etomica.models.hexane;

import etomica.atom.Atom;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomTreeNodeGroup;
import etomica.atom.iterator.AtomIterator;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMovePhase;
import etomica.phase.Phase;
import etomica.potential.PotentialMaster;
import etomica.space.Vector;
import etomica.util.Constants;

public abstract class MCMoveCBMC extends MCMovePhase {

    public MCMoveCBMC(PotentialMaster potentialMaster, IntegratorMC integrator){
        super(potentialMaster);

        beta = 1.0/integrator.getTemperature()/Constants.BOLTZMANN_K;
        atomList = new AtomArrayList(chainlength);
        
        externalMeter = new MeterPotentialEnergy(potentialMaster);
        energyMeter = new MeterPotentialEnergy(potentialMaster);
    }
    
    public double energyChange() {
        return uNew - uOld;
    }

    public void setPhase(Phase p){
        super.setPhase(p);
        energyMeter.setPhase(p);
        externalMeter.setPhase(p);
    }
    
    public void acceptNotify() {
        // I rather think nothing needs to be done.
    }

    public boolean doTrial() {
        //pick a molecule & get its childlist.
        atom = phase.randomMolecule();
        uOld = energyMeter.getDataAsScalar();
        
        atomList = ((AtomTreeNodeGroup)atom.getNode()).getChildList();
        chainlength = atomList.size();
        //store the old locations of every atom in the molecule in positionOld.
        for(int i = 0; i < chainlength; i++){
            positionOld[i].E((Vector)((AtomLeaf)atomList.get(i)).getCoord().position());
        }
        
        calcRosenbluthFactors();
        
        //Accept or reject the move.
        if(Math.random() <= wNew/wOld){
            acceptNotify();
            return true;
        } else {
            rejectNotify();
            return false;
        }
    }

    public abstract double getA();
    public abstract double getB();
    protected abstract void calcRosenbluthFactors();
    protected abstract int calcNumberOfTrials();
    public abstract AtomIterator affectedAtoms();
    
    protected void setChainlength(int n) {chainlength = n;}
    public int getNumTrial(){return numTrial;}
    public int getChainlength() {return chainlength;}
    
    public void rejectNotify() {
        for(int i = 0; i < chainlength; i++){
            ((AtomLeaf)atomList.get(i)).getCoord().position().E(positionOld[i]);
        }
    }
    
    private static final long serialVersionUID = 1L;
    protected final MeterPotentialEnergy externalMeter;
    protected final MeterPotentialEnergy energyMeter;
    protected double wNew;          //Rosenbluth factor of new configuration
    protected double wOld;         //Rosenbluth factor of old configuration
    protected int chainlength;         //the number of atoms in a molecule; some juggling may be necessary to make this work if we want to make the chains longer....
    protected double beta;
    protected Atom atom;
    protected double uOld;
    protected double uNew = Double.NaN;
    private Vector[] positionOld;      //Used to store the position of the molecule before mofing it.
    protected AtomArrayList atomList;
    protected int numTrial;
}