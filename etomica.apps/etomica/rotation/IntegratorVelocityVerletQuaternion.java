package etomica.rotation;

import java.awt.Color;
import java.io.Serializable;

import etomica.EtomicaInfo;
import etomica.action.AtomActionTranslateBy;
import etomica.action.MoleculeChildAtomAction;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomKinetic;
import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomType;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.Atom;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomPositionCOM;
import etomica.atom.AtomSetSinglet;
import etomica.atom.IAtomOrientedKinetic;
import etomica.atom.MoleculeAgentManager;
import etomica.atom.OrientationCalcQuaternion;
import etomica.atom.SpeciesAgentManager;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.atom.MoleculeAgentManager.MoleculeAgentSource;
import etomica.atom.iterator.ApiBuilder;
import etomica.atom.iterator.IteratorDirective;
import etomica.box.Box;
import etomica.config.ConfigurationFile;
import etomica.config.ConfigurationLattice;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorMD;
import etomica.integrator.IntegratorRigidMatrixIterative.BoxImposePbcMolecule;
import etomica.lattice.LatticeCubicFcc;
import etomica.listener.IntegratorListenerAction;
import etomica.models.water.OrientationCalcWater3P;
import etomica.models.water.SpeciesWater3P;
import etomica.potential.P2Electrostatic;
import etomica.potential.P2LennardJones;
import etomica.potential.PotentialCalculationForcePressureSum;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialGroup;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularNonperiodic;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space.Tensor;
import etomica.space3d.RotationTensor3D;
import etomica.space3d.Space3D;
import etomica.species.ISpeciesOriented;
import etomica.units.Electron;
import etomica.units.Kelvin;
import etomica.util.Constants;
import etomica.util.Debug;
import etomica.util.Function;

public class IntegratorVelocityVerletQuaternion extends IntegratorMD implements AgentSource, SpeciesAgentManager.AgentSource, MoleculeAgentSource {

    private static final long serialVersionUID = 2L;
    protected PotentialCalculationForceSum forceSum;
    private final IteratorDirective allAtoms;
    protected final Tensor pressureTensor;
    protected final Tensor workTensor;
    protected final RotationTensor3D rotationTensor;
    protected final IVectorMutable xWork;
    protected final SpeciesAgentManager typeAgentManager;
    protected final IVectorMutable angularVelocity;
    protected final double[] quatVelocity, tempQuat;
    protected final AtomPositionCOM atomPositionCOM;
    protected final AtomActionTranslateBy translateBy;
    protected final MoleculeChildAtomAction translator;
    public int printInterval;
    protected final ISimulation sim;

    protected AtomLeafAgentManager leafAgentManager;
    protected MoleculeAgentManager moleculeAgentManager;

    public IntegratorVelocityVerletQuaternion(ISimulation sim, PotentialMaster potentialMaster, Space space) {
        this(sim, potentialMaster, 0.05, 1.0, space);
    }
    
    public IntegratorVelocityVerletQuaternion(ISimulation sim, PotentialMaster potentialMaster,
            double timeStep, double temperature, Space space) {
        super(potentialMaster,sim.getRandom(),timeStep,temperature, space);
        this.sim = sim;
        forceSum = new PotentialCalculationForcePressureSum(space);
        allAtoms = new IteratorDirective();
        // allAtoms is used only for the force calculation, which has no LRC
        // but we're also calculating the pressure tensor, which does have LRC.
        // things deal with this OK.
        allAtoms.setIncludeLrc(true);
        pressureTensor = space.makeTensor();
        workTensor = space.makeTensor();
        rotationTensor = (RotationTensor3D)space.makeRotationTensor();
        xWork = space.makeVector();
        typeAgentManager = new SpeciesAgentManager(this, sim.getSpeciesManager(), sim.getEventManager());
        angularVelocity = space.makeVector();
        quatVelocity = new double[4];
        tempQuat = new double[4];
        atomPositionCOM = new AtomPositionCOM(space);
        translateBy = new AtomActionTranslateBy(space);
        translator = new MoleculeChildAtomAction(translateBy);
        printInterval = 10;
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Molecular dynamics using velocity Verlet integration algorithm");
        return info;
    }
    
