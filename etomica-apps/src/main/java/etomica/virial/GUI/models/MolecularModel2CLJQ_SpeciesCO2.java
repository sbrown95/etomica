/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial.GUI.models;


import etomica.atom.AtomType;
import etomica.config.ConformationLinear;
import etomica.potential.P22CLJQ;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.ISpecies;
import etomica.species.SpeciesBuilder;
import etomica.units.Kelvin;
import etomica.virial.GUI.components.SimpleElementForSimilarSpecies;

public class MolecularModel2CLJQ_SpeciesCO2 implements IMolecularModel_SpeciesFactory,Cloneable{
	private static String MoleculeDisplayName = "CO2 - 2CCLJQ";
	private Space space;
	private double[] sigma;
	private double[] epsilon;
	private double[] moment;
	private SimpleElementForSimilarSpecies simpleElement;

	private double sigmaHSRef;
	
	
	private double bondLength;


	private int SpeciesID;
	
	private String[] PotentialSites = {"CO2"};
	
	
	
	public String getPotentialSiteAtIndex(int index) {
		return PotentialSites[index];
	}

	private int id;
	private static int numberOfInstances = 0;
	
	private String[] ComponentParameters  =  {"SIGMA","EPSILON", "MOMENT"};
			
	private String[] SharedComponentParameters ={"BONDL"};
	
	private String[][] ParamAndValues; 

	
	private String[][] ComponentValues = {{"3.0354",Double.toString(Kelvin.UNIT.toSim(125.317)),Double.toString(3.0255*Kelvin.UNIT.toSim(125.317)*Math.pow(3.0354,5))}};
	
	private String[] SharedComponentValues = {"2.1347"};
	
	
	private String[] SimEnvParameters = {"SIGMAHSREF"};
	
	private String[] SimEnvValues = {Double.toString(1.5*3.0354)};
	
	
	
	
	//Constructors for different Instantiations
	
	
	public MolecularModel2CLJQ_SpeciesCO2(){
		space = Space3D.getInstance();
		sigma = new double[PotentialSites.length];
		epsilon = new double[PotentialSites.length];
		moment = new double[PotentialSites.length];
		ParamAndValues=setParameterValues();
		//id=++numberOfInstances;
	}
	
