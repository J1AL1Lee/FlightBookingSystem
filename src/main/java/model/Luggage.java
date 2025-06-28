package model;

public class Luggage {
    private String luggageId;        // luggage_ID
    private String orderId;          // order_ID
    private Integer luggageWeight;   // luggage_weight
    private Integer luggageNum;      // luggage_num
    private Integer luggagePrice;    // luggage_price

    public Luggage() {
    }

    public Luggage(String luggageId, String orderId) {
        this.luggageId = luggageId;
        this.orderId = orderId;
    }

    // Getter和Setter
    public String getLuggageId() { return luggageId; }
    public void setLuggageId(String luggageId) { this.luggageId = luggageId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Integer getLuggageWeight() { return luggageWeight; }
    public void setLuggageWeight(Integer luggageWeight) { this.luggageWeight = luggageWeight; }
    public Integer getLuggageNum() { return luggageNum; }
    public void setLuggageNum(Integer luggageNum) { this.luggageNum = luggageNum; }
    public Integer getLuggagePrice() { return luggagePrice; }
    public void setLuggagePrice(Integer luggagePrice) { this.luggagePrice = luggagePrice; }

    @Override
    public String toString() {
        return "Luggage{" +
                "luggageId='" + luggageId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", luggageWeight=" + luggageWeight +
                ", luggageNum=" + luggageNum +
                ", luggagePrice=" + luggagePrice +
                '}';
    }
}