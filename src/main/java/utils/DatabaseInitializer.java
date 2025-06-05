/**
 * Database initialization for Hospital Management System.
 */
package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    /**
     * Initialize database with tables and sample data.
     * 
     * @param conn Database connection
     * @throws SQLException if error during initialization
     */
    public static void initializeDatabase(Connection conn) throws SQLException {
        dropTables(conn);
        createTables(conn);
        // Note: Sample data is now loaded via CSV files through CSVLoader
        // DataGenerator.generateSampleData(conn); // REMOVED - deprecated
    }

    /**
     * Initialize database with tables only (no sample data).
     * 
     * @param conn Database connection
     * @throws SQLException if error during initialization
     */
    public static void initializeDatabaseWithoutData(Connection conn) throws SQLException {
        dropTables(conn);
        createTables(conn);
    }

    /**
     * Drop all tables in correct order.
     * 
     * @param conn Database connection
     * @throws SQLException if error dropping tables
     */
    private static void dropTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Drop tables in reverse order of dependencies
            stmt.execute("DROP TABLE IF EXISTS patientinsurance");
            stmt.execute("DROP TABLE IF EXISTS prescription");
            stmt.execute("DROP TABLE IF EXISTS visit");
            stmt.execute("DROP TABLE IF EXISTS patient");
            stmt.execute("DROP TABLE IF EXISTS doctorspecialty");
            stmt.execute("DROP TABLE IF EXISTS doctor");
            stmt.execute("DROP TABLE IF EXISTS insurance");
            stmt.execute("DROP TABLE IF EXISTS drug");
        }
    }

    /**
     * Create all database tables with constraints.
     * 
     * @param conn Database connection
     * @throws SQLException if error creating tables
     */
    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Create Doctor table - Primary entity for medical staff
            stmt.execute("CREATE TABLE doctor (" +
                    "doctorid VARCHAR(8) PRIMARY KEY," +
                    "firstname VARCHAR(50) NOT NULL," +
                    "surname VARCHAR(50) NOT NULL," +
                    "address VARCHAR(200) NOT NULL," +
                    "email VARCHAR(100) NOT NULL," +
                    "specialization VARCHAR(50) NOT NULL" +
                    ")");

            // Create DoctorSpecialty table - Stores doctor specializations
            stmt.execute("CREATE TABLE doctorspecialty (" +
                    "doctorid VARCHAR(8) PRIMARY KEY," +
                    "specialty VARCHAR(50) NOT NULL," +
                    "experience INT NOT NULL," +
                    "FOREIGN KEY (doctorid) REFERENCES doctor(doctorid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE" +
                    ")");

            // Create Insurance table - Insurance provider information
            stmt.execute("CREATE TABLE insurance (" +
                    "insuranceid VARCHAR(8) PRIMARY KEY," +
                    "company VARCHAR(100) NOT NULL," +
                    "address VARCHAR(200) NOT NULL," +
                    "phone VARCHAR(20) NOT NULL" +
                    ")");

            // Create Drug table - Medication information
            stmt.execute("CREATE TABLE drug (" +
                    "drugid INT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "sideeffects VARCHAR(200)," +
                    "benefits VARCHAR(200)" +
                    ")");

            // Create Patient table - Primary entity for patients
            stmt.execute("CREATE TABLE patient (" +
                    "patientid VARCHAR(8) PRIMARY KEY," +
                    "firstname VARCHAR(50) NOT NULL," +
                    "surname VARCHAR(50) NOT NULL," +
                    "postcode VARCHAR(10) NOT NULL," +
                    "address VARCHAR(200) NOT NULL," +
                    "phone VARCHAR(20) NOT NULL," +
                    "email VARCHAR(100) NOT NULL," +
                    "insuranceid VARCHAR(8)," +
                    "maindoctorid VARCHAR(8)," +
                    "FOREIGN KEY (maindoctorid) REFERENCES doctor(doctorid)" +
                    " ON DELETE SET NULL" +
                    " ON UPDATE CASCADE" +
                    ")");

            // Create Visit table - Records patient visits to doctors
            stmt.execute("CREATE TABLE visit (" +
                    "visitid VARCHAR(8) PRIMARY KEY," +
                    "patientid VARCHAR(8) NOT NULL," +
                    "doctorid VARCHAR(8) NOT NULL," +
                    "dateofvisit DATE NOT NULL," +
                    "symptoms VARCHAR(200)," +
                    "diagnosis VARCHAR(100) NOT NULL," +
                    "FOREIGN KEY (patientid) REFERENCES patient(patientid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE," +
                    "FOREIGN KEY (doctorid) REFERENCES doctor(doctorid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE" +
                    ")");

            // Create Prescription table - Records medications prescribed during visits
            stmt.execute("CREATE TABLE prescription (" +
                    "prescriptionid VARCHAR(10) PRIMARY KEY," +
                    "dateprescribed DATE NOT NULL," +
                    "dosage VARCHAR(50) NOT NULL," +
                    "duration VARCHAR(50) NOT NULL," +
                    "comment VARCHAR(200)," +
                    "drugid INT NOT NULL," +
                    "doctorid VARCHAR(8) NOT NULL," +
                    "patientid VARCHAR(8) NOT NULL," +
                    "FOREIGN KEY (doctorid) REFERENCES doctor(doctorid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE," +
                    "FOREIGN KEY (patientid) REFERENCES patient(patientid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE" +
                    ")");

            // Create PatientInsurance table - Links patients with their insurance records
            stmt.execute("CREATE TABLE patientinsurance (" +
                    "insuranceid VARCHAR(8)," +
                    "patientid VARCHAR(8)," +
                    "startdate DATE NOT NULL," +
                    "enddate DATE NOT NULL," +
                    "PRIMARY KEY (insuranceid, patientid)," +
                    "FOREIGN KEY (patientid) REFERENCES patient(patientid)" +
                    " ON DELETE CASCADE" +
                    " ON UPDATE CASCADE" +
                    ")");
        }
    }
} 