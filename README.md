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
            ├── 📁 dto/                      # 📦 数据传输对象层
            │   ├── 📄 OrderDetailDTO.java        # 订单详情数据传输对象
            │   └── 📄 OrderStatsDTO.java         # 订单统计数据传输对象
            │
            ├── 📁 handler/                  # 🌐 HTTP请求处理器层
            │   ├── 📄 AddFlightHandler.java          # 添加航班处理器
            │   ├── 📄 BookingHandler.java            # 航班预订处理器
            │   ├── 📄 CurrentUserHandler.java        # 当前用户信息处理器
            │   ├── 📄 FindFlightHandler.java         # 查找航班处理器
            │   ├── 📄 GetAllFlightsHandler.java      # 获取所有航班处理器
            │   ├── 📄 GetAllUsersHandler.java        # 获取所有用户处理器
            │   ├── 📄 HelloHandler.java              # 测试用Hello处理器
            │   ├── 📄 LoginHandler.java              # 用户登录处理器
            │   ├── 📄 LogoutHandler.java             # 用户登出处理器
            │   ├── 📄 ModifyUserAuthorityHandler.java # 修改用户权限处理器
            │   ├── 📄 OrderManagementHandler.java    # 订单管理处理器
            │   ├── 📄 PaymentCancelHandler.java      # 支付取消处理器
            │   ├── 📄 PaymentCreateHandler.java      # 支付创建处理器
            │   ├── 📄 PaymentStatusHandler.java      # 支付状态查询处理器
            │   ├── 📄 RegisterHandler.java           # 用户注册处理器
            │   ├── 📄 ResourceBasedStaticHandler.java # 静态资源处理器
            │   ├── 📄 SimpleFlightSearchHandler.java # 简单航班搜索处理器
            │   ├── 📄 SimplePaymentCreateHandler.java # 简单支付创建处理器
            │   ├── 📄 TestHandler.java               # 测试处理器
            │   ├── 📄 UpdateUserHandler.java         # 更新用户信息处理器
            │   └── 📄 UsersHandler.java              # 用户管理处理器
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
            │   ├── 📄 FlightSearchService.java   # 航班搜索服务
            │   └── 📄 OrderService.java          # 订单管理服务
            │
            ├── 📁 test/                     # 🧪 测试类
            │   └── 📄 AlipayTest.java            # 支付宝测试
            │
            └── 📁 utils/                    # 🛠️ 工具类库
                └── 📄 JsonUtil.java              # JSON处理工具
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
                │   ├── 📄 booking.js                # 航班预订页面脚本
                │   ├── 📄 bootstrap.bundle.js       # Bootstrap框架脚本
                │   ├── 📄 bootstrap.js              # Bootstrap核心脚本
                │   ├── 📄 custom.js                 # 自定义通用脚本
                │   ├── 📄 jquery.mCustomScrollbar.concat.min.js # 自定义滚动条插件
                │   ├── 📄 jquery.min.js             # jQuery核心库
                │   ├── 📄 jquery.validate.js        # jQuery表单验证插件
                │   ├── 📄 jquery-3.0.0.min.js       # jQuery 3.0版本
                │   ├── 📄 modernizer.js             # 现代化浏览器特性检测
                │   ├── 📄 order.js                  # 订单管理页面脚本
                │   ├── 📄 plugin.js                 # 通用插件脚本
                │   ├── 📄 popper.min.js             # Popper定位库
                │   └── 📄 slider-setting.js         # 滑块设置脚本
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

### 2. **数据传输对象层 (DTO Layer)**
- `dto/` - 2个DTO类，用于数据传输和响应格式化
- 封装复杂查询结果和统计数据

### 3. **请求处理层 (Handler Layer)**
- `handler/` - 20个处理器类，负责HTTP请求处理
- 实现RESTful API接口，处理用户请求和响应

### 4. **数据模型层 (Model Layer)**
- `model/` - 8个实体类，对应数据库表结构
- 定义核心业务对象：用户、航班、订单、支付等

