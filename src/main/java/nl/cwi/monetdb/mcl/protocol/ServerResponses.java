package nl.cwi.monetdb.mcl.protocol;

/**
 * Created by ferreira on 11/30/16.
 */
public enum ServerResponses {

    /** "there is currently no line", or the the type is unknown is represented by UNKNOWN */
    UNKNOWN,
    /** a line starting with ! indicates ERROR */
    ERROR,
    /** a line starting with % indicates HEADER */
    HEADER,
    /** a line starting with [ indicates RESULT */
    RESULT,
    /** a line which matches the pattern of prompt1 is a PROMPT */
    PROMPT,
    /** a line which matches the pattern of prompt2 is a MORE */
    MORE,
    /** a line starting with &amp; indicates the start of a header block */
    SOHEADER,
    /** a line starting with ^ indicates REDIRECT */
    REDIRECT,
    /** a line starting with # indicates INFO */
    INFO
}