package etomica.modules.pistoncylinder;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import etomica.Action;
import etomica.Constants;
import etomica.DataManager;
import etomica.DataSink;
import etomica.DataSource;
import etomica.Default;
import etomica.Modifier;
import etomica.Phase;
import etomica.atom.AtomTypeSphere;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorHistory;
import etomica.data.DataSourceAdapter;
import etomica.data.DataSourceCountSteps;
import etomica.data.DataSourceUniform;
import etomica.data.meter.MeterTemperature;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceBox;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceToggleButton;
import etomica.graphics.DeviceTrioControllerButton;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayCanvasInterface;
import etomica.graphics.DisplayPhase;
import etomica.graphics.PropertyDisplayBoxes;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.modifier.ModifierBoolean;
import etomica.modifier.ModifierFunctionWrapper;
import etomica.modifier.ModifierGeneral;
import etomica.potential.P2HardSphere;
import etomica.potential.P2SquareWell;
import etomica.potential.Potential2HardSphericalWrapper;
import etomica.potential.PotentialGroup;
import etomica.units.Bar;
import etomica.units.BaseUnit;
import etomica.units.BaseUnitPseudo3D;
import etomica.units.Dimension;
import etomica.units.Joule;
import etomica.units.Kelvin;
import etomica.units.Liter;
import etomica.units.Mole;
import etomica.units.Prefix;
import etomica.units.PrefixedUnit;
import etomica.units.Unit;
import etomica.units.UnitRatio;


public class PistonCylinderGraphic {
    
    public JPanel panel, displayPhasePanel;
    public PistonCylinder pc;
    public Potential2HardSphericalWrapper potentialWrapper;
    public P2HardSphere potentialHS;
    public P2SquareWell potentialSW;
    public PotentialGroup potentialGroupHS, potentialGroupSW;
    public DataSourceCountSteps meterCycles;
    public DataManager densityManager, temperatureManager, pressureManager;
    public DisplayBox displayCycles, tBox; 
    public MeterTemperature thermometer;
    public DisplayPhase displayPhase;
    public DeviceTrioControllerButton controlButtons;
    public ItemListener potentialChooserListener;
    public JComboBox potentialChooser;
    public DeviceSlider scaleSlider, pressureSlider, temperatureSlider;
    public JPanel pressureSliderPanel;
    public MeterPistonDensity densityMeter;
    public DeviceToggleButton fixPistonButton;
    public DisplayPlot plotT, plotD, plotP;
    public final javax.swing.JTabbedPane displayPanel;
    public DeviceBox sigBox, epsBox, lamBox;
	private PropertyDisplayBoxes densityDisplayBox, temperatureDisplayBox, pressureDisplayBox;
    final JRadioButton buttonAdiabatic, buttonIsothermal;
    final JPanel blankPanel = new JPanel();
    public int historyLength;
    public DataSourceWallPressure pressureMeter;
    
    public PistonCylinderGraphic() {
        Default.BLOCK_SIZE = 100;
        displayPhase = new DisplayPhase(null);
        displayPhase.setColorScheme(new ColorSchemeByType());

        displayCycles = new DisplayBox();

        Default.ATOM_SIZE = 3.0;
        historyLength = 100;

        final int p0 = 500;
        
        //restart action and button
        controlButtons = new DeviceTrioControllerButton();
        
        //adiabatic/isothermal radio button
        ButtonGroup thermalGroup = new ButtonGroup();
        buttonAdiabatic = new JRadioButton("Adiabatic");
        buttonIsothermal = new JRadioButton("Isothermal");
        buttonAdiabatic.setSelected(true);
        thermalGroup.add(buttonAdiabatic);
        thermalGroup.add(buttonIsothermal);
        buttonIsothermal.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                pc.controller.doActionNow( new Action() {
                    public void actionPerformed() {
                        pc.integrator.setIsothermal(buttonIsothermal.isSelected());
                    }
                    public String getLabel() {return "";}
                });
            }
        });
        
        //temperature selector
        temperatureSlider = new DeviceSlider(null);
        temperatureSlider.setShowValues(true);
        temperatureSlider.setEditValues(true);
        temperatureSlider.setMinimum(0);
        temperatureSlider.setMaximum(1000);
        temperatureSlider.getSlider().setMajorTickSpacing(200);
        temperatureSlider.getSlider().setMinorTickSpacing(50);
        temperatureSlider.getSlider().setLabelTable(
        temperatureSlider.getSlider().createStandardLabels(200,100));
        temperatureSlider.setValue(300);

	    //combo box to select potentials
