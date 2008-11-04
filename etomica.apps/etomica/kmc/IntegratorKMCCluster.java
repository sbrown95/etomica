package etomica.kmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.BoxImposePbc;
import etomica.action.WriteConfiguration;
import etomica.action.XYZWriter;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomFilterTypeInstance;
import etomica.atom.iterator.AtomIteratorBoxDependent;
import etomica.atom.iterator.AtomIteratorFiltered;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.config.ConfigurationFile;
import etomica.data.meter.MeterMeanSquareDisplacement;
import etomica.dimer.IntegratorDimerMin;
import etomica.dimer.PotentialMasterListDimer;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorBox;
import etomica.nbr.list.PotentialMasterList;
import etomica.space.ISpace;

public class IntegratorKMCCluster extends IntegratorBox{

    private static final long serialVersionUID = 1L;
    IntegratorDimerMin integratorMin1, integratorMin2;
    IPotentialMaster potentialMaster;
    double temperature;
    private final ISpace space;
    IRandom random;
    ISimulation sim;
    ISpecies [] species;
    IVector [] minPosition, currentSaddle, previousSaddle;
    double[] saddleVib;
    double[] saddleEnergies;
    double[] rates;
    double tau;
    double msd;
    double beta;
    double minEnergy;
    double freqProd;
    double saddleEnergy;
    double minVib;
    boolean search;
    int searchNum;
    int kmcStep;
    int totalSearches;
    SimulationGraphic graphic;
    XYZWriter xyzMin1, xyzMin2;
    BoxImposePbc imposePbc;
    MeterMeanSquareDisplacement msd1, msd2;
    FileReader fileReader, writeTau;
    BufferedReader buffReader;
    FileWriter writer;
    
    public IntegratorKMCCluster(ISimulation _sim, IPotentialMaster _potentialMaster, double _temperature, int _totalSearches, IRandom _random, ISpecies [] _species, ISpace _space){
        super(_potentialMaster, _temperature);
        
        this.potentialMaster = _potentialMaster;
        this.temperature = _temperature;
        this.space = _space;
        this.random = _random;
        this.sim = _sim;
        this.species = _species;
        this.totalSearches = _totalSearches;

        
                
        // TODO Auto-generated constructor stub
    }

    protected void doStepInternal(){
        loadConfiguration((kmcStep-1)+"");

        for(int i=0; i<totalSearches; i++){
            try {
                FileWriter goWriter = new FileWriter(i+".go");
                goWriter.close();
            } catch (IOException e1) {        }
        }
        
        while(true){
            boolean success = true;
            for(int i=0; i<totalSearches; i++){
                if(!new File(i+".done").exists()){
                    success = false;
                }  
            }
            if(success){
                break;
            }
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e){        }
        }
        for(int i=0; i<totalSearches; i++){
            new File(i+".go").delete();            
        }
        for(int i=0; i<totalSearches; i++){
            new File(i+".done").delete();            
        }
        
        searchNum = 0;
        for(int i=0; i<saddleEnergies.length; i++){

            loadConfiguration("s_"+i+"_saddle");
            try {
                fileReader = new FileReader("s_"+i+"_s_ev");
                buffReader = new BufferedReader(fileReader);
                saddleEnergy = Double.parseDouble(buffReader.readLine());
                freqProd = Double.parseDouble(buffReader.readLine());
            }
            catch(IOException e) {}

            if(checkUniqueSaddle()){
                System.out.println("Good search "+i+", adding saddle data.");
                saddleEnergies[i] = saddleEnergy;
                saddleVib[i] = freqProd;
            }
            searchNum++;
        }
    
        calcRates();
        int rateNum = chooseRate();
        System.out.println("Rate "+rateNum+" is chosen.");
        System.out.println("Step "+(kmcStep-1)+": tau is "+tau);
                