    public void setBox(Box p) {
        if (box != null) {
            // allow agentManager to de-register itself as a BoxListener
            leafAgentManager.dispose();
            moleculeAgentManager.dispose();
        }
        super.setBox(p);
        leafAgentManager = new AtomLeafAgentManager(this,p);
        moleculeAgentManager = new MoleculeAgentManager(sim, box, this);
        forceSum.setAgentManager(leafAgentManager);
    }

    public void setOrientationCalc(ISpecies species, OrientationCalcQuaternion calcer) {
        MyTypeAgent typeAgent = new MyTypeAgent(calcer);
        typeAgentManager.setAgent(species, typeAgent);
    }
    
//--------------------------------------------------------------
// steps all particles across time interval tStep

    // assumes one box
    public void doStepInternal() {
        super.doStepInternal();
        double KE = 0;
        double PE = 0;
        if (stepCount%printInterval == 0) PE = meterPE.getDataAsScalar();
        if (Debug.ON && Debug.DEBUG_NOW) {
            IAtomList pair = Debug.getAtoms(box);
            if (pair != null) {
                IVectorMutable dr = space.makeVector();
                dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(), ((IAtomPositioned)pair.getAtom(0)).getPosition());
                System.out.println(pair+" dr "+dr);
            }
        }
        IMoleculeList moleculeList = box.getMoleculeList();
        int nMolecules = moleculeList.getMoleculeCount();
        for (int iMolecule = 0; iMolecule<nMolecules; iMolecule++) {
            IMolecule molecule = moleculeList.getMolecule(iMolecule);
            MyTypeAgent typeAgent = (MyTypeAgent)typeAgentManager.getAgent(molecule.getType());
            IAtomList children = molecule.getChildList();
            if (typeAgent == null) {
                for (int iLeaf=0; iLeaf<children.getAtomCount(); iLeaf++) {
                    IAtomKinetic a = (IAtomKinetic)children.getAtom(iLeaf);
                    AtomAgent agent = (AtomAgent)leafAgentManager.getAgent((IAtom)a);
                    IVectorMutable r = a.getPosition();
                    IVectorMutable v = a.getVelocity();
                    KE += v.squared()*((Atom)a).getType().getMass();
                    if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet((IAtom)a))) {
                        System.out.println("first "+a+" r="+r+", v="+v+", f="+agent.force);
                    }
                    v.PEa1Tv1(0.5*timeStep*((Atom)a).getType().rm(),agent.force);  // p += f(old)*dt/2
                    r.PEa1Tv1(timeStep,v);         // r += p*dt/m
                }
                continue;
            }
            
            MoleculeAgent agent = (MoleculeAgent)moleculeAgentManager.getAgent(molecule);
            IVectorMutable angularMomentum = ((IAtomOrientedKinetic)molecule).getAngularVelocity();
            ISpeciesOriented orientedType = (ISpeciesOriented)molecule.getType();
            IVector moment = orientedType.getMomentOfInertia();
            
            if (stepCount%printInterval == 0) {
                angularVelocity.E(angularMomentum);
                rotationTensor.setQuaternions(agent.quat);
                rotationTensor.transform(angularVelocity);
                angularVelocity.DE(moment);
                angularVelocity.TE(angularVelocity);
                angularVelocity.TE(moment);
                KE += angularVelocity.x(0) + angularVelocity.x(1)+ angularVelocity.x(2);
            }
            
//            System.out.println("angular momentum "+angularMomentum);
            double[] quat = agent.quat;
            
            //quatVelocity equation works with body-fixed angular velocity (ugh)
            //get angular velocity at time t
            angularVelocity.E(angularMomentum);
//            System.out.println("quat "+quat[0]+" "+quat[1]+" "+quat[2]+" "+quat[3]);
            rotationTensor.setQuaternions(quat);
//            System.out.println("rotationTensor\n"+rotationTensor);
            rotationTensor.transform(angularVelocity);
            angularVelocity.DE(moment);
            
