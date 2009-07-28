package etomica.dimer;

import java.io.IOException;
import java.util.Formatter;

import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.ISimulation;
import etomica.api.IVectorMutable;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialMaster;
import etomica.space.ISpace;

public class IntegratorEnergyMap extends IntegratorBox implements AgentSource{

    IAtomPositioned adatom;
    public MeterPotentialEnergy energy;
    String fileTail;
    private final ISpace space;

    public IntegratorEnergyMap(ISimulation aSim, PotentialMaster potentialMaster,
    		                   IAtomPositioned aAdatom, String aFileTail,
    		                   ISpace _space) {
        super(potentialMaster, 1.0);
        this.fileTail = aFileTail;
        this.adatom = aAdatom;
        this.space = _space;
    }
    
    public void doStepInternal(){
        try {
           
            Formatter formatter = new Formatter("energy-"+fileTail);
            IVectorMutable pos = adatom.getPosition();
            // Move atom along Y-axis, steps by 0.1
            for(int i=0; i<292; i++){ //292
                
                // Return atom to original Z position
                adatom.getPosition().setX(2, -1.6);
                
                // Move atom along Z-axis, steps by 0.1
                for(int j=0; j<213; j++){  //213
                    // --PRINT-- 
                    formatter.format("%f %7.2f %7.2f %7.2f \n",new Object[] {energy.getDataAsScalar(),pos.getX(0), pos.getX(1), pos.getX(2)});
                    
                    // Step atom by 0.1 along Z-axis
                    adatom.getPosition().setX(2, adatom.getPosition().getX(2) +0.02);
                }
                // Step atom by 0.1 along Y-axis
                adatom.getPosition().setX(1, adatom.getPosition().getX(1) + 0.02);
     
            }
            formatter.close();
        }
        catch (IOException e) {
            
        }
    }
    
    
    protected void setup() {
        super.setup();
    
        // Set variables for energy
        energy = new MeterPotentialEnergy(potentialMaster);
        energy.setBox(box);
        
        
    }

    
    
    public Class getAgentClass() {
        return IntegratorVelocityVerlet.MyAgent.class;
    }

    public Object makeAgent(IAtom a) {
        return new IntegratorVelocityVerlet.MyAgent(space);
    }

    public void releaseAgent(Object agent, IAtom atom) {
        // TODO Auto-generated method stub  
    }
    
}
