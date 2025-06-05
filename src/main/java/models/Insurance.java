package models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Insurance extends BaseModel<Insurance> {
    private String insuranceid;
    private String company;
    private String address;
    private String phone;

    // Default constructor
    public Insurance() {}

    // Constructor
    public Insurance(String insuranceid, String company, String address, String phone) {
        this.insuranceid = insuranceid;
        this.company = company;
        this.address = address;
        this.phone = phone;
    }

    // Getters and Setters
    public String getInsuranceid() { return insuranceid; }
    public void setInsuranceid(String insuranceid) { this.insuranceid = insuranceid; }
    
    // Legacy getter for compatibility
    public String getInsuranceID() { return insuranceid; }
    public void setInsuranceID(String insuranceID) { this.insuranceid = insuranceID; }
    
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
        stmt.setString(1, entity.getInsuranceid());
        stmt.setString(2, entity.getCompany());
        stmt.setString(3, entity.getAddress());
        stmt.setString(4, entity.getPhone());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Insurance entity) throws SQLException {
        stmt.setString(1, entity.getCompany());
        stmt.setString(2, entity.getAddress());
        stmt.setString(3, entity.getPhone());
        stmt.setString(4, entity.getInsuranceid());
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
        return "SELECT * FROM insurance ORDER BY company";
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
            insuranceid, company, phone);
    }

    @Override
    protected Object getId() {
        return insuranceid;
    }
}