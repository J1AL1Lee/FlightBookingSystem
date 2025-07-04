package test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeCancelRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeCancelResponse;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import model.*;
import service.BookingService;
import service.FlightSearchService;
import dao.OrderDao;
import dao.FlightrecordDao;
import dao.UserDao;
import dao.PayrecordDao;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;

public class AlipayBookingTest {
    private static AlipayService alipayService;
    private static BookingService bookingService;
    private static MockOrderDao mockOrderDao;
    private static MockPayrecordDao mockPayrecordDao;
    private static MockFlightrecordDao mockFlightrecordDao;
    private static MockFlightSearchService mockFlightSearchService;
    private static MockUserDao mockUserDao;
    private static JFrame frame;
    private static JLabel qrCodeLabel;
    private static JTextArea orderInfoArea;
    private static JLabel statusLabel;
    private static JButton regenerateButton;
    private static String orderId;
    private static String payId;
    private static LocalDateTime paymentStartTime;
    private static final long TIMEOUT_MINUTES = 5;

    public static void main(String[] args) {
        try {
            System.out.println("🚀 支付宝订票测试开始...");

            // 初始化服务
            initializeServices();

            // 模拟数据
            initializeMockData();

            // 初始化 GUI
            initializeGui();

            // 运行测试
            runTest();

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeServices() throws Exception {
        mockOrderDao = new MockOrderDao();
        mockPayrecordDao = new MockPayrecordDao();
        mockFlightrecordDao = new MockFlightrecordDao();
        mockUserDao = new MockUserDao();
        mockFlightSearchService = new MockFlightSearchService(mockUserDao, mockFlightrecordDao);

        // 初始化 BookingService 并使用反射注入 mock DAO
        bookingService = new BookingService();
        injectMockDao(bookingService, "orderDao", mockOrderDao);
        injectMockDao(bookingService, "FlightrecordDao", mockFlightrecordDao);
        injectMockDao(bookingService, "flightSearchService", mockFlightSearchService);
        injectMockDao(bookingService, "userDao", mockUserDao);

        // 初始化 AlipayService
        alipayService = new AlipayService(mockPayrecordDao);
        System.out.println("✅ 服务初始化完成");
    }

    private static void injectMockDao(Object target, String fieldName, Object mockDao) throws Exception {
        Field field = BookingService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mockDao);
    }

    private static void initializeMockData() {
        User user = new User();
        user.setUserId("123456");
        user.setUserName("张三");
        user.setVipState("是");
        mockUserDao.save(user);

        Flight flight = new Flight();
        flight.setFlightId("00CA1234");
        flight.setAirlinecompanyId("CA");
        flight.setAirportFrom("PEK");
        flight.setAirportTo("HGH");
        flight.setTimeTakeoff(LocalTime.of(8, 0));
        flight.setTimeArrive(LocalTime.of(10, 30));
        flight.setSeat0Price(100000);
        flight.setSeat1Price(180000);
        flight.setSeat0Capacity(100);
        flight.setSeat1Capacity(20);
        flight.setDiscount(0.8f);
        mockFlightSearchService.save(flight);

        Flightrecord flightrecord = new Flightrecord();
        flightrecord.setFlightrecordId("FL12345620250701");
        flightrecord.setFlightId("00CA1234");
        flightrecord.setFlightDate(LocalDate.parse("2025-07-01"));
        flightrecord.setSeat0Left(50);
        flightrecord.setSeat1Left(10);
        mockFlightrecordDao.save(flightrecord);

        System.out.println("✅ 模拟数据初始化完成");
    }

    private static void initializeGui() {
        frame = new JFrame("支付宝订票测试");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 600);
        frame.setLayout(new BorderLayout());

        qrCodeLabel = new JLabel();
        qrCodeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        orderInfoArea = new JTextArea();
        orderInfoArea.setEditable(false);
        orderInfoArea.setRows(6);

        statusLabel = new JLabel("支付状态: 初始化中...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        regenerateButton = new JButton("重新生成二维码");
        regenerateButton.setEnabled(false);
        regenerateButton.addActionListener(e -> regenerateQrCode());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(orderInfoArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(statusLabel, BorderLayout.NORTH);
        bottomPanel.add(regenerateButton, BorderLayout.SOUTH);

        frame.add(qrCodeLabel, BorderLayout.NORTH);
        frame.add(topPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void runTest() {
        System.out.println("\n========== 支付宝订票测试流程 ==========");
        String flightrecordId = "FL12345620250701";
        String userId = "123456";
        int seatType = 0;

        // 1. 创建订单
        System.out.println("\n--- 步骤1：创建未支付订单 ---");
        orderId = bookingService.createUnpaidBooking(flightrecordId, userId, seatType);
        if (orderId == null) {
            System.err.println("❌ 订单创建失败");
            updateStatus("订单创建失败");
            return;
        }
        Order order = mockOrderDao.findById(orderId);
        updateOrderInfo(order);
        System.out.println("✅ 订单创建成功: " + order.toString());

        // 2. 计算价格
        Integer amount = bookingService.calculateOrderPrice(flightrecordId, seatType, userId);
        if (amount == null) {
            System.err.println("❌ 价格计算失败");
            updateStatus("价格计算失败");
            return;
        }
        System.out.println("💰 订单金额: ¥" + amount/100.0);

        // 3. 发起支付
        System.out.println("\n--- 步骤2：发起支付宝支付 ---");
        createPayment(orderId, amount);

        // 4. 轮询支付状态
        startPolling();
    }

    private static void createPayment(String orderId, Integer amount) {
        Map<String, Object> paymentResult = alipayService.createPayment(orderId, amount, "支付宝");
        if (paymentResult == null) {
            System.err.println("❌ 支付创建失败");
            updateStatus("支付创建失败");
            return;
        }
        payId = (String) paymentResult.get("payId");
        String qrCode = (String) paymentResult.get("qrCode");
        paymentStartTime = LocalDateTime.now();
        System.out.println("✅ 支付创建成功: payId=" + payId + ", qrCode=" + qrCode);

        updateQrCode(qrCode);
        updateStatus("支付状态: 等待支付（请用支付宝沙箱App扫描二维码）");
        regenerateButton.setEnabled(false);
    }

    private static void regenerateQrCode() {
        Order order = mockOrderDao.findById(orderId);
        if (order == null || !"未支付".equals(order.getOrderState())) {
            updateStatus("无法重新生成二维码：订单状态不正确");
            return;
        }
        Integer amount = bookingService.calculateOrderPrice("FL12345620250701", 0, "123456");
        createPayment(orderId, amount);
    }

    private static void startPolling() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (payId == null) {
                    timer.cancel();
                    return;
                }

                long minutesElapsed = java.time.Duration.between(paymentStartTime, LocalDateTime.now()).toMinutes();
                if (minutesElapsed >= TIMEOUT_MINUTES) {
                    System.out.println("⏰ 支付超时，取消支付");
                    alipayService.cancelPayment(payId);
                    bookingService.cancelOrder(orderId, "123456");
                    updateStatus("支付超时，订单已取消");
                    regenerateButton.setEnabled(true);
                    timer.cancel();
                    outputFinalResult();
                    return;
                }

                Map<String, Object> queryResult = alipayService.queryPayment(payId);
                String status = (String) queryResult.get("status");
                System.out.println("📊 支付状态: " + status);

                if ("TRADE_SUCCESS".equals(status)) {
                    bookingService.updateOrderStatus(orderId, "正常");
                    updateStatus("支付状态: 已支付");
                    updateOrderInfo(mockOrderDao.findById(orderId));
                    regenerateButton.setEnabled(false);
                    timer.cancel();
                    outputFinalResult();
                } else if ("TRADE_CLOSED".equals(status)) {
                    bookingService.cancelOrder(orderId, "123456");
                    updateStatus("支付状态: 已取消（交易关闭）");
                    regenerateButton.setEnabled(true);
                    timer.cancel();
                    outputFinalResult();
                } else {
                    updateStatus("支付状态: " + status);
                }
            }
        }, 0, 5000);
    }

