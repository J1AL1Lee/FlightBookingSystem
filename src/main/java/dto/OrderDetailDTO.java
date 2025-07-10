package dto;

/**
 * 订单详情DTO - 包含完整的订单显示信息
 * 使用字符串类型的日期字段，避免JSON序列化问题
 */
public class OrderDetailDTO {
    private String orderId;          // 订单号
    private String userId;           // 用户ID
    private String flightId;         // 航班号
    private String flightNumber;     // 航班号（显示用）
    private String flightTime;       // 航班日期（字符串格式：yyyy-MM-dd）
    private String orderTime;        // 下单时间（字符串格式：yyyy-MM-dd HH:mm:ss）
    private String orderState;       // 订单状态

    // 航班信息
    private String departureTime;    // 起飞时间
    private String arrivalTime;      // 到达时间
    private String departureAirport; // 出发机场
    private String arrivalAirport;   // 到达机场
    private String departureCity;    // 出发城市
    private String arrivalCity;      // 到达城市

    // 座位信息
    private int seatId;              // 座位ID
    private int seatType;            // 座位类型（0-经济舱, 1-商务舱）
    private String seatTypeName;     // 座位类型名称
    private int price;               // 票价

    // 支付信息
    private String payId;            // 支付ID
    private int payment;             // 支付金额
    private String payMethod;        // 支付方式
    private String payState;         // 支付状态
    private String payTime;          // 支付时间（字符串格式：yyyy-MM-dd HH:mm:ss）

    // 构造函数
    public OrderDetailDTO() {}

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getFlightTime() { return flightTime; }
    public void setFlightTime(String flightTime) { this.flightTime = flightTime; }

    public String getOrderTime() { return orderTime; }
    public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

    public String getOrderState() { return orderState; }
    public void setOrderState(String orderState) { this.orderState = orderState; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }

    public String getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }

    public String getDepartureCity() { return departureCity; }
    public void setDepartureCity(String departureCity) { this.departureCity = departureCity; }

    public String getArrivalCity() { return arrivalCity; }
    public void setArrivalCity(String arrivalCity) { this.arrivalCity = arrivalCity; }

    public int getSeatId() { return seatId; }
    public void setSeatId(int seatId) { this.seatId = seatId; }

    public int getSeatType() { return seatType; }
    public void setSeatType(int seatType) { this.seatType = seatType; }

    public String getSeatTypeName() { return seatTypeName; }
    public void setSeatTypeName(String seatTypeName) { this.seatTypeName = seatTypeName; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getPayId() { return payId; }
    public void setPayId(String payId) { this.payId = payId; }

    public int getPayment() { return payment; }
    public void setPayment(int payment) { this.payment = payment; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }

    public String getPayState() { return payState; }
    public void setPayState(String payState) { this.payState = payState; }

    public String getPayTime() { return payTime; }
    public void setPayTime(String payTime) { this.payTime = payTime; }

    @Override
    public String toString() {
        return "OrderDetailDTO{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", flightId='" + flightId + '\'' +
                ", flightTime='" + flightTime + '\'' +
                ", orderTime='" + orderTime + '\'' +
                ", orderState='" + orderState + '\'' +
                ", departureAirport='" + departureAirport + '\'' +
                ", arrivalAirport='" + arrivalAirport + '\'' +
                ", payment=" + payment +
                ", payMethod='" + payMethod + '\'' +
                ", payState='" + payState + '\'' +
                '}';
    }
}