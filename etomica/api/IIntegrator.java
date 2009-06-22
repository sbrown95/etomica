
package etomica.api;


public interface IIntegrator {

    /**
     * Performs the elementary integration step, such as a molecular dynamics
     * time step, or a Monte Carlo trial.
     */
    public void doStep();

    /**
     * Returns the number of steps performed by the integrator since it was
     * initialized.
     */
    public long getStepCount();

    /**
     * Defines the actions taken by the integrator to reset itself, such as
     * required if a perturbation is applied to the simulated box (e.g.,
     * addition or deletion of a molecule). Also invoked when the
     * integrator is started or initialized.
     */
    public void reset();

    /**
     * This method resets the step counter.
     */
    public void resetStepCount();

    public IIntegratorEventManager getEventManager();
    
}