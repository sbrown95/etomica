package etomica.action;

import etomica.atom.AtomSet;
import etomica.atom.IAtom;
import etomica.atom.IAtomKinetic;
import etomica.data.meter.MeterTemperature;
import etomica.box.Box;

/**
 * Scales all velocities of a box so that its kinetic temperature is equal to
 * a given value.
 */
public class BoxQuench extends BoxActionAdapter {

    public BoxQuench(int dim) {
        this.dim = dim;
    }
    
    /**
     * Constructs class without specifying box and using Default temperature.
     * Requires call to setBox before action will have any effect.
     */
    public BoxQuench(double temperature, int dim) {
        this(dim);
        setTemperature(temperature);
	}
	
	/**
	 * Constructs class ready to perform quench on given box to given temperature.
	 */
	public BoxQuench(Box p, double temperature) {
		this(temperature, p.getBoundary().getDimensions().getD());
        setBox(p);
	}

    public void setBox(Box p) {
        super.setBox(p);
        meterTemperature = new MeterTemperature(box, dim);
    }
    
	/**
	 * @see etomica.action.BoxActionAdapter#actionPerformed(IAtom)
	 */
	public void actionPerformed() {
		if(box == null) return;
		double currentTemperature = meterTemperature.getDataAsScalar();
		double scale = Math.sqrt(temperature / currentTemperature);
        AtomSet leafList = box.getLeafList();
        int nLeaf = leafList.getAtomCount();
        for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
			((IAtomKinetic)leafList.getAtom(iLeaf)).getVelocity().TE(scale);
        }
	}

	/**
	 * Returns the quench temperature.
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * Sets the quench temperature.
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

    private static final long serialVersionUID = 1L;
	protected double temperature;
	protected MeterTemperature meterTemperature;
	protected final int dim;
}
