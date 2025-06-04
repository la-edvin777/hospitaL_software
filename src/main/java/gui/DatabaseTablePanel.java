package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
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

        // Add columns in the specified order
        for (String fieldName : columnOrder) {
            if (fields.containsKey(fieldName)) {
                tableModel.addColumn(fieldName);
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

    private String[] getColumnOrder() {
        switch (tableName.toLowerCase()) {
            case "doctor":
                return new String[]{"doctorid", "firstname", "surname", "specialization", "address", "email"};
            case "patient":
                return new String[]{"patientid", "firstname", "surname", "phone", "email", "address", "postcode", "insuranceid"};
            case "visit":
                return new String[]{"visitid", "patientName", "doctorName", "dateofvisit", "symptoms", "diagnosis"};
            case "prescription":
                return new String[]{"prescriptionid", "patientid", "doctorid", "drugid", "dateprescribed", "dosage", "duration", "comment"};
            case "doctorspecialty":
                return new String[]{"doctorid", "specialty", "experience"};
            case "patientinsurance":
                return new String[]{"insuranceid", "patientid", "startdate", "enddate"};
            case "insurance":
                return new String[]{"insuranceid", "company", "address", "phone"};
            default:
                return fields.keySet().toArray(new String[0]);
        }
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
            
            JLabel label = new JLabel(fieldName + (metadata.isRequired() ? " *" : "") + ":");
            JComponent input;
            
            // Check if this is an auto-generated ID field
            boolean isAutoGeneratedId = metadata.isPrimaryKey() && isNew;
            
            if (metadata.hasRelation()) {
                // Create combobox for foreign key fields
                JComboBox<ComboBoxItem> comboBox = new JComboBox<>();
                comboBox.addItem(new ComboBoxItem("", "-- Select " + fieldName + " --"));
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
                DatePicker datePicker = new DatePicker();
                if (!isNew && entity != null) {
                    Object value = getFieldValue(entity, fieldName);
                    if (value instanceof Date) {
                        datePicker.setSelectedDate((Date) value);
                    }
                }
                input = datePicker;
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
                        validationErrors.append(fieldName).append(": ").append(validation.getErrorMessage());
                        continue;
                    }
                    
                    // Set the value if validation passed
                    if (value != null && !value.isEmpty()) {
                        try {
                            setFieldValue(entityToSave, fieldName, value, metadata.getType());
                        } catch (Exception ex) {
                            if (validationErrors.length() > 0) validationErrors.append("\n");
                            validationErrors.append(fieldName).append(": Invalid value format");
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
        return fieldName.equals("patientName") || fieldName.equals("doctorName");
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
            return ((DatePicker) input).getDateString();
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
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Method getter = entity.getClass().getMethod(getterName);
            return getter.invoke(entity);
        } catch (Exception e) {
            return null;
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
    private void setFieldValue(T entity, String fieldName, Object value, Class<?> targetType) throws Exception {
        String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        
        if (value == null || (value instanceof String && ((String) value).isEmpty())) {
            // Handle null/empty values
            for (Method method : entity.getClass().getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    method.invoke(entity, (Object) null);
                    return;
                }
            }
        } else if (targetType == Date.class || targetType == java.sql.Date.class) {
            // Handle date conversion
            if (value instanceof String) {
                try {
                    java.util.Date parsedDate = DATE_FORMAT.parse((String) value);
                    java.sql.Date sqlDate = new java.sql.Date(parsedDate.getTime());
                    Method setter = entity.getClass().getMethod(setterName, java.sql.Date.class);
                    setter.invoke(entity, sqlDate);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD");
                }
            }
        } else if (targetType == Integer.class || targetType == int.class) {
            // Handle integer conversion
            if (value instanceof String) {
                try {
                    int intValue = Integer.parseInt((String) value);
                    Method setter = entity.getClass().getMethod(setterName, int.class);
                    setter.invoke(entity, intValue);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Must be a valid number");
                }
            } else if (value instanceof Integer) {
                Method setter = entity.getClass().getMethod(setterName, int.class);
                setter.invoke(entity, value);
            }
        } else {
            // Handle string and other types
            Method setter = entity.getClass().getMethod(setterName, targetType);
            setter.invoke(entity, value);
        }
    }
}