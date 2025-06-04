package models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Drug extends BaseModel<Drug> {
    private int drugID;
    private String name;
    private String sideeffects;
    private String benefits;

    // Constructor
    public Drug(int drugID, String name, String sideeffects, String benefits) {
        this.drugID = drugID;
        this.name = name;
        this.sideeffects = sideeffects;
        this.benefits = benefits;
    }

    // Default constructor
    public Drug() {}

    // Getters and Setters
    public int getDrugID() { return drugID; }
    public void setDrugID(int drugID) { this.drugID = drugID; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSideeffects() { return sideeffects; }
    public void setSideeffects(String sideeffects) { this.sideeffects = sideeffects; }
    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    @Override
    protected String getTableName() {
        return "Drug";
    }

    @Override
    protected Drug mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Drug(
            rs.getInt("drugID"),
            rs.getString("name"),
            rs.getString("sideeffects"),
            rs.getString("benefits")
        );
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, Drug entity) throws SQLException {
        stmt.setInt(1, entity.getDrugID());
        stmt.setString(2, entity.getName());
        stmt.setString(3, entity.getSideeffects());
        stmt.setString(4, entity.getBenefits());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Drug entity) throws SQLException {
        stmt.setString(1, entity.getName());
        stmt.setString(2, entity.getSideeffects());
        stmt.setString(3, entity.getBenefits());
        stmt.setInt(4, entity.getDrugID());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO Drug (drugID, name, sideeffects, benefits) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE Drug SET name = ?, sideeffects = ?, benefits = ? WHERE drugID = ?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM Drug WHERE drugID = ?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT * FROM Drug";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT * FROM Drug WHERE drugID = ?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setInt(1, (Integer) id);
    }

    @Override
    public String toString() {
        return String.format("Drug[ID=%d, Name=%s]", 
            drugID, name);
    }

    @Override
    protected Object getId() {
        return drugID;
    }
} 