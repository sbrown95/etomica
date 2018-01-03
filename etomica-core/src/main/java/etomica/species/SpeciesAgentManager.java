/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.species;

import etomica.simulation.Simulation;
import etomica.simulation.SimulationListener;
import etomica.simulation.SimulationSpeciesEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AtomTypeAgentManager acts on behalf of client classes (an AgentSource) to
 * manage agents in every AtomType in a box.  When species are added or
 * removed from the simulation, the agents array (indexed by the AtomType's
 * global index) is updated.  The client should call getAgents() at any point
 * where an atom might have have been added to the system because the old array
 * would be stale at that point.
 *
 * @author andrew
 */
public final class SpeciesAgentManager<E> {

    private final AgentSource<E> agentSource;
    private final Simulation sim;
    private final Map<ISpecies, E> agents;
    private final SimulationListener simulationListener = new SimulationListener() {
        public void simulationSpeciesAdded(SimulationSpeciesEvent e) {
            SpeciesAgentManager.this.agents.put(e.getSpecies(), SpeciesAgentManager.this.agentSource.makeAgent(e.getSpecies()));
        }

        public void simulationSpeciesRemoved(SimulationSpeciesEvent e) {
            SpeciesAgentManager.this.releaseAgents(e.getSpecies());
        }
    };

    public SpeciesAgentManager(AgentSource<E> source, Simulation sim) {
        this.agentSource = Objects.requireNonNull(source);
        this.sim = Objects.requireNonNull(sim);

        sim.getEventManager().addListener(simulationListener);

        agents = new LinkedHashMap<>();
        sim.getSpeciesList().forEach(species -> agents.put(species, agentSource.makeAgent(species)));
    }

    public Map<ISpecies, E> getAgents() {
        return this.agents;
    }

    /**
     * Sets the agent associated with the given atom type to be the given
     * agent.  The AtomType must be from the Simulation.  The AtomType's old
     * agent is not "released".  This should be done manually if needed.
     */
    public void setAgent(ISpecies species, E newAgent) {
        agents.put(species, newAgent);
    }

    /**
     * Convenience method to return the agent the given AtomType.  For repeated
     * access to the agents from multiple AtomTypes, it might be faster to use
     * the above getAgents method.
     */
    public E getAgent(ISpecies species) {
        return agents.get(species);
    }

    /**
     * Releases the agents associated with the given AtomType and its children.
     */
    private void releaseAgents(ISpecies species) {
        E agent = agents.remove(species);
        if (agent != null) {
            agentSource.releaseAgent(agent, species);
        }
    }

    /**
     * Unregisters this class as a listener for AtomType-related events and
     * releases its agents.
     */
    public void dispose() {
        // remove ourselves as a listener to the old box
        sim.getEventManager().removeListener(simulationListener);
        sim.getSpeciesList().forEach(this::releaseAgents);
    }

    /**
     * Interface for an object that wants an agent associated with each
     * AtomType in a Simulation.
     */
    public interface AgentSource<E> {

        /**
         * Returns an agent for the given AtomType.
         */
        E makeAgent(ISpecies type);

        /**
         * This informs the agent source that the agent is going away and that
         * the agent source should disconnect the agent from other elements.
         */
        void releaseAgent(E agent, ISpecies type);
    }

}
