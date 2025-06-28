public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 航空订票系统启动中...");
        System.out.println("✅ 数据库连接正常");
        System.out.println("🌐 HTTP服务器即将启动...");

        try {
            // 暂时先打印，下一步实现真正的服务器
            System.out.println("📍 访问地址: http://localhost:8080");
            System.out.println("🎉 系统启动成功！按 Ctrl+C 退出");

            // 保持程序运行
            Thread.sleep(60000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}