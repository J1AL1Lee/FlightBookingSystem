package service;

import model.Payrecord;
import model.Order;
import dao.PayrecordDao;
import dao.OrderDao;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.List;

/**
 * 支付服务类
 * 负责处理所有与支付相关的业务逻辑
 * 基于 PayrecordDao 和 Payrecord 模型
 */
public class PaymentService {
    private PayrecordDao payrecordDao = new PayrecordDao();
    private OrderDao orderDao = new OrderDao();

    /**
     * 创建支付记录
     * @param orderId 订单ID
     * @param amount 支付金额
     * @param payMethod 支付方式
     * @return 支付ID，失败返回null
     */
    public String createPayment(String orderId, Integer amount, String payMethod) {
        System.out.println("💳 开始创建支付记录: 订单=" + orderId + ", 金额=¥" + amount + ", 支付方式=" + payMethod);

        try {
            // 1. 验证参数
            if (!validatePaymentParams(orderId, amount, payMethod)) {
                System.err.println("❌ 支付参数验证失败");
                return null;
            }

            // 2. 检查订单状态
            if (!validateOrderForPayment(orderId)) {
                System.err.println("❌ 订单状态验证失败");
                return null;
            }

            // 3. 检查是否已有支付记录
            if (payrecordDao.hasPaymentRecord(orderId)) {
                System.err.println("❌ 该订单已有支付记录");
                return null;
            }

            // 4. 生成支付ID
            String payId = generateUniquePayId();

            // 5. 创建支付对象
            Payrecord payrecord = createPayrecordObject(payId, orderId, amount, payMethod);
            if (payrecord == null) {
                System.err.println("❌ 创建支付对象失败");
                return null;
            }

            // 6. 保存支付记录
            String savedPayId = payrecordDao.save(payrecord);
            if (savedPayId != null) {
                System.out.println("✅ 支付记录创建成功: " + savedPayId);

                // 7. 更新订单状态为处理中
                updateOrderStateAfterPayment(orderId, "处理中");

                return savedPayId;
            } else {
                System.err.println("❌ 保存支付记录失败");
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ 创建支付记录异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 处理支付成功逻辑
     * @param payId 支付ID
     * @return 是否处理成功
     */
    public boolean processPaymentSuccess(String payId) {
        System.out.println("✅ 处理支付成功: " + payId);

        try {
            // 1. 获取支付记录
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) {
                System.err.println("❌ 支付记录不存在");
                return false;
            }

            // 2. 检查当前状态
            if ("已支付".equals(payrecord.getPayState())) {
                System.out.println("⚠️ 支付记录已经是已支付状态");
                return true;
            }

            // 3. 更新支付状态
            payrecordDao.updateStatus(payId, "已支付");

            // 4. 更新订单状态
            boolean orderUpdated = orderDao.updateOrderState(payrecord.getOrderId(), "已支付");
            if (!orderUpdated) {
                System.err.println("❌ 更新订单状态失败");
                return false;
            }

            System.out.println("✅ 支付处理完成: 订单=" + payrecord.getOrderId());
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理支付成功异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理支付失败逻辑
     * @param payId 支付ID
     * @param reason 失败原因
     * @return 是否处理成功
     */
    public boolean processPaymentFailure(String payId, String reason) {
        System.out.println("❌ 处理支付失败: " + payId + ", 原因: " + reason);

        try {
            // 1. 获取支付记录
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) {
                System.err.println("❌ 支付记录不存在");
                return false;
            }

            // 2. 更新支付状态
            payrecordDao.updateStatus(payId, "支付失败");

            // 3. 更新订单状态为支付失败
            boolean orderUpdated = orderDao.updateOrderState(payrecord.getOrderId(), "支付失败");
            if (!orderUpdated) {
                System.err.println("❌ 更新订单状态失败");
                return false;
            }

            System.out.println("✅ 支付失败处理完成: 订单=" + payrecord.getOrderId());
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理支付失败异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取消支付
     * @param payId 支付ID
     * @return 是否取消成功
     */
    public boolean cancelPayment(String payId) {
        System.out.println("🚫 开始取消支付: " + payId);

        try {
            // 1. 获取支付记录
            Payrecord payrecord = payrecordDao.findById(payId);
            if (payrecord == null) {
                System.err.println("❌ 支付记录不存在");
                return false;
            }

            // 2. 检查支付状态
            if ("已支付".equals(payrecord.getPayState())) {
                System.err.println("❌ 支付已完成，无法取消");
                return false;
            }

            // 3. 更新支付状态
            payrecordDao.updateStatus(payId, "已取消");

            // 4. 同时取消关联订单
            orderDao.updateOrderState(payrecord.getOrderId(), "已取消");

            System.out.println("✅ 支付取消成功: " + payId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 取消支付异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 申请退款
     * @param orderId 订单ID
     * @param reason 退款原因
     * @return 退款申请ID，失败返回null
     */
    public String requestRefund(String orderId, String reason) {
        System.out.println("💰 开始申请退款: 订单=" + orderId + ", 原因=" + reason);

        try {
            // 1. 获取原支付记录
            Payrecord originalPayment = payrecordDao.findByOrderId(orderId);
            if (originalPayment == null) {
                System.err.println("❌ 找不到订单的支付记录");
                return null;
            }

            // 2. 检查支付状态
            if (!"已支付".equals(originalPayment.getPayState())) {
                System.err.println("❌ 支付状态不允许退款: " + originalPayment.getPayState());
                return null;
            }

            // 3. 创建退款记录
            String refundId = generateUniqueRefundId();
            Payrecord refundPayment = new Payrecord();
            refundPayment.setPayId(refundId);
            refundPayment.setOrderId(orderId);
            refundPayment.setPayment(-originalPayment.getPayment()); // 负数表示退款
            refundPayment.setPayMethod("退款");
            refundPayment.setPayState("退款处理中");
            refundPayment.setPayTime(LocalDateTime.now());

            // 4. 保存退款记录
            String savedRefundId = payrecordDao.save(refundPayment);
            if (savedRefundId != null) {
                System.out.println("✅ 退款申请成功: " + savedRefundId);

                // 5. 更新原支付记录状态
                payrecordDao.updateStatus(originalPayment.getPayId(), "已退款");

                // 6. 更新订单状态
                orderDao.updateOrderState(orderId, "已退款");

                return savedRefundId;
            }

            return null;

        } catch (Exception e) {
            System.err.println("❌ 申请退款异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理退款成功
     * @param refundId 退款ID
     * @return 是否处理成功
     */
    public boolean processRefundSuccess(String refundId) {
        System.out.println("💰 处理退款成功: " + refundId);

        try {
            // 更新退款记录状态
            payrecordDao.updateStatus(refundId, "退款成功");
            System.out.println("✅ 退款处理完成: " + refundId);
            return true;

        } catch (Exception e) {
            System.err.println("❌ 处理退款成功异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 查询用户支付记录
     * @param userId 用户ID
     * @return 支付记录列表
     */
    public List<Payrecord> getUserPayments(String userId) {
        try {
            return payrecordDao.findByUserId(userId);
        } catch (Exception e) {
            System.err.println("❌ 获取用户支付记录失败: " + e.getMessage());
            throw new RuntimeException("获取用户支付记录失败", e);
        }
    }

    /**
     * 根据订单ID查询支付记录
     * @param orderId 订单ID
     * @return 支付记录
     */
    public Payrecord getPaymentByOrderId(String orderId) {
        try {
            return payrecordDao.findByOrderId(orderId);
        } catch (Exception e) {
            System.err.println("❌ 获取订单支付记录失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据支付ID查询支付记录
     * @param payId 支付ID
     * @return 支付记录
     */
    public Payrecord getPaymentById(String payId) {
        try {
            return payrecordDao.findById(payId);
        } catch (Exception e) {
            System.err.println("❌ 获取支付记录失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据支付状态查询支付记录
     * @param payState 支付状态
     * @return 支付记录列表
     */
    public List<Payrecord> getPaymentsByState(String payState) {
        try {
            return payrecordDao.findByPayState(payState);
        } catch (Exception e) {
            System.err.println("❌ 根据状态查询支付记录失败: " + e.getMessage());
            throw new RuntimeException("根据状态查询支付记录失败", e);
        }
    }

    /**
     * 根据支付方式查询支付记录
     * @param payMethod 支付方式
     * @return 支付记录列表
     */
    public List<Payrecord> getPaymentsByMethod(String payMethod) {
        try {
            return payrecordDao.findByPayMethod(payMethod);
        } catch (Exception e) {
            System.err.println("❌ 根据支付方式查询支付记录失败: " + e.getMessage());
            throw new RuntimeException("根据支付方式查询支付记录失败", e);
        }
    }

    /**
     * 获取用户总支付金额
     * @param userId 用户ID
     * @return 总支付金额
     */
    public int getUserTotalPayment(String userId) {
        try {
            return payrecordDao.getTotalPaymentByUserId(userId);
        } catch (Exception e) {
            System.err.println("❌ 获取用户总支付金额失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取支付方式统计
     * @return 支付方式统计列表
     */
    public List<PayrecordDao.PayMethodStats> getPaymentMethodStatistics() {
        try {
            return payrecordDao.getPayMethodStatistics();
        } catch (Exception e) {
            System.err.println("❌ 获取支付方式统计失败: " + e.getMessage());
            throw new RuntimeException("获取支付方式统计失败", e);
        }
    }

    /**
     * 获取时间范围内的总收入
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总收入
     */
    public int getTotalIncomeByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return payrecordDao.getTotalIncomeByTimeRange(startTime, endTime);
        } catch (Exception e) {
            System.err.println("❌ 获取时间范围收入失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 批量更新支付状态（管理员功能）
     * @param payIds 支付ID列表
     * @param newState 新状态
     * @return 成功更新的数量
     */
    public int batchUpdatePaymentState(List<String> payIds, String newState) {
        try {
            return payrecordDao.batchUpdatePayState(payIds, newState);
        } catch (Exception e) {
            System.err.println("❌ 批量更新支付状态失败: " + e.getMessage());
            throw new RuntimeException("批量更新支付状态失败", e);
        }
    }

    /**
     * 验证支付参数
     */
    private boolean validatePaymentParams(String orderId, Integer amount, String payMethod) {
        if (orderId == null || orderId.trim().isEmpty()) {
            System.err.println("❌ 订单ID不能为空");
            return false;
        }

        if (amount == null || amount <= 0) {
            System.err.println("❌ 支付金额无效: " + amount);
            return false;
        }

        if (payMethod == null || payMethod.trim().isEmpty()) {
            System.err.println("❌ 支付方式不能为空");
            return false;
        }

        return true;
    }

    /**
     * 验证订单是否可以支付
     */
    private boolean validateOrderForPayment(String orderId) {
        try {
            Order order = orderDao.findById(orderId);
            if (order == null) {
                System.err.println("❌ 订单不存在: " + orderId);
                return false;
            }

            String orderState = order.getOrderState();
            if (!"未支付".equals(orderState) && !"支付失败".equals(orderState)) {
                System.err.println("❌ 订单状态不允许支付: " + orderState);
                return false;
            }

            return true;

        } catch (Exception e) {
            System.err.println("❌ 验证订单失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建支付对象
     */
    private Payrecord createPayrecordObject(String payId, String orderId, Integer amount, String payMethod) {
        try {
            Payrecord payrecord = new Payrecord();
            payrecord.setPayId(payId);
            payrecord.setOrderId(orderId);
            payrecord.setPayment(amount);
            payrecord.setPayMethod(payMethod);
            payrecord.setPayState("已支付");
            payrecord.setPayTime(LocalDateTime.now());

            System.out.println("📝 支付对象创建成功: " + payrecord.toString());
            return payrecord;

        } catch (Exception e) {
            System.err.println("❌ 创建支付对象失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 支付后更新订单状态
     */
    private void updateOrderStateAfterPayment(String orderId, String state) {
        try {
            orderDao.updateOrderState(orderId, state);
            System.out.println("📋 订单状态已更新为 " + state + ": " + orderId);
        } catch (Exception e) {
            System.err.println("⚠️ 更新订单状态失败: " + e.getMessage());
        }
    }

    /**
     * 生成唯一支付ID
     */
    public String generateUniquePayId() {
        String payId;
        int attempts = 0;
        do {
            payId = generatePayId();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("生成唯一支付ID失败，尝试次数过多");
            }
        } while (payrecordDao.isPayIdExists(payId));

        return payId;
    }

    /**
     * 生成支付ID
     */
    private String generatePayId() {
        // 方案1: 使用时间戳后8位 + 2位随机数 (推荐)
        long timestamp = System.currentTimeMillis();
        String timestampStr = String.valueOf(timestamp);
        // 取时间戳的后8位
        String timePart = timestampStr.substring(timestampStr.length() - 8);

        // 生成2位随机数 (00-99)
        Random random = new Random();
        String randomPart = String.format("%02d", random.nextInt(100));

        return timePart + randomPart;
    }

    /**
     * 生成唯一退款ID
     */
    private String generateUniqueRefundId() {
        String refundId;
        int attempts = 0;
        do {
            refundId = generateRefundId();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("生成唯一退款ID失败，尝试次数过多");
            }
        } while (payrecordDao.isPayIdExists(refundId));

        return refundId;
    }

    /**
     * 生成退款ID
     */
    private String generateRefundId() {
        // 格式：REF + 年月日 + 6位随机数，如：REF20251229XXXXXX
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        String randomNum = String.format("%06d", random.nextInt(1000000));
        return "REF" + date + randomNum;
    }
}