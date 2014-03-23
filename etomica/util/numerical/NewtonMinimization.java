package etomica.util.numerical;

import Jama.Matrix;
import etomica.util.FunctionMultiDimensionalDifferentiable;

public class NewtonMinimization {

    public NewtonMinimization(FunctionMultiDimensionalDifferentiable f) {
        this.f = f;
    }
    
    public double[] minimize(double[] xGuess, double tol, int maxIter) {
        final int n = xGuess.length;
        double[] x = xGuess.clone();
        int[] d = new int[n];
        double lastVal = Double.MAX_VALUE;
        Matrix F = new Matrix(n, 1);
        Matrix J = new Matrix(n, n);
        double[][] Farray = F.getArray();
        double[][] Jarray = J.getArray();

        double stepSize = 0;
        double val = f.f(x);
        SteepestDescent sd = new SteepestDescent(f);
        for (int iter=0; iter<maxIter; iter++) {
            if (iter>0) System.out.println(String.format("%4d    %10.4e   %10.4e   %10.4e", iter, val, val-lastVal, stepSize));
            else System.out.println(String.format("%4d    %10.4e                %10.4e", iter, val, stepSize));
            if (lastVal - val < tol) return x;

            for (int i=0; i<n; i++) {
                d[i] = 1;
                Farray[i][0] = f.df(d, x);
                for (int j=0; j<n; j++) {
                    d[j]++;
                    Jarray[i][j] = f.df(d, x);
                    d[j]--;
                }
                d[i] = 0;
            }
            lastVal = val;
            Matrix dx = J.solve(F);
            stepSize = 0;
            double totalD = 0;
//                dx = new Matrix(x.length,1);
//                dx.set(0,0,-1e-8);
            for (int i=0; i<n; i++) {
                stepSize += dx.get(i,0)*dx.get(i,0);
                totalD += -dx.get(i,0)*Farray[i][0];
                x[i] += -dx.get(i, 0);
            }
            if (totalD < 0) {
                val = f.f(x);
//                System.out.println("    "+(val-lastVal)+" "+totalD);
//                System.exit(1);
                if (val < lastVal) {
                    if (iter%10==0 && false) {
                        double lv = val;
                        System.out.println("(steepest descent for fun)");
                        double[] xStep = new double[x.length];
                        for (int i=0; i<x.length; i++) {
                            xStep[i] = Math.abs(dx.get(i,0))*0.01;
                        }
                        double[] newX = sd.minimize(x, xStep, 1e-10, 40);
                        val = f.f(newX);
                        if (val < lv) {
                            x = newX;
                        }
                        else {
                            val = lv;
                        }
                    }
                    continue;
                }
                for (int i=0; i<n; i++) {
                    x[i] -= -dx.get(i, 0);
                }

                dx = dx.times(0.1);
                for (int i=0; i<n; i++) {
                    stepSize += dx.get(i,0)*dx.get(i,0);
                    totalD += -dx.get(i,0)*Farray[i][0];
                    x[i] += -dx.get(i, 0);
                }
                val = f.f(x);
                if (val < lastVal) continue;
            }
            else {
                for (int i=0; i<n; i++) {
                    x[i] -= -dx.get(i, 0);
                }
            }
            
            // Newton fails, try steepest descent first
            System.out.println(String.format("(steepest descent, totalD=%10.4e|%10.4e)", totalD, (val-lastVal)));
            double[] xStep = new double[x.length];
            for (int i=0; i<x.length; i++) {
                xStep[i] = Math.abs(dx.get(i,0))*0.01;
            }
            double[] newX = sd.minimize(x, xStep, 1e-10, 40);
            val = f.f(newX);
            if (val > lastVal) return x;
            x = newX;
            
        }
        
        return x;
    }

    protected FunctionMultiDimensionalDifferentiable f;
}