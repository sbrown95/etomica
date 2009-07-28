package etomica.threaded.domain;

import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.atom.IAtomPositionDefinition;
import etomica.nbr.cell.NeighborCellManager;
import etomica.space.ISpace;

public class NeighborCellManagerThreaded extends NeighborCellManager {

    public int totalCells;
    
    public NeighborCellManagerThreaded(ISimulation sim, IBox box, double potentialRange, ISpace space) {
        super(sim, box, potentialRange, space);
        // TODO Auto-generated constructor stub
    }

    public NeighborCellManagerThreaded(ISimulation sim, IBox box, double potentialRange,
            IAtomPositionDefinition positionDefinition, ISpace space) {
        super(sim, box, potentialRange, positionDefinition, space);
        // TODO Auto-generated constructor stub
    }
    
    
    
    public boolean checkDimensions(){
        setNumCells(totalCells);
        return true;
    }
    
    //Divides up simulation box for threads
    public void setNumCells(int totalCells){
        this.totalCells = totalCells;
        if(totalCells==0){return;}
        IVector dimensions = box.getBoundary().getDimensions();
        lattice.setDimensions(dimensions);
       
        int [] nCells = calculateLatticeDimensions(totalCells, dimensions);
     
        //only update the lattice (expensive) if the number of cells changed
        int[] oldSize = lattice.getSize();
        for (int i=0; i<nCells.length; i++) {
            if (oldSize[i] != nCells[i]) {
                lattice.setSize(nCells);
                break;
            }
        }
        
        
        
    }
    
    protected int[] calculateLatticeDimensions(int nCells, IVector shape) {
        int dimLeft = shape.getD();
        int nCellsLeft = nCells;
        int[] latticeDimensions = new int[shape.getD()];
        while (dimLeft > 0) {
            double smin = Double.POSITIVE_INFINITY;
            int dmin = 0;
            double product = 1.0;
            for (int idim = 0; idim < shape.getD(); idim++) {
                if (latticeDimensions[idim] > 0)
                    continue;
                if (shape.getX(idim) < smin) {
                    smin = shape.getX(idim);
                    dmin = idim;
                }
                product *= shape.getX(idim);
            }
            // round off except for last dimension (then round up)
            if (dimLeft > 1) {
                latticeDimensions[dmin] = (int) Math.round(shape.getX(dmin)
                        * Math.pow((nCellsLeft / product), 1.0 / dimLeft));
                if(latticeDimensions[dmin] == 0){
                    latticeDimensions[dmin] = 1;
                }
            } else {
                latticeDimensions[dmin] = (int) Math.ceil(shape.getX(dmin)
                        * nCellsLeft / product);
            }
            
            nCellsLeft = (nCellsLeft + latticeDimensions[dmin] - 1)
                    / latticeDimensions[dmin];
            dimLeft--;
        }
        return latticeDimensions;
    }

}
