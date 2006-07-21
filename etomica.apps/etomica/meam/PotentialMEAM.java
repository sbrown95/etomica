/*
 * Created on May 8, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package etomica.meam;

import etomica.atom.AtomLeaf;
import etomica.atom.AtomSet;
import etomica.phase.Phase;
import etomica.potential.PotentialN;
import etomica.potential.PotentialSoft;
import etomica.space.NearestImageTransformer;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Vector3D;
import etomica.species.Species;
import etomica.util.Arrays;

/**
 * @author ub2092
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PotentialMEAM extends PotentialN implements PotentialSoft {
	
	public PotentialMEAM(Space space) {
		super(space);
    }
	
	
	 public void setParameters(Species s, ParameterSetMEAM p) {
		 int index = s.getFactory().getType().getIndex();
		 if(index > parameters.length) {
			 parameters = (ParameterSetMEAM[])Arrays.resizeArray(parameters, index+1);
		 }
		 parameters[index] = p;
	 }

	/* (non-Javadoc)
	 * @see etomica.potential.Potential#getRange()
	 */
	public double getRange() {
		//from MEAMP2
		return Double.POSITIVE_INFINITY;
	}
	
	double jcut = 4.0; //Sn
	//double jcut = 3.0; //Cu
	double kcut = jcut * 1.14;
	
	
	public void calcSums(AtomSet atoms) {
		
		for (int i = 0; i < sum.length; i++) {
    		sum[i] = 0;
		}
		
		for(int j = 1; j < atoms.count(); j++) {
    	
			AtomLeaf atom0 = (AtomLeaf)atoms.getAtom(0);
			AtomLeaf atomj = (AtomLeaf)atoms.getAtom(j);
			ParameterSetMEAM pi = parameters[atom0.type.getIndex()];
			ParameterSetMEAM pj = parameters[atomj.type.getIndex()];
			rij.Ev1Mv2(atomj.coord.position(), atom0.coord.position());
			nearestImageTransformer.nearestImage(rij);
			double r = Math.sqrt(rij.squared());
        
			if (r > jcut) continue; 
        
			/**To determine amount of screening between atoms i and j 
			* by any atom k which may be between them.
			*/
			
			double Sij = 1.0;
			for(int k = 1; k < atoms.count(); k++) {
				AtomLeaf atomk = (AtomLeaf) atoms.getAtom(k);
				if (k == j) continue;
				rik.Ev1Mv2(atomk.coord.position(), atom0.coord.position());
				nearestImageTransformer.nearestImage(rik);
				double ik = Math.sqrt(rik.squared());
				if (ik > r*1.14) continue;
        	
				double anglekij = Math.toDegrees(Math.acos(
        			((rij.x(0)*rik.x(0)) + 
        			 (rij.x(1)*rik.x(1)) + 
					 (rij.x(2)*rik.x(2)))
					 /(r*ik)));
				if (anglekij >= 90) continue;
        	
				rkj.Ev1Mv2(atomk.coord.position(), atomj.coord.position());
				nearestImageTransformer.nearestImage(rkj);
				double kj = Math.sqrt(rkj.squared());
        	
				//from Baskes (1997)
				double xik = (ik/r)*(ik/r);
				double xjk = (kj/r)*(kj/r);
				double C = ((2.0*(xik + xjk)) - ((xik - xjk)*(xik - xjk))- 1.0)/
					   (1.0 - ((xik - xjk)*(xik - xjk)));
				if (C < 0) continue; //- C forms hyperbola, not ellipse
				
				double Sijk;
				if (C <= pi.Cmin) { 
					Sij = 0;
					break; // break out of for loop over k atoms
				}
				else if (C >= pi.Cmax) { //Sijk = 1, value of Sij won't change
        			continue; //continue to next k atom in for loop
        		}
				else {
					double q = ((C - pi.Cmin)/(pi.Cmax - pi.Cmin));
        			Sijk = (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
						  *(1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)));
				}
        	
				Sij *= Sijk;
		
			} // exit for loop over k atoms
	
			if (Sij == 0) continue; // continue to next j atom in for loop  
   
			/** When the atoms in the pair being examined are different elements,
			* the rhoj terms are not equivalent for both, because the constants
			* for the elements in the expression for rhoj differ. This class 
			* functions for unmixed pairs of atoms only.
			*/
			
			double rhoj0 = pj.rhoScale * 
						Math.exp(-pj.beta0 * ((r/pj.r0) - 1.0)) * Sij;
			double rhoj1 = pj.rhoScale * 
						Math.exp(-pj.beta1 * ((r/pj.r0) - 1.0)) * Sij;
			double rhoj2 = pj.rhoScale * 
						Math.exp(-pj.beta2 * ((r/pj.r0) - 1.0)) * Sij;
			double rhoj3 = pj.rhoScale * 
						Math.exp(-pj.beta3 * ((r/pj.r0) - 1.0)) * Sij;
	
			unitVector.E(rij);
			unitVector.normalize();
    
			double x = unitVector.x(0);
			double y = unitVector.x(1);
			double z = unitVector.x(2);
    
			sum[RHOj0] += rhoj0;

			sum[RHOj1x] += rhoj1 * x;
			sum[RHOj1y] += rhoj1 * y;
			sum[RHOj1z] += rhoj1 * z;  	
    
			sum[RHOj2xx] += rhoj2 * x * x;
			sum[RHOj2xy] += rhoj2 * x * y;
			sum[RHOj2xz] += rhoj2 * x * z;
			sum[RHOj2yy] += rhoj2 * y * y;
			sum[RHOj2yz] += rhoj2 * y * z;
			sum[RHOj2zz] += rhoj2 * z * z;
			sum[RHOj2] += rhoj2;
    
			sum[RHOj3xxx] += rhoj3 * x * x * x;
			sum[RHOj3xxy] += rhoj3 * x * x * y;
			sum[RHOj3xxz] += rhoj3 * x * x * z;
			sum[RHOj3xyy] += rhoj3 * x * y * y;
			sum[RHOj3xyz] += rhoj3 * x * y * z;
			sum[RHOj3xzz] += rhoj3 * x * z * z;
			sum[RHOj3yyy] += rhoj3 * y * y * y;
			sum[RHOj3yyz] += rhoj3 * y * y * z;
			sum[RHOj3yzz] += rhoj3 * y * z * z;
			sum[RHOj3zzz] += rhoj3 * z * z * z;
    
			sum[T1RHOj0] += rhoj0 * pi.t1;
			sum[T2RHOj0] += rhoj0 * pi.t2;
			sum[T3RHOj0] += rhoj0 * pi.t3;
    
			//to calculate the repulsive pair potential, phi
			//Should rhoj0 within phi contain Sij? Phi itself is multiplied 
			//by Sij...
			double rhoj0Ref = pj.rhoScale * Math.exp(-pj.beta0 * ((r/pj.r0) - 1.0));
			double rhoiRef = pi.Z * rhoj0Ref;   //FCC reference structure, Z = 12
			double a = pi.alpha * ((r/pi.r0) - 1.0);
			double EuRef = - pi.Ec * (1.0 + a) * Math.exp(-a);
			double FRef = pi.A * pi.Ec * (rhoiRef/pi.Z) * Math.log(rhoiRef/pi.Z);
			sum[PHI] += ((2.0/pi.Z) * (EuRef - FRef)) * Sij;
		} // exit for loop over j atoms
	} // exit calcSums()

	/** The following methods are not called until after calcSums() is called in
	 * energy() and gradient(), so we do not need to call calcSums() in these 
	 * methods.
	 */
	
	double rhoi0() {
		return sum[RHOj0]; //
	}
    
    protected double rhoi1() {
    	return Math.sqrt( (sum[RHOj1x] * sum[RHOj1x])
				+ (sum[RHOj1y] * sum[RHOj1y])
				+ (sum[RHOj1z] * sum[RHOj1z]));
    }
   
    protected double rhoi2() {
    	return Math.sqrt((sum[RHOj2xx] * sum[RHOj2xx])
			+ (2.0 * sum[RHOj2xy] * sum[RHOj2xy])
			+ (2.0 * sum[RHOj2xz] * sum[RHOj2xz])
			+ (sum[RHOj2yy] * sum[RHOj2yy]) 
			+ (2.0 * sum[RHOj2yz] * sum[RHOj2yz])
			+ (sum[RHOj2zz] * sum[RHOj2zz]) 
			- ((1.0/3.0) * sum[RHOj2] * sum[RHOj2]));
    }
    
    protected double rhoi3() {
    	return Math.sqrt( 
    			   (sum[RHOj3xxx] * sum[RHOj3xxx])
			+(3.0 * sum[RHOj3xxy] * sum[RHOj3xxy])
			+(3.0 * sum[RHOj3xxz] * sum[RHOj3xxz])
			+(3.0 * sum[RHOj3xyy] * sum[RHOj3xyy])
			+(6.0 * sum[RHOj3xyz] * sum[RHOj3xyz])
			+(3.0 * sum[RHOj3xzz] * sum[RHOj3xzz])
			+      (sum[RHOj3yyy] * sum[RHOj3yyy])
			+(3.0 * sum[RHOj3yyz] * sum[RHOj3yyz])
			+(3.0 * sum[RHOj3yzz] * sum[RHOj3yzz])
			+      (sum[RHOj3zzz] * sum[RHOj3zzz]));
    }
    
    protected double tav1() {
    	return sum[T1RHOj0] / sum[RHOj0];
    }

    protected double tav2() {
    	return sum[T2RHOj0] / sum[RHOj0];
    }
    
    protected double tav3() {
    	return sum[T3RHOj0] / sum[RHOj0];
    }
    
    protected double gamma() {
    	double rhoi0 = rhoi0(), rhoi1 = rhoi1(), rhoi2 = rhoi2(), rhoi3 = rhoi3(),
			tav1 = tav1(), tav2 = tav2(), tav3 = tav3();
    	return (tav1 * (rhoi1/rhoi0) * (rhoi1/rhoi0))
			+ (tav2 * (rhoi2/rhoi0) * (rhoi2/rhoi0))
			+ (tav3 * (rhoi3/rhoi0) * (rhoi3/rhoi0));
    }

    protected double rhoi() {
    	double rhoi0 = rhoi0(), gamma = gamma();
    	return (2.0 * rhoi0) / (1.0 + Math.exp(-gamma)); //Sn
    	//return rhoi0 * Math.sqrt(1.0 + gamma); //Cu
    }

	/* (non-Javadoc)
	 * @see etomica.potential.Potential#energy(etomica.atom.AtomSet)
	 */
	public double energy(AtomSet atoms) {
		AtomLeaf atom0 = (AtomLeaf)atoms.getAtom(0);
		ParameterSetMEAM pi = parameters[atom0.type.getIndex()];
		calcSums(atoms);
		double rhoi = rhoi();
		double F = pi.A * pi.Ec * (rhoi/pi.Z) * Math.log(rhoi/pi.Z);
		return F + (0.5*sum[PHI]);
	}

	/* (non-Javadoc)
	 * @see etomica.potential.Potential#setPhase(etomica.phase.Phase)
	 */
	public void setPhase(Phase phase) {
		nearestImageTransformer = phase.getBoundary();
	}

	/* (non-Javadoc)
	 * @see etomica.potential.PotentialSoft#virial(etomica.atom.AtomSet)
	 */
	public double virial(AtomSet atoms) {
		return 0.0;
	}

	/* (non-Javadoc)
	 * @see etomica.potential.PotentialSoft#gradient(etomica.atom.AtomSet)
	 */
	public Vector[] gradient(AtomSet atoms) {
		if (atoms.count() > gnEi.length) {
			gnEi = new Vector3D[atoms.count()];
			for (int i = 0; i < atoms.count(); i++) {
				gnEi[i] = (Vector3D)space.makeVector();
			}
		}
		
		else {
			for (int i = 0; i < atoms.count(); i++) {
			gnEi[i].E(0);
			}
		}

        calcSums(atoms);
        
        double rhoi0 = rhoi0(), rhoi1 = rhoi1(), rhoi2 = rhoi2(), rhoi3 = rhoi3(),
			tav1 = tav1(), tav2 = tav2(), tav3 = tav3(),
			gamma = gamma(), rhoi = rhoi();
        
        AtomLeaf atom0 = (AtomLeaf)atoms.getAtom(0);
		ParameterSetMEAM pi = parameters[atom0.type.getIndex()];
        
        sumGiRhoj0.E(0); 
        sumGiRhoj1x.E(0); sumGiRhoj1y.E(0); sumGiRhoj1z.E(0); 
        sumGiRhoj2xx.E(0); sumGiRhoj2xy.E(0); sumGiRhoj2xz.E(0); 
        sumGiRhoj2yy.E(0); sumGiRhoj2yz.E(0); sumGiRhoj2zz.E(0);
        sumGiRhoj2.E(0); 
        sumGiRhoj3xxx.E(0); sumGiRhoj3xxy.E(0); sumGiRhoj3xxz.E(0); 
        sumGiRhoj3xyy.E(0); sumGiRhoj3xyz.E(0); sumGiRhoj3xzz.E(0); 
        sumGiRhoj3yyy.E(0); sumGiRhoj3yyz.E(0); sumGiRhoj3yzz.E(0); 
        sumGiRhoj3zzz.E(0); 
        sumt1GiRhoj0.E(0); sumt2GiRhoj0.E(0);  sumt3GiRhoj0.E(0); 
        sumGiPhi.E(0); 
        
        for(int n = 1; n < atoms.count(); n++) {
        	AtomLeaf atomn = (AtomLeaf)atoms.getAtom(n);
            rin.Ev1Mv2(atomn.coord.position(), atom0.coord.position());
            nearestImageTransformer.nearestImage(rin);
            double in = Math.sqrt(rin.squared());
            
            if (in > kcut) continue; //only consider n that could be j or k to i
            
            /** We must reset all of the gradients with respect to j, used in
            * calculation of gradient-with-respect-to-n terms, to be the zero 
            * vector.  If an atom n is only a k atom to atom i, the gj terms will 
            * not be calculated for this n, and, if we don't reset the gj terms 
            * to the zero vector as we do below, the gj values for the previous 
            * n will be used.
            */
            
            gjPhi.E(0);
            gjRhoj0.E(0);
            gjRhoj1x.E(0); gjRhoj1y.E(0); gjRhoj1z.E(0);
            gjRhoj2xx.E(0); gjRhoj2xy.E(0); gjRhoj2xz.E(0);
            gjRhoj2yy.E(0); gjRhoj2yz.E(0); gjRhoj2zz.E(0); 
            gjRhoj2.E(0);
            gjRhoj3xxx.E(0); gjRhoj3xxy.E(0); gjRhoj3xxz.E(0);
            gjRhoj3xyy.E(0); gjRhoj3xyz.E(0); gjRhoj3xzz.E(0);
            gjRhoj3yyy.E(0); gjRhoj3yyz.E(0); gjRhoj3yzz.E(0); 
            gjRhoj3zzz.E(0);
            t1GjRhoj0.E(0); t2GjRhoj0.E(0); t3GjRhoj0.E(0);
            
            sumGkPhi.E(0);
            sumGkRhoj0.E(0); 
            sumGkRhoj1x.E(0); sumGkRhoj1y.E(0); sumGkRhoj1z.E(0);
            sumGkRhoj2xx.E(0); sumGkRhoj2xy.E(0); sumGkRhoj2xz.E(0);
            sumGkRhoj2yy.E(0); sumGkRhoj2yz.E(0); sumGkRhoj2zz.E(0); 
            sumGkRhoj2.E(0);
            sumGkRhoj3xxx.E(0); sumGkRhoj3xxy.E(0); sumGkRhoj3xxz.E(0);
            sumGkRhoj3xyy.E(0); sumGkRhoj3xyz.E(0); sumGkRhoj3xzz.E(0);
            sumGkRhoj3yyy.E(0); sumGkRhoj3yyz.E(0); sumGkRhoj3yzz.E(0); 
            sumGkRhoj3zzz.E(0);
            sumt1GkRhoj0.E(0); sumt2GkRhoj0.E(0); sumt3GkRhoj0.E(0);
            
            
            //Here we test to see if n qualifies as a j atom for atom i.
            if (in <= jcut) {
            
            	rij.E(rin); double ij = in;
            	ParameterSetMEAM pj = parameters[atomn.type.getIndex()];
            
            	double Sij = 1.0; giSij.E(0); gjSij.E(0);
            	for(int k = 1; k < atoms.count(); k++) {
            		AtomLeaf atomk = (AtomLeaf) atoms.getAtom(k);
            		if (k == n) continue; // continue to next k atom
            		rik.Ev1Mv2(atomk.coord.position(), atom0.coord.position());
            		nearestImageTransformer.nearestImage(rik);
            		double ik = Math.sqrt(rik.squared());
            		if (ik > ij*1.14) continue; // continue to next k atom
            	
            		double anglekij = Math.toDegrees(Math.acos(
            			((rij.x(0)*rik.x(0)) + 
            			 (rij.x(1)*rik.x(1)) + 
						 (rij.x(2)*rik.x(2)))
						 /(ij*ik)));
            		if (anglekij >= 90) continue; // continue to next k atom
            	
            		rkj.Ev1Mv2(atomk.coord.position(), atomn.coord.position());
            		nearestImageTransformer.nearestImage(rkj);
            		double kj = Math.sqrt(rkj.squared());
            	
            		//from Baskes (1997)
            		double xik = (ik/ij)*(ik/ij);
            		double xkj = (kj/ij)*(kj/ij);
            		double C = ( (2.0*(xik + xkj)) - 
            					 ( (xik - xkj)*(xik - xkj) )- 1.0 ) / 
							   (1.0 - ((xik - xkj)*(xik - xkj)));
            		if (C < 0) continue; // - C does not form ellipse
            		double q = ((C - p.Cmin)/(p.Cmax - p.Cmin));
            		double Sijk;
            		if (C <= p.Cmin) { 
            			Sij = 0;
            			break; //break out of for loop over k atoms
            		}
            		else if (C >= p.Cmax) { //Sijk = 1, value of Sij won't change
            			continue; // continue to next k atom
            		}
            		else {
            			Sijk = (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
    						  *(1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)));
            		}
            	
            		giRij.Ea1Tv1(-1.0/ij, rij);
            		gjRij.Ea1Tv1(-1.0, giRij);
            	
            		giRik.Ea1Tv1(-1.0/ik, rik);
            		gjRik.E(0);
            	
            		giRkj.E(0);
            		gjRkj.Ea1Tv1(-1.0/kj, rkj);
            	
            		giXik.Ea1Tv1(-ik/(ij*ij), giRij);
            		giXik.PEa1Tv1(1.0/ij, giRik);
            		giXik.TE(2.0*ik/ij);
            	
            		giXkj.Ea1Tv1(-kj/(ij*ij), giRij);
            		giXkj.PEa1Tv1(1.0/ij, giRkj);
            		giXkj.TE(2.0*kj/ij);
            	
            		gjXik.Ea1Tv1(-ik/(ij*ij), gjRij);
            		gjXik.PEa1Tv1(1.0/ij, gjRik);
            		gjXik.TE(2.0*ik/ij);
            	
            		gjXkj.Ea1Tv1(-kj/(ij*ij), gjRij);
            		gjXkj.PEa1Tv1(1.0/ij, gjRkj);
            		gjXkj.TE(2.0*kj/ij);
            	
            		giC.Ea1Tv1( 1.0 + (xik - xkj)*(C - 1.0), giXik);
            		giC.PEa1Tv1(1.0 - (xik - xkj)*(C + 1.0), giXkj);
            		giC.TE( 2.0 / ( 1.0 - ((xik - xkj)*(xik - xkj)) ));
		    	
            		gjC.Ea1Tv1( 1.0 + (xik - xkj)*(C - 1.0), gjXik);
            		gjC.PEa1Tv1(1.0 - (xik - xkj)*(C + 1.0), gjXkj);
            		gjC.TE( 2.0 / ( 1.0 - ((xik - xkj)*(xik - xkj)) ));
		    	
            		giSijk.Ea1Tv1(8 * (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
				                    * ((1 - q)*(1 - q)*(1 - q))
									* (1 / (p.Cmax - p.Cmin)), giC);
		    	
            		gjSijk.Ea1Tv1(8 * (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
		                            * ((1 - q)*(1 - q)*(1 - q))
							        * (1 / (p.Cmax - p.Cmin)), gjC);
		    	
            		/** The Sij value used to calculate gradSij is that for 
            		 * previous k's, or, for the first k considered, 1.0.  The 
            		 * same goes for the giSij/gjSij values, except these are 
            		 * initialized as the zero vector...
            		 */
            		
            		giSij.TE(Sijk);
            		giSij.PEa1Tv1(Sij, giSijk);
		    	
            		gjSij.TE(Sijk);
            		gjSij.PEa1Tv1(Sij, gjSijk);
		    	
            		Sij *= Sijk;
            	} // exit loop over k for n = j
    	
            	if (Sij != 0) {
        	
	            	double rhoj0 = pj.rhoScale * 
								Math.exp(-pj.beta0 * ((ij/pj.r0) - 1.0)) * Sij;
	            	double rhoj1 = pj.rhoScale * 
								Math.exp(-pj.beta1 * ((ij/pj.r0) - 1.0)) * Sij;
	            	double rhoj2 = pj.rhoScale * 
								Math.exp(-pj.beta2 * ((ij/pj.r0) - 1.0)) * Sij;
	            	double rhoj3 = p.rhoScale * 
								Math.exp(-pj.beta3 * ((ij/pj.r0) - 1.0)) * Sij;
	        	
	            	unitVector.E(rij);
	            	unitVector.normalize();
	            
	            	double x = unitVector.x(0);
	            	double y = unitVector.x(1);
	            	double z = unitVector.x(2);
           
	            	// to calculate the repulsive pair potential, phi
	            	// Should rhoj0 within phi contain Sij? Phi itself is 
	            	// multiplied by Sij...
	            	// FCC reference structure, Z = 12
	            	double rhoj0Ref = pj.rhoScale * 
								Math.exp(-pj.beta0 * ( (ij/pj.r0) - 1.0) );
	            	double rhoiRef = pi.Z * rhoj0Ref;   
	            	double a = pi.alpha * ( (ij/pi.r0) - 1.0 );
	            	double EuRef = - pi.Ec * (1.0 + a) * Math.exp(-a);
	        		double FRef = pi.A * pi.Ec * (rhoiRef/pi.Z) * 
								Math.log(rhoiRef/pi.Z);
	        		double phi = ( (2.0/pi.Z) * (EuRef - FRef) ) * Sij;
	    	
	        		vector100.setX(0,1.0);
	        		vector010.setX(1,1.0);
	        		vector001.setX(2,1.0);
    	
	        		//Pair-wise terms to calculate giEi
	        		gix.Ea1Tv1(x/(ij*ij), rij);
	        		gix.PEa1Tv1(-1.0/ij, vector100);
	    	
	        		giy.Ea1Tv1(y/(ij*ij), rij);
	        		giy.PEa1Tv1(-1.0/ij, vector010);
	    	
	        		giz.Ea1Tv1(z/(ij*ij), rij);
	        		giz.PEa1Tv1(-1.0/ij, vector001);
	    	
	        		giRij.Ea1Tv1(-1.0/ij, rij);
	    	
	    			//giPhi
	        		giRhoj0Ref.Ea1Tv1(-rhoj0Ref*pj.beta0/pj.r0, giRij);
    	
	        		giRhoiRef.Ea1Tv1(pi.Z, giRhoj0Ref);
	    	
	        		giFRef.Ea1Tv1( (pi.A*pi.Ec/pi.Z)*
	        			(1.0 + Math.log(rhoiRef/pi.Z)), giRhoiRef);
	    	
	        		giERef.Ea1Tv1( (pi.Ec*pi.alpha*pi.alpha/pi.r0) * ((ij/pi.r0) - 1.0)
	        			*(Math.exp(-pi.alpha*((ij/pi.r0)-1.0))), giRij);
	    	
	        		giPhi.E(giERef);
	        		giPhi.ME(giFRef);
	        		giPhi.TE(2.0 * Sij /pi.Z);
	        		giPhi.PEa1Tv1(phi/Sij, giSij);
	        		sumGiPhi.PE(giPhi);

	        		giRhoj0.Ea1Tv1(rhoj0/Sij, giSij);
	        		giRhoj0.PEa1Tv1(-rhoj0*pj.beta0/(pj.r0), giRij);
	        		sumGiRhoj0.PE(giRhoj0);
	    	
	        		giRhoj1.Ea1Tv1(rhoj1/Sij, giSij);
	        		giRhoj1.PEa1Tv1(-rhoj1*pj.beta1/(pj.r0), giRij);
	    	
	        		giRhoj1x.Ea1Tv1(rhoj1, gix);
	        		giRhoj1x.PEa1Tv1(x, giRhoj1);
	        		sumGiRhoj1x.PE(giRhoj1x);
	
	        		giRhoj1y.Ea1Tv1(rhoj1, giy);
	        		giRhoj1y.PEa1Tv1(y, giRhoj1);
	        		sumGiRhoj1y.PE(giRhoj1y);
	
	        		giRhoj1z.Ea1Tv1(rhoj1, giz);
	        		giRhoj1z.PEa1Tv1(z, giRhoj1);
	        		sumGiRhoj1z.PE(giRhoj1z);
    	
	        		giRhoj2.Ea1Tv1(rhoj2/Sij, giSij);
	        		giRhoj2.PEa1Tv1(-rhoj2*pj.beta2/(pj.r0), giRij);
	        		sumGiRhoj2.PE(giRhoj2);
	
	        		giRhoj2xx.Ea1Tv1(2.0*rhoj2*x, gix);
	        		giRhoj2xx.PEa1Tv1(x*x, giRhoj2);
	        		sumGiRhoj2xx.PE(giRhoj2xx);
	
	        		giRhoj2xy.Ea1Tv1(rhoj2*x, giy);
	        		giRhoj2xy.PEa1Tv1(rhoj2*y, gix);
	        		giRhoj2xy.PEa1Tv1(x*y, giRhoj2);
	        		sumGiRhoj2xy.PE(giRhoj2xy);
	
	        		giRhoj2xz.Ea1Tv1(rhoj2*x, giz);
	        		giRhoj2xz.PEa1Tv1(rhoj2*z, gix);
	        		giRhoj2xz.PEa1Tv1(x*z, giRhoj2);
	        		sumGiRhoj2xz.PE(giRhoj2xz);

	        		giRhoj2yy.Ea1Tv1(2.0*rhoj2*y, giy);
	        		giRhoj2yy.PEa1Tv1(y*y, giRhoj2);
	        		sumGiRhoj2yy.PE(giRhoj2yy);
	
	        		giRhoj2yz.Ea1Tv1(rhoj2*y, giz);
	        		giRhoj2yz.PEa1Tv1(rhoj2*z, giy);
	        		giRhoj2yz.PEa1Tv1(y*z, giRhoj2);
	        		sumGiRhoj2yz.PE(giRhoj2yz);
	
	        		giRhoj2zz.Ea1Tv1(2.0*rhoj2*z, giz);
	        		giRhoj2zz.PEa1Tv1(z*z, giRhoj2);
	        		sumGiRhoj2zz.PE(giRhoj2zz);
	    	
	        		giRhoj3.Ea1Tv1(rhoj3/Sij, giSij);
	        		giRhoj3.PEa1Tv1(-rhoj3*pj.beta3/(pj.r0), giRij);
	
	        		giRhoj3xxx.Ea1Tv1(3.0*rhoj3*x*x, gix);
	        		giRhoj3xxx.PEa1Tv1(x*x*x, giRhoj3);
	        		sumGiRhoj3xxx.PE(giRhoj3xxx);
	    
	        		giRhoj3xxy.Ea1Tv1(rhoj3*x*x, giy);
	        		giRhoj3xxy.PEa1Tv1(2.0*rhoj3*x*y, gix);
	        		giRhoj3xxy.PEa1Tv1(x*x*y, giRhoj3);
	        		sumGiRhoj3xxy.PE(giRhoj3xxy);

	        		giRhoj3xxz.Ea1Tv1(rhoj3*x*x, giz);
	        		giRhoj3xxz.PEa1Tv1(2.0*rhoj3*x*z, gix);
	        		giRhoj3xxz.PEa1Tv1(x*x*z, giRhoj3);
	        		sumGiRhoj3xxz.PE(giRhoj3xxz);
	
	        		giRhoj3xyy.Ea1Tv1(2.0*rhoj3*x*y, giy);
	        		giRhoj3xyy.PEa1Tv1(rhoj3*y*y, gix);
	        		giRhoj3xyy.PEa1Tv1(x*y*y, giRhoj3);
	        		sumGiRhoj3xyy.PE(giRhoj3xyy);
	
	        		giRhoj3xyz.Ea1Tv1(rhoj3*x*y, giz);
	        		giRhoj3xyz.PEa1Tv1(rhoj3*x*z, giy);
	        		giRhoj3xyz.PEa1Tv1(rhoj3*y*z, gix);
	        		giRhoj3xyz.PEa1Tv1(x*y*z, giRhoj3);
	        		sumGiRhoj3xyz.PE(giRhoj3xyz);
	
	        		giRhoj3xzz.Ea1Tv1(2.0*rhoj3*x*z, giz);
	        		giRhoj3xzz.PEa1Tv1(rhoj3*z*z, gix);
	        		giRhoj3xzz.PEa1Tv1(x*z*z, giRhoj3);
	        		sumGiRhoj3xzz.PE(giRhoj3xzz);

	        		giRhoj3yyy.Ea1Tv1(3.0*rhoj3*y*y, giy);
	        		giRhoj3yyy.PEa1Tv1(y*y*y, giRhoj3);
	        		sumGiRhoj3yyy.PE(giRhoj3yyy);
	   
	        		giRhoj3yyz.Ea1Tv1(rhoj3*y*y, giz);
	        		giRhoj3yyz.PEa1Tv1(2.0*rhoj3*y*z, giy);
	        		giRhoj3yyz.PEa1Tv1(y*y*z, giRhoj3);
	        		sumGiRhoj3yyz.PE(giRhoj3yyz);
	    	
	        		giRhoj3yzz.Ea1Tv1(2.0*rhoj3*y*z, giz);
	        		giRhoj3yzz.PEa1Tv1(rhoj3*z*z, giy);
	        		giRhoj3yzz.PEa1Tv1(y*z*z, giRhoj3);
	        		sumGiRhoj3yzz.PE(giRhoj3yzz);
	    	
	        		giRhoj3zzz.Ea1Tv1(3.0*rhoj3*z*z, giz);
	        		giRhoj3zzz.PEa1Tv1(z*z*z, giRhoj3);
	        		sumGiRhoj3zzz.PE(giRhoj3zzz);
	    	
	        		t1GiRhoj0.Ea1Tv1(pi.t1, giRhoj0);
	        		sumt1GiRhoj0.PE(t1GiRhoj0);
	    	
	        		t2GiRhoj0.Ea1Tv1(pi.t2, giRhoj0);
	        		sumt2GiRhoj0.PE(t2GiRhoj0);
	    	
	        		t3GiRhoj0.Ea1Tv1(pi.t3, giRhoj0);
	        		sumt3GiRhoj0.PE(t3GiRhoj0);
    
	        		//Pair-wise terms required to calculate gnEi
	        		gjx.Ea1Tv1(-1.0, gix);
	        		gjy.Ea1Tv1(-1.0, giy);
	        		gjz.Ea1Tv1(-1.0, giz);
	        		gjRij.Ea1Tv1(-1.0, giRij);
	        		gjFRef.Ea1Tv1(-1.0, giFRef);
	        		gjERef.Ea1Tv1(-1.0, giERef);
	        		
	        		gjPhi.E(gjERef);
	        		gjPhi.ME(gjFRef);
	        		gjPhi.TE(2.0 * Sij /pi.Z);
	        		gjPhi.PEa1Tv1(phi/Sij, gjSij);
	    	
	        		gjRhoj0.Ea1Tv1(rhoj0/Sij, gjSij);
	        		gjRhoj0.PEa1Tv1(-rhoj0*pj.beta0/(pj.r0), gjRij);
	    	
	        		gjRhoj1.Ea1Tv1(rhoj1/Sij, gjSij);
	        		gjRhoj1.PEa1Tv1(-rhoj1*pj.beta1/(pj.r0), gjRij);
	    	
	        		gjRhoj1x.Ea1Tv1(rhoj1, gjx);
	        		gjRhoj1x.PEa1Tv1(x, gjRhoj1);
	    	
	        		gjRhoj1y.Ea1Tv1(rhoj1, gjy);
	        		gjRhoj1y.PEa1Tv1(y, gjRhoj1);
	    	
	        		gjRhoj1z.Ea1Tv1(rhoj1, gjz);
	        		gjRhoj1z.PEa1Tv1(z, gjRhoj1);
	    	
	        		gjRhoj2.Ea1Tv1(rhoj2/Sij, gjSij);
	        		gjRhoj2.PEa1Tv1(-rhoj2*pj.beta2/(pj.r0), gjRij);
	    	
	        		gjRhoj2xx.Ea1Tv1(2.0*rhoj2*x, gjx);
	        		gjRhoj2xx.PEa1Tv1(x*x, gjRhoj2);
	    	
	        		gjRhoj2xy.Ea1Tv1(rhoj2*x, gjy);
	        		gjRhoj2xy.PEa1Tv1(rhoj2*y, gjx);
	        		gjRhoj2xy.PEa1Tv1(x*y, gjRhoj2);
	    	
	        		gjRhoj2xz.Ea1Tv1(rhoj2*x, gjz);
	        		gjRhoj2xz.PEa1Tv1(rhoj2*z, gjx);
	        		gjRhoj2xz.PEa1Tv1(x*z, gjRhoj2);
	    	
	        		gjRhoj2yy.Ea1Tv1(2.0*rhoj2*y, gjy);
	        		gjRhoj2yy.PEa1Tv1(y*y, gjRhoj2);
	    	
	        		gjRhoj2yz.Ea1Tv1(rhoj2*y, gjz);
	        		gjRhoj2yz.PEa1Tv1(rhoj2*z, gjy);
	        		gjRhoj2yz.PEa1Tv1(y*z, gjRhoj2);
	    	
	        		gjRhoj2zz.Ea1Tv1(2.0*rhoj2*z, gjz);
	        		gjRhoj2zz.PEa1Tv1(z*z, gjRhoj2);
	    	
	        		gjRhoj3.Ea1Tv1(rhoj3/Sij, gjSij);
	        		gjRhoj3.PEa1Tv1(-rhoj3*pj.beta3/(pj.r0), gjRij);
	
	        		gjRhoj3xxx.Ea1Tv1(3.0*rhoj3*x*x, gjx);
	        		gjRhoj3xxx.PEa1Tv1(x*x*x, gjRhoj3);
	
	        		gjRhoj3xxy.Ea1Tv1(rhoj3*x*x, gjy);
	        		gjRhoj3xxy.PEa1Tv1(2.0*rhoj3*x*y, gjx);
	        		gjRhoj3xxy.PEa1Tv1(x*x*y, gjRhoj3);
	
	        		gjRhoj3xxz.Ea1Tv1(rhoj3*x*x, gjz);
	        		gjRhoj3xxz.PEa1Tv1(2.0*rhoj3*x*z, gjx);
	        		gjRhoj3xxz.PEa1Tv1(x*x*z, gjRhoj3);
	
	        		gjRhoj3xyy.Ea1Tv1(2.0*rhoj3*x*y, gjy);
	        		gjRhoj3xyy.PEa1Tv1(rhoj3*y*y, gjx);
	        		gjRhoj3xyy.PEa1Tv1(x*y*y, gjRhoj3);
	    	
	        		gjRhoj3xyz.Ea1Tv1(rhoj3*x*y, gjz);
	        		gjRhoj3xyz.PEa1Tv1(rhoj3*x*z, gjy);
	        		gjRhoj3xyz.PEa1Tv1(rhoj3*y*z, gjx);
	        		gjRhoj3xyz.PEa1Tv1(x*y*z, gjRhoj3);
	
	        		gjRhoj3xzz.Ea1Tv1(2.0*rhoj3*x*z, gjz);
	        		gjRhoj3xzz.PEa1Tv1(rhoj3*z*z, gjx);
	        		gjRhoj3xzz.PEa1Tv1(x*z*z, gjRhoj3);
	
	        		gjRhoj3yyy.Ea1Tv1(3.0*rhoj3*y*y, gjy);
	        		gjRhoj3yyy.PEa1Tv1(y*y*y, gjRhoj3);
	
	        		gjRhoj3yyz.Ea1Tv1(rhoj3*y*y, gjz);
	        		gjRhoj3yyz.PEa1Tv1(2.0*rhoj3*y*z, gjy);
	        		gjRhoj3yyz.PEa1Tv1(y*y*z, gjRhoj3);
	    	
	        		gjRhoj3yzz.Ea1Tv1(2.0*rhoj3*y*z, gjz);
	        		gjRhoj3yzz.PEa1Tv1(rhoj3*z*z, gjy);
	        		gjRhoj3yzz.PEa1Tv1(y*z*z, gjRhoj3);
	    	
	        		gjRhoj3zzz.Ea1Tv1(3.0*rhoj3*z*z, gjz);
	        		gjRhoj3zzz.PEa1Tv1(z*z*z, gjRhoj3);
	
	        		t1GjRhoj0.Ea1Tv1(pi.t1, gjRhoj0);
	   
	        		t2GjRhoj0.Ea1Tv1(pi.t2, gjRhoj0);
	    	
	        		t3GjRhoj0.Ea1Tv1(pi.t3, gjRhoj0);
            	} // exit if statement with condition that Sij != 0
            } // exit if statement with condition that n is a j atom of i
    	
            /** To consider n as a k atom, we must loop through neighbors j of i 
             * again.
             */
            
            for(int j = 1; j < atoms.count(); j++) {
        		AtomLeaf atomj = (AtomLeaf) atoms.getAtom(j);
        		ParameterSetMEAM pj = parameters[atomj.type.getIndex()];
        		//The k atom, n, must not be treated as one of the other j atoms.
        		if (j == n) continue; // continue to next j atom
        		rij.Ev1Mv2(atomj.coord.position(), atom0.coord.position());
        		nearestImageTransformer.nearestImage(rij);
        		double ij = Math.sqrt(rij.squared());
        		if (ij > jcut) continue; // continue to next j atom
        	
        		rik.E(rin);
	        	double ik = in;
	        	if (ik > ij*1.14) continue; // n won't impact this i-j interaction
        	
	        	double anglekij = Math.toDegrees(Math.acos(
        			((rij.x(0)*rik.x(0)) + 
        			 (rij.x(1)*rik.x(1)) + 
					 (rij.x(2)*rik.x(2)))
					 /(ij*ik)));
	        	if (anglekij >= 90) continue;
        	
	        	rkj.Ev1Mv2(atomn.coord.position(), atomj.coord.position());
	        	nearestImageTransformer.nearestImage(rkj);
	        	double kj = Math.sqrt(rkj.squared());
        	
	        	//from Baskes (1997)
	        	double xik = (ik/ij)*(ik/ij);
	        	double xkj = (kj/ij)*(kj/ij);
        	
	        	double C = ( (2.0*(xik + xkj)) - ((xik - xkj)*(xik - xkj))- 1.0 )
							/ (1.0 - ((xik - xkj)*(xik - xkj)));
	        	if (C < 0) continue;
	        	
	        	double q = ((C - p.Cmin)/(p.Cmax - p.Cmin));
	        	
	        	double Sijk;
	        	if (C <= p.Cmin) { 
	        		continue; 
	        		/** If Sijk equals 0, any gradient of Sijk, 
	        		 * including that with respect to k, will also be 0.  We
	        		 * should continue on to the next j atom, whose gkSijk may be 
	        		 * nonzero.  
	        		 */
	        	}
	        	else if (C >= p.Cmax) { 
	        	    continue; 
	        	    /** If Sijk equals 1, any gradient of Sijk, 
	        		 * including that with respect to k, will also be 0.  We
	        		 * should continue on to the next j atom, whose gkSijk may be 
	        		 * nonzero.  
	        		 */
	        	}
	        	else {
        			Sijk = (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
						  *(1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)));
	        	}
	        	
	        	// To calculate Sij. l is k != n.
	        	// We can start Sij out with the value for Sijk (k = n).
	        	double Sij = Sijk;
	        	for(int l = 1; l < atoms.count(); l++) {
	    			AtomLeaf atoml = (AtomLeaf) atoms.getAtom(l);
	    			if (l == j) continue;
	    			if (l == n) continue; //already including Sijk for n = k
	    			ril.Ev1Mv2(atoml.coord.position(), atom0.coord.position());
	    			nearestImageTransformer.nearestImage(ril);
	    			double il = Math.sqrt(ril.squared());
	    			if (il > ij*1.14) continue;
	            	
	    			double anglelij = Math.toDegrees(Math.acos(
	            		( (rij.x(0)*ril.x(0)) + (rij.x(1)*ril.x(1)) + 
	    				  (rij.x(2)*ril.x(2)) ) / (ij*il) ));
	    			if (anglelij >= 90) continue;
	            	
	    			rlj.Ev1Mv2(atoml.coord.position(), atomj.coord.position());
	    			nearestImageTransformer.nearestImage(rlj);
	    			double lj = Math.sqrt(rlj.squared());
	            	
	    			//from Baskes (1997)
	    			double xil = (il/ij)*(il/ij);
	    			double xjl = (lj/ij)*(lj/ij);
	    			double c = ( ( 2.0*(xil + xjl) ) - 
	    					     ( (xil - xjl)*(xil - xjl) ) - 1.0 ) /
	    					   ( 1.0 - ( (xil - xjl)*(xil - xjl) ) );
	    			if (c < 0) continue;
	            	
	    			double Sijl;
	    			if (c <= p.Cmin) { 
	    				Sij = 0;
	    				break; // break out of loop over k atoms for j!=n atom
	    			}
	    			else if (c >= p.Cmax) { //Sijl = 1, value of Sij won't change
	            		continue; // continue to next k!=n atom
	            	}
	    			else {
	    				double qq = ((C - p.Cmin)/(p.Cmax - p.Cmin));
	        			Sijl = (1 - ((1 - qq)*(1 - qq)*(1 - qq)*(1 - qq)))
							  *(1 - ((1 - qq)*(1 - qq)*(1 - qq)*(1 - qq)));
	    			}
	            	
	    			Sij *= Sijl;
	        		} // exit loop over k!=n for j!=n 
        	
	        	// An l (k!=n) atom may have made Sij = 0.
	        	if (Sij == 0) continue; //continue to next j!=n
	        	
	        	double rhoj0 = pj.rhoScale * 
							Math.exp(-pj.beta0 * ((ij/pj.r0) - 1.0)) * Sij;
	        	double rhoj1 = pj.rhoScale * 
							Math.exp(-pj.beta1 * ((ij/pj.r0) - 1.0)) * Sij;
	            double rhoj2 = pj.rhoScale * 
							Math.exp(-pj.beta2 * ((ij/pj.r0) - 1.0)) * Sij;
	        	double rhoj3 = pj.rhoScale * 
							Math.exp(-pj.beta3 * ((ij/pj.r0) - 1.0)) * Sij;
	        	
	        	unitVector.E(rij);
	            unitVector.normalize();
	            
	            double x = unitVector.x(0);
	            double y = unitVector.x(1);
	            double z = unitVector.x(2);
           
	            //to calculate the repulsive pair potential, phi
	            //Should rhoj0 within phi contain Sij? Phi itself is multiplied 
	            //by Sij...
	            //FCC reference structure, Z = 12
	            double rhoj0Ref = pj.rhoScale * 
							Math.exp(-pj.beta0 * ((ij/pj.r0) - 1.0));
	            double rhoiRef = pi.Z * rhoj0Ref;   
	            double a = pi.alpha * ((ij/p.r0) - 1.0);
	        	double EuRef = - pi.Ec * (1.0 + a) * Math.exp(-a);
	        	double FRef = pi.A * pi.Ec * (rhoiRef/pi.Z) * Math.log(rhoiRef/pi.Z);
	        	double phi = ((2.0/pi.Z) * (EuRef - FRef)) * Sij;
	        	
	        	gkRij.E(0);
	        	gkRik.Ea1Tv1(1.0/ik, rik); 
	        	gkRkj.Ea1Tv1(1.0/kj, rkj); 
	        	
	        	gkXik.Ea1Tv1(-ik/(ij*ij), gkRij);
	        	gkXik.PEa1Tv1(1.0/ij, gkRik);
	        	gkXik.TE(2.0*ik/ij);
	        	
	        	gkXkj.Ea1Tv1(-kj/(ij*ij), gkRij);
	        	gkXkj.PEa1Tv1(1.0/ij, gkRkj);
	        	gkXkj.TE(2.0*kj/ij);
	       
		    	gkC.Ea1Tv1( 1.0 + (xik - xkj)*(C - 1.0), gkXik);
		    	gkC.PEa1Tv1(1.0 - (xik - xkj)*(C + 1.0), gkXkj);
		    	gkC.TE( 2.0 / ( 1.0 - ((xik - xkj)*(xik - xkj)) ));
		    
		    	gkSijk.Ea1Tv1( 8 * (1 - ((1 - q)*(1 - q)*(1 - q)*(1 - q)))
	                             * ((1 - q)*(1 - q)*(1 - q))
						         * (1 / (p.Cmax - p.Cmin)), gkC);
		    	
		    	//We only consider one k atom - the k atom that is n
		    	gkSij.Ea1Tv1(Sij/Sijk, gkSijk);
		    	
		    	gkPhi.Ea1Tv1(phi/Sij, gkSij);
		    	sumGkPhi.PE(gkPhi);
		    	
		    	gkRhoj0.Ea1Tv1(rhoj0/Sij, gkSij);
		    	sumGkRhoj0.PE(gkRhoj0);
		    	
		    	gkRhoj1.Ea1Tv1(rhoj1/Sij, gkSij);
	
		    	gkRhoj2.Ea1Tv1(rhoj2/Sij, gkSij);
		    	sumGkRhoj2.PE(gkRhoj2);
	
		    	gkRhoj3.Ea1Tv1(rhoj3/Sij, gkSij);
		    	
		    	gkRhoj1x.Ea1Tv1(x, gkRhoj1);
		    	sumGkRhoj1x.PE(gkRhoj1x);
	    	
		    	gkRhoj1y.Ea1Tv1(y, gkRhoj1);
		    	sumGkRhoj1y.PE(gkRhoj1y);
	
		    	gkRhoj1z.Ea1Tv1(z, gkRhoj1);
		    	sumGkRhoj1z.PE(gkRhoj1z);
	
		    	gkRhoj2xx.Ea1Tv1(x*x, gkRhoj2);
		    	sumGkRhoj2xx.PE(gkRhoj2xx);
	
		    	gkRhoj2xy.Ea1Tv1(x*y, gkRhoj2);
		    	sumGkRhoj2xy.PE(gkRhoj2xy);
	
		    	gkRhoj2xz.Ea1Tv1(x*z, gkRhoj2);
		    	sumGkRhoj2xz.PE(gkRhoj2xz);
	
		    	gkRhoj2yy.Ea1Tv1(y*y, gkRhoj2);
		    	sumGkRhoj2yy.PE(gkRhoj2yy);
	
		    	gkRhoj2yz.Ea1Tv1(y*z, gkRhoj2);
		    	sumGkRhoj2yz.PE(gkRhoj2yz);

		    	gkRhoj2zz.Ea1Tv1(z*z, gkRhoj2);
		    	sumGkRhoj2zz.PE(gkRhoj2zz);
	
		    	gkRhoj3xxx.Ea1Tv1(x*x*x, gkRhoj3);
		    	sumGkRhoj3xxx.PE(gkRhoj3xxx);
		    
		    	gkRhoj3xxy.Ea1Tv1(x*x*y, gkRhoj3);
		    	sumGkRhoj3xxy.PE(gkRhoj3xxy);
	
		    	gkRhoj3xxz.Ea1Tv1(x*x*z, gkRhoj3);
		    	sumGkRhoj3xxz.PE(gkRhoj3xxz);
	
		    	gkRhoj3xyy.Ea1Tv1(x*y*y, gkRhoj3);
		    	sumGkRhoj3xyy.PE(gkRhoj3xyy);
	
		    	gkRhoj3xyz.Ea1Tv1(x*y*z, gkRhoj3);
		    	sumGkRhoj3xyz.PE(gkRhoj3xyz);
	    	
		    	gkRhoj3xzz.Ea1Tv1(x*z*z, gkRhoj3);
		    	sumGkRhoj3xzz.PE(gkRhoj3xzz);
	
		    	gkRhoj3yyy.Ea1Tv1(y*y*y, gkRhoj3);
		    	sumGkRhoj3yyy.PE(gkRhoj3yyy);
		   
		    	gkRhoj3yyz.Ea1Tv1(y*y*z, gkRhoj3);
		    	sumGkRhoj3yyz.PE(gkRhoj3yyz);
	    	
		    	gkRhoj3yzz.Ea1Tv1(y*z*z, gkRhoj3);
		    	sumGkRhoj3yzz.PE(gkRhoj3yzz);
		    	
		    	gkRhoj3zzz.Ea1Tv1(z*z*z, gkRhoj3);
		    	sumGkRhoj3zzz.PE(gkRhoj3zzz);
		    	
		    	t1GkRhoj0.Ea1Tv1(pi.t1, gkRhoj0);
		    	sumt1GkRhoj0.PE(t1GkRhoj0);
		    	
		    	t2GkRhoj0.Ea1Tv1(pi.t2, gkRhoj0);
		    	sumt2GkRhoj0.PE(t2GkRhoj0);
		    	
		    	t3GkRhoj0.Ea1Tv1(pi.t3, gkRhoj0);
		    	sumt3GkRhoj0.PE(t3GkRhoj0);
	        } //exit loop over j!=n atoms, with n as a k atom
	    	
	    	//multi-body terms, n as k is included
	    	
	    	sumGnPhi.E(sumGkPhi);
	    	sumGnPhi.PE(gjPhi);
	    	
			gnRhoi0.E(sumGkRhoj0);
	    	gnRhoi0.PE(gjRhoj0);
	    	
	    	gnRhoi1.Ea1Tv1(sum[RHOj1x], sumGkRhoj1x);
	    	gnRhoi1.PEa1Tv1(sum[RHOj1x], gjRhoj1x);
			gnRhoi1.PEa1Tv1(sum[RHOj1y], sumGkRhoj1y);
			gnRhoi1.PEa1Tv1(sum[RHOj1y], gjRhoj1y);
			gnRhoi1.PEa1Tv1(sum[RHOj1z], sumGkRhoj1z);
			gnRhoi1.PEa1Tv1(sum[RHOj1z], gjRhoj1z);
			gnRhoi1.TE(1.0/rhoi1);
			
			gnRhoi2.Ea1Tv1(sum[RHOj2xx], sumGkRhoj2xx);
			gnRhoi2.PEa1Tv1(sum[RHOj2xx], gjRhoj2xx);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2xy], sumGkRhoj2xy);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2xy], gjRhoj2xy);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2xz], sumGkRhoj2xz);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2xz], gjRhoj2xz);
			gnRhoi2.PEa1Tv1(sum[RHOj2yy], sumGkRhoj2yy);
			gnRhoi2.PEa1Tv1(sum[RHOj2yy], gjRhoj2yy);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2yz], sumGkRhoj2yz);
			gnRhoi2.PEa1Tv1(2.0 * sum[RHOj2yz], gjRhoj2yz);
			gnRhoi2.PEa1Tv1(sum[RHOj2zz], sumGkRhoj2zz);
			gnRhoi2.PEa1Tv1(sum[RHOj2zz], gjRhoj2zz);
			gnRhoi2.PEa1Tv1(-(1.0/3.0) * sum[RHOj2], sumGkRhoj2);
			gnRhoi2.PEa1Tv1(-(1.0/3.0) * sum[RHOj2], gjRhoj2);
			gnRhoi2.TE(1.0/rhoi2);
			
			gnRhoi3.Ea1Tv1( sum[RHOj3xxx], sumGkRhoj3xxx);
			gnRhoi3.PEa1Tv1(sum[RHOj3xxx], gjRhoj3xxx);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxy], sumGkRhoj3xxy);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxy], gjRhoj3xxy);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxz], sumGkRhoj3xxz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxz], gjRhoj3xxz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xyy], sumGkRhoj3xyy);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xyy], gjRhoj3xyy);
			gnRhoi3.PEa1Tv1(6.0 * sum[RHOj3xyz], sumGkRhoj3xyz);
			gnRhoi3.PEa1Tv1(6.0 * sum[RHOj3xyz], gjRhoj3xyz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xzz], sumGkRhoj3xzz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3xzz], gjRhoj3xzz);
			gnRhoi3.PEa1Tv1(sum[RHOj3yyy], sumGkRhoj3yyy);
			gnRhoi3.PEa1Tv1(sum[RHOj3yyy], gjRhoj3yyy);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3yyz], sumGkRhoj3yyz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3yyz], gjRhoj3yyz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3yzz], sumGkRhoj3yzz);
			gnRhoi3.PEa1Tv1(3.0 * sum[RHOj3yzz], gjRhoj3yzz);
			gnRhoi3.PEa1Tv1(sum[RHOj3zzz], sumGkRhoj3zzz);
			gnRhoi3.PEa1Tv1(sum[RHOj3zzz], gjRhoj3zzz);
			gnRhoi3.TE(1.0/rhoi3);
		
			gntav1.Ea1Tv1(-sum[T1RHOj0]/(rhoi0*rhoi0), gnRhoi0);
			gntav1.PEa1Tv1(1.0/rhoi0, sumt1GkRhoj0);
			gntav1.PEa1Tv1(1.0/rhoi0, t1GjRhoj0);
			
			gntav2.Ea1Tv1(-sum[T2RHOj0]/(rhoi0*rhoi0), gnRhoi0);
			gntav2.PEa1Tv1(1.0/rhoi0, sumt2GkRhoj0);
			gntav2.PEa1Tv1(1.0/rhoi0, t2GjRhoj0);
		
			gntav3.Ea1Tv1(-sum[T3RHOj0]/(rhoi0*rhoi0), gnRhoi0);
			gntav3.PEa1Tv1(1.0/rhoi0, sumt3GkRhoj0);
			gntav3.PEa1Tv1(1.0/rhoi0, t3GjRhoj0);
			
			gnGamma.Ea1Tv1( (-1.0/rhoi0)*( (tav1*rhoi1*rhoi1) + (tav2*rhoi2*rhoi2)
					+ (tav3*rhoi3*rhoi3) ), gnRhoi0);
			gnGamma.PEa1Tv1(tav1*rhoi1, gnRhoi1);
			gnGamma.PEa1Tv1(tav2*rhoi2, gnRhoi2);
			gnGamma.PEa1Tv1(tav3*rhoi3, gnRhoi3);
			gnGamma.TE(2.0);
			gnGamma.PEa1Tv1(rhoi1*rhoi1, gntav1);
			gnGamma.PEa1Tv1(rhoi2*rhoi2, gntav2);
			gnGamma.PEa1Tv1(rhoi3*rhoi3, gntav3);
			gnGamma.TE(1.0/(rhoi0*rhoi0));
			
			//Sn
			
			gnRhoi.Ea1Tv1(rhoi0*
						Math.exp(-gamma)/(1.0 + Math.exp(-gamma)), gnGamma);
			gnRhoi.PE(gnRhoi0);
			gnRhoi.TE(2.0/(1.0+Math.exp(-gamma)));
		
			//Cu
			/**
			gnRhoi.Ea1Tv1(rhoi0*0.5*Math.sqrt(1.0/(1.0 + gamma)), gnGamma);
			gnRhoi.PEa1Tv1(Math.sqrt(1.0+gamma), gnRhoi0);
		    **/
			
			gnF.Ea1Tv1( (pi.A*pi.Ec/pi.Z)*
					(1.0 + Math.log(rhoi/pi.Z)), gnRhoi);
			
			//System.out.println("gradF is " + gradF);
			//System.exit(0);
		
			gnEi[n].E(gnF);
			gnEi[n].PEa1Tv1(0.5, sumGnPhi);
        } //exit loop over atom n
        
        giRhoi0.E(sumGiRhoj0);
        
		giRhoi1.Ea1Tv1(sum[RHOj1x], sumGiRhoj1x);
		giRhoi1.PEa1Tv1(sum[RHOj1y], sumGiRhoj1y);
		giRhoi1.PEa1Tv1(sum[RHOj1z], sumGiRhoj1z);
		giRhoi1.TE(1.0/rhoi1);
		
		giRhoi2.Ea1Tv1(sum[RHOj2xx], sumGiRhoj2xx);
		giRhoi2.PEa1Tv1(2.0 * sum[RHOj2xy], sumGiRhoj2xy);
		giRhoi2.PEa1Tv1(2.0 * sum[RHOj2xz], sumGiRhoj2xz);
		giRhoi2.PEa1Tv1(sum[RHOj2yy], sumGiRhoj2yy);
		giRhoi2.PEa1Tv1(2.0 * sum[RHOj2yz], sumGiRhoj2yz);
		giRhoi2.PEa1Tv1(sum[RHOj2zz], sumGiRhoj2zz);
		giRhoi2.PEa1Tv1(-(1.0/3.0) * sum[RHOj2], sumGiRhoj2);
		giRhoi2.TE(1.0/rhoi2);
		
		giRhoi3.Ea1Tv1(sum[RHOj3xxx], sumGiRhoj3xxx);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxy], sumGiRhoj3xxy);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3xxz], sumGiRhoj3xxz);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3xyy], sumGiRhoj3xyy);
		giRhoi3.PEa1Tv1(6.0 * sum[RHOj3xyz], sumGiRhoj3xyz);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3xzz], sumGiRhoj3xzz);
		giRhoi3.PEa1Tv1(sum[RHOj3yyy], sumGiRhoj3yyy);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3yyz], sumGiRhoj3yyz);
		giRhoi3.PEa1Tv1(3.0 * sum[RHOj3yzz], sumGiRhoj3yzz);
		giRhoi3.PEa1Tv1(sum[RHOj3zzz], sumGiRhoj3zzz);
		giRhoi3.TE(1.0/rhoi3);
		
		gitav1.Ea1Tv1(-sum[T1RHOj0]/(rhoi0*rhoi0), sumGiRhoj0);
		gitav1.PEa1Tv1(1.0/rhoi0, sumt1GiRhoj0);
		
		gitav2.Ea1Tv1(-sum[T2RHOj0]/(rhoi0*rhoi0), sumGiRhoj0);
		gitav2.PEa1Tv1(1.0/rhoi0, sumt2GiRhoj0);
		
		gitav3.Ea1Tv1(-sum[T3RHOj0]/(rhoi0*rhoi0), sumGiRhoj0);
		gitav3.PEa1Tv1(1.0/rhoi0, sumt3GiRhoj0);
		
		giGamma.Ea1Tv1( (-1.0/rhoi0)*( (tav1*rhoi1*rhoi1) + (tav2*rhoi2*rhoi2)
				+ (tav3*rhoi3*rhoi3) ), giRhoi0);
		giGamma.PEa1Tv1(tav1*rhoi1, giRhoi1);
		giGamma.PEa1Tv1(tav2*rhoi2, giRhoi2);
		giGamma.PEa1Tv1(tav3*rhoi3, giRhoi3);
		giGamma.TE(2.0);
		giGamma.PEa1Tv1(rhoi1*rhoi1, gitav1);
		giGamma.PEa1Tv1(rhoi2*rhoi2, gitav2);
		giGamma.PEa1Tv1(rhoi3*rhoi3, gitav3);
		giGamma.TE(1.0/(rhoi0*rhoi0));
		
		//Sn
		
		giRhoi.Ea1Tv1(rhoi0*Math.exp(-gamma)/(1.0 + Math.exp(-gamma)), giGamma);
		giRhoi.PE(giRhoi0);
		giRhoi.TE(2.0/(1.0+Math.exp(-gamma)));
		
		//Cu
		/**
		giRhoi.Ea1Tv1(rhoi0*0.5*Math.sqrt(1.0/(1.0 + gamma)), giGamma);
		giRhoi.PEa1Tv1(Math.sqrt(1.0+gamma), giRhoi0);
	    **/
		
		giF.Ea1Tv1( (pi.A*pi.Ec/pi.Z)*
				(1.0 + Math.log(rhoi/pi.Z)), giRhoi);
		
		//System.out.println("gradF is " + gradF);
		//System.exit(0);
		
		gnEi[0].E(giF);
		gnEi[0].PEa1Tv1(0.5, sumGiPhi);
		
		return gnEi;
	}
	
	protected NearestImageTransformer nearestImageTransformer;
    private ParameterSetMEAM p;
    //private AtomPair pair;
    //private AtomSet atoms;
    
    double[] sum = new double[25];
	public static final int RHOj0 = 0;
	public static final int RHOj1x = 1;
	public static final int RHOj1y = 2;
	public static final int RHOj1z = 3;
	public static final int RHOj2xx = 4;
	public static final int RHOj2xy = 5;
	public static final int RHOj2xz = 6;
	public static final int RHOj2yy = 7;
	public static final int RHOj2yz = 8;
	public static final int RHOj2zz = 9;
	public static final int RHOj2 = 10;
	public static final int RHOj3xxx = 11;
	public static final int RHOj3xxy = 12;
	public static final int RHOj3xxz = 13;
	public static final int RHOj3xyy = 14;
	public static final int RHOj3xyz = 15;
	public static final int RHOj3xzz = 16;
	public static final int RHOj3yyy = 17;
	public static final int RHOj3yyz = 18;
	public static final int RHOj3yzz = 19;
	public static final int RHOj3zzz = 20;
	public static final int T1RHOj0 = 21;
	public static final int T2RHOj0 = 22;
	public static final int T3RHOj0 = 23;
	public static final int PHI = 24;
    
	private final Vector3D unitVector = (Vector3D)space.makeVector();
    private final Vector3D vector100 = (Vector3D)space.makeVector();
    private final Vector3D vector010 = (Vector3D)space.makeVector();
    private final Vector3D vector001 = (Vector3D)space.makeVector();
    private final Vector3D rin = (Vector3D)space.makeVector();
    private final Vector3D rij = (Vector3D)space.makeVector();
    private final Vector3D rik = (Vector3D)space.makeVector();
    private final Vector3D rkj = (Vector3D)space.makeVector();
    private final Vector3D ril = (Vector3D)space.makeVector();
    private final Vector3D rlj = (Vector3D)space.makeVector();
    
    private final Vector3D giRij = (Vector3D)space.makeVector();
    private final Vector3D gjRij = (Vector3D)space.makeVector();
    private final Vector3D gkRij = (Vector3D)space.makeVector();
    
    private final Vector3D giRik = (Vector3D)space.makeVector();
    private final Vector3D gjRik = (Vector3D)space.makeVector();
    private final Vector3D gkRik = (Vector3D)space.makeVector();
    
    private final Vector3D giRkj = (Vector3D)space.makeVector();
    private final Vector3D gjRkj = (Vector3D)space.makeVector();
    private final Vector3D gkRkj = (Vector3D)space.makeVector();
    
    private final Vector3D giXik = (Vector3D)space.makeVector();
    private final Vector3D gjXik = (Vector3D)space.makeVector();
    private final Vector3D gkXik = (Vector3D)space.makeVector();
    
    private final Vector3D giXkj = (Vector3D)space.makeVector();
    private final Vector3D gjXkj = (Vector3D)space.makeVector();
    private final Vector3D gkXkj = (Vector3D)space.makeVector();
    
    private final Vector3D giC = (Vector3D)space.makeVector();
    private final Vector3D gjC = (Vector3D)space.makeVector();
    private final Vector3D gkC = (Vector3D)space.makeVector();
    
    private final Vector3D giSijk = (Vector3D)space.makeVector();
    private final Vector3D gjSijk = (Vector3D)space.makeVector();
    private final Vector3D gkSijk = (Vector3D)space.makeVector();
    
    private final Vector3D giSij = (Vector3D)space.makeVector();
    private final Vector3D gkSij = (Vector3D)space.makeVector();
    private final Vector3D gjSij = (Vector3D)space.makeVector();
    
    private final Vector3D gix = (Vector3D)space.makeVector();
    private final Vector3D gjx = (Vector3D)space.makeVector();
    
    private final Vector3D giy = (Vector3D)space.makeVector();
    private final Vector3D gjy = (Vector3D)space.makeVector();
    
    private final Vector3D giz = (Vector3D)space.makeVector();
    private final Vector3D gjz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj1 = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj1 = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj1 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2 = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2 = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2 = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2 = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3 = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3 = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3 = (Vector3D)space.makeVector();
   
    private final Vector3D giRhoj1x = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj1x = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj1x = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj1x = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj1x = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj1y = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj1y = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj1y = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj1y = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj1y = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj1z = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj1z = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj1z = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj1z = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj1z = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2xx = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2xx = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2xx = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2xx = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2xx = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2xy = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2xy = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2xy = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2xy = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2xy = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2xz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2xz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2xz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2xz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2xz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2yy = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2yy = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2yy = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2yy = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2yy = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2yz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2yz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2yz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2yz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2yz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj2zz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj2zz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj2zz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj2zz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj2zz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xxx = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xxx = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xxx = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xxx = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xxx = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xxy = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xxy = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xxy = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xxy = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xxy = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xxz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xxz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xxz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xxz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xxz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xyy = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xyy = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xyy = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xyy = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xyy = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xyz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xyz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xyz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xyz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xyz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3xzz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3xzz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3xzz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3xzz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3xzz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3yyy = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3yyy = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3yyy = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3yyy = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3yyy = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3yyz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3yyz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3yyz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3yyz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3yyz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3yzz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3yzz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3yzz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3yzz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3yzz = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj3zzz = (Vector3D)space.makeVector();
    private final Vector3D gjRhoj3zzz = (Vector3D)space.makeVector();
    private final Vector3D gkRhoj3zzz = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiRhoj3zzz = (Vector3D)space.makeVector();
    private final Vector3D sumGkRhoj3zzz = (Vector3D)space.makeVector();
    
    private final Vector3D t1GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t1GjRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t1GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D sumt1GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D sumt1GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D t2GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t2GjRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t2GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D sumt2GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D sumt2GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D t3GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t3GjRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D t3GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D sumt3GiRhoj0 = (Vector3D)space.makeVector();
    private final Vector3D sumt3GkRhoj0 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoj0Ref = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoiRef = (Vector3D)space.makeVector();
    
    private final Vector3D giERef = (Vector3D)space.makeVector();
    private final Vector3D gjERef = (Vector3D)space.makeVector();
    
    private final Vector3D giFRef = (Vector3D)space.makeVector();
    private final Vector3D gjFRef = (Vector3D)space.makeVector();
    
    private final Vector3D giPhi = (Vector3D)space.makeVector();
    private final Vector3D gjPhi = (Vector3D)space.makeVector();
    private final Vector3D gkPhi = (Vector3D)space.makeVector();
    
    private final Vector3D sumGiPhi = (Vector3D)space.makeVector();
    private final Vector3D sumGkPhi = (Vector3D)space.makeVector();
    private final Vector3D sumGnPhi = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoi0 = (Vector3D)space.makeVector();
    private final Vector3D gnRhoi0 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoi1 = (Vector3D)space.makeVector();
    private final Vector3D gnRhoi1 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoi2 = (Vector3D)space.makeVector();
    private final Vector3D gnRhoi2 = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoi3 = (Vector3D)space.makeVector();
    private final Vector3D gnRhoi3 = (Vector3D)space.makeVector();

    private final Vector3D gitav1 = (Vector3D)space.makeVector();
    private final Vector3D gntav1 = (Vector3D)space.makeVector();
    
    private final Vector3D gitav2 = (Vector3D)space.makeVector();
    private final Vector3D gntav2 = (Vector3D)space.makeVector();
    
    private final Vector3D gitav3 = (Vector3D)space.makeVector();
    private final Vector3D gntav3 = (Vector3D)space.makeVector();
    
    private final Vector3D giGamma = (Vector3D)space.makeVector();
    private final Vector3D gnGamma = (Vector3D)space.makeVector();
    
    private final Vector3D giRhoi = (Vector3D)space.makeVector();
    private final Vector3D gnRhoi = (Vector3D)space.makeVector();
    
    private final Vector3D giF = (Vector3D)space.makeVector();
    private final Vector3D gnF = (Vector3D)space.makeVector();
    
    private Vector3D[] gnEi = new Vector3D[0];
    private ParameterSetMEAM[] parameters = new ParameterSetMEAM[0];
}
