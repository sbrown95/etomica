/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.molecule.IMoleculeList;
import etomica.molecule.MoleculeArrayList;
import etomica.potential.PotentialPolarizable;

import java.util.Arrays;


public class ClusterSumPolarizableWertheimProduct implements ClusterAbstract, java.io.Serializable {

    /**
     * Constructor for ClusterSum for Wertheim diagram including multi body term.
     * @author Hye Min Kim
     */
    public ClusterSumPolarizableWertheimProduct(ClusterBonds[] subClusters, double[] subClusterWeights, MayerFunction[] fArray) {
        if (subClusterWeights.length != subClusters.length) throw new IllegalArgumentException("number of clusters and weights must be the same");
        clusters = new ClusterBonds[subClusters.length];
        clusterWeights = subClusterWeights;
        int pointCount = subClusters[0].pointCount();
        for(int i=0; i<clusters.length; i++) {
            clusters[i] = subClusters[i];
            if(clusters[i].pointCount() != pointCount) throw new IllegalArgumentException("Attempt to construct ClusterSum with clusters having differing numbers of points");
        }
        f = fArray;
        fValues = new double[pointCount][pointCount][fArray.length];
        fOld = new double[pointCount][fArray.length];
        uijPol = new double[pointCount][pointCount];
        scfAtoms = new MoleculeArrayList(5);
        
        if (!clusters[0].isUsePermutations()) {
            // determine which fbonds are actually needed by the diagrams
            fullBondIndexArray = new int[pointCount-1][pointCount][0];
            for (int c=0; c<clusters.length; c++) {
                int[][] bondIndexArray = clusters[c].getBondIndexArray();
                for (int i=0; i<pointCount-1; i++) {
                    for (int j=i+1; j<pointCount; j++) {
                        int kf = bondIndexArray[i][j];
                        if (kf == -1) continue;
                        int[] ff = fullBondIndexArray[i][j];
                        boolean newF = true;
                        for (int k=0; k<ff.length; k++) {
                            if (ff[k] == kf) {
                                // we'll already calculate MayerFunction kf for the i-j pair
                                newF = false;
                                break;
                            }
                        }
                        if (newF) {
                            fullBondIndexArray[i][j] = Arrays.copyOf(ff, ff.length + 1);
                            fullBondIndexArray[i][j][ff.length] = kf;
                        }
                    }
                }
            }
        }
        else {
            // when using permutations in ClusterBonds, everything will get rearranged
            // at some point, so each pair will have each bond
            fullBondIndexArray = new int[pointCount-1][pointCount][f.length];
            for (int i=0; i<pointCount-1; i++) {
                for (int j=i+1; j<pointCount; j++) {
                    for (int k=0; k<f.length; k++) {
                        fullBondIndexArray[i][j][k] = k;
                    }
                }
            }
        }
    }

    // equal point count enforced in constructor 
    public int pointCount() {
        return clusters[0].pointCount();
    }
    
    public ClusterAbstract makeCopy() {
        ClusterSumPolarizableWertheimProduct copy = new ClusterSumPolarizableWertheimProduct(clusters,clusterWeights,f);
        copy.setTemperature(1/beta);
        copy.setDeltaCut(Math.sqrt(deltaCut2));
        return copy;
    }

