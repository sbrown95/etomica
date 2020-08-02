/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.action;

import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.action.activity.Controller2;
import etomica.config.Configuration;
import etomica.config.ConfigurationLattice;
import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.Integrator;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeCubicSimple;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.simulation.Simulation;
import etomica.space.Space;

/**
 * Action that invokes reset method of all registered simulation elements,
 * effectively initializing the entire simulation.
 */
public final class SimulationRestart extends SimulationActionAdapter {

    private static final long serialVersionUID = 1L;
    protected Configuration configuration;
    protected boolean ignoreOverlap;
    protected SimulationDataAction accumulatorAction;
    protected IAction postAction;
    private final Controller controller;
    private final Controller2 controller2;

    public SimulationRestart(Simulation sim) {
        super.setSimulation(sim, sim.getSpace());
        controller = sim.getController();
        controller2 = sim.getController2();
        if (space != null) {
            if (space.D() == 3) {
                setConfiguration(new ConfigurationLattice(new LatticeCubicFcc(space), space));
            } else if (space.D() == 2) {
                setConfiguration(new ConfigurationLattice(new LatticeOrthorhombicHexagonal(space), space));
            } else {
                Space sp = Space.getInstance(1);
                setConfiguration(new ConfigurationLattice(new LatticeCubicSimple(sp, 1.0), sp));
            }
        }
        ignoreOverlap = false;
        accumulatorAction = new SimulationDataAction(new ResetAccumulatorsAveraged());
    }

    public SimulationDataAction getDataResetAction() {
        return accumulatorAction;
    }

    public void setDataResetAction(SimulationDataAction newResetAction) {
        accumulatorAction = newResetAction;
    }

    public IAction getPostAction() {
        return postAction;
    }

    public void setPostAction(IAction postAction) {
        this.postAction = postAction;
    }

    public boolean isIgnoreOverlap() {
        return ignoreOverlap;
    }

    public void setIgnoreOverlap(boolean doIgnoreOverlap) {
        ignoreOverlap = doIgnoreOverlap;
    }

    /**
     * Resets boxs, integrators, and accumulators.
     */
    public void actionPerformed() {
        int boxCount = simulation.getBoxCount();
        for (int i = 0; i < boxCount; i++) {
            if (configuration != null) {
                configuration.initializeCoordinates(simulation.getBox(i));
            }
        }

        this.controller2.restartCurrentActivity();

        accumulatorAction.actionPerformed();
        if (postAction != null) postAction.actionPerformed();
    }

    /**
     * @return Returns the configuration.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration The configuration to set.
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
