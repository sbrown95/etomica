/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.mappedDensity.mappedDensityfromlatticesite;

import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomType;
import etomica.atom.IAtom;
import etomica.box.Box;
import etomica.data.*;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataFunction;
import etomica.data.types.DataGroup;
import etomica.graphics.ColorScheme;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicFcc;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.liquidLJ.DataProcessorReweight;
import etomica.liquidLJ.DataProcessorReweightRatio;
import etomica.liquidLJ.Potential2SoftSphericalLSMultiLat;
import etomica.liquidLJ.ValueCache;
import etomica.mappedDensity.FunctionPhilnparabolicpfouriersum;
import etomica.math.function.FunctionDifferentiable;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.BasisBigCell;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.MCMoveAtomCoupled;
import etomica.normalmode.MeterSolidDACut;
import etomica.potential.*;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.util.random.RandomMersenneTwister;

import java.awt.*;
import java.util.Arrays;


public class MappedDensityfromlattice extends Simulation {

    public final CoordinateDefinitionLeaf coordinateDefinition;
    public IntegratorMC integrator;
    public ActivityIntegrate activityIntegrate;
    public Box box;
    public Boundary boundary;
    public int[] nCells;
    public Basis basis;
    public Primitive primitive;
    public MCMoveAtomCoupled atomMove;
    public PotentialMasterList potentialMaster;
    public Potential2SoftSpherical potential;
    public SpeciesSpheresMono species;
    public MappedDensityfromlattice(Space _space, int numAtoms, double density, double temperature, double rc, boolean ss, int[] seeds) {
        super(_space);
        if (seeds != null) {
            setRandom(new RandomMersenneTwister(seeds));
        }
        species = new SpeciesSpheresMono(this, space);
        addSpecies(species);

        potentialMaster = new PotentialMasterList(this, space);

        // TARGET
        double L = Math.pow(4.0 / density, 1.0 / 3.0);
        int n = (int) Math.round(Math.pow(numAtoms / 4, 1.0 / 3.0));
        boundary = new BoundaryRectangularPeriodic(space, n * L);
        box = this.makeBox(boundary);
        box.setNMolecules(species, numAtoms);

        integrator = new IntegratorMC(potentialMaster, getRandom(), temperature, box);
        MeterPotentialEnergy meterPE = new MeterPotentialEnergy(potentialMaster, box);
        atomMove = new MCMoveAtomCoupled(potentialMaster, meterPE, getRandom(), space);
        atomMove.setStepSize(0.1);
        atomMove.setStepSizeMax(0.5);
        atomMove.setDoExcludeNonNeighbors(true);
        integrator.getMoveManager().addMCMove(atomMove);
//        ((MCMoveStepTracker)atomMove.getTracker()).setNoisyAdjustment(true);

        primitive = new PrimitiveCubic(space, n * L);

        nCells = new int[]{n, n, n};
        Basis basisFCC = new BasisCubicFcc();
        basis = new BasisBigCell(space, basisFCC, nCells);

        coordinateDefinition = new CoordinateDefinitionLeaf(box, primitive, basis, space);
        coordinateDefinition.initializeCoordinates(new int[]{1, 1, 1});

        potential = ss ? new P2SoftSphere(space, 1.0, 4.0, 12) : new P2LennardJones(space, 1.0, 1.0);
        potential = new P2SoftSphericalTruncated(space, potential, rc);
        atomMove.setPotential(potential);
        AtomType sphereType = species.getLeafType();
        potentialMaster.addPotential(potential, new AtomType[]{sphereType, sphereType});

        potentialMaster.lrcMaster().setEnabled(false);

        int cellRange = 2;
        potentialMaster.setRange(rc);
        potentialMaster.setCellRange(cellRange); // NeighborCellManager handles this even if cells are a bit small
        // find neighbors now.  Don't hook up NeighborListManager (neighbors won't change)
        potentialMaster.getNeighborManager(box).reset();
        int potentialCells = potentialMaster.getNbrCellManager(box).getLattice().getSize()[0];
        if (potentialCells < cellRange * 2 + 1) {
            throw new RuntimeException("oops (" + potentialCells + " < " + (cellRange * 2 + 1) + ")");
        }

        activityIntegrate = new ActivityIntegrate(integrator);

        getController().addAction(activityIntegrate);

        // extend potential range, so that atoms that move outside the truncation range will still interact
        // atoms that move in will not interact since they won't be neighbors
        ((P2SoftSphericalTruncated) potential).setTruncationRadius(0.6 * boundary.getBoxSize().getX(0));
    }

