package etomica.modules.pistoncylinder;

import etomica.potential.P1HardMovingBoundary;
import etomica.space.BoundaryRectangularNonperiodic;
import etomica.space.ISpace;


/**
 * Boundary class for PistonCylinder that accounts for the piston and collision
 * radius when calculating the (available) volume
 * @author andrew
 */
public class BoundaryPistonCylinder extends BoundaryRectangularNonperiodic {

    public BoundaryPistonCylinder(ISpace _space) {
        super(_space);
    }
    
    public void setPistonPotential(P1HardMovingBoundary newPistonPotential) {
        pistonPotential = newPistonPotential;
    }

    public double volume() {
        double collisionDiameter = pistonPotential.getCollisionRadius()*2;
        double v = 1;
        for (int i=0; i<space.D(); i++) {
            if (i == 1) {
                if (space.D() == 2) {
                    // bottom of the box is +dimensions/2, top is wall position
                    v *= (0.5*dimensions.getX(i) - pistonPotential.getWallPosition() - collisionDiameter);
                }
                else {
                    // bottom of the box is -dimensions/2, top is wall position
                    v *= (0.5*dimensions.getX(i) + pistonPotential.getWallPosition() - collisionDiameter);
                }
            }
            else {
                v *= dimensions.getX(i) - collisionDiameter;
            }
        }
        return v;
    }

    private P1HardMovingBoundary pistonPotential;
    private static final long serialVersionUID = 1L;
}