### 5. **业务服务层 (Service Layer)**
- `service/` - 4个核心服务类
- 封装业务逻辑：搜索、预订、支付、订单管理

### 6. **服务器层 (Server Layer)**
- `server/` - HTTP服务器实现
- 处理HTTP请求和响应

### 7. **工具层 (Utils Layer)**
- `utils/` - 通用工具类
- JSON处理和系统启动入口

## 🔧 核心组件详情

| 层级 | 组件数量 | 主要文件 |
|------|---------|----------|
| **DAO层** | 8个 | UserDao, FlightDao, OrderDao, PayrecordDao等 |
| **DTO层** | 2个 | OrderDetailDTO, OrderStatsDTO |
| **Handler层** | 20个 | 完整的HTTP请求处理器，覆盖所有API接口 |
| **Model层** | 8个 | User, Flight, Order, Payrecord, Airport等 |
| **Service层** | 4个 | BookingService, FlightSearchService, AlipayService, OrderService |
| **Server层** | 1个 | SimpleHttpServer |
| **Utils层** | 1个 | JsonUtil |
| **前端页面** | 9个 | 完整的用户界面，包含搜索、预订、支付等功能 |
| **前端脚本** | 12个 | jQuery、Bootstrap、自定义业务逻辑脚本 |
| **数据库脚本** | 2个 | 建表脚本和初始化数据 |
| **测试类** | 2个 | AirlinecompanyDaoTest, AlipayTest |

## 🌐 HTTP处理器详细列表

### 🔐 用户认证相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **LoginHandler** | 用户登录认证 | `/api/login` |
| **LogoutHandler** | 用户登出 | `/api/logout` |
| **RegisterHandler** | 用户注册 | `/api/register` |
| **CurrentUserHandler** | 获取当前用户信息 | `/api/current-user` |

### 👥 用户管理相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **UsersHandler** | 用户管理主处理器 | `/api/users` |
| **GetAllUsersHandler** | 获取所有用户列表 | `/api/users/all` |
| **UpdateUserHandler** | 更新用户信息 | `/api/users/update` |
| **ModifyUserAuthorityHandler** | 修改用户权限 | `/api/users/authority` |

### ✈️ 航班管理相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **AddFlightHandler** | 添加新航班 | `/api/flights/add` |
| **FindFlightHandler** | 查找特定航班 | `/api/flights/find` |
| **GetAllFlightsHandler** | 获取所有航班 | `/api/flights/all` |
| **SimpleFlightSearchHandler** | 简单航班搜索 | `/api/flights/search` |

### 📋 订单管理相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **BookingHandler** | 航班预订处理 | `/api/booking` |
| **OrderManagementHandler** | 订单管理 | `/api/orders` |

### 💳 支付管理相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **PaymentCreateHandler** | 创建支付订单 | `/api/payment/create` |
| **PaymentStatusHandler** | 查询支付状态 | `/api/payment/status` |
| **PaymentCancelHandler** | 取消支付 | `/api/payment/cancel` |
| **SimplePaymentCreateHandler** | 简单支付创建 | `/api/payment/simple` |

### 🛠️ 系统工具相关
| 处理器 | 功能描述 | API路径 |
|--------|----------|---------|
| **HelloHandler** | 系统健康检查 | `/api/hello` |
| **TestHandler** | 测试接口 | `/api/test` |
| **ResourceBasedStaticHandler** | 静态资源处理 | `/static/*` |

## 📦 DTO数据传输对象详情

### 📋 OrderDetailDTO - 订单详情数据传输对象
用于封装订单的详细信息，包括用户、航班、支付等相关数据，主要用于订单查询和详情展示的数据传输。

### 📊 OrderStatsDTO - 订单统计数据传输对象
用于封装订单统计分析数据，主要用于管理员后台的数据统计和报表展示。

## 📜 JavaScript文件详情

