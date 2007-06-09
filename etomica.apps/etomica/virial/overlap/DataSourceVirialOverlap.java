package etomica.virial.overlap;

import etomica.data.AccumulatorRatioAverage;
import etomica.data.DataSourceScalar;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.units.Fraction;

/**
 * Measures ratio of two cluster integrals using overlap sampling.  The resulting ratio is 
 * formed from the ratio of a target and reference ratio from two different sub-simulations. 
 */
public class DataSourceVirialOverlap extends DataSourceScalar {

    private static final long serialVersionUID = 1L;
    private AccumulatorVirialOverlapSingleAverage refAccumulator, targetAccumulator;
	private final int nBennetPoints;
	
	public DataSourceVirialOverlap(AccumulatorVirialOverlapSingleAverage aRefAccumulator, 
			AccumulatorVirialOverlapSingleAverage aTargetAccumulator) {
        super("Virial Overlap Ratio",Fraction.DIMENSION);
		refAccumulator = aRefAccumulator;
		targetAccumulator = aTargetAccumulator;
		nBennetPoints = aRefAccumulator.getNBennetPoints();
	}
    
    public AccumulatorVirialOverlapSingleAverage[] getAccumulators() {
        return new AccumulatorVirialOverlapSingleAverage[]{refAccumulator, targetAccumulator};
    }

    /**
     * Returns the ratio of the reference to target overlap-to-virial ratios
     * (which reduces to target/reference) for the optimal value of the Bennet
     * parameter.
     */
	public double getDataAsScalar() {
		return getAverage(minDiffLocation());
	}
	
    public int getDataLength() {
        return 1;
    }
    
    /**
     * Returns the ratio of the reference to target overlap-to-virial ratios
     * (which reduces to target/reference) for the given value of the Bennet
     * parameter.
     */
	public double getAverage(int iParam) {
        double targetAvg = ((DataDoubleArray)((DataGroup)targetAccumulator.getData(iParam)).getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1];
        double refAvg = ((DataDoubleArray)((DataGroup)refAccumulator.getData(iParam)).getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1];
        return refAvg/targetAvg;
	}
	
	/**
     * Returns the index of the Bennet parameter which minimizes the differences 
     * between the Bennet "sums" from the target and references accumulators.  This
     * parameter should be optimal for overlap sampling.
	 */
    public int minDiffLocation() {
		int minDiffLoc = 0;
		double minDiff = Math.abs(refAccumulator.getBennetAverage(0)
                /(targetAccumulator.getBennetAverage(0)*refAccumulator.getBennetBias(0))-1);
		for (int i=1; i<nBennetPoints; i++) {
            double ratio = refAccumulator.getBennetAverage(i)/targetAccumulator.getBennetAverage(i);
            double bias = refAccumulator.getBennetBias(i);
            double newDiff = ratio/bias + bias/ratio - 2;
			if (Math.abs(newDiff) < minDiff) {
				minDiffLoc = i;
				minDiff = Math.abs(newDiff);
			}
		}
		return minDiffLoc;
	}
	
    /**
     * Returns the error in the ratio of the reference to target 
     * overlap-to-virial ratios (which reduces to target/reference) 
     * for the optimal value of the Bennet parameter.
     */
	public double getError() {
		return getError(minDiffLocation());
	}

    /**
     * Returns the error in the ratio of the reference to target 
     * overlap-to-virial ratios (which reduces to target/reference) 
     * for the given value of the Bennet parameter.
     */
	public double getError(int iParam) {
		double avg = getAverage(iParam);
        DataGroup dataGroup = (DataGroup)refAccumulator.getData(iParam);
		double refErr = ((DataDoubleArray)dataGroup.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1];
        double refAvg = ((DataDoubleArray)dataGroup.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1];
        double refRelErr = refErr/refAvg;
        dataGroup = (DataGroup)targetAccumulator.getData(iParam);
		double targetErr = ((DataDoubleArray)dataGroup.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1];
		double targetAvg = ((DataDoubleArray)dataGroup.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1];
        double targetRelErr = targetErr/targetAvg;
		return Math.abs(avg)*Math.sqrt(refRelErr*refRelErr+targetRelErr*targetRelErr);
	}

    /**
     * Convenience method that resets reference and target accumulators
     */
    public void reset() {
        targetAccumulator.reset();
        refAccumulator.reset();
    }
}