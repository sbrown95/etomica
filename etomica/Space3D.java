package etomica;
import java.awt.Graphics;
import java.awt.Color;
import java.util.Random;
import etomica.units.*;

/**
 * Modeled after space2D and space, most of this should be correct.  All of the boundary conditions should be checked
 * before an attemt to use this is made.
 *
 * @author Rob Riggleman
 * @author David Kofke
 */

public class Space3D extends Space implements EtomicaElement {

    public static String version() {return "Space3D:01.07.11/"+Space.VERSION;}
    public static final int D = 3;
    public final int D() {return D;}
    
    public double sphereVolume(double r) {return (Math.PI*4.0*r*r*r/3.0);}
    public double sphereArea(double r)  {return (Math.PI*4*r*r);}
    public Space.Vector makeVector() {return new Vector();}
    public Space.Orientation makeOrientation() {System.out.println("Orientation class not yet developed in 3D"); return null;}
    public Space.Tensor makeTensor() {return new Tensor();}
    public Space.Coordinate makeCoordinate(Atom a) {return new Coordinate(a);}
    public Space.CoordinatePair makeCoordinatePair(Phase p) {return new CoordinatePair(p);}

    public Space.Boundary.Type[] boundaryTypes() {return Boundary.TYPES;}
    public Space.Boundary makeBoundary() {return makeBoundary(Boundary.PERIODIC_SQUARE);}  //default
    public Space.Boundary makeBoundary(Space.Boundary.Type t) {
        if(t == Boundary.NONE) {return new BoundaryNone();}
        else if(t == Boundary.PERIODIC_SQUARE) {return new BoundaryPeriodicSquare();}
        else if(t == Boundary.SLIDING_BRICK) return new BoundarySlidingBrick();
        else return null;
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Three-dimensional space");
        return info;
    }