### 🔧 核心库文件
| 文件名 | 功能描述 | 版本 |
|--------|----------|------|
| **jquery.min.js** | jQuery核心库 | 最新版本 |
| **jquery-3.0.0.min.js** | jQuery核心库 | 3.0.0版本 |
| **bootstrap.js** | Bootstrap核心脚本 | - |
| **bootstrap.bundle.js** | Bootstrap完整包 | - |
| **popper.min.js** | Popper定位库 | - |

### 🔌 插件文件
| 文件名 | 功能描述 | 用途 |
|--------|----------|------|
| **jquery.validate.js** | jQuery表单验证插件 | 用户输入验证 |
| **jquery.mCustomScrollbar.concat.min.js** | 自定义滚动条插件 | 美化滚动条 |
| **modernizer.js** | 现代化浏览器特性检测 | 兼容性处理 |
| **slider-setting.js** | 滑块设置脚本 | 轮播图和滑块 |
| **plugin.js** | 通用插件脚本 | 公共功能 |

### 🎯 业务文件
| 文件名 | 功能描述 | 对应页面 |
|--------|----------|----------|
| **booking.js** | 航班预订页面脚本 | booking.html |
| **order.js** | 订单管理页面脚本 | order.html |
| **custom.js** | 自定义通用脚本 | 全局使用 |

## 📦 技术特点

- **纯Java实现** - 不依赖Spring等框架，轻量级架构
- **分层架构** - DAO-DTO-Handler-Model-Service-Server清晰分层
- **RESTful API** - 20个处理器提供完整的HTTP接口
- **完整前端** - 9个HTML页面覆盖全业务流程
- **支付集成** - 集成支付宝支付服务，含使用文档
- **JSON支持** - 自研JSON处理工具
- **数据库支持** - 完整的SQL建表和数据脚本
- **演示页面** - 提供demo版本便于测试和展示

## 🎯 页面功能

| 页面 | 功能描述 | 对应Handler |
|------|----------|-------------|
| **main.html** | 系统首页，导航和功能入口 | ResourceBasedStaticHandler |
| **search.html / search_demo.html** | 航班搜索页面和演示版本 | SimpleFlightSearchHandler |
| **booking.html / booking_demo.html** | 航班预订页面和演示版本 | BookingHandler |
| **payment.html** | 支付页面，支持支付宝 | PaymentCreateHandler |
| **personal.html** | 个人中心，订单管理 | CurrentUserHandler, OrderManagementHandler |
| **admin.html** | 管理员后台管理 | GetAllUsersHandler, GetAllFlightsHandler |
| **sign_log.html** | 用户登录注册 | LoginHandler, RegisterHandler |
| **order.html** | 订单管理页面 | OrderManagementHandler |

## 🚀 API接口概览

### 🔐 认证接口
- `POST /api/login` - 用户登录
- `POST /api/logout` - 用户登出
- `POST /api/register` - 用户注册
- `GET /api/current-user` - 获取当前用户

### 👥 用户管理接口
- `GET /api/users` - 用户管理
- `GET /api/users/all` - 获取所有用户
- `PUT /api/users/update` - 更新用户信息
- `PUT /api/users/authority` - 修改用户权限

### ✈️ 航班管理接口
- `POST /api/flights/add` - 添加航班
- `GET /api/flights/find` - 查找航班
- `GET /api/flights/all` - 获取所有航班
- `GET /api/flights/search` - 搜索航班

### 📋 订单管理接口
- `POST /api/booking` - 创建预订
- `GET /api/orders` - 订单管理

### 💳 支付管理接口
- `POST /api/payment/create` - 创建支付
- `GET /api/payment/status` - 查询支付状态
- `POST /api/payment/cancel` - 取消支付
- `POST /api/payment/simple` - 简单支付

### 🛠️ 系统接口
- `GET /api/hello` - 健康检查
- `GET /api/test` - 测试接口
- `GET /static/*` - 静态资源

## 🚀 启动方式

```bash
# 编译项目
mvn clean compile

# 运行主程序
java -cp target/classes utils.Main
```

