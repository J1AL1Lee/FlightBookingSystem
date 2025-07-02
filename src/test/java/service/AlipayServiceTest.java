package service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import dao.OrderDao;
import dao.PayrecordDao;
import model.Order;
import model.Payrecord;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.SimpleHttpServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AlipayServiceTest {

    @Mock
    private AlipayClient alipayClient;

    @Mock
    private PayrecordDao payrecordDao;

    @Mock
    private OrderDao orderDao;

    private static SimpleHttpServer server;
    private static int serverPort = 8080;

    private AlipayService alipayService;

    // JavaFX 应用
    static class QrDisplayApp extends Application {
        private ImageView imageView = new ImageView();
        private String payId;

        @Override
        public void start(Stage primaryStage) {
            StackPane root = new StackPane(imageView);
            Scene scene = new Scene(root, 300, 300);
            primaryStage.setScene(scene);
            primaryStage.setTitle("动态二维码");
            primaryStage.show();

            // 启动状态轮询
            new Thread(() -> pollPaymentStatus()).start();
        }

        public void updateQrCode(String qrUrl) {
            Platform.runLater(() -> {
                imageView.setImage(new Image(qrUrl, 250, 250, true, true));
            });
        }

        public void setPayId(String payId) {
            this.payId = payId;
        }

        private void pollPaymentStatus() {
            while (true) {
                try {
                    Thread.sleep(5000); // 每 5 秒检查一次
                    URL url = new URL("http://localhost:" + serverPort + "/api/payments/status?payId=" + payId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                            String line;
                            StringBuilder response = new StringBuilder();
                            while ((line = br.readLine()) != null) {
                                response.append(line.trim());
                            }
                            if (response.toString().contains("\"status\":\"TRADE_SUCCESS\"")) {
                                Platform.runLater(() -> {
                                    imageView.setImage(null);
                                    Stage stage = (Stage) imageView.getScene().getWindow();
                                    stage.setTitle("支付成功");
                                });
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @BeforeAll
    public static void startServer() throws Exception {
        // 检测端口并启动服务器
        while (true) {
            try (var socket = new java.net.ServerSocket(serverPort)) {
                server = new SimpleHttpServer();
                server.start(); // 假设 SimpleHttpServer 有 start 方法
                System.out.println("Server started on port " + serverPort);
                break;
            } catch (java.net.BindException e) {
                System.out.println("端口 " + serverPort + " 已被占用，尝试 " + ++serverPort);
            } catch (Exception e) {
                throw new Exception("服务器启动失败: " + e.getMessage(), e);
            }
        }
        Thread.sleep(3000); // 等待服务器就绪
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop(0); // 假设有 stop 方法
        }
    }

    @BeforeEach
    public void setUp() throws AlipayApiException {
        MockitoAnnotations.openMocks(this);
        alipayService = new AlipayService(alipayClient, payrecordDao);

        // 预置订单
        Order order = new Order();
        order.setOrderId("ORDER001");
        order.setOrderState("未支付");
        when(orderDao.findById("ORDER001")).thenReturn(order);

        // 模拟成功响应
        AlipayTradePrecreateResponse precreateSuccess = mock(AlipayTradePrecreateResponse.class);
        when(precreateSuccess.isSuccess()).thenReturn(true);
        when(precreateSuccess.getOutTradeNo()).thenReturn("ORDER_123456789");
        when(precreateSuccess.getQrCode()).thenReturn("https://qr.alipay.com/123456789");
        when(precreateSuccess.getMsg()).thenReturn("成功");

        AlipayTradeQueryResponse querySuccess = mock(AlipayTradeQueryResponse.class);
        when(querySuccess.isSuccess()).thenReturn(true);
        when(querySuccess.getTradeStatus()).thenReturn("TRADE_SUCCESS");
        when(querySuccess.getMsg()).thenReturn("查询成功");

        // 模拟失败响应
        AlipayTradePrecreateResponse precreateFailure = mock(AlipayTradePrecreateResponse.class);
        when(precreateFailure.isSuccess()).thenReturn(false);
        when(precreateFailure.getMsg()).thenReturn("支付失败");

        AlipayTradeQueryResponse queryFailure = mock(AlipayTradeQueryResponse.class);
        when(queryFailure.isSuccess()).thenReturn(false);
        when(queryFailure.getMsg()).thenReturn("查询失败");

        // 配置 execute 方法
        when(alipayClient.execute(any(AlipayTradePrecreateRequest.class)))
                .thenReturn(precreateSuccess)
                .thenReturn(precreateFailure);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenReturn(querySuccess)
                .thenReturn(queryFailure);
    }

    @Test
    public void testCreatePayment_WebSuccess() throws Exception {
        Payrecord payrecord = new Payrecord();
        when(payrecordDao.save(any(Payrecord.class))).thenReturn("ORDER_123456789");

        // 发起支付请求
        URL url = new URL("http://localhost:" + serverPort + "/api/payments/create");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        String jsonInputString = "{\"orderId\": \"ORDER001\", \"amount\": 10.50}";
        try (var os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }
            fail("请求失败，状态码: " + responseCode + ", 响应: " + response.toString());
        }

        // 提取二维码 URL 和 payId
        String htmlResponse = response.toString();
        String qrCodeUrl = htmlResponse.contains("https://qr.alipay.com/123456789") ? "https://qr.alipay.com/123456789" : null;
        String payId = "ORDER_123456789"; // 简化提取，需解析 HTML 或 JSON

        assertNotNull(qrCodeUrl);
        assertEquals(200, responseCode);
        verify(payrecordDao).save(any(Payrecord.class));

        // 启动 JavaFX 窗口显示二维码
        QrDisplayApp app = new QrDisplayApp();
        app.setPayId(payId);
        Platform.startup(() -> app.updateQrCode(qrCodeUrl));
    }

    @Test
    public void testCreatePayment_Failure() throws AlipayApiException {
        reset(alipayClient);
        AlipayTradePrecreateResponse precreateFailure = mock(AlipayTradePrecreateResponse.class);
        when(precreateFailure.isSuccess()).thenReturn(false);
        when(precreateFailure.getMsg()).thenReturn("支付失败");
        when(alipayClient.execute(any(AlipayTradePrecreateRequest.class))).thenReturn(precreateFailure);

        Map<String, Object> result = alipayService.createPayment("ORDER002", 10.50);

        assertFalse((boolean) result.get("success"));
        assertEquals("支付创建失败: 支付失败", result.get("message"));
        verify(payrecordDao, never()).save(any(Payrecord.class));
    }

    @Test
    public void testCreatePayment_Exception() throws AlipayApiException {
        reset(alipayClient);
        when(alipayClient.execute(any(AlipayTradePrecreateRequest.class)))
                .thenThrow(new AlipayApiException("模拟API异常"));

        AlipayApiException exception = assertThrows(AlipayApiException.class, () -> {
            alipayService.createPayment("ORDER003", 10.50);
        });
        assertEquals("模拟API异常", exception.getMessage());
    }

    @Test
    public void testCheckPaymentStatus_Success() throws AlipayApiException {
        Payrecord payrecord = new Payrecord();
        payrecord.setPayId("ORDER_123456789");
        when(payrecordDao.findById("ORDER_123456789")).thenReturn(payrecord);
        when(payrecordDao.save(any(Payrecord.class))).thenReturn("ORDER_123456789");

        Map<String, Object> result = alipayService.checkPaymentStatus("ORDER_123456789");

        assertTrue((boolean) result.get("success"));
        assertEquals("TRADE_SUCCESS", result.get("status"));
        assertNull(result.get("qrCode"));
        verify(payrecordDao).save(payrecord);
    }

    @Test
    public void testCheckPaymentStatus_Failure() throws AlipayApiException {
        reset(alipayClient);
        AlipayTradeQueryResponse queryFailure = mock(AlipayTradeQueryResponse.class);
        when(queryFailure.isSuccess()).thenReturn(false);
        when(queryFailure.getMsg()).thenReturn("查询失败");
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(queryFailure);

        Map<String, Object> result = alipayService.checkPaymentStatus("ORDER_123456790");

        assertFalse((boolean) result.get("success"));
        assertEquals("查询失败: 查询失败", result.get("message"));
        verify(payrecordDao, never()).save(any(Payrecord.class));
    }

    @Test
    public void testCheckPaymentStatus_Exception() throws AlipayApiException {
        reset(alipayClient);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class)))
                .thenThrow(new AlipayApiException("模拟API异常"));

        AlipayApiException exception = assertThrows(AlipayApiException.class, () -> {
            alipayService.checkPaymentStatus("ORDER_123456791");
        });
        assertEquals("模拟API异常", exception.getMessage());
    }
}