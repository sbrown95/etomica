package etomica.config;

import etomica.api.IAtom;
import etomica.api.IVectorMutable;
import etomica.atom.IAtomOriented;
import etomica.space.IOrientation;
import etomica.space.ISpace;
import etomica.space3d.IOrientationFull3D;

/**
 * ConfigurationFile subclass capable of handling oriented atoms.  When an
 * oriented atom is encountered, the primary and secondary directions of the
 * orientation are expected on the same line as the position.
 * 
 * @author Andrew Schultz
 */
public class ConfigurationOrientedFile extends ConfigurationFile {

    public ConfigurationOrientedFile(String aConfName, ISpace space) {
        super(aConfName);
        direction = space.makeVector();
        secondaryDirection = space.makeVector();
    }

    protected void setPosition(IAtom atom, String string) {
        super.setPosition(atom, string);
        if (atom instanceof IAtomOriented) {
            String[] coordStr = string.split(" +");
            for (int i=0; i<direction.getD(); i++) {
                direction.setX(i, Double.valueOf(coordStr[i+direction.getD()]).doubleValue());
            }
            IOrientation orientation = ((IAtomOriented)atom).getOrientation();
            if (orientation instanceof IOrientationFull3D) {
                for (int i=0; i<direction.getD(); i++) {
                    secondaryDirection.setX(i, Double.valueOf(coordStr[i+direction.getD()*2]).doubleValue());
                }
                ((IOrientationFull3D)orientation).setDirections(direction, secondaryDirection);
            }
            else {
                orientation.setDirection(direction);
            }
        }

    }
    
    private static final long serialVersionUID = 2L;
    protected final IVectorMutable direction, secondaryDirection;
}
