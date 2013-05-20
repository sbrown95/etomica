package etomica.virial;

import java.util.HashSet;

import etomica.graph.model.Graph;
import etomica.graph.model.impl.GraphImpl;
import etomica.graph.operations.GraphOp.GraphOpNull;
import etomica.graph.operations.MaxIsomorph;
import etomica.graph.operations.MaxIsomorph.MaxIsomorphParameters;

/**
 * This class calculates the sum of all biconnected clusters using Wheatley's
 * recursive formulation.
 * 
 * @author David Kofke and Andrew Schultz 
 */
public class ClusterWheatleyPartitionScreening implements ClusterAbstract {

    protected final int n;
    protected final MayerFunction f;
    
    protected final double[] fQ, fC;
    protected final double[] fA, fB;
    protected int cPairID = -1, lastCPairID = -1;
    protected double value, lastValue;
    protected double beta;
    protected final byte[] outDegree;
    protected final int[] fullBondMask;
    protected final boolean[] cliqueSet;
    protected int cliqueCount, eCliqueCount;
    protected boolean precalcQ = true;
    protected final int[] cliqueList;
    
    protected final int[] nBonds;
    protected final int[] nPts;
    long bigSum1, bigSum2, count, bigSumN, bigSumNCount;

    protected static final boolean checkme = true;

    public ClusterWheatleyPartitionScreening(int nPoints, MayerFunction f) {
        this.n = nPoints;
        this.f = f;
        int nf = 1<<n;  // 2^n
        fQ = new double[nf];
        fC = new double[nf];
        nBonds = new int[nf];
        nPts = new int[nf];
        for(int i=0; i<n; i++) {
            fQ[1<<i] = 1.0;
        }
        fA = new double[nf];
        fB = new double[nf];
        outDegree = new byte[n];
        fullBondMask = new int[nf];
        cliqueSet = new boolean[nf];
        cliqueList = new int[nf];
    }

    public ClusterAbstract makeCopy() {
        ClusterWheatleyPartitionScreening c = new ClusterWheatleyPartitionScreening(n, f);
        c.setTemperature(1/beta);
        return c;
    }

    public int pointCount() {
        return n;
    }

    public double value(BoxCluster box) {
      CoordinatePairSet cPairs = box.getCPairSet();
      int thisCPairID = cPairs.getID();
      if (thisCPairID == cPairID) {
          return value;
      }
      if (thisCPairID == lastCPairID) {
          // we went back to the previous cluster, presumably because the last
          // cluster was a trial that was rejected.  so drop the most recent value/ID
          cPairID = lastCPairID;
          value = lastValue;
          return value;
      }

      // a new cluster
      lastCPairID = cPairID;
      lastValue = value;
      cPairID = thisCPairID;
      
      updateF(box);
      
      calcValue(box, true);
      if (Double.isNaN(value) || Double.isInfinite(value)) {
          updateF(box);
          calcValue(box);
          throw new RuntimeException("oops");
      }
      return value;
    }

    /**
     * This calculates all FQ values given that the entries for pairs have
     * already been populated.
     */
    protected void calcFullFQ(BoxCluster box) {
        int nf = 1<<n;
        eCliqueCount = 0;
        // generate all partitions and compute product of e-bonds for all pairs in partition
        for (int i=3; i<nf; i++) {
            int j = i & -i;//lowest bit in i
            if (i==j) continue; // 1-point set
            int k = i&~j; //strip j bit from i and set result to k
            if (k == (k&-k)) continue; // 2-point set; these fQ's were filled when bonds were computed, so skip
            fQ[i] = fQ[k]; //initialize with previously-computed product of all pairs in partition, other than j
            if (fQ[i] == 0) continue;
            //loop over pairs formed from j and each point in partition; multiply by bond for each pair
            //all such pairs will be with bits higher than j, as j is the lowest bit in i
            for (int l=(j<<1); l<i; l=(l<<1)) {
                if ((l&i)==0) continue; //l is not in partition
                fQ[i] *= fQ[l | j];
                if(fQ[i] == 0) break;
            }
        }
        
        for (int i=3; i<nf; i++) {
            nPts[i] = 1;
            
            int j = i & -i;//lowest bit in i
            if (i==j) continue; // 1-point set
            int k = i&~j; //strip j bit from i and set result to k
            if (k == (k&-k)) {//2-point set
                nPts[i] = 2;
                nBonds[i] = fQ[k|j]==0 ? 1 : 0;//fQ is e-bond, so if zero it means f-bond is nonzero
                continue;
            }
            nBonds[i] = nBonds[k];
            //loop over pairs formed from j and each point in partition; multiply by bond for each pair
            //all such pairs will be with bits higher than j, as j is the lowest bit in i
            for (int l=(j<<1); l<i; l=(l<<1)) {
                if ((l&i)==0) continue; //l is not in partition
                nPts[i]++;
                if(fQ[l | j] == 0) nBonds[i]++;
            }
            if (fQ[i] != 0) eCliqueCount++;
        }
    }

