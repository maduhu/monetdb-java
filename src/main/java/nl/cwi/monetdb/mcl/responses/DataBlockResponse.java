package nl.cwi.monetdb.mcl.responses;

import nl.cwi.monetdb.mcl.protocol.AbstractProtocol;
import nl.cwi.monetdb.mcl.protocol.ProtocolException;
import nl.cwi.monetdb.mcl.protocol.ServerResponses;

import java.sql.SQLException;
import java.sql.Types;

/**
 * The DataBlockResponse is tabular data belonging to a
 * ResultSetResponse.  Tabular data from the server typically looks
 * like:
 * <pre>
 * [ "value",	56	]
 * </pre>
 * where each column is separated by ",\t" and each tuple surrounded
 * by brackets ("[" and "]").  A DataBlockResponse object holds the
 * raw data as read from the server, in a parsed manner, ready for
 * easy retrieval.
 *
 * This object is not intended to be queried by multiple threads
 * synchronously. It is designed to work for one thread retrieving
 * rows from it.  When multiple threads will retrieve rows from this
 * object, it is possible for threads to get the same data.
 */
public class DataBlockResponse implements IIncompleteResponse {

    /** The array to keep the data in */
    private Object[] data;
    /** The counter which keeps the current position in the data array */
    private int pos;
    /** The connection protocol to parse the tuple lines */
    private final AbstractProtocol<?> protocol;
    /** The JdbcSQLTypes mapping */
    private final int[] jdbcSQLTypes;
    /** A mapping of null values of the current Row */
    private boolean[][] nullMappings;
    /** A 'pointer' to the current line */
    private int blockLine;

    /**
     * Constructs a DataBlockResponse object.
     *
     * @param rowcount the number of rows
     * @param columncount the number of columns
     * @param forward whether this is a forward only result
     */
    DataBlockResponse(int rowcount, int columncount, boolean forward, AbstractProtocol<?> protocol, int[] JdbcSQLTypes) {
        this.pos = -1;
        this.data = new Object[columncount];
        this.nullMappings = new boolean[rowcount][columncount];
        this.protocol = protocol;
        this.jdbcSQLTypes = JdbcSQLTypes;
    }

    /**
     * addLine adds a String of data to this object's data array. Note that an IndexOutOfBoundsException can be thrown
     * when an attempt is made to add more than the original construction size specified.
     *
     * @param line the header line as String
     * @param response the line type according to the MAPI protocol
     * @throws ProtocolException If the result line is not expected
     */
    @Override
    public void addLine(ServerResponses response, Object line) throws ProtocolException {
        if (response != ServerResponses.RESULT)
            throw new ProtocolException("protocol violation: unexpected line in data block: " + line.toString());

        if(this.pos == -1) { //if it's the first line, initialize the matrix
            int numberOfColumns = this.data.length, numberOfRows = this.nullMappings.length;
            for (int i = 0 ; i < numberOfColumns ; i++) {
                switch (this.jdbcSQLTypes[i]) {
                    case Types.BOOLEAN:
                        this.data[i] = new boolean[numberOfRows];
                        break;
                    case Types.TINYINT:
                        this.data[i] = new byte[numberOfRows];
                        break;
                    case Types.SMALLINT:
                        this.data[i] = new short[numberOfRows];
                        break;
                    case Types.INTEGER:
                        this.data[i] = new int[numberOfRows];
                        break;
                    case Types.BIGINT:
                        this.data[i] = new long[numberOfRows];
                        break;
                    case Types.REAL:
                        this.data[i] = new float[numberOfRows];
                        break;
                    case Types.DOUBLE:
                        this.data[i] = new double[numberOfRows];
                        break;
                    default:
                        this.data[i] = new Object[numberOfRows];
                }
            }
        }

        // add to the backing array
        int nextPos = ++this.pos;
        this.protocol.parseTupleLine(nextPos, line, this.jdbcSQLTypes, this.data, this.nullMappings[nextPos]);
    }

    /**
     * Returns whether this Response expects more lines to be added to it.
     *
     * @return true if a next line should be added, false otherwise
     */
    @Override
    public boolean wantsMore() {
        // remember: pos is the value already stored
        return (this.pos + 1) < this.nullMappings.length;
    }

