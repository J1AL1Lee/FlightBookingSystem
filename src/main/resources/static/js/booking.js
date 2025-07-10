// 全局变量
let selectedSeatType = 0; // 默认选择经济舱
let flightrecordId = null;
let flightId = null;
let flightInfo = null;
let currentUser = null;
let priceInfo = {
    economy: { price: 0, originalPrice: 0, hasDiscount: false },
    business: { price: 0, originalPrice: 0, hasDiscount: false }
};

// 🔧 添加防重复提交的标志
let isBookingInProgress = false;

// 封装安全的API请求工具类
const ApiClient = {
    request: function(url, options = {}) {
        const defaultOptions = {
            type: 'GET',
            contentType: 'application/json; charset=utf-8',
            timeout: 10000,
            xhrFields: {
                withCredentials: true // 重要：包含Cookie用于session认证
            }
        };

        const finalOptions = Object.assign({}, defaultOptions, options);

        return new Promise((resolve, reject) => {
            $.ajax(finalOptions.url || url, {
                ...finalOptions,
                success: function(data, textStatus, xhr) {
                    // 检查是否因为session过期需要重新登录
                    if (data && data.success === false && data.message === '未登录') {
                        redirectToLogin('登录已过期，请重新登录');
                        return;
                    }
                    resolve(data);
                },
                error: function(xhr, textStatus, errorThrown) {
                    // 401表示未认证，跳转到登录页
                    if (xhr.status === 401) {
                        redirectToLogin('登录已过期，请重新登录');
                        return;
                    }

                    let errorMsg = '请求失败';
                    try {
                        const response = JSON.parse(xhr.responseText);
                        errorMsg = response.message || errorMsg;
                    } catch (e) {
                        if (xhr.status === 0) {
                            errorMsg = '网络连接失败，请检查网络';
                        } else {
                            errorMsg = `服务器错误 (${xhr.status})`;
                        }
                    }
                    reject(new Error(errorMsg));
                }
            });
        });
    }
};

// 页面加载时执行 - 🎯 合并到一个事件监听器
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 页面加载完成，开始初始化...');

    // 🔧 绑定预订按钮事件（只绑定一次）
    const bookBtn = document.getElementById('bookBtn');
    if (bookBtn) {
        // 🎯 移除可能存在的旧事件监听器
        bookBtn.removeEventListener('click', confirmBooking);

        // 🎯 绑定新的事件监听器
        bookBtn.addEventListener('click', function(event) {
            event.preventDefault(); // 防止表单提交
            event.stopPropagation(); // 阻止事件冒泡

            console.log('🎯 预订按钮被点击了！');

            // 🔧 防重复提交检查
            if (isBookingInProgress) {
                console.log('⚠️ 预订正在进行中，忽略重复点击');
                return;
            }

            confirmBooking();
        });

        console.log('✅ 预订按钮事件绑定完成');
    } else {
        console.error('❌ 未找到预订按钮元素');
    }

    // 初始化页面
    initializePage();
});

// 初始化页面
async function initializePage() {
    try {
        showMessage('正在加载航班和用户信息...', 'info');

        // 1. 检查用户登录状态
        const isLoggedIn = await checkLoginStatus();
        if (!isLoggedIn) {
            return;
        }

        // 2. 获取URL参数
        if (!getUrlParameters()) {
            return;
        }

        // 3. 通过航班号查询API获取航班信息
        await loadFlightInfoByApi();

        // 4. 加载价格信息（基于获取到的航班信息）
        await loadPriceInfo();

        // 5. 默认选择经济舱
        selectSeat(0);

        // 清除加载消息
        clearMessages();
        showMessage('页面加载完成！您可以选择座位类型并确认预订。', 'success');

        console.log('✅ 页面初始化完成');

    } catch (error) {
        console.error('❌ 页面初始化失败:', error);
        showMessage('页面初始化失败：' + error.message, 'error');

        // 如果初始化失败，禁用预订按钮
        const bookBtn = document.getElementById('bookBtn');
        if (bookBtn) {
            bookBtn.disabled = true;
            bookBtn.innerHTML = '<i class="fa fa-exclamation-triangle"></i> 加载失败';
        }
    }
}

