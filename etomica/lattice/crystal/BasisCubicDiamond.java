package etomica.lattice.crystal;
import etomica.space3d.Space3D;
import etomica.space3d.Vector3D;

/**
 * An 8-atom basis that makes a diamond crystal using a BravaisLattice having a
 * Cubic primitive.  Diamond is 4 fcc sites each with 2 subsites.
 *
 * @author David Kofke
 */
 
 /* History
  * 09/26/02 (DAK) new, from BasisCubicFccDiamond
  */
 
public class BasisCubicDiamond extends BasisCubic {
    
    /**
     * Makes a diamond-on-fcc 8-atom basis.
     */
    public BasisCubicDiamond(PrimitiveCubic primitive) {
        super(primitive, scaledPositions);
    }
    
    private static final Vector3D[] scaledPositions = new Vector3D[] {
			new Vector3D(0.00, 0.00, 0.00),
			new Vector3D(0.00, 0.50, 0.50),
			new Vector3D(0.50, 0.50, 0.00),
			new Vector3D(0.50, 0.00, 0.50),
			new Vector3D(0.25, 0.25, 0.25),
			new Vector3D(0.25, 0.75, 0.75),
			new Vector3D(0.75, 0.75, 0.25),
			new Vector3D(0.75, 0.25, 0.75)
    };
}//end of BasisCubicDiamond