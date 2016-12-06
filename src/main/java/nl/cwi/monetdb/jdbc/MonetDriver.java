/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright 1997 - July 2008 CWI, August 2008 - 2016 MonetDB B.V.
 */

package nl.cwi.monetdb.jdbc;

import nl.cwi.monetdb.mcl.connection.MonetDBConnectionFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * A Driver suitable for the MonetDB database.
 *
 * This driver will be used by the DriverManager to determine if an URL
 * is to be handled by this driver, and if it does, then this driver
 * will supply a Connection suitable for MonetDB.
 *
 * This class has no explicit constructor, the default constructor
 * generated by the Java compiler will be sufficient since nothing has
 * to be set in order to use this driver.
 *
 * This Driver supports MonetDB database URLs. MonetDB URLs are defined
 * as:
 * <tt>jdbc:monetdb://&lt;host&gt;[:&lt;port&gt;]/&lt;database&gt;</tt>
 * where [:&lt;port&gt;] denotes that a port is optional. If not
 * given the default (@JDBC_DEF_PORT@) will be used.
 *
 * @author Fabian Groffen
 * @version @JDBC_MAJOR@.@JDBC_MINOR@ (@JDBC_VER_SUFFIX@)
 */
final public class MonetDriver implements Driver {
	// the url kind will be jdbc:monetdb://<host>[:<port>]/<database>
	// Chapter 9.2.1 from Sun JDBC 3.0 specification
	/** The prefix of a MonetDB url */
	private static final String MONETURL = "jdbc:monetdb://";
	/** Major version of this driver */
	private static final int DRIVERMAJOR = 4;
	/** Minor version of this driver */
	private static final int DRIVERMINOR = 1;
	/** Version suffix string */
	private static final String DRIVERVERSIONSUFFIX =
		"@JDBC_VER_SUFFIX@ based on MCL v@MCL_MAJOR@.@MCL_MINOR@";
	// We're not fully compliant, but what we support is compliant
	/** Whether this driver is JDBC compliant or not */
	private static final boolean MONETJDBCCOMPLIANT = false;

	/** MonetDB default port to connect to */
	private static final String PORT = "50000";

	public static String getPORT() {
		return PORT;
	}

