package models;

import java.sql.*;

public class DoctorSpecialty extends BaseModel<DoctorSpecialty> {
    private String doctorid;
    private String specialty;
    private int experience;

    public DoctorSpecialty(String doctorid, String specialty, int experience) {
        this.doctorid = doctorid;
        this.specialty = specialty;
        this.experience = experience;
    }

    // Default constructor
    public DoctorSpecialty() {}

    // Getters and Setters
    public String getDoctorid() { return doctorid; }
    public void setDoctorid(String doctorid) { this.doctorid = doctorid; }
    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    @Override
    protected String getTableName() {
        return "doctorspecialty";
    }

    @Override
    protected DoctorSpecialty mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new DoctorSpecialty(
            rs.getString("doctorid"),
            rs.getString("specialty"),
            rs.getInt("experience")
        );
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, DoctorSpecialty entity) throws SQLException {
        stmt.setString(1, entity.getDoctorid());
        stmt.setString(2, entity.getSpecialty());
        stmt.setInt(3, entity.getExperience());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, DoctorSpecialty entity) throws SQLException {
        stmt.setString(1, entity.getSpecialty());
        stmt.setInt(2, entity.getExperience());
        stmt.setString(3, entity.getDoctorid());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO doctorspecialty (doctorid, specialty, experience) VALUES (?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE doctorspecialty SET specialty=?, experience=? WHERE doctorid=?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM doctorspecialty WHERE doctorid=?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT * FROM doctorspecialty";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT * FROM doctorspecialty WHERE doctorid=?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String)id);
    }

    @Override
    public String toString() {
        return String.format("DoctorSpecialty{doctorid='%s', specialty='%s', experience=%d}", 
                doctorid, specialty, experience);
    }

    @Override
    protected Object getId() {
        return doctorid;
    }
} 