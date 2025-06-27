```
flight-booking-system/
├── 📄 pom.xml                                    # Maven配置文件
├── 📄 README.md                                  # 项目说明文档
├── 📄 .gitignore                                 # Git忽略配置
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/
│   │   │   ├── 📄 FlightBookingMain.java         # 🚀 系统启动类
│   │   │   ├── 📁 server/                        # 🌐 HTTP服务器层
│   │   │   │   ├── 📄 FlightHttpServer.java      # HTTP服务器
│   │   │   │   └── 📄 FlightRequestHandler.java  # 请求处理器
│   │   │   ├── 📁 model/                         # 📊 实体类层
│   │   │   │   ├── 📄 User.java                  # 用户实体
│   │   │   │   ├── 📄 Flight.java                # 航班实体
│   │   │   │   ├── 📄 FlightSchedule.java        # 航程实体
│   │   │   │   ├── 📄 Order.java                 # 订单实体
│   │   │   │   ├── 📄 Payment.java               # 支付实体
│   │   │   │   ├── 📄 Seat.java                  # 座位实体
│   │   │   │   ├── 📄 Airport.java               # 机场实体
│   │   │   │   └── 📄 Airline.java               # 航空公司实体
│   │   │   ├── 📁 dao/                           # 🗄️ 数据访问层
│   │   │   │   ├── 📄 DatabaseConnection.java    # 数据库连接管理
│   │   │   │   ├── 📄 UserDao.java               # 用户数据访问
│   │   │   │   ├── 📄 FlightDao.java             # 航班数据访问
│   │   │   │   ├── 📄 FlightScheduleDao.java     # 航程数据访问
│   │   │   │   ├── 📄 OrderDao.java              # 订单数据访问
│   │   │   │   ├── 📄 PaymentDao.java            # 支付数据访问
│   │   │   │   ├── 📄 SeatDao.java               # 座位数据访问
│   │   │   │   └── 📄 AirportDao.java            # 机场数据访问
│   │   │   ├── 📁 service/                       # 🔧 业务逻辑层（可选）
│   │   │   │   ├── 📄 FlightSearchService.java   # 航班搜索服务
│   │   │   │   ├── 📄 BookingService.java        # 订票服务
│   │   │   │   └── 📄 PaymentService.java        # 支付服务
│   │   │   ├── 📁 utils/                         # 🛠️ 工具类层
│   │   │   │   ├── 📄 JsonUtil.java              # JSON工具
│   │   │   │   ├── 📄 OrderNumberGenerator.java  # 订单号生成器
│   │   │   │   ├── 📄 DateUtil.java              # 日期工具
│   │   │   │   ├── 📄 ValidationUtil.java        # 验证工具
│   │   │   │   └── 📄 ResponseUtil.java          # 响应工具
│   │   │   └── 📁 exception/                     # ⚠️ 异常处理层
│   │   │       ├── 📄 FlightBookingException.java # 业务异常
│   │   │       ├── 📄 DatabaseException.java     # 数据库异常
│   │   │       └── 📄 PaymentException.java      # 支付异常
│   │   └── 📁 resources/
│   │       ├── 📁 static/                        # 🌐 静态资源
│   │       │   ├── 📄 index.html                 # 首页
│   │       │   ├── 📄 booking.html               # 订票页面
│   │       │   ├── 📄 search.html                # 搜索页面
│   │       │   ├── 📄 orders.html                # 订单页面
│   │       │   ├── 📁 css/
│   │       │   │   ├── 📄 style.css              # 主样式
│   │       │   │   └── 📄 booking.css            # 订票样式
│   │       │   ├── 📁 js/
│   │       │   │   ├── 📄 main.js                # 主脚本
│   │       │   │   ├── 📄 booking.js             # 订票脚本
│   │       │   │   └── 📄 api.js                 # API调用
│   │       │   └── 📁 images/
│   │       │       ├── 📄 logo.png               # 系统Logo
│   │       │       └── 📄 plane.svg              # 飞机图标
│   │       ├── 📁 sql/                           # 🗄️ SQL脚本
│   │       │   ├── 📄 create_database.sql        # 建库脚本
│   │       │   ├── 📄 create_tables.sql          # 建表脚本
│   │       │   ├── 📄 init_data.sql              # 初始数据
│   │       │   └── 📄 sample_data.sql            # 示例数据
│   │       └── 📄 application.properties         # 配置文件
│   └── 📁 test/
│       ├── 📁 java/
│       │   ├── 📄 FlightBookingTest.java         # 系统测试
│       │   ├── 📁 dao/
│       │   │   ├── 📄 UserDaoTest.java           # 用户DAO测试
│       │   │   ├── 📄 FlightDaoTest.java         # 航班DAO测试
│       │   │   └── 📄 OrderDaoTest.java          # 订单DAO测试
│       │   ├── 📁 service/
│       │   │   ├── 📄 BookingServiceTest.java    # 订票服务测试
│       │   │   └── 📄 PaymentServiceTest.java    # 支付服务测试
│       │   └── 📁 utils/
│       │       ├── 📄 JsonUtilTest.java          # JSON工具测试
│       │       └── 📄 OrderGeneratorTest.java    # 订单生成测试
│       └── 📁 resources/
│           └── 📄 test.properties                # 测试配置
├── 📁 docs/                                     # 📚 文档目录
│   ├── 📄 API.md                                 # API文档
│   ├── 📄 DATABASE.md                            # 数据库设计文档
│   ├── 📄 DEPLOYMENT.md                          # 部署文档
│   └── 📁 images/
│       ├── 📄 architecture.png                   # 架构图
│       └── 📄 database_schema.png                # 数据库ER图
├── 📁 scripts/                                  # 🔧 脚本目录
│   ├── 📄 start.sh                               # 启动脚本（Linux/Mac）
│   ├── 📄 start.bat                              # 启动脚本（Windows）
│   ├── 📄 build.sh                               # 构建脚本
│   └── 📄 deploy.sh                              # 部署脚本
└── 📁 target/                                   # 🎯 Maven构建输出
    ├── 📁 classes/                               # 编译后的class文件
    ├── 📁 test-classes/                          # 测试class文件
    └── 📄 flight-booking-system-1.0.0-jar-with-dependencies.jar  # 可执行JAR包

```

