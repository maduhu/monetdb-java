/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 MonetDB B.V.
 */

package nl.cwi.monetdb.embedded.resultset;

import nl.cwi.monetdb.embedded.env.MonetDBEmbeddedException;

/**
 * A MonetDB column converted to an array of Java double values.
 *
 * @author <a href="mailto:pedro.ferreira@monetdbsolutions.com">Pedro Ferreira</a>
 */
public class QueryResultSetDoubleColumn extends AbstractQueryResultSetColumn<double[]> {

    /**
     * MonetDB's double null constant.
     */
    private static double DoubleNullConstant;

    /**
     * Gets MonetDB's double null constant
     *
     * @return MonetDB's double null constant
     */
    public static double GetDoubleNullConstant() { return DoubleNullConstant; }

    /**
     * Array with the retrieved values.
     */
    private final double[] values;

    protected QueryResultSetDoubleColumn(String columnType, long tablePointer, int resultSetIndex, String columnName,
                                         int columnDigits, int columnScale, int numberOfRows) {
        super(columnType, tablePointer, resultSetIndex, columnName, columnDigits, columnScale, numberOfRows);
        if(!this.getMapping().getJavaClass().equals(Double.class)) {
            throw new ClassCastException("The parameter must be of boolean type!!");
        }
        this.values = new double[numberOfRows];
    }

    @Override
    protected void fetchMoreData(int startIndex, int endIndex) throws MonetDBEmbeddedException {
        double[] values = this.fetchValuesInternal(this.tablePointer, this.resultSetIndex, startIndex, endIndex);
        System.arraycopy(values, 0, this.values, startIndex, values.length);
    }

    @Override
    protected double[] storeNewDataAndGetResult(int startIndex, int numberOfRowsToRetrieve) {
        double[] result = new double[numberOfRowsToRetrieve];
        System.arraycopy(this.values, startIndex, result, 0, numberOfRowsToRetrieve);
        return result;
    }

    @Override
    protected boolean[] checkIfIndexesAreNullImplementation(double[] values, boolean[] res) throws MonetDBEmbeddedException {
        for(int i = 0 ; i < values.length ; i++) {
            res[i] = (values[i] == DoubleNullConstant);
        }
        return res;
    }

    @Override
    protected Double[] mapValuesToObjectArrayImplementation(double[] values) throws MonetDBEmbeddedException {
        Double[] res = new Double[values.length];
        for(int i = 0 ; i < values.length ; i++) {
            res[i] = (values[i] == DoubleNullConstant) ? null : values[i];
        }
        return res;
    }

    /**
     * Internal implementation to fetch values from the column.
     */
    private native double[] fetchValuesInternal(long tablePointer, int resultSetIndex, int startIndex, int endIndex)
            throws MonetDBEmbeddedException;
}