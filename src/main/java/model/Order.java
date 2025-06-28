package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Order {
    private String orderId;         // order_ID
    private String userId;          // user_ID
    private String flightId;        // flight_ID
    private String orderState;      // order_state
    private LocalDate flightTime;   // flight_time
    private LocalDateTime orderTime; // order_time
    private Integer seatId;         // seat_id
    private Integer seatType;       // seat_type

    public Order() {
    }

    public Order(String orderId, String userId, String flightId) {
        this.orderId = orderId;
        this.userId = userId;
        this.flightId = flightId;
    }

    // Getter和Setter
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public String getOrderState() { return orderState; }
    public void setOrderState(String orderState) { this.orderState = orderState; }
    public LocalDate getFlightTime() { return flightTime; }
    public void setFlightTime(LocalDate flightTime) { this.flightTime = flightTime; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }
    public Integer getSeatType() { return seatType; }
    public void setSeatType(Integer seatType) { this.seatType = seatType; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", flightId='" + flightId + '\'' +
                ", orderState='" + orderState + '\'' +
                ", flightTime=" + flightTime +
                ", orderTime=" + orderTime +
                ", seatId=" + seatId +
                ", seatType=" + seatType +
                '}';
    }
}