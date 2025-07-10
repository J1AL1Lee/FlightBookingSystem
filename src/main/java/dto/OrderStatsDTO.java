package dto;

public class OrderStatsDTO {
    private int totalCount;      // 总订单数
    private int pendingCount;    // 待支付订单数
    private int upcomingCount;   // 未出行订单数
    private int completedCount;  // 已完成订单数

    // 构造函数
    public OrderStatsDTO() {}

    public OrderStatsDTO(int totalCount, int pendingCount, int upcomingCount, int completedCount) {
        this.totalCount = totalCount;
        this.pendingCount = pendingCount;
        this.upcomingCount = upcomingCount;
        this.completedCount = completedCount;
    }

    // Getters and Setters
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getUpcomingCount() { return upcomingCount; }
    public void setUpcomingCount(int upcomingCount) { this.upcomingCount = upcomingCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    @Override
    public String toString() {
        return "OrderStatsDTO{" +
                "totalCount=" + totalCount +
                ", pendingCount=" + pendingCount +
                ", upcomingCount=" + upcomingCount +
                ", completedCount=" + completedCount +
                '}';
    }
}
