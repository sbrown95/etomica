package etomica.simulation;

import java.util.HashMap;
import java.util.LinkedList;

import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IEvent;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.ISpeciesManager;
import etomica.chem.elements.Element;
import etomica.util.Arrays;

/**
 * The SpeciesManager manages Species and AtomTypes on behalf of the
 * Simulation.
 * 
 * @author Andrew Schultz
 */
public class SpeciesManager implements java.io.Serializable, ISpeciesManager {

    public SpeciesManager(ISimulation sim) {
        this.sim = sim;
        speciesList = new ISpecies[0];
        elementSymbolHash = new HashMap<String,Element>();
        elementAtomTypeHash = new HashMap<Element,LinkedList<IAtomTypeLeaf>>();
    }

    /* (non-Javadoc)
	 * @see etomica.simulation.ISpeciesManager#addSpecies(etomica.api.ISpecies)
	 */
    public void addSpecies(ISpecies species) {
        for (int i=0; i<speciesList.length; i++) {
            if (speciesList[i] == species) {
                throw new IllegalArgumentException("Species already exists");
            }
        }
        int index = speciesList.length;
        species.setIndex(index);
        speciesList = (ISpecies[])Arrays.addObject(speciesList,species);

        // All of the atom types index need to be incremented
        // due to the insertion of the new species.
        for(int i = speciesList.length-2; i > -1; i--) {
            for(int j = speciesList[i].getChildTypeCount()-1 ; j > -1; j--) {
                IAtomTypeLeaf leafType = speciesList[i].getChildType(j);
                int oldIndex = leafType.getIndex();
                leafType.setIndex(oldIndex+1);
                index++;
                IEvent evt = new SimulationAtomTypeIndexChangedEvent(leafType, oldIndex);
                sim.getEventManager().fireEvent(evt);
            }
	    }

	    
        for(int i = 0; i < species.getChildTypeCount(); i++) {
            species.getChildType(i).setIndex(++index);
            atomTypeAddedNotify(species.getChildType(i));
        }

        int boxCount = sim.getBoxCount();
        for (int i=0; i<boxCount; i++) {
            sim.getBox(i).addSpeciesNotify(species);
        }

        // this just fires an event for listeners to receive
        IEvent evt = new SimulationSpeciesAddedEvent(species);
	    sim.getEventManager().fireEvent(evt);

    }

    /* (non-Javadoc)
	 * @see etomica.simulation.ISpeciesManager#removeSpecies(etomica.api.ISpecies)
	 */
    public void removeSpecies(ISpecies removedSpecies) {

        int index = removedSpecies.getIndex();
        
        if (speciesList[index] != removedSpecies) {
            throw new IllegalArgumentException("Species to remove not found at expected location.");
        }
        
        speciesList = (ISpecies[])Arrays.removeObject(speciesList,removedSpecies);
        
        for(int j = 0; j < removedSpecies.getChildTypeCount(); j++) {
            atomTypeRemovedNotify(removedSpecies.getChildType(j));
        }
        
        for (int j = 0; j < speciesList.length; j++) {
            if(speciesList[j].getIndex() != j) {
                int oldIndex = speciesList[j].getIndex();
                speciesList[j].setIndex(j);
                IEvent evt = new SimulationAtomTypeIndexChangedEvent(speciesList[j], oldIndex);
                sim.getEventManager().fireEvent(evt);
            }
        }
        
        int boxCount = sim.getBoxCount();
        for (int j = 0; j < boxCount; j++) {
            sim.getBox(j).removeSpeciesNotify(removedSpecies);
        }
        IEvent evt = new SimulationSpeciesRemovedEvent(removedSpecies);
        sim.getEventManager().fireEvent(evt);
        
        int atomTypeIndex = speciesList.length;
        int numAtomTypes = 0;
        for (int j = 0; j < speciesList.length; j++) {
            for(int k = 0; k < speciesList[j].getChildTypeCount(); k++) {
                numAtomTypes++;
                IAtomTypeLeaf leafType = speciesList[j].getChildType(k);
                int oldIndex = leafType.getIndex();
                leafType.setIndex(atomTypeIndex++);
                if(oldIndex != atomTypeIndex-1) {
                    IEvent event = new SimulationAtomTypeIndexChangedEvent(leafType, oldIndex);
                    sim.getEventManager().fireEvent(event);
                }
            }
        }
        
        IEvent maxEvt = new SimulationAtomTypeMaxIndexEvent(speciesList.length + numAtomTypes);
        sim.getEventManager().fireEvent(maxEvt);

    }

    /* (non-Javadoc)
	 * @see etomica.simulation.ISpeciesManager#boxAddedNotify(etomica.api.IBox)
	 */
    public void boxAddedNotify(IBox newBox) {
        for(int i=0; i<speciesList.length; i++) {
            newBox.addSpeciesNotify(speciesList[i]);
        }
    }


    public int getSpeciesCount() {
    	return speciesList.length;
    }

    public ISpecies getSpecies(int index) {
        return speciesList[index];
    }

    protected void atomTypeAddedNotify(IAtomTypeLeaf newChildType) {
        Element newElement = newChildType.getElement();
        Element oldElement = elementSymbolHash.get(newElement.getSymbol());
        if (oldElement != null && oldElement != newElement) {
            // having two AtomTypes with the same Element is OK, but having
            // two Elements with the same symbol is not allowed.
            throw new IllegalStateException("Element symbol "+newElement.getSymbol()+" already exists in this simulation as a different element");
        }
        // remember the element so we can check for future duplication
        elementSymbolHash.put(newElement.getSymbol(), newElement);
        LinkedList<IAtomTypeLeaf> atomTypeList = elementAtomTypeHash.get(newElement);
        if (atomTypeList == null) {
            atomTypeList = new LinkedList<IAtomTypeLeaf>();
            elementAtomTypeHash.put(newElement, atomTypeList);
        }
        atomTypeList.add(newChildType);
    }

    protected void atomTypeRemovedNotify(IAtomTypeLeaf removedType) {
        // remove the type's element from our hash 
        Element oldElement = removedType.getElement();
        elementSymbolHash.remove(oldElement.getSymbol());
    }

    /**
     * Returns an Element symbol starting with symbolBase that does not yet 
     * exist in the Simulation.  Return values will be like "base0, base1, base2..." 
     */
    public String makeUniqueElementSymbol(String symbolBase) {
        int n = 0;
        while (elementSymbolHash.containsKey(symbolBase+n)) {
            n++;
        }
        // reserve this symbol so future calls to makeUniqueElementSymbol won't return it
        // this will get replaced by the actual Element when it gets added via childTypeAddedNotify
        elementSymbolHash.put(symbolBase+n, null);
        return symbolBase+n;
    }

    private static final long serialVersionUID = 1L;
    private ISpecies[] speciesList;
    private final HashMap<String,Element> elementSymbolHash;
    private final HashMap<Element,LinkedList<IAtomTypeLeaf>> elementAtomTypeHash;
    private final ISimulation sim;
}
