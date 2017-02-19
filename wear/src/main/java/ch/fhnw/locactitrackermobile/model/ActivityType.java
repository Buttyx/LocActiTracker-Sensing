package ch.fhnw.locactitrackermobile.model;

/**
 * List of supported activity
 */
public enum ActivityType {

    COOKING("Cooking", 1),
    DINNING("Dinning", 2),
    SHOPPING("Shopping",3 ),
    ENTERTAINMENT("Entertainment",4),
    STUDYING("Studying",5),
    TRANSPORTATION("Transportation",6),
    WALKING("Walking",7),
    BIKING("Biking", 8),
    OTHER("Other", 9);

    private String label;
    private int id;

    ActivityType(String label, int id) {
        this.id = id;
        this.label = label;
    }

    public String getLabel(){
        return label;
    }

}
