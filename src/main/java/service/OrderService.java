package service;

import dao.*;
import model.*;
import dto.OrderDetailDTO;
import dto.OrderStatsDTO;
import service.BookingService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务类 - 基于现有BookingService扩展订单管理功能
 */
public class OrderService {

    private OrderDao orderDao;
    private PayrecordDao payrecordDao;
    private FlightDao flightDao;
    private UserDao userDao;
    private FlightrecordDao flightrecordDao;
    private BookingService bookingService; // 复用现有的订票服务

    public OrderService() {
        this.orderDao = new OrderDao();
        this.payrecordDao = new PayrecordDao();
        this.flightDao = new FlightDao();
        this.userDao = new UserDao();
        this.flightrecordDao = new FlightrecordDao();
        this.bookingService = new BookingService();
    }

    /**
     * 获取用户的详细订单信息（包含航班、支付信息）
     * @param userId 用户ID
     * @return 详细订单信息列表
     */
    public List<OrderDetailDTO> getUserOrderDetails(String userId) {
        try {
            // 获取用户所有订单
            List<Order> orders = orderDao.findByUserId(userId);
            List<OrderDetailDTO> orderDetails = new ArrayList<>();

            for (Order order : orders) {
                OrderDetailDTO detail = new OrderDetailDTO();

                // 基本订单信息
                detail.setOrderId(order.getOrderId());
                detail.setUserId(order.getUserId());
                detail.setFlightId(order.getFlightId());

                // 将LocalDate和LocalDateTime转换为字符串，并进行空值检查
                if (order.getFlightTime() != null) {
                    detail.setFlightTime(order.getFlightTime().toString()); // yyyy-MM-dd
                }
                if (order.getOrderTime() != null) {
                    detail.setOrderTime(order.getOrderTime().toString());   // yyyy-MM-ddTHH:mm:ss
                }

                detail.setOrderState(order.getOrderState());
                detail.setSeatId(order.getSeatId());
                detail.setSeatType(order.getSeatType());

                // 获取航班信息
                Flight flight = flightDao.findById(order.getFlightId());
                if (flight != null) {
                    detail.setFlightNumber(flight.getFlightId());
                    detail.setDepartureTime(flight.getTimeTakeoff().toString());
                    detail.setArrivalTime(flight.getTimeArrive().toString());

                    // 直接使用airportFrom和airportTo作为机场信息
                    detail.setDepartureAirport(flight.getAirportFrom());
                    detail.setArrivalAirport(flight.getAirportTo());
                    detail.setDepartureCity(extractCityFromAirport(flight.getAirportFrom()));
                    detail.setArrivalCity(extractCityFromAirport(flight.getAirportTo()));

                    // 设置票价
                    if (order.getSeatType() == 0) {
                        detail.setPrice(flight.getSeat0Price());
                    } else {
                        detail.setPrice(flight.getSeat1Price());
                    }

                    // 设置座位类型名称
                    detail.setSeatTypeName(order.getSeatType() == 0 ? "经济舱" : "商务舱");
                }

                // 获取支付信息
                Payrecord payrecord = payrecordDao.findByOrderId(order.getOrderId());
                if (payrecord != null) {
                    detail.setPayment(payrecord.getPayment());
                    detail.setPayMethod(payrecord.getPayMethod());
                    detail.setPayState(payrecord.getPayState());

                    // 将LocalDateTime转换为字符串
                    if (payrecord.getPayTime() != null) {
                        detail.setPayTime(payrecord.getPayTime().toString());
                    }

                    detail.setPayId(payrecord.getPayId());
                } else {
                    // 如果没有支付记录，设置默认值
                    detail.setPayment(detail.getPrice());
                    detail.setPayMethod("未选择");
                    detail.setPayState("未支付");
                    detail.setPayTime(null); // 未支付时支付时间为空
                }

                orderDetails.add(detail);
            }

            // 按订单时间倒序排列（最新的在前面）
            orderDetails.sort((o1, o2) -> o2.getOrderTime().compareTo(o1.getOrderTime()));

            System.out.println("📋 用户 " + userId + " 的订单详情查询完成，共 " + orderDetails.size() + " 条");
            return orderDetails;

        } catch (Exception e) {
            System.err.println("❌ 获取用户订单详情失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取用户订单详情失败", e);
        }
    }