	private String[][] setParameterValues() {
		
		int NoOfParam = ComponentParameters.length;
		int NoOfCommonParam = SharedComponentParameters.length;
		int NoOfSites = PotentialSites.length;
		int totalNoOfParam = NoOfParam*NoOfSites;
		String[][] ReturnArray = new String[totalNoOfParam][2];
		int index = 0;
		for(int i=0;i<NoOfSites;i++){
			for(int j=0;j<NoOfParam;j++){
				
				if(ComponentParameters[j]=="SIGMA"){
					setSigma(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				if(ComponentParameters[j]=="EPSILON"){
					setEpsilon(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				if(ComponentParameters[j]=="MOMENT"){
					setMoment(Double.parseDouble(ComponentValues[i][j]),i);
					
				}
				
				ReturnArray[index][0] = ComponentParameters[j]+PotentialSites[i];
				ReturnArray[index][1] = ComponentValues[i][j];
				index++;
			}
		}
		for(int k = 0;k<NoOfCommonParam;k++){
			if(SharedComponentParameters[k]=="BONDL"){
				setBondLength(Double.parseDouble(SharedComponentValues[k]));
			}
		}
		
		int NoOfSimEnvParam = 1;
		for(int l = 0;l<NoOfSimEnvParam;l++){
			
			
			if(SimEnvParameters[l]=="SIGMAHSREF"){
				setSigmaHSRef(Double.parseDouble(SimEnvValues[l]));
			}
		}
		return ReturnArray;
		
		
	}
	
	

	public double getSigmaHSRef() {
		return sigmaHSRef;
	}

	public void setSigmaHSRef(double sigmaHSRef) {
		this.sigmaHSRef = sigmaHSRef;
	}

	public int getId() {
		return id;
	}
	
	 public Object clone(){
		 try{
			 MolecularModel2CLJQ_SpeciesCO2 cloned = (MolecularModel2CLJQ_SpeciesCO2)super.clone();
			 return cloned;
		  }
		  catch(CloneNotSupportedException e){
		     System.out.println(e);
		     return null;
		   }
	 }

	
	public int getSpeciesID() {
		return SpeciesID;
	}

	public void setSpeciesID(int speciesID) {
		SpeciesID = speciesID;
	}

	//Creates the LJ Molecule Species
	public ISpecies createSpecies(){

		simpleElement = SimpleElementForSimilarSpecies.getInstance();
		return new SpeciesBuilder(space)
                .withConformation(new ConformationLinear(this.space, this.bondLength))
				.addCount(AtomType.element(simpleElement.getTSelement()), 2)
				.build();

	}

	public double getSigma(int index) {
		return sigma[index];
	}

	public void setSigma(double sigma, int index) {
		this.sigma[index] = sigma;
	}

	public double getEpsilon(int index) {
		return epsilon[index];
	}

	public void setEpsilon(double epsilon, int index) {
		this.epsilon[index] = epsilon;
	}

	public double getMoment(int index) {
		return moment[index];
	}

	public void setMoment(double moment, int index) {
		this.moment[index] = moment;
	}

	public double getBondLength() {
		return bondLength;
	}
	
	public void setBondLength(double bondLength) {
		this.bondLength = bondLength;
	}

	@Override
	public int getParameterCount() {
		return 4;
	}

	@Override
	public void setParameter(String Parameter, String ParameterValue) {
		// TODO Auto-generated method stub
		for(int i=0;i<PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMA.toString()+PotentialSites[i])){
				setSigma(Double.parseDouble(ParameterValue),i); 
				ParamAndValues[i][1] = ParameterValue;
			}
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.EPSILON.toString()+PotentialSites[i])){
				setEpsilon(Double.parseDouble(ParameterValue),i); 
				ParamAndValues[i][1] = ParameterValue;
			}
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.MOMENT.toString()+PotentialSites[i])){
				setMoment(Double.parseDouble(ParameterValue),i); 
				ParamAndValues[i][1] = ParameterValue;
			}
			
		}
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.BONDL.toString())){
			setBondLength(Double.parseDouble(ParameterValue)); 
		}
		
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMAHSREF.toString())){
			setSigmaHSRef(Double.parseDouble(ParameterValue)); 
		}
		
	}

	@Override
	public String getDescription(String Parameter) {
		String Description = null;
		for(int i = 0;i <PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMA.toString()+PotentialSites[i])){
				Description = EnumPotentialParamDescription.SIGMA.description();
			}
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.EPSILON.toString()+PotentialSites[i])){
				Description = EnumPotentialParamDescription.EPSILON.description();
			}
		
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.MOMENT.toString()+PotentialSites[i])){
				Description = EnumPotentialParamDescription.MOMENT.description();
			}
		}
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.BONDL.toString())){
			Description = EnumPotentialParamDescription.BONDL.description();
		}
		
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.TEMPERATURE.toString())){
			Description = EnumPotentialParamDescription.TEMPERATURE.description();
		}
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.STEPS.toString())){
			Description = EnumPotentialParamDescription.STEPS.description();
		}
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMAHSREF.toString())){
			Description = EnumPotentialParamDescription.SIGMAHSREF.description();
		}
		return Description;
	}

	//Testing Class
	public static void main(String[] args){
		MolecularModel2CLJQ_SpeciesCO2 lj = new MolecularModel2CLJQ_SpeciesCO2();
		
	}

	@Override
	public Double getDoubleDefaultParameters(String Parameter) {
		// TODO Auto-generated method stub
		Double parameterValue = null;
		for(int i=0;i<PotentialSites.length;i++){
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMA.toString()+PotentialSites[i])){
				parameterValue = getSigma(i);
			}
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.EPSILON.toString()+PotentialSites[i])){
				parameterValue = getEpsilon(i);
			}
		
			if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.MOMENT.toString()+PotentialSites[i])){
				parameterValue = getMoment(i);
			}
		}
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.BONDL.toString())){
			parameterValue = getBondLength();
		}
		
		if(Parameter.toUpperCase().equals(EnumPotentialParamDescription.SIGMAHSREF.toString())){
			parameterValue = getSigmaHSRef();
		}
		
		
		return parameterValue;
	}



	public String[] getParametersArray() {
		return ComponentParameters;
	}

	@Override
	public String getMolecularModelDisplayName() {
		// TODO Auto-generated method stub
		return "2CLJQ";
	}


	public String getPotentialSites(int index) {
		return PotentialSites[index];
	}

	@Override
	public String[][] getParamAndValues() {
		// TODO Auto-generated method stub
		return ParamAndValues;
	}

	@Override
	public String[] getPotentialSites() {
		// TODO Auto-generated method stub
		return PotentialSites;
	}

	
	@Override
	public String getMoleculeDisplayName() {
		// TODO Auto-generated method stub
		return MoleculeDisplayName;
	}

	@SuppressWarnings("rawtypes")
	
	public Class getPotential() {
		// TODO Auto-generated method stub
		return P22CLJQ.class;
	}
	
	@Override
	public Space getSpace() {
		// TODO Auto-generated method stub
		return this.space;
	}

	@Override
	public boolean hasElectrostaticInteraction() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getNonBondedInteractionModel() {
		// TODO Auto-generated method stub
		return "2CenteredLennardJonesWithQuadrapole";
	}


}
