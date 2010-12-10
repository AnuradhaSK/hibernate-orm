/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.spi;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.jdbc.Expectation;

/**
 * Encapsulates JDBC Connection management SPI.
 * <p/>
 * The lifecycle is intended to span a logical series of interactions with the
 * database.  Internally, this means the the lifecycle of the Session.
 *
 * @author Gail Badner
 */
public interface ConnectionManager extends Serializable {

	/**
	 * Retrieves the connection currently managed by this ConnectionManager.
	 * <p/>
	 * Note, that we may need to obtain a connection to return here if a
	 * connection has either not yet been obtained (non-UserSuppliedConnectionProvider)
	 * or has previously been aggressively released (if supported in this environment).
	 *
	 * @return The current Connection.
	 *
	 * @throws HibernateException Indicates a connection is currently not
	 * available (we are currently manually disconnected).
	 */
	Connection getConnection();

	// TODO: should this be removd from the SPI?
	boolean hasBorrowedConnection();

	// TODO: should this be removd from the SPI?
	void releaseBorrowedConnection();

	/**
	 * Is this ConnectionManager instance "logically" connected.  Meaning
	 * do we either have a cached connection available or do we have the
	 * ability to obtain a connection on demand.
	 *
	 * @return True if logically connected; false otherwise.
	 */
	boolean isCurrentlyConnected();

	/**
	 * To be called after execution of each JDBC statement.  Used to
	 * conditionally release the JDBC connection aggressively if
	 * the configured release mode indicates.
	 */
	void afterStatement();

	void setTransactionTimeout(int seconds);

	/**
	 * To be called after Session completion.  Used to release the JDBC
	 * connection.
	 *
	 * @return The connection mantained here at time of close.  Null if
	 * there was no connection cached internally.
	 */
	Connection close();

	/**
	 * Manually disconnect the underlying JDBC Connection.  The assumption here
	 * is that the manager will be reconnected at a later point in time.
	 *
	 * @return The connection mantained here at time of disconnect.  Null if
	 * there was no connection cached internally.
	 */
	Connection manualDisconnect();

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for ConnectionProvider-supplied connections.
	 */
	void manualReconnect();

	/**
	 * Manually reconnect the underlying JDBC Connection.  Should be called at
	 * some point after manualDisconnect().
	 * <p/>
	 * This form is used for user-supplied connections.
	 */
	void manualReconnect(Connection suppliedConnection);

	/**
	 * Callback to let us know that a flush is beginning.  We use this fact
	 * to temporarily circumvent aggressive connection releasing until after
	 * the flush cycle is complete {@link #flushEnding()}
	 */
	void flushBeginning();

	/**
	 * Callback to let us know that a flush is ending.  We use this fact to
	 * stop circumventing aggressive releasing connections.
	 */
	void flushEnding();

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating,
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, int)}).
	 */
	public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys);

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 * using JDBC3 getGeneratedKeys ({@link java.sql.Connection#prepareStatement(String, String[])}).
	 */
	public PreparedStatement prepareStatement(String sql, String[] columnNames);

	/**
	 * Get a non-batchable prepared statement to use for selecting. Does not
	 * result in execution of the current batch.
	 */
	public PreparedStatement prepareSelectStatement(String sql);

	/**
	 * Get a non-batchable prepared statement to use for inserting / deleting / updating.
	 */
	public PreparedStatement prepareStatement(String sql, boolean isCallable);

	/**
	 * Get a non-batchable callable statement to use for inserting / deleting / updating.
	 */
	public CallableStatement prepareCallableStatement(String sql);

	/**
	 * Get a batchable prepared statement to use for inserting / deleting / updating
	 * (might be called many times before a single call to <tt>executeBatch()</tt>).
	 * After setting parameters, call <tt>addToBatch</tt> - do not execute the
	 * statement explicitly.
	 * @see org.hibernate.jdbc.Batcher#addToBatch
	 */
	public PreparedStatement prepareBatchStatement(String sql, boolean isCallable);

	/**
	 * Get a prepared statement for use in loading / querying. If not explicitly
	 * released by <tt>closeQueryStatement()</tt>, it will be released when the
	 * session is closed or disconnected.
	 */
	public PreparedStatement prepareQueryStatement(
			String sql,
			boolean isScrollable,
			ScrollMode scrollMode,
			boolean isCallable);
	/**
	 * Cancel the current query statement
	 */
	public void cancelLastQuery();

	public void abortBatch(SQLException sqle);

	public void addToBatch(Expectation expectation );

	public void executeBatch();
}