    /**
     * 从机场名称中提取城市信息（简单的字符串处理）
     * @param airport 机场名称，如"北京大兴机场"
     * @return 城市名称，如"北京"
     */
    private String extractCityFromAirport(String airport) {
        if (airport == null || airport.isEmpty()) {
            return "未知城市";
        }

        // 简单的城市提取逻辑，可以根据实际需要完善
        if (airport.contains("北京")) return "北京";
        if (airport.contains("上海")) return "上海";
        if (airport.contains("广州")) return "广州";
        if (airport.contains("深圳")) return "深圳";
        if (airport.contains("杭州")) return "杭州";
        if (airport.contains("成都")) return "成都";
        if (airport.contains("西安")) return "西安";
        if (airport.contains("重庆")) return "重庆";
        if (airport.contains("南京")) return "南京";
        if (airport.contains("武汉")) return "武汉";

        // 如果包含"机场"，尝试提取前面的城市名
        if (airport.contains("机场")) {
            String city = airport.replace("机场", "");
            if (city.length() >= 2) {
                return city.substring(0, 2); // 取前两个字符作为城市名
            }
        }

        return airport; // 如果无法识别，返回原字符串
    }

    /**
     * 根据订单状态筛选订单
     * @param userId 用户ID
     * @param orderState 订单状态
     * @return 筛选后的订单列表
     */
    public List<OrderDetailDTO> getUserOrdersByState(String userId, String orderState) {
        List<OrderDetailDTO> allOrders = getUserOrderDetails(userId);

        if (orderState == null || "all".equals(orderState)) {
            return allOrders;
        }

        return allOrders.stream()
                .filter(order -> {
                    switch (orderState) {
                        case "pending":
                            return "未支付".equals(order.getPayState());
                        case "upcoming":
                            return "已支付".equals(order.getPayState()) &&
                                    order.getFlightTime() != null &&
                                    LocalDate.parse(order.getFlightTime()).isAfter(LocalDate.now());
                        case "completed":
                            return "已支付".equals(order.getPayState()) &&
                                    order.getFlightTime() != null &&
                                    LocalDate.parse(order.getFlightTime()).isBefore(LocalDate.now());
                        default:
                            return orderState.equals(order.getOrderState()) ||
                                    orderState.equals(order.getPayState());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取订单统计信息
     * @param userId 用户ID
     * @return 订单统计
     */
    public OrderStatsDTO getUserOrderStats(String userId) {
        List<OrderDetailDTO> orders = getUserOrderDetails(userId);

        OrderStatsDTO stats = new OrderStatsDTO();
        stats.setTotalCount(orders.size());

        long pendingCount = orders.stream().filter(o -> "未支付".equals(o.getPayState())).count();
        long upcomingCount = orders.stream().filter(o ->
                "已支付".equals(o.getPayState()) &&
                        o.getFlightTime() != null &&
                        LocalDate.parse(o.getFlightTime()).isAfter(LocalDate.now())).count();
        long completedCount = orders.stream().filter(o ->
                "已支付".equals(o.getPayState()) &&
                        o.getFlightTime() != null &&
                        LocalDate.parse(o.getFlightTime()).isBefore(LocalDate.now())).count();

        stats.setPendingCount((int) pendingCount);
        stats.setUpcomingCount((int) upcomingCount);
        stats.setCompletedCount((int) completedCount);

        return stats;
    }

    /**
     * 取消订单 - 复用BookingService的逻辑
     * @param orderId 订单ID
     * @param userId 用户ID
     * @return 是否成功
     */
    public boolean cancelOrder(String orderId, String userId) {
        try {
            return bookingService.cancelOrder(orderId, userId);
        } catch (Exception e) {
            System.err.println("❌ 取消订单失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 申请退款 - 复用BookingService的逻辑
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param reason 退款原因
     * @return 退款申请ID
     */
    public String requestRefund(String orderId, String userId, String reason) {
        try {
            return bookingService.requestRefund(orderId, userId, reason);
        } catch (Exception e) {
            System.err.println("❌ 申请退款失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 直接处理退款 - 更新订单状态和支付记录状态
     * @param orderId 订单ID
     * @param userId 用户ID
     * @param reason 退款原因
     * @return 是否处理成功
     */
    public boolean processRefund(String orderId, String userId, String reason) {
        try {
            System.out.println("💰 开始处理退款: 订单=" + orderId + ", 用户=" + userId + ", 原因=" + reason);

            // 1. 验证订单归属和状态
            Order order = orderDao.findById(orderId);
            if (order == null) {
                System.err.println("❌ 订单不存在: " + orderId);
                return false;
            }

            if (!order.getUserId().equals(userId)) {
                System.err.println("❌ 订单不属于该用户: " + orderId + " -> " + userId);
                return false;
            }

            // 检查订单状态（只有正常状态的订单可以退款）
            if (!"正常".equals(order.getOrderState()) && !"未支付".equals(order.getOrderState())) {
                System.err.println("❌ 订单状态不允许退款: " + order.getOrderState());
                return false;
            }

            // 2. 更新订单状态为已取消
            boolean orderUpdated = orderDao.updateOrderState(orderId, "已取消");
            if (!orderUpdated) {
                System.err.println("❌ 更新订单状态失败: " + orderId);
                return false;
            }
            System.out.println("✅ 订单状态已更新为已取消: " + orderId);

            // 3. 更新支付记录状态为已退款
            Payrecord payrecord = payrecordDao.findByOrderId(orderId);
            if (payrecord != null) {
                try {
                    payrecordDao.updateStatus(payrecord.getPayId(), "已退款");
                    System.out.println("✅ 支付记录状态已更新为已退款: " + payrecord.getPayId());
                } catch (Exception e) {
                    System.err.println("⚠️ 更新支付记录状态失败，但订单已取消: " + e.getMessage());
                    // 不影响整体退款流程
                }
            } else {
                System.out.println("ℹ️ 未找到支付记录，可能是未支付订单");
            }

            // 4. 释放座位（复用BookingService的逻辑）
            try {
                String flightrecordId = findFlightRecordId(order.getFlightId(), order.getFlightTime());
                if (flightrecordId != null) {
                    boolean seatReleased = bookingService.releaseSeat(flightrecordId, order.getSeatType());
                    if (seatReleased) {
                        System.out.println("✅ 座位已释放: " + flightrecordId + " 座位类型=" + order.getSeatType());
                    } else {
                        System.err.println("⚠️ 释放座位失败，但退款已处理");
                    }
                } else {
                    System.err.println("⚠️ 未找到航班记录，无法释放座位");
                }
            } catch (Exception e) {
                System.err.println("⚠️ 释放座位时出错: " + e.getMessage());
                // 不影响退款流程
            }

            System.out.println("🎉 退款处理完成: " + orderId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理退款异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据航班ID和日期查找航班记录ID
     * @param flightId 航班ID
     * @param flightDate 航班日期（LocalDate对象）
     * @return 航班记录ID
     */
    private String findFlightRecordId(String flightId, LocalDate flightDate) {
        try {
            if (flightDate == null) {
                return null;
            }
            return flightrecordDao.getFlightRecordId(flightId, flightDate);
        } catch (Exception e) {
            System.err.println("❌ 查找航班记录ID失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取订单详情 - 复用BookingService的逻辑
     * @param orderId 订单ID
     * @return 订单对象
     */
    public Order getOrderById(String orderId) {
        return bookingService.getOrderById(orderId);
    }

    /**
     * 更新订单状态 - 复用BookingService的逻辑
     * @param orderId 订单ID
     * @param newStatus 新状态
     * @return 是否成功
     */
    public boolean updateOrderStatus(String orderId, String newStatus) {
        return bookingService.updateOrderStatus(orderId, newStatus);
    }
}