// 安全的登录状态检查函数
async function checkLoginStatus() {
    try {
        // 首先检查本地是否有登录标记
        const isLoggedIn = localStorage.getItem('isLoggedIn');
        if (isLoggedIn !== 'true') {
            redirectToLogin('请先登录后再进行预订');
            return false;
        }

        // 从服务器验证session是否有效并获取完整用户信息
        const response = await ApiClient.request('/api/user/current');

        if (response && response.success && response.userInfo) {
            // 登录有效，设置当前用户信息
            currentUser = response.userInfo;
            console.log('✅ 当前用户:', currentUser);

            // 显示用户信息
            displayUserInfo();

            return true;
        } else {
            // session无效，清除本地状态
            clearLocalLoginState();
            redirectToLogin('登录已过期，请重新登录');
            return false;
        }

    } catch (error) {
        console.error('❌ 检查登录状态失败:', error);

        // 网络错误或其他问题，清除本地状态
        clearLocalLoginState();
        redirectToLogin('验证登录状态失败，请重新登录');
        return false;
    }
}

// 清除本地登录状态
function clearLocalLoginState() {
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('userName');
    // 清理可能残留的旧版本数据
    localStorage.removeItem('userInfo');
    sessionStorage.removeItem('currentUser');
}

// 跳转到登录页面
function redirectToLogin(message) {
    // 保存当前页面URL，登录后可以回到这里
    localStorage.setItem('redirectUrl', window.location.href);

    if (message) {
        showMessage(message, 'error');
    }

    setTimeout(() => {
        window.location.href = 'sign_log.html';
    }, 2000);
}

// 获取URL参数
function getUrlParameters() {
    const urlParams = new URLSearchParams(window.location.search);
    flightrecordId = urlParams.get('flightrecordId');
    flightId = urlParams.get('flightId');

    if (!flightrecordId) {
        showMessage('缺少航班信息，请返回重新选择', 'error');
        setTimeout(() => {
            window.location.href = 'search.html';
        }, 2000);
        return false;
    }

    // 如果没有flightId，从flightrecordId中解析
    if (!flightId && flightrecordId) {
        flightId = flightrecordId.substring(0, flightrecordId.length - 8);
    }

    console.log('📝 航班参数:', {
        flightrecordId,
        flightId
    });

    return true;
}