    /**
     * Indicates that no more header lines will be added to this Response implementation. In most cases this is a
     * redundant operation because the data array is full. However... it can happen that this is NOT the case!
     *
     * @throws SQLException if not all rows are filled
     */
    @Override
    public void complete() throws SQLException {
        if ((this.pos + 1) != this.nullMappings.length) {
            throw new SQLException("Inconsistent state detected! Current block capacity: " + this.nullMappings.length +
                    ", block usage: " + (this.pos + 1) + ". Did MonetDB send what it promised to?", "M0M10");
        }
    }

    /**
     * Instructs the Response implementation to close and do the necessary clean up procedures.
     */
    @Override
    public void close() {
        // feed all rows to the garbage collector
        int numberOfColumns = this.data.length;
        for (int i = 0; i < numberOfColumns; i++) {
            data[i] = null;
            nullMappings[i] = null;
        }
        data = null;
        nullMappings = null;
    }

    /* Methods to be called after the block construction has been completed */

    void setBlockLine(int blockLine) {
        this.blockLine = blockLine;
    }

    public void setData(Object[] data) { /* For VirtualResultSet :( */
        this.data = data;
    }

    public Object[] getData() { /* For VirtualResultSet :( */
        return data;
    }

    public boolean checkValueIsNull(int column) {
        return this.nullMappings[this.blockLine][column];
    }

    public boolean getBooleanValue(int column) {
        return ((boolean[]) this.data[column])[this.blockLine];
    }

    public byte getByteValue(int column) {
        return ((byte[]) this.data[column])[this.blockLine];
    }

    public short getShortValue(int column) {
        return ((short[]) this.data[column])[this.blockLine];
    }

    public int getIntValue(int column) {
        return ((int[]) this.data[column])[this.blockLine];
    }

    public long getLongValue(int column) {
        return ((long[]) this.data[column])[this.blockLine];
    }

    public float getFloatValue(int column) {
        return ((float[]) this.data[column])[this.blockLine];
    }

    public double getDoubleValue(int column) {
        return ((double[]) this.data[column])[this.blockLine];
    }

    public Object getObjectValue(int column) {
        return ((Object[]) this.data[column])[this.blockLine];
    }

    public String getValueAsString(int column) {
        switch (this.jdbcSQLTypes[column]) {
            case Types.BOOLEAN:
                return Boolean.toString(((boolean[]) this.data[column])[this.blockLine]);
            case Types.TINYINT:
                return Byte.toString(((byte[]) this.data[column])[this.blockLine]);
            case Types.SMALLINT:
                return Short.toString(((short[]) this.data[column])[this.blockLine]);
            case Types.INTEGER:
                return Integer.toString(((int[]) this.data[column])[this.blockLine]);
            case Types.BIGINT:
                return Long.toString(((long[]) this.data[column])[this.blockLine]);
            case Types.REAL:
                return Float.toString(((float[]) this.data[column])[this.blockLine]);
            case Types.DOUBLE:
                return Double.toString(((double[]) this.data[column])[this.blockLine]);
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.CLOB:
            case Types.OTHER:
                return (String) ((Object[]) this.data[column])[this.blockLine];
            default:
                return ((Object[]) this.data[column])[this.blockLine].toString();
        }
    }

    public Object getValueAsObject(int column) {
        switch (this.jdbcSQLTypes[column]) {
            case Types.BOOLEAN:
                return ((boolean[]) this.data[column])[this.blockLine];
            case Types.TINYINT:
                return (((byte[]) this.data[column])[this.blockLine]);
            case Types.SMALLINT:
                return (((short[]) this.data[column])[this.blockLine]);
            case Types.INTEGER:
                return (((int[]) this.data[column])[this.blockLine]);
            case Types.BIGINT:
                return (((long[]) this.data[column])[this.blockLine]);
            case Types.REAL:
                return (((float[]) this.data[column])[this.blockLine]);
            case Types.DOUBLE:
                return (((double[]) this.data[column])[this.blockLine]);
            default:
                return ((Object[]) this.data[column])[this.blockLine];
        }
    }
}
