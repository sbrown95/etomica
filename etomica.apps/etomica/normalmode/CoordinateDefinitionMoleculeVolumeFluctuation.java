package etomica.normalmode;

import java.io.Serializable;

import etomica.action.BoxInflate;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.IAtomPositionDefinition;
import etomica.atom.MoleculeAgentManager;
import etomica.atom.MoleculeAgentManager.MoleculeAgentSource;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisMonatomic;
import etomica.lattice.crystal.Primitive;
import etomica.space.ISpace;

/**
 * CoordinateDefinition implementation for molecules. The class takes the first
 * space.D values of u to be real space displacements of the molecule center of
 * mass from its nominal position. Subclasses should add additional u values for
 * intramolecular degrees of freedom.
 * 
 * with an extra degree of freedom for volume fluctuation
 * 
 * @author Andrew Schultz & Tai Boon Tan
 */
public class CoordinateDefinitionMoleculeVolumeFluctuation extends CoordinateDefinition
        implements Serializable {

    public CoordinateDefinitionMoleculeVolumeFluctuation(ISimulation sim, IBox box, Primitive primitive, int orientationDim, ISpace space) {
        this(sim, box, primitive, orientationDim, new BasisMonatomic(space), space);
    }
    
    public CoordinateDefinitionMoleculeVolumeFluctuation(ISimulation sim, IBox box, Primitive primitive, int orientationDim, Basis basis, ISpace space) {
        super(box, ((space.D() + orientationDim)*basis.getScaledCoordinates().length+1), primitive, basis, space);
        this.sim = sim;
        work1 = space.makeVector();
        inflate = new BoxInflate(space);
        inflate.setBox(box);
        
        u = new double[coordinateDim];
        setPositionDefinition(new AtomPositionGeometricCenter(space));
        rScale = 1.0;
    }
    
    public void initializeCoordinates(int[] nCells) {
        super.initializeCoordinates(nCells);
        moleculeSiteManager = new MoleculeAgentManager(sim, box, new MoleculeSiteSource(space, positionDefinition));
    }

    public double[] calcU(IMoleculeList molecules) {
        // calculates components of U related to the the center of mass of the
        // molecules
        // subclass is responsible for setting orientation or intramolecular
        // degrees of freedom
    	
    	/*
    	 * assigning the degrees of freedom for volume fluctuation
    	 * in the last 3 components in u[] array
    	 * 
    	 *  x-direction fluctuation : u[coordinateDim-3]
    	 *  y-direction fluctuation : u[coordinateDim-2]
    	 *  z-direction fluctuation : u[coordinateDim-1]
    	 *  
    	 */
    	
    	double currentDimi = box.getBoundary().getBoxSize().getX(0);
    	rScale = currentDimi/initVolume.getX(0);
    	u[coordinateDim-1] = currentDimi - initVolume.getX(0);  
    		
    	
        
    	int j = 0;
        for (int i=0; i<molecules.getMoleculeCount(); i++) {
            IMolecule molecule = molecules.getMolecule(i);
            IVector pos = positionDefinition.position(molecule);
            IVectorMutable site = getLatticePosition(molecule);
            
            work1.E(new double[]{(1/rScale)*pos.getX(0),
            					 (1/rScale)*pos.getX(1),
            					 (1/rScale)*pos.getX(2)});
            
            work1.ME(site);
            //work1.Ev1Mv2(pos, site);
               
            for (int k = 0; k < pos.getD(); k++) {
                u[j+k] = work1.getX(k);
            }
               j += (coordinateDim-1)/molecules.getMoleculeCount();

        }
        
        return u;
    }

    /**
     * Override if nominal U is more than the lattice position of the molecule
     */
    public void initNominalU(IMoleculeList molecules) {
    }

    public void setToU(IMoleculeList molecules, double[] newU) {
        // sets the center of mass of the molecules to that specified by newU
        // subclass is responsible for setting orientation or intramolecular
        // degrees of freedom
    	

    	
        int j = 0;
        for (int i=0; i<molecules.getMoleculeCount(); i++) {
            IMolecule molecule = molecules.getMolecule(i);
            IVectorMutable site = getLatticePosition(molecule);
            for (int k = 0; k < site.getD(); k++) {
                work1.setX(k, site.getX(k) + newU[j+k]);
            }
            
            atomActionTranslateTo.setDestination(work1);
            atomActionTranslateTo.actionPerformed(molecule);
            
            j += coordinateDim/molecules.getMoleculeCount();

        }
    	
        rScale = (initVolume.getX(0) + newU[coordinateDim-1])/initVolume.getX(0); //rescale the fluctation to assigned fluctuation
    	
       	inflate.setScale(rScale);
    	inflate.actionPerformed();
    	    	   	
    }
    
    public IVectorMutable getLatticePosition(IMolecule molecule) {
        return (IVectorMutable)moleculeSiteManager.getAgent(molecule);
    }
    
    public void setPositionDefinition(IAtomPositionDefinition positionDefinition) {
        this.positionDefinition = positionDefinition;
        atomActionTranslateTo.setAtomPositionDefinition(positionDefinition);
    }

    public IAtomPositionDefinition getPositionDefinition() {
        return positionDefinition;
    }
    
    public void setInitVolume(IVector initV){
    	this.initVolume = initV;
    }
    
    private static final long serialVersionUID = 1L;
    protected final ISimulation sim;
    protected MoleculeAgentManager moleculeSiteManager;
    protected final IVectorMutable work1;
    protected final double[] u;
    protected IAtomPositionDefinition positionDefinition;
    protected double rScale;
    protected IVector initVolume;
    protected final BoxInflate inflate;

    protected static class MoleculeSiteSource implements MoleculeAgentSource, Serializable {
        
        public MoleculeSiteSource(ISpace space, IAtomPositionDefinition positionDefinition) {
            this.space = space;
            this.positionDefinition = positionDefinition;
        }
        public Class getMoleculeAgentClass() {
            return IVectorMutable.class;
        }
        public Object makeAgent(IMolecule molecule) {
            IVectorMutable vector = space.makeVector();
            vector.E(positionDefinition.position(molecule));
            return vector;
        }
        public void releaseAgent(Object agent, IMolecule molecule) {
            //nothing to do
        }

        private final ISpace space;
        protected final IAtomPositionDefinition positionDefinition;
        private static final long serialVersionUID = 1L;
    }
}