// 通过航班号查询API获取航班信息
async function loadFlightInfoByApi() {
    try {
        console.log('✈️ 开始通过API加载航班信息...');

        if (!flightId) {
            throw new Error('航班号不能为空');
        }

        // 从flightrecordId解析日期
        const flightDate = flightrecordId.substring(flightrecordId.length - 8);
        const formattedDate = `${flightDate.substring(0,4)}-${flightDate.substring(4,6)}-${flightDate.substring(6,8)}`;

        console.log('🔍 查询航班:', flightId, '日期:', formattedDate);

        // 使用安全的API请求
        const result = await ApiClient.request('/api/flights/search', {
            type: 'POST',
            data: JSON.stringify({
                flightId: flightId,
                flightDate: formattedDate,
                fuzzySearch: false, // 精确搜索
                userId: currentUser.userId
            })
        });

        console.log('🔍 API响应:', result);

        if (!result.success) {
            throw new Error(result.message || '查询航班信息失败');
        }

        if (!result.data || result.data.length === 0) {
            throw new Error(`未找到航班 ${flightId} 在 ${formattedDate} 的信息`);
        }

        // 查找匹配的航班记录
        let matchedFlight = result.data.find(flight =>
            flight.flightrecordId === flightrecordId
        );

        // 如果没找到完全匹配的记录，使用第一个航班
        if (!matchedFlight) {
            console.warn('⚠️ 未找到完全匹配的航班记录，使用第一个匹配的航班');
            matchedFlight = result.data[0];
        }

        // 转换API返回的数据格式
        flightInfo = {
            flightId: matchedFlight.flightId,
            flightrecordId: matchedFlight.flightrecordId,
            airlineName: matchedFlight.airlineName,
            airportFrom: matchedFlight.airportFrom,
            airportTo: matchedFlight.airportTo,
            timeTakeoff: matchedFlight.timeTakeoff,
            timeArrive: matchedFlight.timeArrive,
            flightDate: matchedFlight.flightDate,
            seat0Left: matchedFlight.seat0Left,
            seat1Left: matchedFlight.seat1Left,
            finalPrice0: matchedFlight.finalPrice0,
            finalPrice1: matchedFlight.finalPrice1,
            originalPrice0: matchedFlight.originalPrice0,
            originalPrice1: matchedFlight.originalPrice1,
            isVipUser: matchedFlight.isVipUser,
            hasDiscount: matchedFlight.hasDiscount,
            discount: matchedFlight.discount
        };

        // 更新页面显示
        updateFlightDisplay();

        console.log('✅ 航班信息加载成功:', flightInfo);

    } catch (error) {
        console.error('❌ 加载航班信息失败:', error);
        throw new Error('加载航班信息失败：' + error.message);
    }
}

// 加载价格信息
async function loadPriceInfo() {
    try {
        console.log('💰 开始加载价格信息...');

        if (!flightInfo) {
            throw new Error('航班信息未加载，无法获取价格');
        }

        // 从API返回的航班信息中提取价格信息
        priceInfo.economy.price = flightInfo.finalPrice0 || 0;
        priceInfo.business.price = flightInfo.finalPrice1 || 0;

        // 处理VIP折扣信息
        if (flightInfo.hasDiscount && flightInfo.isVipUser) {
            priceInfo.economy.originalPrice = flightInfo.originalPrice0 || flightInfo.finalPrice0;
            priceInfo.business.originalPrice = flightInfo.originalPrice1 || flightInfo.finalPrice1;
            priceInfo.economy.hasDiscount = true;
            priceInfo.business.hasDiscount = true;

            console.log('🎉 检测到VIP折扣');
        } else {
            priceInfo.economy.hasDiscount = false;
            priceInfo.business.hasDiscount = false;
        }

        // 更新价格显示
        updatePriceDisplay();

        console.log('✅ 价格信息加载完成:', priceInfo);

    } catch (error) {
        console.error('❌ 加载价格信息失败:', error);

        // 如果价格信息加载失败，使用默认价格
        priceInfo.economy.price = 500;
        priceInfo.business.price = 1200;
        priceInfo.economy.hasDiscount = false;
        priceInfo.business.hasDiscount = false;

        updatePriceDisplay();
        throw new Error('加载价格信息失败：' + error.message);
    }
}

// 显示用户信息
function displayUserInfo() {
    if (!currentUser) return;

    const elements = {
        passengerName: currentUser.userName || '',
        passengerPhone: currentUser.userTelephone || '',
        userId: currentUser.userId || '',
        vipStatus: currentUser.vipState === '是' ? 'VIP会员' : '普通会员'
    };

    // 更新表单元素
    Object.keys(elements).forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.value = elements[id];
        }
    });

    // 显示VIP状态
    if (currentUser.vipState === '是') {
        const vipBadgeContainer = document.getElementById('vipBadgeContainer');
        if (vipBadgeContainer && !vipBadgeContainer.querySelector('.vip-badge')) {
            const vipBadge = document.createElement('span');
            vipBadge.className = 'vip-badge';
            vipBadge.innerHTML = '<i class="fa fa-crown"></i> VIP会员';
            vipBadgeContainer.appendChild(vipBadge);
        }

        // 显示VIP折扣信息
        const discountInfo = document.getElementById('vipDiscountInfo');
        if (discountInfo) {
            discountInfo.innerHTML = '<div class="discount-info"><i class="fa fa-star"></i> 恭喜！作为VIP会员，您将享受专属优惠价格</div>';
            discountInfo.style.display = 'block';
        }
    }
}

