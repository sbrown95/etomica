package etomica.simulations;
import java.util.Iterator;

import etomica.Controller;
import etomica.DataManager;
import etomica.DataSink;
import etomica.Default;
import etomica.Phase;
import etomica.Simulation;
import etomica.Space;
import etomica.Species;
import etomica.SpeciesSpheresMono;
import etomica.action.activity.ActivityIntegrate;
import etomica.data.AccumulatorAverage;
import etomica.data.DataSourceCountSteps;
import etomica.data.meter.MeterDensity;
import etomica.data.meter.MeterPressureHard;
import etomica.data.meter.MeterTemperature;
import etomica.graphics.Display;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayCanvasInterface;
import etomica.graphics.DisplayPhase;
import etomica.graphics.DisplayTimer;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHardPiston;
import etomica.potential.P1HardBoundary;
import etomica.potential.P1HardMovingBoundary;
import etomica.potential.P2SquareWell;
import etomica.space.BoundaryNone;
import etomica.space.Vector;
import etomica.space2d.Vector2D;
import etomica.space3d.Vector3D;
import etomica.units.Bar;
import etomica.units.BaseUnit;
import etomica.units.Kelvin;
import etomica.units.PrefixedUnit;

/**
 * Simple hard-sphere MD in piston-cylinder apparatus
 */
public class PistonCylinder extends Simulation {
    
    public IntegratorHardPiston integrator;
    public SpeciesSpheresMono species;
    public Phase phase;
    public Controller controller;
    public P2SquareWell potential;
    public P1HardBoundary wallPotential;
    public P1HardMovingBoundary pistonPotential;
    public ActivityIntegrate ai;
    public double lambda;

    public PistonCylinder(int D) {
        super(Space.makeSpace(D));
        lambda = 1.5;
        controller = getController();
        Default.ATOM_MASS = 16;
	    species = new SpeciesSpheresMono(this);
	    species.setNMolecules(100);
	    phase = new Phase(space);
        phase.setBoundary(new BoundaryNone(space));
        Vector newDim;
        if (space.D() == 2) {
            newDim = new Vector2D(100,1000);
        }
        else {
            newDim = new Vector3D(150,150,150);
        }
        phase.boundary().setDimensions(newDim);
        
        phase.speciesMaster.addSpecies(species);
	    
	    potential = new P2SquareWell(space,Default.ATOM_SIZE,lambda,31.875);
//        potential = new P2HardSphere(space,Default.ATOM_SIZE);
	    potentialMaster.setSpecies(potential,new Species[]{species,species});
	    
        wallPotential = new P1HardBoundary(space);
        wallPotential.setCollisionRadius(Default.ATOM_SIZE*0.5); //potential.getCoreDiameter()*0.5);
        potentialMaster.setSpecies(wallPotential,new Species[]{species});
        wallPotential.setActive(0,true,true);  // left wall
        wallPotential.setActive(0,false,true); // right wall
        wallPotential.setActive(1,true,false); // top wall
        wallPotential.setActive(1,false,true);// bottom wall
        if (D==3) {
            wallPotential.setActive(2,true,true);  // front wall
            wallPotential.setActive(2,false,true); // back wall
        }

        pistonPotential = new P1HardMovingBoundary(space,phase.boundary(),1,Default.ATOM_MASS*100);
        pistonPotential.setCollisionRadius(Default.ATOM_SIZE*0.5);
        pistonPotential.setWallPosition(0.0);
        pistonPotential.setWallVelocity(0.5);
        if (D == 3) {
            pistonPotential.setPressure(Bar.UNIT.toSim(1.0));
        }
        else {
            pistonPotential.setPressure(Bar.UNIT.toSim(100.0));
        }
        pistonPotential.setThickness(1.0);
        potentialMaster.setSpecies(pistonPotential,new Species[]{species});
	    
	    integrator = new IntegratorHardPiston(potentialMaster,pistonPotential);
        integrator.addPhase(phase);
        integrator.setIsothermal(true);
        integrator.setThermostatInterval(1000);
        integrator.setTimeStep(1.0);
        ai = new ActivityIntegrate(integrator);
        getController().addAction(ai);
	    
    }

    public static class Applet extends javax.swing.JApplet {
        public DataSourceCountSteps meterCycles;
        public DisplayBox displayCycles;
        public MeterTemperature thermometer;

