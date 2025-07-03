// order.js - 订单管理页面脚本

// 订单管理类
class OrderManager {
    constructor() {
        this.orders = [];
        this.currentFilter = 'all';
        this.init();
    }

    // 初始化
    init() {
        this.bindEvents();
        this.loadOrders();
    }

    // 绑定事件
    bindEvents() {
        // 标签页切换
        document.querySelectorAll('[data-bs-toggle="tab"]').forEach(tab => {
            tab.addEventListener('click', (e) => {
                const target = e.target.getAttribute('data-bs-target');
                this.currentFilter = target.replace('#', '');
                this.renderFilteredOrders();
            });
        });
    }

    // 从后端API加载订单数据
    async loadOrders() {
        this.showLoading();

        try {
            const response = await fetch('/api/orders/user', {
                method: 'GET',
                credentials: 'include', // 包含cookies用于身份验证
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                if (response.status === 401) {
                    this.showMessage('请先登录', 'error');
                    window.location.href = '/sign_log.html';
                    return;
                }
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();

            if (result.success) {
                this.orders = result.data || [];
                this.updateStats();
                this.renderFilteredOrders();
                console.log('✅ 订单数据加载成功，共', this.orders.length, '条');
            } else {
                throw new Error(result.message || '加载订单失败');
            }

        } catch (error) {
            console.error('❌ 加载订单失败:', error);
            this.showMessage('加载订单失败: ' + error.message, 'error');
            this.showEmptyState();
        } finally {
            this.hideLoading();
        }
    }

    // 更新统计数据
    updateStats() {
        const stats = this.calculateStats();

        document.getElementById('pendingCount').textContent = stats.pending;
        document.getElementById('upcomingCount').textContent = stats.upcoming;
        document.getElementById('completedCount').textContent = stats.completed;
        document.getElementById('totalCount').textContent = stats.total;
    }

    // 计算统计数据
    calculateStats() {
        const pending = this.orders.filter(order => order.payState === '未支付').length;
        const upcoming = this.orders.filter(order =>
            order.payState === '已支付' &&
            order.flightTime &&
            new Date(order.flightTime + 'T00:00:00') > new Date()).length;
        const completed = this.orders.filter(order =>
            order.payState === '已支付' &&
            order.flightTime &&
            new Date(order.flightTime + 'T00:00:00') <= new Date()).length;
        const total = this.orders.length;

        return { pending, upcoming, completed, total };
    }

    // 根据当前过滤器渲染订单
    renderFilteredOrders() {
        const filteredOrders = this.getFilteredOrders();
        this.renderOrders(filteredOrders);
    }

    // 获取过滤后的订单
    getFilteredOrders() {
        switch(this.currentFilter) {
            case 'pending':
                return this.orders.filter(order => order.payState === '未支付');
            case 'upcoming':
                return this.orders.filter(order =>
                    order.payState === '已支付' &&
                    order.flightTime &&
                    new Date(order.flightTime + 'T00:00:00') > new Date());
            case 'completed':
                return this.orders.filter(order =>
                    order.payState === '已支付' &&
                    order.flightTime &&
                    new Date(order.flightTime + 'T00:00:00') <= new Date());
            case 'all':
            default:
                return this.orders;
        }
    }

    // 渲染订单列表
    renderOrders(orders) {
        // 清空所有容器
        document.getElementById('allOrders').innerHTML = '';
        document.getElementById('pendingOrders').innerHTML = '';
        document.getElementById('upcomingOrders').innerHTML = '';
        document.getElementById('completedOrders').innerHTML = '';

        if (orders.length === 0) {
            this.showEmptyState();
            return;
        }

        this.hideEmptyState();

        // 根据当前过滤器渲染到对应容器
        const container = this.getContainerByFilter();
        container.innerHTML = orders.map(order => this.createOrderCard(order)).join('');

        // 绑定订单操作事件
        this.bindOrderActions();
    }

    // 根据过滤器获取容器
    getContainerByFilter() {
        switch(this.currentFilter) {
            case 'pending':
                return document.getElementById('pendingOrders');
            case 'upcoming':
                return document.getElementById('upcomingOrders');
            case 'completed':
                return document.getElementById('completedOrders');
            case 'all':
            default:
                return document.getElementById('allOrders');
        }
    }

    // 创建订单卡片HTML
    createOrderCard(order) {
        const statusClass = this.getStatusClass(order.payState);
        const statusText = order.payState || '未知状态';

        // 格式化日期时间
        const orderDate = this.formatDateTime(order.orderTime);
        const flightDate = this.formatDate(order.flightTime);
        const payAmount = order.payment || order.price || 0;
        const payMethod = order.payMethod || '未选择';

        // 构建机场信息
        const airportInfo = this.buildAirportInfo(order);

        return `
            <div class="order-card" data-order-id="${order.orderId}">
                <div class="order-header">
                    <div class="order-info">
                        <h4>订单号: ${order.orderId}</h4>
                        <div class="flight-number">航班号: ${order.flightId}</div>
                        <div class="flight-time">
                            <i class="fas fa-calendar"></i>
                            航班日期: ${flightDate}
                        </div>
                    </div>
                    <div class="order-date">
                        <small>下单时间</small><br>
                        ${orderDate}
                    </div>
                </div>

                <div class="order-body">
                    <div class="order-detail">
                        <h5>用户信息</h5>
                        <p>用户ID: ${order.userId}</p>
                    </div>
                    
                    <div class="order-detail">
                        <h5>航线信息</h5>
                        <div class="flight-route">
                            <div class="route-point">
                                <div class="departure-city">${airportInfo.departure}</div>
                            </div>
                            <div class="route-arrow">
                                <i class="fas fa-plane"></i>
                            </div>
                            <div class="route-point">
                                <div class="arrival-city">${airportInfo.arrival}</div>
                            </div>
                        </div>
                        ${order.departureTime ? `<small>起飞: ${order.departureTime} | 到达: ${order.arrivalTime}</small>` : ''}
                    </div>
                    
                    <div class="order-detail">
                        <h5>支付信息</h5>
                        <div class="price-amount">¥${payAmount}</div>
                        <div class="pay-method">支付方式: ${payMethod}</div>
                        <span class="order-status ${statusClass}">${statusText}</span>
                    </div>
                </div>

                <div class="order-actions">
                    <button class="btn-action btn-secondary-action" onclick="orderManager.showOrderDetail('${order.orderId}')">
                        <i class="fas fa-info-circle"></i> 详情
                    </button>
                    ${this.shouldShowRefundButton(order) ? `
                        <button class="btn-action btn-danger-action" onclick="orderManager.requestRefund('${order.orderId}')">
                            <i class="fas fa-undo"></i> 申请退款
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
    }

    // 构建机场信息显示
    buildAirportInfo(order) {
        let departure = '出发机场';
        let arrival = '到达机场';

        if (order.departureAirport && order.departureCity) {
            departure = `${order.departureCity} ${order.departureAirport}`;
        } else if (order.departureAirport) {
            departure = order.departureAirport;
        }

        if (order.arrivalAirport && order.arrivalCity) {
            arrival = `${order.arrivalCity} ${order.arrivalAirport}`;
        } else if (order.arrivalAirport) {
            arrival = order.arrivalAirport;
        }

        return { departure, arrival };
    }

    // 判断是否显示退款按钮
    shouldShowRefundButton(order) {
        // 只有已支付且未取消的订单才能申请退款
        return (order.payState === '已支付' || order.orderState === '正常') &&
            order.orderState !== '已取消' &&
            order.payState !== '已退款';
    }

    // 获取状态样式类
    getStatusClass(status) {
        const statusMap = {
            '未支付': 'status-pending',
            '已支付': 'status-paid',
            '正常': 'status-paid',
            '已完成': 'status-completed',
            '已取消': 'status-cancelled',
            '已退款': 'status-refunded'
        };
        return statusMap[status] || 'status-pending';
    }

    // 格式化日期时间
    formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '未知时间';

        try {
            const date = new Date(dateTimeStr);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return dateTimeStr;
        }
    }

    // 格式化日期
    formatDate(dateStr) {
        if (!dateStr) return '未知日期';

        try {
            const date = new Date(dateStr);
            return date.toLocaleDateString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        } catch (e) {
            return dateStr;
        }
    }

    // 绑定订单操作事件
    bindOrderActions() {
        // 事件已通过onclick绑定，这里可以添加其他事件处理
    }

    // 显示订单详情
    showOrderDetail(orderId) {
        const order = this.orders.find(o => o.orderId === orderId);
        if (!order) return;

        const modalContent = document.getElementById('orderDetailContent');
        const airportInfo = this.buildAirportInfo(order);

        modalContent.innerHTML = `
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-muted mb-3">订单信息</h6>
                    <p><strong>订单号:</strong> ${order.orderId}</p>
                    <p><strong>用户ID:</strong> ${order.userId}</p>
                    <p><strong>下单时间:</strong> ${this.formatDateTime(order.orderTime)}</p>
                    <p><strong>订单状态:</strong> <span class="order-status ${this.getStatusClass(order.orderState)}">${order.orderState}</span></p>
                </div>
                <div class="col-md-6">
                    <h6 class="text-muted mb-3">航班信息</h6>
                    <p><strong>航班号:</strong> ${order.flightId}</p>
                    <p><strong>航班日期:</strong> ${this.formatDate(order.flightTime)}</p>
                    <p><strong>出发:</strong> ${airportInfo.departure}</p>
                    <p><strong>到达:</strong> ${airportInfo.arrival}</p>
                    ${order.departureTime ? `<p><strong>起飞时间:</strong> ${order.departureTime}</p>` : ''}
                    ${order.arrivalTime ? `<p><strong>到达时间:</strong> ${order.arrivalTime}</p>` : ''}
                </div>
            </div>
            <hr>
            <div class="row">
                <div class="col-md-6">
                    <h6 class="text-muted mb-3">座位信息</h6>
                    <p><strong>座位类型:</strong> ${order.seatTypeName || (order.seatType == 0 ? '经济舱' : '商务舱')}</p>
                    <p><strong>座位号:</strong> ${order.seatId}</p>
                </div>
                <div class="col-md-6">
                    <h6 class="text-muted mb-3">支付信息</h6>
                    <p><strong>支付金额:</strong> <span class="text-danger fw-bold">¥${order.payment || order.price || 0}</span></p>
                    <p><strong>支付方式:</strong> ${order.payMethod || '未选择'}</p>
                    <p><strong>支付状态:</strong> <span class="order-status ${this.getStatusClass(order.payState)}">${order.payState || '未知'}</span></p>
                    ${order.payTime ? `<p><strong>支付时间:</strong> ${this.formatDateTime(order.payTime)}</p>` : ''}
                </div>
            </div>
        `;

        // 显示模态框
        const modal = new bootstrap.Modal(document.getElementById('orderDetailModal'));
        modal.show();
    }

    // 申请退款
    async requestRefund(orderId) {
        // 显示确认对话框
        if (!confirm('确定要申请退款吗？\n\n退款后：\n• 订单状态将变为"已取消"\n• 支付状态将变为"已退款"\n• 座位将被释放\n\n此操作不可撤销！')) {
            return;
        }

        this.showLoading();

        try {
            const response = await fetch('/api/orders/refund', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    orderId: orderId,
                    reason: '用户申请退款'
                })
            });

