package etomica;

import etomica.units.Dimension;

/**
 * Standard Monte Carlo atom-displacement trial move.
 *
 * @author David Kofke
 */
public class MCMoveMolecule extends MCMove {
    
    private final IteratorDirective iteratorDirective = new IteratorDirective(IteratorDirective.BOTH);
    private final AtomIteratorSinglet affectedAtomIterator = new AtomIteratorSinglet();
    private Atom molecule;

    public MCMoveMolecule(IntegratorMC parentIntegrator) {
        super(parentIntegrator);
        setStepSizeMax(Default.BOX_SIZE);
        setStepSizeMin(0.0);
        setStepSize(Default.ATOM_SIZE);
        setPerParticleFrequency(true);
        iteratorDirective.includeLrc = true;
        //set directive to exclude intramolecular contributions to the energy
        iteratorDirective.addCriterion(new IteratorDirective.PotentialCriterion() {
            public boolean excludes(Potential p) {return (p instanceof Potential1.Intramolecular);}
        });
    }
    
    public final Dimension getStepSizeDimension() {return Dimension.LENGTH;}
    public final Dimension getStepSizeMaxDimension() {return Dimension.LENGTH;}
    public final Dimension getStepSizeMinDimension() {return Dimension.LENGTH;}
    

    public boolean thisTrial() {
        if(phase.moleculeCount()==0) {return false;}
        molecule = phase.randomMolecule();

        double uOld = potential.set(phase).calculate(iteratorDirective.set(molecule), energy.reset()).sum();
        molecule.coord.displaceWithin(stepSize);
        double uNew = potential.calculate(iteratorDirective.set(molecule), energy.reset()).sum();//not thread safe for multiphase systems
        if(uNew >= Double.MAX_VALUE) {//reject
            molecule.coord.replace();
            return false;
        } else if(uNew <= uOld) {   //accept
            return true;
        } else if(  //reject
           Math.exp(-(uNew-uOld)/parentIntegrator.temperature) < Simulation.random.nextDouble()) {
             molecule.coord.replace();
             return false;
        }
        //accept
        return true;
    }//end thisTrial
    
    public final AtomIterator affectedAtoms() {
        affectedAtomIterator.setBasis(molecule);
        return affectedAtomIterator;
    }

/*    public static void main(String[] args) {
        
	    IntegratorMC integrator = new IntegratorMC();
	    MCMoveMolecule mcMove = new MCMoveMolecule(integrator);
	    SpeciesSpheres species = new SpeciesSpheres(20,3);
	    Phase phase = new Phase();
	    P2LennardJones potential = new P2LennardJones();
	    Controller controller = new Controller();
	    DisplayPhase display = new DisplayPhase();

		MeterEnergy energy = new MeterEnergy();
		energy.setPhase(phase);
		energy.setHistorying(true);
		energy.setActive(true);		
		energy.getHistory().setNValues(500);		
		DisplayPlot plot = new DisplayPlot();
		plot.setLabel("Energy");
		plot.setDataSources(energy.getHistory());
		
		integrator.setSleepPeriod(2);
		
		Simulation.instance.elementCoordinator.go();
	    
        potential.setIterator(new AtomPairIterator(phase));
        potential.set(species.getAgent(phase));
        
        Simulation.makeAndDisplayFrame(Simulation.instance);
    }//end of main
  */  
}