//            System.out.println("body-fixed angular velocity "+angularVelocity);
//            xWork.E(angularVelocity);
//            rotationTensor.inverse();
//            rotationTensor.transform(xWork);
//            System.out.println("space-fixed angular velocity "+xWork);

            // calculate 0 timestep quat velocities
            quatVelocity[0] = 0.5*(-quat[1]*angularVelocity.x(0)
                                   -quat[2]*angularVelocity.x(1)
                                   -quat[3]*angularVelocity.x(2));
            quatVelocity[1] = 0.5*(+quat[0]*angularVelocity.x(0)
                                   -quat[3]*angularVelocity.x(1)
                                   +quat[2]*angularVelocity.x(2));
            quatVelocity[2] = 0.5*(+quat[3]*angularVelocity.x(0)
                                   +quat[0]*angularVelocity.x(1)
                                   -quat[1]*angularVelocity.x(2));
            quatVelocity[3] = 0.5*(-quat[2]*angularVelocity.x(0)
                                   +quat[1]*angularVelocity.x(1)
                                   +quat[0]*angularVelocity.x(2));
            
            // advance quat to half-timestep
            double qNorm = 0;
            for (int i=0; i<4; i++) {
                tempQuat[i] = quat[i] + 0.5*timeStep*quatVelocity[i];
//                System.out.println("half "+tempQuat[i]+" "+timeStep+" "+quatVelocity[i]);
                qNorm += tempQuat[i]*tempQuat[i];
            }
            qNorm = 1.0/Math.sqrt(qNorm);
            for (int i=0; i<4; i++) {
                // renormalize due to roundoff
                tempQuat[i] *= qNorm;
            }

            //advance momentum to half timestep
            angularMomentum.PEa1Tv1(0.5*timeStep, agent.torque);

            // get half timestep angular velocity and calculate half timestep quat velocities 
            angularVelocity.E(angularMomentum);
            rotationTensor.setQuaternions(tempQuat);
            rotationTensor.transform(angularVelocity);
            angularVelocity.DE(moment);
//            System.out.println("half timestep angularVelocity "+angularVelocity);
            
            quatVelocity[0] = 0.5*(-tempQuat[1]*angularVelocity.x(0)
                                   -tempQuat[2]*angularVelocity.x(1)
                                   -tempQuat[3]*angularVelocity.x(2));
            quatVelocity[1] = 0.5*(+tempQuat[0]*angularVelocity.x(0)
                                   -tempQuat[3]*angularVelocity.x(1)
                                   +tempQuat[2]*angularVelocity.x(2));
            quatVelocity[2] = 0.5*(+tempQuat[3]*angularVelocity.x(0)
                                   +tempQuat[0]*angularVelocity.x(1)
                                   -tempQuat[1]*angularVelocity.x(2));
            quatVelocity[3] = 0.5*(-tempQuat[2]*angularVelocity.x(0)
                                   +tempQuat[1]*angularVelocity.x(1)
                                   +tempQuat[0]*angularVelocity.x(2));

            //advance quat to full timestep
            qNorm = 0;
            double omega = Math.sqrt(angularVelocity.squared());
            double fac = 2*Math.sqrt(2-2*Math.cos(0.5*omega*timeStep))/(omega*timeStep);
            fac = 1;
            for (int i=0; i<4; i++) {
//                System.out.println(quat[i]+" "+timeStep+" "+quatVelocity[i]);
                quat[i] += fac*timeStep*quatVelocity[i];
                qNorm += quat[i]*quat[i];
            }
//            System.out.println("qnorm "+qNorm);
            qNorm = 1.0/Math.sqrt(qNorm);
            for (int i=0; i<4; i++) {
                // renormalize due to roundoff
                quat[i] *= qNorm;
            }
            typeAgent.calcer.setOrientation(molecule, quat);
