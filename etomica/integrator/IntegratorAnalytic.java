package etomica.integrator;

import etomica.atom.AtomSet;
import etomica.exception.ConfigurationOverlapException;
import etomica.potential.PotentialMaster;
import etomica.simulation.ISimulation;
import etomica.util.IRandom;

/**
 * Integrator that generates atom trajectories from an analytic formula.
 * Takes an IntegratorAnalytic.AtomAction instance and performs action on all atoms in each
 * time step; intention is for the action to set the position of the atom for the
 * current time (but it could do anything).
 *
 * @author David Kofke
 * @author Nancy Cribbin
 */
public class IntegratorAnalytic extends IntegratorMD {
    
    private static final long serialVersionUID = 1L;
    private AtomAction action;
    
    public IntegratorAnalytic(ISimulation sim, PotentialMaster potentialMaster) {
        this(potentialMaster, sim.getRandom(), 0.05);
    }
    
    public IntegratorAnalytic(PotentialMaster potentialMaster, IRandom random,
                              double timeStep) {
        super(potentialMaster,random,timeStep,0);
    }
    
    public void doStepInternal() {
        super.doStepInternal();
        if(action == null) return;
        elapsedTime += getTimeStep();
        action.setTime(elapsedTime);
        AtomSet leafList = phase.getSpeciesMaster().getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
            action.actionPerformed(leafList.getAtom(iLeaf));
        }
    }
    
    public void reset() throws ConfigurationOverlapException {
        elapsedTime = 0.0;
        super.reset();
    }
    
    public void setAction(AtomAction action) {this.action = action;}
    
    public AtomAction getAction() {return action;}
    
    private double elapsedTime = 0.0;
    
    /**
     * Extends AtomAction class to add a method to set the time.
     */
    public static abstract class AtomAction extends etomica.action.AtomActionAdapter {
        protected double time;
        public void setTime(double t) {time = t;}
    }
    
 }//end of IntegratorAnalytic
 
