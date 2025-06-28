package model;

import java.time.LocalDate;

public class Flightrecord {
    private String flightrecordId;   // flightrecord_ID
    private String flightId;         // flight_ID
    private LocalDate flightDate;    // flight_date
    private Integer seat0Left;       // seat0_left
    private Integer seat1Left;       // seat1_left

    public Flightrecord() {
    }

    public Flightrecord(String flightrecordId, String flightId) {
        this.flightrecordId = flightrecordId;
        this.flightId = flightId;
    }

    // Getter和Setter
    public String getFlightrecordId() { return flightrecordId; }
    public void setFlightrecordId(String flightrecordId) { this.flightrecordId = flightrecordId; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public LocalDate getFlightDate() { return flightDate; }
    public void setFlightDate(LocalDate flightDate) { this.flightDate = flightDate; }
    public Integer getSeat0Left() { return seat0Left; }
    public void setSeat0Left(Integer seat0Left) { this.seat0Left = seat0Left; }
    public Integer getSeat1Left() { return seat1Left; }
    public void setSeat1Left(Integer seat1Left) { this.seat1Left = seat1Left; }

    @Override
    public String toString() {
        return "Flightrecord{" +
                "flightrecordId='" + flightrecordId + '\'' +
                ", flightId='" + flightId + '\'' +
                ", flightDate=" + flightDate +
                ", seat0Left=" + seat0Left +
                ", seat1Left=" + seat1Left +
                '}';
    }
}