/**
 * Represents an item in a combo box (dropdown) in the Hospital Management System GUI.
 * This class maintains both the actual value and display text for combo box items,
 * particularly useful for displaying human-readable text while maintaining database IDs.
 */
package gui;

public class ComboBoxItem {
    /** The actual value (usually a database ID) */
    private final String value;
    
    /** The text to display in the combo box */
    private final String display;

    /**
     * Creates a new combo box item with the specified value and display text.
     * 
     * @param value The actual value to be used internally (e.g., database ID)
     * @param display The text to display to the user
     */
    public ComboBoxItem(String value, String display) {
        this.value = value;
        this.display = display;
    }

    /**
     * Gets the actual value of the item.
     * @return The internal value (e.g., database ID)
     */
    public String getValue() { return value; }

    /**
     * Gets the display text of the item.
     * @return The text to show in the combo box
     */
    public String getDisplay() { return display; }

    /**
     * Returns the display text when the object needs to be converted to a string.
     * This is used by JComboBox to show the correct text in the dropdown.
     * 
     * @return The display text of the item
     */
    @Override
    public String toString() {
        return display;
    }
} 