    private static void updateQrCode(String qrCode) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(qrCode, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
            qrCodeLabel.setIcon(new ImageIcon(image));
        } catch (Exception e) {
            System.err.println("❌ 生成二维码失败: " + e.getMessage());
            updateStatus("生成二维码失败");
        }
    }

    private static void updateOrderInfo(Order order) {
        if (order == null) return;
        Payrecord payrecord = mockPayrecordDao.findById(payId);
        String info = String.format(
                "订单号: %s\n用户ID: %s\n航班号: %s\n座位类型: %s\n订单状态: %s\n总金额: ¥%d",
                order.getOrderId(), order.getUserId(), order.getFlightId(),
                order.getSeatType() == 0 ? "经济舱" : "商务舱", order.getOrderState(),
                payrecord != null ? payrecord.getPayment() /100 : 0
        );
        orderInfoArea.setText(info);
    }

    private static void updateStatus(String status) {
        statusLabel.setText(status);
    }

    private static void outputFinalResult() {
        System.out.println("\n--- 最终结果 ---");
        Order order = mockOrderDao.findById(orderId);
        Payrecord payrecord = mockPayrecordDao.findById(payId);
        System.out.println("📝 订单信息: " + (order != null ? order.toString() : "无"));
        System.out.println("💳 支付信息: " + (payrecord != null ? payrecord.toString() : "无"));
        System.out.println("🎉 测试流程结束");
    }
}

