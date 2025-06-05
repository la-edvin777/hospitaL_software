package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * A custom date picker component for the Hospital Management System.
 * Provides an intuitive calendar interface for date selection.
 */
public class DatePicker extends JPanel {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    private static final String[] MONTHS = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    
    private JTextField dateField;
    private JButton calendarButton;
    private Calendar calendar;
    private Date selectedDate;
    private JDialog calendarDialog;
    private JLabel monthYearLabel;
    private JTable calendarTable;
    private DefaultTableModel tableModel;
    
    public DatePicker() {
        this(null);
    }
    
    public DatePicker(Date initialDate) {
        this.calendar = Calendar.getInstance();
        this.selectedDate = initialDate;
        
        setLayout(new BorderLayout());
        initComponents();
        updateDateField();
    }
    
    private void initComponents() {
        // Create the text field
        dateField = new JTextField(10);
        dateField.setToolTipText("Enter date in YYYY-MM-DD format or click calendar button");
        dateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateAndSetDate();
            }
        });
        
        dateField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    validateAndSetDate();
                }
            }
        });
        
        // Create the calendar button
        calendarButton = new JButton("📅");
        calendarButton.setToolTipText("Open calendar");
        calendarButton.setPreferredSize(new Dimension(30, dateField.getPreferredSize().height));
        calendarButton.addActionListener(e -> showCalendar());
        
        // Add components
        add(dateField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);
    }
    
    private void validateAndSetDate() {
        String text = dateField.getText().trim();
        if (text.isEmpty()) {
            selectedDate = null;
            return;
        }
        
        try {
            DATE_FORMAT.setLenient(false);
            Date parsedDate = DATE_FORMAT.parse(text);
            selectedDate = parsedDate;
            calendar.setTime(parsedDate);
            updateDateField();
        } catch (Exception e) {
            // Invalid date format - revert to previous value
            updateDateField();
            JOptionPane.showMessageDialog(this, 
                "Invalid date format. Please use YYYY-MM-DD format.", 
                "Invalid Date", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateDateField() {
        if (selectedDate != null) {
            dateField.setText(DATE_FORMAT.format(selectedDate));
        } else {
            dateField.setText("");
        }
    }
    
    private void showCalendar() {
        if (calendarDialog == null) {
            createCalendarDialog();
        }
        
        // Set calendar to current date or today
        if (selectedDate != null) {
            calendar.setTime(selectedDate);
        } else {
            calendar.setTime(new Date());
        }
        
        updateCalendarDisplay();
        
        // Position dialog relative to this component
        Point location = getLocationOnScreen();
        calendarDialog.setLocation(location.x, location.y + getHeight());
        calendarDialog.setVisible(true);
    }
    
    private void createCalendarDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        if (parentWindow instanceof Frame) {
            calendarDialog = new JDialog((Frame) parentWindow, "Select Date", true);
        } else if (parentWindow instanceof Dialog) {
            calendarDialog = new JDialog((Dialog) parentWindow, "Select Date", true);
        } else {
            calendarDialog = new JDialog((Frame) null, "Select Date", true);
        }
        
        calendarDialog.setLayout(new BorderLayout());
        calendarDialog.setResizable(false);
        
        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        calendarDialog.add(headerPanel, BorderLayout.NORTH);
        
        // Create calendar table
        createCalendarTable();
        calendarDialog.add(new JScrollPane(calendarTable), BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = createButtonPanel();
        calendarDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        calendarDialog.pack();
        calendarDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        // Previous month button
        JButton prevButton = new JButton("◀");
        prevButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendarDisplay();
        });
        
        // Month/Year label
        monthYearLabel = new JLabel("", SwingConstants.CENTER);
        monthYearLabel.setFont(monthYearLabel.getFont().deriveFont(Font.BOLD, 14f));
        
        // Next month button
        JButton nextButton = new JButton("▶");
        nextButton.addActionListener(e -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendarDisplay();
        });
        
        headerPanel.add(prevButton, BorderLayout.WEST);
        headerPanel.add(monthYearLabel, BorderLayout.CENTER);
        headerPanel.add(nextButton, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private void createCalendarTable() {
        tableModel = new DefaultTableModel(DAYS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        calendarTable = new JTable(tableModel);
        calendarTable.setRowHeight(30);
        calendarTable.setShowGrid(true);
        calendarTable.setGridColor(Color.LIGHT_GRAY);
        calendarTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom cell renderer
        calendarTable.setDefaultRenderer(Object.class, new CalendarCellRenderer());
        
        // Add mouse listener for date selection
        calendarTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = calendarTable.rowAtPoint(e.getPoint());
                int col = calendarTable.columnAtPoint(e.getPoint());
                Object value = calendarTable.getValueAt(row, col);
                
                if (value != null && value instanceof Integer) {
                    int day = (Integer) value;
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    selectedDate = calendar.getTime();
                    updateDateField();
                    calendarDialog.setVisible(false);
                    firePropertyChange("selectedDate", null, selectedDate);
                }
            }
        });
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(e -> {
            calendar.setTime(new Date());
            selectedDate = calendar.getTime();
            updateDateField();
            calendarDialog.setVisible(false);
            firePropertyChange("selectedDate", null, selectedDate);
        });
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            selectedDate = null;
            updateDateField();
            calendarDialog.setVisible(false);
            firePropertyChange("selectedDate", null, selectedDate);
        });
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> calendarDialog.setVisible(false));
        
        buttonPanel.add(todayButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);
        
        return buttonPanel;
    }
    
    private void updateCalendarDisplay() {
        // Update month/year label
        monthYearLabel.setText(MONTHS[calendar.get(Calendar.MONTH)] + " " + calendar.get(Calendar.YEAR));
        
        // Clear existing rows
        tableModel.setRowCount(0);
        
        // Get first day of month and number of days
        Calendar tempCal = (Calendar) calendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday
        int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Fill calendar grid
        Object[][] calendarData = new Object[6][7]; // 6 weeks max
        int currentDay = 1;
        
        for (int week = 0; week < 6; week++) {
            for (int day = 0; day < 7; day++) {
                if (week == 0 && day < firstDayOfWeek) {
                    calendarData[week][day] = null; // Empty cell
                } else if (currentDay <= daysInMonth) {
                    calendarData[week][day] = currentDay++;
                } else {
                    calendarData[week][day] = null; // Empty cell
                }
            }
            tableModel.addRow(calendarData[week]);
        }
        
        calendarTable.repaint();
    }
    
    // Custom cell renderer for calendar
    private class CalendarCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value == null) {
                setText("");
                setBackground(Color.WHITE);
                return component;
            }
            
            setText(value.toString());
            setHorizontalAlignment(SwingConstants.CENTER);
            
            // Highlight today
            Calendar today = Calendar.getInstance();
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                value.equals(today.get(Calendar.DAY_OF_MONTH))) {
                setBackground(new Color(173, 216, 230)); // Light blue
            }
            // Highlight selected date
            else if (selectedDate != null) {
                Calendar selectedCal = Calendar.getInstance();
                selectedCal.setTime(selectedDate);
                if (calendar.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                    value.equals(selectedCal.get(Calendar.DAY_OF_MONTH))) {
                    setBackground(new Color(144, 238, 144)); // Light green
                }
            } else {
                setBackground(Color.WHITE);
            }
            
            if (isSelected) {
                setBackground(getBackground().darker());
            }
            
            return component;
        }
    }
    
    // Public methods
    public Date getSelectedDate() {
        return selectedDate;
    }
    
    public void setSelectedDate(Date date) {
        this.selectedDate = date;
        if (date != null) {
            calendar.setTime(date);
        }
        updateDateField();
        firePropertyChange("selectedDate", null, selectedDate);
    }
    
    public String getDateString() {
        return selectedDate != null ? DATE_FORMAT.format(selectedDate) : "";
    }
    
    public void setDateString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            setSelectedDate(null);
            return;
        }
        
        try {
            DATE_FORMAT.setLenient(false);
            Date date = DATE_FORMAT.parse(dateString.trim());
            setSelectedDate(date);
        } catch (Exception e) {
            // Invalid format - clear the date
            setSelectedDate(null);
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        dateField.setEnabled(enabled);
        calendarButton.setEnabled(enabled);
    }
    
    public JTextField getTextField() {
        return dateField;
    }
}