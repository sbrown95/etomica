package simulate; 
import java.io.*;

public class Potential implements Serializable {

  Space space;
 // int species1Index, species2Index;  delete this
  protected transient final double[] r12 = new double[Space.D];  //work arrays
  protected transient final double[] f = new double[Space.D];
  protected transient final double[] v12 = new double[Space.D];
  protected transient final PairInteraction pair = new PairInteraction();

  public Potential() {;}
  
  public void bump(Atom atom1, Atom atom2){;}

  public double collisionTime(Atom atom1, Atom atom2){
    return 0.99*Double.MAX_VALUE;
  }
  
  public double[] force(Atom atom1, Atom atom2) {
    Space.uEa1(f,0.0);
    return f;
  }
  
  public double energy(Atom atom1, Atom atom2) {
    return 0.0;
  }
  
  public double dfdr(double r2) {return 0.0;}
  
  public PairInteraction computePairInteraction(Atom atom1, Atom atom2) {
    PairInteraction pair = new PairInteraction();
    pair.force = this.force(atom1,atom2);
    pair.rij = new double[2];
    pair.rij[0] = pair.rij[1] = pair.rSquared = Double.MAX_VALUE;    
    return pair;
  }
  
  public boolean overlap(Atom atom1, Atom atom2, double u) {u = 0.0; return false;}
  
    /*  delete the following
  public final int getSpecies1Index() {return this.species1Index;}  
  public final void setSpecies1Index(int index) {this.species1Index = index;}
  
  public final int getSpecies2Index() {return this.species2Index;}
  public final void setSpecies2Index(int index) {this.species2Index = index;}
  */
}