class AlipayService {
    private AlipayClient alipayClient;
    private PayrecordDao payrecordDao;

    public AlipayService(PayrecordDao payrecordDao) throws AlipayApiException {
        this.payrecordDao = payrecordDao;
        AlipayConfig config = new AlipayConfig();
        config.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        config.setAppId("9021000149697288");
        // 请替换为你的沙箱私钥和支付宝公钥
        config.setPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8j80DL2o11vTtip6bhPZNE+dmhQarBCSnYzD3TYLI4WYRA5FyYfw1LolsYjgOK+v4OeiMk31NLfrlXBZCpFEiEnMDU6bVlIMIN6uD/MtvrLyf0uepGLso01B8UrB7jja/F81kfutkeQVdxPsl67XRZ2qPax6HGBA8msaoggoEr2k/xd9tBByYwYbaF5UueSfpjj6FheOr4cFzFOy1POWPAYqRM6XxQK+w7i5xl+ah7IdmJsOyGRImdg4J3LHlQJ8XkjfsmzgqNq93WGVrXH0Egrgrx5VZm4IgVP6fZfSjhhxokkj/dhPk/idWYrURzSfiJ0PUvRMaL8Gzx7TuBKhHAgMBAAECggEBAKAIF4HdivG4xtSXsjbhaLxP6TNcMSWRdZ5Ok+8/bIEasyo7cgS23nswTNecoGB+rF1WoGQ2hMCtBmQEfKwAkw8sw0oOg+h+i5q8zKdPNEVKQCgQsiYUZDuo5IUvFLM4JoSWKe5hvVvfTkuf81riqsPXVlv0GMulA5q77WB0RRZlZ3+01Vhkz4PIeWce23endgd7RjGfNhpq7Z/OKtl7N+jChjGToUTL6s3TsEirbP9DcQYO1M8ETLFkYTXViMt3Z3lB3fMEcjX53cc9DqdbuRX0+K4HnPtBIlRKOvpf6+l8Vb6s6d8OTSWHuj1/1cegpE5M7d+bZ/VjPEFVF2FdayECgYEA6TFeR/rtSvmdowed6OdUmA1hgPJidaGuP3jsVY/hyNnSGhkbE7WssNmC7HXeVVjRC81iULaSr9kJc2Rh0O5EGrn0GarKFkA3kyBE50YVqSkIrCIpFtydFdNKvpAepQDu0BfoxJsDac2BfPrvCbS35J3HZ/sHrSNXqWlQV0Cm2fECgYEAzwD32/HdfVKby6IEpSVqW14v3yyPElShHQL7j3TluLneZbi9cN3eZxA9ebNRhe60wtoAGWOx0LS12fHWg2FsuTywcdhVTbzkElog33lzlLwEko6fgbE4ANL9q48vRmxptWDKivEl3pW9qgP+SLD/+xJBgjLto6KX0SHlxh3nrbcCgYBqFlqVFpQTsuHDRHjTd0Jl9lhwaFTgvRBfseyatF18mZPa6acG3XTV8+57Eth2LXTVELf0jkrHk06YX4ecnHkBS63Aa5GKc+aUmW6fZKQAFDnszZGx4+XXAwwTC8/VM0pyAx6TKw5veN269RIAcWXjrOAF7w879kMwQEgbmb8OkQKBgQCAY9FXkcQWns4SlwLai0JUOS7n9PMoI2VqYRc1+wMgd+gAn3ygLHxs4B3BBf9iWpOy5xN4q+T11Z+U9fJeumZ83a9ybQM7nBS5bT1GXkXZ0mPjoqI8Bnb9y9+aMMzZmRRXcxks5DTgwW9JrABjhaS/TKtk3cGW5JnVFHk3UAUKMQKBgE3PJY5gVB+Gpg+hElNV+IWrIZ6atDE5HEtMJrYjnuRSchuwkW2pIGwsOAya83LLvk6Of6V5oSM0Mn9LcKpgO++AiS85zIS61CHPhetirFDDqhqvzK6Xpp9rWrIUnOU4wHToQP4VWRiSPYkU/PpXcDEeOG7C4/IghkzpJfzNMqHP");
        config.setAlipayPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmlEmO339xgGfOex/PGyi7HyhD7XTYb3QpUmlK2Z2Qnk0eSQ2dtOOo19tdZgBnFM7n9ybBC4y/uchhvcV59tDx8c4h82ST8H7wf2TfldsfXIbk10/z5xuSeqnoQ4aXLx8UfXMywMLT+Ytpz2+75rTkWeE4/q1lCDBRZ+0qHcUmCTNy4Mg554lWfQ60XmpJawvOB0jjM2zlFggoG4V0ieJCdcI+3fnw1WP5/probDo1ZPSr3b9zu1Y3XoDO0smADp4+4NHuJEQCL7R06ModhemFUm+FI6V1H7pj1zrt2mR3pXD7CasVY3nEGLyO59oGSGvpPottKQem7IvpujoU+Do/wIDAQAB");
        config.setCharset("UTF-8");
        config.setSignType("RSA2");
        alipayClient = new DefaultAlipayClient(config);
        System.out.println("✅ 支付宝服务初始化成功");
    }

