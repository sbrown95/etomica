/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import java.math.BigDecimal;

import etomica.api.IMoleculeList;
import etomica.atom.MoleculeArrayList;
import etomica.math.SpecialFunctions;

/**
 * This class uses Wheatley's recursion approach to calculating all biconnected
 * diagrams, but adds in non-additive contributions.  All computations are done
 * with BigDecimal variables.
 * 
 * @author Andrew Schultz
 */
public class ClusterWheatleyMultibodyBD extends ClusterWheatleySoftBD {

    protected final MayerFunctionNonAdditive[] fMulti;
    protected final int[] moleculeIndices;
    protected final double[] r2;
    protected final MoleculeArrayList molecules;
    protected boolean doMulti;
    protected double rCut2;
    protected final BigDecimal[] fQmulti;

    /**
     * @param nPoints number of points
     * @param f pair Mayer function
     * @param fMulti array of non-additive Mayer functions.  fMulti[3] is the
     *          3-body Mayer function (exp(-beta*deltaU3)-1), fMulti[4] is the
     *          4-body Mayer function, etc.  fMulti null entries will be
     *          ignored and the array need to not be of size equal to nPoints.
     *          If only 3-body Mayer function is available, then fMulti can be
     *          of length 4 (0,1,2,3).
     * @param precision number of digits used for internal computation and storage
     */
    public ClusterWheatleyMultibodyBD(int nPoints, MayerFunction f, MayerFunctionNonAdditive[] fMulti, int precision) {
        super(nPoints, f, precision);
        this.fMulti = fMulti;
        moleculeIndices = new int[nPoints];
        r2 = new double[nPoints*(nPoints-1)/2];
        molecules = new MoleculeArrayList(nPoints);
        rCut2 = Double.POSITIVE_INFINITY;
        fQmulti = new BigDecimal[1<<n];
        fQmulti[0] = fQmulti[1] = fQmulti[2] = BDONE;
    }
    
    public ClusterAbstract makeCopy() {
        ClusterWheatleyMultibodyBD c = new ClusterWheatleyMultibodyBD(n, f, fMulti, mc.getPrecision());
        c.setTemperature(1/beta);
        c.setDoCaching(doCaching);
        return c;
    }

    public void setRCut(double newRCut) {
        rCut2 = newRCut * newRCut;
    }

    public void calcValue(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        double rMax = 0;
        for(int i=0; i<n-1; i++) {
            for(int j=i+1; j<n; j++) {
                if (cPairs.getr2(i,j) > rMax) rMax = cPairs.getr2(i,j);
                if (cPairs.getr2(i,j) > rCut2) {
                    value = 0;
                    return;
                }
            }
        }

        // do (multi+pair) - pair here so that we avoid recomputing f bonds
        doMulti = false;
        super.calcValue(box);
        int nf = 1<<n;
        BigDecimal fBPair = fB[nf-1].multiply(BDONE,mc);
//        double pairValue = value;
        doMulti = true;
        super.calcValue(box);
//        if (debugme) System.out.println("BDpair "+pairValue+"  full "+value+"   NA "+(value-pairValue));
        value = (1-n)*fB[nf-1].subtract(fBPair, mc).doubleValue()/SpecialFunctions.factorial(n);
    }

    protected void calcFullFQ(BoxCluster box) {
        super.calcFullFQ(box);
        if (!doMulti) return;
        for (int i=3; i<fMulti.length; i++) {
            if (fMulti[i]!=null) fMulti[i].setBox(box);
        }
        int nf = 1<<n;
        IMoleculeList boxMolecules = box.getMoleculeList();
        // FQ[i] now contains the exp(-bU2) where U2 is the pair-wise energy for set i.
        // we need to go around and add the non-additive energy for each set.

        for (int i=3; i<nf; i++) {
            fQmulti[i] = BDONE;
            int j = i & -i;//lowest bit in i
            if (i==j) continue; // 1-point set
            int k = i&~j; //strip j bit from i and set result to k
            if (k == (k&-k)) continue; // 2-point set; these fQ's were filled when bonds were computed, so skip
            if (fQ[i].doubleValue() == 0) continue;

            // we want to loop over subsets of i with at least 3 points
            int iLowBit = (i & -i);//next lowest bit
            for (int isub=iLowBit; isub<=i; isub+=iLowBit) {//sum over partitions of i
                while ((isub & ~i) != 0) {
                    // loop until isub is an actual subset of i
                    isub += iLowBit;
                }
                fQ[i] = fQ[i].multiply(fQmulti[isub],mc);
                if (fQ[i].doubleValue() == 0) break;
            }
            if (fQ[i].doubleValue() == 0) continue;
            
            int l = 0;
            molecules.clear();
            for (int a=0; a<n; a++) {
                if ((i & (1<<a)) != 0) {
                    moleculeIndices[l] = a;
                    molecules.add(boxMolecules.getMolecule(a));
                    l++;
                }
            }
            if (fMulti.length <= l || fMulti[l] == null) continue;
            int ll = 0;
            for (int a=0; a<l-1; a++) {
                for (int b=a+1; b<l; b++) {
                    r2[ll] = box.getCPairSet().getr2(moleculeIndices[a],moleculeIndices[b]);
                    ll++;
                }
            }
            fQmulti[i] = new BigDecimal(fMulti[l].f(molecules, l, moleculeIndices, r2, beta),mc).add(BDONE);
            fQ[i] = fQ[i].multiply(fQmulti[i], mc);
        }
    }
}