## 📋 关键文件说明

### 🚀 核心启动文件
- **FlightBookingMain.java** - 系统启动入口

### 🌐 HTTP服务层
- **FlightHttpServer.java** - 基于Java内置HttpServer的Web服务器
- **FlightRequestHandler.java** - 处理所有API请求的核心逻辑

### 📊 数据模型层
- **8个实体类** - 完整映射数据库表结构
- 支持用户、航班、订单、支付等全业务场景

### 🗄️ 数据访问层
- **纯JDBC实现** - 不依赖任何ORM框架
- **8个DAO类** - 对应各实体的CRUD操作

### 🛠️ 工具类层
- **JsonUtil.java** - 手写JSON序列化工具
- **OrderNumberGenerator.java** - 订单号生成算法
- 各种业务工具类

### 🌐 前端资源
- **HTML页面** - 简洁的用户界面
- **CSS样式** - 现代化的UI设计
- **JavaScript** - 前后端API交互

### 🗄️ 数据库脚本
- **完整SQL脚本** - 建库、建表、初始数据
- **示例数据** - 测试用的航班和机场数据

## 🎯 项目特点

### ✅ 零依赖架构
```
依赖列表：
├── MySQL驱动 (唯一必需)
├── Gson (JSON处理)
└── JUnit (测试，可选)
```

### ✅ 模块化设计
- 📦 **分层清晰** - MVC架构，职责分明
- 🔧 **高内聚低耦合** - 各模块独立，易于维护
- 🎯 **业务完整** - 覆盖航空订票全流程

### ✅ 开发友好
- 🚀 **启动超快** - 3秒内启动完成
- 🐛 **易于调试** - 纯Java代码，逻辑清晰
- 📝 **文档完整** - API文档、部署文档齐全

## 🚀 快速启动

```bash
# 1. 克隆项目
git clone <repository>
cd flight-booking-system

# 2. 编译项目
mvn clean compile

# 3. 启动系统
mvn exec:java

# 4. 访问系统
curl http://localhost:8080/api/airports
```