//	    final AtomPairIterator iterator = potentialDisks.iterator();
	    potentialChooser = new javax.swing.JComboBox(new String[] {
	        "Ideal gas", "Repulsion only", "Repulsion and attraction"});
	    potentialChooser.setSelectedIndex(0);

        sigBox = new DeviceBox();
        epsBox = new DeviceBox();
        lamBox = new DeviceBox();
        
//        displayPhase.canvas.setDrawBoundary(DisplayCanvasInterface.DRAW_BOUNDARY_NONE);
//        displayPhase.getOriginShift()[0] = thickness;
//        displayPhase.getOriginShift()[1] = -thickness;
	    
	    //slider for scale of display
	    ModifierFunctionWrapper scaleModulator = new ModifierFunctionWrapper(displayPhase, "scale");
	    scaleModulator.setFunction(new etomica.utility.Function.Linear(0.01, 0.0));
	    scaleSlider = new DeviceSlider(null, scaleModulator);
	    JPanel scaleSliderPanel = new JPanel(new java.awt.GridLayout(0,1));
	    scaleSliderPanel.add(scaleSlider.graphic());	    
//        scaleSliderPanel.setBorder(new javax.swing.border.TitledBorder("Scale (%)"));
	    scaleSlider.getSlider().addChangeListener(new javax.swing.event.ChangeListener() {
	        public void stateChanged(javax.swing.event.ChangeEvent evt) {
	            if(displayPhase.graphic() != null) displayPhase.graphic().repaint();
	        }
	    });
	    scaleSlider.setMinimum(10);
	    scaleSlider.setMaximum(100);
//	    scaleSlider.getSlider().setSnapToTicks(true);
	    scaleSlider.getSlider().setValue(100);
	    scaleSlider.getSlider().setMajorTickSpacing(10);
	    scaleSlider.getSlider().setMinorTickSpacing(5);
	    scaleSlider.getSlider().setOrientation(1);
	    scaleSlider.getSlider().setLabelTable(scaleSlider.getSlider().createStandardLabels(10));
		
		//add meter and display for current kinetic temperature
		Default.HISTORY_PERIOD = 1000;

		thermometer = new MeterTemperature();
		tBox = new DisplayBox();
        tBox.setUpdateInterval(10);
//		tBox.setWhichValue(MeterAbstract.CURRENT);
		tBox.setLabel("Measured value");
		tBox.setLabelPosition(Constants.NORTH);

        //plot of temperature and density histories
/*		DisplayPlot plotD = new DisplayPlot(this);
		DisplayPlot plotT = new DisplayPlot(this);
		plotD.setDataSources(densityHistory);
        plotD.setUnit(dUnit);
		plotT.setDataSources(temperatureHistory);
		plotT.setUnit(tUnit);
		plotD.setLabel("Density");
		plotT.setLabel("Temperature");
		plotT.getPlot().setYRange(0.0,1500.);
*/		
		//display of averages
/*		DisplayTable table = new DisplayTable(this);
		table.setUpdateInterval(20);
		table.setWhichValues(new MeterAbstract.ValueType[] {
		                MeterAbstract.CURRENT, MeterAbstract.AVERAGE, MeterAbstract.ERROR});
        this.mediator().addMediatorPair(new Mediator.DisplayMeter.NoAction(this.mediator()));
*/				
		
		//pressure device
//        sliderModulator.setFunction(pressureRescale);
//        pressureSlider = new DeviceSelectPressure(controller,integrator);
        pressureSlider = new DeviceSlider(null);
        pressureSlider.setShowValues(true);
        pressureSlider.setEditValues(true);
        pressureSlider.setMinimum(0);
        pressureSlider.setMaximum(1000);
	    pressureSlider.getSlider().setMajorTickSpacing(200);
	    pressureSlider.getSlider().setMinorTickSpacing(50);
	    pressureSlider.getSlider().setLabelTable(
        pressureSlider.getSlider().createStandardLabels(200,100));
	    pressureSlider.setValue(p0);
        
        //set-pressure history
