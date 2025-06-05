/**
 * Combo box item with ID and display text.
 */
package gui;

public class ComboBoxItem {
    /** The actual value (usually a database ID) */
    private final String id;
    
    /** The text to display in the combo box */
    private final String display;

    /**
     * Creates a new combo box item with the specified ID and display text.
     * 
     * @param id The actual value to be used internally (e.g., database ID)
     * @param display The text to display to the user
     */
    public ComboBoxItem(String id, String display) {
        this.id = id != null ? id : "";
        this.display = display != null ? display : "";
    }

    /**
     * Gets the actual ID of the item.
     * @return The internal ID (e.g., database ID)
     */
    public String getId() { 
        return id; 
    }

    /**
     * Gets the display text of the item.
     * @return The text to show in the combo box
     */
    public String getDisplay() { 
        return display; 
    }

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

    /**
     * Checks if this item represents an empty/null selection.
     * @return true if the ID is null or empty
     */
    public boolean isEmpty() {
        return id == null || id.trim().isEmpty();
    }

    /**
     * Equals method for proper comparison
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ComboBoxItem that = (ComboBoxItem) obj;
        return id.equals(that.id);
    }

    /**
     * Hash code for proper hashing
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}