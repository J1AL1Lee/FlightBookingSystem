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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AlipayTest {

    private static AlipayClient alipayClient;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            System.out.println("🚀 支付宝API测试开始...");

            // 初始化支付宝客户端
            initAlipayClient();

            // 显示测试菜单
            showMenu();

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    /**
     * 初始化支付宝客户端
     */
    private static void initAlipayClient() throws AlipayApiException {
        System.out.println("🔧 初始化支付宝客户端...");

        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do"); // 沙箱网关
        alipayConfig.setAppId("9021000149697288"); // 你的沙箱 AppID

        // 🔧 私钥（请确保这是你的完整私钥）
        alipayConfig.setPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC8j80DL2o11vTtip6bhPZNE+dmhQarBCSnYzD3TYLI4WYRA5FyYfw1LolsYjgOK+v4OeiMk31NLfrlXBZCpFEiEnMDU6bVlIMIN6uD/MtvrLyf0uepGLso01B8UrB7jja/F81kfutkeQVdxPsl67XRZ2qPax6HGBA8msaoggoEr2k/xd9tBByYwYbaF5UueSfpjj6FheOr4cFzFOy1POWPAYqRM6XxQK+w7i5xl+ah7IdmJsOyGRImdg4J3LHlQJ8XkjfsmzgqNq93WGVrXH0Egrgrx5VZm4IgVP6fZfSjhhxokkj/dhPk/idWYrURzSfiJ0PUvRMaL8Gzx7TuBKhHAgMBAAECggEBAKAIF4HdivG4xtSXsjbhaLxP6TNcMSWRdZ5Ok+8/bIEasyo7cgS23nswTNecoGB+rF1WoGQ2hMCtBmQEfKwAkw8sw0oOg+h+i5q8zKdPNEVKQCgQsiYUZDuo5IUvFLM4JoSWKe5hvVvfTkuf81riqsPXVlv0GMulA5q77WB0RRZlZ3+01Vhkz4PIeWce23endgd7RjGfNhpq7Z/OKtl7N+jChjGToUTL6s3TsEirbP9DcQYO1M8ETLFkYTXViMt3Z3lB3fMEcjX53cc9DqdbuRX0+K4HnPtBIlRKOvpf6+l8Vb6s6d8OTSWHuj1/1cegpE5M7d+bZ/VjPEFVF2FdayECgYEA6TFeR/rtSvmdowed6OdUmA1hgPJidaGuP3jsVY/hyNnSGhkbE7WssNmC7HXeVVjRC81iULaSr9kJc2Rh0O5EGrn0GarKFkA3kyBE50YVqSkIrCIpFtydFdNKvpAepQDu0BfoxJsDac2BfPrvCbS35J3HZ/sHrSNXqWlQV0Cm2fECgYEAzwD32/HdfVKby6IEpSVqW14v3yyPElShHQL7j3TluLneZbi9cN3eZxA9ebNRhe60wtoAGWOx0LS12fHWg2FsuTywcdhVTbzkElog33lzlLwEko6fgbE4ANL9q48vRmxptWDKivEl3pW9qgP+SLD/+xJBgjLto6KX0SHlxh3nrbcCgYBqFlqVFpQTsuHDRHjTd0Jl9lhwaFTgvRBfseyatF18mZPa6acG3XTV8+57Eth2LXTVELf0jkrHk06YX4ecnHkBS63Aa5GKc+aUmW6fZKQAFDnszZGx4+XXAwwTC8/VM0pyAx6TKw5veN269RIAcWXjrOAF7w879kMwQEgbmb8OkQKBgQCAY9FXkcQWns4SlwLai0JUOS7n9PMoI2VqYRc1+wMgd+gAn3ygLHxs4B3BBf9iWpOy5xN4q+T11Z+U9fJeumZ83a9ybQM7nBS5bT1GXkXZ0mPjoqI8Bnb9y9+aMMzZmRRXcxks5DTgwW9JrABjhaS/TKtk3cGW5JnVFHk3UAUKMQKBgE3PJY5gVB+Gpg+hElNV+IWrIZ6atDE5HEtMJrYjnuRSchuwkW2pIGwsOAya83LLvk6Of6V5oSM0Mn9LcKpgO++AiS85zIS61CHPhetirFDDqhqvzK6Xpp9rWrIUnOU4wHToQP4VWRiSPYkU/PpXcDEeOG7C4/IghkzpJfzNMqHP");

        alipayConfig.setFormat("json");
        alipayConfig.setCharset("UTF-8");

        // 🔧 支付宝公钥（请确保这是支付宝的公钥，不是你的应用公钥）
        alipayConfig.setAlipayPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmlEmO339xgGfOex/PGyi7HyhD7XTYb3QpUmlK2Z2Qnk0eSQ2dtOOo19tdZgBnFM7n9ybBC4y/uchhvcV59tDx8c4h82ST8H7wf2TfldsfXIbk10/z5xuSeqnoQ4aXLx8UfXMywMLT+Ytpz2+75rTkWeE4/q1lCDBRZ+0qHcUmCTNy4Mg554lWfQ60XmpJawvOB0jjM2zlFggoG4V0ieJCdcI+3fnw1WP5/probDo1ZPSr3b9zu1Y3XoDO0smADp4+4NHuJEQCL7R06ModhemFUm+FI6V1H7pj1zrt2mR3pXD7CasVY3nEGLyO59oGSGvpPottKQem7IvpujoU+Do/wIDAQAB");
        alipayConfig.setSignType("RSA2");

        alipayClient = new DefaultAlipayClient(alipayConfig);
        System.out.println("✅ 支付宝客户端初始化成功");
    }

    /**
     * 显示测试菜单
     */
    private static void showMenu() {
        while (true) {
            System.out.println("\n========== 支付宝API测试菜单 ==========");
            System.out.println("1. 创建支付（扫码支付）");
            System.out.println("2. 查询支付状态");
            System.out.println("3. 取消支付");
            System.out.println("4. 完整测试流程");
            System.out.println("0. 退出");
            System.out.println("====================================");
            System.out.print("请选择操作 (0-4): ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // 消费换行符

            switch (choice) {
                case 1:
                    testCreatePayment();
                    break;
                case 2:
                    testQueryPayment();
                    break;
                case 3:
                    testCancelPayment();
                    break;
                case 4:
                    testFullProcess();
                    break;
                case 0:
                    System.out.println("👋 测试结束");
                    return;
                default:
                    System.out.println("❌ 无效选择，请重新输入");
            }
        }
    }

    /**
     * 测试创建支付
     */
    private static void testCreatePayment() {
        try {
            System.out.println("\n💳 测试创建支付...");

            // 生成唯一订单号（时间戳 + 随机数）
            String outTradeNo = generateOrderNo();
            System.out.println("📝 订单号: " + outTradeNo);

            System.out.print("请输入支付金额（元，建议0.01用于测试）: ");
            String amount = scanner.nextLine();
            if (amount.isEmpty()) {
                amount = "0.01"; // 默认金额
            }

            // 创建请求
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            bizContent.put("total_amount", amount);
            bizContent.put("subject", "航班预订测试-" + outTradeNo);
            bizContent.put("timeout_express", "5m"); // 5分钟超时

            request.setBizContent(new Gson().toJson(bizContent));

            System.out.println("📤 发送请求到支付宝...");

            // 调用API
            AlipayTradePrecreateResponse response = alipayClient.execute(request);

            System.out.println("📥 API响应:");
            System.out.println("  响应码: " + response.getCode());
            System.out.println("  响应消息: " + response.getMsg());
            System.out.println("  子响应码: " + response.getSubCode());
            System.out.println("  子响应消息: " + response.getSubMsg());

            if (response.isSuccess()) {
                System.out.println("✅ 支付创建成功!");
                System.out.println("🆔 交易号: " + response.getOutTradeNo());
                System.out.println("🔗 二维码链接: " + response.getQrCode());

                // 生成二维码图片
                generateQRCode(response.getQrCode(), outTradeNo);

                // 保存订单号以供后续测试
                System.setProperty("lastOrderNo", outTradeNo);

            } else {
                System.err.println("❌ 支付创建失败!");
                System.err.println("  错误码: " + response.getCode());
                System.err.println("  错误信息: " + response.getMsg());
                if (response.getSubCode() != null) {
                    System.err.println("  子错误码: " + response.getSubCode());
                    System.err.println("  子错误信息: " + response.getSubMsg());
                }
            }

        } catch (AlipayApiException e) {
            System.err.println("❌ API调用异常: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ 测试异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试查询支付状态
     */
    private static void testQueryPayment() {
        try {
            System.out.println("\n🔍 测试查询支付状态...");

            System.out.print("请输入要查询的订单号（回车使用上次创建的订单）: ");
            String outTradeNo = scanner.nextLine();

            if (outTradeNo.isEmpty()) {
                outTradeNo = System.getProperty("lastOrderNo");
                if (outTradeNo == null) {
                    System.err.println("❌ 没有可用的订单号，请先创建支付或手动输入订单号");
                    return;
                }
            }

            System.out.println("📝 查询订单号: " + outTradeNo);

            // 创建查询请求
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            request.setBizContent(new Gson().toJson(bizContent));

            System.out.println("📤 发送查询请求...");

            // 调用API
            AlipayTradeQueryResponse response = alipayClient.execute(request);

            System.out.println("📥 查询响应:");
            System.out.println("  响应码: " + response.getCode());
            System.out.println("  响应消息: " + response.getMsg());

            if (response.isSuccess()) {
                System.out.println("✅ 查询成功!");
                System.out.println("📊 交易状态: " + response.getTradeStatus());
                System.out.println("💰 交易金额: " + response.getTotalAmount());
                System.out.println("🆔 支付宝交易号: " + response.getTradeNo());
                System.out.println("🆔 商户订单号: " + response.getOutTradeNo());

                // 显示更多可用信息
                if (response.getBuyerUserId() != null) {
                    System.out.println("👤 买家用户ID: " + response.getBuyerUserId());
                }
                if (response.getReceiptAmount() != null) {
                    System.out.println("💵 实收金额: " + response.getReceiptAmount());
                }

                // 解释交易状态
                explainTradeStatus(response.getTradeStatus());

            } else {
                System.err.println("❌ 查询失败!");
                System.err.println("  错误信息: " + response.getMsg());
                if (response.getSubCode() != null) {
                    System.err.println("  子错误码: " + response.getSubCode());
                    System.err.println("  子错误信息: " + response.getSubMsg());
                }
            }

        } catch (AlipayApiException e) {
            System.err.println("❌ API调用异常: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 测试异常: " + e.getMessage());
        }
    }

    /**
     * 测试取消支付
     */
    private static void testCancelPayment() {
        try {
            System.out.println("\n🚫 测试取消支付...");

            System.out.print("请输入要取消的订单号（回车使用上次创建的订单）: ");
            String outTradeNo = scanner.nextLine();

            if (outTradeNo.isEmpty()) {
                outTradeNo = System.getProperty("lastOrderNo");
                if (outTradeNo == null) {
                    System.err.println("❌ 没有可用的订单号，请先创建支付或手动输入订单号");
                    return;
                }
            }

            System.out.println("📝 取消订单号: " + outTradeNo);

            // 创建取消请求
            AlipayTradeCancelRequest request = new AlipayTradeCancelRequest();
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            request.setBizContent(new Gson().toJson(bizContent));

            System.out.println("📤 发送取消请求...");

            // 调用API
            AlipayTradeCancelResponse response = alipayClient.execute(request);

            System.out.println("📥 取消响应:");
            System.out.println("  响应码: " + response.getCode());
            System.out.println("  响应消息: " + response.getMsg());

            if (response.isSuccess()) {
                System.out.println("✅ 取消成功!");
                System.out.println("📊 操作结果: " + response.getAction());
                System.out.println("🆔 支付宝交易号: " + response.getTradeNo());

            } else {
                System.err.println("❌ 取消失败!");
                System.err.println("  错误信息: " + response.getMsg());
                if (response.getSubCode() != null) {
                    System.err.println("  子错误码: " + response.getSubCode());
                    System.err.println("  子错误信息: " + response.getSubMsg());
                }
            }

        } catch (AlipayApiException e) {
            System.err.println("❌ API调用异常: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 测试异常: " + e.getMessage());
        }
    }

    /**
     * 完整测试流程
     */
    private static void testFullProcess() {
        System.out.println("\n🔄 开始完整测试流程...");

        // 1. 创建支付
        System.out.println("\n--- 步骤1：创建支付 ---");
        testCreatePayment();

        String orderNo = System.getProperty("lastOrderNo");
        if (orderNo == null) {
            System.err.println("❌ 支付创建失败，终止测试");
            return;
        }

        // 2. 等待用户决定
        System.out.println("\n--- 步骤2：模拟支付过程 ---");
        System.out.println("请选择接下来的操作:");
        System.out.println("1. 模拟支付完成（查询状态）");
        System.out.println("2. 模拟支付超时（取消订单）");
        System.out.print("请选择 (1/2): ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        if (choice == 1) {
            System.out.println("\n--- 步骤3：查询支付状态 ---");
            System.out.println("💡 提示：请在手机支付宝沙箱版中扫码支付，然后按回车继续");
            scanner.nextLine();
            testQueryPayment();
        } else {
            System.out.println("\n--- 步骤3：取消支付 ---");
            testCancelPayment();
        }

        System.out.println("\n🎉 完整测试流程结束");
    }

    /**
     * 生成订单号
     */
    private static String generateOrderNo() {
        return "TEST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", (int)(Math.random() * 1000));
    }

    /**
     * 生成二维码图片
     */
    private static void generateQRCode(String qrCodeUrl, String orderNo) {
        if (qrCodeUrl == null || qrCodeUrl.isEmpty()) {
            System.err.println("❌ 二维码链接为空，无法生成图片");
            return;
        }

        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(qrCodeUrl, BarcodeFormat.QR_CODE, 300, 300);
            String fileName = "qrcode_" + orderNo + ".png";

            try (FileOutputStream fos = new FileOutputStream(fileName)) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", fos);
                System.out.println("📱 二维码图片已生成: " + new File(fileName).getAbsolutePath());
                System.out.println("💡 请使用支付宝扫描二维码进行支付测试");
            }

        } catch (WriterException e) {
            System.err.println("❌ 二维码编码失败: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("❌ 生成二维码图片失败: " + e.getMessage());
        }
    }

    /**
     * 解释交易状态
     */
    private static void explainTradeStatus(String tradeStatus) {
        if (tradeStatus == null) {
            System.out.println("📝 状态说明: 未知状态");
            return;
        }

        System.out.print("📝 状态说明: ");
        switch (tradeStatus) {
            case "WAIT_BUYER_PAY":
                System.out.println("等待买家付款");
                break;
            case "TRADE_CLOSED":
                System.out.println("交易关闭（未付款或超时）");
                break;
            case "TRADE_SUCCESS":
                System.out.println("交易成功");
                break;
            case "TRADE_FINISHED":
                System.out.println("交易完结");
                break;
            default:
                System.out.println("其他状态: " + tradeStatus);
        }
    }
}