//        etomica.MeterScalar pressureSetting = new MeterDatumSourceWrapper(pressureSlider.getModulator());
//        pressureSetting.setHistorying(true);
//        History pHistory = pressureSetting.getHistory();
//        pHistory.setLabel("Set Pressure");
/*        DisplayPlot plotP = new DisplayPlot(this);
        plotP.setDataSources(pHistory);
        plotP.setUnit(pUnit);
        plotP.setLabel("Pressure"); 
        plotP.getPlot().setYRange(0.0, 1500.);
*/        
        //measured pressure on piston
        //wrap it in a MeterDatumSource wrapper because we want to average pressure
        //over a longer interval than is used by other meters.  By wrapping it
        //we can still have history synchronized with others
//        Atom piston = ((SpeciesAgent)pistonCylinder.getAgent(phase)).node.firstLeafAtom();
//        piston.coord.setMass(pistonMass);
//        etomica.MeterScalar pressureMeter = ((AtomType.Wall)piston.type).new MeterPressure(this);
//        pressureMeter.setUpdateInterval(10);
//        pressureMeter.setFunction(pressureScale);
//        etomica.MeterDatumSourceWrapper pressureMeterWrapper = new MeterDatumSourceWrapper(pressureMeter);
////        pressureMeterWrapper.setFunction(pressureRescale);
//        pressureMeterWrapper.setWhichValue(MeterAbstract.MOST_RECENT);
//        pressureMeterWrapper.setHistorying(true);
//        History pMeterHistory = pressureMeterWrapper.getHistory();
//        pMeterHistory.setLabel("Pressure ("+pUnit.symbol()+")");
        
//        DisplayPlot plot = new DisplayPlot(this);
//        plot.setDataSources(new DataSource[] {
//                densityHistory, temperatureHistory, pMeterHistory, pHistory});
//        plot.setLabel("History");
//        plot.getPlot().setYLabel("");
//        plot.getPlot().setYRange(0.0, 1500.);
//        plot.setYUnit(new Unit[] {dadUnit, tUnit, pUnit, pUnit});
        
        fixPistonButton = new DeviceToggleButton(null);

        plotD = new DisplayPlot();
        plotT = new DisplayPlot();
        plotP = new DisplayPlot();


        //************* Lay out components ****************//
        
        panel = new JPanel();
        panel.setLayout(new java.awt.BorderLayout());      

        //tabbed pane for the big displays
    	displayPanel = new javax.swing.JTabbedPane();
    	displayPhasePanel = new javax.swing.JPanel(new java.awt.BorderLayout());
    	displayPhasePanel.add(scaleSliderPanel,java.awt.BorderLayout.EAST);
        
        JPanel plotPanel = new JPanel(new java.awt.GridLayout(0,1));
        plotPanel.add(plotD.graphic());
        plotPanel.add(plotT.graphic());
        plotPanel.add(plotP.graphic());
        displayPanel.add("Plots",new javax.swing.JScrollPane(plotPanel));

        JPanel startPanel = (JPanel)controlButtons.graphic();
        java.awt.GridBagConstraints gbc0 = new java.awt.GridBagConstraints();
        startPanel.setBorder(new javax.swing.border.TitledBorder("Control"));
        gbc0.gridx = 0; gbc0.gridy = 0;
        gbc0.gridx = 0; gbc0.gridy = 2; gbc0.gridwidth = 2;
        startPanel.add(fixPistonButton.graphic(null), gbc0);
        startPanel.setLayout(new GridLayout(2,2));

        //panel for the temperature control/display
        JPanel temperaturePanel = new JPanel(new java.awt.GridBagLayout());
        temperaturePanel.setBorder(new javax.swing.border.TitledBorder("Temperature (K)"));
        java.awt.GridBagConstraints gbc1 = new java.awt.GridBagConstraints();
        gbc1.gridx = 0;  gbc1.gridy = 1;
        gbc1.gridwidth = 1;
        temperaturePanel.add(buttonAdiabatic,gbc1);
        gbc1.gridx = 1;  gbc1.gridy = 1;
        gbc1.gridwidth = 1;
        temperaturePanel.add(buttonIsothermal,gbc1);
        gbc1.gridx = 0;  gbc1.gridy = 2;
        gbc1.gridwidth = 2;
        temperaturePanel.add(temperatureSlider.graphic(),gbc1);
        gbc1.gridx = 0;  gbc1.gridy = 3;
        temperaturePanel.add(tBox.graphic(),gbc1);
        
        //panel for pressure slider
        pressureSliderPanel = new JPanel(new java.awt.GridLayout(0,1));
        pressureSlider.setShowBorder(false);
        pressureSliderPanel.add(pressureSlider.graphic());
        
        //panel for all the controls
        JPanel dimensionPanel = new JPanel(new GridLayout(1,0));
        ButtonGroup dimensionGroup = new ButtonGroup();
        final JRadioButton button2D = new JRadioButton("2D");
        JRadioButton button3D = new JRadioButton("3D");
        button2D.setSelected(true);
        dimensionGroup.add(button2D);
        dimensionGroup.add(button3D);
        dimensionPanel.add(button2D);
        dimensionPanel.add(button3D);
        button2D.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent evt) {
               if(button2D.isSelected()) {
                   setSimulation(new PistonCylinder(2));
               } else {
                   setSimulation(new PistonCylinder(3));
               }
           }
        });