// 更新航班信息显示
function updateFlightDisplay() {
    if (!flightInfo) {
        console.warn('⚠️ 航班信息为空，无法更新显示');
        return;
    }

    console.log('🖼️ 更新航班显示:', flightInfo);

    // 更新基本信息
    const updates = {
        flightNumber: flightInfo.flightId,
        airlineName: flightInfo.airlineName,
        departureCode: flightInfo.airportFrom,
        departureName: getAirportName(flightInfo.airportFrom),
        arrivalCode: flightInfo.airportTo,
        arrivalName: getAirportName(flightInfo.airportTo),
        departureTime: flightInfo.timeTakeoff,
        arrivalTime: flightInfo.timeArrive,
        flightDate: flightInfo.flightDate
    };

    Object.keys(updates).forEach(id => {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = updates[id];
        }
    });

    // 计算并显示飞行时长
    const durationElement = document.getElementById('flightDuration');
    if (durationElement) {
        const duration = calculateFlightDuration(flightInfo.timeTakeoff, flightInfo.timeArrive);
        durationElement.textContent = duration;
    }

    // 更新座位信息
    updateSeatDisplay();
}

// 更新座位显示
function updateSeatDisplay() {
    if (!flightInfo) return;

    const economySeats = parseInt(flightInfo.seat0Left) || 0;
    const businessSeats = parseInt(flightInfo.seat1Left) || 0;

    console.log('💺 座位信息 - 经济舱:', economySeats, '商务舱:', businessSeats);

    // 更新座位可用性文本
    const economyAvailability = document.getElementById('economyAvailability');
    const businessAvailability = document.getElementById('businessAvailability');

    if (economyAvailability) {
        economyAvailability.textContent = economySeats > 0 ? `剩余 ${economySeats} 座位` : '座位已满';
        economyAvailability.style.color = economySeats > 0 ? '#28a745' : '#e74c3c';
    }

    if (businessAvailability) {
        businessAvailability.textContent = businessSeats > 0 ? `剩余 ${businessSeats} 座位` : '座位已满';
        businessAvailability.style.color = businessSeats > 0 ? '#28a745' : '#e74c3c';
    }

    // 更新座位选项状态
    const economyOption = document.querySelector('[data-seat-type="0"]');
    const businessOption = document.querySelector('[data-seat-type="1"]');

    if (economyOption) {
        economyOption.classList.remove('disabled');
        if (economySeats <= 0) {
            economyOption.classList.add('disabled');
            economyOption.onclick = null;
        } else {
            economyOption.onclick = () => selectSeat(0);
        }
    }

    if (businessOption) {
        businessOption.classList.remove('disabled');
        if (businessSeats <= 0) {
            businessOption.classList.add('disabled');
            businessOption.onclick = null;
        } else {
            businessOption.onclick = () => selectSeat(1);
        }
    }
}

// 更新价格显示
function updatePriceDisplay() {
    // 更新座位选项中的价格
    const economyPriceElement = document.getElementById('economyPrice');
    const businessPriceElement = document.getElementById('businessPrice');

    if (economyPriceElement) {
        economyPriceElement.textContent = '¥' + priceInfo.economy.price;
    }
    if (businessPriceElement) {
        businessPriceElement.textContent = '¥' + priceInfo.business.price;
    }

    // 更新当前选中座位的价格汇总
    updatePriceSummary();

    // 更新预订按钮状态
    updateBookButton();
}

