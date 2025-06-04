package models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

public class Doctor extends BaseModel<Doctor> {
    private String doctorid;
    private String firstname;
    private String surname;
    private String address;
    private String email;
    private String specialization;

    // Constructor
    public Doctor(String doctorid, String firstname, String surname, String address, String email, String specialization) {
        this.doctorid = doctorid;
        this.firstname = firstname;
        this.surname = surname;
        this.address = address;
        this.email = email;
        this.specialization = specialization;
    }

    // Constructor for Specialist class
    public Doctor(String doctorid, String name, String specialization) {
        this.doctorid = doctorid;
        String[] names = name.split(" ", 2);
        this.firstname = names.length > 0 ? names[0] : "";
        this.surname = names.length > 1 ? names[1] : "";
        this.specialization = specialization;
    }

    // Default constructor
    public Doctor() {}

    @Override
    protected void afterCreate(Connection conn) throws SQLException {
        // If specialization is not "general", add to specialists table
        if (!"general".equalsIgnoreCase(specialization)) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO doctorspecialty (doctorid, specialty, experience) VALUES (?, ?, 0)")) {
                stmt.setString(1, doctorid);
                stmt.setString(2, specialization);
                stmt.executeUpdate();
            }
        }
    }

    @Override
    protected void afterUpdate(Connection conn) throws SQLException {
        // Check if specialization changed
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT specialty FROM doctorspecialty WHERE doctorid = ?")) {
            stmt.setString(1, doctorid);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Doctor exists in specialists table
                if ("general".equalsIgnoreCase(specialization)) {
                    // Remove from specialists if changed to general
                    try (PreparedStatement deleteStmt = conn.prepareStatement(
                            "DELETE FROM doctorspecialty WHERE doctorid = ?")) {
                        deleteStmt.setString(1, doctorid);
                        deleteStmt.executeUpdate();
                    }
                } else if (!specialization.equals(rs.getString("specialty"))) {
                    // Update specialization if changed
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE doctorspecialty SET specialty = ? WHERE doctorid = ?")) {
                        updateStmt.setString(1, specialization);
                        updateStmt.setString(2, doctorid);
                        updateStmt.executeUpdate();
                    }
                }
            } else if (!"general".equalsIgnoreCase(specialization)) {
                // Add to specialists if not general
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO doctorspecialty (doctorid, specialty, experience) VALUES (?, ?, 0)")) {
                    insertStmt.setString(1, doctorid);
                    insertStmt.setString(2, specialization);
                    insertStmt.executeUpdate();
                }
            }
        }
    }

    @Override
    protected void beforeDelete(Connection conn) throws SQLException {
        // Remove from specialists table before deleting doctor
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM doctorspecialty WHERE doctorid = ?")) {
            stmt.setString(1, doctorid);
            stmt.executeUpdate();
        }
    }

    // Getters and Setters
    public String getDoctorid() { return doctorid; }
    public void setDoctorid(String doctorid) { this.doctorid = doctorid; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    @Override
    protected String getTableName() {
        return "doctor";
    }

    @Override
    protected Doctor mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new Doctor(
            rs.getString("doctorid"),
            rs.getString("firstname"),
            rs.getString("surname"),
            rs.getString("address"),
            rs.getString("email"),
            rs.getString("specialization")
        );
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, Doctor entity) throws SQLException {
        stmt.setString(1, entity.getDoctorid());
        stmt.setString(2, entity.getFirstname());
        stmt.setString(3, entity.getSurname());
        stmt.setString(4, entity.getAddress());
        stmt.setString(5, entity.getEmail());
        stmt.setString(6, entity.getSpecialization());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Doctor entity) throws SQLException {
        stmt.setString(1, entity.getFirstname());
        stmt.setString(2, entity.getSurname());
        stmt.setString(3, entity.getAddress());
        stmt.setString(4, entity.getEmail());
        stmt.setString(5, entity.getSpecialization());
        stmt.setString(6, entity.getDoctorid());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO doctor (doctorid, firstname, surname, address, email, specialization) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE doctor SET firstname=?, surname=?, address=?, email=?, specialization=? WHERE doctorid=?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM doctor WHERE doctorid=?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT * FROM doctor";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT * FROM doctor WHERE doctorid=?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String)id);
    }

    @Override
    public String toString() {
        return String.format("Doctor[ID=%s, Name=%s %s, Address=%s, Email=%s, Specialization=%s]", 
            doctorid, firstname, surname, address, email, specialization);
    }

    @Override
    protected Object getId() {
        return doctorid;
    }
} 