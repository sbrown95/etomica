package etomica.normalmode;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.atom.AtomTypeSphere;
import etomica.box.Box;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorMD;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P1HardPeriodic;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential;
import etomica.potential.PotentialMaster;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformableLattice;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;

/**
 * MD simulation of hard spheres in 1D or 3D with tabulation of the
 * collective-coordinate S-matrix. No graphic display of simulation.
 */
public class SimCalcS extends Simulation {

    public SimCalcS(Space _space, int numAtoms, double density) {
        super(_space, true);
        PotentialMaster potentialMaster = new PotentialMasterMonatomic(this);

        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);

        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, numAtoms);

        integrator = new IntegratorHard(this, potentialMaster, space);
        integrator.setTimeStep(0.04);
        integrator.setTemperature(1.0);

        integrator.setIsothermal(false);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        // activityIntegrate.setMaxSteps(nSteps);

        Potential potential = new P2HardSphere(space, 1.0, false);
        AtomTypeSphere sphereType = (AtomTypeSphere)species.getLeafType();
        potentialMaster.addPotential(potential, new IAtomType[] { sphereType,
                sphereType });

        int nCells;
        Basis basis;
        if (space.D() == 1) {
            primitive = new PrimitiveCubic(space, 1.0/density);
            nCells = numAtoms;
            bdry = new BoundaryRectangularPeriodic(space, numAtoms/density);
            ((IntegratorHard) integrator).setNullPotential(new P1HardPeriodic(space), sphereType);
            basis = new BasisMonatomic(space);
        } else {
            primitive = new PrimitiveCubic(space, 1);
            double v = primitive.unitCell().getVolume();
            primitive.scaleSize(Math.pow(v*density/4,-1.0/3.0));
            nCells = (int)Math.round(Math.pow(numAtoms/4, 1.0/3.0));
            bdry = new BoundaryDeformableLattice(primitive, new int[]{nCells,nCells,nCells});
            basis = new BasisCubicFcc();
        }
        box.setBoundary(bdry);

        coordinateDefinition = new CoordinateDefinitionLeaf(box, primitive, basis, space);
        if (space.D() == 1) {
            coordinateDefinition.initializeCoordinates(new int[]{nCells});
        }
        else {
            coordinateDefinition.initializeCoordinates(new int[]{nCells, nCells, nCells});
        }
        
        integrator.setBox(box);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // defaults
        int D = 3;
        int nA = 32;
        double density = 1.3;
        if (D == 1) {
            nA = 3;
            density = 0.5;
        }

        double simTime = 10000;

        // parse arguments
        if (args.length > 1) {
            density = Double.parseDouble(args[1]);
        }
        if (args.length > 2) {
            simTime = Double.parseDouble(args[2]);
        }
        if (args.length > 3) {
            nA = Integer.parseInt(args[3]);
        }
        String filename = "normal_modes" + D + "D_"+nA+"_"+((int)(density*100))+"_cubic";
        if (args.length > 0) {
            filename = args[0];
        }

        System.out.println("Running "
                + (D == 1 ? "1D" : (D == 3 ? "FCC" : "2D hexagonal"))
                + " hard sphere simulation");
        System.out.println(nA + " atoms at density " + density);
        System.out.println(simTime + " time units");
        System.out.println("output data to " + filename);

        // construct simulation
        SimCalcS sim = new SimCalcS(Space.getInstance(D), nA, density);

        Primitive primitive = sim.primitive;

        // set up normal-mode meter
        MeterNormalMode meterNormalMode = new MeterNormalMode();
        meterNormalMode.setCoordinateDefinition(sim.coordinateDefinition);
        WaveVectorFactory waveVectorFactory;
        if (D == 1) {
            waveVectorFactory = new WaveVectorFactory1D();
        } else if (D == 2) {
            waveVectorFactory = null;
        } else {
            waveVectorFactory = new WaveVectorFactorySimple(primitive, sim.space);
        }
        meterNormalMode.setWaveVectorFactory(waveVectorFactory);
        meterNormalMode.setBox(sim.box);

        IntegratorListenerAction meterListener = new IntegratorListenerAction(meterNormalMode);
        meterListener.setInterval(2);
        sim.integrator.getEventManager().addListener(meterListener);
        
        // MeterMomentumCOM meterCOM = new MeterMomentumCOM(sim.space);
        // MeterPositionCOM meterCOM = new MeterPositionCOM(sim.space);
        // DataSinkConsole console = new DataSinkConsole();
        // DataPump comPump = new DataPump(meterCOM,console);
        // IntervalActionAdapter comAdapter = new
        // IntervalActionAdapter(comPump);
        // sim.integrator.addListener(comAdapter);
        // meterCOM.setBox(sim.box);

        // start simulation
        int nSteps = (int) (simTime / sim.integrator.getTimeStep());
        sim.activityIntegrate.setMaxSteps(nSteps/10);
        sim.getController().actionPerformed();
        System.out.println("equilibration finished");
        meterNormalMode.reset();
        sim.getController().reset();
        
        sim.activityIntegrate.setMaxSteps(nSteps);
        sim.getController().actionPerformed();

        WriteS sWriter = new WriteS(sim.space);
        sWriter.setFilename(filename);
        sWriter.setMeter(meterNormalMode);
        sWriter.setWaveVectorFactory(waveVectorFactory);
        sWriter.setOverwrite(true);
        sWriter.actionPerformed();

    }

    private static final long serialVersionUID = 1L;
    public IntegratorMD integrator;
    public ActivityIntegrate activityIntegrate;
    public IBox box;
    public Boundary bdry;
    public Primitive primitive;
    public CoordinateDefinition coordinateDefinition;
}