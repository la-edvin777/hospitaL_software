package models;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Patient extends BaseModel<Patient> {
    private String patientid;
    private String firstname;
    private String surname;
    private String postcode;
    private String address;
    private String phone;
    private String email;
    private String insuranceid;
    private String maindoctorid;
    
    // Display field for main doctor name
    private String maindoctorname;

    // Constructor
    public Patient(String patientid, String firstname, String surname, String postcode, 
                  String address, String phone, String email, String insuranceid) {
        this.patientid = patientid;
        this.firstname = firstname;
        this.surname = surname;
        this.postcode = postcode;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.insuranceid = insuranceid;
    }

    // Full constructor with main doctor
    public Patient(String patientid, String firstname, String surname, String postcode, 
                  String address, String phone, String email, String insuranceid, String maindoctorid) {
        this.patientid = patientid;
        this.firstname = firstname;
        this.surname = surname;
        this.postcode = postcode;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.insuranceid = insuranceid;
        this.maindoctorid = maindoctorid;
    }

    // Default constructor
    public Patient() {}

    // Getters and Setters
    public String getPatientid() { return patientid; }
    public void setPatientid(String patientid) { this.patientid = patientid; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }
    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInsuranceid() { return insuranceid; }
    public void setInsuranceid(String insuranceid) { this.insuranceid = insuranceid; }
    public String getMaindoctorid() { return maindoctorid; }
    public void setMaindoctorid(String maindoctorid) { this.maindoctorid = maindoctorid; }
    public String getMaindoctorname() { return maindoctorname; }
    public void setMaindoctorname(String maindoctorname) { this.maindoctorname = maindoctorname; }

    @Override
    protected String getTableName() {
        return "patient";
    }

    @Override
    protected Patient mapResultSetToEntity(ResultSet rs) throws SQLException {
        Patient patient = new Patient(
            rs.getString("patientid"),
            rs.getString("firstname"),
            rs.getString("surname"),
            rs.getString("postcode"),
            rs.getString("address"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("insuranceid"),
            rs.getString("maindoctorid")
        );
        
        // Set main doctor name if it exists in the result set
        try {
            patient.maindoctorname = rs.getString("maindoctorname");
        } catch (SQLException e) {
            patient.maindoctorname = null;
        }
        
        return patient;
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, Patient entity) throws SQLException {
        stmt.setString(1, entity.getPatientid());
        stmt.setString(2, entity.getFirstname());
        stmt.setString(3, entity.getSurname());
        stmt.setString(4, entity.getPostcode());
        stmt.setString(5, entity.getAddress());
        stmt.setString(6, entity.getPhone());
        stmt.setString(7, entity.getEmail());
        stmt.setString(8, entity.getInsuranceid());
        stmt.setString(9, entity.getMaindoctorid());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Patient entity) throws SQLException {
        stmt.setString(1, entity.getFirstname());
        stmt.setString(2, entity.getSurname());
        stmt.setString(3, entity.getPostcode());
        stmt.setString(4, entity.getAddress());
        stmt.setString(5, entity.getPhone());
        stmt.setString(6, entity.getEmail());
        stmt.setString(7, entity.getInsuranceid());
        stmt.setString(8, entity.getMaindoctorid());
        stmt.setString(9, entity.getPatientid());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO patient (patientid, firstname, surname, postcode, address, phone, email, insuranceid, maindoctorid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE patient SET firstname = ?, surname = ?, postcode = ?, address = ?, phone = ?, email = ?, insuranceid = ?, maindoctorid = ? WHERE patientid = ?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM patient WHERE patientid = ?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT p.*, CONCAT(d.firstname, ' ', d.surname) AS maindoctorname " +
               "FROM patient p " +
               "LEFT JOIN doctor d ON p.maindoctorid = d.doctorid " +
               "ORDER BY p.firstname, p.surname";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT p.*, CONCAT(d.firstname, ' ', d.surname) AS maindoctorname " +
               "FROM patient p " +
               "LEFT JOIN doctor d ON p.maindoctorid = d.doctorid " +
               "WHERE p.patientid = ?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String) id);
    }

    @Override
    public String toString() {
        return String.format("Patient[ID=%s, Name=%s %s, Address=%s, Postcode=%s, Phone=%s, Email=%s, Insurance=%s, Main Doctor=%s]", 
            patientid, firstname, surname, address, postcode, phone, email, insuranceid, maindoctorname);
    }

    @Override
    protected Object getId() {
        return patientid;
    }
} 