package models;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class Visit extends BaseModel<Visit> {
    private String visitid;
    private String patientid;
    private String doctorid;
    private Date dateofvisit;
    private String symptoms;
    private String diagnosis;
    
    private String patientName;
    private String doctorName;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Default constructor
    public Visit() {}

    // Full constructor
    public Visit(String visitid, String patientid, String doctorid, Date dateofvisit, String symptoms, String diagnosis) {
        this.visitid = visitid;
        this.patientid = patientid;
        this.doctorid = doctorid;
        this.dateofvisit = dateofvisit;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
    }

    @Override
    protected String getTableName() {
        return "visit";
    }

    @Override
    protected Visit mapResultSetToEntity(ResultSet rs) throws SQLException {
        Visit visit = new Visit();
        visit.visitid = rs.getString("visitid");
        visit.patientid = rs.getString("patientid");
        visit.doctorid = rs.getString("doctorid");
        visit.dateofvisit = rs.getDate("dateofvisit");
        visit.symptoms = rs.getString("symptoms");
        visit.diagnosis = rs.getString("diagnosis");
        
        // Set patient and doctor names if they exist in the result set
        try {
            visit.patientName = rs.getString("patientName");
        } catch (SQLException e) {
            visit.patientName = null;
        }
        
        try {
            visit.doctorName = rs.getString("doctorName");
        } catch (SQLException e) {
            visit.doctorName = null;
        }
        
        return visit;
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, Visit visit) throws SQLException {
        stmt.setString(1, visit.visitid);
        stmt.setString(2, visit.patientid);
        stmt.setString(3, visit.doctorid);
        stmt.setDate(4, visit.dateofvisit);
        stmt.setString(5, visit.symptoms);
        stmt.setString(6, visit.diagnosis);
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, Visit visit) throws SQLException {
        stmt.setString(1, visit.patientid);
        stmt.setString(2, visit.doctorid);
        stmt.setDate(3, visit.dateofvisit);
        stmt.setString(4, visit.symptoms);
        stmt.setString(5, visit.diagnosis);
        stmt.setString(6, visit.visitid);
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO visit (visitid, patientid, doctorid, dateofvisit, symptoms, diagnosis) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE visit SET patientid=?, doctorid=?, dateofvisit=?, symptoms=?, diagnosis=? WHERE visitid=?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM visit WHERE visitid=?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT v.*, " +
               "CONCAT(p.firstname, ' ', p.surname) AS patientName, " +
               "CONCAT(d.firstname, ' ', d.surname) AS doctorName " +
               "FROM visit v " +
               "LEFT JOIN patient p ON v.patientid = p.patientid " +
               "LEFT JOIN doctor d ON v.doctorid = d.doctorid " +
               "ORDER BY v.dateofvisit DESC";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT v.*, " +
               "CONCAT(p.firstname, ' ', p.surname) AS patientName, " +
               "CONCAT(d.firstname, ' ', d.surname) AS doctorName " +
               "FROM visit v " +
               "LEFT JOIN patient p ON v.patientid = p.patientid " +
               "LEFT JOIN doctor d ON v.doctorid = d.doctorid " +
               "WHERE v.visitid = ?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String)id);
    }

    // Getters and Setters
    public String getVisitid() { return visitid; }
    public void setVisitid(String visitid) { this.visitid = visitid; }
    
    public String getPatientid() { return patientid; }
    public void setPatientid(String patientid) { this.patientid = patientid; }
    
    public String getDoctorid() { return doctorid; }
    public void setDoctorid(String doctorid) { this.doctorid = doctorid; }
    
    public Date getDateofvisit() { return dateofvisit; }
    public void setDateofvisit(Date dateofvisit) { this.dateofvisit = dateofvisit; }
    
    // Additional setter for java.util.Date compatibility
    public void setDateofvisit(java.util.Date dateofvisit) { 
        this.dateofvisit = dateofvisit != null ? new java.sql.Date(dateofvisit.getTime()) : null; 
    }
    
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    @Override
    public String toString() {
        String formattedDate = dateofvisit != null ? DATE_FORMAT.format(dateofvisit) : "N/A";
        return String.format("Visit{visitid='%s', patientid='%s', doctorid='%s', dateofvisit='%s', symptoms='%s', diagnosis='%s'}", 
                visitid, patientid, doctorid, formattedDate, symptoms, diagnosis);
    }

    @Override
    protected Object getId() {
        return visitid;
    }
}