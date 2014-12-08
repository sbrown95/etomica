/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.api.IAtomList;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.IAtomPositionDefinition;
import etomica.space.ISpace;

public class ClusterCoupledFlipped implements ClusterAbstract {

    public ClusterCoupledFlipped(ClusterAbstract cluster, ISpace space) {
        this(cluster, space, 0);
    }
    
    public ClusterCoupledFlipped(ClusterAbstract cluster, ISpace space, double minFlipDistance) {
        this.space = space;
        wrappedCluster = cluster;
        childAtomVector = space.makeVector();
        flippedAtoms = new boolean[cluster.pointCount()];
        positionDefinition = new AtomPositionGeometricCenter(space);
        this.minFlipDistance = minFlipDistance;
    }

    public ClusterAbstract makeCopy() {
        return new ClusterCoupledFlipped(wrappedCluster.makeCopy(), space, minFlipDistance);
    }

    public int pointCount() {
        return wrappedCluster.pointCount();
    }

    public ClusterAbstract getSubCluster() {
        return wrappedCluster;
    }
    
    public double value(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        long thisCPairID = cPairs.getID();
//      System.out.println(thisCPairID+" "+cPairID+" "+lastCPairID+" "+value+" "+lastValue+" "+f[0].getClass());
        if (thisCPairID == cPairID) {
//          System.out.println("clusterSum "+cPairID+" returning recent "+value);
            return value;
        }
        else if (thisCPairID == lastCPairID) {
          // we went back to the previous cluster, presumably because the last
          // cluster was a trial that was rejected.  so drop the most recent value/ID
            cPairID = lastCPairID;
            value = lastValue;
//          System.out.println("clusterSum "+cPairID+" returning previous recent "+lastValue);
            return value;
        }

        // a new cluster
        lastCPairID = cPairID;
        lastValue = value;
        cPairID = thisCPairID;

        final int pointCount = wrappedCluster.pointCount();
        
        boolean flipit = false;
        double minR2 = minFlipDistance*minFlipDistance;
        for (int i=0; i<pointCount; i++) {
            flippedAtoms[i] = false;
            for (int j=i+1; !flipit && j<pointCount; j++) {
                if (box.getCPairSet().getr2(0,1) > minR2) {
                    flipit=true;
                }
            }
        }
        
        boolean debugme = box.getCPairSet().getr2(0,1) > 1000 && false;
        double vsum = wrappedCluster.value(box);
        if (!flipit) {
            value = vsum;
            cPairID = cPairs.getID();
            return vsum;
        }
        if (debugme) System.out.print(String.format("%10.4e ", vsum));

        IMoleculeList atomList = box.getMoleculeList();
        // loop through the atoms, toggling each one until we toggle one "on"
        // this should generate each combination of flipped/unflipped for all
        // the molecules
        while (true) {
            boolean didFlipTrue = false;
            for (int i=0; !didFlipTrue && i<pointCount; i++) {
                flippedAtoms[i] = !flippedAtoms[i];
                didFlipTrue = flippedAtoms[i];
                flip(atomList.getMolecule(i));
            }
            if (!didFlipTrue) {
                // if we flipped every atom from true to false, we must be done
                break;
            }
            double foo = wrappedCluster.value(box);
            if (debugme) System.out.print(String.format("%10.4e ", foo));
            vsum += foo;
            if (Double.isNaN(vsum)) {throw new RuntimeException("oops");}
        }
        
        value = vsum / Math.pow(2, pointCount);
        if (debugme) System.out.print(String.format("%10.4e\n", value));
        if (debugme) System.exit(1);
        
        cPairID = cPairs.getID();
        return value;
    }
    
    protected void flip(IMolecule flippedMolecule) {
        IVector COM = positionDefinition.position(flippedMolecule);
		IAtomList childAtoms = flippedMolecule.getChildList();
		for (int i = 0; i < childAtoms.getAtomCount(); i++) {
		    childAtomVector.Ea1Tv1(2,COM);
			childAtomVector.ME(childAtoms.getAtom(i).getPosition());
			childAtoms.getAtom(i).getPosition().E(childAtomVector);
		}
    }

    public void setTemperature(double temperature) {
        wrappedCluster.setTemperature(temperature);
    }
    
    protected final ClusterAbstract wrappedCluster;
    protected final ISpace space;
    protected long cPairID = -1, lastCPairID = -1;
    protected double value, lastValue;
    protected final boolean[] flippedAtoms;
    private IVectorMutable childAtomVector;
    protected IAtomPositionDefinition positionDefinition;
    protected final double minFlipDistance;
}
