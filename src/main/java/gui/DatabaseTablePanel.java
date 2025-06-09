package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import models.BaseModel;

public class DatabaseTablePanel<T extends BaseModel<T>> extends JPanel {
    private final Connection connection;
    private final T entity;
    private final String tableName;
    private final Map<String, FieldMetadata> fields;
    private final Supplier<T> entityFactory;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton refreshButton;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);

    public DatabaseTablePanel(Connection connection, T entity, String tableName, 
                            Map<String, FieldMetadata> fields, Supplier<T> entityFactory) {
        this.connection = connection;
        this.entity = entity;
        this.tableName = tableName;
        this.fields = fields;
        this.entityFactory = entityFactory;

        setLayout(new BorderLayout());

        // Create table with proper column class detection
        tableModel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // Return String.class by default for empty tables
                if (getRowCount() == 0) {
                    return String.class;
                }
                
                // Get the first non-null value in the column
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, columnIndex);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return String.class;
            }
        };
        
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        
        // Add mouse listener for column sorting
        table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int columnModelIndex = table.getColumnModel().getColumnIndexAtX(evt.getX());
                int viewIndex = table.convertColumnIndexToView(columnModelIndex);
                
                if (viewIndex != -1) {
                    int column = table.convertColumnIndexToModel(viewIndex);
                    if (evt.getClickCount() == 1) {
                        // Single click - sort ascending
                        sorter.setSortKeys(List.of(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
                    } else if (evt.getClickCount() == 2) {
                        // Double click - sort descending
                        sorter.setSortKeys(List.of(new RowSorter.SortKey(column, SortOrder.DESCENDING)));
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add button listeners
        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteSelected());
        refreshButton.addActionListener(e -> refreshTable());

        // Initial load
        refreshTable();
    }

    private void showAddDialog() {
        T newEntity = entityFactory.get();
        JDialog dialog = new JDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            "Add " + tableName,
            true);
        setupEditDialog(dialog, newEntity, true);
        dialog.setVisible(true);
    }

    private void showEditDialog() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a record to edit",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        T selectedEntity = getEntityFromRow(selectedRow);
        JDialog dialog = new JDialog((JFrame)SwingUtilities.getWindowAncestor(this),
            "Edit " + tableName,
            true);
        setupEditDialog(dialog, selectedEntity, false);
        dialog.setVisible(true);
    }

    private void deleteSelected() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a record to delete",
                "No Selection",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this record?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            T selectedEntity = getEntityFromRow(selectedRow);
            try {
                selectedEntity.delete(connection);
                refreshTable();
                JOptionPane.showMessageDialog(this, 
                    "Record deleted successfully",
                    "Delete Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                String message = "Unable to delete this record. ";
                if (e.getMessage().toLowerCase().contains("foreign key")) {
                    message += "This record is referenced by other data in the system and cannot be deleted.";
                } else if (e.getMessage().toLowerCase().contains("access denied")) {
                    message += "You don't have permission to delete records.";
                } else {
                    message += "Database error: " + e.getMessage();
                }
                JOptionPane.showMessageDialog(this, 
                    message,
                    "Delete Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refreshTable() {
        try {
            List<T> entities = entity.selectAll(connection);
            updateTableModel(entities);
        } catch (SQLException e) {
            String message = "Unable to load data from " + tableName + ". ";
            if (e.getMessage().toLowerCase().contains("table") && e.getMessage().toLowerCase().contains("doesn't exist")) {
                message += "The table does not exist in the database.";
            } else if (e.getMessage().toLowerCase().contains("access denied")) {
                message += "You don't have permission to view this data.";
            } else if (e.getMessage().toLowerCase().contains("connection")) {
                message += "Cannot connect to the database. Please check your connection.";
            } else {
                message += "Database error: " + e.getMessage();
            }
            JOptionPane.showMessageDialog(this, 
                message,
                "Data Refresh Failed",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTableModel(List<T> entities) {
        // Clear existing data
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);

        // Define column order based on table name
        String[] columnOrder = getColumnOrder();

        // Add columns in the specified order with friendly names
        for (String fieldName : columnOrder) {
            if (fields.containsKey(fieldName)) {
                tableModel.addColumn(getFriendlyColumnName(fieldName));
            }
        }

        // Add rows
        for (T entity : entities) {
            Vector<Object> row = new Vector<>();
            for (String fieldName : columnOrder) {
                if (fields.containsKey(fieldName)) {
                    try {
                        Object value = getFieldValue(entity, fieldName);
                        // Format date values
                        if (value instanceof Date) {
                            value = DATE_FORMAT.format(value);
                        }
                        row.add(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                        row.add(null); // Add null for fields that can't be retrieved
                    }
                }
            }
            tableModel.addRow(row);
        }

        // Set default sorting based on table
        if (tableModel.getRowCount() > 0) {
            setDefaultSorting();
        }
    }

    /**
     * Convert database field names to user-friendly display names
     */
    private String getFriendlyColumnName(String fieldName) {
        switch (fieldName.toLowerCase()) {
            // Common fields
            case "doctorid": return "Doctor ID";
            case "patientid": return "Patient ID";
            case "visitid": return "Visit ID";
            case "prescriptionid": return "Prescription ID";
            case "insuranceid": return "Insurance ID";
            case "firstname": return "First Name";
            case "surname": return "Surname";
            case "email": return "Email";
            case "address": return "Address";
            case "phone": return "Phone";
            case "postcode": return "Post Code";
            case "specialization": return "Specialization";
            case "company": return "Company";
            
            // Date fields
            case "dateofvisit": return "Date of Visit";
            case "dateprescribed": return "Date Prescribed";
            case "startdate": return "Start Date";
            case "enddate": return "End Date";
            
            // Medical fields
            case "symptoms": return "Symptoms";
            case "diagnosis": return "Diagnosis";
            case "dosage": return "Dosage";
            case "duration": return "Duration";
            case "comment": return "Comment";
            case "drugid": return "Drug ID";
            case "specialty": return "Specialty";
            case "experience": return "Experience (Years)";
            
            // Display names
            case "patientname": return "Patient Name";
            case "doctorname": return "Doctor Name";
            case "maindoctorname": return "Main Doctor";
            
            // Default: capitalize first letter and add spaces before uppercase letters
            default:
                return fieldName.substring(0, 1).toUpperCase() + 
                       fieldName.substring(1).replaceAll("([A-Z])", " $1");
        }
    }

    private String[] getColumnOrder() {
        switch (tableName.toLowerCase()) {
            case "doctor":
                return new String[]{"doctorid", "firstname", "surname", "specialization", "address", "email"};
            case "patient":
                return new String[]{"patientid", "firstname", "surname", "phone", "email", "address", "postcode", 
                    "insuranceid", "insurancecompany", "maindoctorid", "maindoctorname"};
            case "visit":
                return new String[]{"visitid", "patientid", "patientname", "doctorid", "doctorname", 
                    "dateofvisit", "symptoms", "diagnosis"};
            case "prescription":
                return new String[]{"prescriptionid", "patientid", "patientname", "doctorid", "doctorname", 
                    "drugid", "drugname", "dateprescribed", "dosage", "duration", "comment"};
            case "doctorspecialty":
                return new String[]{"doctorid", "doctorname", "specialty", "experience"};
            case "patientinsurance":
                return new String[]{"insuranceid", "insurancecompany", "patientid", "patientname", 
                    "startdate", "enddate"};
            case "insurance":
                return new String[]{"insuranceid", "company", "address", "phone"};
            default:
                return fields.keySet().toArray(new String[0]);
        }
    }

    /**
     * Get display name for a column.
     * Makes the column headers more readable.
     */
    private String getColumnDisplayName(String columnName) {
        // Convert camelCase to Title Case with Spaces
        String displayName = columnName.replaceAll("([a-z])([A-Z])", "$1 $2");
        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
        
        // Special handling for ID fields
        if (displayName.toLowerCase().endsWith("id")) {
            displayName = displayName.substring(0, displayName.length() - 2) + " ID";
        }
        
        return displayName;
    }

    private void setDefaultSorting() {
        switch (tableName.toLowerCase()) {
            case "doctor":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING))); // Sort by firstname
                break;
            case "patient":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING))); // Sort by firstname
                break;
            case "visit":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(3, SortOrder.DESCENDING))); // Sort by dateofvisit
                break;
            case "prescription":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.DESCENDING))); // Sort by dateprescribed
                break;
            case "doctorspecialty":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING))); // Sort by specialty
                break;
            case "patientinsurance":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(2, SortOrder.ASCENDING))); // Sort by startdate
                break;
            case "insurance":
                sorter.setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING))); // Sort by company
                break;
        }
    }

    private T getEntityFromRow(int row) {
        T entity = entityFactory.get();
        String[] columnOrder = getColumnOrder();
        
        for (int col = 0; col < table.getColumnCount(); col++) {
            String fieldName = columnOrder[col];
            Object value = table.getValueAt(row, col);
            
            try {
                setFieldValue(entity, fieldName, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return entity;
    }

    private void setupEditDialog(JDialog dialog, T entity, boolean isNew) {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        Map<String, JComponent> inputFields = new HashMap<>();
        
        // Create a new entity instance for new records
        T workingEntity = isNew ? entityFactory.get() : entity;
        
        String[] columnOrder = getColumnOrder();
        
        for (String fieldName : columnOrder) {
            if (!fields.containsKey(fieldName)) continue;
            
            FieldMetadata metadata = fields.get(fieldName);
            
            // Skip display-only fields (like patientName, doctorName) from input forms
            if (isInputOnlyField(fieldName)) continue;
            
            JLabel label = new JLabel(getFriendlyColumnName(fieldName) + (metadata.isRequired() ? " *" : "") + ":");
            JComponent input;
            
            // Check if this is an auto-generated ID field
            boolean isAutoGeneratedId = metadata.isPrimaryKey() && isNew;
            
            if (metadata.hasRelation()) {
                // Create combobox for foreign key fields
                JComboBox<ComboBoxItem> comboBox = new JComboBox<>();
                comboBox.addItem(new ComboBoxItem("", "-- Select " + getFriendlyColumnName(fieldName) + " --"));
                loadForeignKeyItems(comboBox, metadata);
                input = comboBox;
                
                // Set initial value if editing
                if (!isNew && entity != null) {
                    Object currentValue = getFieldValue(entity, fieldName);
                    String currentId = currentValue != null ? currentValue.toString() : null;
                    selectComboBoxItem(comboBox, currentId);
                }
            } else if (metadata.getType() == Date.class || metadata.getType() == java.sql.Date.class) {
                // Create date picker for date input
                input = new DatePicker();
                if (!isNew && entity != null) {
                    Object value = getFieldValue(entity, fieldName);
                    if (value instanceof Date) {
                        ((DatePicker) input).setSelectedDate((Date) value);
                    } else if (value instanceof String) {
                        ((DatePicker) input).setDateString((String) value);
                    }
                }
            } else {
                // Create text field for other fields
                JTextField textField = new JTextField(20);
                if (!isNew && entity != null) {
                    Object value = getFieldValue(entity, fieldName);
                    textField.setText(value != null ? value.toString() : "");
                } else if (isAutoGeneratedId) {
                    // Generate and set the ID
                    String generatedId = metadata.generateId(tableName);
                    textField.setText(generatedId);
                    textField.setEditable(false);
                    // Set the ID in the entity
                    setFieldValue(workingEntity, fieldName, generatedId);
                }
                input = textField;
            }
            
            // Make ID fields read-only when editing (but not when adding foreign key references)
            if (!isNew && metadata.isPrimaryKey()) {
                if (input instanceof JTextField) {
                    ((JTextField) input).setEditable(false);
                } else if (input instanceof JComboBox) {
                    ((JComboBox<?>) input).setEnabled(false);
                } else if (input instanceof DatePicker) {
                    input.setEnabled(false);
                }
            }
            
            panel.add(label);
            panel.add(input);
            inputFields.put(fieldName, input);
        }
        
        // Add buttons
        JPanel buttonPanel = new JPanel();
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            try {
                T entityToSave = isNew ? workingEntity : entity;
                StringBuilder validationErrors = new StringBuilder();
                
                // Validate all fields and collect errors
                for (String fieldName : columnOrder) {
                    if (!fields.containsKey(fieldName)) continue;
                    
                    // Skip display-only fields from validation
                    if (isInputOnlyField(fieldName)) continue;
                    
                    FieldMetadata metadata = fields.get(fieldName);
                    JComponent input = inputFields.get(fieldName);
                    String value = getInputValue(input);
                    
                    // Validate the field
                    FieldMetadata.ValidationResult validation = metadata.validateValue(value);
                    if (!validation.isValid()) {
                        if (validationErrors.length() > 0) validationErrors.append("\n");
                        validationErrors.append(getFriendlyColumnName(fieldName)).append(": ").append(validation.getErrorMessage());
                        continue;
                    }
                    
                    // Set the value if validation passed
                    if (value != null && !value.isEmpty()) {
                        try {
                            setFieldValue(entityToSave, fieldName, value, metadata.getType());
                        } catch (Exception ex) {
                            if (validationErrors.length() > 0) validationErrors.append("\n");
                            validationErrors.append(getFriendlyColumnName(fieldName)).append(": Invalid value format");
                        }
                    }
                }
                
                // Check for validation errors
                if (validationErrors.length() > 0) {
                    throw new IllegalArgumentException("Validation errors:\n" + validationErrors.toString());
                }
                
                // Save the entity
                if (isNew) {
                    entityToSave.create(connection);
                    JOptionPane.showMessageDialog(dialog, 
                        "Record created successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    entityToSave.update(connection);
                    JOptionPane.showMessageDialog(dialog, 
                        "Record updated successfully",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                }
                refreshTable();
                dialog.dispose();
                
            } catch (SQLException ex) {
                String message = "Unable to save the record.\n\n";
                if (ex.getMessage().toLowerCase().contains("duplicate")) {
                    message += "Error: A record with this ID already exists.";
                } else if (ex.getMessage().toLowerCase().contains("foreign key")) {
                    message += "Error: One of the selected references does not exist in the database.";
                } else if (ex.getMessage().toLowerCase().contains("cannot be null")) {
                    message += "Error: Required fields cannot be empty.";
                } else if (ex.getMessage().toLowerCase().contains("data truncated")) {
                    message += "Error: Some fields contain invalid data or are too long.";
                } else if (ex.getMessage().toLowerCase().contains("access denied")) {
                    message += "Error: You don't have permission to save records.";
                } else {
                    message += "Database error: " + ex.getMessage();
                }
                JOptionPane.showMessageDialog(dialog, message, "Save Failed", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, 
                    "Validation failed:\n\n" + ex.getMessage(), 
                    "Validation Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.add(new JScrollPane(panel), BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(500, Math.min(600, 100 + fields.size() * 40));
        dialog.setLocationRelativeTo(this);
    }

    /**
     * Check if a field is input-only (not displayed in forms)
     */
    private boolean isInputOnlyField(String fieldName) {
        return fieldName.equals("patientName") || fieldName.equals("doctorName") || fieldName.equals("maindoctorname") || 
               fieldName.equals("patientname") || fieldName.equals("doctorname") || fieldName.equals("drugname") ||
               fieldName.equals("insurancecompany");
    }

    /**
     * Helper class for combo box items
     */
    private static class ComboBoxItem {
        private final String id;
        private final String display;

        public ComboBoxItem(String id, String display) {
            this.id = id;
            this.display = display;
        }

        public String getId() { return id; }

        @Override
        public String toString() {
            return display;
        }
    }

    /**
     * Get input value from component
     */
    private String getInputValue(JComponent input) {
        if (input instanceof JTextField) {
            return ((JTextField) input).getText().trim();
        } else if (input instanceof JComboBox) {
            ComboBoxItem selected = (ComboBoxItem) ((JComboBox<?>) input).getSelectedItem();
            return selected != null ? selected.getId() : null;
        } else if (input instanceof DatePicker) {
            Date date = ((DatePicker) input).getSelectedDate();
            if (date != null) {
                return DATE_FORMAT.format(date);
            }
            return null;
        }
        return null;
    }

    /**
     * Select combo box item by ID
     */
    private void selectComboBoxItem(JComboBox<ComboBoxItem> comboBox, String id) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            if (comboBox.getItemAt(i).getId().equals(id)) {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Load foreign key items into a combo box
     */
    private void loadForeignKeyItems(JComboBox<ComboBoxItem> comboBox, FieldMetadata metadata) {
        try {
            List<gui.ComboBoxItem> items = metadata.getRelatedItems(connection);
            for (gui.ComboBoxItem externalItem : items) {
                comboBox.addItem(new ComboBoxItem(externalItem.getId(), externalItem.toString()));
            }
        } catch (SQLException e) {
            String message = "Unable to load reference data for " + metadata.getForeignKeyTable() + ".\n\n";
            if (e.getMessage().toLowerCase().contains("table") && e.getMessage().toLowerCase().contains("doesn't exist")) {
                message += "Error: The reference table does not exist in the database.";
            } else if (e.getMessage().toLowerCase().contains("column") && e.getMessage().toLowerCase().contains("doesn't exist")) {
                message += "Error: One or more columns are missing in the reference table.";
            } else if (e.getMessage().toLowerCase().contains("access denied")) {
                message += "Error: Database access denied. Please check your database permissions.";
            } else {
                message += "Database error: " + e.getMessage();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, message, "Data Loading Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get field value using reflection
     */
    private Object getFieldValue(T entity, String fieldName) {
        try {
            // Handle special display-only fields that don't exist as getters
            if (isDisplayOnlyField(fieldName)) {
                return getDisplayValue(entity, fieldName);
            }
            
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = entity.getClass().getMethod(getterName);
            return getter.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if a field is display-only (calculated from related data)
     */
    private boolean isDisplayOnlyField(String fieldName) {
        return fieldName.equals("doctorname") || fieldName.equals("patientname") || 
               fieldName.equals("drugname") || fieldName.equals("insurancecompany") ||
               fieldName.equals("patientName") || fieldName.equals("doctorName") || 
               fieldName.equals("maindoctorname");
    }
    
    /**
     * Get display value for display-only fields by looking up related data
     */
    private Object getDisplayValue(T entity, String fieldName) {
        try {
            switch (fieldName.toLowerCase()) {
                case "doctorname":
                    return getRelatedDisplayValue(entity, "doctorid", "doctor", "CONCAT(firstname, ' ', surname)");
                case "patientname":
                    return getRelatedDisplayValue(entity, "patientid", "patient", "CONCAT(firstname, ' ', surname)");
                case "drugname":
                    return getRelatedDisplayValue(entity, "drugid", "drug", "name");
                case "insurancecompany":
                    return getRelatedDisplayValue(entity, "insuranceid", "insurance", "company");
                case "maindoctorname":
                    return getRelatedDisplayValue(entity, "maindoctorid", "doctor", "CONCAT(firstname, ' ', surname)");
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get related display value by querying the referenced table
     */
    private String getRelatedDisplayValue(T entity, String foreignKeyField, String relatedTable, String displayColumn) {
        try {
            // Get the foreign key value from the entity using direct reflection (avoid recursion)
            Object foreignKeyValue = getFieldValueDirect(entity, foreignKeyField);
            if (foreignKeyValue == null) {
                return null;
            }
            
            // Query the related table for the display value
            String sql;
            if (displayColumn.toUpperCase().startsWith("CONCAT(")) {
                sql = String.format("SELECT %s AS display_name FROM %s WHERE %s = ?", 
                    displayColumn, relatedTable, getPrimaryKeyField(relatedTable));
            } else {
                sql = String.format("SELECT %s AS display_name FROM %s WHERE %s = ?", 
                    displayColumn, relatedTable, getPrimaryKeyField(relatedTable));
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, foreignKeyValue.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("display_name");
                    }
                }
            }
        } catch (SQLException e) {
            // Return the ID if we can't get the display value
            try {
                Object foreignKeyValue = getFieldValueDirect(entity, foreignKeyField);
                return foreignKeyValue != null ? foreignKeyValue.toString() : null;
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Get field value using direct reflection (without display field handling)
     */
    private Object getFieldValueDirect(T entity, String fieldName) {
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = entity.getClass().getMethod(getterName);
            return getter.invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get the primary key field name for a table
     */
    private String getPrimaryKeyField(String tableName) {
        switch (tableName.toLowerCase()) {
            case "doctor": return "doctorid";
            case "patient": return "patientid";
            case "drug": return "drugid";
            case "insurance": return "insuranceid";
            case "visit": return "visitid";
            case "prescription": return "prescriptionid";
            default: return "id";
        }
    }

    /**
     * Set field value using reflection
     */
    private void setFieldValue(T entity, String fieldName, Object value) {
        try {
            FieldMetadata metadata = fields.get(fieldName);
            if (metadata != null) {
                setFieldValue(entity, fieldName, value, metadata.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set field value using reflection with type conversion
     */
    private void setFieldValue(T entity, String fieldName, Object value, Class<?> type) throws Exception {
        if (value == null) {
            return;
        }

        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        Method setter = null;

        if (type == Date.class || type == java.sql.Date.class) {
            if (value instanceof String) {
                try {
                    Date date = DATE_FORMAT.parse((String) value);
                    setter = entity.getClass().getMethod(setterName, java.util.Date.class);
                    setter.invoke(entity, date);
                    return;
                } catch (ParseException e) {
                    throw new Exception("Invalid date format for " + fieldName);
                }
            } else if (value instanceof Date) {
                setter = entity.getClass().getMethod(setterName, java.util.Date.class);
                setter.invoke(entity, value);
                return;
            }
        } else if (type == Integer.class || type == int.class) {
            if (value instanceof String) {
                setter = entity.getClass().getMethod(setterName, type);
                setter.invoke(entity, Integer.parseInt((String) value));
                return;
            }
        }

        // Default handling for other types
        setter = entity.getClass().getMethod(setterName, type);
        setter.invoke(entity, value);
    }
}