    public boolean checkConfig(BoxCluster box) {
        if (false && total>0 && total%100000==0) {
            System.out.println("screened: "+((double)screened)/total+"     zero-not-screened: "+((double)(total-screened-notzero))/(total-screened));
        }
        updateF(box);
        total++;
        screened++;
        edgeCount = 0;

        for (int i=0; i<n; i++) {
            outDegree[i] = 0;
        }
        int nf = 1<<n;
        for (int i=0; i<nf; i++) {
            fullBondMask[i] = 0;
        }

        for (int i=0; i<n-1; i++) {
            for (int j=i+1; j<n; j++) {
                int k = (1<<i)|(1<<j);
                boolean fBond = (fQ[k] == 0); 
                cliqueSet[k] = fBond;
                if (fBond) {
                    outDegree[i]++;
                    outDegree[j]++;
                    edgeCount++;
                    fullBondMask[1<<i] |= 1<<j;
                    fullBondMask[1<<j] |= 1<<i;
                }
            }
        }

        int maxEdges = n*(n-1)/2;
        if (edgeCount==maxEdges) {
            screened--;
            cliqueCount = nf-n-(n*(n-1)/2);
            calcFullFQ(box);
            return true;
        }
        if (edgeCount==maxEdges-1) return false;
        for (int i=0; i<n; i++) {
            if (outDegree[i] < 2) {
                return false;
            }
        }

        // the existence of a clique separator indicates that the value for
        // this configuration is zero.  Loop through all sets, considering
        // each as a clique separator.
        cliqueCount = 0;
iLoop:  for (int i=1; i<nf-3; i++) {
            int j = i & -i;//lowest bit in i
            if (i==j) {
                // 1-point set.  check as an articulation point
                if (cliqueCheck(i, nf-1)) return false;
                continue;
            }
            int k = i&~j; //strip j bit from i and set result to k
            if (k == (k&-k)) {
                // 2-point set.  cliqueSet[i] was set in the above loop
                // over pairs.
                if (cliqueSet[i] && cliqueCheck(i, nf-1)) return false;
                continue;
            }
            cliqueSet[i] = cliqueSet[k]; //initialize with previously-computed product of all pairs in partition, other than j

            if (!cliqueSet[i]) continue;
            //loop over pairs formed from j and each point in partition; multiply by bond for each pair
            //all such pairs will be with bits higher than j, as j is the lowest bit in i
            for (int l=(j<<1); l<i; l=(l<<1)) {
                if ((l&i)==0) continue; //l is not in partition
                if (!cliqueSet[l|j]) {
                    cliqueSet[i] = false;
                    continue iLoop;
                }
            }
            // i is a clique
            if (cliqueCheck(i, nf-1)) {
                return false;
            }
            cliqueList[cliqueCount] = i;
            cliqueCount++;
        }
        
        if (precalcQ) {
            calcFullFQ(box);
        }

        screened--;
        return true;
    }