// 更新价格汇总
function updatePriceSummary() {
    const currentPrice = selectedSeatType === 0 ? priceInfo.economy.price : priceInfo.business.price;
    const hasDiscount = selectedSeatType === 0 ? priceInfo.economy.hasDiscount : priceInfo.business.hasDiscount;
    const originalPrice = selectedSeatType === 0 ? priceInfo.economy.originalPrice : priceInfo.business.originalPrice;

    // 更新主要价格显示
    const ticketPriceElement = document.getElementById('ticketPrice');
    const totalPriceElement = document.getElementById('totalPrice');

    if (ticketPriceElement) ticketPriceElement.textContent = '¥' + currentPrice;
    if (totalPriceElement) totalPriceElement.textContent = '¥' + currentPrice;

    // 显示/隐藏折扣信息
    const originalPriceRow = document.getElementById('originalPriceRow');
    const discountRow = document.getElementById('discountRow');

    if (hasDiscount && originalPrice && originalPrice > currentPrice) {
        if (originalPriceRow) {
            originalPriceRow.style.display = 'flex';
            const originalPriceElement = document.getElementById('originalPrice');
            if (originalPriceElement) originalPriceElement.textContent = '¥' + originalPrice;
        }

        if (discountRow) {
            discountRow.style.display = 'flex';
            const discountAmountElement = document.getElementById('discountAmount');
            if (discountAmountElement) {
                discountAmountElement.textContent = '-¥' + (originalPrice - currentPrice);
                discountAmountElement.style.color = '#28a745';
            }
        }
    } else {
        if (originalPriceRow) originalPriceRow.style.display = 'none';
        if (discountRow) discountRow.style.display = 'none';
    }
}

// 更新预订按钮状态
function updateBookButton() {
    const bookBtn = document.getElementById('bookBtn');
    if (!bookBtn) return;

    if (!flightInfo) {
        bookBtn.disabled = true;
        bookBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 加载中...';
        return;
    }

    const economySeats = parseInt(flightInfo.seat0Left) || 0;
    const businessSeats = parseInt(flightInfo.seat1Left) || 0;
    const hasAvailableSeats = (selectedSeatType === 0 && economySeats > 0) ||
        (selectedSeatType === 1 && businessSeats > 0);

    console.log('🔘 更新预订按钮状态:', {
        selectedSeatType,
        economySeats,
        businessSeats,
        hasAvailableSeats
    });

    if (hasAvailableSeats && !isBookingInProgress) {
        bookBtn.disabled = false;
        bookBtn.innerHTML = '<i class="fa fa-check"></i> 确认预订';
    } else if (isBookingInProgress) {
        bookBtn.disabled = true;
        bookBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> 处理中...';
    } else {
        bookBtn.disabled = true;
        bookBtn.innerHTML = '<i class="fa fa-ban"></i> 座位已满';
    }
}

// 选择座位类型
function selectSeat(seatType) {
    console.log('🎯 尝试选择座位类型:', seatType);

    if (!flightInfo) {
        console.warn('⚠️ 航班信息未加载完成');
        showMessage('航班信息未加载完成，请稍后再试', 'error');
        return;
    }

    // 检查座位是否可用
    const economySeats = parseInt(flightInfo.seat0Left) || 0;
    const businessSeats = parseInt(flightInfo.seat1Left) || 0;
    const isAvailable = (seatType === 0 && economySeats > 0) ||
        (seatType === 1 && businessSeats > 0);

    console.log('💺 座位可用性检查:', {
        seatType,
        economySeats,
        businessSeats,
        isAvailable
    });

    if (!isAvailable) {
        const seatTypeName = seatType === 0 ? '经济舱' : '商务舱';
        showMessage(`${seatTypeName}已无可用座位`, 'error');
        return;
    }

    selectedSeatType = seatType;

    // 更新选中状态
    document.querySelectorAll('.seat-option').forEach(option => {
        option.classList.remove('selected');
    });

    const selectedOption = document.querySelector(`[data-seat-type="${seatType}"]`);
    if (selectedOption && !selectedOption.classList.contains('disabled')) {
        selectedOption.classList.add('selected');
    }

    // 更新价格显示
    updatePriceSummary();

    const seatTypeName = seatType === 0 ? '经济舱' : '商务舱';
    console.log('✅ 选择座位类型:', seatTypeName);
}