    public static final double r2(Vector u1, Vector u2, Boundary b) {
        Vector.WORK.x = u1.x - u2.x;
        Vector.WORK.y = u1.y - u2.y;
        Vector.WORK.z = u1.z - u2.z;
        b.nearestImage(Vector.WORK);
        return Vector.WORK.x*Vector.WORK.x + Vector.WORK.y*Vector.WORK.y + Vector.WORK.z*Vector.WORK.z;
    }
   
    
    public static final class Vector extends Space.Vector{
        public static final Random random = new Random();
        public static final Vector ORIGIN = new Vector(0.0, 0.0, 0.0);
        public static final Vector WORK = new Vector();
        public double x, y, z;
        public int length() {return D;}
        public int D() {return D;}
        public Vector () {x = 0.0; y = 0.0; z = 0.0;}
        public Vector (double a1,double a2,double a3) {x = a1; y = a2; z = a3;}
        public Vector (double[] a) {x = a[0]; y = a[1]; z = a[2];}//should check length of a for exception
        public Vector (Vector u) {this.E(u);}
        public String toString() {return "("+x+", "+y+", "+z+")";}
        public double component(int i) {if (i==0) return x; else if (i==1) return y; else return z;}
        public double[] toArray() {return new double[] {x, y, z};}
        public void sphericalCoordinates(double[] result) {
            result[0] = Math.sqrt(x*x + y*y + z*z);
            result[1] = Math.acos(z/result[0]); //theta
            result[2] = Math.atan2(x, y);  //phi
        }
        public void E(Vector u) {x = u.x; y = u.y; z = u.z;}
        public void E(double a) {x = a; y = a; z = a;}
        public void E(int i, double a) {if (i==0) x=a; else if (i==1) y=a; else z=a;}
        public void E(double[] u) {x = u[0]; y = u[1]; z = u[2];}  //should check length of array for exception
        public void Ea1Tv1(double a1, Space.Vector u) {Vector u1=(Vector)u; x = a1*u1.x; y = a1*u1.y; z = a1*u1.z;}
        public void PEa1Tv1(double a1, Space.Vector u) {Vector u1=(Vector)u; x += a1*u1.x; y += a1*u1.y; z += a1*u1.z;}
        public void PE(Vector u) {x += u.x; y += u.y; z += u.z;}
        public void PE(double a) {x += a; y += a; z += a;}
        public void ME(Vector u) {x -= u.x; y -= u.y; z -= u.z;}
        public void PE(int i, double a) {if (i==0) x+=a; else if (i==1) y+=a; else z+=a;}
        public void TE(double a) {x *= a; y *= a; z *= a;}
        public void TE(int i, double a) {if (i==0) x*=a; else if (i==1) y*=a; else z*=a;}
        public void TE(Vector u) {x *= u.x; y *= u.y; z *= u.z;}
        public void DE(double a) {x /= a; y /= a; z /= a;}
        public Space.Vector P(Space.Vector u) {Vector u1=(Vector)u; WORK.x = x+u1.x; WORK.y = y+u1.y; WORK.z = z+u1.z; return WORK;}
        public Space.Vector M(Space.Vector u) {Vector u1=(Vector)u; WORK.x = x-u1.x; WORK.y = y-u1.y; WORK.z = z-u1.z; return WORK;}
        public Space.Vector T(Space.Vector u) {Vector u1=(Vector)u; WORK.x = x*u1.x; WORK.y = y*u1.y; WORK.z = z*u1.z; return WORK;}
        public Space.Vector D(Space.Vector u) {Vector u1=(Vector)u; WORK.x = x/u1.x; WORK.y = y/u1.y; WORK.z = z/u1.z; return WORK;}
        public Space.Vector abs() {WORK.x = (x>0)?x:-x; WORK.y = (y>0)?y:-y; WORK.z = (z>0)?z:-z; return WORK;}
        public double min() {return (x < y) ? (x<z)?x:z : (y<z)?y:z;}
        public double max() {return (x > y) ? (x>z)?x:z : (y>z)?y:z;}
        public double squared() {return x*x + y*y + z*z;}
        public double dot(Vector u) {return x*u.x + y*u.y + z*u.z;}
        public void randomStep(double d) {x += (2.*random.nextDouble()-1)*d; y +=(2.*random.nextDouble()-1)*d; z +=(2.*random.nextDouble()-1)*d;}
        public void setRandom(double d) {x = random.nextDouble()*d; y = random.nextDouble()*d; z = random.nextDouble()*d;}
        public void setRandom(double dx, double dy, double dz) {x = random.nextDouble()*dx; y = random.nextDouble()*dy; z = random.nextDouble()*dz;}
        public void setRandom(Vector u) {setRandom(u.x, u.y, u.z);}
        public void setRandomCube() {
            x = random.nextDouble() - 0.5;
            y = random.nextDouble() - 0.5;
            z = random.nextDouble() - 0.5;
        }
        public void setComponent(int a, double d) { if(a==0) x=d; else if(a==1) y=d; else z=d;}
        public void setRandomSphere() {//check before using
            double z1 = 0.0;
            double z2 = 0.0;
            double z3 = 0.0;
            double rsq = Double.MAX_VALUE;
            while(rsq > 1.0) {
                
                z1 = 1.0 - 2.0*Math.random();
                z2 = 1.0 - 2.0*Math.random();
                z3 = 1.0 - 2.0*Math.random();
        
                rsq = z1*z1+z2*z2+z3*z3;
            }
            double r = Math.sqrt(rsq);
            x = z1/r;
            y = z2/r;
            z = z3/r;
        }
        public void setToOrigin() {x = ORIGIN.x; y = ORIGIN.y; z = ORIGIN.z;}
        public void randomDirection() {//check before using
            double z1 = 0.0;
            double z2 = 0.0;
            double z3 = 0.0;
            double rsq = Double.MAX_VALUE;
            while(rsq > 1.0) {
                
                z1 = 1.0 - 2.0*Math.random();
                z2 = 1.0 - 2.0*Math.random();
                z3 = 1.0 - 2.0*Math.random();
        
                rsq = z1*z1+z2*z2+z3*z3;
            }
            double r = Math.sqrt(rsq);
            x = z1/r;
            y = z2/r;
            z = z3/r;
        }
        public void E(Space.Vector u) {E((Vector) u);}
        public void PE(Space.Vector u) {PE((Vector) u);}
        public void TE(Space.Vector u) {TE((Vector) u);}
        public void ME(Space.Vector u) {ME((Vector) u);}
        public void DE(Space.Vector u) {DE((Vector) u);}
        public double dot(Space.Vector u) {return dot((Vector)u);}
        public Space3D.Vector cross(Space2D.Vector u) {
            Space3D.Vector.WORK.x = -z*u.y;
            Space3D.Vector.WORK.y = z*u.x;
            Space3D.Vector.WORK.z = x*u.y - y*u.x;
            return Space3D.Vector.WORK;
        }
        public Space3D.Vector cross(Space3D.Vector u) {//not thread safe
            Space3D.Vector.WORK.x = y*u.z - z*u.y;
            Space3D.Vector.WORK.y = z*u.x - x*u.z;
            Space3D.Vector.WORK.z = x*u.y - y*u.x;
            return Space3D.Vector.WORK;
        }
        public void XE(Vector u) {//cross product
            double xNew = y*u.z - z*u.y;
            double yNew = z*u.x - x*u.z;
            z = x*u.y - y*u.x;
            y = yNew;
            x = xNew;
        }
        public void normalize() {
            double norm = 1./Math.sqrt(x*x + y*y + z*z);
            x *= norm;
            y *= norm;
            z *= norm;
        }
    }
    