	// initialize this class: register it at the DriverManager
	// Chapter 9.2 from Sun JDBC 3.0 specification
	static {
		try {
			DriverManager.registerDriver(new MonetDriver());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	//== methods of interface Driver

	/**
	 * Retrieves whether the driver thinks that it can open a connection to the
	 * given URL. Typically drivers will return true if they understand the
	 * subprotocol specified in the URL and false if they do not.
	 *
	 * @param url the URL of the database
	 * @return true if this driver understands the given URL; false otherwise
	 */
	public boolean acceptsURL(String url) {
		return url != null && url.startsWith(MONETURL);
	}

	/**
	 * Attempts to make a database connection to the given URL. The driver
	 * should return "null" if it realizes it is the wrong kind of driver to
	 * connect to the given URL. This will be common, as when the JDBC driver
	 * manager is asked to connect to a given URL it passes the URL to each
	 * loaded driver in turn.
	 *
	 * The driver should throw an SQLException if it is the right driver to
	 * connect to the given URL but has trouble connecting to the database.
	 *
	 * The java.util.Properties argument can be used to pass arbitrary string
	 * tag/value pairs as connection arguments. Normally at least "user" and
	 * "password" properties should be included in the Properties object.
	 *
	 * @param url the URL of the database to which to connect
	 * @param info a list of arbitrary string tag/value pairs as connection
	 *        arguments. Normally at least a "user" and "password" property
	 *        should be included
	 * @return a Connection object that represents a connection to the URL
	 * @throws SQLException if a database access error occurs
	 */
	public Connection connect(String url, Properties info) throws SQLException {
		int tmp;
		Properties props = new Properties();
		props.put("port", PORT);
		props.putAll(info);
		info = props;

		// url should be of style jdbc:monetdb://<host>/<database>
		if (!acceptsURL(url))
			throw new SQLException("Invalid URL: it does not start with: " + MONETURL, "08M26");

		// remove leading "jdbc:" so the rest is a valid hierarchical URI
		URI uri;
		try {
			uri = new URI(url.substring(5));
		} catch (URISyntaxException e) {
			throw new SQLException(e.toString(), "08M26");
		}

		String uri_host = uri.getHost();
		if (uri_host == null)
			throw new SQLException("Invalid URL: no hostname given or unparsable in '" + url + "'", "08M26");
		info.put("host", uri_host);

		int uri_port = uri.getPort();
		if (uri_port > 0)
			info.put("port", "" + uri_port);

		// check the database
		String uri_path = uri.getPath();
		if (uri_path != null && uri_path.length() != 0) {
			uri_path = uri_path.substring(1);
			if (!uri_path.trim().isEmpty())
				info.put("database", uri_path);
		}

		String uri_query = uri.getQuery();
		if (uri_query != null) {
			// handle additional arguments
			String args[] = uri_query.split("&");
			for (String arg : args) {
				tmp = arg.indexOf('=');
				if (tmp > 0)
					info.put(arg.substring(0, tmp), arg.substring(tmp + 1));
			}
		}

		// finally return the Connection as requested
		return MonetDBConnectionFactory.CreateMonetDBJDBCConnection(info);
	}

	/**
	 * Retrieves the driver's major version number. Initially this should be 1.
	 *
	 * @return this driver's major version number
	 */
	public int getMajorVersion() {
		return DRIVERMAJOR;
	}

	/**
	 * Gets the driver's minor version number. Initially this should be 0.
	 *
	 * @return this driver's minor version number
	 */
	public int getMinorVersion() {
		return DRIVERMINOR;
	}

	/**
	 * Gets information about the possible properties for this driver.
	 *
	 * The getPropertyInfo method is intended to allow a generic GUI tool to
	 * discover what properties it should prompt a human for in order to get
	 * enough information to connect to a database. Note that depending on the
	 * values the human has supplied so far, additional values may become
	 * necessary, so it may be necessary to iterate though several calls to the
	 * getPropertyInfo method.
	 *
	 * @param url the URL of the database to which to connect
	 * @param info a proposed list of tag/value pairs that will be sent on
	 *        connect open
	 * @return an array of DriverPropertyInfo objects describing possible
	 *         properties. This array may be an empty array if no properties
	 *         are required.
	 */
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
		if (!acceptsURL(url))
			return null;

		List<DriverPropertyInfo> props = new ArrayList<>();

		DriverPropertyInfo prop = new DriverPropertyInfo("user", info.getProperty("user"));
		prop.required = true;
		prop.description = "The user loginname to use when authenticating on the database server";
		props.add(prop);

		prop = new DriverPropertyInfo("password", info.getProperty("password"));
		prop.required = true;
		prop.description = "The password to use when authenticating on the database server";
		props.add(prop);

		prop = new DriverPropertyInfo("debug", "false");
		prop.required = false;
		prop.description = "Whether or not to create a log file for debugging purposes";
		props.add(prop);

		prop = new DriverPropertyInfo("logfile", "");
		prop.required = false;
		prop.description = "The filename to write the debug log to. Only takes effect if debug is set to true. If the file exists, an incrementing number is added, till the filename is unique.";
		props.add(prop);

		prop = new DriverPropertyInfo("language", "sql");
		prop.required = false;
		prop.description = "What language to use for MonetDB conversations (experts only)";
		props.add(prop);

		prop = new DriverPropertyInfo("hash", "");
		prop.required = false;
		prop.description = "Force the use of the given hash algorithm during challenge response (one of SHA1, MD5, plain)";
		props.add(prop);

		prop = new DriverPropertyInfo("follow_redirects", "true");
		prop.required = false;
		prop.description = "Whether redirects issued by the server should be followed";
		props.add(prop);

		prop = new DriverPropertyInfo("treat_blob_as_binary", "false");
		prop.required = false;
		prop.description = "Whether BLOBs on the server should be treated as BINARY types, thus mapped to byte[]";
		props.add(prop);

		prop = new DriverPropertyInfo("so_timeout", "0");
		prop.required = false;
		prop.description = "Defines the maximum time to wait in milliseconds on a blocking read socket call"; // this corresponds to the Connection.setNetworkTimeout() method introduced in JDBC 4.1
		props.add(prop);

		prop = new DriverPropertyInfo("embedded", "false");
		prop.required = false;
		prop.description = "Whether or not to use an embedded MonetDB connection";
		props.add(prop);

		DriverPropertyInfo[] dpi = new DriverPropertyInfo[props.size()];
		return props.toArray(dpi);
	}

	/**
	 * Reports whether this driver is a genuine JDBC Compliant&tm; driver. A
	 * driver may only report true here if it passes the JDBC compliance tests;
	 * otherwise it is required to return false.
	 *
	 * JDBC compliance requires full support for the JDBC API and full support
	 * for SQL 92 Entry Level. It is expected that JDBC compliant drivers will
	 * be available for all the major commercial databases.
	 *
	 * This method is not intended to encourage the development of non-JDBC
	 * compliant drivers, but is a recognition of the fact that some vendors are
	 * interested in using the JDBC API and framework for lightweight databases
	 * that do not support full database functionality, or for special databases
	 * such as document information retrieval where a SQL implementation may not
	 * be feasible.
	 *
	 * @return true if this driver is JDBC Compliant; false otherwise
	 */
	public boolean jdbcCompliant() {
		return MONETJDBCCOMPLIANT;
	}

	//== end methods of interface driver


	/** A static Map containing the mapping between MonetDB types and Java SQL types */
	/* use SELECT sqlname, * FROM sys.types order by 1, id; to view all MonetDB types */
	/* see http://docs.oracle.com/javase/7/docs/api/java/sql/Types.html to view all supported java SQL types */
	private static Map<String, Integer> typeMap = new HashMap<>();
	static {
		// fill the typeMap once
		// typeMap.put("any", Integer.valueOf(Types.???));
		typeMap.put("bigint", Types.BIGINT);
		typeMap.put("blob", Types.BLOB);
		typeMap.put("boolean", Types.BOOLEAN);
		typeMap.put("char", Types.CHAR);
		typeMap.put("clob", Types.CLOB);
		typeMap.put("date", Types.DATE);
		typeMap.put("decimal", Types.DECIMAL);
		typeMap.put("double", Types.DOUBLE);
		typeMap.put("geometry", Types.VARCHAR);
		typeMap.put("geometrya", Types.VARCHAR);
		typeMap.put("hugeint", Types.NUMERIC);
		typeMap.put("inet", Types.VARCHAR);
		typeMap.put("int", Types.INTEGER);
		typeMap.put("json", Types.VARCHAR);
		// typeMap.put("mbr", Integer.valueOf(Types.???));
		typeMap.put("month_interval", Types.INTEGER);
		typeMap.put("oid", Types.BIGINT);
		// typeMap.put("ptr", Integer.valueOf(Types.???));
		typeMap.put("real", Types.REAL);
		typeMap.put("sec_interval", Types.DECIMAL);
		typeMap.put("smallint", Types.SMALLINT);
		// typeMap.put("table", Integer.valueOf(Types.???));
		typeMap.put("time", Types.TIME);
		typeMap.put("timestamp", Types.TIMESTAMP);
		typeMap.put("timestamptz", Types.TIMESTAMP);
// new in Java 8: Types.TIMESTAMP_WITH_TIMEZONE (value 2014). Can't use it yet as we compile for java 7
		typeMap.put("timetz", Types.TIME);
// new in Java 8: Types.TIME_WITH_TIMEZONE (value 2013). Can't use it yet as we compile for java 7
		typeMap.put("tinyint", Types.TINYINT);
		typeMap.put("url", Types.VARCHAR);
		typeMap.put("uuid", Types.VARCHAR);
		typeMap.put("varchar", Types.VARCHAR);
		typeMap.put("wrd", Types.BIGINT);
	}

	/**
	 * Returns the java.sql.Types equivalent of the given MonetDB type.
	 *
	 * @param type the type as used by MonetDB
	 * @return the mathing java.sql.Types constant or java.sql.Types.OTHER if
	 *         nothing matched on the given string
	 */
	static int getJavaType(String type) {
		// match the currentColumns type on a java.sql.Types constant
		Integer tp = typeMap.get(type);
		if (tp != null) {
			return tp;
		} else {
			// this should not be able to happen
			// do not assert, since maybe future versions introduce
			// new types
			return Types.OTHER;
		}
	}

	private static String TypeMapppingSQL = null;	// cache to optimise getSQLTypeMap()

	/**
	 * Returns a String usable in an SQL statement to map the server types
	 * to values of java.sql.Types using the global static type map.
	 * The returned string will be a SQL CASE x statement where the x is
	 * replaced with the given currentColumns name (or expression) string.
	 *
	 * @param column a String representing the value that should be evaluated
	 *               in the SQL CASE statement
	 * @return a SQL CASE statement
	 */
	static String getSQLTypeMap(String column) {
		if (TypeMapppingSQL == null) {
			// first time, compose TypeMappping SQL string
			StringBuilder val = new StringBuilder((typeMap.size() * (7 + 7 + 7 + 4)) + 14);
			for (Entry<String, Integer> entry : typeMap.entrySet()) {
				val.append(" WHEN '").append(entry.getKey()).append("' THEN ").append(entry.getValue().toString());
			}
			val.append(" ELSE ").append(Types.OTHER).append(" END");
			// as the typeMap is static, cache this SQL part for all next calls
			TypeMapppingSQL = val.toString();
		}
		return "CASE " + column + TypeMapppingSQL;
	}

	/**
	 * Returns a touched up identifying version string of this driver.
	 *
	 * @return the version string
	 */
	public static String getDriverVersion() {
		return "" + DRIVERMAJOR + "." + DRIVERMINOR + " (" + DRIVERVERSIONSUFFIX + ")";
	}

	public static int getDriverMajorVersion() {
		return DRIVERMAJOR;
	}

	public static int getDriverMinorVersion() {
		return DRIVERMINOR;
	}

	/**
	 * Return the parent Logger of all the Loggers used by this data
	 * source.  This should be the Logger farthest from the root Logger
	 * that is still an ancestor of all of the Loggers used by this data
	 * source.  Configuring this Logger will affect all of the log
	 * messages generated by the data source. In the worst case, this
	 * may be the root Logger.
	 *
	 * @return the parent Logger for this data source
	 * @throws SQLFeatureNotSupportedException if the data source does
	 *         not use java.util.logging
	 */
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("java.util.logging not in use", "0A000");
	}
}