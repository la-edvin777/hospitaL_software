package models;

public class Specialist extends Doctor {
    private int experience;

    public Specialist(String doctorid, String name, String specialty, int experience) {
        super(doctorid, name, specialty);
        this.experience = experience;
    }

    // Default constructor
    public Specialist() {
        super();
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    @Override
    public String toString() {
        return String.format("Specialist[%s, Experience=%d years]", 
            super.toString(), experience);
    }
} 