            const result = await response.json();

            if (result.success) {
                this.showMessage('退款成功！订单已取消，座位已释放', 'success');
                // 刷新订单列表
                await this.loadOrders();
            } else {
                throw new Error(result.message || '退款失败');
            }

        } catch (error) {
            console.error('❌ 退款失败:', error);
            this.showMessage('退款失败: ' + error.message, 'error');
        } finally {
            this.hideLoading();
        }
    }

    // 显示加载状态
    showLoading() {
        document.getElementById('loadingContainer').style.display = 'block';
    }

    // 隐藏加载状态
    hideLoading() {
        document.getElementById('loadingContainer').style.display = 'none';
    }

    // 显示空状态
    showEmptyState() {
        document.getElementById('emptyState').style.display = 'block';
    }

    // 隐藏空状态
    hideEmptyState() {
        document.getElementById('emptyState').style.display = 'none';
    }

    // 显示消息提示
    showMessage(message, type = 'info') {
        // 创建临时提示元素
        const alert = document.createElement('div');
        alert.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
        alert.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        alert.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        document.body.appendChild(alert);

        // 3秒后自动移除
        setTimeout(() => {
            if (alert.parentNode) {
                alert.parentNode.removeChild(alert);
            }
        }, 3000);
    }

    // 刷新订单数据
    async refreshOrders() {
        await this.loadOrders();
    }
}

// 全局订单管理实例
let orderManager;

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    orderManager = new OrderManager();

    // 绑定刷新按钮（如果有的话）
    const refreshBtn = document.getElementById('refreshBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            orderManager.refreshOrders();
        });
    }
});

// 导出函数供HTML调用
window.orderManager = orderManager;