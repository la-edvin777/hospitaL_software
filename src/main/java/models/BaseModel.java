/**
 * Abstract base class for all database entity models in the Hospital Management System.
 * Provides common CRUD (Create, Read, Update, Delete) operations and database interaction functionality.
 * 
 * @param <T> The specific entity type that extends this base class
 */
package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseModel<T extends BaseModel<T>> {
    /**
     * Gets the name of the database table associated with this entity.
     * @return The table name as a String
     */
    protected abstract String getTableName();

    /**
     * Maps a database ResultSet row to an entity object.
     * @param rs The ResultSet containing the database row
     * @return A new instance of the entity populated with data from the ResultSet
     * @throws SQLException if there's an error accessing the ResultSet
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;

    /**
     * Sets parameters for the create (INSERT) prepared statement.
     * @param stmt The PreparedStatement for the insert operation
     * @param entity The entity containing the data to insert
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setCreateStatement(PreparedStatement stmt, T entity) throws SQLException;

    /**
     * Sets parameters for the update (UPDATE) prepared statement.
     * @param stmt The PreparedStatement for the update operation
     * @param entity The entity containing the updated data
     * @throws SQLException if there's an error setting the parameters
     */
    protected abstract void setUpdateStatement(PreparedStatement stmt, T entity) throws SQLException;

    /**
     * Gets the SQL statement for creating a new record.
     * @return The INSERT SQL statement
     */
    protected abstract String getCreateSQL();

    /**
     * Gets the SQL statement for updating an existing record.
     * @return The UPDATE SQL statement
     */
    protected abstract String getUpdateSQL();

    /**
     * Gets the SQL statement for deleting a record.
     * @return The DELETE SQL statement
     */
    protected abstract String getDeleteSQL();

    /**
     * Gets the SQL statement for retrieving all records.
     * @return The SELECT ALL SQL statement
     */
    protected abstract String getSelectAllSQL();

    /**
     * Gets the SQL statement for retrieving a record by ID.
     * @return The SELECT BY ID SQL statement
     */
    protected abstract String getSelectByIdSQL();

    /**
     * Sets the ID parameter in a prepared statement.
     * @param stmt The PreparedStatement requiring the ID parameter
     * @param id The ID value to set
     * @throws SQLException if there's an error setting the parameter
     */
    protected abstract void setIdParameter(PreparedStatement stmt, Object id) throws SQLException;

    protected void afterCreate(Connection conn) throws SQLException {}
    protected void afterUpdate(Connection conn) throws SQLException {}
    protected void beforeDelete(Connection conn) throws SQLException {}

    /**
     * Creates a new record in the database.
     * @param conn The database connection
     * @throws SQLException if there's an error executing the insert
     */
    public void create(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(getCreateSQL())) {
            setCreateStatement(stmt, (T)this);
            stmt.executeUpdate();
            afterCreate(conn);
        }
    }

    /**
     * Updates an existing record in the database.
     * @param conn The database connection
     * @throws SQLException if there's an error executing the update
     */
    public void update(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(getUpdateSQL())) {
            setUpdateStatement(stmt, (T)this);
            stmt.executeUpdate();
            afterUpdate(conn);
        }
    }

    /**
     * Deletes a record from the database.
     * @param conn The database connection
     * @throws SQLException if there's an error executing the delete
     */
    public void delete(Connection conn) throws SQLException {
        beforeDelete(conn);
        try (PreparedStatement stmt = conn.prepareStatement(getDeleteSQL())) {
            setIdParameter(stmt, getId());
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieves all records from the database.
     * @param conn The database connection
     * @return A List of all entities
     * @throws SQLException if there's an error executing the select
     */
    public List<T> selectAll(Connection conn) throws SQLException {
        List<T> entities = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(getSelectAllSQL());
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                entities.add(mapResultSetToEntity(rs));
            }
        }
        return entities;
    }

    /**
     * Retrieves a single record by ID from the database.
     * @param conn The database connection
     * @param id The ID of the record to retrieve
     * @return The entity if found, null otherwise
     * @throws SQLException if there's an error executing the select
     */
    public T selectById(Connection conn, Object id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(getSelectByIdSQL())) {
            setIdParameter(stmt, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                }
            }
        }
        return null;
    }

    protected abstract Object getId();
} 