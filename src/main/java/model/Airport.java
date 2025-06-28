package model;

public class Airport {
    private String airportId;        // airport_ID
    private String airportName;      // airport_name
    private String city;            // city
    private String country;         // country

    public Airport() {
    }

    public Airport(String airportId, String airportName) {
        this.airportId = airportId;
        this.airportName = airportName;
    }

    // Getter和Setter
    public String getAirportId() { return airportId; }
    public void setAirportId(String airportId) { this.airportId = airportId; }
    public String getAirportName() { return airportName; }
    public void setAirportName(String airportName) { this.airportName = airportName; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public String toString() {
        return "Airport{" +
                "airportId='" + airportId + '\'' +
                ", airportName='" + airportName + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}