## 📋 关键文件说明

### 🚀 核心启动文件
- **Main.java** - 系统启动入口

### 🌐 HTTP服务层
- **SimpleHttpServer.java** - 基于Java内置HttpServer的Web服务器
- **20个Handler类** - 处理所有API请求的核心逻辑

### 📦 数据传输层
- **OrderDetailDTO.java** - 订单详情数据传输对象
- **OrderStatsDTO.java** - 订单统计数据传输对象

### 📊 数据模型层
- **8个实体类** - 完整映射数据库表结构
- 支持用户、航班、订单、支付等全业务场景

### 🗄️ 数据访问层
- **纯JDBC实现** - 不依赖任何ORM框架
- **8个DAO类** - 对应各实体的CRUD操作

### 🛠️ 工具类层
- **JsonUtil.java** - 手写JSON序列化工具
- 各种业务工具类

### 🌐 前端资源
- **HTML页面** - 简洁的用户界面
- **CSS样式** - 现代化的UI设计
- **JavaScript脚本** - 前后端API交互
    - jQuery系列：核心库、验证插件、自定义滚动条
    - Bootstrap：响应式框架和组件
    - 业务脚本：booking.js（预订）、order.js（订单）
    - 工具库：modernizer.js、popper.min.js、slider-setting.js

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
- 📦 **分层清晰** - 六层架构，职责分明
- 🔧 **高内聚低耦合** - 各模块独立，易于维护
- 🎯 **业务完整** - 覆盖航空订票全流程
- 🌐 **RESTful API** - 20个处理器提供完整接口

### ✅ 开发友好
- 🚀 **启动超快** - 3秒内启动完成
- 🐛 **易于调试** - 纯Java代码，逻辑清晰
- 📝 **文档完整** - API文档、部署文档齐全
- 🔄 **热部署** - 支持开发时快速重启

## 🚀 快速启动

```bash
# 1. 克隆项目
git clone https://github.com/J1AL1Lee/FlightBookingSystem.git
cd FlightBookingSystem

# 2. 编译项目
mvn clean compile

# 3. 启动系统
mvn exec:java

# 4. 访问系统
# 主页: http://localhost:8080/main.html
# API测试: http://localhost:8080/api/hello
# 航班搜索: http://localhost:8080/api/flights/search
```

## 🔧 开发指南

### 添加新的Handler
1. 在 `handler/` 目录下创建新的Handler类
2. 实现HTTP请求处理逻辑
3. 在 `SimpleHttpServer.java` 中注册路由
4. 编写对应的前端页面

### 添加新的DTO
1. 在 `dto/` 目录下创建新的DTO类
2. 添加完整的字段注释
3. 实现getter/setter方法
4. 在对应的Handler中使用

### 数据库扩展
1. 修改 `create_table.sql` 添加新表
2. 在 `model/` 目录下创建对应实体类
3. 在 `dao/` 目录下创建对应DAO类
4. 更新相关的Service和Handler

## 📊 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        用户界面层                              │
│  main.html │ search.html │ booking.html │ payment.html      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     HTTP处理器层                             │
│  LoginHandler │ FlightHandler │ BookingHandler │ PaymentHandler │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      业务服务层                              │
│  BookingService │ FlightSearchService │ AlipayService      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     数据访问层                               │
│  UserDao │ FlightDao │ OrderDao │ PayrecordDao             │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      数据库层                                │
│            MySQL Database                                  │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 总结

这是一个完整的航空订票系统，采用传统的MVC架构，具有以下特点：

- **完整性**: 覆盖用户注册、登录、搜索、预订、支付、管理等全流程
- **轻量级**: 不依赖重型框架，启动快速，易于部署
- **可扩展**: 模块化设计，易于添加新功能
- **生产就绪**: 包含完整的前端界面和后端API
- **文档完善**: 详细的代码注释和使用说明

适合用于学习Java Web开发、理解分层架构设计，或作为小型项目的基础框架。