    public Map<String, Object> createPayment(String orderId, Integer amount, String payMethod) {
        try {
            String payId = generateUniquePayId();
            String outTradeNo = payId;
            String totalAmount = String.format("%.2f", amount / 100.0);

            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("total_amount", totalAmount);
            bizContent.put("subject", "航班预订-" + orderId);
            bizContent.put("timeout_express", "5m");
            request.setBizContent(new Gson().toJson(bizContent));

            AlipayTradePrecreateResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                System.err.println("❌ 支付创建失败: " + response.getMsg());
                return null;
            }

            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(payId);
            payrecord.setOrderId(orderId);
            payrecord.setPayment(amount);
            payrecord.setPayMethod(payMethod);
            payrecord.setPayState("处理中");
            payrecord.setPayTime(LocalDateTime.now());
            payrecordDao.save(payrecord);

            Map<String, Object> result = new HashMap<>();
            result.put("payId", payId);
            result.put("qrCode", response.getQrCode());
            result.put("orderId", orderId);
            result.put("payMethod", payMethod);
            result.put("payState", "处理中");
            return result;

        } catch (AlipayApiException e) {
            System.err.println("❌ 支付宝API异常: " + e.getMessage());
            return null;
        }
    }

    public boolean processPaymentSuccess(String payId) {
        try {
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) return false;

            payrecord.setPayState("已支付");
            payrecord.setPayTime(LocalDateTime.now());
            payrecordDao.save(payrecord);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理支付成功失败: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> queryPayment(String payId) {
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", payId);
            request.setBizContent(new Gson().toJson(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(request);
            Map<String, Object> result = new HashMap<>();
            result.put("payId", payId);
            result.put("status", response.isSuccess() ? response.getTradeStatus() : "查询失败");
            if (response.isSuccess()) {
                Payrecord payrecord = payrecordDao.findById(payId);
                payrecord.setPayState(response.getTradeStatus().equals("TRADE_SUCCESS") ? "已支付" : "处理中");
                payrecordDao.save(payrecord);
            }
            return result;

        } catch (AlipayApiException e) {
            System.err.println("❌ 查询支付状态失败: " + e.getMessage());
            return Map.of("payId", payId, "status", "查询失败");
        }
    }

    public boolean cancelPayment(String payId) {
        try {
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) return false;

            AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", payId);
            request.setBizContent(new Gson().toJson(bizContent));

            AlipayTradeCancelResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                payrecord.setPayState("已取消");
                payrecordDao.save(payrecord);
                return true;
            }
            return false;

        } catch (AlipayApiException e) {
            System.err.println("❌ 取消支付失败: " + e.getMessage());
            return false;
        }
    }

    private String generateUniquePayId() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%06d", random.nextInt(1000000));
        return "PAY" + date + randomNum;
    }
}