        public void init() {
            PistonCylinder pc = new PistonCylinder(2);
            BaseUnit.Length.Sim.TO_PIXELS = 800/pc.phase.boundary().dimensions().x(1);
            pc.ai.setDoSleep(true);
            pc.ai.setSleepPeriod(1);

            pc.integrator.setThermostatInterval(1000);
            pc.integrator.setTimeStep(0.5);

            pc.wallPotential.setLongWall(0,true,true);  // left wall
            pc.wallPotential.setLongWall(0,false,true); // right wall
            // skip top wall
            pc.wallPotential.setLongWall(1,false,false);// bottom wall
            pc.wallPotential.setPhase(pc.phase);  // so it has a boundary

            
            SimulationGraphic sg = new SimulationGraphic(pc);
            getContentPane().add(sg.panel());
            Iterator displayIterator = sg.displayList().iterator();
            while (displayIterator.hasNext()) {
                Display display = (Display)displayIterator.next();
                if (display instanceof DisplayPhase) {
                    ((DisplayPhase)display).canvas.setDrawBoundary(DisplayCanvasInterface.DRAW_BOUNDARY_NONE);
                    ((DisplayPhase)display).addDrawable(pc.pistonPotential);
                    ((DisplayPhase)display).addDrawable(pc.wallPotential);
                    break;
                }
            }
            
            meterCycles = new DataSourceCountSteps(pc.integrator);
            displayCycles = new DisplayBox(meterCycles);
            pc.integrator.addIntervalListener(displayCycles);
            
            sg.panel().add(displayCycles.graphic());
            sg.panel().setBackground(java.awt.Color.yellow);

            //part unique to this class
            thermometer = new MeterTemperature();
            thermometer.setPhase(new Phase[]{pc.phase});
            DisplayBox tBox = new DisplayBox();
            pc.integrator.addIntervalListener(tBox);
            tBox.setDataSource(thermometer);
            tBox.setUnit(new PrefixedUnit(Kelvin.UNIT));
            sg.panel().add(tBox.graphic());
            
            DisplayTimer timer = new DisplayTimer(pc.integrator);
            pc.integrator.addIntervalListener(timer);
            timer.setUpdateInterval(10);
            sg.panel().add(timer.graphic());

/*            DeviceSlider pressureSlider = new DeviceSlider((SpeciesPistonCylinder.PistonPressureField)phase.firstField(),"pressure");
            sg.panel().add(pressureSlider.graphic());
            pressureSlider.setUnit(new PrefixedUnit(Bar.UNIT));
            pressureSlider.setMinimum(50);
            pressureSlider.setMaximum(1000);*/
        }
}
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        PistonCylinder sim = new PistonCylinder(3);
        sim.ai.setMaxSteps(50000);
        sim.integrator.setTimeStep(20.0);
        Default.BLOCK_SIZE=1000;

        MeterPressureHard pMeter = new MeterPressureHard(sim.integrator);
        pMeter.setPhase(new Phase[]{sim.phase});
        AccumulatorAverage pAcc = new AccumulatorAverage();
        DataManager pMan = new DataManager(pMeter,new DataSink[]{pAcc});
        pMan.setUpdateInterval(10);
        sim.integrator.addIntervalListener(pMan);
        
        MeterPistonDensity dMeter = new MeterPistonDensity(sim.pistonPotential,1);
        dMeter.setPhase(new Phase[]{sim.phase});
        AccumulatorAverage dAcc = new AccumulatorAverage();
        DataManager dMan = new DataManager(dMeter,new DataSink[]{dAcc});
        dMan.setUpdateInterval(10);
        sim.integrator.addIntervalListener(dMan);
        
        System.out.println("density (mol/L) = "+dMeter.getDataAsScalar(sim.phase)*10000/6.0221367);
      
        sim.getController().actionPerformed();
        
        System.out.println("density average "+dAcc.getData(AccumulatorAverage.AVERAGE)[0]*10000/6.0221367+" +/- "
                +dAcc.getData(AccumulatorAverage.ERROR)[0]*10000/6.0221367);
        System.out.println("Z="+pAcc.getData(AccumulatorAverage.AVERAGE)[0]+" +/- "
                +pAcc.getData(AccumulatorAverage.ERROR)[0]);

   }
    
    private static class MeterPistonDensity extends MeterDensity {
        public MeterPistonDensity(P1HardMovingBoundary potential, int wallDim) {
            super();
            pistonPotential = potential;
            wallD = wallDim;
        }
        
        public double getDataAsScalar(Phase p) {
            double totDensity = super.getDataAsScalar(p);
            double d = p.boundary().dimensions().x(wallD);
            return totDensity * ((d-pistonPotential.getWallPosition())/d);
        }
        
        private P1HardMovingBoundary pistonPotential;
        private int wallD;
    }
}