//            System.out.println("q "+quat[0]+" "+quat[1]+" "+quat[2]+" "+quat[3]);
//            typeAgent.calcer.calcOrientation(molecule, quat);
//            System.out.println("new q "+quat[0]+" "+quat[1]+" "+quat[2]+" "+quat[3]);
            double mass = orientedType.getMass();
            IVectorMutable velocity = ((IAtomKinetic)molecule).getVelocity();
            KE += mass * velocity.squared();
            //advance linear velocity to half timestep
            velocity.PEa1Tv1(0.5*timeStep/mass, agent.force);
            
            //advance position to full timestep
            IVectorMutable transVec = ((AtomActionTranslateBy)translator.getAtomAction()).getTranslationVector();
            transVec.Ea1Tv1(timeStep, velocity);
            ((IAtomPositioned)molecule).getPosition().PE(transVec);
            translator.actionPerformed(molecule);
        }
        
        KE *= 0.5;
        if (stepCount%printInterval == 0) System.out.println(currentTime+" "+KE+" "+PE+" "+(PE+KE));

        forceSum.reset();
        //Compute forces on each atom at t+dt
        potentialMaster.calculate(box, allAtoms, forceSum);
        
        if(forceSum instanceof PotentialCalculationForcePressureSum){
            pressureTensor.E(((PotentialCalculationForcePressureSum)forceSum).getPressureTensor());
        }
        
        for (int iMolecule = 0; iMolecule<nMolecules; iMolecule++) {
            IMolecule molecule = moleculeList.getMolecule(iMolecule);
            IAtomList children = molecule.getChildList();
            MyTypeAgent typeAgent = (MyTypeAgent)typeAgentManager.getAgent(molecule.getType());
            if (typeAgent == null) {
                // unimolecular or at least not rigid
                //Finish integration step
                for (int iLeaf=0; iLeaf<children.getAtomCount(); iLeaf++) {
                    IAtomKinetic a = (IAtomKinetic)children.getAtom(iLeaf);
//                    System.out.println("force: "+((MyAgent)a.ia).force.toString());
                    IVectorMutable velocity = a.getVelocity();
                    workTensor.Ev1v2(velocity,velocity);
                    workTensor.TE(((Atom)a).getType().getMass());
                    pressureTensor.PE(workTensor);
                    if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet((IAtom)a))) {
                        System.out.println("second "+a+" v="+velocity+", f="+((AtomAgent)leafAgentManager.getAgent((IAtom)a)).force);
                    }
                    velocity.PEa1Tv1(0.5*timeStep*((Atom)a).getType().rm(),((AtomAgent)leafAgentManager.getAgent((IAtom)a)).force);  //p += f(new)*dt/2
                }
                
                continue;
            }
            
            MoleculeAgent agent = (MoleculeAgent)moleculeAgentManager.getAgent(molecule);
            //calc torque and linear force
            agent.torque.E(0);
            agent.force.E(0);
            IVectorMutable moleculePosition = ((IAtomPositioned)molecule).getPosition();
            for (int i=0; i<children.getAtomCount(); i++) {
                IAtomKinetic atom = (IAtomKinetic)children.getAtom(i);
                IVectorMutable atomForce = ((AtomAgent)leafAgentManager.getAgent((IAtom)atom)).force;
                agent.force.PE(atomForce);

                xWork.Ev1Mv2(atom.getPosition(), moleculePosition);
                xWork.XE(atomForce);
                agent.torque.PE(xWork);
            }

            //advance linear velocity to full timestep
            ((IAtomKinetic)molecule).getVelocity().PEa1Tv1(0.5*timeStep/((ISpeciesOriented)molecule.getType()).getMass(), agent.force);

            //advance momentum to full timestep
            ((IAtomOrientedKinetic)molecule).getAngularVelocity().PEa1Tv1(0.5*timeStep, agent.torque);
