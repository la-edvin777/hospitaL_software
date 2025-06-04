package models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extended Patient model that includes insurance company and main doctor information.
 * This class is used for display purposes in the Patient tab.
 */
public class PatientExtendedView extends Patient {
    private String insuranceCompany;
    private String mainDoctorName;
    
    /**
     * Default constructor
     */
    public PatientExtendedView() {
        super();
    }
    
    /**
     * Constructor using Patient data
     */
    public PatientExtendedView(Patient patient) {
        super(
            patient.getPatientid(),
            patient.getFirstname(),
            patient.getSurname(),
            patient.getPostcode(),
            patient.getAddress(),
            patient.getPhone(),
            patient.getEmail(),
            patient.getInsuranceid()
        );
    }
    
    /**
     * Get the insurance company name
     * @return The insurance company name
     */
    public String getInsuranceCompany() {
        return insuranceCompany;
    }
    
    /**
     * Set the insurance company name
     * @param insuranceCompany The insurance company name
     */
    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }
    
    /**
     * Get the main doctor name
     * @return The main doctor name
     */
    public String getMainDoctorName() {
        return mainDoctorName;
    }
    
    /**
     * Set the main doctor name
     * @param mainDoctorName The main doctor name
     */
    public void setMainDoctorName(String mainDoctorName) {
        this.mainDoctorName = mainDoctorName;
    }
    
    /**
     * Get all patients with extended information
     * @param connection Database connection
     * @return List of patients with extended information
     */
    public static List<PatientExtendedView> getAllWithExtendedInfo(Connection connection) throws SQLException {
        List<PatientExtendedView> result = new ArrayList<>();
        
        // First, get all basic patient data
        Patient patientModel = new Patient();
        List<Patient> patients = patientModel.selectAll(connection);
        
        // Convert to extended model
        for (Patient patient : patients) {
            PatientExtendedView extendedPatient = new PatientExtendedView(patient);
            result.add(extendedPatient);
        }
        
        // Get insurance company names
        String insuranceQuery = "SELECT i.insuranceid, i.company FROM insurance i";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(insuranceQuery)) {
            
            Map<String, String> insuranceMap = new HashMap<>();
            while (rs.next()) {
                insuranceMap.put(rs.getString("insuranceid"), rs.getString("company"));
            }
            
            // Update insurance company names
            for (PatientExtendedView patient : result) {
                String insuranceId = patient.getInsuranceid();
                if (insuranceId != null) {
                    patient.setInsuranceCompany(insuranceMap.getOrDefault(insuranceId, "Unknown"));
                } else {
                    patient.setInsuranceCompany("None");
                }
            }
        }
        
        // Get main doctor for each patient
        for (PatientExtendedView patient : result) {
            String mainDoctorQuery = 
                "SELECT d.doctorid, CONCAT(d.firstname, ' ', d.surname) as doctor_name, COUNT(*) as visit_count " +
                "FROM visit v " +
                "JOIN doctor d ON v.doctorid = d.doctorid " +
                "WHERE v.patientid = ? " +
                "GROUP BY d.doctorid, d.firstname, d.surname " +
                "ORDER BY visit_count DESC " +
                "LIMIT 1";
                
            try (PreparedStatement pstmt = connection.prepareStatement(mainDoctorQuery)) {
                pstmt.setString(1, patient.getPatientid());
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    patient.setMainDoctorName(rs.getString("doctor_name"));
                } else {
                    patient.setMainDoctorName("No visits");
                }
                
                rs.close();
            }
        }
        
        return result;
    }

    @Override
    public String toString() {
        return String.format("Patient[ID=%s, Name=%s %s, Insurance=%s, Main Doctor=%s]", 
            getPatientid(), getFirstname(), getSurname(), getInsuranceCompany(), getMainDoctorName());
    }
} 