// 🔧 确认预订 - 添加防重复提交机制
async function confirmBooking() {
    console.log('🎯 开始确认预订流程...');

    // 🔧 防重复提交检查
    if (isBookingInProgress) {
        console.log('⚠️ 预订正在进行中，忽略重复请求');
        return;
    }

    console.log('🔍 selectedSeatType 类型和值:', typeof selectedSeatType, selectedSeatType);
    console.log('🔍 parseInt(selectedSeatType):', parseInt(selectedSeatType));

    if (!currentUser || !flightInfo) {
        showMessage('用户信息或航班信息丢失，请重新登录', 'error');
        return;
    }

    // 检查座位可用性
    const economySeats = parseInt(flightInfo.seat0Left) || 0;
    const businessSeats = parseInt(flightInfo.seat1Left) || 0;
    const hasAvailableSeats = (selectedSeatType === 0 && economySeats > 0) ||
        (selectedSeatType === 1 && businessSeats > 0);

    console.log('🔍 预订前座位检查:', {
        selectedSeatType,
        economySeats,
        businessSeats,
        hasAvailableSeats
    });

    if (!hasAvailableSeats) {
        const seatTypeName = selectedSeatType === 0 ? '经济舱' : '商务舱';
        showMessage(`选择的${seatTypeName}已无可用座位`, 'error');
        return;
    }

    // 显示确认对话框
    const seatTypeName = selectedSeatType === 0 ? '经济舱' : '商务舱';
    const currentPrice = selectedSeatType === 0 ? priceInfo.economy.price : priceInfo.business.price;

    const confirmMsg = `确认预订信息：\n\n` +
        `航班：${flightInfo.flightId}\n` +
        `日期：${flightInfo.flightDate}\n` +
        `航线：${flightInfo.airportFrom} → ${flightInfo.airportTo}\n` +
        `起飞时间：${flightInfo.timeTakeoff}\n` +
        `到达时间：${flightInfo.timeArrive}\n` +
        `座位类型：${seatTypeName}\n` +
        `总费用：¥${currentPrice}\n\n` +
        `确认提交预订吗？`;

    if (!confirm(confirmMsg)) {
        return;
    }

    // 🔧 设置预订进行中标志
    isBookingInProgress = true;

    // 显示加载状态
    showLoading(true);

    try {
        console.log('📤 提交预订请求...');

        // 使用安全的API请求
        const result = await ApiClient.request('/api/booking/create', {
            type: 'POST',
            data: JSON.stringify({
                flightrecordId: flightrecordId,
                userId: currentUser.userId,
                seatType: Number(selectedSeatType)
            })
        });

        if (result.success) {
            showMessage(`预订成功！\n订单号：${result.orderId}\n总费用：¥${result.totalPrice}\n订单状态：${result.orderState}`, 'success');

            console.log('✅ 预订成功:', result);

            // 延迟跳转
            setTimeout(() => {
                window.location.href = 'order.html';
            }, 3000);

        } else {
            console.error('❌ 预订失败:', result.message);
            showMessage('预订失败：' + result.message, 'error');
        }

    } catch (error) {
        console.error('❌ 预订请求异常:', error);
        showMessage('预订失败：' + error.message, 'error');
    } finally {
        // 🔧 重置预订进行中标志
        isBookingInProgress = false;
        showLoading(false);
    }
}

// 取消预订，返回搜索页面
function cancelBooking() {
    if (confirm('确定要取消预订吗？')) {
        window.location.href = 'search.html';
    }
}