    /**
     * @param args filename containing simulation parameters
     */
    public static void main(String[] args) {
        //set up simulation parameters
        SimOverlapParam params = new SimOverlapParam();
        if (args.length == 0) {
            params.rcMax1 = 3;
            params.rcMax0 = 3;
            params.rc = 3;
            params.rc0 = 3;
            params.bpharm = new double[]{9.550752087386252e+00,9.554899656911383e+00,9.557975701182272e+00,9.561039289571333e+00,9.561785691168332e+00,9.562084920108349e+00,9.562184015777641e+00,9.562223770855450e+00,9.562237600652669e+00}; //500
            params.bpharmLJ = new double[]{1.361085875265710e+00,1.362422294066396e+00,1.363399142959180e+00,1.364383687422787e+00,1.364621191334029e+00,1.364711705394565e+00,1.364747826183867e+00,1.364760708535937e+00,1.364768368160011e+00}; //500
            params.ss = false;
        }
        else {
            ParseArgs.doParseArgs(params, args);
        }
        boolean ss = params.ss;
        double density = params.density;
        long numSteps = params.numSteps;
        final int numAtoms = params.numAtoms;
        double temperature = params.temperature;
        double rc = params.rc;
        double rc0 = params.rc0;
        double rcMax0 = params.rcMax0;
        double rcMax1 = params.rcMax1;
        if (rcMax1 > rcMax0) rcMax1 = rcMax0;
//        double[] bpharm = params.bpharm;
        double[] bpharmLJ = params.bpharmLJ;
        int[] seeds = params.randomSeeds;

        System.out.println(numAtoms+" atoms at density "+density+" and temperature "+temperature);
        System.out.println(numSteps+" steps");

        if(params.temperature==0.1 && params.density==1) {params.msd=0.00444872;}
        if(params.temperature==0.2 && params.density==1) {params.msd= 0.00882799;}
        if(params.temperature==0.3 && params.density==1) {params.msd=0.0124214 ;}
        if(params.temperature==0.4 && params.density==1) {params.msd=0.0176487 ;}
        if(params.temperature==0.5 && params.density==1) {params.msd= 0.0206668;}
        if(params.temperature==0.6 && params.density==1) {params.msd= 0.0246517;}
        if(params.temperature==0.7 && params.density==1) {params.msd= 0.0298408;}
        if(params.temperature==0.8 && params.density==1) {params.msd= 0.0337209;}
        if(params.temperature==0.9 && params.density==1) {params.msd= 0.0371567;}
        if(params.temperature==1.0 && params.density==1) {params.msd= 0.0413929;}
        if(params.temperature==0.55555556 && params.density==1.29) {params.msd=0.00551846 ;}
        if(params.temperature==1.11111111 && params.density==1.29) {params.msd=0.011446 ;}
        if(params.temperature==1.66666667 && params.density==1.29) {params.msd=0.0168222 ;}
        if(params.temperature==2.22222222 && params.density==1.29) {params.msd=0.0225697 ;}
        if(params.temperature==2.77777778 && params.density==1.29) {params.msd=0.0277941 ;}
        if(params.temperature==3.33333333 && params.density==1.29) {params.msd=0.0346079 ;}
        if(params.temperature==3.88888889 && params.density==1.29) {params.msd=0.0391218 ;}
        if(params.temperature==4.22222222 && params.density==1.29) {params.msd=0.0434448 ;}
        if(params.temperature==1.0 && params.density==3.16) {params.msd= 0.00012008;}
        if(params.temperature==1.0 && params.density==2.23) {params.msd= 0.000636781;}
        if(params.temperature==1.0 && params.density==1.58) {params.msd= 0.0034778;}
        if(params.temperature==1.0 && params.density==1.29) {params.msd= 0.00988742;}


        System.out.println(params.msd+" =msd here");

        //instantiate simulation
        final MappedDensityfromlattice sim = new MappedDensityfromlattice(Space.getInstance(3), numAtoms, density, temperature, rc*Math.pow(density, -1.0/3.0), ss, seeds);
        if (seeds == null) {
            seeds = ((RandomMersenneTwister)sim.getRandom()).getSeedArray();
        }
        System.out.println("Random seeds: "+Arrays.toString(seeds));
        if (false) {
            SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE);
            simGraphic.setPaintInterval(sim.box, 1000);
            ColorScheme colorScheme = new ColorScheme() {
                protected Color[] allColors;

                public Color getAtomColor(IAtom a) {
                    if (allColors==null) {
                        allColors = new Color[768];
                        for (int i=0; i<256; i++) {
                            allColors[i] = new Color(255-i,i,0);
                        }
                        for (int i=0; i<256; i++) {
                            allColors[i+256] = new Color(0,255-i,i);
                        }
                        for (int i=0; i<256; i++) {
                            allColors[i+512] = new Color(i,0,255-i);
                        }
                    }
                    return allColors[(2*a.getLeafIndex()) % 768];
                }
            };
            simGraphic.getDisplayBox(sim.box).setColorScheme(colorScheme);

            DisplayTextBox timer = new DisplayTextBox();
            DataSourceCountSteps counter = new DataSourceCountSteps(sim.integrator);
            DataPumpListener counterPump = new DataPumpListener(counter, timer, 100);
            sim.integrator.getEventManager().addListener(counterPump);
            simGraphic.getPanel().controlPanel.add(timer.graphic());

            simGraphic.makeAndDisplayFrame((ss?"SS":"LJ")+" FCC");

            return;
        }

