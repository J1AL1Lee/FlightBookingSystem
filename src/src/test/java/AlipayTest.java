package test;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCreateRequest;
import com.alipay.api.response.AlipayTradeCreateResponse;
import java.util.HashMap;
import java.util.Map;

public class AlipayTest {

    // 沙箱环境配置 (替换为你的实际值)
    private static final String APP_ID = "your_sandbox_app_id"; // 沙箱 AppID
    private static final String PRIVATE_KEY = "your_private_key"; // 你的私钥
    private static final String ALIPAY_PUBLIC_KEY = "your_alipay_public_key"; // 支付宝公钥
    private static final String GATEWAY_URL = "https://openapi.alipaydev.com/gateway.do"; // 沙箱网关
    private static final String FORMAT = "JSON";
    private static final String SIGN_TYPE = "RSA2";
    private static final String CHARSET = "UTF-8";

    public static void main(String[] args) {
        // 初始化 AlipayClient
        AlipayClient alipayClient = new DefaultAlipayClient(
                GATEWAY_URL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        // 创建请求
        AlipayTradeCreateRequest request = new AlipayTradeCreateRequest();
        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", "ORDER202506290001"); // 订单号 (唯一)
        bizContent.put("total_amount", "0.01"); // 金额 (沙箱测试用小额)
        bizContent.put("subject", "Flight Booking Payment"); // 订单标题
        bizContent.put("buyer_id", "sandbox_buyer@alipay.com"); // 沙箱买家ID
        bizContent.put("seller_id", "sandbox_seller@alipay.com"); // 沙箱卖家ID
        request.setBizContent(JsonUtil.toJson(bizContent)); // 假设 JsonUtil 可用

        // 调用 API
        try {
            AlipayTradeCreateResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("✅ 支付创建成功!");
                System.out.println("交易号: " + response.getTradeNo());
                System.out.println("二维码链接: " + response.getQrCode());
            } else {
                System.err.println("❌ 支付创建失败!");
                System.err.println("错误码: " + response.getCode());
                System.err.println("错误信息: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            System.err.println("❌ API 调用异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}