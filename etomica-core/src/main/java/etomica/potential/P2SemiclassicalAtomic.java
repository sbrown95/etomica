/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.potential;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IAtomType;
import etomica.box.Box;
import etomica.api.IPotentialAtomic;
import etomica.api.IVector;
import etomica.atom.AtomTypeAgentManager;
import etomica.atom.IAtomOriented;
import etomica.space.ISpace;
import etomica.util.Constants;

/**
 * Effective semiclassical molecular potential using the approach of
 * Takahashi and Imada
 * 
 * http://dx.doi.org/10.1143/JPSJ.53.3765
 * 
 * as described by Schenter
 * 
 * http://dx.doi.org/10.1063/1.1505441
 * 
 * @author Andrew Schultz
 */
public class P2SemiclassicalAtomic implements IPotentialAtomic {

    protected final IPotentialTorque p2Classy;
    protected double temperature, fac;
    protected final AtomTypeAgentManager agents;
    protected final ISpace space;
    
    public P2SemiclassicalAtomic(ISpace space, IPotentialTorque p2Classy, double temperature) {
        this.space = space;
        this.p2Classy = p2Classy;
        if (p2Classy.nBody() != 2) throw new RuntimeException("I would really rather have a 2-body potential");
        agents = new AtomTypeAgentManager(null);
        setTemperature(temperature);
    }
    
    public void setAtomInfo(IAtomType species, AtomInfo moleculeInfo) {
        agents.setAgent(species, moleculeInfo);
    }
    
    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
        double hbar = Constants.PLANCK_H/(2*Math.PI);
        fac = hbar*hbar/(24*temperature*temperature);
    }
    
    public double getRange() {
        return p2Classy.getRange();
    }

    public void setBox(Box box) {
        p2Classy.setBox(box);
    }

    public int nBody() {
        return 2;
    }

    public double energy(IAtomList molecules) {
        double uC = p2Classy.energy(molecules);
        if (uC/temperature > 100) return Double.POSITIVE_INFINITY;
        IVector[][] gradAndTorque = p2Classy.gradientAndTorque(molecules);
        double sum = 0;
        for (int i=0; i<2; i++) {
            IAtom iMol = molecules.getAtom(i);
            double mi = iMol.getType().getMass();
            sum += gradAndTorque[0][i].squared()/mi;
            if (iMol instanceof IAtomOriented) {
                AtomInfo atomInfo = (AtomInfo)agents.getAgent(iMol.getType());
                if (atomInfo == null) {
                    throw new RuntimeException("You must provide AtomInfo for oriented atoms");
                }
                IVector[] momentAndAxes = atomInfo.getMomentAndAxes((IAtomOriented)iMol);
                IVector moment = momentAndAxes[0];
                for (int j=0; j<3; j++) {
                    if (moment.getX(j) < 1e-10) continue;
                    IVector axis = momentAndAxes[j+1];
                    double torque = gradAndTorque[1][i].dot(axis);
                    sum += torque*torque/moment.getX(j);
                }
            }
        }
        double uFull = uC + fac*sum;
//        System.out.println(uC+" "+uFull);
        return uFull;
    }

    public interface AtomInfo {
        
        /**
         * Returns the moment of inertia as a vector containing Ix, Iy, Iz
         * and also the principle axes.  The 0 element is the moment of inertia
         * vector while elements 1, 2 and 3 are the principle axes.
         */
        public IVector[] getMomentAndAxes(IAtomOriented molecule);
    }
}
