package model;

import java.time.LocalDateTime;

public class Payrecord {
    private String payId;           // pay_ID
    private String orderId;         // order_ID
    private Integer payment;        // payment
    private String payMethod;       // pay_method
    private String payState;        // pay_state
    private LocalDateTime payTime;  // pay_time

    public Payrecord() {
    }

    public Payrecord(String payId, String orderId, Integer payment) {
        this.payId = payId;
        this.orderId = orderId;
        this.payment = payment;
    }

    // Getter和Setter
    public String getPayId() { return payId; }
    public void setPayId(String payId) { this.payId = payId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Integer getPayment() { return payment; }
    public void setPayment(Integer payment) { this.payment = payment; }
    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }
    public String getPayState() { return payState; }
    public void setPayState(String payState) { this.payState = payState; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }

    @Override
    public String toString() {
        return "Payrecord{" +
                "payId='" + payId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", payment=" + payment +
                ", payMethod='" + payMethod + '\'' +
                ", payState='" + payState + '\'' +
                ", payTime=" + payTime +
                '}';
    }
}