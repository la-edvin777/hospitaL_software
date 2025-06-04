/**
 * Utility class for loading CSV data into the Hospital Management System database.
 * Handles the bulk import of data from CSV files into corresponding database tables.
 * Supports error handling and provides feedback on the loading process.
 */
package utils;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.ArrayList;
import java.sql.BatchUpdateException;

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
        // Configuration for each table's CSV structure
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

        // Construct and execute LOAD DATA command
        String sql = String.format(
            "LOAD DATA LOCAL INFILE '%s' " +
            "INTO TABLE %s " +
            "FIELDS TERMINATED BY ',' " +
            "ENCLOSED BY '\"' " +
            "LINES TERMINATED BY '\r\n' " +
            "IGNORE 1 LINES %s",
            absolutePath, tableName, columns);

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
            int autoIdOffset = needsAutoId ? 1 : 0;
            
            if (needsAutoId) {
                placeholders.append("?,");
            }
            
            for (int i = 0; i < columnNames.length; i++) {
                if (i > 0) placeholders.append(",");
                placeholders.append("?");
            }
            
            String modifiedColumnsSpec = columnsSpec;
            if (needsAutoId) {
                modifiedColumnsSpec = "(visitid," + columnsStr + ")";
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
                            throw new SQLException("Error parsing date: " + value, e);
                        }
                    }
                    
                    pstmt.setString(paramIndex, value);
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
     * 
     * @throws SQLException if there's an error loading the data
     */
    public void loadAllDataWithInsert() throws SQLException {
        // Configuration for each table's CSV structure
        String[] tableConfigs = {
            "Doctor,doctorID,firstname,surname,address,email,specialization",
            "Insurance,insuranceID,company,address,phone",
            "Patient,patientID,firstname,surname,postcode,address,phone,email,insuranceID",
            "Drug,drugID,name,sideeffects,benefits",
            "Visit,patientID,doctorID,dateofvisit,symptoms,diagnosis",
            "Prescription,prescriptionID,dateprescribed,dosage,duration,comment,drugID,doctorID,patientID",
            "PatientInsurance,insuranceID,patientID,startdate,enddate"
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
                throw new SQLException("Could not load " + tableName + " data into the database. Some records may contain invalid or duplicate information.", e);
            }
        }
    }
} 