        //Minimum Search with random transition
        integratorMin1.setFileName("s_"+rateNum);
        try {
            integratorMin1.initialize();
        } catch (ConfigurationOverlapException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        xyzMin1.setFileName((kmcStep-1)+"_s-A.xyz");
        writeConfiguration((kmcStep-1)+"_s");      
        
        for(int j=0;j<1000;j++){
            integratorMin1.doStep();
            if(integratorMin1.minFound){
                break;
            }
        }
        msd += msd1.getDataAsScalar();
        if(checkMin()){
            minEnergy = integratorMin1.e0;
            minVib = integratorMin1.vib.getProductOfFrequencies();
            writeConfiguration(kmcStep+"");
            writeConfiguration("searchStart");
            setInitialStateConditions(minEnergy, minVib);
            System.out.println("Good minimum found. Computing MSD for other direction...");
            integratorMin2.setFileName("s_"+rateNum);
            try {
                integratorMin2.initialize();
            } catch (ConfigurationOverlapException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            xyzMin2.setFileName((kmcStep-1)+"_s-B.xyz");
            for(int j=0;j<1000;j++){
                integratorMin2.doStep();
                if(integratorMin2.minFound){
                    break;
                }
            }
            msd += msd2.getDataAsScalar();
        }else{
            integratorMin2.setFileName("s_"+rateNum);
            //rename minimum 1 search XYZ file
            try {
                integratorMin2.initialize();
            } catch (ConfigurationOverlapException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            xyzMin2.setFileName((kmcStep-1)+"_s-A-right.xyz");
            for(int j=0;j<1000;j++){
                integratorMin2.doStep();
                if(integratorMin2.minFound){
                    break;
                }
            }
            msd += msd2.getDataAsScalar();
            minEnergy = integratorMin2.e0;
            minVib = integratorMin2.vib.getProductOfFrequencies();
            writeConfiguration(kmcStep+"");
            writeConfiguration("searchStart");
            setInitialStateConditions(minEnergy, minVib);
            System.out.println("Good minimum found on second attempt.");
        }
                
        try {
            writer = new FileWriter("tau-msd.dat", true);
            writer.write("-step "+kmcStep+"\n"+"tau: "+tau+"\n"+"msd: "+msd+"\n");
            writer.close();
            
            FileWriter writer2 = new FileWriter(kmcStep+"_ev");
            writer2.write(minEnergy+"\n"+minVib);
            writer2.close();
            
        }catch(IOException e) {
            
        }
        clearRatesandEnergies();
        msd1.reset();
        msd2.reset();
        kmcStep++;
    }
    
    public void setup(){
        search = true;
        saddleVib = new double[totalSearches];
        saddleEnergies = new double[totalSearches];
        
        msd = 0;
        tau = 0;
        searchNum = 0;
        kmcStep = 1;
        imposePbc = new BoxImposePbc(box, space);
        rates = new double[totalSearches];
        beta = 1.0/(temperature);
        currentSaddle = new IVector[box.getMoleculeList().getAtomCount()];
        previousSaddle = new IVector[box.getMoleculeList().getAtomCount()];
        for(int i=0; i<currentSaddle.length; i++){
            currentSaddle[i] = space.makeVector();
            previousSaddle[i] = space.makeVector();
        }
        
        createIntegrators();

    }
    
    public void setInitialStateConditions(double energy, double vibFreq){
        minEnergy = energy;
        minVib = vibFreq;
        
        IAtomSet loopSet2 = box.getMoleculeList();
        minPosition = new IVector[loopSet2.getAtomCount()];
        for(int i=0; i<minPosition.length; i++){
            minPosition[i] = space.makeVector();
        }
        
        for(int i=0; i<loopSet2.getAtomCount(); i++){
            minPosition[i].E(((IAtomPositioned)((IMolecule)loopSet2.getAtom(i)).getChildList().getAtom(0)).getPosition());
        }  
    }
    
    public void randomizePositions(){
        IVector workVector = space.makeVector();
        IAtomSet loopSet3 = box.getMoleculeList(species[0]);
        IVector [] currentPos = new IVector [loopSet3.getAtomCount()];
        double offset = 0;
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i] = (((IAtomPositioned)((IMolecule)loopSet3.getAtom(i)).getChildList().getAtom(0)).getPosition());
            for(int j=0; j<3; j++){
                offset = random.nextGaussian()/10.0;
                if(Math.abs(offset)>0.1){offset=0.1;}
                workVector.setX(j,offset);
            }
            currentPos[i].PE(workVector);
        }
    }
    
    public void calcRates(){
        //convert energies to Joules and use hTST
        double rateSum = 0;
        for(int i=0; i<rates.length; i++){
            if(saddleEnergies[i]==0){continue;}
            rates[i] = (minVib / saddleVib[i]) * Math.exp( -(saddleEnergies[i] - minEnergy)*beta);
            rateSum += rates[i];
        }
        //compute residence time
        tau += -Math.log(random.nextDouble())/rateSum;

    }
    
