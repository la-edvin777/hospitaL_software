/**
 * Represents metadata for database fields in the Hospital Management System.
 * This class manages field properties such as type, auto-generation, and relationships
 * between different database tables. It's used primarily for GUI form generation
 * and data validation.
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
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.MaskFormatter;

public class FieldMetadata {
    private final Class<?> type;
    private final boolean isPrimaryKey;
    private final String foreignKeyTable;
    private final String foreignKeyColumn;
    private final String displayColumn;
    private final boolean isRequired;
    
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
        this(type, false, foreignKeyTable, foreignKeyColumn, displayColumn);
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
        this.type = type;
        this.isPrimaryKey = isPrimaryKey;
        this.foreignKeyTable = foreignKeyTable;
        this.foreignKeyColumn = foreignKeyColumn;
        this.displayColumn = displayColumn;
        this.isRequired = isPrimaryKey;
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
     * Generates a unique ID for auto-generated fields.
     * Uses table-specific patterns for ID generation.
     * 
     * @param tableName The name of the table for which to generate an ID
     * @return A new unique identifier string
     */
    public String generateId(String tableName) {
        switch (tableName.toLowerCase()) {
            case "doctor":
                return "DR" + UUID.randomUUID().toString().substring(0, 6);
            case "patient":
                return "PT" + UUID.randomUUID().toString().substring(0, 6);
            case "visit":
                return "VT" + UUID.randomUUID().toString().substring(0, 6);
            case "prescription":
                return String.format("%010d", (long)(Math.random() * 10000000000L));
            case "doctorspecialty":
                return "DS" + UUID.randomUUID().toString().substring(0, 6);
            case "patientinsurance":
                return "PI" + UUID.randomUUID().toString().substring(0, 6);
            default:
                return UUID.randomUUID().toString().substring(0, 8);
        }
    }
    
    /**
     * Retrieves related items from the database for populating dropdowns and selections.
     * 
     * @param conn The database connection
     * @return List of ComboBoxItem objects containing key-value pairs
     * @throws SQLException if a database access error occurs
     */
    public List<ComboBoxItem> getRelatedItems(Connection conn) throws SQLException {
        if (!hasRelation()) {
            return new ArrayList<>();
        }
        
        List<ComboBoxItem> items = new ArrayList<>();
        String sql;
        
        // Handle special cases where displayColumn contains a function
        if (displayColumn.toUpperCase().startsWith("CONCAT(")) {
            sql = String.format("SELECT %s, %s AS display_name FROM %s", 
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
        } else {
            // Handle regular columns
            sql = String.format("SELECT %s, %s FROM %s", 
                foreignKeyColumn, displayColumn, foreignKeyTable);
                
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

    public JComponent createInputComponent() {
        if (type == Date.class) {
            try {
                MaskFormatter formatter = new MaskFormatter("####-##-##");
                formatter.setPlaceholderCharacter('_');
                JFormattedTextField dateField = new JFormattedTextField(formatter);
                dateField.setToolTipText("Enter date in YYYY-MM-DD format");
                return dateField;
            } catch (ParseException e) {
                e.printStackTrace();
                return new JTextField(); // Fallback to regular text field
            }
        }
        return new JTextField();
    }

    public String getValueFromComponent(JComponent component) {
        if (type == Date.class && component instanceof JFormattedTextField) {
            String text = ((JFormattedTextField) component).getText();
            if (text != null && text.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return text;
            }
            return null;
        } else if (component instanceof JTextField) {
            return ((JTextField) component).getText();
        }
        return null;
    }

    public void setValueInComponent(JComponent component, String value) {
        if (component instanceof JTextField) {
            ((JTextField) component).setText(value);
        }
    }
} 