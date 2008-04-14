//Source file generated by Etomica

package etomica.simulation.prototypes;

import etomica.action.BoxImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.api.IAtom;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.ISimulation;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.graphics.ColorScheme;
import etomica.graphics.DisplayBox;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.modifier.Modifier;
import etomica.potential.P1HardPeriodic;
import etomica.potential.P2SquareWell;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Dimension;
import etomica.units.Length;

//remember to set up Space3D.CoordinatePair.reset if experiencing 
//problems with this simulation hanging

public class SWMD3D extends Simulation {

	public class MyModifier implements Modifier {

        public Dimension getDimension() {
            return Length.DIMENSION;
        }

		/**
		 * @see etomica.modifier.Modifier#setValue(double)
		 */
		public void setValue(double d) {
			potential.setCoreDiameter(d);
			((IAtomTypeSphere)species.getLeafType()).setDiameter(d);
		}

		/**
		 * @see etomica.modifier.Modifier#getValue()
		 */
		public double getValue() {
			return potential.getCoreDiameter();
		}
        
        public String getLabel() {
            return "diameter";
        }

	}
	
  public SWMD3D(Space _space) {
	super(_space);
	IPotentialMaster potentialMaster = new PotentialMasterMonatomic(this, space);
	
    integrator = new IntegratorHard(this, potentialMaster, space);
    integrator.setTimeStep(0.01);
    integrator.setIsothermal(true);
    integrator.setTemperature(1);
    double lambda = 1.6;
    ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
    getController().addAction(activityIntegrate);


    box = new Box(this, space);
    addBox(box);
    potential  = new etomica.potential.P2SquareWell(space);
    potential.setLambda(lambda);

    species  = new etomica.species.SpeciesSpheresMono(this, space);
    getSpeciesManager().addSpecies(species);
    box.setNMolecules(species, 108);
    integrator.setNullPotential(new P1HardPeriodic(space, lambda), species.getLeafType());

	
//	DeviceSlider tControl = new DeviceSlider(integrator, "temperature");
//	DeviceSlider sigmaControl = new DeviceSlider(new MyModifier());
//	DeviceSlider lambdaControl = new DeviceSlider(potential0, "lambda");
//	tControl.setLabel("Temperature (K)");
//	sigmaControl.setLabel("Atom size (Angstroms)");
//	tControl.setShowValues(true);
//	tControl.setShowBorder(true);
//	tControl.setMinimum(100);
//	tControl.setMaximum(700);
//	sigmaControl.setShowValues(true);
//	sigmaControl.setShowBorder(true);
//	sigmaControl.setPrecision(2);
//	sigmaControl.setMinimum(0.0);
//	sigmaControl.setMaximum(3.0);
//	lambdaControl.setShowValues(true);
//	lambdaControl.setShowBorder(true);
//	lambdaControl.setPrecision(2);
//	lambdaControl.setMinimum(1.1);
//	lambdaControl.setMaximum(2.1);
//	lambdaControl.setValue(1.4);
//	lambdaControl.setNMajor(5);


    potentialMaster.addPotential(potential,new IAtomTypeLeaf[]{species.getLeafType(),species.getLeafType()});

    integrator.setBox(box);
    integrator.addIntervalAction(new BoxImposePbc(box, space));

//	DeviceNSelector nControl = new DeviceNSelector(speciesSpheres0.getAgent(box0));
//	nControl.setMaximum(108);
	box.setDensity(0.0405);
    ConfigurationLattice configuration = new ConfigurationLattice(new LatticeCubicFcc(), space);
    configuration.initializeCoordinates(box);
  }

  private static final long serialVersionUID = 1L;
  public IntegratorHard integrator;
  public SpeciesSpheresMono species;
  public IBox box;
  public P2SquareWell potential;
  public Controller controller;
  public DisplayBox display;


  
  public static class MyColorScheme extends ColorScheme {
      public MyColorScheme(ISimulation sim, IAtom redAtom) {
    	  super(sim);
          atom = redAtom;
      }
	  public java.awt.Color getAtomColor(IAtom a) {
		  return (a == atom) ? java.awt.Color.red : java.awt.Color.yellow;
	  }
      private static final long serialVersionUID = 1L;
      private IAtom atom;
  }

}//end of class
