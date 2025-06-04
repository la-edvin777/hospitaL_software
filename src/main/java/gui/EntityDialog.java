package gui;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.Enumeration;
import java.text.ParseException;

public class EntityDialog<T> extends JDialog {
    private final Map<String, JTextField> textFields = new HashMap<>();
    private final Map<String, JComboBox<String>> comboBoxes = new HashMap<>();
    private final Map<String, JSpinner> spinners = new HashMap<>();
    private final Map<String, JTextArea> textAreas = new HashMap<>();
    private final Map<String, JFormattedTextField> dateFields = new HashMap<>();
    private final Map<String, JCheckBox> checkBoxes = new HashMap<>();
    private final Map<String, JRadioButton> radioButtons = new HashMap<>();
    private final Map<String, ButtonGroup> radioGroups = new HashMap<>();
    private final Map<String, JLabel> labels = new HashMap<>();
    private final Map<String, JPanel> panels = new HashMap<>();
    private final Map<String, Object> values = new HashMap<>();
    private final Consumer<T> onSave;
    private final Consumer<T> onCancel;
    private final T entity;
    private boolean isCancelled = true;

    public EntityDialog(JFrame parent, String title, T entity, Map<String, FieldMetadata> fields, 
                       Consumer<T> onSave, Consumer<T> onCancel) {
        super(parent, title, true);
        this.entity = entity;
        this.onSave = onSave;
        this.onCancel = onCancel;
        
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(parent);
        
        // Create main panel with scroll
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add fields
        int row = 0;
        for (Map.Entry<String, FieldMetadata> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            FieldMetadata metadata = entry.getValue();
            
            // Create label
            JLabel label = new JLabel(fieldName + ":");
            gbc.gridx = 0;
            gbc.gridy = row;
            mainPanel.add(label, gbc);
            
            // Create input component based on field type
            JComponent inputComponent = createInputComponent(fieldName, metadata);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            mainPanel.add(inputComponent, gbc);
            
            row++;
        }
        
        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            isCancelled = false;
            saveValues();
            dispose();
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Add window listener for cancel
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (isCancelled && onCancel != null) {
                    onCancel.accept(entity);
                }
            }
        });
    }
    
    private JComponent createInputComponent(String fieldName, FieldMetadata metadata) {
        Class<?> type = metadata.getType();
        
        if (type == String.class) {
            JTextField textField = new JTextField();
            textFields.put(fieldName, textField);
            return textField;
        } else if (type == Integer.class) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            spinners.put(fieldName, spinner);
            return spinner;
        } else if (type == Boolean.class) {
            JCheckBox checkBox = new JCheckBox();
            checkBoxes.put(fieldName, checkBox);
            return checkBox;
        } else if (type == java.util.Date.class) {
            try {
                MaskFormatter formatter = new MaskFormatter("####-##-##");
                formatter.setPlaceholderCharacter('_');
                JFormattedTextField dateField = new JFormattedTextField(formatter);
                dateField.setToolTipText("Enter date in YYYY-MM-DD format");
                dateFields.put(fieldName, dateField);
                return dateField;
            } catch (ParseException e) {
                e.printStackTrace();
                JTextField textField = new JTextField();
                textFields.put(fieldName, textField);
                return textField;
            }
        } else if (metadata.getForeignKeyTable() != null) {
            JComboBox<String> comboBox = new JComboBox<>();
            comboBoxes.put(fieldName, comboBox);
            return comboBox;
        } else {
            JTextField textField = new JTextField();
            textFields.put(fieldName, textField);
            return textField;
        }
    }
    
    private void saveValues() {
        // Save text field values
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getText());
        }
        
        // Save combo box values
        for (Map.Entry<String, JComboBox<String>> entry : comboBoxes.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getSelectedItem());
        }
        
        // Save spinner values
        for (Map.Entry<String, JSpinner> entry : spinners.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getValue());
        }
        
        // Save check box values
        for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
            values.put(entry.getKey(), entry.getValue().isSelected());
        }
        
        // Save date field values
        for (Map.Entry<String, JFormattedTextField> entry : dateFields.entrySet()) {
            values.put(entry.getKey(), entry.getValue().getText());
        }
        
        // Save radio button values
        for (Map.Entry<String, ButtonGroup> entry : radioGroups.entrySet()) {
            for (Enumeration<AbstractButton> buttons = entry.getValue().getElements(); buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    values.put(entry.getKey(), button.getText());
                    break;
                }
            }
        }
        
        // Call onSave callback
        if (onSave != null) {
            onSave.accept(entity);
        }
    }
    
    public Map<String, Object> getValues() {
        return values;
    }
    
    public boolean isCancelled() {
        return isCancelled;
    }
    
    public void setValue(String fieldName, Object value) {
        if (textFields.containsKey(fieldName)) {
            textFields.get(fieldName).setText(value != null ? value.toString() : "");
        } else if (comboBoxes.containsKey(fieldName)) {
            comboBoxes.get(fieldName).setSelectedItem(value);
        } else if (spinners.containsKey(fieldName)) {
            spinners.get(fieldName).setValue(value);
        } else if (checkBoxes.containsKey(fieldName)) {
            checkBoxes.get(fieldName).setSelected(value != null && (Boolean)value);
        } else if (dateFields.containsKey(fieldName)) {
            dateFields.get(fieldName).setText(value != null ? value.toString() : "");
        }
    }
    
    public void setComboBoxItems(String fieldName, String[] items) {
        if (comboBoxes.containsKey(fieldName)) {
            JComboBox<String> comboBox = comboBoxes.get(fieldName);
            comboBox.removeAllItems();
            for (String item : items) {
                comboBox.addItem(item);
            }
        }
    }
} 