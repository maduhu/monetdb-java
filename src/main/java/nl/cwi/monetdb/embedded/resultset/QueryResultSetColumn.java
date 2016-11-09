/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 2016 MonetDB B.V.
 */

package nl.cwi.monetdb.embedded.resultset;

import nl.cwi.monetdb.embedded.mapping.AbstractColumn;
import nl.cwi.monetdb.embedded.env.MonetDBEmbeddedException;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.ListIterator;

/**
 * An abstract class for accessing materialised (Java-level) query result columns.
 *
 * @param <T> A Java class mapped to a MonetDB data type
 * @author <a href="mailto:pedro.ferreira@monetdbsolutions.com">Pedro Ferreira</a>
 */
public class QueryResultSetColumn<T> extends AbstractColumn<T> implements Iterable<T> {

    /**
     * The C pointer of the result set of the column.
     */
    protected final long resultSetPointer;

    /**
     * Array with the retrieved values.
     */
    private final T[] values;

    /**
     * The number of rows in this column.
     */
    protected final int numberOfRows;

    /**
     * The index of the first value mapped to a Java class.
     */
    private int firstRetrievedIndex;

    /**
     * The index of the last value mapped to a Java class.
     */
    private int lastRetrievedIndex;

    @SuppressWarnings("unchecked")
	protected QueryResultSetColumn(int resultSetIndex, String columnName, String columnType, int columnDigits,
                                   int columnScale, long resultSetPointer, int numberOfRows) {
        super(resultSetIndex, columnName, columnType, columnDigits, columnScale);
        this.resultSetPointer = resultSetPointer;
        this.numberOfRows = numberOfRows;
        this.firstRetrievedIndex = numberOfRows;
        this.lastRetrievedIndex = 0;
        this.values = (T[]) Array.newInstance(this.mapping.getJavaClass(), numberOfRows);
 	}

    /**
     * Get the number of rows in this column.
     *
     * @return The number of rows
     */
    public int getNumberOfRows() { return numberOfRows; }

    /**
     * Maps columns values into the Java representation.
     *
     * @param startIndex The first column index to retrieve
     * @param endIndex The last column index to retrieve
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    @SuppressWarnings("unchecked")
    public T[] fetchColumnValues(int startIndex, int endIndex, Class<T> javaClass) throws MonetDBEmbeddedException {
        if(endIndex < startIndex) {
            int aux = startIndex;
            startIndex = endIndex;
            endIndex = aux;
        }
        if (startIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("The start index must be larger than 0!");
        } else if (endIndex > this.numberOfRows) {
            throw new ArrayIndexOutOfBoundsException("The index must be smaller than the number of elements in the columns!");
        } else if(startIndex == endIndex) {
            throw new ArrayIndexOutOfBoundsException("Retrieving 0 values?");
        }

        boolean hasToConvert = false;
        int numberOfRowsToRetrieve = endIndex - startIndex;
        int firstIndexToFetch = Math.min(startIndex, this.firstRetrievedIndex);
        int lastIndexToFetch = Math.max(endIndex, this.lastRetrievedIndex);
        if(startIndex < this.firstRetrievedIndex) {
            this.firstRetrievedIndex = startIndex;
            hasToConvert = true;
        }
        if(endIndex > this.lastRetrievedIndex) {
            this.lastRetrievedIndex = endIndex;
            hasToConvert = true;
        }
        if(hasToConvert) {
            if(this.resultSetPointer == 0) {
                throw new MonetDBEmbeddedException("Connection closed!");
            }
            T[] newvalues = this.fetchValuesInternal(this.resultSetPointer, this.resultSetIndex,
                    (Class<T>) this.mapping.getJavaClass(), this.mapping.ordinal(), firstIndexToFetch, lastIndexToFetch);
            System.arraycopy(newvalues, 0, this.values, firstIndexToFetch, newvalues.length);
        }

        T[] result = (T[]) Array.newInstance(javaClass, numberOfRowsToRetrieve);
        System.arraycopy(this.values, startIndex, result, 0, numberOfRowsToRetrieve);
        return result;
    }

    /**
     * Maps columns values into the Java representation asynchronously.
     *
     * @param startIndex The first column index to retrieve
     * @param endIndex The last column index to retrieve
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*public T[] fetchColumnValuesAsync(int startIndex, int endIndex, Class<T> javaClass) throws MonetDBEmbeddedException {
        CompletableFuture.supplyAsync(() -> this.fetchColumnValues(startIndex, endIndex, javaClass));
        throw new UnsupportedOperationException("Must wait for Java 8 :(");
    }*/

