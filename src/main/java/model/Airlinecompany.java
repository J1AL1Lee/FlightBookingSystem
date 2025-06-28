package model;

public class Airlinecompany {
    private String airlinecompanyId;    // airlinecompany_ID
    private String airlinecompanyName;  // airlinecompany_name
    private String airlinecompanyTelephone; // airlinecompany_telephone

    public Airlinecompany() {
    }

    public Airlinecompany(String airlinecompanyId, String airlinecompanyName) {
        this.airlinecompanyId = airlinecompanyId;
        this.airlinecompanyName = airlinecompanyName;
    }

    // Getter和Setter
    public String getAirlinecompanyId() { return airlinecompanyId; }
    public void setAirlinecompanyId(String airlinecompanyId) { this.airlinecompanyId = airlinecompanyId; }
    public String getAirlinecompanyName() { return airlinecompanyName; }
    public void setAirlinecompanyName(String airlinecompanyName) { this.airlinecompanyName = airlinecompanyName; }
    public String getAirlinecompanyTelephone() { return airlinecompanyTelephone; }
    public void setAirlinecompanyTelephone(String airlinecompanyTelephone) { this.airlinecompanyTelephone = airlinecompanyTelephone; }

    @Override
    public String toString() {
        return "Airlinecompany{" +
                "airlinecompanyId='" + airlinecompanyId + '\'' +
                ", airlinecompanyName='" + airlinecompanyName + '\'' +
                ", airlinecompanyTelephone='" + airlinecompanyTelephone + '\'' +
                '}';
    }
}