class MockOrderDao extends OrderDao {
    private Map<String, Order> orders = new HashMap<>();

    @Override
    public String save(Order order) {
        orders.put(order.getOrderId(), order);
        System.out.println("✅ Mock 订单保存成功: " + order.getOrderId());
        return order.getOrderId();
    }

    @Override
    public Order findById(String orderId) {
        return orders.get(orderId);
    }

    @Override
    public boolean updateOrderState(String orderId, String state) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setOrderState(state);
            System.out.println("✅ Mock 订单状态更新成功: " + orderId + " -> " + state);
            return true;
        }
        return false;
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return new ArrayList<>(orders.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean deleteOrder(String orderId) {
        return orders.remove(orderId) != null;
    }

    @Override
    public boolean isOrderOwnedByUser(String orderId, String userId) {
        Order order = orders.get(orderId);
        return order != null && order.getUserId().equals(userId);
    }

    @Override
    public List<Order> findByFlightAndDate(String flightId, LocalDate flightDate) {
        return new ArrayList<>(orders.values().stream()
                .filter(o -> o.getFlightId().equals(flightId) && o.getFlightTime().equals(flightDate))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Order> findByOrderState(String orderState) {
        return new ArrayList<>(orders.values().stream()
                .filter(o -> o.getOrderState().equals(orderState))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Order> findByUserIdAndDateRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return new ArrayList<>(orders.values().stream()
                .filter(o -> o.getUserId().equals(userId) &&
                        !o.getOrderTime().isBefore(startTime) &&
                        !o.getOrderTime().isAfter(endTime))
                .collect(Collectors.toList()));
    }

    @Override
    public int countOrdersByUserId(String userId) {
        return (int) orders.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .count();
    }

    @Override
    public boolean isOrderIdExists(String orderId) {
        return orders.containsKey(orderId);
    }

    @Override
    public int batchUpdateOrderState(List<String> orderIds, String newState) {
        int successCount = 0;
        for (String orderId : orderIds) {
            if (updateOrderState(orderId, newState)) {
                successCount++;
            }
        }
        System.out.println("✅ Mock 批量更新订单状态完成: " + successCount + "/" + orderIds.size() + " 成功");
        return successCount;
    }

    @Override
    public List<Order> findTimeoutUnpaidOrders(int timeoutMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return new ArrayList<>(orders.values().stream()
                .filter(o -> "未支付".equals(o.getOrderState()) && o.getOrderTime().isBefore(cutoffTime))
                .collect(Collectors.toList()));
    }
}

class MockPayrecordDao extends PayrecordDao {
    private Map<String, Payrecord> payrecords = new HashMap<>();

    @Override
    public String save(Payrecord payrecord) {
        payrecords.put(payrecord.getPayId(), payrecord);
        System.out.println("✅ Mock 支付记录保存成功: " + payrecord.getPayId());
        return payrecord.getPayId();
    }

    @Override
    public Payrecord findById(String payId) {
        return payrecords.get(payId);
    }

    @Override
    public void updateStatus(String payId, String status) {
        Payrecord payrecord = payrecords.get(payId);
        if (payrecord != null) {
            payrecord.setPayState(status);
            payrecord.setPayTime(LocalDateTime.now());
            System.out.println("✅ Mock 支付状态更新成功: " + payId + " -> " + status);
        }
    }

    @Override
    public Payrecord findByOrderId(String orderId) {
        return payrecords.values().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Payrecord> findByUserId(String userId) {
        return new ArrayList<>(payrecords.values());
    }

    @Override
    public List<Payrecord> findByPayState(String payState) {
        return new ArrayList<>(payrecords.values().stream()
                .filter(p -> p.getPayState().equals(payState))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Payrecord> findByPayMethod(String payMethod) {
        return new ArrayList<>(payrecords.values().stream()
                .filter(p -> p.getPayMethod().equals(payMethod))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Payrecord> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return new ArrayList<>(payrecords.values().stream()
                .filter(p -> !p.getPayTime().isBefore(startTime) && !p.getPayTime().isAfter(endTime))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean isPayIdExists(String payId) {
        return payrecords.containsKey(payId);
    }

    @Override
    public boolean hasPaymentRecord(String orderId) {
        return payrecords.values().stream().anyMatch(p -> p.getOrderId().equals(orderId));
    }

    @Override
    public int getTotalPaymentByUserId(String userId) {
        return payrecords.values().stream()
                .filter(p -> p.getPayState().equals("已支付"))
                .mapToInt(Payrecord::getPayment)
                .sum();
    }

    @Override
    public int getTotalIncomeByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return payrecords.values().stream()
                .filter(p -> p.getPayState().equals("已支付") &&
                        !p.getPayTime().isBefore(startTime) &&
                        !p.getPayTime().isAfter(endTime))
                .mapToInt(Payrecord::getPayment)
                .sum();
    }

    @Override
    public List<PayrecordDao.PayMethodStats> getPayMethodStatistics() {
        Map<String, PayMethodStats> statsMap = new HashMap<>();
        payrecords.values().stream()
                .filter(p -> p.getPayState().equals("已支付"))
                .forEach(p -> {
                    PayMethodStats stats = statsMap.computeIfAbsent(p.getPayMethod(), k -> new PayMethodStats());
                    stats.payMethod = p.getPayMethod();
                    stats.count++;
                    stats.totalAmount += p.getPayment();
                });
        return new ArrayList<>(statsMap.values());
    }

    @Override
    public boolean deletePayrecord(String payId) {
        return payrecords.remove(payId) != null;
    }

    @Override
    public int batchUpdatePayState(List<String> payIds, String newState) {
        int successCount = 0;
        for (String payId : payIds) {
            Payrecord payrecord = payrecords.get(payId);
            if (payrecord != null) {
                payrecord.setPayState(newState);
                payrecord.setPayTime(LocalDateTime.now());
                successCount++;
            }
        }
        System.out.println("✅ Mock 批量更新支付状态完成: " + successCount + "/" + payIds.size() + " 成功");
        return successCount;
    }
}

class MockFlightrecordDao extends FlightrecordDao {
    private Map<String, Flightrecord> flightrecords = new HashMap<>();

    @Override
    public String save(Flightrecord flightrecord) {
        flightrecords.put(flightrecord.getFlightrecordId(), flightrecord);
        System.out.println("✅ Mock 航程记录保存成功: " + flightrecord.getFlightrecordId());
        return flightrecord.getFlightrecordId();
    }

    @Override
    public Flightrecord findById(String flightrecordId) {
        return flightrecords.get(flightrecordId);
    }

    @Override
    public Flightrecord findByFlightAndDate(String flightId, LocalDate flightDate) {
        return flightrecords.values().stream()
                .filter(r -> r.getFlightId().equals(flightId) && r.getFlightDate().equals(flightDate))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Flightrecord> findByFlightAndDateRange(String flightId, LocalDate startDate, LocalDate endDate) {
        return new ArrayList<>(flightrecords.values().stream()
                .filter(r -> r.getFlightId().equals(flightId) &&
                        !r.getFlightDate().isBefore(startDate) &&
                        !r.getFlightDate().isAfter(endDate))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Flightrecord> findByDate(LocalDate flightDate) {
        return new ArrayList<>(flightrecords.values().stream()
                .filter(r -> r.getFlightDate().equals(flightDate))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Flightrecord> findAvailableFlightrecords(LocalDate flightDate, int seatType, int requiredSeats) {
        return new ArrayList<>(flightrecords.values().stream()
                .filter(r -> r.getFlightDate().equals(flightDate) &&
                        (seatType == 0 ? r.getSeat0Left() : r.getSeat1Left()) >= requiredSeats)
                .collect(Collectors.toList()));
    }

    @Override
    public boolean updateSeatCount(String flightrecordId, int seatType, int seatCount) {
        Flightrecord record = flightrecords.get(flightrecordId);
        if (record == null) return false;
        if (seatType == 0) {
            record.setSeat0Left(record.getSeat0Left() + seatCount);
            System.out.println("✅ Mock 座位数更新成功: " + flightrecordId + " 经济舱");
            return true;
        } else if (seatType == 1) {
            record.setSeat1Left(record.getSeat1Left() + seatCount);
            System.out.println("✅ Mock 座位数更新成功: " + flightrecordId + " 商务舱");
            return true;
        }
        return false;
    }

    @Override
    public boolean bookSeats(String flightrecordId, int seatType, int seatCount) {
        Flightrecord record = flightrecords.get(flightrecordId);
        if (record == null) return false;
        int availableSeats = (seatType == 0) ? record.getSeat0Left() : record.getSeat1Left();
        if (availableSeats < seatCount) return false;
        return updateSeatCount(flightrecordId, seatType, -seatCount);
    }

    @Override
    public boolean cancelSeats(String flightrecordId, int seatType, int seatCount) {
        return updateSeatCount(flightrecordId, seatType, seatCount);
    }

    @Override
    public boolean update(Flightrecord flightrecord) {
        flightrecords.put(flightrecord.getFlightrecordId(), flightrecord);
        System.out.println("✅ Mock 航程记录更新成功: " + flightrecord.getFlightrecordId());
        return true;
    }

    @Override
    public boolean deleteById(String flightrecordId) {
        return flightrecords.remove(flightrecordId) != null;
    }

    @Override
    public boolean existsById(String flightrecordId) {
        return flightrecords.containsKey(flightrecordId);
    }

    @Override
    public boolean existsByFlightAndDate(String flightId, LocalDate flightDate) {
        return flightrecords.values().stream()
                .anyMatch(r -> r.getFlightId().equals(flightId) && r.getFlightDate().equals(flightDate));
    }

    @Override
    public int countByDate(LocalDate flightDate) {
        return (int) flightrecords.values().stream()
                .filter(r -> r.getFlightDate().equals(flightDate))
                .count();
    }

    @Override
    public String createFlightrecordFromFlight(String flightId, LocalDate flightDate, int seat0Capacity, int seat1Capacity) {
        String flightrecordId = FlightrecordDao.generateFlightrecordId(flightId, flightDate);
        if (existsById(flightrecordId)) return null;
        Flightrecord record = new Flightrecord();
        record.setFlightrecordId(flightrecordId);
        record.setFlightId(flightId);
        record.setFlightDate(flightDate);
        record.setSeat0Left(seat0Capacity);
        record.setSeat1Left(seat1Capacity);
        return save(record);
    }

    @Override
    public String getFlightRecordId(String flightId, LocalDate flightDate) {
        return flightrecords.values().stream()
                .filter(r -> r.getFlightId().equals(flightId) && r.getFlightDate().equals(flightDate))
                .map(Flightrecord::getFlightrecordId)
                .findFirst()
                .orElse(null);
    }

    @Override
    public int batchSave(List<Flightrecord> flightrecords) {
        int savedCount = 0;
        for (Flightrecord record : flightrecords) {
            if (save(record) != null) savedCount++;
        }
        System.out.println("✅ Mock 批量保存航程记录成功: " + savedCount + "/" + flightrecords.size());
        return savedCount;
    }
}

class MockUserDao extends UserDao {
    private Map<String, User> users = new HashMap<>();

    @Override
    public String save(User user) {
        users.put(user.getUserId(), user);
        System.out.println("✅ Mock 用户保存成功: " + user.getUserId());
        return user.getUserId();
    }

    @Override
    public User findByUserId(String userId) {
        return users.get(userId);
    }

    @Override
    public User findById(String userId) {
        return users.get(userId);
    }

    @Override
    public User login(String userId, String password) {
        User user = users.get(userId);
        if (user != null && user.getUserPassword().equals(password)) {
            return user;
        }
        return null;
    }

    @Override
    public boolean existsByUserId(String userId) {
        return users.containsKey(userId);
    }

    @Override
    public List<User> findByUserName(String userName) {
        return new ArrayList<>(users.values().stream()
                .filter(u -> u.getUserName().equals(userName))
                .collect(Collectors.toList()));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}

class MockFlightSearchService extends FlightSearchService {
    private Map<String, Flight> flights = new HashMap<>();
    private UserDao userDao;
    private FlightrecordDao flightrecordDao;

    public MockFlightSearchService(UserDao userDao, FlightrecordDao flightrecordDao) {
        this.userDao = userDao;
        this.flightrecordDao = flightrecordDao;
    }

    @Override
    public Flight getFlightById(String flightId) {
        return flights.get(flightId);
    }

    @Override
    public List<Flight> searchFlightsByIdPattern(String flightIdPattern) {
        return new ArrayList<>(flights.values().stream()
                .filter(f -> f.getFlightId().contains(flightIdPattern))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Flight> searchFlightsByIdPrefix(String prefix) {
        return new ArrayList<>(flights.values().stream()
                .filter(f -> f.getFlightId().startsWith(prefix))
                .collect(Collectors.toList()));
    }

    @Override
    public List<Flight> smartSearchFlightId(String searchTerm) {
        Flight exactMatch = getFlightById(searchTerm);
        if (exactMatch != null) {
            return List.of(exactMatch);
        }
        return searchFlightsByIdPattern(searchTerm);
    }

    @Override
    public List<FlightSearchService.FlightSearchResult> searchAvailableFlights(
            String airportFrom, String airportTo, LocalDate flightDate, String userId) {
        List<FlightSearchResult> results = new ArrayList<>();
        for (Flight flight : flights.values()) {
            if (flight.getAirportFrom().equals(airportFrom) && flight.getAirportTo().equals(airportTo)) {
                Flightrecord record = flightrecordDao.findByFlightAndDate(flight.getFlightId(), flightDate);
                if (record != null) {
                    boolean isVipUser = userDao.findByUserId(userId) != null &&
                            "是".equals(userDao.findByUserId(userId).getVipState());
                    results.add(new FlightSearchResult(flight, record, new Airlinecompany(), isVipUser));
                }
            }
        }
        return results;
    }

    @Override
    public List<FlightSearchService.FlightSearchResult> searchFlightsWithAvailableSeats(
            String airportFrom, String airportTo, LocalDate flightDate, int seatType, int requiredSeats, String userId) {
        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        return allFlights.stream()
                .filter(r -> (seatType == 0 ? r.getSeat0Left() : r.getSeat1Left()) >= requiredSeats)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightSearchService.FlightSearchResult> searchFlightsByPrice(
            String airportFrom, String airportTo, LocalDate flightDate, int minPrice, int maxPrice, int seatType, String userId) {
        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        return allFlights.stream()
                .filter(r -> {
                    int price = seatType == 0 ? r.getFinalPrice0() : r.getFinalPrice1();
                    return price >= minPrice && price <= maxPrice;
                })
                .collect(Collectors.toList());
    }

    @Override
    public FlightSearchService.FlightSearchResult findCheapestFlight(
            String airportFrom, String airportTo, LocalDate flightDate, int seatType, String userId) {
        List<FlightSearchResult> flights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        FlightSearchResult cheapest = null;
        int minPrice = Integer.MAX_VALUE;
        for (FlightSearchResult result : flights) {
            int availableSeats = (seatType == 0) ? result.getSeat0Left() : result.getSeat1Left();
            if (availableSeats > 0) {
                int price = (seatType == 0) ? result.getFinalPrice0() : result.getFinalPrice1();
                if (price < minPrice) {
                    minPrice = price;
                    cheapest = result;
                }
            }
        }
        return cheapest;
    }

    @Override
    public List<FlightSearchService.FlightSearchResult> searchVipDiscountFlights(
            String airportFrom, String airportTo, LocalDate flightDate, String userId) {
        List<FlightSearchResult> allFlights = searchAvailableFlights(airportFrom, airportTo, flightDate, userId);
        return allFlights.stream()
                .filter(r -> r.getDiscount() < 1.0f)
                .collect(Collectors.toList());
    }

    @Override
    public boolean cancelBooking(String flightrecordId, int seatType, int seatCount) {
        return flightrecordDao.cancelSeats(flightrecordId, seatType, seatCount);
    }

    @Override
    public int initializeFlightrecordsForDate(LocalDate flightDate) {
        int savedCount = 0;
        for (Flight flight : flights.values()) {
            if (!flightrecordDao.existsByFlightAndDate(flight.getFlightId(), flightDate)) {
                String recordId = FlightrecordDao.generateFlightrecordId(flight.getFlightId(), flightDate);
                Flightrecord record = new Flightrecord();
                record.setFlightrecordId(recordId);
                record.setFlightId(flight.getFlightId());
                record.setFlightDate(flightDate);
                record.setSeat0Left(flight.getSeat0Capacity());
                record.setSeat1Left(flight.getSeat1Capacity());
                if (flightrecordDao.save(record) != null) {
                    savedCount++;
                }
            }
        }
        return savedCount;
    }

    public void save(Flight flight) {
        flights.put(flight.getFlightId(), flight);
    }
}