//        JPanel controlPanel = new JPanel(new java.awt.GridLayout(0,1));
        JPanel controlPanel = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc2 = new java.awt.GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = java.awt.GridBagConstraints.RELATIVE;
        controlPanel.add(dimensionPanel);
        controlPanel.add(startPanel,gbc2);
        java.awt.GridBagConstraints gbc3 = new java.awt.GridBagConstraints();
        gbc3.gridx = java.awt.GridBagConstraints.RELATIVE;
        gbc3.gridy = 0;
        controlPanel.add(displayCycles.graphic(),gbc2);
        
        JPanel statePanel = new JPanel(new GridBagLayout());
        statePanel.add(temperaturePanel, gbc2);
        statePanel.add(pressureSliderPanel, gbc2);

        JPanel potentialPanel = new JPanel(new GridBagLayout());
        potentialPanel.add(potentialChooser,gbc2);
	    potentialPanel.setBorder(new javax.swing.border.TitledBorder("Potential selection"));
	    JPanel parameterPanel = new JPanel(new GridLayout(0,1));
        parameterPanel.add(sigBox.graphic());
        parameterPanel.add(epsBox.graphic());
        parameterPanel.add(lamBox.graphic());
        potentialPanel.add(parameterPanel,gbc2);
        
        JTabbedPane setupPanel = new JTabbedPane();
        setupPanel.add(statePanel, "State");
        setupPanel.add(potentialPanel, "Potential");
        
        JPanel dataPanel = new JPanel(new GridBagLayout());
        
        //panel for the density, temperature, and pressure displays
        
        densityDisplayBox = new PropertyDisplayBoxes();
        densityDisplayBox.setLabel("Density (mol/L)");
        densityDisplayBox.setLabelType(DisplayBox.BORDER);
        dataPanel.add(densityDisplayBox.graphic());
        
        temperatureDisplayBox = new PropertyDisplayBoxes();
        temperatureDisplayBox.setLabel("Temperature (K)");
        temperatureDisplayBox.setLabelType(DisplayBox.BORDER);
        dataPanel.add(temperatureDisplayBox.graphic());
        
        pressureDisplayBox = new PropertyDisplayBoxes();
        pressureDisplayBox.setLabel("Pressure (bar)");
        pressureDisplayBox.setLabelType(DisplayBox.BORDER);
        dataPanel.add(pressureDisplayBox.graphic());
        
        JPanel leftPanel = new JPanel(new GridBagLayout());
        
        leftPanel.add(controlPanel, gbc2);
        leftPanel.add(setupPanel, gbc2);
        leftPanel.add(dataPanel, gbc2);
        
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(displayPanel, BorderLayout.EAST);

        setSimulation(new PistonCylinder(2));
    }
    
    public void setSimulation(PistonCylinder sim) {
        boolean pistonHeld = true;
        if (pc != null) {
            pistonHeld = pc.pistonPotential.isStationary();
            pc.getController().halt();
        }
        pc = sim;
        int D = pc.space.D();

        BaseUnit.Length.Sim.TO_PIXELS = 800/pc.phase.boundary().dimensions().x(1);
        pc.ai.setDoSleep(true);
        if (pc.space.D() == 2) {
            pc.ai.setSleepPeriod(10);
        }
        else {
            pc.ai.setSleepPeriod(1);
        }

        pc.integrator.setTimeStep(0.5);
        pc.integrator.clearIntervalListeners();

        pc.wallPotential.setLongWall(0,true,true);  // left wall
        pc.wallPotential.setLongWall(0,false,true); // right wall
        // skip top wall
        pc.wallPotential.setLongWall(1,false,false);// bottom wall
        pc.wallPotential.setPhase(pc.phase);  // so it has a boundary
        
        if (displayPhase.graphic() != null) {
            displayPhasePanel.remove(displayPhase.graphic());
            displayPanel.remove(displayPhasePanel);
            displayPanel.remove(blankPanel);
        }
        if (D == 2) {
            displayPanel.insertTab(displayPhase.getLabel(), null, displayPhasePanel, "", 0);
//            displayPanel.add(displayPhase.getLabel(), displayPhasePanel);
            displayPhase.setPhase(pc.phase);
            displayPhase.setAlign(1,DisplayPhase.BOTTOM);
            displayPhase.canvas.setDrawBoundary(DisplayCanvasInterface.DRAW_BOUNDARY_NONE);
            displayPhase.getDrawables().clear();
            displayPhase.addDrawable(pc.pistonPotential);
            displayPhase.addDrawable(pc.wallPotential);
            displayPhasePanel.add(displayPhase.graphic(),java.awt.BorderLayout.WEST);
            pc.integrator.addIntervalListener(displayPhase);
        } else {
            
            displayPanel.add("Run Faster", blankPanel);
        }
                
        meterCycles = new DataSourceCountSteps(pc.integrator);
        displayCycles.setDataSource(meterCycles);
        displayCycles.setLabel("Integrator steps");
        pc.integrator.addIntervalListener(displayCycles);
        controlButtons.setSimulation(pc);

        
        Unit tUnit = Kelvin.UNIT;
        pc.integrator.setIsothermal(buttonIsothermal.isSelected());
        pc.integrator.setTemperature(tUnit.toSim(temperatureSlider.getValue()));
        temperatureSlider.setUnit(tUnit);
        temperatureSlider.setModifier(new ModifierGeneral(pc.integrator,"temperature"));
        temperatureSlider.setController(pc.getController());

        //initialize for ideal gas
        potentialSW = new P2SquareWell();
        potentialHS = new P2HardSphere();
/*        pc.potentialMaster.setSpecies(potentialHS, new Species[] {pc.species, pc.species});
        pc.potentialMaster.setEnabled(potentialHS, false);
        pc.potentialMaster.setEnabled(potentialSW, false);*/
        
        if(potentialChooserListener != null) potentialChooser.removeItemListener(potentialChooserListener);
        
        potentialChooserListener = new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                if(evt.getStateChange() == java.awt.event.ItemEvent.DESELECTED) return;
                setPotential((String)evt.getItem());
            }
        };
        potentialChooser.addItemListener(potentialChooserListener);
        setPotential((String)potentialChooser.getSelectedItem());

        ModifierAtomDiameter sigModifier = new ModifierAtomDiameter();
        ModifierGeneral epsModifier = new ModifierGeneral(potentialSW, "epsilon");
        ModifierGeneral lamModifier = new ModifierGeneral(potentialSW, "lambda");
        sigBox.setModifier(sigModifier);
        epsBox.setModifier(epsModifier);
        epsBox.setUnit(new UnitRatio(Joule.UNIT,Mole.UNIT));
        lamBox.setModifier(lamModifier);
        sigBox.setController(pc.getController());
        epsBox.setController(pc.getController());
        lamBox.setController(pc.getController());
        
        plotD.removeAllSinks();
        plotT.removeAllSinks();
        plotP.removeAllSinks();
        
        thermometer.setPhase(new Phase[] {pc.phase});
        AccumulatorHistory temperatureHistory = new AccumulatorHistory();
        temperatureHistory.setHistoryLength(historyLength);
        temperatureManager = new DataManager(thermometer,temperatureHistory);
        temperatureManager.setUpdateInterval(10);
        DataManager temperaturePlotManager = new DataManager(temperatureHistory, plotT.makeDataSink());
        temperaturePlotManager.setUpdateInterval(10);
        tBox.setDataSource(thermometer);
        tBox.setUnit(tUnit);
        
        DataSource targetPressureDataSource = new DataSourceAdapter(Dimension.PRESSURE) {
            double[] value = new double[1];
            public double[] getData() {
                value[0] = pressureSlider.getModifier().getValue();
                return value;
            }
        };
        AccumulatorHistory targetPressureHistory = new AccumulatorHistory();
        targetPressureHistory.setHistoryLength(historyLength);
        DataManager targetPressureDataManager = new DataManager(targetPressureDataSource, targetPressureHistory);
        DataSink targetPressureDataSink = plotP.makeDataSink();
        DataManager targetPressurePlotManager = new DataManager(targetPressureHistory, targetPressureDataSink);
        targetPressureDataManager.setUpdateInterval(10);
        targetPressurePlotManager.setUpdateInterval(10);
        
        DataSource targetTemperatureDataSource = new DataSourceAdapter(Dimension.TEMPERATURE) {
            double[] value = new double[1];
            public double[] getData() {
                value[0] = temperatureSlider.getModifier().getValue();
                return value;
            }
        };
        AccumulatorHistory targetTemperatureHistory = new AccumulatorHistory();
        targetTemperatureHistory.setHistoryLength(historyLength);
        DataManager targetTemperatureDataManager = new DataManager(targetTemperatureDataSource, targetTemperatureHistory);
        DataSink targetTemperatureDataSink = plotT.makeDataSink();
        DataManager targetTemperaturePlotManager = new DataManager(targetTemperatureHistory, targetTemperatureDataSink);
        targetTemperatureDataManager.setUpdateInterval(10);
        targetTemperaturePlotManager.setUpdateInterval(10);

        densityMeter = new MeterPistonDensity(pc.pistonPotential,1,Default.ATOM_SIZE);
        AccumulatorAverage densityAvg = new AccumulatorAverage();
        AccumulatorHistory densityHistory = new AccumulatorHistory();
        densityHistory.setHistoryLength(historyLength);
