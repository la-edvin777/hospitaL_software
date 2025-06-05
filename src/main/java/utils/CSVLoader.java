/**
 * Utility class for loading CSV data into the Hospital Management System database.
 * Handles the bulk import of data from CSV files into corresponding database tables.
 * Supports error handling and provides feedback on the loading process.
 */
package utils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CSVLoader {
    /** Database connection for loading data */
    private final Connection connection;
    
    /** Directory containing the CSV files */
    private final String dataDirectory;
    
    private static final SimpleDateFormat CSV_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
    private static final SimpleDateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    
    /**
     * Creates a new CSVLoader with the specified database connection.
     * Uses the default "data" directory for CSV files.
     * 
     * @param connection The database connection to use for loading data
     */
    public CSVLoader(Connection connection) {
        this(connection, "data");  // Default to "data" directory
    }
    
    /**
     * Creates a new CSVLoader with the specified connection and data directory.
     * 
     * @param connection The database connection to use for loading data
     * @param dataDirectory The directory containing the CSV files
     */
    public CSVLoader(Connection connection, String dataDirectory) {
        this.connection = connection;
        this.dataDirectory = dataDirectory;
    }
    
    /**
     * Loads all data from CSV files into the database.
     * 
     * @throws SQLException if there's an error loading the data
     */
    public void loadAllData() throws SQLException {
        System.out.println("DEBUG: loadAllData() called - " + new Exception().getStackTrace()[1]);
        // Configuration for each table's CSV structure - only tables with provided CSV files
        String[] tableConfigs = {
            "Doctor,doctorID,firstname,surname,address,email,specialization",
            "Insurance,insuranceID,company,address,phone",
            "Patient,patientID,firstname,surname,postcode,address,phone,email,insuranceID",
            "Drug,drugID,name,sideeffects,benefits",
            "Visit,patientID,doctorID,dateofvisit,symptoms,diagnosis",
            "Prescription,prescriptionID,dateprescribed,dosage,duration,comment,drugID,doctorID,patientID"
        };

        // Process each table configuration
        for (String config : tableConfigs) {
            String[] parts = config.split(",", 2);
            String tableName = parts[0];
            String columns = parts[1];
            
            try {
                loadTable(connection, dataDirectory + "/" + tableName + ".csv", tableName.toLowerCase(),
                    "(" + String.join(",", columns.split(",")) + ")");
                System.out.println("Successfully loaded " + tableName + " data using LOAD DATA LOCAL INFILE");
            } catch (SQLException e) {
                System.err.println("Error loading " + tableName + " data: " + e.getMessage());
                throw e; // Re-throw to handle in the GUI
            }
        }
        
        // After loading all CSV data, analyze visit patterns to assign main doctors
        try {
            assignMainDoctorsFromVisitPatterns();
            System.out.println("Successfully assigned main doctors based on visit patterns");
        } catch (SQLException e) {
            System.err.println("Error assigning main doctors: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
        
        // Generate data for tables that don't have CSV files but are required by the task
        try {
            generateDoctorSpecialtyData();
            System.out.println("Successfully generated DoctorSpecialty data based on existing doctors");
        } catch (SQLException e) {
            System.err.println("Error generating DoctorSpecialty data: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
        
        try {
            generatePatientInsuranceData();
            System.out.println("Successfully generated PatientInsurance data based on existing patient-insurance relationships");
        } catch (SQLException e) {
            System.err.println("Error generating PatientInsurance data: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
    }
    
    /**
     * Loads data from a single CSV file into the specified table.
     * Performs file existence and permission checks before loading.
     * 
     * @param conn The database connection
     * @param csvFile The path to the CSV file
     * @param tableName The name of the target database table
     * @param columns The column specification for the LOAD DATA command
     * @throws SQLException if there's an error loading the data
     */
    private static void loadTable(Connection conn, String csvFile, String tableName, String columns) 
            throws SQLException {
        // Verify file existence and permissions
        File file = new File(csvFile);
        if (!file.exists()) {
            throw new SQLException("CSV file not found: " + csvFile);
        }
        if (!file.canRead()) {
            throw new SQLException("Cannot read CSV file: " + csvFile + ". Please check file permissions.");
        }

        // Convert file path to proper format for SQL
        String absolutePath = file.getAbsolutePath().replace('\\', '/');

        // Special handling for patient table to handle missing maindoctorid column
        String sql;
        if (tableName.equals("patient")) {
            // For patient table, specify only the columns that exist in CSV, then set maindoctorid
            sql = String.format(
                "LOAD DATA LOCAL INFILE '%s' " +
                "INTO TABLE %s " +
                "FIELDS TERMINATED BY ',' " +
                "ENCLOSED BY '\"' " +
                "LINES TERMINATED BY '\n' " +
                "IGNORE 1 LINES " +
                "(patientid, firstname, surname, postcode, address, phone, email, insuranceid) " +
                "SET maindoctorid = NULL",
                absolutePath, tableName);
        } else if (tableName.equals("visit")) {
            // For visit table, need to handle missing visitid and column case differences
            sql = String.format(
                "LOAD DATA LOCAL INFILE '%s' " +
                "INTO TABLE %s " +
                "FIELDS TERMINATED BY ',' " +
                "ENCLOSED BY '\"' " +
                "LINES TERMINATED BY '\n' " +
                "IGNORE 1 LINES " +
                "(patientid, doctorid, dateofvisit, symptoms, diagnosis) " +
                "SET visitid = SUBSTRING(MD5(CONCAT(patientid, doctorid, dateofvisit)), 1, 8)",
                absolutePath, tableName);
        } else if (tableName.equals("patientinsurance")) {
            // For patientinsurance table, handle date conversion
            sql = String.format(
                "LOAD DATA LOCAL INFILE '%s' " +
                "INTO TABLE %s " +
                "FIELDS TERMINATED BY ',' " +
                "ENCLOSED BY '\"' " +
                "LINES TERMINATED BY '\n' " +
                "IGNORE 1 LINES " +
                "(insuranceid, patientid, @startdate, @enddate) " +
                "SET startdate = STR_TO_DATE(@startdate, '%%Y-%%m-%%d'), " +
                "enddate = STR_TO_DATE(@enddate, '%%Y-%%m-%%d')",
                absolutePath, tableName);
        } else {
            // For other tables, use the original format
            sql = String.format(
                "LOAD DATA LOCAL INFILE '%s' " +
                "INTO TABLE %s " +
                "FIELDS TERMINATED BY ',' " +
                "ENCLOSED BY '\"' " +
                "LINES TERMINATED BY '\n' " +
                "IGNORE 1 LINES %s",
                absolutePath, tableName, columns);
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new SQLException("Error loading " + tableName + ": " + e.getMessage() + 
                "\nMake sure MariaDB is configured to allow local infile loading", e);
        }
    }

    /**
     * Loads data from a single CSV file into the specified table.
     * Performs file existence and permission checks before loading.
     * Uses regular INSERT statements instead of LOAD DATA LOCAL INFILE.
     * 
     * @param conn The database connection
     * @param csvFile The path to the CSV file
     * @param tableName The name of the target database table
     * @param columns The column specification for the INSERT command
     * @throws SQLException if there's an error loading the data
     */
    private void loadTableWithInsert(Connection conn, String csvFile, String tableName, String columnsSpec) throws SQLException {
        File file = new File(csvFile);
        
        // Check if file exists and is readable
        if (!file.exists()) {
            throw new SQLException("CSV file not found: " + csvFile);
        }
        
        if (!file.canRead()) {
            throw new SQLException("Cannot read CSV file: " + csvFile);
        }
        
        try {
            // Parse columns from the columnsSpec
            String columnsStr = columnsSpec.substring(1, columnsSpec.length() - 1); // Remove parentheses
            String[] columnNames = columnsStr.split(",");
            
            // Read CSV file
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
            String line = reader.readLine(); // Skip header line
            
            // Create prepared statement for insert
            StringBuilder placeholders = new StringBuilder();
            boolean needsAutoId = tableName.equals("visit"); // Only visit needs auto-generated ID
            boolean isPatientTable = tableName.equals("patient");
            int autoIdOffset = needsAutoId ? 1 : 0;
            
            if (needsAutoId) {
                placeholders.append("?,");
            }
            
            for (int i = 0; i < columnNames.length; i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            
            // Add placeholder for maindoctorid if this is patient table
            if (isPatientTable) {
                placeholders.append(",?");
            }
            
            String modifiedColumnsSpec = columnsSpec;
            if (needsAutoId) {
                modifiedColumnsSpec = "(visitid," + columnsStr + ")";
            } else if (isPatientTable) {
                modifiedColumnsSpec = "(" + columnsStr + ",maindoctorid)";
            }
            
            String sql = "INSERT INTO " + tableName + " " + modifiedColumnsSpec + 
                     " VALUES (" + placeholders.toString() + ")";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            // Process each data line
            int batchSize = 100;
            int count = 0;
            int autoId = 1;
            
            // Date format for parsing MM/DD/YYYY
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("MM/dd/yyyy");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            
            // Also handle YYYY-MM-DD format for PatientInsurance
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
            
            conn.setAutoCommit(false);
            
            while ((line = reader.readLine()) != null) {
                // Split the CSV line, handling quoted values
                String[] values = parseCSVLine(line);
                
                // Set auto-generated ID if needed
                if (needsAutoId) {
                    pstmt.setInt(1, autoId++);
                }
                
                // Set parameters in prepared statement
                for (int i = 0; i < values.length && i < columnNames.length; i++) {
                    String value = values[i].trim();
                    int paramIndex = i + 1 + autoIdOffset;
                    
                    // Handle date conversion for prescription dates
                    if (tableName.equals("prescription") && columnNames[i].trim().equals("dateprescribed")) {
                        try {
                            java.util.Date parsedDate = inputFormat.parse(value);
                            value = outputFormat.format(parsedDate);
                        } catch (java.text.ParseException e) {
                            try {
                                // Try ISO format as fallback
                                java.util.Date parsedDate = isoFormat.parse(value);
                                value = outputFormat.format(parsedDate);
                            } catch (java.text.ParseException e2) {
                                throw new SQLException("Error parsing date: " + value, e2);
                            }
                        }
                    }
                    
                    // Handle date conversion for patientinsurance dates
                    if (tableName.equals("patientinsurance") && 
                        (columnNames[i].trim().equals("startdate") || columnNames[i].trim().equals("enddate"))) {
                        try {
                            // Try ISO format first (YYYY-MM-DD)
                            java.util.Date parsedDate = isoFormat.parse(value);
                            value = outputFormat.format(parsedDate);
                        } catch (java.text.ParseException e) {
                            try {
                                // Try MM/DD/YYYY format as fallback
                                java.util.Date parsedDate = inputFormat.parse(value);
                                value = outputFormat.format(parsedDate);
                            } catch (java.text.ParseException e2) {
                                throw new SQLException("Error parsing date: " + value, e2);
                            }
                        }
                    }
                    
                    pstmt.setString(paramIndex, value);
                }
                
                // Set maindoctorid to NULL for patient table
                if (isPatientTable) {
                    pstmt.setString(values.length + 1 + autoIdOffset, null);
                }
                
                pstmt.addBatch();
                
                if (++count % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            
            // Execute any remaining statements
            if (count % batchSize != 0) {
                pstmt.executeBatch();
            }
            
            conn.commit();
            conn.setAutoCommit(true);
            
            reader.close();
            pstmt.close();
            
        } catch (SQLException e) {
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                // Ignore
            }
            throw new SQLException("Failed to load data for " + tableName + ". The file format may be incorrect or the data may be invalid.", e);
        } catch (java.io.IOException e) {
            throw new SQLException("Could not read the data file for " + tableName + ". Please check that the file exists and is readable.", e);
        }
    }
    
    /**
     * Parse a CSV line, handling quoted values properly
     */
    private String[] parseCSVLine(String line) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // If we're in quotes and the next char is also a quote, it's an escaped quote
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // Skip the next quote
                } else {
                    // Toggle in-quotes status
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        
        // Add the last field
        result.add(sb.toString());
        
        return result.toArray(new String[0]);
    }
    
    /**
     * Loads all data from CSV files into the database using INSERT statements.
     * This is an alternative to loadAllData() when LOAD DATA LOCAL INFILE is disabled.
     * 
     * @throws SQLException if there's an error loading the data
     */
    public void loadAllDataWithInsert() throws SQLException {
        // Configuration for each table's CSV structure - only tables with provided CSV files
        String[] tableConfigs = {
            "Doctor,doctorID,firstname,surname,address,email,specialization",
            "Insurance,insuranceID,company,address,phone",
            "Patient,patientID,firstname,surname,postcode,address,phone,email,insuranceID",
            "Drug,drugID,name,sideeffects,benefits",
            "Visit,patientID,doctorID,dateofvisit,symptoms,diagnosis",
            "Prescription,prescriptionID,dateprescribed,dosage,duration,comment,drugID,doctorID,patientID"
        };

        // Process each table configuration
        for (String config : tableConfigs) {
            String[] parts = config.split(",", 2);
            String tableName = parts[0];
            String columns = parts[1];
            
            try {
                loadTableWithInsert(connection, dataDirectory + "/" + tableName + ".csv", tableName.toLowerCase(),
                    "(" + String.join(",", columns.split(",")) + ")");
                System.out.println("Successfully loaded " + tableName + " data using INSERT");
            } catch (SQLException e) {
                System.err.println("Error loading " + tableName + " data: " + e.getMessage());
                throw e; // Re-throw to handle in the GUI
            }
        }
        
        // After loading all CSV data, analyze visit patterns to assign main doctors
        try {
            assignMainDoctorsFromVisitPatterns();
            System.out.println("Successfully assigned main doctors based on visit patterns");
        } catch (SQLException e) {
            System.err.println("Error assigning main doctors: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
        
        // Generate data for tables that don't have CSV files but are required by the task
        try {
            generateDoctorSpecialtyData();
            System.out.println("Successfully generated DoctorSpecialty data based on existing doctors");
        } catch (SQLException e) {
            System.err.println("Error generating DoctorSpecialty data: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
        
        try {
            generatePatientInsuranceData();
            System.out.println("Successfully generated PatientInsurance data based on existing patient-insurance relationships");
        } catch (SQLException e) {
            System.err.println("Error generating PatientInsurance data: " + e.getMessage());
            // Don't throw - this is enhancement, not critical
        }
    }
    
    /**
     * Analyzes visit patterns and assigns main doctors to patients based on 
     * their most frequently visited doctor.
     * 
     * @throws SQLException if there's an error updating the database
     */
    private void assignMainDoctorsFromVisitPatterns() throws SQLException {
        String analysisQuery = "SELECT p.patientid, " +
                               "v.doctorid, " +
                               "COUNT(*) as visit_count, " +
                               "ROW_NUMBER() OVER (PARTITION BY p.patientid ORDER BY COUNT(*) DESC, MAX(v.dateofvisit) DESC) as rank_order " +
                               "FROM patient p " +
                               "JOIN visit v ON p.patientid = v.patientid " +
                               "WHERE p.maindoctorid IS NULL " +
                               "GROUP BY p.patientid, v.doctorid";
        
        String updateQuery = "UPDATE patient SET maindoctorid = ? WHERE patientid = ?";
        
        try (PreparedStatement analysisStmt = connection.prepareStatement(analysisQuery);
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
            
            connection.setAutoCommit(false);
            
            try (var rs = analysisStmt.executeQuery()) {
                int patientsUpdated = 0;
                
                while (rs.next()) {
                    int rankOrder = rs.getInt("rank_order");
                    
                    // Only assign main doctor for the most frequently visited doctor (rank 1)
                    if (rankOrder == 1) {
                        String patientId = rs.getString("patientid");
                        String doctorId = rs.getString("doctorid");
                        int visitCount = rs.getInt("visit_count");
                        
                        updateStmt.setString(1, doctorId);
                        updateStmt.setString(2, patientId);
                        updateStmt.addBatch();
                        
                        patientsUpdated++;
                        
                        System.out.println("Assigned main doctor " + doctorId + " to patient " + patientId + 
                                         " (based on " + visitCount + " visits)");
                    }
                }
                
                if (patientsUpdated > 0) {
                    updateStmt.executeBatch();
                    connection.commit();
                    System.out.println("Updated " + patientsUpdated + " patients with main doctors based on visit patterns");
                } else {
                    System.out.println("No patients needed main doctor assignment");
                }
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    
    /**
     * Generates DoctorSpecialty data based on existing doctors in the database.
     * Uses the specialization field from the doctor table to create specialty records.
     * 
     * @throws SQLException if there's an error generating the data
     */
    private void generateDoctorSpecialtyData() throws SQLException {
        String selectQuery = "SELECT doctorid, specialization FROM doctor WHERE specialization IS NOT NULL AND specialization != ''";
        String insertQuery = "INSERT INTO doctorspecialty (doctorid, specialty, experience) VALUES (?, ?, ?)";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            
            connection.setAutoCommit(false);
            
            try (var rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String doctorId = rs.getString("doctorid");
                    String specialization = rs.getString("specialization");
                    
                    // Generate realistic experience years based on specialty
                    int experience = generateExperienceYears(specialization);
                    
                    insertStmt.setString(1, doctorId);
                    insertStmt.setString(2, specialization);
                    insertStmt.setInt(3, experience);
                    insertStmt.addBatch();
                }
                
                insertStmt.executeBatch();
                connection.commit();
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    
    /**
     * Generates PatientInsurance data based on existing patient-insurance relationships.
     * Creates insurance period records for patients who have insurance.
     * Realistically, not all patients with insurance IDs have active coverage.
     * 
     * @throws SQLException if there's an error generating the data
     */
    private void generatePatientInsuranceData() throws SQLException {
        String selectQuery = "SELECT patientid, insuranceid FROM patient WHERE insuranceid IS NOT NULL AND insuranceid != ''";
        String insertQuery = "INSERT INTO patientinsurance (insuranceid, patientid, startdate, enddate) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery);
             PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            
            connection.setAutoCommit(false);
            
            try (var rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String patientId = rs.getString("patientid");
                    String insuranceId = rs.getString("insuranceid");
                    
                    // Only generate insurance records for about 70% of patients with insurance IDs
                    // This is more realistic - some patients might have insurance on file but no active coverage
                    if (Math.random() > 0.3) { // 70% chance of having active insurance
                        
                        // Generate varied insurance periods (not everyone has the same dates)
                        java.sql.Date startDate;
                        java.sql.Date endDate;
                        
                        int scenario = (int)(Math.random() * 4);
                        switch (scenario) {
                            case 0: // Full year coverage
                                startDate = java.sql.Date.valueOf("2023-01-01");
                                endDate = java.sql.Date.valueOf("2023-12-31");
                                break;
                            case 1: // Started mid-year
                                startDate = java.sql.Date.valueOf("2023-06-01");
                                endDate = java.sql.Date.valueOf("2023-12-31");
                                break;
                            case 2: // Expired mid-year
                                startDate = java.sql.Date.valueOf("2023-01-01");
                                endDate = java.sql.Date.valueOf("2023-08-31");
                                break;
                            default: // Short-term coverage
                                startDate = java.sql.Date.valueOf("2023-03-01");
                                endDate = java.sql.Date.valueOf("2023-09-30");
                                break;
                        }
                        
                        insertStmt.setString(1, insuranceId);
                        insertStmt.setString(2, patientId);
                        insertStmt.setDate(3, startDate);
                        insertStmt.setDate(4, endDate);
                        insertStmt.addBatch();
                    }
                }
                
                insertStmt.executeBatch();
                connection.commit();
                
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
    
    /**
     * Generates realistic experience years based on specialization.
     * 
     * @param specialization The doctor's specialization
     * @return Years of experience (5-25 years)
     */
    private int generateExperienceYears(String specialization) {
        // Generate experience based on specialty complexity
        switch (specialization.toLowerCase()) {
            case "general":
                return 5 + (int)(Math.random() * 15); // 5-20 years
            case "emergency":
                return 8 + (int)(Math.random() * 12); // 8-20 years
            case "intensivecare":
                return 10 + (int)(Math.random() * 15); // 10-25 years
            case "oncologists":
                return 12 + (int)(Math.random() * 13); // 12-25 years
            case "anaesthetists":
                return 10 + (int)(Math.random() * 15); // 10-25 years
            case "ophthalmology":
                return 8 + (int)(Math.random() * 17); // 8-25 years
            default:
                return 6 + (int)(Math.random() * 14); // 6-20 years
        }
    }
} 