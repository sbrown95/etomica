/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.ensembles;


import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomType;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveAtom;
import etomica.integrator.mcmove.MCMoveInsertDelete;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.integrator.mcmove.MCMoveVolume;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Space3D;
import etomica.species.SpeciesGeneral;

public class LJMC extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public final SpeciesGeneral species;
    public final Box box;

    public final IntegratorMC integrator;
    public final MCMoveAtom mcMoveAtom;
    public final MCMoveVolume mcMoveVolume;
    public final MCMoveInsertDelete mcMoveID;

    public LJMC(Space _space) {
        super(_space);
        //species
        species = SpeciesGeneral.monatomic(space, AtomType.simpleFromSim(this), true);//index 1
        addSpecies(species);

        PotentialMasterCell potentialMaster = new PotentialMasterCell(this, 2.5, space);
        potentialMaster.setCellRange(2);
        int N = 200;  //number of atoms

        //controller and integrator
        box = this.makeBox();
        integrator = new IntegratorMC(potentialMaster, random, 1.0, box);
        getController().addActivity(new ActivityIntegrate(integrator));

        //instantiate several potentials for selection in combo-box
        P2LennardJones potential = new P2LennardJones(space);
        P2SoftSphericalTruncated p2Truncated = new P2SoftSphericalTruncated(space, potential, 2.5);
        potentialMaster.addPotential(p2Truncated, new AtomType[]{species.getLeafType(), species.getLeafType()});

        //construct box
        Vector dim = space.makeVector();
        dim.E(space.D() == 2 ? 15 : 10);
        box.getBoundary().setBoxSize(dim);
        box.setNMolecules(species, N);
        new ConfigurationLattice(space.D() == 2 ? (new LatticeOrthorhombicHexagonal(space)) : (new LatticeCubicFcc(space)), space).initializeCoordinates(box);
        potentialMaster.getNbrCellManager(box).assignCellAll();

        mcMoveAtom = new MCMoveAtom(random, potentialMaster, space);
        integrator.getMoveManager().addMCMove(mcMoveAtom);
        ((MCMoveStepTracker) mcMoveAtom.getTracker()).setMaxAdjustInterval(50000);
        ((MCMoveStepTracker) mcMoveAtom.getTracker()).setMinAdjustStep(1.05);

        integrator.getMoveEventManager().addListener(potentialMaster.getNbrCellManager(box).makeMCMoveListener());

        mcMoveVolume = new MCMoveVolume(potentialMaster, random, space, 1) {
            public boolean doTrial() {
                double vOld = box.getBoundary().volume();
                uOld = energyMeter.getDataAsScalar();
                hOld = uOld + pressure * vOld;
                biasOld = vBias.f(vOld);
                vScale = (2. * random.nextDouble() - 1.) * stepSize;
                vNew = vOld * Math.exp(vScale); //Step in ln(V)
                if (vNew < Math.pow(6, space.D())) return false;
                double rScale = Math.exp(vScale / D);
                inflate.setScale(rScale);
                inflate.actionPerformed();
                uNew = energyMeter.getDataAsScalar();
                hNew = uNew + pressure * vNew;
                return true;
            }
        };

        mcMoveID = new MCMoveInsertDelete(potentialMaster, random, space) {
            public void acceptNotify() {
                if (moleculeList.size() > 999 && insert) {
                    rejectNotify();
                    uNew = 0;
                    return;
                }
                super.acceptNotify();
            }
        };
        mcMoveID.setSpecies(species);
    }
    
    public static void main(String[] args) {
        Space space = Space3D.getInstance();
        if(args.length != 0) {
            try {
                int D = Integer.parseInt(args[0]);
                if (D == 3) {
                    space = Space3D.getInstance();
                }
            } catch(NumberFormatException e) {}
        }
            
        LJMC sim = new LJMC(space);
        sim.getController().runActivityBlocking(new ActivityIntegrate(sim.integrator, Long.MAX_VALUE));
    }
}
