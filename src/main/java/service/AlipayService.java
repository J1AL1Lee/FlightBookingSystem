package service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.google.gson.Gson;
import dao.PayrecordDao;
import model.Payrecord;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public class AlipayService {

    private final AlipayClient alipayClient;
    private final PayrecordDao payrecordDao;

    public AlipayService() {
        Properties props = new Properties();
        try (InputStream input = AlipayService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("❌ 无法找到 application.properties 文件");
            }
            props.load(input);
            AlipayConfig config = new AlipayConfig();
            config.setServerUrl(props.getProperty("alipay.server.url"));
            config.setAppId(props.getProperty("alipay.app.id"));
            config.setPrivateKey(props.getProperty("alipay.private.key"));
            config.setAlipayPublicKey(props.getProperty("alipay.public.key"));
            config.setCharset("UTF-8");
            config.setSignType("RSA2");
            try {
                this.alipayClient = new DefaultAlipayClient(config);
            } catch (AlipayApiException e) {
                System.err.println("❌ 支付宝客户端初始化失败: " + e.getMessage());
                throw new RuntimeException("支付宝客户端初始化失败", e); // 转换为 RuntimeException，适配现有结构
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ 加载支付宝配置失败: " + e.getMessage(), e);
        }
        this.payrecordDao = new PayrecordDao();
    }

    public Map<String, Object> createPayment(String orderId, double amount) throws AlipayApiException {
        String outTradeNo = generateUniqueTradeNo();

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", String.format("%.2f", amount)); // 动态金额
        bizContent.put("subject", "Flight Booking Payment for Order " + orderId);
        request.setBizContent(new Gson().toJson(bizContent));

        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();

        if (response.isSuccess()) {
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(response.getOutTradeNo());
            payrecord.setOrderId(orderId);
            payrecord.setPayment((int) (amount * 100)); // 转换为分
            payrecord.setPayMethod("Alipay");
            payrecord.setPayState("等待支付");
            payrecord.setPayTime(LocalDateTime.now());
            payrecordDao.save(payrecord);

            result.put("success", true);
            result.put("message", "支付创建成功");
            result.put("payId", response.getOutTradeNo());
            result.put("qrCode", response.getQrCode());
        } else {
            result.put("success", false);
            result.put("message", "支付创建失败: " + response.getMsg());
            result.put("errorCode", response.getCode());
        }
        return result;
    }

    public Map<String, Object> checkPaymentStatus(String payId) throws AlipayApiException {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent(new Gson().toJson(Map.of("out_trade_no", payId)));
        AlipayTradeQueryResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();
        if (response.isSuccess()) {
            String tradeStatus = response.getTradeStatus();
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord != null) {
                payrecord.setPayState(tradeStatus.equals("TRADE_SUCCESS") ? "已支付" : "等待支付");
                payrecordDao.save(payrecord);
            }
            result.put("success", true);
            result.put("status", tradeStatus);
            result.put("qrCode", tradeStatus.equals("TRADE_SUCCESS") ? null : payrecord.getPayId()); // 支付成功清空
        } else {
            result.put("success", false);
            result.put("message", "查询失败: " + response.getMsg());
        }
        return result;
    }

    private String generateUniqueTradeNo() {
        return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}