/**
 * Field metadata for database columns.
 */
package gui;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.swing.JComponent;
import javax.swing.JTextField;

public class FieldMetadata {
    private final Class<?> type;
    private final boolean isPrimaryKey;
    private final String foreignKeyTable;
    private final String foreignKeyColumn;
    private final String displayColumn;
    private final boolean isRequired;
    private final int maxLength;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    
    /**
     * Creates a new field metadata instance with the specified type.
     * Auto-generation is set to false by default.
     * 
     * @param type The Java type of the field
     */
    public FieldMetadata(Class<?> type) {
        this(type, false);
    }
    
    /**
     * Creates a new field metadata instance with the specified type and auto-generation setting.
     * 
     * @param type The Java type of the field
     * @param isPrimaryKey Whether the field is a primary key
     */
    public FieldMetadata(Class<?> type, boolean isPrimaryKey) {
        this(type, isPrimaryKey, null, null, null);
    }
    
    /**
     * Creates a new field metadata instance for a field that references another table.
     * Used for foreign key relationships and dropdown selections in the GUI.
     * 
     * @param type The Java type of the field
     * @param foreignKeyTable The name of the referenced table
     * @param foreignKeyColumn The primary key field in the referenced table
     * @param displayColumn The field to display in dropdowns/selections
     */
    public FieldMetadata(Class<?> type, String foreignKeyTable, String foreignKeyColumn, String displayColumn) {
        // Foreign keys are always required unless explicitly marked as optional
        this(type, false, foreignKeyTable, foreignKeyColumn, displayColumn, true, 0);
    }
    
    /**
     * Creates a new field metadata instance for a field that references another table with optional parameters.
     * 
     * @param type The Java type of the field
     * @param foreignKeyTable The name of the referenced table
     * @param foreignKeyColumn The primary key field in the referenced table
     * @param displayColumn The field to display in dropdowns/selections
     * @param isRequired Whether the field is required
     * @param maxLength Maximum length for the field (0 for no limit)
     */
    public FieldMetadata(Class<?> type, String foreignKeyTable, String foreignKeyColumn, String displayColumn, boolean isRequired, int maxLength) {
        this(type, false, foreignKeyTable, foreignKeyColumn, displayColumn, isRequired, maxLength);
    }
    
    /**
     * Creates a new field metadata instance for a field that references another table.
     * Used for foreign key relationships and dropdown selections in the GUI.
     * 
     * @param type The Java type of the field
     * @param isPrimaryKey Whether the field is a primary key
     * @param foreignKeyTable The name of the referenced table
     * @param foreignKeyColumn The primary key field in the referenced table
     * @param displayColumn The field to display in dropdowns/selections
     */
    public FieldMetadata(Class<?> type, boolean isPrimaryKey, String foreignKeyTable, String foreignKeyColumn, String displayColumn) {
        // Foreign keys are always required unless explicitly marked as optional
        this(type, isPrimaryKey, foreignKeyTable, foreignKeyColumn, displayColumn, true, 0);
    }
    
    /**
     * Creates a new field metadata instance with all parameters including max length.
     * 
     * @param type The Java type of the field
     * @param isPrimaryKey Whether the field is a primary key
     * @param foreignKeyTable The name of the referenced table
     * @param foreignKeyColumn The primary key field in the referenced table
     * @param displayColumn The field to display in dropdowns/selections
     * @param isRequired Whether the field is required
     * @param maxLength Maximum length for the field (0 for no limit)
     */
    public FieldMetadata(Class<?> type, boolean isPrimaryKey, String foreignKeyTable, String foreignKeyColumn, String displayColumn, boolean isRequired, int maxLength) {
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.foreignKeyTable = foreignKeyTable;
        this.foreignKeyColumn = foreignKeyColumn;
        this.displayColumn = displayColumn;
        this.isRequired = isRequired;
        this.maxLength = maxLength;
    }
    
    /**
     * Gets the Java type of the field.
     * @return The field's Class object
     */
    public Class<?> getType() {
        return type;
    }
    
