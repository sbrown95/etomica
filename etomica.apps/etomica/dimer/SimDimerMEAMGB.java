package etomica.dimer;

import etomica.action.CalcVibrationalModes;
import etomica.action.WriteConfiguration;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomArrayList;
import etomica.box.Box;
import etomica.chem.elements.Tin;
import etomica.config.ConfigurationFile;
import etomica.config.GrainBoundaryTiltConfiguration;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.BravaisLatticeCrystal;
import etomica.lattice.crystal.BasisBetaSnA5;
import etomica.lattice.crystal.PrimitiveTetragonal;
import etomica.meam.ParameterSetMEAM;
import etomica.meam.PotentialMEAM;
import etomica.nbr.CriterionSimple;
import etomica.nbr.CriterionTypesCombination;
import etomica.nbr.list.PotentialMasterList;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.space3d.Vector3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.RandomNumberGenerator;
import etomica.util.numerical.CalcGradientDifferentiable;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom of Sn on a surface, modeled with MEAM.
 * 
 * @author msellers
 *
 */

public class SimDimerMEAMGB extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerMEAMadatomSn";
    public final PotentialMasterList potentialMaster;
    public final PotentialMasterListDimer potentialMasterD;
    public IntegratorVelocityVerlet integratorMD;
    public IntegratorDimerRT integratorDimer;
    public IntegratorDimerMin integratorDimerMin;
    public IBox box;
    public double clusterRadius;
    public IVector [] saddle;
    public SpeciesSpheresMono fixed, movable, dimer;
    public PotentialMEAM potential;
    public PotentialCalculationForcePressureSumGB pcGB;
    public ActivityIntegrate activityIntegrateMD, activityIntegrateDimer, activityIntegrateMin;
    public CalcGradientDifferentiable calcGradientDifferentiable;
    public CalcVibrationalModes calcVibrationalModes;
    public double [][] dForces;
    public int [] d, modeSigns;
    public double [] positions;
    public double [] lambdas, frequencies;
    public IVector adAtomPos;
    public IAtomSet movableSet;
    public int [] millerPlane;
    

    
    public SimDimerMEAMGB(int[] amillerPlane, int[] boxSize) {
    	super(Space3D.getInstance(), true);
    	
    	this.millerPlane = amillerPlane;
    	potentialMaster = new PotentialMasterList(this, space);
    	potentialMasterD = new PotentialMasterListDimer(this, space);
        
      //SIMULATION BOX
        box = new Box(new BoundaryRectangularSlit(random, 2, 5, space), space);
        addBox(box);
     
      //SPECIES
        
        //Sn
        Tin tinFixed = new Tin("SnFix", Double.POSITIVE_INFINITY);
        Tin dimerTin = new Tin("SnD", 118.710);
        fixed = new SpeciesSpheresMono(this, space, tinFixed);
        movable = new SpeciesSpheresMono(this, space, Tin.INSTANCE);
        dimer = new SpeciesSpheresMono(this, space, dimerTin);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        getSpeciesManager().addSpecies(dimer);
        ((IAtomTypeSphere)fixed.getLeafType()).setDiameter(3.022); 
        ((IAtomTypeSphere)movable.getLeafType()).setDiameter(3.022);
        ((IAtomTypeSphere)dimer.getLeafType()).setDiameter(3.022);
        potential = new PotentialMEAM(space);
        potential.setParameters(fixed.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(movable.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(dimer.getLeafType(), ParameterSetMEAM.Sn);
        
        
        
        //Sn
        //beta-Sn box
        
        //The dimensions of the simulation box must be proportional to those of
        //the unit cell to prevent distortion of the lattice.  The values for the 
        //lattice parameters for tin's beta box (a = 5.8314 angstroms, c = 3.1815 
        //angstroms) are taken from the ASM Handbook. 
              
        double a = 5.92; 
        double c = 3.23;
        PrimitiveTetragonal primitive = new PrimitiveTetragonal(space, a, c);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisBetaSnA5());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new ISpecies[] {fixed, movable}, potential.getRange(), space);
            
        
        //Ag
        /**
        Silver silverFixed = new Silver("AgFix", Double.POSITIVE_INFINITY);
        fixed = new SpeciesSpheresMono(this, Silver.INSTANCE);
        movable = new SpeciesSpheresMono(this, Silver.INSTANCE);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(2.8895); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(2.8895);
        potential = new PotentialMEAM(space);
        potential.setParameters(agFix, ParameterSetMEAM.Ag);
        potential.setParameters(ag, ParameterSetMEAM.Ag);
        potential.setParameters(agAdatom, ParameterSetMEAM.Ag);
        potential.setParameters(movable, ParameterSetMEAM.Ag);
        
        double a = 4.0863;
        PrimitiveCubic primitive = new PrimitiveCubic(space, a);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new ISpecies[] {fixed, movable}, 4.56, space);

        */
    
        
        //Cu
       /**
        //Copper copperFixed = new Copper("CuFix", Double.POSITIVE_INFINITY);
        fixed = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        movable = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        dimer = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        getSpeciesManager().addSpecies(dimer);
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(2.5561); 
        ((AtomTypeSphere)dimer.getLeafType()).setDiameter(2.5561); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(2.5561);
        potential = new PotentialMEAM(space);
        potential.setParameters(fixed.getLeafType(), ParameterSetMEAM.Cu);
        potential.setParameters(movable.getLeafType(), ParameterSetMEAM.Cu);
        potential.setParameters(dimer.getLeafType(), ParameterSetMEAM.Cu);
        
        double a = 3.6148;
        PrimitiveCubic primitive = new PrimitiveCubic(space, a);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new ISpecies[] {fixed, movable}, potential.getRange(), space);
       */
        
        this.potentialMaster.addPotential(potential, new IAtomTypeLeaf[]{fixed.getLeafType(), movable.getLeafType(), dimer.getLeafType()});
        potentialMaster.setRange(potential.getRange()*1.1);
        CriterionSimple criteria = new CriterionSimple(this, space, potential.getRange(), potential.getRange()*1.1);
        potentialMaster.setCriterion(potential, new CriterionTypesCombination(criteria, new IAtomTypeLeaf[] {fixed.getLeafType(), movable.getLeafType(), dimer.getLeafType()}));
        
        this.potentialMasterD.addPotential(potential, new IAtomTypeLeaf[]{movable.getLeafType(), dimer.getLeafType()});
        potentialMasterD.setSpecies(new ISpecies []{dimer, movable});
        potentialMasterD.setRange(potential.getRange()*1.1);
        CriterionSimple criteria2 = new CriterionSimple(this, space, potential.getRange(), potential.getRange()*1.1);
        potentialMasterD.setCriterion(potential, new CriterionTypesCombination(criteria2, new IAtomTypeLeaf[] {movable.getLeafType(), dimer.getLeafType()}));
        
        gbtilt.setFixedSpecies(fixed);
        gbtilt.setMobileSpecies(movable);

        gbtilt.setGBplane(millerPlane);
        gbtilt.setBoxSize(box, boxSize);
        gbtilt.initializeCoordinates(box);
               
        IVector newBoxLength = space.makeVector();
        newBoxLength.E(box.getBoundary().getDimensions());
        newBoxLength.setX(2,newBoxLength.x(2)+1.0);
        newBoxLength.setX(1,newBoxLength.x(1)+0.0001);
        newBoxLength.setX(0,newBoxLength.x(0)+0.0001);
        box.getBoundary().setDimensions(newBoxLength);
        
        
    }
    
    public void setMovableAtomsSphere(double distance, IVector center){
        distance = distance*distance;
        IVector rij = space.makeVector();
        AtomArrayList movableList = new AtomArrayList();
        IAtomSet loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getAtomCount(); i++){
            if(((IMolecule)loopSet.getAtom(i)).getType()==fixed){continue;}
        	rij.E(((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            rij.Ev1Mv2(center, rij);
            box.getBoundary().nearestImage(rij);
            if(rij.squared()<distance){//Math.abs(rij.x(0)) < 0.5 && Math.abs(rij.x(1)) < distance && Math.abs(rij.x(2)) < distance){
               movableList.add(loopSet.getAtom(i));
            } 
        }
        for (int i=0; i<movableList.getAtomCount(); i++){
            IMolecule newMolecule = dimer.makeMolecule();
            box.addMolecule(newMolecule);
           ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)movableList.getAtom(i)).getChildList().getAtom(0)).getPosition());
           box.removeMolecule((IMolecule)movableList.getAtom(i));
        }
        movableSet = box.getMoleculeList(dimer);
    }
    
    public void setMovableAtomsCube(IVector dimensions, IVector center){
        IVector cube = dimensions;
        IVector rij = space.makeVector();
        AtomArrayList movableList = new AtomArrayList();
        IAtomSet loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getAtomCount(); i++){
        	if(((IMolecule)loopSet.getAtom(i)).getType()==fixed){continue;}
            rij.E(((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            rij.Ev1Mv2(center, rij);
            box.getBoundary().nearestImage(rij);
            if(Math.abs(rij.x(0)) < cube.x(0) && Math.abs(rij.x(1)) < cube.x(1) && Math.abs(rij.x(2)) < cube.x(2)){
               movableList.add(loopSet.getAtom(i));
            } 
        }
        for (int i=0; i<movableList.getAtomCount(); i++){
            IMolecule newMolecule = dimer.makeMolecule();
            box.addMolecule(newMolecule);
           ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)movableList.getAtom(i)).getChildList().getAtom(0)).getPosition());
           box.removeMolecule((IMolecule)movableList.getAtom(i));
        }
        movableSet = box.getMoleculeList(dimer);
    }
    
    public void setMovableAtomsList(){
        AtomArrayList neighborList = new AtomArrayList();
        AtomArrayList fixedList = new AtomArrayList();
        IAtomSet loopSet = box.getMoleculeList();
        IAtomSet dimerSet = box.getMoleculeList(dimer);
        for(int i=0; i<loopSet.getAtomCount(); i++){
            if(((IMolecule)loopSet.getAtom(i)).getType()==dimer){
                continue;
            }
            boolean fixedFlag = true;
            for(int j=0; j<dimerSet.getAtomCount(); j++){
                IVector dist = space.makeVector();
                dist.Ev1Mv2(((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition(),((IAtomPositioned)((IMolecule)dimerSet.getAtom(j)).getChildList().getAtom(0)).getPosition());
                box.getBoundary().nearestImage(dist);
                if(Math.sqrt(dist.squared())<potentialMasterD.getMaxPotentialRange()+2.0){
                    neighborList.add(loopSet.getAtom(i));
                    fixedFlag = false;
                    break;
                }               
            
            }
            if(fixedFlag){
                fixedList.add(loopSet.getAtom(i));
            }
        }
        for (int i=0; i<neighborList.getAtomCount(); i++){
            IMolecule newMolecule = movable.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)neighborList.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.removeMolecule((IMolecule)neighborList.getAtom(i));
         }
        for (int i=0; i<fixedList.getAtomCount(); i++){
            IMolecule newMolecule = fixed.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)fixedList.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.removeMolecule((IMolecule)fixedList.getAtom(i));
         }
        
    }
    
    //Must be run after setMovableAtoms
    public void removeAtoms(double distance, IVector center){
        distance = distance*distance;
        int rmvCount = 0;
        IVector rij = space.makeVector();
        //movable species
        IAtomSet loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getAtomCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule((IMolecule)loopSet.getAtom(i));
               rmvCount++;
            } 
        }
        
        //dimer species
        IAtomSet loopSet2 = box.getMoleculeList(dimer);
        for (int i=0; i<loopSet2.getAtomCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)((IMolecule)loopSet2.getAtom(i)).getChildList().getAtom(0)).getPosition());
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule((IMolecule)loopSet2.getAtom(i));
               rmvCount++;
            } 
        }
        System.out.println(rmvCount+" atoms removed.");
    }
    
    public void initializeConfiguration(String fileName){
        ConfigurationFile config = new ConfigurationFile(fileName);
        config.initializeCoordinates(box);
    }
    
    public void generateConfigs(String fileName, double percentd){       
        
        RandomNumberGenerator random = new RandomNumberGenerator();
        IVector workVector = space.makeVector();
        IVector [] currentPos = new IVector [movableSet.getAtomCount()];
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i].E(((IAtomPositioned)((IMolecule)movableSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
        }
        
        //Create multiple configurations
        for(int m=0; m<50; m++){
            WriteConfiguration genConfig = new WriteConfiguration(space);
            genConfig.setBox(box);
            genConfig.setConfName(fileName+"_config_"+m);
            //Displaces atom's by at most +/-0.03 in each coordinate
            for(int i=0; i<movableSet.getAtomCount(); i++){
                IVector atomPosition = ((IAtomPositioned)((IMolecule)movableSet.getAtom(i)).getChildList().getAtom(0)).getPosition();
                for(int j=0; j<3; j++){
                    workVector.setX(j,percentd*random.nextGaussian());
                }
                atomPosition.Ev1Pv2(currentPos[i],workVector);
            }
            genConfig.actionPerformed();            
        }
    }
        
    public void enableMolecularDynamics(long maxSteps){
        integratorMD = new IntegratorVelocityVerlet(this, potentialMaster, space);
        integratorMD.setTimeStep(0.001);
        integratorMD.setTemperature(Kelvin.UNIT.toSim(100));
        integratorMD.setThermostatInterval(100);
        integratorMD.setIsothermal(true);
        integratorMD.setBox(box);
        //pcGB = new PotentialCalculationForcePressureSumGB(space, box);
        //integratorMD.setForceSum(pcGB);
        integratorMD.addNonintervalListener(potentialMaster.getNeighborManager(box));
        integratorMD.addIntervalAction(potentialMaster.getNeighborManager(box));  
        activityIntegrateMD = new ActivityIntegrate(integratorMD);
        getController().addAction(activityIntegrateMD);
        activityIntegrateMD.setMaxSteps(maxSteps);
    }
    
    public void enableDimerSearch(String fileName, long maxSteps, Boolean orthoSearch, Boolean fine){
        
        integratorDimer = new IntegratorDimerRT(this, potentialMasterD, new ISpecies[]{dimer}, space);
        integratorDimer.setBox(box);
        integratorDimer.setOrtho(orthoSearch, false);
        if(fine){
            ConfigurationFile configFile = new ConfigurationFile(fileName+"_saddle");
            configFile.initializeCoordinates(box);
            
            integratorDimer.setFileName(fileName+"_fine");
            integratorDimer.deltaR = 0.0005;
            integratorDimer.dXl = 10E-5;       
            integratorDimer.deltaXmax = 0.005;
            integratorDimer.dFsq = 0.0001*0.0001;
            integratorDimer.dFrot = 0.01;
        }
        integratorDimer.setFileName(fileName);
        integratorDimer.addNonintervalListener(potentialMasterD.getNeighborManager(box));
        integratorDimer.addIntervalAction(potentialMasterD.getNeighborManager(box));  
        activityIntegrateDimer = new ActivityIntegrate(integratorDimer);
        integratorDimer.setActivityIntegrate(activityIntegrateDimer);
        getController().addAction(activityIntegrateDimer);
        activityIntegrateDimer.setMaxSteps(maxSteps);
    }
        
    public void enableMinimumSearch(String fileName, Boolean normalDir){
        
        integratorDimerMin = new IntegratorDimerMin(this, potentialMasterD, new ISpecies[]{dimer}, normalDir, space);
        integratorDimerMin.setBox(box);
        integratorDimerMin.addNonintervalListener(potentialMasterD.getNeighborManager(box));
        integratorDimerMin.addIntervalAction(potentialMasterD.getNeighborManager(box)); 
        activityIntegrateMin = new ActivityIntegrate(integratorDimerMin);
        integratorDimerMin.setActivityIntegrate(activityIntegrateMin);
        getController().addAction(activityIntegrateMin);
    }
    
    public static void main(String[] args){
        String fileName = "sngb101v-test" ;//args[0];
        //int mdSteps = 10;//Integer.parseInt(args[1]);
        /*
        int h = Integer.parseInt(args[1]);
        int k = Integer.parseInt(args[2]);
        int l = Integer.parseInt(args[3]);
        
        int x = Integer.parseInt(args[4]);
        int y = Integer.parseInt(args[5]);
        int z = Integer.parseInt(args[6]);
        */
        final String APP_NAME = "SimDimerMEAMGBCluster";
        
        final SimDimerMEAMGB sim = new SimDimerMEAMGB(new int[] {2,1,0}, new int[] {2,6,12});
        
        sim.initializeConfiguration("sngb210-2612");
        
        //System.out.println(sim.box.getBoundary().getDimensions().x(0));
        
        
        IVector dimerCenter = sim.getSpace().makeVector();
        dimerCenter.setX(0, sim.box.getBoundary().getDimensions().x(0)/2.0);
        dimerCenter.setX(1, 1.0);
        dimerCenter.setX(2, 0.0);
        IVector cubeSize = sim.getSpace().makeVector();
        cubeSize.setX(0, 6.0);
        cubeSize.setX(1, 8.0);
        cubeSize.setX(2, 8.0);
        
        if(sim.millerPlane[2] == 0){
            dimerCenter.setX(1, sim.box.getBoundary().getDimensions().x(1)/2.0);
            dimerCenter.setX(0, 1.0);
            dimerCenter.setX(2, 0.0);
            cubeSize.setX(0, 6.0);
            cubeSize.setX(1, 8.0);
            cubeSize.setX(2, 8.0);
        }
        
        IAtomSet list = sim.box.getLeafList();
        IVector rij = sim.space.makeVector();
        Vector3D move = new Vector3D(0.0,0.0,5.0);
        Vector3D move2 = new Vector3D(0.0,0.0,10.0);
        move2.PE(sim.box.getBoundary().getDimensions());
        System.out.println("Atoms: "+list.getAtomCount());
        System.out.println("Interface Area: "+move2.x(0)*move2.x(1)+" angstroms");
       
        /*
        sim.box.getBoundary().setDimensions(move2);
        for(int i=0; i<list.getAtomCount(); i++){
        	rij = ((IAtomPositioned)list.getAtom(i)).getPosition();
        	if(rij.x(2)>0.0){rij.PE(move);}
        	else{rij.ME(move);}
        }
        */
        //sim.setMovableAtomsSphere(6.0, dimerCenter);
        sim.setMovableAtomsCube(cubeSize, dimerCenter);
        sim.setMovableAtomsList();
        
        sim.removeAtoms(3.0, dimerCenter);
        /*
        sim.initializeConfiguration(fileName+"_saddle");
        sim.calculateVibrationalModes(fileName+"_saddle");
        sim.initializeConfiguration(fileName+"_A_minimum");
        sim.calculateVibrationalModes(fileName+"_A_minimum");
        sim.initializeConfiguration(fileName+"_B_minimum");
        sim.calculateVibrationalModes(fileName+"_B_minimum");
        */      
        
        //sim.initializeConfiguration("sngb101-d1-00_saddle");
        sim.enableMolecularDynamics(2);
        //sim.enableDimerSearch(fileName, 2000, false, false);
        //sim.integratorDimer.setRotNum(1);
        //sim.enableMinimumSearch("sngb5-r1", true);
        
        /*
        XYZWriter xyzwriter = new XYZWriter(sim.box);
        xyzwriter.setFileName(fileName+"_saddle.xyz");
        xyzwriter.setIsAppend(true);
        sim.integratorDimer.addIntervalAction(xyzwriter);
        sim.integratorDimer.setActionInterval(xyzwriter, 5);
        */
  
        
        /*
        WriteConfiguration writer = new WriteConfiguration(sim.getSpace());
        writer.setBox(sim.box);
        writer.setConfName(fileName);
        sim.integratorMD.addIntervalAction(writer);
        sim.integratorMD.setActionInterval(writer, 10000);
        */
                
        MeterPotentialEnergy energyMeter = new MeterPotentialEnergy(sim.potentialMasterD);
        energyMeter.setBox(sim.box);
        AccumulatorHistory energyAccumulator = new AccumulatorHistory(new HistoryCollapsingAverage());
        AccumulatorAverageCollapsing accumulatorAveragePE = new AccumulatorAverageCollapsing();
        DataPump energyPump = new DataPump(energyMeter,accumulatorAveragePE);       
        accumulatorAveragePE.addDataSink(energyAccumulator, new StatType[]{StatType.MOST_RECENT});
        DisplayPlot plotPE = new DisplayPlot();
        plotPE.setLabel("PE Plot");
        energyAccumulator.setDataSink(plotPE.getDataSet().makeDataSink());
        accumulatorAveragePE.setPushInterval(1);      
        //sim.integratorDimer.addIntervalAction(energyPump);
        //sim.integratorDimer.setActionInterval(energyPump,1);
        
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, 1, sim.space, sim.getController());
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));        
        simGraphic.add(plotPE);
        
        sim.integratorMD.addIntervalAction(simGraphic.getPaintAction(sim.box));
        
        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());

        colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.blue);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.dimer.getLeafType(), java.awt.Color.orange);
        
        simGraphic.makeAndDisplayFrame(APP_NAME);
    }
    
}
