package etomica.threaded;

import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialMaster;
import etomica.simulation.ISimulation;
import etomica.util.IRandom;

public class IntegratorVelocityVerletThreaded extends IntegratorVelocityVerlet {

    private static final long serialVersionUID = 1L;

    public IntegratorVelocityVerletThreaded(ISimulation sim, PotentialMaster potentialMaster, int numThreads) {
        this(potentialMaster,sim.getRandom(), 0.05, 1.0, numThreads);
    }

    public IntegratorVelocityVerletThreaded(PotentialMaster potentialMaster,
            IRandom random, double timeStep, double temperature, int numThreads) {
        super(potentialMaster, random, timeStep, temperature);
        
        PotentialCalculationForceSum[] pcfs = new PotentialCalculationForceSum[numThreads];
        for(int i=0; i<numThreads; i++){
            pcfs[i] = new PotentialCalculationForceSum();
        }
        
        forceSum = new PotentialCalculationForceSumThreaded(pcfs);
    }

}
