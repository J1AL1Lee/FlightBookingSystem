package model;

import java.time.LocalTime;

public class Flight {
    private String flightId;            // flight_ID
    private String airlinecompanyId;    // airlinecompany_ID
    private String airportFrom;         // airport_from
    private String airportTo;           // airport_to
    private LocalTime timeTakeoff;      // time_takeoff
    private LocalTime timeArrive;       // time_arrive
    private Integer seat0Capacity;      // seat0_capacity
    private Integer seat1Capacity;      // seat1_capacity
    private Integer seat0Price;         // seat0_price
    private Integer seat1Price;         // seat1_price
    private Float discount;             // discount

    public Flight() {
    }

    public Flight(String flightId, String airlinecompanyId, String airportFrom, String airportTo) {
        this.flightId = flightId;
        this.airlinecompanyId = airlinecompanyId;
        this.airportFrom = airportFrom;
        this.airportTo = airportTo;
    }

    // Getter和Setter
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public String getAirlinecompanyId() { return airlinecompanyId; }
    public void setAirlinecompanyId(String airlinecompanyId) { this.airlinecompanyId = airlinecompanyId; }
    public String getAirportFrom() { return airportFrom; }
    public void setAirportFrom(String airportFrom) { this.airportFrom = airportFrom; }
    public String getAirportTo() { return airportTo; }
    public void setAirportTo(String airportTo) { this.airportTo = airportTo; }
    public LocalTime getTimeTakeoff() { return timeTakeoff; }
    public void setTimeTakeoff(LocalTime timeTakeoff) { this.timeTakeoff = timeTakeoff; }
    public LocalTime getTimeArrive() { return timeArrive; }
    public void setTimeArrive(LocalTime timeArrive) { this.timeArrive = timeArrive; }
    public Integer getSeat0Capacity() { return seat0Capacity; }
    public void setSeat0Capacity(Integer seat0Capacity) { this.seat0Capacity = seat0Capacity; }
    public Integer getSeat1Capacity() { return seat1Capacity; }
    public void setSeat1Capacity(Integer seat1Capacity) { this.seat1Capacity = seat1Capacity; }
    public Integer getSeat0Price() { return seat0Price; }
    public void setSeat0Price(Integer seat0Price) { this.seat0Price = seat0Price; }
    public Integer getSeat1Price() { return seat1Price; }
    public void setSeat1Price(Integer seat1Price) { this.seat1Price = seat1Price; }
    public Float getDiscount() { return discount; }
    public void setDiscount(Float discount) { this.discount = discount; }

    @Override
    public String toString() {
        return "Flight{" +
                "flightId='" + flightId + '\'' +
                ", airlinecompanyId='" + airlinecompanyId + '\'' +
                ", airportFrom='" + airportFrom + '\'' +
                ", airportTo='" + airportTo + '\'' +
                ", timeTakeoff=" + timeTakeoff +
                ", timeArrive=" + timeArrive +
                ", seat0Capacity=" + seat0Capacity +
                ", seat1Capacity=" + seat1Capacity +
                ", seat0Price=" + seat0Price +
                ", seat1Price=" + seat1Price +
                ", discount=" + discount +
                '}';
    }
}