        //start simulation

        double L = Math.pow(numAtoms, 1.0/3.0);
        if (rcMax1 > 0.494*L) rcMax1 = 0.494*L;
        double delta = 0.5;
        int nCutoffs = 1;
        double c = rc0;
        for (nCutoffs=1; c<=rcMax1*1.0001; nCutoffs++) {
            c += delta;
            if (nCutoffs%2==0) delta += 0.5;
        }
        nCutoffs--;
        delta = 0.5;
        double[] cutoffs = new double[nCutoffs];
        cutoffs[0] = rc0;
        for (int i=1; i<nCutoffs; i++) {
            cutoffs[i] = cutoffs[i-1] + delta;
            if (i%2==0) delta += 0.5;
        }
        for (int i=0; i<nCutoffs; i++) {
            cutoffs[i] *= Math.pow(density, -1.0/3.0);
        }

        PotentialMasterList potentialMasterData;
        Potential2SoftSpherical potential = ss ? new P2SoftSphere(sim.getSpace(), 1.0, 4.0, 12) : new P2LennardJones(sim.getSpace(), 1.0, 1.0);
        {
            // |potential| is our local potential used for data collection.
            potentialMasterData = new PotentialMasterList(sim, cutoffs[nCutoffs-1], sim.getSpace());
            P2SoftSphericalTruncated potentialT = new P2SoftSphericalTruncated(sim.getSpace(), potential, cutoffs[nCutoffs-1]-0.01);
            AtomType sphereType = sim.species.getLeafType();
            potentialMasterData.addPotential(potentialT, new AtomType[]{sphereType, sphereType});
            potentialMasterData.lrcMaster().setEnabled(false);

            int cellRange = 2;
            potentialMasterData.setCellRange(cellRange);
            // find neighbors now.  Don't hook up NeighborListManager (neighbors won't change)
            potentialMasterData.getNeighborManager(sim.box).reset();
            int potentialCells = potentialMasterData.getNbrCellManager(sim.box).getLattice().getSize()[0];
            if (potentialCells < cellRange*2+1) {
                throw new RuntimeException("oops ("+potentialCells+" < "+(cellRange*2+1)+")");
            }

            // extend potential range, so that atoms that move outside the truncation range will still interact
            // atoms that move in will not interact since they won't be neighbors
            potentialT.setTruncationRadius(0.6*sim.box.getBoundary().getBoxSize().getX(0));
        }