//        densityHistory.setLabel("Density ("+dadUnit.symbol()+")");
        
        densityDisplayBox.setAccumulator(densityAvg);

        AccumulatorAverage temperatureAvg = new AccumulatorAverage();
        temperatureDisplayBox.setAccumulator(temperatureAvg);
        temperatureManager.addDataSink(temperatureAvg);
        
        pressureMeter = new DataSourceWallPressure(pc.pistonPotential,pc.integrator);
        pressureMeter.setPhase(new Phase[]{pc.phase});
        AccumulatorAverage pressureAvg = new AccumulatorAverage();
        pressureDisplayBox.setAccumulator(pressureAvg);
        pressureManager = new DataManager(pressureMeter, new DataSink[]{pressureAvg});
        pressureManager.setUpdateInterval(10);
        densityManager = new DataManager(densityMeter,new DataSink[]{densityAvg, densityHistory});
        DataManager densityPlotManager = new DataManager(densityHistory, plotD.makeDataSink());
        densityManager.setUpdateInterval(10);
        densityPlotManager.setUpdateInterval(10);
        
        Unit dUnit, dadUnit;
        if (D == 3) {
            dUnit = new UnitRatio(Mole.UNIT, Liter.UNIT);
            dadUnit = new UnitRatio(new PrefixedUnit(Prefix.DECI, Mole.UNIT), 
                                    new PrefixedUnit(Liter.UNIT));
        }
        else {
            dUnit = new UnitRatio(Mole.UNIT, 
                    new BaseUnitPseudo3D.Volume(Liter.UNIT));
            dadUnit = new UnitRatio(new PrefixedUnit(Prefix.DECI, Mole.UNIT), 
                                    new PrefixedUnit(new BaseUnitPseudo3D.Volume(Liter.UNIT)));
        }
        densityMeter.setPhase(new Phase[] {pc.phase});
        
        pc.integrator.addIntervalListener(tBox);
        scaleSlider.setController(pc.controller);
        pc.integrator.addIntervalListener(densityManager);
        pc.integrator.addIntervalListener(densityPlotManager);
        pc.integrator.addIntervalListener(temperatureManager);
        pc.integrator.addIntervalListener(pressureManager);
        pc.integrator.addIntervalListener(temperaturePlotManager);
        pc.integrator.addIntervalListener(targetTemperatureDataManager);
        pc.integrator.addIntervalListener(targetTemperaturePlotManager);
        pc.integrator.addIntervalListener(targetPressureDataManager);
        pc.integrator.addIntervalListener(targetPressurePlotManager);
        pc.integrator.addIntervalListener(densityDisplayBox);
        pc.integrator.addIntervalListener(temperatureDisplayBox);
        pc.integrator.addIntervalListener(pressureDisplayBox);
        densityDisplayBox.setUpdateInterval(10);
        densityDisplayBox.setUnit(dUnit);
        temperatureDisplayBox.setUpdateInterval(10);
        temperatureDisplayBox.setUnit(tUnit);
        
        Unit pUnit;
        if (D == 3) {
            pUnit = Bar.UNIT;
        }
        else {
            pUnit = new BaseUnitPseudo3D.Pressure(Bar.UNIT);
        }

        pressureDisplayBox.setUpdateInterval(10);
        pressureDisplayBox.setUnit(pUnit);        

        plotP.getPlot().setTitle("Pressure ("+pUnit.symbol()+")");
        plotT.getPlot().setTitle("Temperature ("+tUnit.symbol()+")");
        plotD.getPlot().setTitle("Density ("+dUnit.symbol()+")");
        
        plotT.getPlot().setXRange(0, historyLength);
        plotP.getPlot().setXRange(0, historyLength);
        plotD.getPlot().setXRange(0, historyLength);
        plotT.getPlot().setYRange(0, 1000.);
        plotP.getPlot().setYRange(0, 1000.);
        DataSourceUniform xSource = new DataSourceUniform(historyLength, 1, historyLength);
        plotT.setXSource(xSource);
        plotP.setXSource(xSource);
        plotD.setXSource(xSource);

        pressureSlider.setUnit(pUnit);
        pressureSliderPanel.setBorder(new javax.swing.border.TitledBorder("Set pressure ("+pUnit.toString()+")"));
        Dimension pDim = (D==2) ? Dimension.PRESSURE2D : Dimension.PRESSURE;
        pc.pistonPotential.setPressure(pUnit.toSim(pressureSlider.getValue()));
        pressureSlider.setModifier(new ModifierPistonPressure(pc.pistonPotential,pDim));
        pressureSlider.setPostAction(new ActionPistonUpdate(pc.integrator));
        pressureSlider.setController(pc.getController());

        ModifierBoolean fixPistonModulator = new ModifierBoolean() {
            public void setBoolean(boolean b) {
                pc.pistonPotential.setStationary(b);
            }
            public boolean getBoolean() {
                return pc.pistonPotential.isStationary();
            }
        };
        fixPistonButton.setController(pc.controller);
        fixPistonButton.setModifier(fixPistonModulator, "Release piston", "Hold piston");
        fixPistonButton.setPostAction(new ActionPistonUpdate(pc.integrator));
        fixPistonButton.setState(pistonHeld);
    }
    
    public void setPotential(String potentialDesc) {
        final boolean HS = potentialDesc.equals("Repulsion only"); 
        final boolean SW = potentialDesc.equals("Repulsion and attraction"); 
        pc.controller.doActionNow( new Action() {
            public void actionPerformed() {
                if (HS) {
                    pc.potentialWrapper.setPotential(potentialHS);
                }
                else if (SW) {
                    pc.potentialWrapper.setPotential(potentialSW);
                }
                else {
                    pc.potentialWrapper.setPotential(null);
                }
                pc.integrator.reset();
            }
            public String getLabel() {return "";}
        });
    }
    
    private class ModifierAtomDiameter implements Modifier {

        public void setValue(double d) {
            Default.ATOM_SIZE = d;
            //assume one type of atom
            ((AtomTypeSphere)pc.phase.firstAtom().type).setDiameter(d);
            PistonCylinderGraphic.this.densityMeter.setAtomDiameter(d);
            PistonCylinderGraphic.this.potentialHS.setCollisionDiameter(d);
            PistonCylinderGraphic.this.potentialSW.setCoreDiameter(d);
            pc.pistonPotential.setCollisionRadius(0.5*d);
            pc.wallPotential.setCollisionRadius(0.5*d);
            displayPhase.repaint();
        }

        public double getValue() {
            return Default.ATOM_SIZE;
        }

        public Dimension getDimension() {
            return Dimension.LENGTH;
        }
        
        public String getLabel() {
            return "Atom Diameter";
        }
        
        public String toString() {
            return getLabel();
        }
    }
    
    public static void main(String[] args) {
        PistonCylinderGraphic sim = new PistonCylinderGraphic();
		SimulationGraphic.makeAndDisplayFrame(sim.panel);
//		sim.phase.reset();
 //       sim.controller1.start();
    }//end of main
    
    public static class Applet extends javax.swing.JApplet {

	    public void init() {
		    getContentPane().add(new PistonCylinderGraphic().panel);
	    }
    }//end of Applet
}//end of PistonCylinderGraphic class