    /**
     * Checks if the field is a primary key.
     * @return true if the field is a primary key, false otherwise
     */
    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }
    
    /**
     * Checks if the field has a relationship with another table.
     * @return true if the field references another table, false otherwise
     */
    public boolean hasRelation() {
        return foreignKeyTable != null;
    }
    
    /**
     * Gets the name of the related table.
     * @return The related table name or null if no relation exists
     */
    public String getForeignKeyTable() {
        return foreignKeyTable;
    }
    
    /**
     * Gets the key field name in the related table.
     * @return The related key field name or null if no relation exists
     */
    public String getForeignKeyColumn() {
        return foreignKeyColumn;
    }
    
    /**
     * Gets the display field name in the related table.
     * @return The related display field name or null if no relation exists
     */
    public String getDisplayColumn() {
        return displayColumn;
    }
    
    /**
     * Checks if the field is required.
     * @return true if the field is required, false otherwise
     */
    public boolean isRequired() {
        return isRequired;
    }
    
    /**
     * Gets the maximum length for the field.
     * @return The maximum length (0 for no limit)
     */
    public int getMaxLength() {
        return maxLength;
    }
    
    /**
     * Generate ID for table.
     * 
     * @param tableName Table name
     * @return Unique ID
     */
    public String generateId(String tableName) {
        switch (tableName.toLowerCase()) {
            case "doctor":
                return "DR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            case "patient":
                return "PT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            case "visit":
                return "VT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            case "prescription":
                return String.format("%010d", (long)(Math.random() * 10000000000L));
            case "doctorspecialty":
                return "DS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            case "patientinsurance":
                return "PI" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            case "insurance":
                return "IN" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            default:
                return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
    
    /**
     * Get related items for dropdowns.
     * 
     * @param conn Database connection
     * @return List of items
     * @throws SQLException if database error
     */
    public List<ComboBoxItem> getRelatedItems(Connection conn) throws SQLException {
        if (!hasRelation()) {
            return new ArrayList<>();
        }
        
        List<ComboBoxItem> items = new ArrayList<>();
        String sql;
        
        // Handle special cases where displayColumn contains a function
        if (displayColumn.toUpperCase().startsWith("CONCAT(")) {
            sql = String.format("SELECT %s, %s AS display_name FROM %s ORDER BY display_name", 
                foreignKeyColumn, displayColumn, foreignKeyTable);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ComboBoxItem(
                        rs.getString(foreignKeyColumn),
                        rs.getString("display_name")
                    ));
                }
            }
        } else if (displayColumn.contains("||")) {
            // Handle Oracle-style concatenation
            String modifiedDisplayColumn = displayColumn.replace("||", ", ");
            sql = String.format("SELECT %s, CONCAT(%s) AS display_name FROM %s ORDER BY display_name", 
                foreignKeyColumn, modifiedDisplayColumn.replace("'", "").replace(" ", ""), foreignKeyTable);
                
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ComboBoxItem(
                        rs.getString(foreignKeyColumn),
                        rs.getString("display_name")
                    ));
                }
            }
        } else {
            // Handle regular columns
            sql = String.format("SELECT %s, %s FROM %s ORDER BY %s", 
                foreignKeyColumn, displayColumn, foreignKeyTable, displayColumn);
                
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new ComboBoxItem(
                        rs.getString(foreignKeyColumn),
                        rs.getString(displayColumn)
                    ));
                }
            }
        }
        
        return items;
    }

    /**
     * Creates an appropriate input component for this field type.
     * 
     * @return A JComponent suitable for inputting this field's data
     */
    public JComponent createInputComponent() {
        if (type == Date.class || type == java.sql.Date.class) {
            return new DatePicker();
        }
        return new JTextField();
    }

    /**
     * Gets the value from a component, with proper type conversion.
     * 
     * @param component The input component
     * @return The string representation of the value
     */
    public String getValueFromComponent(JComponent component) {
        if (type == Date.class || type == java.sql.Date.class) {
            if (component instanceof DatePicker) {
                String dateStr = ((DatePicker) component).getDateString();
                if ((isRequired || hasRelation()) && (dateStr == null || dateStr.trim().isEmpty())) {
                    return null;
                }
                return dateStr;
            }
        } else if (component instanceof JTextField) {
            String text = ((JTextField) component).getText();
            if (text == null || text.trim().isEmpty()) {
                // Return null for required fields or foreign keys, empty string for optional fields
                return (isRequired || hasRelation()) ? null : "";
            }
            return text.trim();
        }
        return null;
    }

    /**
     * Sets a value in a component with proper formatting.
     * 
     * @param component The input component
     * @param value The value to set
     */
    public void setValueInComponent(JComponent component, String value) {
        if (type == Date.class || type == java.sql.Date.class) {
            if (component instanceof DatePicker) {
                ((DatePicker) component).setDateString(value);
            }
        } else if (component instanceof JTextField) {
            ((JTextField) component).setText(value != null ? value : "");
        }
    }
    
    /**
     * Validates a field value according to its type and constraints.
     * 
     * @param value The value to validate
     * @return A validation result with success status and error message
     */
    public ValidationResult validateValue(String value) {
        // Check required fields and foreign key constraints
        if (isRequired || hasRelation()) {
            if (value == null || value.trim().isEmpty()) {
                String fieldType = hasRelation() ? 
                    "Foreign key reference to " + foreignKeyTable : 
                    "This field";
                return new ValidationResult(false, fieldType + " is required");
            }
        }
        
        // Allow empty non-required fields (except foreign keys)
        if (!isRequired && !hasRelation() && (value == null || value.trim().isEmpty())) {
            return new ValidationResult(true, null);
        }
        
        // If we get here and value is null, it's an error
        if (value == null) {
            return new ValidationResult(false, "Invalid value provided");
        }
        
        value = value.trim();
        
        // Check max length
        if (maxLength > 0 && value.length() > maxLength) {
            return new ValidationResult(false, "Value too long (max " + maxLength + " characters)");
        }
        
        // Type-specific validation
        if (type == Date.class || type == java.sql.Date.class) {
            try {
                DATE_FORMAT.setLenient(false);
                DATE_FORMAT.parse(value);
                return new ValidationResult(true, null);
            } catch (ParseException e) {
                return new ValidationResult(false, "Invalid date format (use YYYY-MM-DD)");
            }
        } else if (type == Integer.class || type == int.class) {
            try {
                int intValue = Integer.parseInt(value);
                if (intValue < 0) {
                    return new ValidationResult(false, "Value must be positive");
                }
                return new ValidationResult(true, null);
            } catch (NumberFormatException e) {
                return new ValidationResult(false, "Must be a valid number");
            }
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validates if a foreign key value exists in the referenced table.
     * 
     * @param conn Database connection
     * @param value The foreign key value to validate
     * @return true if the value exists in the referenced table
     */
    public boolean validateForeignKeyValue(Connection conn, String value) {
        if (!hasRelation() || value == null || value.trim().isEmpty()) {
            return false;
        }

        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", 
            foreignKeyTable, foreignKeyColumn);
            
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, value.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
    
    /**
     * Inner class for validation results.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}