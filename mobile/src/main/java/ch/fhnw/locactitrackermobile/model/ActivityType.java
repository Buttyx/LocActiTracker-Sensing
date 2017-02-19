package ch.fhnw.locactitrackermobile.model;

/**
 * List of supported activity
 */
public enum ActivityType {

    COOKING("Cooking"),
    DINNING("Dinning"),
    SPORT("Sport"),
    SHOPPING("Shopping"),
    ENTERTAINMENT("Entertainment"),
    STUDYING("Studying"),
    TRANSPORTATION("Transportation");

    private String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel(){
        return label;
    }

}