    /**
     * Checks to see if the set |clique| is a clique separator.  The graph
     * connectivity is described by the fullBondMask array.
     */
    protected boolean cliqueCheck(int clique, int nfm1) {
        int cComp = nfm1^clique;
        int m = cComp & -cComp;
        if (m==cComp) {
            // there is only one point not in the clique
            return false;
        }
        int seen = m | clique;

        while (true) {
            int newSeen = seen;
            while (m != 0) {
                int lb = m & -m;
                newSeen |= fullBondMask[lb];
                m = m ^ lb;
            }
            if (newSeen == seen) {
                // we didn't pick up any new points
                return true;
            }
            if (newSeen == nfm1) {
                // we found all points
                return false;
            }
            m = newSeen - seen;
            seen = newSeen;
        }
    }

    /**
     * Returns the cluster value for the given configuration.  You must call
     * doCheck(BoxCluster) before calling this method.
     */
    public double calcValue(BoxCluster box) {
        calcValue(box, false);
        return value;
    }

    /*
     * Computation of sum of biconnected diagrams.
     */
    protected void calcValue(BoxCluster box, boolean doCheck) {
        if (doCheck && !checkConfig(box)) {
            value = 0;
            return;
        }
        if (!precalcQ) {
            calcFullFQ(box);
        }

        int nf = 1<<n;
        //Compute the fC's
        for(int i=1; i<nf; i++) {
            if (!checkme && nBonds[i]+1 < nPts[i]) {
                fC[i] = 0;
                continue;
            }
            fC[i] = fQ[i];
            int iLowBit = i & -i;
            int inc = iLowBit<<1;
            for(int j=iLowBit; j<i; j+=inc) {
                int jComp = i & ~j;
                while ((j|jComp) != i && j<i) {
                    int jHighBits = j^iLowBit;
                    int jlow = jHighBits & -jHighBits;
                    j += jlow;
                    jComp = (i & ~j);
                }
                if (j==i) break;
                fC[i] -= fC[j] * fQ[jComp];//for fQ, flip the bits on j; use only those appearing in i
            }
        }

        // find fA1
        for (int i=2; i<nf; i+=2) {
            // all even sets don't contain 1
            fA[i] = 0;
            fB[i] = fC[i];
        }
        fA[1] = 0;
        fB[1] = fC[1];
        int sum1 = 0;
        int sum2 = 0;
        int counter = 0;
        for (int i=3; i<nf; i+=2) {
            // every set will contain 1
            fA[i] = 0;
            fB[i] = fC[i];
            if (!checkme && nBonds[i]+1 < nPts[i]) {
                continue;
            }
            int ii = i - 1;//all bits in i but lowest
            int iLow2Bit = (ii & -ii);//next lowest bit
            int jBits = 1 | iLow2Bit;
            if (jBits==i) continue;
            //jBits has 1 and next lowest bit in i
            int iii = ii ^ iLow2Bit;//i with 2 lowest bits off
            int jInc = (iii & -iii);//3rd lowest bit, also increment for j
            for (int j=jBits; j<i; j+=jInc) {//sum over partitions of i containing jBits
                int jComp = (i & ~j); //subset of i complementing j
                while ((j|jComp) != i && j<i) {
                    int jHighBits = j^jBits;
                    int jlow = jHighBits & -jHighBits;
                    j += jlow;
                    jComp = (i & ~j);
                }
                if (j==i) break;
                fA[i] += fB[j] * fC[jComp|1];
            }
            fB[i] -= fA[i];//remove from B graphs that contain articulation point at 0
        }
        if (checkme) {
            for(int i=0; i<nf; i++) {
                if(fB[i]==0 && nPts[i] > 2) sum2++;
                counter++;
            }
        }

        for (int v=1; v<n; v++) {
            int vs1 = 1<<v;
            for (int i=vs1+1; i<nf; i++) {
                if (!checkme && nBonds[i]+1 < nPts[i]) {
                    continue;
                }
//                boolean wasZero = fB[i] == 0;
                fA[i] = 0;
//                fB[v][i] = fB[v-1][i];//no a.p. at v or below, starts with those having no a.p. at v-1 or below
                //rest of this is to generate A (diagrams having a.p. at v but not below), and subtract it from B
                if ((i & vs1) == 0) continue;//if i doesn't contain v, fA and fB are done
                int iLowBit = (i&-i);//lowest bit in i
                if (iLowBit == i) { //lowest bit is only bit; fA and fB are done
                    continue;
                }
                int jBits;
                int ii = i ^ iLowBit;
                int iLow2Bit = (ii & -ii);
                if (iLowBit != vs1 && iLow2Bit != vs1) {
                    //v is not in the lowest 2 bits
                    // jBits is the lowest bit and v
                    jBits = iLowBit | vs1;

                    // we can only increment by the 2nd lowest
                    int jInc = iLow2Bit;

                    //at this point jBits has (lowest bit + v) or (v + next lowest bit)
                    for (int j=jBits; j<i; j+=jInc) {//sum over partitions of i
                        if ((j & jBits) != jBits) {
                            //ensure jBits are in j
                            j |= vs1;
                            if (j==i) break;
                        }
                        int jComp = i & ~j;//subset of i complementing j
                        while ((j|jComp) != i && j<i) {
                            int jHighBits = j^jBits;
                            int jlow = jHighBits & -jHighBits;
                            j += jlow; // this might knock out the v bit
                            j |= vs1;
                            jComp = (i & ~j);
                        }
                        if (j==i) break;
                        fA[i] += fB[j] * (fB[jComp|vs1] + fA[jComp|vs1]);
                    }
                }
                else {
                    //lowest 2 bits contain v
                    // jBits is the lowest 2 bits
                    // we can start at jBits and increment by the 3rd lowest bit
                    jBits = iLowBit | iLow2Bit;
                    if (jBits == i) continue; // no bits left for jComp
                    
                    int iii = ii ^ iLow2Bit;
                    int jInc = (iii & -iii);

                    //at this point jBits has (lowest bit + v) or (v + next lowest bit)
                    for (int j=jBits; j<i; j+=jInc) {//sum over partitions of i
                        // start=jBits and jInc ensure that every set includes jBits
                        int jComp = i & ~j;//subset of i complementing j
                        while ((j|jComp) != i && j<i) {
                            int jHighBits = j^jBits;
                            int jlow = jHighBits & -jHighBits;
                            j += jlow;
                            jComp = (i & ~j);
                        }
                        if (j==i) break;
                        fA[i] += fB[j] * (fB[jComp|vs1] + fA[jComp|vs1]);
                    }
                }

                //if(fB[i]==0 && fA[i]!=0 && nPts[i]>2) System.out.println("fB went from zero to nonzero");
                fB[i] -= fA[i];//remove from B graphs that contain articulation point at v
               // if(wasZero && fB[i]!=0 && nPts[i]>2) System.out.println("fB went from zero to nonzero");
                
            }
            if (checkme) {
                for(int i=0; i<nf; i++) {
                    if(fB[i]==0 && nPts[i] > 2) sum2++;
                    counter++;
                }
                for(int i=0; i<nf; i++) {
    //                if((nBonds[i]+1 < nPts[i]) && fC[i] != 0) {
    //                    System.out.println("problem with fC");
    //                }
                    if((nBonds[i]+1 < nPts[i]) && fB[i] != 0 && nPts[i] > 2) {
                        System.out.println("problem with fB");
                    }
    //                if(fB[i]!=0 && fC[i] != 0 && nPts[i]>2) {
    //                    System.out.println("problem with fB relation to fC");
    //                }
                }
            }
        }
        
        if (checkme) {
            long sumn = 0;
            long nCount = 0;
            for(int i=0; i<nf; i++) {
                if (nPts[i]>2) {
                    if (nBonds[i] < nPts[i]) {
                        sumn += nPts[i];
                        nCount++;
                    }
                }
                if((nBonds[i]+1 < nPts[i]) && fC[i] != 0) {
                    System.out.println("problem with fC");
                }
                if((nBonds[i] < nPts[i]) && fB[i] != 0 && nPts[i] > 2) {
                    System.out.println("problem with fB");
                }
    //            if(nBonds[i]+1 < nPts[i] && nPts[i] > 2) sum1++;
    //            if(nBonds[i] < nPts[i] && nPts[i] > 2) sum2++;
                if(fC[i]==0 && nPts[i] > 2) sum1++;
    //            if(fB[i]==0 && nPts[i] > 2) sum2++;
            }
            bigSum1 += sum1;
            bigSum2 += sum2;
            bigSumN += sumn;
            bigSumNCount += nCount;
            count++;
    //        System.out.println(sum1+"\t"+sum2+"\t"+nf);
            System.out.println(((double)(bigSum1)/nf/count)+"\t"+((double)(bigSum2)/nf/n/count)+"\t"+((double)bigSumN)/(bigSumNCount)+"\t"+nf+"\t"+counter);
        }

        

        value = (1-n)*fB[nf-1]; ///SpecialFunctions.factorial(n);
        if (value != 0) {
            notzero++;
            // disable check above and then enable this to see if non-zero
            // configurations would be screened
            if (false && !checkConfig(box)) {
                Graph g = new GraphImpl((byte)n);
                for (int i=0; i<n-1; i++) {
                    for (int j=i+1; j<n; j++) {
                        if ((fullBondMask[i] & (1<<j)) != 0) {
                            g.putEdge((byte)i, (byte)j);
                        }
                    }
                }
//                MaxIsomorph maxIso = new MaxIsomorph();
//                MaxIsomorphParameters mip = new MaxIsomorphParameters(new GraphOpNull(), MaxIsomorph.PROPERTY_ALL);
//                g = maxIso.apply(g, mip);
                String s = g.getStore().toNumberString();
                System.out.println("**** oops thought this was zero: "+s);
                checkConfig(box);
            }
        }
        else if (false) {
            // enable this to see what zero-value configurations are not being
            // screened
            Graph g = new GraphImpl((byte)n);
            for (int i=0; i<n-1; i++) {
                for (int j=i+1; j<n; j++) {
                    if ((fullBondMask[i] & (1<<j)) != 0) {
                        g.putEdge((byte)i, (byte)j);
                    }
                }
            }
            MaxIsomorph maxIso = new MaxIsomorph();
            MaxIsomorphParameters mip = new MaxIsomorphParameters(new GraphOpNull(), MaxIsomorph.PROPERTY_ALL);
            g = maxIso.apply(g, mip);
            String s = g.getStore().toNumberString();
            if (!zeroMaps.contains(s)) {
                System.out.println(s+" is zero");
                zeroMaps.add(s);
                for (String ss : zeroMaps) {
                    System.out.print(ss+",");
                }
                System.out.println();
            }
        }
    }
    
    /**
     * Returns edgeCount (number of overlaps) of configuration passed to
     * checkConfig
     */
    public int getEdgeCount() {
        return edgeCount;
    }
    
    public int getCliqueCount() {
        return cliqueCount;
    }
    
    public int getECliqueCount() {
        return eCliqueCount;
    }
    
    public int[] getFullBondMask() {
        return fullBondMask;
    }
    
    public int[] getCliques() {
        return cliqueList;
    }
    
    /**
     * Returns outDegee (number of bonds for each point) of the configuration
     * passed to checkConfig
     */
    public byte[] getOutDegree() {
        return outDegree;
    }

    protected long total, notzero, screened;
    protected int edgeCount;
    HashSet<String> zeroMaps = new HashSet<String>();

    protected void updateF(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();

        f.setBox(box);
        // recalculate all f values for all pairs
        for(int i=0; i<n-1; i++) {
            for(int j=i+1; j<n; j++) {
                fQ[(1<<i)|(1<<j)] = f.f(aPairs.getAPair(i,j),cPairs.getr2(i,j), beta)+1;
            }
        }
    }

    public void setTemperature(double temperature) {
        beta = 1/temperature;
    }
}