//            System.out.println(currentTime+" "+agent.angularMomentum);
        }
        pressureTensor.TE(1/box.getBoundary().volume());

        if(isothermal) {
            doThermostat();
        }
    }

    /**
     * Returns the pressure tensor based on the forces calculated during the
     * last time step.
     */
    public Tensor getPressureTensor() {
        return pressureTensor;
    }

    public void scaleMomenta() {
//        System.out.println("bite me");
    }

    public void randomizeMomenta() {
        IMoleculeList moleculeList = box.getMoleculeList();
        int nMolecules = moleculeList.getMoleculeCount();
        for (int iMolecule = 0; iMolecule<nMolecules; iMolecule++) {
            IMolecule molecule = moleculeList.getMolecule(iMolecule);
            MyTypeAgent typeAgent = (MyTypeAgent)typeAgentManager.getAgent(molecule.getType());
            if (typeAgent == null) {
                super.randomizeMomentum((IAtomKinetic)molecule.getChildList().getAtom(0));
                continue;
            }
    
            MoleculeAgent agent = (MoleculeAgent)moleculeAgentManager.getAgent(molecule);
            IVectorMutable velocity = ((IAtomKinetic)molecule).getVelocity();
            IVectorMutable angularMomentum = ((IAtomOrientedKinetic)molecule).getAngularVelocity();
            double mass = ((ISpeciesOriented)molecule.getType()).getMass();
//            System.out.println("mass = "+mass);
            int D = velocity.getD();
            for(int i=0; i<D; i++) {
                velocity.setX(i,random.nextGaussian());
            }
            velocity.TE(Math.sqrt(temperature/mass));
//            velocity.E(0);
    
            for(int i=0; i<D; i++) {
                angularVelocity.setX(i,random.nextGaussian());
            }
            angularMomentum.Ea1Tv1(temperature, ((ISpeciesOriented)molecule.getType()).getMomentOfInertia());
            angularMomentum.map(new Function.Sqrt());
            angularMomentum.TE(angularVelocity);
            //angularMomentum is now the correct body-fixed angular momentum
            
            typeAgent.calcer.calcOrientation(molecule, agent.quat);
            rotationTensor.setQuaternions(agent.quat);
            // body-fixed to space-fixed, so invert
            rotationTensor.invert();
            // transform to space-fixed angular momentum
            rotationTensor.transform(angularMomentum);
        }
    }
    
    public void randomizeMomentum(IAtomKinetic atom) {
        if (atom instanceof Atom) {
            super.randomizeMomentum(atom);
            return;
        }

        MyTypeAgent typeAgent = (MyTypeAgent)typeAgentManager.getAgent(((IMolecule)atom).getType());
        if (typeAgent == null) {
            super.randomizeMomentum(atom);
            return;
        }

        MoleculeAgent agent = (MoleculeAgent)moleculeAgentManager.getAgent((IMolecule)atom);
        IVectorMutable velocity = atom.getVelocity();
        IVectorMutable angularMomentum = ((IAtomOrientedKinetic)atom).getAngularVelocity();
        double mass = ((ISpeciesOriented)((IMolecule)atom).getType()).getMass();
        int D = velocity.getD();
        for(int i=0; i<D; i++) {
            velocity.setX(i,random.nextGaussian());
        }
        velocity.TE(Math.sqrt(temperature/mass));

        for(int i=0; i<D; i++) {
            angularVelocity.setX(i,random.nextGaussian());
        }
        angularVelocity.TE(Math.sqrt(temperature/mass));
        typeAgent.calcer.calcOrientation((IMolecule)atom, agent.quat);
        rotationTensor.setQuaternions(agent.quat);
        // body-fixed to space-fixed, so invert
        rotationTensor.invert();
        rotationTensor.transform(angularVelocity);
        
        angularMomentum.E(angularVelocity);
        angularMomentum.TE(angularMomentum);
        angularMomentum.TE(temperature);
        IVector moment = ((ISpeciesOriented)((IMolecule)atom).getType()).getMomentOfInertia();
        angularMomentum.DE(moment);
        angularMomentum.map(new Function.Sqrt());
        angularMomentum.DE(moment);
    }
    
    public void reset() {
        super.reset();

        if (Debug.ON && Debug.DEBUG_NOW) {
            IAtomList pair = Debug.getAtoms(box);
            if (pair != null) {
                IVectorMutable dr = space.makeVector();
                dr.Ev1Mv2(((IAtomPositioned)pair.getAtom(1)).getPosition(), ((IAtomPositioned)pair.getAtom(0)).getPosition());
                System.out.println(pair+" dr "+dr);
            }
        }

        forceSum.reset();
        potentialMaster.calculate(box, allAtoms, forceSum);

        IMoleculeList moleculeList = box.getMoleculeList();
        int nMolecules = moleculeList.getMoleculeCount();

        for (int iMolecule=0; iMolecule<nMolecules; iMolecule++) {
            IMolecule molecule = moleculeList.getMolecule(iMolecule);
            MyTypeAgent typeAgent = (MyTypeAgent)typeAgentManager.getAgent(molecule.getType());
            if (typeAgent== null) {
                continue;
            }
            
            MoleculeAgent agent = (MoleculeAgent)moleculeAgentManager.getAgent(molecule);
            IVectorMutable position = ((IAtomPositioned)molecule).getPosition();
            typeAgent.calcer.calcOrientation(molecule, agent.quat);
            position.E(atomPositionCOM.position(molecule));
            agent.force.E(0);
            agent.torque.E(0);
            //calc angular velocities
            IAtomList children = molecule.getChildList();
            for (int i=0; i<children.getAtomCount(); i++) {
                IAtomKinetic atom = (IAtomKinetic)children.getAtom(i);
                IVectorMutable force = ((AtomAgent)leafAgentManager.getAgent((IAtom)atom)).force;
                agent.force.PE(force);

                xWork.Ev1Mv2(atom.getPosition(), position);
                xWork.XE(force);
                agent.torque.PE(xWork);
            }
        }
    }
              
