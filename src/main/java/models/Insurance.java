package models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Insurance extends BaseModel<Insurance> {
    private String insuranceID;
    private String company;
    private String address;
    private String phone;

    // Constructor
    public Insurance(String insuranceID, String company, String address, String phone) {
        this.insuranceID = insuranceID;
        this.company = company;
        this.address = address;
        this.phone = phone;
    }

    // Default constructor
    public Insurance() {}

    // Getters and Setters
    public String getInsuranceID() { return insuranceID; }
    public void setInsuranceID(String insuranceID) { this.insuranceID = insuranceID; }
    
    // Add lowercase version for compatibility with UI
    public String getInsuranceid() { return insuranceID; }
    
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    protected String getTableName() {
        return "insurance";
    }

    @Override
    protected Insurance mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Insurance(
            rs.getString("insuranceid"),
            rs.getString("company"),
            rs.getString("address"),
            rs.getString("phone")
        );
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, Insurance entity) throws SQLException {
        stmt.setString(1, entity.getInsuranceID());
        stmt.setString(2, entity.getCompany());
        stmt.setString(3, entity.getAddress());
        stmt.setString(4, entity.getPhone());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Insurance entity) throws SQLException {
        stmt.setString(1, entity.getCompany());
        stmt.setString(2, entity.getAddress());
        stmt.setString(3, entity.getPhone());
        stmt.setString(4, entity.getInsuranceID());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO insurance (insuranceid, company, address, phone) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE insurance SET company = ?, address = ?, phone = ? WHERE insuranceid = ?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM insurance WHERE insuranceid = ?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT * FROM insurance";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT * FROM insurance WHERE insuranceid = ?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String) id);
    }

    @Override
    public String toString() {
        return String.format("Insurance[ID=%s, Company=%s, Phone=%s]", 
            insuranceID, company, phone);
    }

    @Override
    protected Object getId() {
        return insuranceID;
    }
} 