    public static final class Tensor extends Space.Tensor {
        double xx, xy, xz, yx, yy, yz, zx, zy, zz;
        public static final Tensor ORIGIN = new Tensor();
        public static final Tensor WORK = new Tensor();
        public Tensor () {xx = xy = xz = yx = yy = yz = zx = zy = zz = 0.0;}
        public Tensor (double xx, double xy, double xz, double yx, double yy, double yz, double zx, double zy, double zz) {
            this.xx=xx; this.xy=xy; this.xz=xz; this.yx=yx; this.yy=yy; this.yz=yz; this.zx=zx; this.zy=zy; this.zz=zz;
        }
        public double component(int i, int j) {
            return ( i==0 ) ? ( (j==0) ? xx : ( j==1 ? xy : xz ) ) : ( (i==1) ? ( (j==0) ? yx : ( (j==1) ? yy : yz ) ) : ( (j==0) ? zx : ((j==1) ? zy : zz)));
        }
        public int length() {return D;}
        public void setComponent(int i, int j, double d) {
            if (i==0) {if (j==0) {xx = d;} else if (j==1) {xy = d;} else xz = d;}
            else if (i==1) {if (j==0) {yx = d;} else if (j==1) {yy=d;} else yz = d;}
            else {if (j==0) {zx = d;} else if (j==1) {zy = d;} else zz = d;}
        }
        public void E(Tensor t) {xx=t.xx; xy=t.xy; xz=t.xz; yx=t.yx; yy=t.yy; yz=t.yz; zx=t.zx; zy=t.zy; zz=t.zz;}
        public void E(Vector u1, Vector u2) {xx=u1.x*u2.x; xy=u1.x*u2.y; xz=u1.x*u2.z; yx=u1.y*u2.x; yy=u1.y*u2.y; yz=u1.y*u2.z; zx=u1.z*u2.x; zy=u1.z*u2.y; zz=u1.z*u2.z;}
        public void E(double a) {xx=xy=xz=yx=yy=yz=zx=zy=zz=a;}
        public void PE(Tensor t) {xx+=t.xx; xy+=t.xy; xz+=t.xz; yx+=t.yx; yy+=t.yy; yz+=t.yz; zx+=t.zx; zy+=t.zy; zz+=t.zz;}
        public void PE(int i, int j, double d) {
            if (i==0) {if (j==0) {xx += d;} else if (j==1) {xy += d;} else xz += d;}
            else if (i==1) {if (j==0) {yx += d;} else if (j==1) {yy += d;} else yz += d;}
            else {if (j==0) {zx += d;} else if (j==1) {zy += d;} else zz += d;}
        }
        public double trace() {return xx+yy+zz;}
        public void E(Space.Tensor t) {E((Tensor)t);}
        public void E(Space.Vector u1, Space.Vector u2) {E((Vector)u1, (Vector)u2);}
        public void PE(Space.Tensor t) {PE((Tensor) t);}
        public void PE(Space.Vector u1, Space.Vector u2) {PE((Vector)u1, (Vector)u2);}
        public void TE(double a) {xx*=a; xy*=a; xz*=a; yx*=a; yy*=a; yz*=a; zx*=a; zy*=a; zz*=a;}
    }
    