//--------------------------------------------------------------
    
    public Class getAgentClass() {
        return AtomAgent.class;
    }

    public Class getMoleculeAgentClass() {
        return MoleculeAgent.class;
    }

    public final Object makeAgent(IMolecule a) {
        return new MoleculeAgent(space);
    }
    
    public Object makeAgent(IAtom a) {
        return new AtomAgent(space);
    }
    
    public void releaseAgent(Object agent, IAtom atom) {}
    public void releaseAgent(Object agent, IMolecule atom) {}
            
    public static class MoleculeAgent implements Serializable {  //need public so to use with instanceof
        private static final long serialVersionUID = 1L;
        public final double[] quat;  // for molecules
        public final IVectorMutable torque;
        public final IVectorMutable force;

        public MoleculeAgent(ISpace space) {
            quat = new double[4];
            torque = space.makeVector();
            force = space.makeVector();
        }
    }
    
    public static class AtomAgent implements IntegratorBox.Forcible, Serializable {  //need public so to use with instanceof
        private static final long serialVersionUID = 1L;
        public final IVectorMutable force;  // for leaf atoms

        public AtomAgent(ISpace space) {
            force = space.makeVector();
        }
        
        public IVectorMutable force() {return force;}
    }

    public Class getSpeciesAgentClass() {
        return MyTypeAgent.class;
    }

    public Object makeAgent(ISpecies type) {
        return null;
    }

    public void releaseAgent(Object agent, ISpecies type) {}
    
    public static class MyTypeAgent implements Serializable {
        private static final long serialVersionUID = 1L;
        public final OrientationCalcQuaternion calcer;
        
        public MyTypeAgent(OrientationCalcQuaternion calcer) {
            this.calcer = calcer;
        }
    }
    
    public static void main(String[] args) {
        Space space = Space3D.getInstance();
        Simulation sim = new Simulation(space);
        Box box = new Box(new BoundaryRectangularNonperiodic(space), space);
        sim.addBox(box);
        SpeciesWater3P species = new SpeciesWater3P(sim.getSpace());
        sim.getSpeciesManager().addSpecies(species);
        box.setNMolecules(species, 108);
        box.setDensity(1/18.0*Constants.AVOGADRO/1E24);
        double timeInterval = 0.001;
        int interval = 10;
        if (false) {
            new ConfigurationLattice(new LatticeCubicFcc(space), space).initializeCoordinates(box);
        }
        else {
            ConfigurationFile configFile = new ConfigurationFile("water108");
            configFile.initializeCoordinates(box);
        }
        PotentialMaster potentialMaster = new PotentialMaster();
        IntegratorVelocityVerletQuaternion integrator = new IntegratorVelocityVerletQuaternion(sim, potentialMaster, timeInterval/interval, 1, space);
        integrator.setBox(box);
        integrator.printInterval = interval;
        integrator.setOrientationCalc(species, new OrientationCalcWater3P(sim.getSpace()));
        integrator.setTemperature(Kelvin.UNIT.toSim(298));
//        integrator.setIsothermal(true);
        integrator.setThermostatInterval(100);
        ActivityIntegrate ai = new ActivityIntegrate(integrator);
        sim.getController().addAction(ai);
        BoxImposePbcMolecule pbc = new BoxImposePbcMolecule(box, space);
        integrator.getEventManager().addListener(new IntegratorListenerAction(pbc));
        double oCharge = Electron.UNIT.toSim(-0.82);
        double hCharge = Electron.UNIT.toSim(0.41);
        P2Electrostatic pOO = new P2Electrostatic(sim.getSpace());
        pOO.setCharge1(oCharge);
        pOO.setCharge2(oCharge);
        P2Electrostatic pOH = new P2Electrostatic(sim.getSpace());
        pOH.setCharge1(oCharge);
        pOH.setCharge2(hCharge);
        P2Electrostatic pHH = new P2Electrostatic(sim.getSpace());
        pHH.setCharge1(hCharge);
        pHH.setCharge2(hCharge);
        P2LennardJones p2 = new P2LennardJones(sim.getSpace(), 3.1670, Kelvin.UNIT.toSim(78.23));
        IAtomType oType = species.getOxygenType();
        IAtomType hType = species.getHydrogenType();
       
        potentialMaster.addPotential(p2 /*new P2SoftSphericalTruncatedShifted(p2, boxlength*0.5)*/, new IAtomType[]{oType,oType});
        PotentialGroup pGroup = potentialMaster.getPotential(new ISpecies[]{species, species});
        potentialMaster.addPotential(pOO /*new P2SoftSphericalTruncatedShifted(pOO, boxlength*0.5)*/, new IAtomType[]{oType,oType});
        pGroup.addPotential(pOH /*new P2SoftSphericalTruncatedShifted(pOH, boxlength*0.5)*/, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{oType, hType}));
        pGroup.addPotential(pOH /*new P2SoftSphericalTruncatedShifted(pOH, boxlength*0.5)*/, ApiBuilder.makeIntergroupTypeIterator(new IAtomType[]{hType, oType}));
        potentialMaster.addPotential(pHH /*new P2SoftSphericalTruncatedShifted(pHH, boxlength*0.5)*/, new IAtomType[]{hType,hType});
        
