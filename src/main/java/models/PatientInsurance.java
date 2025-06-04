package models;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PatientInsurance extends BaseModel<PatientInsurance> {
    private String insuranceid;
    private String patientid;
    private Date startdate;
    private Date enddate;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

    // Default constructor
    public PatientInsurance() {}

    // Constructor
    public PatientInsurance(String insuranceid, String patientid, Date startdate, Date enddate) {
        this.insuranceid = insuranceid;
        this.patientid = patientid;
        this.startdate = startdate;
        this.enddate = enddate;
    }

    // Getters and Setters
    public String getInsuranceid() { return insuranceid; }
    public void setInsuranceid(String insuranceid) { this.insuranceid = insuranceid; }
    public String getPatientid() { return patientid; }
    public void setPatientid(String patientid) { this.patientid = patientid; }
    public Date getStartdate() { return startdate; }
    public void setStartdate(Date startdate) { this.startdate = startdate; }
    public Date getEnddate() { return enddate; }
    public void setEnddate(Date enddate) { this.enddate = enddate; }

    @Override
    protected String getTableName() {
        return "patientinsurance";
    }

    @Override
    protected PatientInsurance mapResultSetToEntity(ResultSet rs) throws SQLException {
        return new PatientInsurance(
            rs.getString("insuranceid"),
            rs.getString("patientid"),
            rs.getDate("startdate"),
            rs.getDate("enddate")
        );
    }

    @Override
    protected void setCreateStatement(PreparedStatement stmt, PatientInsurance entity) throws SQLException {
        stmt.setString(1, entity.getInsuranceid());
        stmt.setString(2, entity.getPatientid());
        stmt.setDate(3, entity.getStartdate());
        stmt.setDate(4, entity.getEnddate());
    }

    @Override
    protected void setUpdateStatement(PreparedStatement stmt, PatientInsurance entity) throws SQLException {
        stmt.setDate(1, entity.getStartdate());
        stmt.setDate(2, entity.getEnddate());
        stmt.setString(3, entity.getInsuranceid());
        stmt.setString(4, entity.getPatientid());
    }

    @Override
    protected String getCreateSQL() {
        return "INSERT INTO patientinsurance (insuranceid, patientid, startdate, enddate) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateSQL() {
        return "UPDATE patientinsurance SET startdate=?, enddate=? WHERE insuranceid=? AND patientid=?";
    }

    @Override
    protected String getDeleteSQL() {
        return "DELETE FROM patientinsurance WHERE insuranceid=?";
    }

    @Override
    protected String getSelectAllSQL() {
        return "SELECT * FROM patientinsurance";
    }

    @Override
    protected String getSelectByIdSQL() {
        return "SELECT * FROM patientinsurance WHERE insuranceid=?";
    }

    @Override
    protected void setIdParameter(PreparedStatement stmt, Object id) throws SQLException {
        stmt.setString(1, (String)id);
    }

    @Override
    public String toString() {
        String formattedStartDate = startdate != null ? DATE_FORMAT.format(startdate) : "N/A";
        String formattedEndDate = enddate != null ? DATE_FORMAT.format(enddate) : "N/A";
        return String.format("PatientInsurance{insuranceid='%s', patientid='%s', startdate='%s', enddate='%s'}", 
            insuranceid, patientid, formattedStartDate, formattedEndDate);
    }

    @Override
    protected Object getId() {
        return insuranceid;
    }
} 