    public static final class CoordinatePair extends Space.CoordinatePair {
        Coordinate c1;
        Coordinate c2;
        Boundary boundary;
        final Vector dimensions;
        private final Vector dr = new Vector();
        private double dvx, dvy, drx, dry, dvz, drz;
        public CoordinatePair() {this(new BoundaryNone());}
        public CoordinatePair(Space.Boundary b) {boundary = (Boundary)b; dimensions=(Vector)boundary.dimensions();}
        public CoordinatePair(Phase p) {
            this(p.boundary());
            p.boundaryMonitor.addObserver(this);
        }
        /**
         * Implementation of Observer interface to update boundary if notified of change by phase.
         */
        public void update(java.util.Observable obs, Object arg) {boundary = (Boundary)arg;}
        public void reset(Space.Coordinate coord1, Space.Coordinate coord2) {
            c1 = (Coordinate)coord1;
            c2 = (Coordinate)coord2;
            reset();
        }
        public void reset() {
            dr.x = c2.r.x - c1.r.x;
            dr.y = c2.r.y - c1.r.y;
            dr.z = c2.r.z - c1.r.z;
            boundary.nearestImage(dr);
            drx = dr.x;
            dry = dr.y;
            drz = dr.z;
            r2 = drx*drx + dry*dry + drz*drz;
            double rm1 = c1.rm();
            double rm2 = c2.rm();
            dvx = rm2*c2.p.x - rm1*c1.p.x;
            dvy = rm2*c2.p.y - rm1*c1.p.y;
            dvz = rm2*c2.p.z - rm1*c1.p.z;
        }
            
        public void reset(Space3D.Vector M) {
            dr.x = c2.r.x - c1.r.x + M.x;
            dr.y = c2.r.y - c1.r.y + M.y;
            dr.z = c2.r.z - c1.r.z + M.z;
            drx = dr.x;
            dry = dr.y;
            drz = dr.z;
            r2 = drx*drx + dry*dry + drz*drz;
        }
        public Space.Vector dr() {return dr;}
        public double dr(int i) {return (i==0) ? drx : ((i==1) ? dry : drz);}
        public double dv(int i) {return (i==0) ? dvx : ((i==1) ? dry : drz);}
        public double v2() {return dvx*dvx + dvy*dvy + dvz*dvz;}
        public double vDot(Space.Vector u) {return vDot((Space3D.Vector)u);}
        public double vDot(Space3D.Vector u) {return dvx*u.x + dvy*u.y + dvz*u.z;}
        public double vDotr() {return drx*dvx + dry*dvy + drz*dvz;}
        public void push(double impulse) {
            c1.p.x += impulse*drx;
            c1.p.y += impulse*dry;
            c1.p.z += impulse*drz;
            c2.p.x -= impulse*drx;
            c2.p.y -= impulse*dry;
            c2.p.z -= impulse*drz;
        }
        public void setSeparation(double r2New) {
            double ratio = c2.mass()*c1.rm();
            double delta = (Math.sqrt(r2New/this.r2()) - 1.0)/(1 + ratio);
            c1.r.x -= ratio*delta*drx;
            c1.r.y -= ratio*delta*dry;
            c1.r.z -= ratio*delta*drz;
            c2.r.x += ratio*delta*drx;
            c2.r.y += ratio*delta*dry;
            c2.r.z += ratio*delta*drz;
        }
    }
        
    public static class Coordinate extends Space.Coordinate {
        public Coordinate nextCoordinate, previousCoordinate;
        public final Vector r = new Vector();
        public final Vector p = new Vector();
        public final Vector rLast = new Vector();  //vector for saving position
        public final Vector work = new Vector();
        public Coordinate(Atom a) {super(a);}
        
        public void setNextAtom(Atom a) {
            if(a == null) nextCoordinate = null;
            else {
                nextCoordinate = (Coordinate)a.coord;
                ((Coordinate)a.coord).previousCoordinate = this;
            }
        }
        public Atom nextAtom() {return nextCoordinate!=null ? nextCoordinate.atom : null;}
        public Atom previousAtom() {return previousCoordinate!=null ? previousCoordinate.atom : null;}
        public void clearPreviousAtom() {previousCoordinate = null;}
                
        public Space.Vector position() {return r;}
        public Space.Vector momentum() {return p;}
        public double position(int i) {return r.component(i);}
        public double momentum(int i) {return p.component(i);}
        public double kineticEnergy() {return 0.5*p.squared()*rm();}
        public void freeFlight(double t) {
            double tM = t*rm(); // t/mass
            r.x += p.x*tM;
            r.y += p.y*tM;
            r.z += p.z*tM;
        }
        /**
        * Moves the atom by some vector distance
        * 
        * @param u
        */
        public void translateBy(Space.Vector u) {r.PE((Vector)u);}
        /**
        * Moves the atom by some vector distance
        * 
        * @param u
        */
        public void translateBy(double d, Space.Vector u) {r.PEa1Tv1(d,(Vector)u);}
        /**
        * Moves the atom by some vector distance
        * 
        * @param u
        */
        public void translateTo(Space.Vector u) {r.E((Vector)u);}      
        public void displaceBy(Space.Vector u) {rLast.E(r); translateBy((Vector)u);}
        public void displaceBy(double d, Space.Vector u) {rLast.E(r); translateBy(d,(Vector)u);}
        public void displaceTo(Space.Vector u) {rLast.E(r); translateTo((Vector)u);}  
        public void displaceWithin(double d) {work.setRandomCube(); displaceBy(d,work);}
        public void displaceToRandom(etomica.Phase p) {rLast.E(r); translateToRandom(p);}
        public void replace() {r.E(rLast);}
    //    public final void inflate(double s) {r.TE(s);}