//        System.out.println("h1 at "+((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition());
//        System.out.println("o at "+((IAtomPositioned)box.getLeafList().getAtom(2)).getPosition());
        if (false) {
          // ai.setSleepPeriod(10);
            SimulationGraphic graphic = new SimulationGraphic(sim, "Quat", 1, space, sim.getController());
            ColorSchemeByType colorScheme = (ColorSchemeByType)graphic.getDisplayBox(box).getColorScheme();
            colorScheme.setColor(oType, Color.RED);
            colorScheme.setColor(hType, Color.WHITE);
            
            graphic.makeAndDisplayFrame();
        }
        else {
            ai.setMaxSteps(10000);
//            AtomSet atoms = box.getLeafList();
//            Printme printme = new Printme((IAtomPositioned)atoms.getAtom(0), (IAtomPositioned)atoms.getAtom(1), new DataSourceCountTime(integrator), "quat"+(dt*1000));
//            integrator.addIntervalAction(printme);
            ai.actionPerformed();
            
//            printme.cleanup();
//            integrator.setThermostatInterval(1000);
//            ai.setMaxSteps(100000);
//            ai.actionPerformed();
//            System.out.println("t="+integrator.getCurrentTime());
//            System.out.println("h1 at "+((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition());
//            System.out.println("o at "+((IAtomPositioned)box.getLeafList().getAtom(2)).getPosition());
//            OrientationCalcWater calcer = new OrientationCalcWater(sim.getSpace());
//            OrientationFull3D orientation = new OrientationFull3D(sim.getSpace());
//            calcer.calcOrientation((IMolecule)box.getMoleculeList().getAtom(0), orientation);
//            System.out.println(orientation.getDirection()+" "+orientation.getSecondaryDirection());
        }
    }
}
