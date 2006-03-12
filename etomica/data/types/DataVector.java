package etomica.data.types;

import java.io.Serializable;

import etomica.data.Data;
import etomica.data.DataFactory;
import etomica.data.DataInfo;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.units.Dimension;
import etomica.util.Function;

/**
 * Data object wrapping a single mutable value of type (Space) Vector. Value is
 * final public and can be accessed directly. 
 * <p>
 * All arithmetic methods throw ClassCastException if given a Data instance that
 * is not of this type.
 * 
 * @author David Kofke
 *  
 */

/*
 * History Created on Jun 15, 2005 by kofke
 */
public class DataVector extends Data implements DataArithmetic {

    /**
     * Constructs a new instance with the given DataInfo, wrapping a new Vector
     * instance from the given space.
     * 
     * @param space
     *            used to construct the wrapped Vector
     * @param label
     *            a descriptive label for this data
     * @param dimension
     *            the physical dimensions of the data
     */
    public DataVector(Space space, String label, Dimension dimension) {
        super(new DataInfo(label, dimension, getFactory(space)));
        x = space.makeVector();
    }

    /**
     * Copy constructor.
     */
    public DataVector(DataVector data) {
        super(data);
        x = (Vector) data.x.clone();
    }

    /**
     * Returns a deep copy of this instance. Returned object has its own instances of
     * all fields, set equal to the values of this instance's fields.  This instance
     * and the copy share the same DataInfo, which is immutable.
     */
    public Data makeCopy() {
        return new DataVector(this);
    }

    /**
     * Copies the elements of the given vector (wrapped in the Data object)
     * to this vector.
     */
    public void E(Data y) {
        x.E(((DataVector) y).x);
    }

    /**
     * Sets all vector elements to the given value.
     */
    public void E(double y) {
        x.E(y);
    }

    /**
     * Minus-equals (-=) operation.  Performed element-by-element.
     */
    public void ME(DataArithmetic y) {
        x.ME(((DataVector) y).x);
    }

    /**
     * Plus-equals (+=) operation. Performed element-by-element.
     */
    public void PE(DataArithmetic y) {
        x.PE(((DataVector) y).x);
    }

    /**
     * Times-equals (*=) operation. Performed element-by-element.
     */
    public void TE(DataArithmetic y) {
        x.TE(((DataVector) y).x);
    }

    /**
     * Divide-equals (/=) operation. Performed element-by-element.
     */
    public void DE(DataArithmetic y) {
        x.DE(((DataVector) y).x);
    }

    /**
     * Plus-equals (+=) operation.  Adds given value to all elements.
     */
    public void PE(double y) {
        x.PE(y);
    }

    /**
     * Times-equals (*=) operation. Multiplies all elements by the given value.
     */
    public void TE(double y) {
        x.TE(y);
    }

    /**
     * Returns true if any vector element is not-a-number, as given by Double.isNaN.
     */
    public boolean isNaN() {
        return x.isNaN();
    }

    /**
     * Maps the function on all vector elements, replacing each with the
     * value given by the function applied to it.
     */
    public void map(Function function) {
        x.map(function);
    }

    /**
     * Returns the number of elements in the wrapped vector.
     */
    public int getLength() {
        return x.D();
    }

    /**
     * Returns the i-th vector value.
     */
    public double getValue(int i) {
        if(i < 0 || i>= x.D()) throw new IllegalArgumentException("Illegal value: " + i);
        return x.x(i);
    }

    /**
     * Assigns the elements of the wrapped vector to the given array.
     */
    public void assignTo(double[] array) {
        x.assignTo(array);
    }

    /**
     * Returns a string formed from the dataInfo label and the vector values.
     */
    public String toString() {
        return dataInfo.getLabel() + " " + x.toString();
    }
    
    /**
     * Returns a DataFactory that makes a DataVector for the given Space.
     */
    public static DataFactory getFactory(Space space) {
        if(FACTORY == null || FACTORY.space != space) { 
            FACTORY = new Factory(space);
        }
        return FACTORY;
    }

    /**
     * The wrapped vector data.
     */
    public final Vector x;
    
    private transient static Factory FACTORY = null;
    
    /**
     * DataFactory that makes DataVector instances of a specific length.
     */
    public static class Factory implements DataFactory, Serializable {
        
        protected final Space space;
        
        Factory(Space space) {
            this.space = space;
        }
        
        /**
         * Makes a new DataVector with DataInfo having the given label and dimension.
         */
        public Data makeData(String label, Dimension dimension) {
            return new DataVector(space, label, dimension);
        }
        
        /**
         * Returns DataVector.class.
         */
        public Class getDataClass() {
            return DataVector.class;
        }
        
        /**
         * Returns the Space used to make the Vectors.
         */
        public Space getSpace() {
            return space;
        }
    }//end of Factory
    
}
