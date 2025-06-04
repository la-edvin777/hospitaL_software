package utils;

import java.sql.*;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public class DataGenerator {
    private static final Random random = new Random();
    private static final String[] FIRST_NAMES = {
        "John", "Emma", "Michael", "Sarah", "James",
        "Lisa", "David", "Jennifer", "Robert", "Mary"
    };
    private static final String[] LAST_NAMES = {
        "Smith", "Wilson", "Brown", "Davis", "Miller",
        "Anderson", "Taylor", "Thomas", "Martin", "Johnson"
    };
    private static final String[] POSTCODES = {
        "SW1A 1AA", "E1 6AN", "M1 1AE", "B1 1HQ", "G1 1XW",
        "L1 8JQ", "NE1 7RU", "CF10 1EP", "BS1 1SZ", "EH1 1YZ"
    };
    private static final String[] ADDRESSES = {
        "10 Downing Street", "221B Baker Street", "1 London Bridge",
        "15 Oxford Street", "25 High Street", "8 Church Lane",
        "45 Queen's Road", "12 King's Way", "33 Victoria Avenue",
        "67 Albert Road"
    };
    private static final String[] SPECIALTIES = {
        "Cardiology", "Dermatology", "Neurology", "Pediatrics", "Orthopedics",
        "Oncology", "Psychiatry", "Radiology", "Surgery", "Internal Medicine"
    };
    private static final String[] MEDICATIONS = {
        "Aspirin", "Ibuprofen", "Amoxicillin", "Lisinopril", "Metformin",
        "Omeprazole", "Simvastatin", "Levothyroxine", "Gabapentin", "Sertraline"
    };
    private static final String[] DIAGNOSES = {
        "Common Cold", "Hypertension", "Type 2 Diabetes", "Anxiety", "Depression",
        "Arthritis", "Asthma", "Migraine", "Allergies", "Back Pain"
    };
    private static final String[] INSURANCE_PROVIDERS = {
        "Blue Cross", "Aetna", "UnitedHealth", "Cigna", "Humana",
        "Kaiser", "Anthem", "MetLife", "Prudential", "State Farm"
    };

    public static void generateSampleData(Connection conn) throws SQLException {
        generateDoctors(conn);
        generatePatients(conn);
        generateDoctorSpecialties(conn);
        generateVisits(conn);
        generatePrescriptions(conn);
        generatePatientInsurance(conn);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static void generateDoctors(Connection conn) throws SQLException {
        String sql = "INSERT INTO doctor (doctorid, firstname, surname, address, email, specialization) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < 10; i++) {
                String firstName = FIRST_NAMES[i];
                String lastName = LAST_NAMES[i];
                stmt.setString(1, generateId());
                stmt.setString(2, firstName);
                stmt.setString(3, lastName);
                stmt.setString(4, ADDRESSES[random.nextInt(ADDRESSES.length)]);
                stmt.setString(5, firstName.toLowerCase() + "." + lastName.toLowerCase() + "@hospital.com");
                stmt.setString(6, SPECIALTIES[i]);
                stmt.executeUpdate();
            }
        }
    }

    private static void generatePatients(Connection conn) throws SQLException {
        String sql = "INSERT INTO patient (patientid, firstname, surname, postcode, address, phone, email, insuranceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < 20; i++) {
                String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                stmt.setString(1, generateId());
                stmt.setString(2, firstName);
                stmt.setString(3, lastName);
                stmt.setString(4, POSTCODES[random.nextInt(POSTCODES.length)]);
                stmt.setString(5, ADDRESSES[random.nextInt(ADDRESSES.length)]);
                stmt.setString(6, String.format("+44%d", 7000000000L + random.nextInt(999999999)));
                stmt.setString(7, firstName.toLowerCase() + "." + lastName.toLowerCase() + "@email.com");
                stmt.setString(8, null); // Will be updated when generating insurance
                stmt.executeUpdate();
            }
        }
    }

    private static void generateDoctorSpecialties(Connection conn) throws SQLException {
        // Get all doctor IDs
        ResultSet rs = conn.createStatement().executeQuery("SELECT doctorid FROM doctor");
        String[] doctorIds = new String[10];
        
        int i = 0;
        while (rs.next() && i < 10) {
            doctorIds[i++] = rs.getString("doctorid");
        }

        String sql = "INSERT INTO doctorspecialty (doctorid, specialty, experience) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String doctorid : doctorIds) {
                stmt.setString(1, doctorid);
                stmt.setString(2, SPECIALTIES[random.nextInt(SPECIALTIES.length)]);
                stmt.setInt(3, 1 + random.nextInt(20)); // 1-20 years experience
                stmt.executeUpdate();
            }
        }
    }

    private static void generateVisits(Connection conn) throws SQLException {
        // Get all patient and doctor IDs
        ResultSet rs1 = conn.createStatement().executeQuery("SELECT patientid FROM patient");
        ResultSet rs2 = conn.createStatement().executeQuery("SELECT doctorid FROM doctor");
        
        String[] patientIds = new String[20];
        String[] doctorIds = new String[10];
        
        int i = 0;
        while (rs1.next() && i < 20) {
            patientIds[i++] = rs1.getString("patientid");
        }
        
        i = 0;
        while (rs2.next() && i < 10) {
            doctorIds[i++] = rs2.getString("doctorid");
        }

        String sql = "INSERT INTO visit (visitid, patientid, doctorid, dateofvisit, symptoms, diagnosis) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int j = 0; j < 30; j++) { // Generate 30 visits
                stmt.setString(1, generateId());
                stmt.setString(2, patientIds[random.nextInt(patientIds.length)]);
                stmt.setString(3, doctorIds[random.nextInt(doctorIds.length)]);
                stmt.setDate(4, Date.valueOf(LocalDate.now().minusDays(random.nextInt(365)))); // Last year
                stmt.setString(5, "Patient reported " + DIAGNOSES[random.nextInt(DIAGNOSES.length)].toLowerCase() + " symptoms");
                stmt.setString(6, DIAGNOSES[random.nextInt(DIAGNOSES.length)]);
                stmt.executeUpdate();
            }
        }
    }

    private static void generatePrescriptions(Connection conn) throws SQLException {
        // Get all patient and doctor IDs
        ResultSet rs1 = conn.createStatement().executeQuery("SELECT patientid FROM patient");
        ResultSet rs2 = conn.createStatement().executeQuery("SELECT doctorid FROM doctor");
        
        String[] patientIds = new String[20];
        String[] doctorIds = new String[10];
        
        int i = 0;
        while (rs1.next() && i < 20) {
            patientIds[i++] = rs1.getString("patientid");
        }
        
        i = 0;
        while (rs2.next() && i < 10) {
            doctorIds[i++] = rs2.getString("doctorid");
        }

        String sql = "INSERT INTO prescription (prescriptionid, dateprescribed, dosage, duration, comment, drugid, doctorid, patientid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int j = 0; j < 30; j++) { // Generate 30 prescriptions
                if (random.nextBoolean()) { // 50% chance of prescription
                    stmt.setString(1, generateId());
                    stmt.setDate(2, Date.valueOf(LocalDate.now().minusDays(random.nextInt(365)))); // Last year
                    stmt.setString(3, (random.nextInt(3) + 1) + " times daily");
                    stmt.setString(4, (random.nextInt(14) + 1) + " days");
                    stmt.setString(5, "Prescribed for " + DIAGNOSES[random.nextInt(DIAGNOSES.length)]);
                    stmt.setInt(6, random.nextInt(100000) + 1); // Random drug ID
                    stmt.setString(7, doctorIds[random.nextInt(doctorIds.length)]);
                    stmt.setString(8, patientIds[random.nextInt(patientIds.length)]);
                    stmt.executeUpdate();
                }
            }
        }
    }

    private static void generatePatientInsurance(Connection conn) throws SQLException {
        ResultSet rs = conn.createStatement().executeQuery("SELECT patientid FROM patient");
        String[] patientIds = new String[20];
        
        int i = 0;
        while (rs.next() && i < 20) {
            patientIds[i++] = rs.getString("patientid");
        }

        String sql = "INSERT INTO patientinsurance (insuranceid, patientid, startdate, enddate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String patientId : patientIds) {
                String insuranceId = generateId();
                stmt.setString(1, insuranceId);
                stmt.setString(2, patientId);
                stmt.setDate(3, Date.valueOf(LocalDate.now()));
                stmt.setDate(4, Date.valueOf(LocalDate.now().plusYears(1)));
                stmt.executeUpdate();
                
                try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE patient SET insuranceid = ? WHERE patientid = ?")) {
                    updateStmt.setString(1, insuranceId);
                    updateStmt.setString(2, patientId);
                    updateStmt.executeUpdate();
                }
            }
        }
    }
} 