        public void accelerateBy(Space.Vector u) {p.PE(u);}
        public void accelerateBy(double d, Space.Vector u) {p.PEa1Tv1(d,u);}

        public void randomizeMomentum(double temperature) {  //not very sophisticated; random only in direction, not magnitude
            double magnitude = Math.sqrt(mass()*temperature*(double)D);  //need to divide by sqrt(m) to get velocity
            momentum().setRandomSphere();
            momentum().TE(magnitude);
        }
    }//end of Coordinate
    
    public static class CoordinateGroup extends Coordinate implements Space.CoordinateGroup {
        private final Vector work = new Vector();
        public Coordinate firstChild, lastChild;
        public CoordinateGroup(AtomGroup a) {super(a);}

        public final Atom firstChild() {return (firstChild != null) ? firstChild.atom : null;}
        public final void setFirstChild(Atom atom) {firstChild = (Coordinate)atom.coord;}
        public final Atom lastChild() {return (lastChild != null) ? lastChild.atom : null;}
        public final void setLastChild(Atom atom) {lastChild = (Coordinate)atom.coord;}
                
        public double mass() {
            double massSum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                massSum += coord.mass();
            }
            return massSum;
        } 
        public double rm() {return 1.0/mass();}
        public Space.Vector position() {
            work.E(0.0); double massSum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                work.PEa1Tv1(coord.mass(), coord.position()); massSum += coord.mass();
            }
            work.DE(massSum);
            return work;
        }
        public Space.Vector momentum() {
            work.E(0.0);
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                work.PE(coord.momentum());
            }
            return work;
        }
        public double position(int i) {
            double sum = 0.0; double massSum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                sum += coord.mass()*coord.position(i); massSum += coord.mass();
            }
            sum /= massSum;
            return sum;
        }
        public double momentum(int i) {
            double sum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                sum += coord.mass()*coord.momentum(i);
            }
            return sum;
        }
        public double kineticEnergy() {
            double sum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                sum += coord.kineticEnergy();
            }
            return sum;
        }
        public void freeFlight(double t) {
            double sum = 0.0;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.freeFlight(t);
            }
        }
        public void translateBy(Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.translateBy(u0);
            }
        }
        public void translateBy(double d, Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.translateBy(d, u0);
            }
        }
        public void translateTo(Space.Vector u) {
            work.E((Vector)u);
            work.ME(position());
            translateBy(work);
        }
        public void displaceBy(Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.displaceBy(u0);
            }
        }
        public void displaceBy(double d, Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.displaceBy(d, u0);
            }
        }
        public void displaceTo(Space.Vector u) {
            work.E((Vector)u);
            work.ME(position());
            displaceBy(work);
        }
        public void displaceToRandom(etomica.Phase p) {
            displaceTo((Vector)p.boundary().randomPosition());
        }
        public void replace() {
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.replace();
            }
        }
        public void accelerateBy(Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.accelerateBy(u0);
            }
        }
        public void accelerateBy(double d, Space.Vector u) {
            Vector u0 = (Vector)u;
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.accelerateBy(d, u0);
            }
        }
        public final void displaceWithin(double d) {work.setRandomCube(); displaceBy(d,work);}
        public void randomizeMomentum(double temperature) {
            for(Coordinate coord=firstChild; coord!=null; coord=coord.nextCoordinate) {
                coord.randomizeMomentum(temperature);
            }
        }
    }//end of CoordinateGroup
    
    //***NOTE**** this class needs to be developed; this is only a duplicate of Space2D.Orientation
    public static class Orientation extends Space.Orientation {
        //The rotation matrix A operates on the components of a vector in the space-fixed frame to yield the
        //components in the body-fixed frame
        private final double[][] A = new double[D][D];
        private final Vector[] bodyFrame = new Vector[3];//= new Vector[] {new Vector(1.0,0.0), new Vector(0.0,1.0)};
        private final double[] angle = new double[1];
        private boolean needToUpdateA = true;
        public void E(Space.Orientation o) {E((Orientation)o);}
        public void E(Orientation o) {angle[0] = o.angle[0]; needToUpdateA = true;}
        public Space.Vector[] bodyFrame() {return bodyFrame;}
        public double[] angle() {return angle;}
        public void rotateBy(int i, double dt) {angle[0] += dt; needToUpdateA = true;}
        public void rotateBy(double[] dt) {angle[0] += dt[0]; needToUpdateA = true;}
        public void randomRotation(double t) {System.out.println("Space3D.Orientation.randomRotation not yet implemented");}
        private final void updateRotationMatrix() {
            A[0][0] = A[1][1] = Math.cos(angle[0]);
            A[0][1] = Math.sin(angle[0]);
            A[1][0] = -A[0][1];
            bodyFrame[0].E(A[0]);
            bodyFrame[1].E(A[1]);
            needToUpdateA = false;
        }
     //   public double[][] rotationMatrix() {return A;}
        public void convertToBodyFrame(Vector v) {
            if(needToUpdateA) updateRotationMatrix();
            double x = A[0][0]*v.x + A[0][1]*v.y;
            v.y = A[1][0]*v.x + A[1][1]*v.y;
            v.x = x;
        }
        public void convertToSpaceFrame(Vector v) {
            if(needToUpdateA) updateRotationMatrix();
            double x = A[0][0]*v.x + A[1][0]*v.y;
            v.y = A[0][1]*v.x + A[1][1]*v.y;
            v.x = x;
        }
        public void convertToBodyFrame(Space.Vector v) {convertToBodyFrame((Vector)v);}
        public void convertToSpaceFrame(Space.Vector v) {convertToSpaceFrame((Vector)v);}
    }

    /*public static class Orientation extends Space.Orientation {
        public final Vector[] eArray = new Vector[] {new Vector(1.0,0.0,0.0)};
        public final Vector L = new Vector(); //angular momentum
        public final Vector e = eArray[0];
        private final Vector[] bodyFrame = new Vector[] {new Vector(1.0,0.0,0.0), 
                                                         new Vector(0.0,1.0,0.0),
                                                         new Vector(0.0,0.0,1.0)};
        public final double[] angle = new double[1];
        public Space.Vector[] direction() {return eArray;}
        public double[] angle() {angle[0] = Math.atan2(e.y,e.x); return angle;}
        public Space.Vector momentum() {return L;}
        public Space.Vector[] bodyFrame() {return bodyFrame;} 
        public void freeFlight(double I) {//motion of a spherical top
        }
    }
    */
    
    public static abstract class Boundary extends Space.Boundary {
        public static class Type extends Space.Boundary.Type {
            private Type(String label) {super(label);}
            public Constants.TypedConstant[] choices() {return TYPES;}
        }
        public static final String[] TAGS = {"None", "Periodic Square", "Sliding Brick"};
        public static final Type NONE = new Type("None");
        public static final Type PERIODIC_SQUARE = new Type("Periodic Square");
        public static final Type SLIDING_BRICK = new Type("Sliding Brick");
        public static final Type[] TYPES = {NONE,PERIODIC_SQUARE,SLIDING_BRICK};
        public Boundary() {super();}
        public Boundary(Phase p) {super(p);}
        public abstract void nearestImage(Vector dr);
        public abstract void centralImage(Vector r);
        public abstract void centralImage(Coordinate c);
    }
    
    
    public static final class BoundaryNone extends Boundary {
        private final Vector temp = new Vector();
        private final double[][] shift0 = new double[0][D];
        public final Vector dimensions = new Vector();
        public final Space.Vector dimensions() {return dimensions;}
        public static final Random random = new Random();
        public BoundaryNone() {super();}
        public BoundaryNone(Phase p) {super(p);}
        public Space.Boundary.Type type() {return Boundary.NONE;}
        public void nearestImage(Space.Vector dr) {}
        public void centralImage(Space.Vector r) {}
        public void nearestImage(Vector dr) {}
        public void centralImage(Vector r) {}
        public void centralImage(Coordinate c) {}
        public double volume() {return Double.MAX_VALUE;}
        public void inflate(double s) {}
        public double[][] imageOrigins(int nShells) {return new double[0][D];}
        public double[][] getOverflowShifts(Space.Vector rr, double distance) {return shift0;}
        public Space.Vector randomPosition() {
            temp.x = random.nextDouble();
            temp.y = random.nextDouble();
            temp.z = random.nextDouble();
            return temp;
        }
        public void draw(Graphics g, int[] origin, double scale) {}
    }//end of BoundaryNone
        
    protected static class BoundaryPeriodicSquare extends Boundary implements Space.Boundary.Periodic  {
        private final Vector temp = new Vector();
        public static final Random random = new Random();
        private static Space.Tensor zilch = new Tensor();
        private double[][] shift0 = new double[0][D];
        private double[][] shift1 = new double[1][D];
        private double[][] shift3 = new double[3][D];
        public BoundaryPeriodicSquare() {this(Default.BOX_SIZE,Default.BOX_SIZE,Default.BOX_SIZE);}
        public BoundaryPeriodicSquare(Phase p) {this(p,Default.BOX_SIZE,Default.BOX_SIZE,Default.BOX_SIZE);}
        public BoundaryPeriodicSquare(Phase p, double lx, double ly, double lz) {super(p);dimensions.x=lx; dimensions.y=ly; dimensions.z=lz;}
        public BoundaryPeriodicSquare(double lx, double ly, double lz) {super();dimensions.x=lx; dimensions.y=ly; dimensions.z=lz;}
        public Space.Boundary.Type type() {return Boundary.PERIODIC_SQUARE;}
        public final Vector dimensions = new Vector();
        public final Space.Vector dimensions() {return dimensions;}
        public Space.Vector randomPosition() {
            temp.x = dimensions.x*random.nextDouble();
            temp.y = dimensions.y*random.nextDouble();
            temp.z = dimensions.z*random.nextDouble();
            return temp;
        }
        public void nearestImage(Space.Vector dr) {nearestImage((Vector) dr);}
        public void nearestImage(Vector dr) {
            dr.x -= dimensions.x*((dr.x > 0.0) ? Math.floor(dr.x/dimensions.x + 0.5) : Math.ceil(dr.x/dimensions.x - 0.5));
            dr.y -= dimensions.y*((dr.y > 0.0) ? Math.floor(dr.y/dimensions.y + 0.5) : Math.ceil(dr.y/dimensions.y - 0.5));
            dr.z -= dimensions.z*((dr.z > 0.0) ? Math.floor(dr.z/dimensions.z + 0.5) : Math.ceil(dr.z/dimensions.z - 0.5));
        }
        public void centralImage(Coordinate c) {centralImage(c.r);}
        public void centralImage(Space.Vector r) {centralImage((Vector) r);}
        public void centralImage(Vector r) {
            r.x -= dimensions.x* ((r.x>0) ? Math.floor(r.x/dimensions.x) : Math.ceil(r.x/dimensions.x - 1.0));
            r.y -= dimensions.y *((r.y>0) ? Math.floor(r.y/dimensions.y) : Math.ceil(r.y/dimensions.y - 1.0));
            r.z -= dimensions.z *((r.z>0) ? Math.floor(r.z/dimensions.z) : Math.ceil(r.z/dimensions.z - 1.0));
        }
        public void inflate(double scale) {dimensions.TE(scale);}
        public double volume() {return dimensions.x*dimensions.y*dimensions.z;}
                
        public void draw(Graphics g, int[] origin, double scale) {
            g.setColor(Color.gray);
            double toPixels = scale*BaseUnit.Length.Sim.TO_PIXELS;
            g.drawRect(origin[0], origin[1], (int)(toPixels*dimensions.component(0))-1, (int)(toPixels*dimensions.component(1))-1);
        }
        
        /**
         * imageOrigins and getOverFlowShifts are both probably incorrect, if they are
         * even completed.  They should definitely be checked before being implemented.
         */
        
        public double[][] imageOrigins(int nShells) {
            int nImages = (2*nShells+1)*(2*nShells+1)-1;
            double[][] origins = new double[nImages][D];
            int k = 0;
            for (int i=-nShells; i<=nShells; i++) {
                for (int j=-nShells; j<=nShells; j++) {
                    for (int m=-nShells; m<=nShells; m++) {
                        if ((i==0 && j==0) && m==0 ) {continue;}
                        origins[k][0] = i*dimensions.x;
                        origins[k][1] = j*dimensions.y;
                        origins[k][2] = m*dimensions.z;
                        k++;
                    }
                }
            }
            return origins;
        }
        
        
        //this was copied directly from space2d just to have something here.  needs to be changed to work in 3d.
        public double[][] getOverflowShifts(Space.Vector rr, double distance) {
            Vector r = (Vector)rr;
            int shiftX = 0;
            int shiftY = 0;
            if(r.x-distance < 0.0) {shiftX = +1;}
            else if(r.x+distance > dimensions.x) {shiftX = -1;}
            
            if(r.y-distance < 0.0) {shiftY = +1;}
            else if(r.y+distance > dimensions.y) {shiftY = -1;}
            
            if(shiftX == 0) {
                if(shiftY == 0) {
                    return shift0;
                }
                else {
                    shift1[0][0] = 0.0;
                    shift1[0][1] = shiftY*dimensions.y;
                    return shift1;
                }
            }
            else { //shiftX != 0
                if(shiftY == 0) {
                    shift1[0][0] = shiftX*dimensions.x;
                    shift1[0][1] = 0.0;
                    return shift1;
                }
                else {
                    shift3[0][0] = shiftX*dimensions.x;
                    shift3[0][1] = 0.0;
                    shift3[1][0] = 0.0;
                    shift3[1][1] = shiftY*dimensions.y;
                    shift3[2][0] = shift3[0][0];
                    shift3[2][1] = shift3[1][1];
                    return shift3;
                }
            }
        }
    }
    
    public static final class BoundarySlidingBrick extends BoundaryPeriodicSquare {
        private double gamma = 0.0;
        private double delvx;
        private IntegratorMD.ChronoMeter timer;
        public BoundarySlidingBrick() {super();}
        public BoundarySlidingBrick(Phase p) {super(p);}
        public Space.Boundary.Type type() {return Boundary.SLIDING_BRICK;}
        public void setShearRate(double g) {gamma = g; computeDelvx();}
        public double getShearRate() {return gamma;}
        private void computeDelvx() {delvx = gamma*dimensions.y;}
        
        public void setTimer(IntegratorMD.ChronoMeter t) {timer = t;}
        
        public void nearestImage(Vector dr) {
            double delrx = delvx*timer.currentValue();
            double cory = ((dr.y > 0.0) ? Math.floor(dr.y/dimensions.y + 0.5) : Math.ceil(dr.y/dimensions.y - 0.5));
            double corz = ((dr.z > 0.0) ? Math.floor(dr.z/dimensions.z + 0.5) : Math.ceil(dr.z/dimensions.z - 0.5));
            dr.x -= cory*delrx;
            dr.x -= dimensions.x*((dr.x > 0.0) ? Math.floor(dr.x/dimensions.x + 0.5) : Math.ceil(dr.x/dimensions.x - 0.5));
            dr.y -= dimensions.y * cory;
            dr.z -= dimensions.z * corz;
        }
        
        public void centralImage(Vector r) {
            double delrx = delvx*timer.currentValue();
            double cory = ((r.y > 0.0) ? Math.floor(r.y/dimensions.y) : Math.ceil(r.y/dimensions.y - 1.));
            double corz = ((r.z > 0.0) ? Math.floor(r.z/dimensions.z) : Math.ceil(r.z/dimensions.z - 1.));
            r.x -= cory*delrx;
            r.x -= dimensions.x * ((r.x >= 0.0) ? Math.floor(r.x/dimensions.x) : Math.ceil(r.x/dimensions.x - 1.0));
            r.y -= dimensions.y * cory;
            r.z -= dimensions.z * corz;
        }
        
        public void centralImagine(Coordinate c) {
            Vector r = c.r;
            double cory = ((r.y > 0.0) ? Math.floor(r.y/dimensions.y) : Math.ceil(r.y/dimensions.y-1.0));
            double corx = ((r.x > 0.0) ? Math.floor(r.x/dimensions.x) : Math.ceil(r.x/dimensions.x-1.0));
            double corz = ((r.z > 0.0) ? Math.floor(r.z/dimensions.z) : Math.ceil(r.z/dimensions.z-1.0));
            if (corz == 0.0 && (cory == 0.0 && corz == 0.0)) return;
            double delrx = delvx*timer.currentValue();
            Vector p = c.p;
            r.x -= cory*delrx;
            r.x -= dimensions.x * corx;
            r.y -= dimensions.y * cory;
            r.z -= dimensions.z * corz;
            p.x -= corx*delvx;
        }
        
        public double[][] imageOrigins(int nShells) {
            int nImages = (2*nShells + 1)*(2*nShells + 1)-1;
            double[][] origins = new double[nImages][D];
            int k = 0;
            for (int i=-nShells; i <= nShells; i++) {
                for (int j=-nShells; j <= nShells; j++) {
                    if(i==0 && j==0) {continue;}
                    origins[k][0] = i*dimensions.x + j*delvx*timer.currentValue();
                    origins[k][1] = j*dimensions.y;
                    k++;
                }
            }
            return origins;
        }
    }
}