    public void clearRatesandEnergies(){
        for(int i=0; i<rates.length; i++){
            rates[i] = 0.0;
            saddleEnergies[i] = 0.0;
            saddleVib[i] = 0.0;
        }
    }

    public int chooseRate(){
        int rt = 0;
        double sum = 0;
        double rand = random.nextDouble();
        for(int q=0; q<rates.length; q++){
            sum += rates[q];
        }
        double sumgrt = 0;
        for(int i=0; i<rates.length; i++){
            sumgrt += rates[i];
            if(rand*sum<=sumgrt){
                rt = i;
                System.out.println("-----Choosing a rate-----");
                for(int l=0; l<rates.length; l++){ 
                    System.out.println("Rate "+l+": "+rates[l]);
                }
                System.out.println("Sum:    "+sum);
                System.out.println("-------------------------");
                System.out.println(rand*sum+" <= "+sumgrt);
                break;
            }
        }
        return rt;
    }
    
    private boolean checkUniqueSaddle(){    
        for(int p=0; p<box.getMoleculeList().getAtomCount(); p++){
            currentSaddle[p].E(((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(p)).getChildList().getAtom(0)).getPosition());
        }
        for(int i=0; i<searchNum; i++){
            double positionDiff = 0;
            loadConfiguration("s_"+i+"_saddle");
            for(int j=0; j<box.getMoleculeList().getAtomCount(); j++){
                previousSaddle[j].E(((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(j)).getChildList().getAtom(0)).getPosition());
                previousSaddle[j].ME(currentSaddle[j]);
                positionDiff += previousSaddle[j].squared();
            }
            if(positionDiff < 0.5){
                System.out.println("Duplicate saddle found.");
                return false;
            }
        }
        System.out.println("Unique saddle found.");
        return true;  
    }
    
    public boolean checkMin(){
        IVector workVector = space.makeVector();
        double positionDiff=0;
        for(int i=0; i<box.getMoleculeList().getAtomCount(); i++){
            workVector.Ev1Mv2(minPosition[i],((IAtomPositioned)((IMolecule)box.getMoleculeList().getAtom(i)).getChildList().getAtom(0)).getPosition());
            positionDiff += workVector.squared();
        }
        if(positionDiff > 0.5){return true;}
        return false;
    }
    
    public void writeConfiguration(String file){
        WriteConfiguration writer = new WriteConfiguration(space);
        writer.setBox(box);
        writer.setConfName(file);
        writer.actionPerformed();
    }
    
    public void loadConfiguration(String file){
        ConfigurationFile config = new ConfigurationFile(file);
        config.initializeCoordinates(box);
    }
    
    public void createIntegrators(){
        integratorMin1 = new IntegratorDimerMin(sim, potentialMaster, species, true, space);
        integratorMin2= new IntegratorDimerMin(sim, potentialMaster, species, false, space);
        
        integratorMin1.setBox(box);
        integratorMin2.setBox(box);
        
        if(potential instanceof PotentialMasterListDimer){
            integratorMin1.addNonintervalListener(((PotentialMasterList)potentialMaster).getNeighborManager(box));
            integratorMin2.addIntervalAction(((PotentialMasterList)potentialMaster).getNeighborManager(box)); 
        }
                
        xyzMin1 = new XYZWriter(box);
        xyzMin2 = new XYZWriter(box);
        xyzMin1.setIsAppend(true);
        xyzMin2.setIsAppend(true);
        
        integratorMin1.addIntervalAction(xyzMin1);
        integratorMin2.addIntervalAction(xyzMin2);
        integratorMin1.setActionInterval(xyzMin1, 5);
        integratorMin2.setActionInterval(xyzMin2, 5);
        integratorMin1.addIntervalAction(imposePbc);
        integratorMin2.addIntervalAction(imposePbc);
        integratorMin1.setActionInterval(imposePbc, 1);
        integratorMin2.setActionInterval(imposePbc, 1);
        
        //Limit MSD calculation to a specific species
        AtomIteratorFiltered aif = AtomIteratorFiltered.makeIterator(new AtomIteratorLeafAtoms(box),new AtomFilterTypeInstance(species[0].getChildType(0)));
        msd1 = new MeterMeanSquareDisplacement(space, integratorMin1);
        msd2 = new MeterMeanSquareDisplacement(space, integratorMin2);
        msd1.setIterator((AtomIteratorBoxDependent)aif);
        msd2.setIterator((AtomIteratorBoxDependent)aif);
    }
    
    
}
