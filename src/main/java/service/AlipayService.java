package service;

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
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import dao.PayrecordDao;
import model.Payrecord;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlipayService {

    private final AlipayClient alipayClient;
    private final PayrecordDao payrecordDao;

    public AlipayService(AlipayClient alipayClient, PayrecordDao payrecordDao) {
        this.alipayClient = alipayClient;
        this.payrecordDao = payrecordDao;
    }

    public Map<String, Object> createPayment(String orderId, double amount) throws AlipayApiException {
        String outTradeNo = "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", String.format("%.2f", amount));
        bizContent.put("subject", "Flight Booking Payment for Order " + orderId);
        bizContent.put("timeout_express", "5m");
        request.setBizContent(new Gson().toJson(bizContent));

        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();

        if (response.isSuccess()) {
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(outTradeNo);
            payrecord.setOrderId(orderId);
            payrecord.setPayment((int) (amount * 100));
            payrecord.setPayMethod("Alipay");
            payrecord.setPayState("等待支付");
            payrecord.setPayTime(LocalDateTime.now());
            payrecordDao.save(payrecord);

            result.put("success", true);
            result.put("message", "支付创建成功");
            result.put("payId", outTradeNo);
            result.put("qrCode", response.getQrCode());
            generateQRCode(response.getQrCode(), outTradeNo); // 生成二维码
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
            result.put("qrCode", tradeStatus.equals("TRADE_SUCCESS") ? null : payrecord.getPayId());
        } else {
            result.put("success", false);
            result.put("message", "查询失败: " + response.getMsg());
        }
        return result;
    }

    public Map<String, Object> cancelPayment(String payId) throws AlipayApiException {
        AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
        request.setBizContent(new Gson().toJson(Map.of("out_trade_no", payId)));
        AlipayTradeCancelResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();
        if (response.isSuccess()) {
            result.put("success", true);
            result.put("message", "取消成功");
            result.put("action", response.getAction());
            result.put("tradeNo", response.getTradeNo());
        } else {
            result.put("success", false);
            result.put("message", "取消失败: " + response.getMsg());
        }
        return result;
    }

    private void generateQRCode(String qrCodeUrl, String orderNo) {
        if (qrCodeUrl == null || qrCodeUrl.isEmpty()) return;
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(qrCodeUrl, BarcodeFormat.QR_CODE, 300, 300);
            String fileName = "qrcode_" + orderNo + ".png";
            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", fos);
            }
        } catch (WriterException | IOException e) {
            System.err.println("二维码生成失败: " + e.getMessage());
        }
    }
}