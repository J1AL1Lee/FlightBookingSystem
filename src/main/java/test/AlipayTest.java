package test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AlipayTest {

    public static void main(String[] args) throws AlipayApiException {
        // 配置支付宝参数
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do"); // 沙箱网关
        alipayConfig.setAppId("9021000149697288"); // 示例沙箱 AppID，替换为你的
        alipayConfig.setPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8j80DL2o11vTtip6bhPZNE+dmhQarBCSnYzD3TYLI4WYRA5FyYfw1LolsYjgOK+v4OeiMk31NLfrlXBZCpFEiEnMDU6bVlIMIN6uD/MtvrLyf0uepGLso01B8UrB7jja/F81kfutkeQVdxPsl67XRZ2qPax6HGBA8msaoggoEr2k/xd9tBByYwYbaF5UueSfpjj6FheOr4cFzFOy1POWPAYqRM6XxQK+w7i5xl+ah7IdmJsOyGRImdg4J3LHlQJ8XkjfsmzgqNq93WGVrXH0Egrgrx5VZm4IgVP6fZfSjhhxokkj/dhPk/idWYrURzSfiJ0PUvRMaL8Gzx7TuBKhHAgMBAAECggEBAKAIF4HdivG4xtSXsjbhaLxP6TNcMSWRdZ5Ok+8/bIEasyo7cgS23nswTNecoGB+rF1WoGQ2hMCtBmQEfKwAkw8sw0oOg+h+i5q8zKdPNEVKQCgQsiYUZDuo5IUvFLM4JoSWKe5hvVvfTkuf81riqsPXVlv0GMulA5q77WB0RRZlZ3+01Vhkz4PIeWce23endgd7RjGfNhpq7Z/OKtl7N+jChjGToUTL6s3TsEirbP9DcQYO1M8ETLFkYTXViMt3Z3lB3fMEcjX53cc9DqdbuRX0+K4HnPtBIlRKOvpf6+l8Vb6s6d8OTSWHuj1/1cegpE5M7d+bZ/VjPEFVF2FdayECgYEA6TFeR/rtSvmdowed6OdUmA1hgPJidaGuP3jsVY/hyNnSGhkbE7WssNmC7HXeVVjRC81iULaSr9kJc2Rh0O5EGrn0GarKFkA3kyBE50YVqSkIrCIpFtydFdNKvpAepQDu0BfoxJsDac2BfPrvCbS35J3HZ/sHrSNXqWlQV0Cm2fECgYEAzwD32/HdfVKby6IEpSVqW14v3yyPElShHQL7j3TluLneZbi9cN3eZxA9ebNRhe60wtoAGWOx0LS12fHWg2FsuTywcdhVTbzkElog33lzlLwEko6fgbE4ANL9q48vRmxptWDKivEl3pW9qgP+SLD/+xJBgjLto6KX0SHlxh3nrbcCgYBqFlqVFpQTsuHDRHjTd0Jl9lhwaFTgvRBfseyatF18mZPa6acG3XTV8+57Eth2LXTVELf0jkrHk06YX4ecnHkBS63Aa5GKc+aUmW6fZKQAFDnszZGx4+XXAwwTC8/VM0pyAx6TKw5veN269RIAcWXjrOAF7w879kMwQEgbmb8OkQKBgQCAY9FXkcQWns4SlwLai0JUOS7n9PMoI2VqYRc1+wMgd+gAn3ygLHxs4B3BBf9iWpOy5xN4q+T11Z+U9fJeumZ83a9ybQM7nBS5bT1GXkXZ0mPjoqI8Bnb9y9+aMMzZmRRXcxks5DTgwW9JrABjhaS/TKtk3cGW5JnVFHk3UAUKMQKBgE3PJY5gVB+Gpg+hElNV+IWrIZ6atDE5HEtMJrYjnuRSchuwkW2pIGwsOAya83LLvk6Of6V5oSM0Mn9LcKpgO++AiS85zIS61CHPhetirFDDqhqvzK6Xpp9rWrIUnOU4wHToQP4VWRiSPYkU/PpXcDEeOG7C4/IghkzpJfzNMqHP"); // 替换为你的私钥
        alipayConfig.setFormat("json");
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setAlipayPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmlEmO339xgGfOex/PGyi7HyhD7XTYb3QpUmlK2Z2Qnk0eSQ2dtOOo19tdZgBnFM7n9ybBC4y/uchhvcV59tDx8c4h82ST8H7wf2TfldsfXIbk10/z5xuSeqnoQ4aXLx8UfXMywMLT+Ytpz2+75rTkWeE4/q1lCDBRZ+0qHcUmCTNy4Mg554lWfQ60XmpJawvOB0jjM2zlFggoG4V0ieJCdcI+3fnw1WP5/probDo1ZPSr3b9zu1Y3XoDO0smADp4+4NHuJEQCL7R06ModhemFUm+FI6V1H7pj1zrt2mR3pXD7CasVY3nEGLyO59oGSGvpPottKQem7IvpujoU+Do/wIDAQAB"); // 替换为支付宝公钥
        alipayConfig.setSignType("RSA2");

        // 初始化客户端
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);

        // 创建请求
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", "ORDER202506290003"); // 唯一订单号
        bizContent.put("total_amount", "0.01"); // 沙箱测试金额
        bizContent.put("subject", "Flight Booking Test Payment"); // 订单标题
        request.setBizContent(new Gson().toJson(bizContent)); // 使用 Gson 序列化

        // 调用 API
        AlipayTradePrecreateResponse response = alipayClient.execute(request);
        System.out.println("响应内容: " + response.getBody());

        if (response.isSuccess()) {
            System.out.println("✅ 支付预创建成功!");
            System.out.println("交易号: " + response.getOutTradeNo());
            String qrCodeUrl = response.getQrCode();
            System.out.println("二维码链接: " + qrCodeUrl);

            if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
                try {
                    // 使用 zxing 生成二维码
                    BitMatrix bitMatrix = new MultiFormatWriter().encode(qrCodeUrl, BarcodeFormat.QR_CODE, 250, 250);
                    String outputPath = "qrcode.png"; // 相对路径
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", fos);
                        System.out.println("✅ 二维码图片已生成: " + new File(outputPath).getAbsolutePath());
                    }
                } catch (com.google.zxing.WriterException e) {
                    System.err.println("❌ 二维码编码失败: " + e.getMessage());
                } catch (IOException e) {
                    System.err.println("❌ 生成二维码图片失败: " + e.getMessage());
                }
            } else {
                System.err.println("❌ 二维码链接无效或为空");
            }
        }
    }
}