        PotentialMasterList potentialMasterDataLJ = null;
        P2LennardJones p2LJ = null;
        Potential2SoftSpherical potentialLJ = null;
        if (ss) {
            // |potential| is our local potential used for data collection.
            potentialMasterDataLJ = new PotentialMasterList(sim, cutoffs[nCutoffs-1], sim.getSpace());
            p2LJ = new P2LennardJones(sim.getSpace());

            potentialLJ = new P2SoftSphericalTruncated(sim.getSpace(), p2LJ, cutoffs[nCutoffs-1]-0.01);
            AtomType sphereType = sim.species.getLeafType();
            potentialMasterDataLJ.addPotential(potentialLJ, new AtomType[]{sphereType, sphereType});
            potentialMasterDataLJ.lrcMaster().setEnabled(false);

            int cellRange = 2;
            potentialMasterDataLJ.setCellRange(cellRange);
            // find neighbors now.  Don't hook up NeighborListManager (neighbors won't change)
            potentialMasterDataLJ.getNeighborManager(sim.box).reset();
            int potentialCells = potentialMasterDataLJ.getNbrCellManager(sim.box).getLattice().getSize()[0];
            if (potentialCells < cellRange*2+1) {
                throw new RuntimeException("oops ("+potentialCells+" < "+(cellRange*2+1)+")");
            }

            // extend potential range, so that atoms that move outside the truncation range will still interact
            // atoms that move in will not interact since they won't be neighbors
            ((P2SoftSphericalTruncated)potentialLJ).setTruncationRadius(0.6*sim.box.getBoundary().getBoxSize().getX(0));
        }

        // meter needs lattice energy, so make it now


        double rcMaxLS = 3*0.494*L;
        if (rcMaxLS>rcMax0) rcMaxLS = rcMax0;
        if (rcMax1 >= rcMax0) rcMaxLS=0;

        delta = 0.5;
        int nCutoffsLS = 1;
        c = rc0;
        for (nCutoffsLS=1; c<rcMaxLS*1.0001; nCutoffsLS++) {
            c += delta;
            if (nCutoffsLS%2==0) delta += 0.5;
        }
        nCutoffsLS--;

        final double[] cutoffsLS = new double[nCutoffsLS];
        PotentialMasterMonatomic potentialMasterLS = new PotentialMasterMonatomic(sim);
        Potential2SoftSphericalLSMultiLat pLS = null;
        PotentialMasterMonatomic potentialMasterLJLS = null;
        Potential2SoftSphericalLSMultiLat pLJLS = null;
        final double[] uFacCutLS = new double[cutoffsLS.length];
        MeterSolidDACut meterSolidLS = null;

        if (args.length == 0) {
            // quick initialization
            sim.initialize(numSteps/10);
        }
        else {
            long nSteps = numSteps/20 + 50*numAtoms + numAtoms*numAtoms*3;
            if (nSteps > numSteps/2) nSteps = numSteps/2;
            sim.initialize(nSteps);
        }

        MeterConventional meterConventional = new MeterConventional(params.msd,sim.box, sim.coordinateDefinition);
        long steps = params.numSteps;
        int interval = 5* params.numAtoms;
        int blocks = 100;
        long blockSize = steps / (interval * blocks);
        meterConventional.getXDataSource().setNValues(params.bins);  //con bins=1000
        meterConventional.reset();
        AccumulatorAverageFixed accCon = new AccumulatorAverageFixed(blockSize);
        DataPumpListener pumpCon = new DataPumpListener(meterConventional, accCon, interval);
        sim.getIntegrator().getEventManager().addListener(pumpCon);

//do for mapped
        FunctionDifferentiable f;
       f = new Function(params.msd);
   //     f = new FunctionUniform(params.msd);

