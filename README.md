# 🛠️ 项目结构

```
FlightBookingSystem/
├── 📄 pom.xml                                # Maven项目配置文件
├── 📄 README.md                              # 项目说明文档
├── 📄 .gitignore                             # Git版本控制忽略配置
│
└── 📁 src/                                   # 源代码目录
    └── 📁 main/                             # 主程序代码
        └── 📁 java/                         # Java源代码
            │
            ├── 📁 dao/                      # 🗄️ 数据访问对象层
            │   ├── 📄 AirlinecompanyDao.java    # 航空公司数据访问
            │   ├── 📄 AirlinecompanyDaoTest.java # 航空公司DAO测试
            │   ├── 📄 DatabaseConnection.java    # 数据库连接管理
            │   ├── 📄 FlightDao.java             # 航班数据访问
            │   ├── 📄 FlightrecordDao.java       # 航班记录数据访问
            │   ├── 📄 OrderDao.java              # 订单数据访问
            │   ├── 📄 PayrecordDao.java          # 支付记录数据访问
            │   └── 📄 UserDao.java               # 用户数据访问
            │
            ├── 📁 dto/                      # 🗄️ 
            │   ├── 📄 OrderDetailDTO.java    # 
            │   ├── 📄 OrderStatsDTO.java # 
            │
            ├── 📁 model/                    # 📊 数据实体层
            │   ├── 📄 Airlinecompany.java        # 航空公司实体
            │   ├── 📄 Airport.java               # 机场实体
            │   ├── 📄 Flight.java                # 航班实体
            │   ├── 📄 Flightrecord.java          # 航班记录实体
            │   ├── 📄 Luggage.java               # 行李实体
            │   ├── 📄 Order.java                 # 订单实体
            │   ├── 📄 Payrecord.java             # 支付记录实体
            │   └── 📄 User.java                  # 用户实体
            │
            ├── 📁 org.example/              # 📦 主程序包
            │
            ├── 📁 server/                   # 🌐 HTTP服务器层
            │   └── 📄 SimpleHttpServer.java     # 简单HTTP服务器实现
            │
            ├── 📁 service/                  # 🔧 业务逻辑服务层
            │   ├── 📄 AlipayService.java         # 支付宝支付服务
            │   ├── 📄 BookingService.java        # 航班预订服务
            │   └── 📄 FlightSearchService.java   # 航班搜索服务
            │   └── 📄 OrderService.java          # 订单管理服务
            │
            ├── 📁 test/                     # 🧪 测试类
            │   └── 📄 AlipayTest.java            # 支付宝测试
            │
            └── 📁 utils/                    # 🛠️ 工具类库
                ├── 📄 JsonUtil.java              # JSON处理工具
        │
        └── 📁 resources/                    # 📦 资源文件目录
            ├── 📁 sql/                      # 🗄️ 数据库脚本
            │   ├── 📄 create_table.sql          # 数据表创建脚本
            │   └── 📄 data.sql                  # 初始化数据脚本
            │
            └── 📁 static/                   # 🌐 静态Web资源
                ├── 📁 css/                      # 样式表文件目录
                ├── 📁 images/                   # 图片资源目录
                ├── 📁 js/                       # JavaScript脚本目录
                ├── 📁 可复用html代码/             # HTML模板代码
                │
                ├── 📄 admin.html                # 管理员页面
                ├── 📄 order.html                # 订单管理页面
                ├── 📄 booking_demo.html         # 航班预订页面
                ├── 📄 main.html                 # 系统主页
                ├── 📄 payment.html              # 支付页面
                ├── 📄 personal.html             # 个人中心页面
                ├── 📄 search.html               # 航班搜索页面
                ├── 📄 search_demo.html          # 搜索演示页面
                ├── 📄 sign_log.html             # 登录注册页面
                └── 📄 test.js                   # 测试脚本
    │
    └── 📁 test/                             # 🧪 测试代码目录
        ├── 📁 java/                         # Java测试代码
        └── 📁 resources/                    # 测试资源文件
│
├── 📁 target/                               # 🎯 Maven构建输出目录
│   ├── 📁 classes/                          # 编译后的字节码文件
│   └── 📁 generated-sources/                # 生成的源代码
│
├── 📄 .gitignore                            # Git忽略配置文件
├── 📄 Alipay_uasage.md                      # 支付宝使用说明文档
├── 📄 pom.xml                               # Maven项目配置文件
└── 📄 qrcode.png                            # 项目二维码图片
```

## 🏗️ 架构层次说明

### 1. **数据访问层 (DAO Layer)**
- `dao/` - 8个DAO类，负责数据库CRUD操作
- 包含数据库连接管理和各实体的数据访问接口

### 2. **数据模型层 (Model Layer)**
- `model/` - 8个实体类，对应数据库表结构
- 定义核心业务对象：用户、航班、订单、支付等

### 3. **业务服务层 (Service Layer)**
- `service/` - 3个核心服务类
- 封装业务逻辑：搜索、预订、支付

### 4. **服务器层 (Server Layer)**
- `server/` - HTTP服务器实现
- 处理HTTP请求和响应

### 5. **工具层 (Utils Layer)**
- `utils/` - 通用工具类
- JSON处理和系统启动入口

## 🔧 核心组件详情

| 层级 | 组件数量 | 主要文件 |
|------|---------|----------|
| **DAO层** | 8个 | UserDao, FlightDao, OrderDao, PayrecordDao等 |
| **Model层** | 8个 | User, Flight, Order, Payrecord, Airport等 |
| **Service层** | 3个 | BookingService, FlightSearchService, AlipayService |
| **Server层** | 1个 | SimpleHttpServer |
| **Utils层** | 2个 | JsonUtil, Main |
| **前端页面** | 9个 | 完整的用户界面，包含搜索、预订、支付等功能 |
| **数据库脚本** | 2个 | 建表脚本和初始化数据 |
| **测试类** | 2个 | AirlinecompanyDaoTest, AlipayTest |

## 📦 技术特点

- **纯Java实现** - 不依赖Spring等框架，轻量级架构
- **分层架构** - DAO-Model-Service-Server清晰分层
- **完整前端** - 9个HTML页面覆盖全业务流程
- **支付集成** - 集成支付宝支付服务，含使用文档
- **JSON支持** - 自研JSON处理工具
- **数据库支持** - 完整的SQL建表和数据脚本
- **演示页面** - 提供demo版本便于测试和展示

## 🎯 页面功能

| 页面 | 功能描述 |
|------|----------|
| **main.html** | 系统首页，导航和功能入口 |
| **search.html / search_demo.html** | 航班搜索页面和演示版本 |
| **booking.html / booking_demo.html** | 航班预订页面和演示版本 |
| **payment.html** | 支付页面，支持支付宝 |
| **personal.html** | 个人中心，订单管理 |
| **admin.html** | 管理员后台管理 |
| **sign_log.html** | 用户登录注册 |

## 🚀 启动方式

```bash
# 编译项目
mvn clean compile

# 运行主程序
java -cp target/classes utils.Main
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


