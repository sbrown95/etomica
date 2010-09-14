package etomica.integrator.mcmove;

import etomica.action.AtomActionTranslateBy;
import etomica.action.MoleculeActionTranslateTo;
import etomica.action.MoleculeChildAtomAction;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionCOM;
import etomica.atom.IAtomPositionDefinition;
import etomica.atom.MoleculeSource;
import etomica.atom.MoleculeSourceRandomMolecule;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.atom.iterator.AtomIteratorNull;
import etomica.box.RandomPositionSource;
import etomica.box.RandomPositionSourceRectangular;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorMC;
import etomica.space.ISpace;

/**
 * Performs a trial that results in the exchange of a molecule from one box to another.
 * Primary use is as an elementary move in a Gibbs ensemble simulation
 *
 * @author David Kofke
 */
 
public class MCMoveMoleculeExchange extends MCMove {
    
    private static final long serialVersionUID = 2L;
    protected IBox box1;
    protected IBox box2;
    protected final IntegratorBox integrator1, integrator2;
    private final MeterPotentialEnergy energyMeter;
    private final AtomIteratorArrayListSimple affectedAtomIterator = new AtomIteratorArrayListSimple();
    private final MoleculeActionTranslateTo moleculeTranslator;
    private final MoleculeChildAtomAction moleculeReplacer;
    private final IVectorMutable translationVector;
    private final IRandom random;
    private MoleculeSource moleculeSource;
    protected RandomPositionSource positionSource;
    
    private transient IMolecule molecule;
    private transient IBox iBox, dBox;
    private transient double uOld;
    private transient double uNew = Double.NaN;
    

    public MCMoveMoleculeExchange(IPotentialMaster potentialMaster, IRandom random,
    		                      ISpace _space,
                                  IntegratorBox integrator1,
                                  IntegratorBox integrator2) {
        super(potentialMaster);
        this.random = random;
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        energyMeter.setIncludeLrc(true);
        moleculeReplacer = new MoleculeChildAtomAction(new AtomActionTranslateBy(_space));
        moleculeTranslator = new MoleculeActionTranslateTo(_space);
        translationVector = _space.makeVector();
        setAtomPositionDefinition(new AtomPositionCOM(_space));
        this.integrator1 = integrator1;
        this.integrator2 = integrator2;
        box1 = integrator1.getBox();
        box2 = integrator2.getBox();
        moleculeSource = new MoleculeSourceRandomMolecule();
        ((MoleculeSourceRandomMolecule)moleculeSource).setRandom(random);
        positionSource = new RandomPositionSourceRectangular(_space, random);
    }
    
    public boolean doTrial() {
        if(random.nextInt(2) == 0) {
            iBox = box1;
            dBox = box2;
        }
        else {
            iBox = box2;
            dBox = box1;
        }
        if(dBox.getMoleculeList().getMoleculeCount() == 0) { //no molecules to delete; trial is over
            uNew = uOld = 0.0;
            return false;
        }

        moleculeSource.setBox(dBox);
        molecule = moleculeSource.getMolecule();  //select random molecule to delete

        energyMeter.setBox(dBox);
        energyMeter.setTarget(molecule);
        uOld = energyMeter.getDataAsScalar();
        dBox.removeMolecule(molecule);

        positionSource.setBox(iBox);
        moleculeTranslator.setDestination(positionSource.randomPosition());         //place at random in insertion box
        moleculeTranslator.actionPerformed(molecule);
        iBox.addMolecule(molecule);
        uNew = Double.NaN;
        return true;
    }//end of doTrial

    /**
     * Sets a new RandomPositionSource for this move to use.  By default, a
     * position source is used which assumes rectangular boundaries.
     */
    public void setPositionSource(RandomPositionSource newPositionSource) {
        positionSource = newPositionSource;
    }
    
    /**
     * Returns the RandomPositionSource used by this move.
     */
    public RandomPositionSource getPositionSource() {
        return positionSource;
    }

    /**
     * Sets the AtomSource this class uses to pick molecules to delete.
     */
    public void setMoleculeSource(MoleculeSource newMoleculeSource) {
        moleculeSource = newMoleculeSource;
    }
    
    /**
     * Returns the AtomSource this class uses to pick molecules to delete.
     */
    public MoleculeSource getMoleculeSource() {
        return moleculeSource;
    }
    
    public double getA() {
        energyMeter.setBox(iBox);
        energyMeter.setTarget(molecule);
        uNew = energyMeter.getDataAsScalar();
        double B = -(uNew - uOld);
        // assume both integrators have the same temperature
        double T = integrator1.getTemperature();
        //note that dSpecies.nMolecules has been decremented
        //and iSpecies.nMolecules has been incremented
        return Math.exp(B/T) * (dBox.getNMolecules(molecule.getType())+1)/dBox.getBoundary().volume()
               * iBox.getBoundary().volume()/iBox.getNMolecules(molecule.getType()); 
    }
    
    public double getB() {
        //IntegratorManagerMC only calls getA since it doesn't have a temperature
        throw new IllegalStateException("You shouldn't be calling this method");
    }
    
    public void acceptNotify() {
        IntegratorBox iIntegrator = integrator1;
        IntegratorBox dIntegrator = integrator2;
        if (iIntegrator.getBox() == dBox) {
            iIntegrator = integrator2;
            dIntegrator = integrator1;
        }
        if (iIntegrator instanceof IntegratorMC) {
            ((IntegratorMC)iIntegrator).notifyEnergyChange(uNew);
        }
        else {
            //XXX grossly inefficient
            iIntegrator.reset();
        }
        if (dIntegrator instanceof IntegratorMC) {
            ((IntegratorMC)dIntegrator).notifyEnergyChange(-uOld);
        }
        else {
            //XXX grossly inefficient
            dIntegrator.reset();
        }
    }
    
    public void rejectNotify() {
        iBox.removeMolecule(molecule);
        translationVector.Ea1Tv1(-1,moleculeTranslator.getTranslationVector());
        ((AtomActionTranslateBy)moleculeReplacer.getAtomAction()).setTranslationVector(translationVector);
        moleculeReplacer.actionPerformed(molecule);
        dBox.addMolecule(molecule);
    }

    public final AtomIterator affectedAtoms(IBox box) {
        if(this.box1 != box && this.box2 != box) return AtomIteratorNull.INSTANCE;
        affectedAtomIterator.setList(molecule.getChildList());
        return affectedAtomIterator;
    }
    
    public double energyChange(IBox box) {
        if(box == iBox) return uNew;
        else if(box == dBox) return -uOld;
        else return 0.0;
    }

    
    /**
     * @return Returns the atomPositionDefinition.
     */
    public IAtomPositionDefinition getAtomPositionDefinition() {
        return moleculeTranslator.getAtomPositionDefinition();
    }
    /**
     * @param atomPositionDefinition The atomPositionDefinition to set.
     */
    public void setAtomPositionDefinition(
            IAtomPositionDefinition atomPositionDefinition) {
        moleculeTranslator.setAtomPositionDefinition(atomPositionDefinition);
    }
}//end of MCMoveMoleculeExchange