        MeterMappedAvg meterMappedAvg = new MeterMappedAvg(params.msd,sim.box(), sim.potentialMaster, params.temperature, f, sim.coordinateDefinition);
        meterMappedAvg.getXDataSource().setNValues(params.bins);  //map bins=1000
        meterMappedAvg.reset();
        AccumulatorAverageFixed accMappedAvg = new AccumulatorAverageFixed(blockSize);
        DataPumpListener pumpMappedAvg = new DataPumpListener(meterMappedAvg, accMappedAvg, interval);
        sim.getIntegrator().getEventManager().addListener(pumpMappedAvg);

        int numBlocks = 100;
         int intervalLS = 5*interval;
         if (blockSize == 0) blockSize = 1;
        long blockSizeLS = numSteps/(numBlocks*intervalLS);
        if (blockSizeLS == 0) blockSizeLS = 1;
        int o=2;
        while (blockSize<numSteps/5 && (numSteps != numBlocks*intervalLS*blockSizeLS || numSteps != numBlocks*interval*blockSize)) {
            interval = 2*numAtoms+(o%2==0 ? (o/2) : -(o/2));
            if (interval < 1 || interval > numSteps/5) {
                throw new RuntimeException("oops interval "+interval);
            }
            // only need to enforce intervalLS if nCutoffsLS>0.  whatever.
            intervalLS = 5*interval;
            blockSize = numSteps/(numBlocks*interval);
            if (blockSize == 0) blockSize = 1;
            blockSizeLS = numSteps/(numBlocks*intervalLS);
            if (blockSizeLS == 0) blockSizeLS = 1;
            o++;
        }
        if (numSteps != numBlocks*intervalLS*blockSizeLS || numSteps != numBlocks*interval*blockSize) {
            throw new RuntimeException("unable to find appropriate intervals");
        }
 //       System.out.println("block size "+blockSize+" interval "+interval);

        final long startTime = System.currentTimeMillis();

        sim.activityIntegrate.setMaxSteps(numSteps);

        sim.getController().actionPerformed();
        long endTime = System.currentTimeMillis();

        IData data =  accCon.getData(accCon.AVERAGE);
        IData dataunc =  accCon.getData(accCon.ERROR);
        IData dataMappedAvg =  accMappedAvg.getData(accMappedAvg.AVERAGE);
        IData dataMappedAvgunc =  accMappedAvg.getData(accMappedAvg.ERROR);

        IData rdata= ((DataFunction.DataInfoFunction)((DataGroup.DataInfoGroup)accCon.getDataInfo()).getSubDataInfo(0)).getXDataSource().getIndependentData(0);
        for (int i=0;i<rdata.getLength();i++){
            System.out.println(rdata.getValue(i)+" "+data.getValue(i)+" "+dataunc.getValue(i)+" "+dataMappedAvg.getValue(i)+" "+dataMappedAvgunc.getValue(i));
        }

        System.out.println("time: " + (endTime - startTime)/1000.0);
    }

    public void initialize(long initSteps) {
        // equilibrate off the lattice to avoid anomalous contributions
        activityIntegrate.setMaxSteps(initSteps);
        getController().actionPerformed();
        getController().reset();
        integrator.getMoveManager().setEquilibrating(false);
    }


    
    /**
     * Inner class for parameters understood by the HSMD3D constructor
     */
    public static class SimOverlapParam extends ParameterBase {
        public int numAtoms = 500;
        public double msd = 0.00444872;
        public int bins = 1000;
        public double density = 1;
        public long numSteps = 50000000;
        public double temperature = 1;
        public double rc = 3;
        public double rc0 = rc;
        public double rcMax1 = 3;
        public double rcMax0 = 3;
        public double[] bpharm = new double[0];
        public double[] bpharmLJ = new double[0];
        public boolean ss = false;
        public int[] randomSeeds = null;
    }
}
