/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.integrator.mcmove;

import etomica.atom.*;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.box.Box;
import etomica.box.BoxAgentManager;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.nbr.cell.Api1ACell;
import etomica.nbr.cell.NeighborCellManager;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.nbr.cell.molecule.NeighborCellManagerMolecular;
import etomica.potential.IPotentialAtomic;
import etomica.simulation.Simulation;
import etomica.space.RotationTensor;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.util.random.IRandom;

/**
 * Standard Monte Carlo atom-displacement trial move.
 *
 * @author David Kofke
 */
public class MCMoveDimerRotate extends MCMoveBoxStep {
    
    private static final long serialVersionUID = 2L;
    protected final AtomIteratorArrayListSimple affectedAtomIterator;
    protected final AtomArrayList affectedAtoms;
    protected final MeterPotentialEnergy energyMeter;
    protected final Vector translationVector;
    protected IAtom atom;
    protected double uOld;
    protected double uNew = Double.NaN;
    protected AtomSource atomSource;
    protected boolean fixOverlap;
    protected final IRandom random;
    protected Space space;
    protected final PotentialMasterCell potentialMaster;
    protected final Vector dr;
    protected final IPotentialAtomic dimerPotential;
    protected IAtom atom1;
    protected transient RotationTensor rotationTensor;
    protected final Vector r0;
    private final BoxAgentManager<Api1ACell> neighborIterators;

    public MCMoveDimerRotate(Simulation sim, PotentialMasterCell potentialMaster, Space _space, IPotentialAtomic dimerPotential) {
        this(sim, potentialMaster, sim.getRandom(), _space, 1.0, 15.0, false, dimerPotential);
    }
    
    public MCMoveDimerRotate(Simulation sim, PotentialMasterCell potentialMaster, IRandom random,
                             Space _space, double stepSize, double stepSizeMax,
                             boolean fixOverlap, IPotentialAtomic dimerPotential) {
        super(potentialMaster);
        this.affectedAtoms = new AtomArrayList(2);
        this.affectedAtomIterator = new AtomIteratorArrayListSimple(affectedAtoms);
        this.potentialMaster = potentialMaster;
        this.neighborIterators = new BoxAgentManager<>(sim, aBox -> new Api1ACell(1.0, aBox, ((NeighborCellManager) potentialMaster.getBoxCellManager(box))));
        this.dr = _space.makeVector();
        rotationTensor = _space.makeRotationTensor();
        this.r0= _space.makeVector();
        this.random = random;
        this.space = _space;
        this.dimerPotential = dimerPotential;
        atomSource = new AtomSourceRandomLeaf();
        ((AtomSourceRandomLeaf)atomSource).setRandomNumberGenerator(random);
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        translationVector = space.makeVector();
        setStepSizeMax(stepSizeMax);
        setStepSizeMin(0.0);
        setStepSize(stepSize);
        perParticleFrequency = true;
        energyMeter.setIncludeLrc(false);
        this.fixOverlap = fixOverlap;
    }
    
    /**
     * Method to perform trial move.
     */
    public boolean doTrial() {
        atom = atomSource.getAtom();
        if (atom == null) return false;
        Api1ACell neighborIterator = neighborIterators.getAgent(this.box);
        neighborIterator.setTarget(atom);
        neighborIterator.reset();
        IAtom atom0 = atom;
        atom1 = null;
        for (IAtomList pair = neighborIterator.next(); pair != null;
             pair = neighborIterator.next()) {
        	 atom1 = pair.get(1);
        	 if (atom1 == atom0) {
        		 atom1 = pair.get(0);
        	 }
        	dr.Ev1Mv2(atom0.getPosition(),atom1.getPosition());
        	box.getBoundary().nearestImage(dr);
        	if (dr.squared() < 1) {
        		 double u = dimerPotential.energy(pair);
        		 if (u < 0) {
        			 break;
        		 }
        	}
        	atom1 = null;
        	        }
        if (atom1 == null){
        	return false;
        }
        energyMeter.setTarget(atom);
        uOld = energyMeter.getDataAsScalar();
        energyMeter.setTarget(atom1);
        uOld += energyMeter.getDataAsScalar();
        if(uOld > 1e8 && !fixOverlap) {
            throw new RuntimeException("atom "+atom+" in box "+box+" has an overlap");
        }
        double dTheta = (2*random.nextDouble() - 1.0)*stepSize;
        rotationTensor.setAxial(r0.getD() == 3 ? random.nextInt(3) : 2,dTheta);
        r0.E(atom1.getPosition());
        r0.PEa1Tv1(0.5,dr);
        doTransform(atom0);
        doTransform(atom1);
        return true;
    }//end of doTrial
    protected void doTransform(IAtom a) {
            Vector r = a.getPosition();
            r.ME(r0);
            box.getBoundary().nearestImage(r);
            rotationTensor.transform(r);
            r.PE(r0);
        }

    public double getChi(double temperature) {
        uNew = energyMeter.getDataAsScalar();
        energyMeter.setTarget(atom);
        uNew += energyMeter.getDataAsScalar();
        return Math.exp(-(uNew - uOld) / temperature);
    }
    
    public double energyChange() {return uNew - uOld;}
    
    /**
     * Method called by IntegratorMC in the event that the most recent trial is accepted.
     */
    public void acceptNotify() {  /* do nothing */
    }
    
    /**
     * Method called by IntegratorMC in the event that the most recent trial move is
     * rejected.  This method should cause the system to be restored to the condition
     * before the most recent call to doTrial.
     */
    public void rejectNotify() {
        rotationTensor.invert();
        doTransform(atom);
        doTransform(atom1);
        
    }
        
    
    public AtomIterator affectedAtoms() {
        affectedAtoms.clear();
        affectedAtoms.add(atom);
        affectedAtoms.add(atom1);
        return affectedAtomIterator;
    }
    
    public void setBox(Box p) {
        super.setBox(p);
        energyMeter.setBox(p);
        atomSource.setBox(p);
    }
    
    /**
     * @return Returns the atomSource.
     */
    public AtomSource getAtomSource() {
        return atomSource;
    }
    /**
     * @param source The atomSource to set.
     */
    public void setAtomSource(AtomSource source) {
        atomSource = source;
    }
}