    /**
     * Maps the first N column values.
     *
     * @param n The last column index to map
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    public T[] fetchFirstNColumnValues(int n, Class<T> javaClass) throws MonetDBEmbeddedException {
        return this.fetchColumnValues(0, n, javaClass);
    }

    /**
     * Maps the first N column values asynchronously.
     *
     * @param n The last column index to map
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*public T[] fetchFirstNColumnValuesAsync(int n, Class<T> javaClass) throws MonetDBEmbeddedException {
        return this.fetchColumnValuesAsync(0, n, javaClass);
    }*/

    /**
     * Maps all column values.
     *
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    public T[] fetchAllColumnValues(Class<T> javaClass) throws MonetDBEmbeddedException {
        return this.fetchColumnValues(0, this.numberOfRows, javaClass);
    }

    /**
     * Maps all column values asynchronously.
     *
     * @param javaClass The Java class to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*public T[] fetchAllColumnValuesAsync(Class<T> javaClass) throws MonetDBEmbeddedException {
        return this.fetchColumnValuesAsync(0, this.numberOfRows, javaClass);
    }*/

    /**
     * Maps columns values using the provided Java representation by the query.
     *
     * @param startIndex The first column index to retrieve
     * @param endIndex The last column index to retrieve
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    @SuppressWarnings("unchecked")
    public T[] fetchColumnValues(int startIndex, int endIndex) throws MonetDBEmbeddedException {
        return this.fetchColumnValues(startIndex, endIndex, (Class<T>) this.mapping.getJavaClass());
    }

    /**
     * Maps columns values using the provided Java representation by the query asynchronously.
     *
     * @param startIndex The first column index to retrieve
     * @param endIndex The last column index to retrieve
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*@SuppressWarnings("unchecked")
    public T[] fetchColumnValuesAsync(int startIndex, int endIndex) throws MonetDBEmbeddedException {
        return this.fetchColumnValuesAsync(startIndex, endIndex, (Class<T>) this.mapping.getJavaClass());
    }*/

    /**
     * Maps the first N column values using the provided Java representation by the query.
     *
     * @param n The last column index to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    public T[] fetchFirstNColumnValues(int n) throws MonetDBEmbeddedException {
        return this.fetchColumnValues(0, n);
    }

    /**
     * Maps the first N column values using the provided Java representation by the query asynchronously.
     *
     * @param n The last column index to map
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*public T[] fetchFirstNColumnValuesAsync(int n) throws MonetDBEmbeddedException {
        return this.fetchColumnValuesAsync(0, n);
    }*/

    /**
     * Maps all column values using the provided Java representation by the query.
     *
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    public T[] fetchAllColumnValues() throws MonetDBEmbeddedException {
        return this.fetchColumnValues(0, this.numberOfRows);
    }

    /**
     * Maps all column values using the provided Java representation by the query asynchronously.
     *
     * @return The column values as a Java array
     * @throws MonetDBEmbeddedException If an error in the database occurred
     */
    /*public T[] fetchAllColumnValuesAsync() throws MonetDBEmbeddedException {
        return this.fetchColumnValuesAsync(0, this.numberOfRows);
    }*/

    @Override
    public ListIterator<T> iterator() {
        try {
            return Arrays.asList(this.fetchAllColumnValues()).listIterator();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Internal implementation to fetch values from the column.
     */
    private native T[] fetchValuesInternal(long resultPointer, int resultSetIndex, Class<T> jclass, int enumEntry,
                                           int first, int last) throws MonetDBEmbeddedException;
}