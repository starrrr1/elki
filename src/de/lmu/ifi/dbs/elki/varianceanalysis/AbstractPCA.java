package de.lmu.ifi.dbs.elki.varianceanalysis;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract super class for pca algorithms. Provides the eigenvalue and eigenvectors.
 *
 * @author Elke Achtert
 */
public abstract class AbstractPCA extends AbstractParameterizable implements PCA {

    /**
     * Parameter to specify the filter for determination of the strong and weak eigenvectors,
     * must be a subclass of {@link EigenPairFilter}.
     * <p>Default value: {@link PercentageEigenPairFilter}</p>
     * <p>Key: {@code -pca.filter} </p>
     */
    private ClassParameter<EigenPairFilter> EIGENPAIR_FILTER_PARAM =
        new ClassParameter<EigenPairFilter>(
            OptionID.PCA_EIGENPAIR_FILTER,
            EigenPairFilter.class,
            PercentageEigenPairFilter.class.getName());

    /**
     * The eigenpair filter to determine the strong and weak eigenvectors.
     */
    private EigenPairFilter eigenPairFilter;

    /**
     * The eigenvalues in decreasing order.
     */
    private double[] eigenvalues;

    /**
     * The eigenvectors in decreasing order to their corresponding eigenvalues.
     */
    private Matrix eigenvectors;

    /**
     * The eigenpaiss in decreasing order.
     */
    private SortedEigenPairs eigenPairs;

    /**
     * The strong eigenvalues.
     */
    private double[] strongEigenvalues;

    /**
     * The strong eigenvectors to their corresponding filtered eigenvalues.
     */
    private Matrix strongEigenvectors;

    /**
     * The weak eigenvalues.
     */
    private double[] weakEigenvalues;

    /**
     * The amount of Variance explained.
     */
    private double explainedVariance;

    /**
     * The weak eigenvectors to their corresponding filtered eigenvalues.
     */
    private Matrix weakEigenvectors;

    /**
     * Provides an abstract super class for pca algorithms.
     */
    protected AbstractPCA() {
        super();
        optionHandler.put(EIGENPAIR_FILTER_PARAM);
    }

    /**
     * Returns a copy of the matrix of eigenvectors
     * of the object to which this PCA belongs to.
     *
     * @return the matrix of eigenvectors
     */
    public final Matrix getEigenvectors() {
        return eigenvectors.copy();
    }

    /**
     * Returns a copy of the eigenvalues of the object to which this PCA belongs to
     * in decreasing order.
     *
     * @return the eigenvalues
     */
    public final double[] getEigenvalues() {
        return Util.copy(eigenvalues);
    }

    /**
     * Returns a copy of the eigenpairs of the object to which this PCA belongs to
     * in decreasing order.
     *
     * @return the eigenpairs
     */
    public final SortedEigenPairs getEigenPairs() {
        // FIXME: return a copy.
        return eigenPairs;
    }

    /**
     * Returns a copy of the matrix of strong eigenvectors
     * after passing the eigen pair filter.
     *
     * @return the matrix of eigenvectors
     */
    public final Matrix getStrongEigenvectors() {
        return strongEigenvectors.copy();
    }

    /**
     * Returns a copy of the strong eigenvalues of the object
     * after passing the eigen pair filter.
     *
     * @return the eigenvalues
     */
    public final double[] getStrongEigenvalues() {
        return Util.copy(strongEigenvalues);
    }

    /**
     * Returns a copy of the matrix of weak eigenvectors
     * after passing the eigen pair filter.
     *
     * @return the matrix of eigenvectors
     */
    public final Matrix getWeakEigenvectors() {
        return weakEigenvectors.copy();
    }

    /**
     * Returns a copy of the weak eigenvalues of the object
     * after passing the eigen pair filter.
     *
     * @return the eigenvalues
     */
    public final double[] getWeakEigenvalues() {
        return Util.copy(weakEigenvalues);
    }

    /**
     * Returns explained variance
     *
     * @return the variance explained by the strong Eigenvectors
     */
    public double getExplainedVariance() {
        return explainedVariance;
    }

    /**
     * Determines the (strong and weak) eigenpairs (i.e. the eigenvectors and their
     * corresponding eigenvalues) sorted in descending order of their eigenvalues of
     * the specified matrix.
     *
     * @param pcaMatrix the matrix used for performing pca
     */
    protected void determineEigenPairs(Matrix pcaMatrix) {
        // eigen value decomposition
        EigenvalueDecomposition evd = pcaMatrix.eig();
        eigenPairs = new SortedEigenPairs(evd, false);
        eigenvectors = eigenPairs.eigenVectors();
        eigenvalues = eigenPairs.eigenValues();

        if (this.debug) {
            StringBuffer msg = new StringBuffer();
            msg.append("\ncov ").append(pcaMatrix);
            msg.append("\neigenpairs: ").append(Arrays.asList(eigenPairs));
            msg.append("\neigenvalues: ").append(Util.format(eigenvalues));
            msg.append("\neigenvectors: ").append(eigenvectors);
            debugFine(msg.toString());
        }

        // filter
        FilteredEigenPairs filteredEigenPairs = eigenPairFilter.filter(eigenPairs);
        if (this.debug) {
            StringBuffer msg = new StringBuffer();
            msg.append("\nfilteredEigenPairs: ").append(filteredEigenPairs);
            debugFine(msg.toString());
        }

        double sumStrongEigenvalues = 0;
        double sumWeakEigenvalues = 0;
        {// strong eigenpairs
            List<EigenPair> strongEigenPairs = filteredEigenPairs.getStrongEigenPairs();
            strongEigenvalues = new double[strongEigenPairs.size()];
            strongEigenvectors = new Matrix(eigenvectors.getRowDimensionality(), strongEigenPairs.size());
            int i = 0;
            for (Iterator<EigenPair> it = strongEigenPairs.iterator(); it.hasNext(); i++) {
                EigenPair eigenPair = it.next();
                strongEigenvalues[i] = eigenPair.getEigenvalue();
                strongEigenvectors.setColumn(i, eigenPair.getEigenvector());
                sumStrongEigenvalues += strongEigenvalues[i];
            }
        }

        {// weak eigenpairs
            List<EigenPair> weakEigenPairs = filteredEigenPairs.getWeakEigenPairs();
            weakEigenvalues = new double[weakEigenPairs.size()];
            weakEigenvectors = new Matrix(eigenvectors.getRowDimensionality(), weakEigenPairs.size());
            int i = 0;
            for (Iterator<EigenPair> it = weakEigenPairs.iterator(); it.hasNext(); i++) {
                EigenPair eigenPair = it.next();
                weakEigenvalues[i] = eigenPair.getEigenvalue();
                weakEigenvectors.setColumn(i, eigenPair.getEigenvector());
                sumWeakEigenvalues += weakEigenvalues[i];
            }
        }
        explainedVariance = sumStrongEigenvalues / (sumStrongEigenvalues + sumWeakEigenvalues);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // eigenpair filter
        eigenPairFilter = EIGENPAIR_FILTER_PARAM.instantiateClass();
        remainingParameters = eigenPairFilter.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(eigenPairFilter.getAttributeSettings());
        return attributeSettings;
    }
}