// 显示加载状态
function showLoading(show) {
    const loadingDiv = document.getElementById('loadingDiv');
    const bookBtn = document.getElementById('bookBtn');

    if (show) {
        if (loadingDiv) loadingDiv.style.display = 'block';
        if (bookBtn) bookBtn.disabled = true;
    } else {
        if (loadingDiv) loadingDiv.style.display = 'none';
        updateBookButton();
    }
}

// 显示消息
function showMessage(message, type = 'info') {
    const container = document.getElementById('messageContainer');
    if (!container) return;

    const messageDiv = document.createElement('div');

    let className = 'info-message';
    let icon = 'info-circle';

    if (type === 'error') {
        className = 'error-message';
        icon = 'exclamation-triangle';
    } else if (type === 'success') {
        className = 'success-message';
        icon = 'check-circle';
    }

    messageDiv.className = className;
    messageDiv.innerHTML = `<i class="fa fa-${icon}"></i> ${message.replace(/\n/g, '<br>')}`;

    container.innerHTML = '';
    container.appendChild(messageDiv);

    // 自动消失（除了错误消息）
    if (type !== 'error') {
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.remove();
            }
        }, 5000);
    }

    // 滚动到消息位置
    try {
        messageDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } catch (e) {
        // 兼容性处理
        messageDiv.scrollIntoView();
    }
}

// 清除消息
function clearMessages() {
    const container = document.getElementById('messageContainer');
    if (container) {
        container.innerHTML = '';
    }
}

// 获取机场名称
function getAirportName(code) {
    const airportNames = {
        'PEK': '北京首都国际机场',
        'SHA': '上海虹桥国际机场',
        'PVG': '上海浦东国际机场',
        'CAN': '广州白云国际机场',
        'SZX': '深圳宝安国际机场',
        'CTU': '成都双流国际机场',
        'KMG': '昆明长水国际机场',
        'XIY': '西安咸阳国际机场',
        'HGH': '杭州萧山国际机场',
        'NKG': '南京禄口国际机场',
        'TSN': '天津滨海国际机场',
        'WUH': '武汉天河国际机场'
    };
    return airportNames[code] || code + '机场';
}

// 计算飞行时长
function calculateFlightDuration(takeoffTime, arriveTime) {
    try {
        if (!takeoffTime || !arriveTime) return '未知';

        const [takeoffHour, takeoffMin] = takeoffTime.split(':').map(Number);
        const [arriveHour, arriveMin] = arriveTime.split(':').map(Number);

        let takeoffMinutes = takeoffHour * 60 + takeoffMin;
        let arriveMinutes = arriveHour * 60 + arriveMin;

        // 处理跨日情况
        if (arriveMinutes <= takeoffMinutes) {
            arriveMinutes += 24 * 60; // 加一天
        }

        const durationMinutes = arriveMinutes - takeoffMinutes;
        const hours = Math.floor(durationMinutes / 60);
        const minutes = durationMinutes % 60;

        return `${hours}小时${minutes}分钟`;
    } catch (error) {
        console.error('计算飞行时长失败:', error);
        return '计算中...';
    }
}

// 工具函数：检查用户是否有特定权限
function hasPermission(requiredAuthority) {
    return currentUser && currentUser.userAuthority >= requiredAuthority;
}

// 工具函数：检查用户是否为VIP
function isVipUser() {
    return currentUser && currentUser.vipState === '是';
}

// 安全的API请求封装（带登录检查）
async function makeAuthenticatedRequest(url, options = {}) {
    try {
        // 确保用户已登录
        const isLoggedIn = await checkLoginStatus();
        if (!isLoggedIn) {
            return null;
        }

        // 发送请求
        return await ApiClient.request(url, options);

    } catch (error) {
        console.error('❌ 认证请求失败:', error);

        // 如果是认证错误，重新检查登录状态
        if (error.message.includes('登录') || error.message.includes('401')) {
            await checkLoginStatus();
        }

        throw error;
    }
}