    public double value(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();
        int nPoints = pointCount();
        IMoleculeList atomSet = box.getMoleculeList();
        long thisCPairID = cPairs.getID();
        
        PotentialPolarizable scfPotential = (PotentialPolarizable) f[0].getPotential();

        // deltaD and deltaE run into precision problems for long distances
        
//        System.out.println(thisCPairID+" "+cPairID+" "+lastCPairID+" "+value+" "+lastValue+" "+f[0].getClass());
        if (thisCPairID == cPairID) {
//            System.out.println("clusterSum "+cPairID+" returning recent "+value);
            return value;
        }
        if (thisCPairID == lastCPairID) {
            // we went back to the previous cluster, presumably because the last
            // cluster was a trial that was rejected.  so drop the most recent value/ID
            if (oldDirtyAtom > -1) {
                revertF();
            }
            cPairID = lastCPairID;
            value = lastValue;
//            System.out.println("clusterSum "+cPairID+" returning previous recent "+lastValue);
            return value;
        }

        // a new cluster
        lastCPairID = cPairID;
        lastValue = value;
        cPairID = thisCPairID;
        
        
        updateF(box);
//        checkF(cPairs,aPairs);
        
        calcValue();
        
        for(int i=0; i<nPoints-1; i++) {
            for(int j=i+1; j<nPoints; j++) {       	
                    uijPol[i][j] = scfPotential.getPolarizationEnergy(aPairs.getAPair(i, j));
                    if(Double.isNaN(uijPol[i][j])){//pair is overlapped
                    	value = 0;
                    	return value;
                    }
            }
        }
        
        if (nPoints == 3) {
        	scfAtoms.clear();
            scfAtoms.add(atomSet.get(0));
            scfAtoms.add(atomSet.get(1));
            scfAtoms.add(atomSet.get(2));
            double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);
            double deltau123 = u123Pol-(uijPol[0][1] + uijPol[0][2] + uijPol[1][2]);
            double betaU123 = beta*deltau123;
            double expBetaU123;
            
            if (Math.abs(betaU123) < 1.e-8) {
                // for small x, exp(-x)-1 ~= -x
                // for x < 1E-8, the approximation is value within machine precision
                // for x < 1E-15, exp(-x) is 1, so the approximation is more accurate
                //   than simply doing the math.
                expBetaU123 = -betaU123;
            }
            else {
                expBetaU123 = Math.exp(-beta*deltau123)-1;
            }

	      value *=expBetaU123;
	//      if (Double.isNaN(value)){
	//    	  System.out.println("value "+value+" beta "+beta+" exp "+expBetaU123+" delta "+deltau123+" u123Pol "+u123Pol+" uijPol "+uijPol[0][1]+" "+uijPol[0][2]+" "+uijPol[1][2]);
	//      }
        }
        else if (nPoints == 4) {
        	double g12 = Math.exp(-beta*uijPol[0][1]);
        	double g13 = Math.exp(-beta*uijPol[0][2]);
        	double g14 = Math.exp(-beta*uijPol[0][3]);
        	double g23 = Math.exp(-beta*uijPol[1][2]);
        	double g24 = Math.exp(-beta*uijPol[1][3]);
        	double g34 = Math.exp(-beta*uijPol[2][3]);
        	
        	scfAtoms.clear();
            scfAtoms.add(atomSet.get(0));
            scfAtoms.add(atomSet.get(1));
            scfAtoms.add(atomSet.get(2));
            scfAtoms.add(atomSet.get(3));
            
            if (g12*g13*g14*g23*g24*g34 != 0)
            {

                double u1234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU1234 = u1234Pol-(uijPol[0][1]+uijPol[0][2]+uijPol[0][3]+uijPol[1][2]+uijPol[1][3]+uijPol[2][3]); //-deltaU123-deltaU124-deltaU134-deltaU234;
                double betaU1234 = beta*deltaU1234; //deltaU123+deltaU124+deltaU134+deltaU234+deltaU1234);
                double expBetaU1234 = -betaU1234;
                if (Math.abs(betaU1234) > 1E-8) {
                    expBetaU1234 = Math.exp(-betaU1234) - 1;
                }
                scfAtoms.remove(3);
                double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU123 = u123Pol - (uijPol[0][1] + uijPol[0][2] + uijPol[1][2]);
                double beta123 = beta*deltaU123;
                double expBetaU123 = -beta123;
                if (Math.abs(beta123) > 1E-8) {
                    expBetaU123 = Math.exp(-beta123) - 1;
                }
                scfAtoms.remove(2);
                scfAtoms.add(atomSet.get(3));
                double u124Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU124 = u124Pol-(uijPol[0][1]+uijPol[0][3]+uijPol[1][3]);
                double beta124 = beta*deltaU124;
                double expBetaU124 = -beta124;
                if (Math.abs(beta124) > 1E-8) {
                    expBetaU124 = Math.exp(-beta124) - 1;
                }
                scfAtoms.remove(1);
                scfAtoms.add(atomSet.get(2));
                double u134Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU134 = u134Pol-(uijPol[0][2]+uijPol[0][3]+uijPol[2][3]);
                double beta134 = beta*deltaU134;
                double expBetaU134 = -beta134;
                if (Math.abs(beta134) > 1E-8) {
                    expBetaU134 = Math.exp(-beta134) - 1;
                }
                scfAtoms.remove(0);
                scfAtoms.add(atomSet.get(1));
                double u234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU234 = u234Pol-(uijPol[1][2]+uijPol[1][3]+uijPol[2][3]);
                double beta234 = beta*deltaU234;
                double expBetaU234 = -beta234;
                if (Math.abs(beta234) > 1E-8) {
                    expBetaU234 = Math.exp(-beta234) - 1;
                }
              value *= (expBetaU1234-expBetaU123/g14/g34/g24-expBetaU124/g13/g23/g34-expBetaU134/g12/g23/g24-expBetaU234/g12/g13/g14);
//              if (value !=0){
//            	  //System.out.println(expBetaU1234+" "+expBetaU123/g14/g34/g24+" "+expBetaU124/g13/g23/g34+" "+expBetaU134/g12/g23/g24+" "+expBetaU234/g12/g13/g14);
//            	  System.out.println(expBetaU123/g14/g34/g24+" "+expBetaU124/g13/g23/g34+" "+expBetaU134/g12/g23/g24+" "+expBetaU234/g12/g13/g14);
//            	  System.out.println("g: "+g12+" "+g13+" "+g14+" "+g23+" "+g24+" "+g34);
//              }
            }
//            if (g12 == 0 || g13== 0 || g14== 0){
//                double u234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
//                double deltaU234 = u234Pol-(uijPol[1][2]+uijPol[1][3]+uijPol[2][3]);
//                double beta234 = beta*deltaU234;
//                double expBetaU234 = -beta234;
//                if (Math.abs(beta234) > 1E-8) {
//                    expBetaU234 = Math.exp(-beta234) - 1;
//                }
//            	value += expBetaU234*g23*g24*g34*4;
//            }

//              scfAtoms.clear();
//              scfAtoms.add(atomSet.getMolecule(0));
//              scfAtoms.add(atomSet.getMolecule(1));
//              scfAtoms.add(atomSet.getMolecule(2));
//
//              
//              if (g12*g13*g23 != 0) {
//                  double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);
//                  double deltaU123 = u123Pol - (uijPol[0][1] + uijPol[0][2] + uijPol[1][2]);
//                  double beta123 = beta*deltaU123;
//                  double expBetaU123 = -beta123;
//                  if (Math.abs(beta123) > 1E-8) {
//                      expBetaU123 = Math.exp(-beta123) - 1;
//                  }
//                  value += -expBetaU123*g12*g13*g23;
//              }
//              
//              
//
//              scfAtoms.remove(2);
//              scfAtoms.add(atomSet.getMolecule(3));
//              if (g12*g14*g24 != 0) {
//                  double u124Pol = scfPotential.getPolarizationEnergy(scfAtoms);
//                  double deltaU124 = u124Pol-(uijPol[0][1]+uijPol[0][3]+uijPol[1][3]);
//                  double beta124 = beta*deltaU124;
//                  double expBetaU124 = -beta124;
//                  if (Math.abs(beta124) > 1E-8) {
//                      expBetaU124 = Math.exp(-beta124) - 1;
//                  }
//                  value +=  -expBetaU124*g12*g14*g24;
//              }
//
//              scfAtoms.remove(1);
//              scfAtoms.add(atomSet.getMolecule(2));
//              if (g13*g14*g34 != 0) {
//                  double u134Pol = scfPotential.getPolarizationEnergy(scfAtoms);
//                  double deltaU134 = u134Pol-(uijPol[0][2]+uijPol[0][3]+uijPol[2][3]);
//                  double beta134 = beta*deltaU134;
//                  double exp134 = -beta134;
//                  if (Math.abs(beta134) > 1E-8) {
//                      exp134 = Math.exp(-beta134) - 1;
//                  }
//                  value += -exp134*g13*g14*g34;
//              }
//
//              scfAtoms.remove(0);
//              scfAtoms.add(atomSet.getMolecule(1));
//              if (g23*g24*g34 != 0) {
//                  double u234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
//                  double deltaU234 = u234Pol-(uijPol[1][2]+uijPol[1][3]+uijPol[2][3]);
//                  double beta234 = beta*deltaU234;
//                  double exp234 = -beta234;
//                  if (Math.abs(beta234) > 1E-8) {
//                      exp234 = Math.exp(-beta234) - 1;
//                  }
//                  value += -exp234*g23*g24*g34;
//              }
        }
        return value;
    }
    
    protected void calcValue() {
        value = 0.0;
        for(int i=0; i<clusters.length; i++) {
            double v = clusters[i].value(fValues);
            value += clusterWeights[i] * v;
        }
        if (value < 0.0){
//        	System.out.println("value= "+value);
//        	System.out.println("fValues0 "+Arrays.toString(fValues[0][1]));
//        	System.out.println("fValues1 "+Arrays.toString(fValues[0][2]));
//        	System.out.println("fValues2 "+Arrays.toString(fValues[1][2]));
        }
        
    }

    
    
    protected void revertF() {
        int nPoints = pointCount();

        for(int j=0; j<nPoints; j++) {
            if (j == oldDirtyAtom) {
                continue;
            }
            for(int k=0; k<f.length; k++) {
                fValues[oldDirtyAtom][j][k] = fOld[j][k];
                fValues[j][oldDirtyAtom][k] = fOld[j][k];
            }
        }
        oldDirtyAtom = -1;
    }
    
    protected void updateF(BoxCluster box) {
        int nPoints = pointCount();
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();

        for (int k=0; k<f.length; k++) {
            f[k].setBox(box);
        }
        // recalculate all f values for all pairs
        for(int i=0; i<nPoints-1; i++) {
            for(int j=i+1; j<nPoints; j++) {
                // only update the mayer functions that we'll need for this pair
                int[] fij = fullBondIndexArray[i][j];
                for(int k=0; k<fij.length; k++) {
                    int fk = fij[k];
                    fValues[i][j][fk] = f[fk].f(aPairs.getAPair(i,j),cPairs.getr2(i,j), beta);
                    fValues[j][i][fk] = fValues[i][j][fk];
                }
            }
        }
    }
    
    private void checkF(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();
        int nPoints = pointCount();
        for(int i=0; i<nPoints-1; i++) {
            for(int j=i+1; j<nPoints; j++) {
                int[] fij = fullBondIndexArray[i][j];
                for(int k=0; k<fij.length; k++) {
                    int fk = fij[k];
                    if (fValues[i][j][fk] != f[fk].f(aPairs.getAPair(i,j),cPairs.getr2(i,j), beta)) {
                        throw new RuntimeException("oops2 "+i+" "+j+" "+fk+" "+f[fk].f(aPairs.getAPair(i,j),cPairs.getr2(i,j), beta));
                    }
                    if (fValues[j][i][fk] != fValues[i][j][fk]) {
                        throw new RuntimeException("oops3 "+i+" "+j+" "+fk+" "+fValues[j][i][fk]+" "+fValues[i][j][fk]);
                    }
                }
            }
        }
    }
    
    public ClusterBonds[] getClusters() {return clusters;}
    /**
     * @return Returns the temperature.
     */
    public double getTemperature() {
        return 1/beta;
    }
    /**
     * @param temperature The temperature to set.
     */
    public void setTemperature(double temperature) {
        beta = 1/temperature;
    }

    public double[][][] getFValues() {
        return fValues;
    }
    
    public void setDeltaCut(double newDeltaDCut) {
        deltaCut2 = newDeltaDCut*newDeltaDCut;
    }
    
    public double getDeltaCut() {
        return Math.sqrt(deltaCut2);
    }

    private static final long serialVersionUID = 1L;
    protected final ClusterBonds[] clusters;
    protected final double[] clusterWeights;
    int[][][] fullBondIndexArray;
    protected final MayerFunction[] f;
    protected double[][][] fValues;
    protected final double[][] fOld;
    protected int oldDirtyAtom;
    protected long cPairID = -1, lastCPairID = -1;
    protected double value, lastValue;
    protected double beta;
    protected final MoleculeArrayList scfAtoms;
    protected double deltaCut2 = Double.POSITIVE_INFINITY;
    protected final double[][] uijPol;
    public double pushR2 = 0;
}
