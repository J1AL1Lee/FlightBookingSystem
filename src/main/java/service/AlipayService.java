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

    // 原始构造函数（保留用于生产环境）
    public AlipayService() {
        Properties props = new Properties();
        try (InputStream input = AlipayService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("❌ 无法找到 application.properties 文件");
            }
            props.load(input);
            AlipayConfig config = new AlipayConfig();
            config.setServerUrl(props.getProperty("https://openapi-sandbox.dl.alipaydev.com/gateway.do"));
            config.setAppId(props.getProperty("9021000149697288"));
            config.setPrivateKey(props.getProperty("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8j80DL2o11vTtip6bhPZNE+dmhQarBCSnYzD3TYLI4WYRA5FyYfw1LolsYjgOK+v4OeiMk31NLfrlXBZCpFEiEnMDU6bVlIMIN6uD/MtvrLyf0uepGLso01B8UrB7jja/F81kfutkeQVdxPsl67XRZ2qPax6HGBA8msaoggoEr2k/xd9tBByYwYbaF5UueSfpjj6FheOr4cFzFOy1POWPAYqRM6XxQK+w7i5xl+ah7IdmJsOyGRImdg4J3LHlQJ8XkjfsmzgqNq93WGVrXH0Egrgrx5VZm4IgVP6fZfSjhhxokkj/dhPk/idWYrURzSfiJ0PUvRMaL8Gzx7TuBKhHAgMBAAECggEBAKAIF4HdivG4xtSXsjbhaLxP6TNcMSWRdZ5Ok+8/bIEasyo7cgS23nswTNecoGB+rF1WoGQ2hMCtBmQEfKwAkw8sw0oOg+h+i5q8zKdPNEVKQCgQsiYUZDuo5IUvFLM4JoSWKe5hvVvfTkuf81riqsPXVlv0GMulA5q77WB0RRZlZ3+01Vhkz4PIeWce23endgd7RjGfNhpq7Z/OKtl7N+jChjGToUTL6s3TsEirbP9DcQYO1M8ETLFkYTXViMt3Z3lB3fMEcjX53cc9DqdbuRX0+K4HnPtBIlRKOvpf6+l8Vb6s6d8OTSWHuj1/1cegpE5M7d+bZ/VjPEFVF2FdayECgYEA6TFeR/rtSvmdowed6OdUmA1hgPJidaGuP3jsVY/hyNnSGhkbE7WssNmC7HXeVVjRC81iULaSr9kJc2Rh0O5EGrn0GarKFkA3kyBE50YVqSkIrCIpFtydFdNKvpAepQDu0BfoxJsDac2BfPrvCbS35J3HZ/sHrSNXqWlQV0Cm2fECgYEAzwD32/HdfVKby6IEpSVqW14v3yyPElShHQL7j3TluLneZbi9cN3eZxA9ebNRhe60wtoAGWOx0LS12fHWg2FsuTywcdhVTbzkElog33lzlLwEko6fgbE4ANL9q48vRmxptWDKivEl3pW9qgP+SLD/+xJBgjLto6KX0SHlxh3nrbcCgYBqFlqVFpQTsuHDRHjTd0Jl9lhwaFTgvRBfseyatF18mZPa6acG3XTV8+57Eth2LXTVELf0jkrHk06YX4ecnHkBS63Aa5GKc+aUmW6fZKQAFDnszZGx4+XXAwwTC8/VM0pyAx6TKw5veN269RIAcWXjrOAF7w879kMwQEgbmb8OkQKBgQCAY9FXkcQWns4SlwLai0JUOS7n9PMoI2VqYRc1+wMgd+gAn3ygLHxs4B3BBf9iWpOy5xN4q+T11Z+U9fJeumZ83a9ybQM7nBS5bT1GXkXZ0mPjoqI8Bnb9y9+aMMzZmRRXcxks5DTgwW9JrABjhaS/TKtk3cGW5JnVFHk3UAUKMQKBgE3PJY5gVB+Gpg+hElNV+IWrIZ6atDE5HEtMJrYjnuRSchuwkW2pIGwsOAya83LLvk6Of6V5oSM0Mn9LcKpgO++AiS85zIS61CHPhetirFDDqhqvzK6Xpp9rWrIUnOU4wHToQP4VWRiSPYkU/PpXcDEeOG7C4/IghkzpJfzNMqHP"));
            config.setAlipayPublicKey(props.getProperty("\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmlEmO339xgGfOex/PGyi7HyhD7XTYb3QpUmlK2Z2Qnk0eSQ2dtOOo19tdZgBnFM7n9ybBC4y/uchhvcV59tDx8c4h82ST8H7wf2TfldsfXIbk10/z5xuSeqnoQ4aXLx8UfXMywMLT+Ytpz2+75rTkWeE4/q1lCDBRZ+0qHcUmCTNy4Mg554lWfQ60XmpJawvOB0jjM2zlFggoG4V0ieJCdcI+3fnw1WP5/probDo1ZPSr3b9zu1Y3XoDO0smADp4+4NHuJEQCL7R06ModhemFUm+FI6V1H7pj1zrt2mR3pXD7CasVY3nEGLyO59oGSGvpPottKQem7IvpujoU+Do/wIDAQAB"));
            config.setCharset("UTF-8");
            config.setSignType("RSA2");
            try {
                this.alipayClient = new DefaultAlipayClient(config);
            } catch (AlipayApiException e) {
                System.err.println("❌ 支付宝客户端初始化失败: " + e.getMessage());
                throw new RuntimeException("支付宝客户端初始化失败", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("❌ 加载支付宝配置失败: " + e.getMessage(), e);
        }
        this.payrecordDao = new PayrecordDao();
    }

    // 新增构造函数，用于测试
    public AlipayService(AlipayClient alipayClient, PayrecordDao payrecordDao) {
        this.alipayClient = alipayClient;
        this.payrecordDao = payrecordDao;
    }

    public Map<String, Object> createPayment(String orderId, double amount) throws AlipayApiException {
        String outTradeNo = generateUniqueTradeNo();

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("total_amount", String.format("%.2f", amount));
        bizContent.put("subject", "Flight Booking Payment for Order " + orderId);
        request.setBizContent(new Gson().toJson(bizContent));

        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        Map<String, Object> result = new HashMap<>();

        if (response.isSuccess()) {
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(response.getOutTradeNo());
            payrecord.setOrderId(orderId);
            payrecord.setPayment((int) (amount * 100));
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
            result.put("qrCode", tradeStatus.equals("TRADE_SUCCESS") ? null : payrecord.getPayId());
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