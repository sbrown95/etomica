package etomica.action;
import etomica.Atom;
import etomica.Space;
import etomica.atom.AtomPositionDefinition;
import etomica.data.DataSourceCOM;
import etomica.space.Vector;

/**
 * Moves (translates) an atom to a specified position.  Location of the
 * atom (which may be an atom group) is determined by an AtomPositionDefinition
 * instance that may be set for this class.
 */
public class AtomActionTranslateTo extends AtomActionAdapter {
    
    private final Vector dr;
    private Vector destination;
    private AtomPositionDefinition atomPositionDefinition;
    private AtomGroupAction atomTranslator;

    /**
     * Creates new action with atom position defined by its
     * center of mass (via DataSourceCOM).
     * @param space
     */
    public AtomActionTranslateTo(Space space) {
        dr = space.makeVector();
        atomPositionDefinition = new DataSourceCOM(space);
        atomTranslator = new AtomGroupAction(new AtomActionTranslateBy(space));
    }
    
    public void actionPerformed(Atom atom) {
        Vector currentPosition = atomPositionDefinition.position(atom);
        dr.Ev1Mv2(destination, currentPosition);
        ((AtomActionTranslateBy)atomTranslator.getAction()).setTranslationVector(dr);
        atomTranslator.actionPerformed(atom);
    }
       
    /**
     * @return Returns the destination, the position that the
     * atom will be moved to by this action.
     */
    public Vector getDestination() {
        return destination;
    }
    /**
     * @param destination The destination to set.  A local copy
     * is made of the given vector.
     */
    public void setDestination(Vector destination) {
        this.destination.E(destination);
    }
    /**
     * @return Returns the atomPositionDefinition.
     */
    public AtomPositionDefinition getAtomPositionDefinition() {
        return atomPositionDefinition;
    }
    /**
     * @param atomPositionDefinition The atomPositionDefinition to set.
     */
    public void setAtomPositionDefinition(
            AtomPositionDefinition atomPositionDefinition) {
        this.atomPositionDefinition = atomPositionDefinition;
    }
}