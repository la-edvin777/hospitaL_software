/**
 * Utility class for initializing the Hospital Management System database.
 * Handles the creation of database tables, dropping existing tables,
 * and generating sample data for testing and demonstration purposes.
 */
package utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    /**
     * Initializes the database by dropping existing tables,
     * creating new tables, and populating them with sample data.
     * 
     * @param conn The database connection
     * @throws SQLException if there's an error during initialization
     */
    public static void initializeDatabase(Connection conn) throws SQLException {
        dropTables(conn);
        createTables(conn);
        DataGenerator.generateSampleData(conn);
    }

    /**
     * Drops all existing tables in the correct order to handle dependencies.
     * Tables are dropped in reverse order of their dependencies to avoid
     * foreign key constraint violations.
     * 
     * @param conn The database connection
     * @throws SQLException if there's an error dropping the tables
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
     * Creates all database tables with appropriate fields and constraints.
     * Tables are created in the correct order to satisfy foreign key dependencies.
     * 
     * @param conn The database connection
     * @throws SQLException if there's an error creating the tables
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
                    "insuranceid VARCHAR(8)" +
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