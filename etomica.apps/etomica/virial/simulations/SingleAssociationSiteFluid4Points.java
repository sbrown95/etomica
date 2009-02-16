package etomica.virial.simulations;

import etomica.api.IAction;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.potential.P2HardAssociationCone;
import etomica.potential.P2MoleculeMonatomic;
import etomica.potential.P2SoftSphere;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.util.Arrays;
import etomica.util.ParameterBase;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterBonds;
import etomica.virial.ClusterSum;
import etomica.virial.ConfigurationClusterMove;
import etomica.virial.MayerEHardSphere;
import etomica.virial.MayerESpherical;
import etomica.virial.MayerFunction;
import etomica.virial.MayerFunctionSumGeneral;
import etomica.virial.MayerGeneral;
import etomica.virial.MayerGeneralSpherical;
import etomica.virial.MayerHardSphere;
import etomica.virial.SpeciesFactoryOrientedSpheres;
import etomica.virial.cluster.Standard;
import etomica.virial.simulations.SingleAssociationSiteFluid.VirialAssociatingFluidParam;

public class SingleAssociationSiteFluid4Points {


	public static void main(String[] args) {
		VirialAssociatingFluidParam params = new VirialAssociatingFluidParam();
		Space space = Space3D.getInstance();
		
		int rhopoint=params.rhopoint;
		int rho0point=params.rho0point;
		int n=12; // repulsive part
		int diagramIndex = params.diagramIndex;
		final int nBody = rhopoint + rho0point;
		double temperature=params.temperature;
		double sigmaHSRef = params.sigmaHSRef;
		long numSteps = params.numSteps;
		
		if (args.length == 6) {
        	temperature = Double.parseDouble(args[0]);
            sigmaHSRef = Double.parseDouble(args[1]);
            numSteps = Integer.parseInt(args[2]);
            rhopoint = Integer.parseInt(args[3]);
            rho0point = Integer.parseInt(args[4]);
            diagramIndex = Integer.parseInt(args[5]);
            
        } else if (args.length != 0){
        	throw new IllegalArgumentException("Wrong number of arguments");
        }
		double sigma=1;
		double epsilon=1;
		final double[] HSB = new double[9];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B2HS: "+HSB[2]);
        System.out.println("B3HS: "+HSB[3]+" = "+(HSB[3]/(HSB[2]*HSB[2]))+" B2HS^2");
        System.out.println("B4HS: "+HSB[4]+" = "+(HSB[4]/(HSB[2]*HSB[2]*HSB[2]))+" B2HS^3");
		P2HardAssociationCone p = new P2HardAssociationCone(space, sigma, epsilon, Double.POSITIVE_INFINITY, 20.0); //Lennard-Jones potential+square-well site-site attraction potential, 1.0=cutoffFactor
		P2MoleculeMonatomic pMolecule = new P2MoleculeMonatomic(p);
		P2SoftSphere pR = new P2SoftSphere(space, sigma, 4*epsilon, n);//repulsion potential in LJ
		MayerGeneralSpherical fR = new MayerGeneralSpherical(space, pR);//repulsion Mayer fR function
		MayerESpherical eR = new MayerESpherical(space, pR);//repulsion eR function
		MayerGeneral f = new MayerGeneral(pMolecule);//usual Mayer f function
		MayerFunctionSumGeneral F = new MayerFunctionSumGeneral(space, new MayerFunction[]{f,fR}, new double[]{1,-1});//F=f-fR
		
		MayerHardSphere fRef = new MayerHardSphere(space,sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(space,sigmaHSRef);
		
		int nBondTypes = 3;//fR,F,eR
		ClusterBonds[] clusters = new ClusterBonds[0];
		int[][][] bondList = new int[nBondTypes][][];	
        ClusterSum targetCluster = null;
            		
        if (rhopoint == 4 && rho0point == 0) {
			 if (diagramIndex == 0){
			bondList[0]=new int [][]{{0,1},{1,2},{2,3},{3,0}};		
			clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
			targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			 }
			 else if (diagramIndex == 1) {
				 bondList[0]=new int [][]{{0,1},{1,2},{2,3},{3,0},{0,2}};		
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			 }
			 else if (diagramIndex == 2) {
				 bondList[0]=new int [][]{{0,1},{1,2},{2,3},{3,0},{0,2},{3,1}};		
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
				 
			 }
			
		}   else if (rho0point ==2 && rhopoint ==2) {
				if (diagramIndex == 0){
					bondList[0] = new int [][]{{1,2},{2,3},{3,0}};
					bondList[1] = new int [][]{{0,1}};
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
				}
				else if (diagramIndex == 1) {
					bondList[0] = new int [][]{{1,2},{2,3},{3,0},{0,2}};
					bondList[1] = new int [][]{{0,1}};
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
				}
				else if (diagramIndex == 2) {
					bondList[0] = new int [][]{{0,1},{1,2},{2,3},{3,0}};
					bondList[1] = new int [][]{{0,2}};
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
				}
				else if (diagramIndex == 3) {
					bondList[0] = new int [][]{{1,2},{2,3},{3,0},{0,2},{3,1}};
					bondList[1] = new int [][]{{0,1}};
					clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
					targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
				}

		}  else if (rhopoint == 0 && rho0point == 4) {
			if (diagramIndex == 0){
				bondList[0] = new int [][]{{1,2},{3,0}};
				bondList[1] = new int [][]{{0,1},{2,3}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 1) {
				bondList[0] = new int [][]{{1,2},{3,0},{0,2}};
				bondList[1] = new int [][]{{0,1},{2,3}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 2) {
				bondList[0] = new int [][]{{1,2},{3,0},{0,2},{3,1}};
				bondList[1] = new int [][]{{0,1},{2,3}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 3) {
				bondList[1] = new int [][]{{2,3},{3,0},{3,1}};
				bondList[2] = new int [][]{{0,1},{1,2},{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 4) {
				bondList[1] = new int [][]{{1,2},{2,3},{3,0}};
				bondList[2] = new int [][]{{0,1},{1,3},{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 5) {
				bondList[1] = new int [][]{{0,2},{2,3},{3,0},{3,1}};
				bondList[2] = new int [][]{{0,1},{1,2}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 6) {
				bondList[1] = new int [][]{{0,1},{1,2},{2,3},{3,0}};
				bondList[2] = new int [][]{{0,2},{3,1}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 7) {
				bondList[1] = new int [][]{{0,1},{1,2},{2,3},{3,0},{3,1}};
				bondList[2] = new int [][]{{0,2}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 8) {
				bondList[1] = new int [][]{{0,1},{1,2},{2,3},{3,0},{3,1},{1,2}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			
		} else if (rhopoint == 1 && rho0point == 3) {
			if (diagramIndex == 0){  
				bondList[0] = new int [][]{{2,3},{3,0}};
				bondList[1] = new int [][]{{0,1},{1,2}};
				bondList[2] = new int [][]{{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 1){  
				bondList[0] = new int [][]{{2,3},{3,0}};
				bondList[1] = new int [][]{{0,1},{2,0}};
				bondList[2] = new int [][]{{1,2}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 2){  
				bondList[0] = new int [][]{{2,3},{3,0},{3,1}};
				bondList[1] = new int [][]{{0,1},{1,2}};
				bondList[2] = new int [][]{{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 3){  
				bondList[0] = new int [][]{{2,3},{3,0}};
				bondList[1] = new int [][]{{0,1},{1,2},{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			else if (diagramIndex == 4){  
				bondList[0] = new int [][]{{2,3},{3,0},{3,1}};
				bondList[1] = new int [][]{{0,1},{1,2},{2,0}};
				clusters = (ClusterBonds[])Arrays.addObject(clusters,new ClusterBonds(nBody, bondList, false));
				targetCluster = new ClusterSum(clusters,new double []{1}, new MayerFunction[]{fR,F,eR});
			}
			}
		 
			else {
			throw new RuntimeException("This is strange");
		} 

		
		ClusterAbstract refCluster = Standard.virialCluster(nBody, fRef, nBody>3, eRef, true);
        refCluster.setTemperature(temperature);
        targetCluster.setTemperature(temperature);
		final SimulationVirialOverlap sim = new SimulationVirialOverlap(space, new SpeciesFactoryOrientedSpheres(), temperature, refCluster, targetCluster);
		ConfigurationClusterMove configuration = new ConfigurationClusterMove(space, sim.getRandom());
		configuration.initializeCoordinates(sim.box[1]);
		sim.setAccumulatorBlockSize((int)numSteps*10);
		sim.integratorOS.setNumSubSteps(1000);
        // if running interactively, don't use the file
        String refFileName = args.length > 0 ? "refpref"+rhopoint+"_"+rho0point+"_"+temperature : null;
        // this will either read the refpref in from a file or run a short simulation to find it
//        sim.setRefPref(1.0082398078547523);
        sim.initRefPref(refFileName, numSteps/100);
        // run another short simulation to find MC move step sizes and maybe narrow in more on the best ref pref
        // if it does continue looking for a pref, it will write the value to the file
        sim.equilibrate(refFileName, numSteps/40);
        
        System.out.println("equilibration finished");

        IAction progressReport = new IAction() {
            public void actionPerformed() {
                System.out.print(sim.integratorOS.getStepCount()+" steps: ");
                double ratio = sim.dsvo.getDataAsScalar();
                double error = sim.dsvo.getError();
                System.out.println("abs average: "+ratio*HSB[nBody]+", error: "+error*HSB[nBody]);
            }
        };
        sim.integratorOS.addIntervalAction(progressReport);
        sim.integratorOS.setActionInterval(progressReport, (int)(numSteps/10));
        
        sim.integratorOS.getMoveManager().setEquilibrating(false);
        sim.ai.setMaxSteps(numSteps);
        for (int i=0; i<2; i++) {
            System.out.println("MC Move step sizes "+sim.mcMoveTranslate[i].getStepSize());
        }
        sim.getController().actionPerformed();

        System.out.println("final reference step frequency "+sim.integratorOS.getStepFreq0());
        
        double ratio = sim.dsvo.getDataAsScalar();
        double error = sim.dsvo.getError();
        System.out.println("ratio average: "+ratio+", error: "+error);
        System.out.println("abs average: "+ratio*HSB[nBody]+", error: "+error*HSB[nBody]);
        DataGroup allYourBase = (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("hard sphere ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("hard sphere   average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("hard sphere overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
        
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.accumulators[1].getNBennetPoints()-sim.dsvo.minDiffLocation()-1);
        System.out.println("lennard jones ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("lennard jones average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("lennard jones overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
    }
    
	
	public static class VirialAssociatingFluidParam extends ParameterBase {
		public double temperature = 0.7;//reduced temperature
		public double sigmaHSRef = 1.5;
		public long numSteps = 1000000;
		public int rhopoint = 4;
		public int rho0point = 0;
		public int diagramIndex = 0;
		
	}

}
