// 全局变量
let selectedSeatType = 0; // 默认选择经济舱
let flightrecordId = null;
let flightInfo = null;
let currentUser = null;
let priceInfo = {
    economy: { price: 0, originalPrice: 0, hasDiscount: false },
    business: { price: 0, originalPrice: 0, hasDiscount: false }
};

// 页面加载时执行
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 页面加载完成，开始初始化...');
    initializePage();
});

// 初始化页面
async function initializePage() {
    try {
        showMessage('正在加载航班和用户信息...', 'info');

        // 1. 检查用户登录状态
        if (!checkLoginStatus()) {
            return;
        }

        // 2. 获取URL参数
        if (!getUrlParameters()) {
            return;
        }

        // 3. 优先加载航班信息（因为用户信息相对稳定）
        try {
            await loadFlightInfo();
            console.log('✅ 航班信息加载成功');
        } catch (flightError) {
            console.warn('⚠️ 航班信息加载失败，但继续加载其他信息:', flightError.message);
            // 不要阻止后续加载
        }

        // 4. 加载用户信息
        try {
            await loadUserInfo();
            console.log('✅ 用户信息加载成功');
        } catch (userError) {
            console.warn('⚠️ 用户详细信息加载失败，使用缓存信息:', userError.message);
            // 使用缓存的用户信息
            displayUserInfo();
        }

        // 5. 加载价格信息
        try {
            await loadPriceInfo();
            console.log('✅ 价格信息加载成功');
        } catch (priceError) {
            console.warn('⚠️ 价格信息加载失败，使用默认价格:', priceError.message);
            // 使用默认价格
            priceInfo.economy.price = 500;
            priceInfo.business.price = 1200;
            updatePriceDisplay();
        }

        // 6. 默认选择经济舱
        selectSeat(0);

        // 清除加载消息
        clearMessages();
        showMessage('页面加载完成！您可以选择座位类型并确认预订。', 'success');

        console.log('✅ 页面初始化完成');

    } catch (error) {
        console.error('❌ 页面初始化失败:', error);
        showMessage('页面初始化失败：' + error.message + '\n\n已加载默认信息，您仍可以尝试预订。', 'error');

        // 即使初始化失败，也要确保页面基本可用
        if (!flightInfo) {
            createDefaultFlightInfo();
            updateFlightDisplay();
        }
        if (!currentUser.userName) {
            displayUserInfo();
        }
        updatePriceDisplay();
        selectSeat(0);
    }
}

// 检查用户登录状态
function checkLoginStatus() {
    const userInfo = localStorage.getItem('userInfo');

    if (!userInfo) {
        localStorage.setItem('redirectUrl', window.location.href);
        showMessage('请先登录后再进行预订', 'error');
        setTimeout(() => {
            window.location.href = 'sign_log.html';
        }, 2000);
        return false;
    }

    try {
        currentUser = JSON.parse(userInfo);
        console.log('✅ 当前用户:', currentUser);
        return true;
    } catch (error) {
        console.error('❌ 解析用户信息失败:', error);
        localStorage.removeItem('userInfo');
        window.location.href = 'sign_log.html';
        return false;
    }
}

// 获取URL参数
function getUrlParameters() {
    const urlParams = new URLSearchParams(window.location.search);
    flightrecordId = urlParams.get('flightrecordId');

    if (!flightrecordId) {
        showMessage('缺少航班信息，请返回重新选择', 'error');
        setTimeout(() => {
            window.location.href = 'search.html';
        }, 2000);
        return false;
    }

    console.log('📝 航班记录ID:', flightrecordId);
    return true;
}

// 加载用户信息
async function loadUserInfo() {
    try {
        console.log('👤 开始加载用户信息...');

        const response = await fetch('/api/users');
        if (!response.ok) {
            throw new Error('获取用户列表失败');
        }

        const result = await response.json();
        if (!result.success) {
            throw new Error(result.message || '获取用户信息失败');
        }

        // 查找当前用户
        const userDetail = result.data.find(user => user.userId === currentUser.userId);
        if (!userDetail) {
            throw new Error('未找到用户详细信息');
        }

        // 更新当前用户信息
        currentUser = { ...currentUser, ...userDetail };

        // 显示用户信息
        displayUserInfo();

        console.log('✅ 用户信息加载完成:', currentUser);

    } catch (error) {
        console.error('❌ 加载用户信息失败:', error);
        // 使用缓存的用户信息
        displayUserInfo();
        throw new Error('加载用户详细信息失败：' + error.message);
    }
}

// 加载航班信息
async function loadFlightInfo() {
    try {
        console.log('✈️ 开始加载航班信息...');

        // 从航班记录ID解析信息
        const flightId = flightrecordId.substring(0, flightrecordId.length - 8);
        const flightDate = flightrecordId.substring(flightrecordId.length - 8);
        const formattedDate = `${flightDate.substring(0,4)}-${flightDate.substring(4,6)}-${flightDate.substring(6,8)}`;

        console.log('📝 解析航班信息:', {
            flightrecordId,
            flightId,
            flightDate,
            formattedDate
        });

        // 方法1: 先尝试直接用URL参数构建航班信息
        const urlParams = new URLSearchParams(window.location.search);
        if (urlParams.has('airportFrom') && urlParams.has('airportTo')) {
            console.log('📝 从URL参数获取航班信息');
            flightInfo = {
                flightId: flightId,
                flightrecordId: flightrecordId,
                airlineName: urlParams.get('airlineName') || '中国南方航空',
                airportFrom: urlParams.get('airportFrom'),
                airportTo: urlParams.get('airportTo'),
                timeTakeoff: urlParams.get('timeTakeoff') || '08:30',
                timeArrive: urlParams.get('timeArrive') || '11:15',
                flightDate: formattedDate,
                seat0Left: parseInt(urlParams.get('seat0Left')) || 50,
                seat1Left: parseInt(urlParams.get('seat1Left')) || 10
            };

            updateFlightDisplay();
            console.log('✅ 从URL参数加载航班信息成功:', flightInfo);
            return;
        }

        // 方法2: 尝试从localStorage获取最近的搜索结果
        const recentSearch = localStorage.getItem('recentFlightSearch');
        if (recentSearch) {
            try {
                const searchData = JSON.parse(recentSearch);
                console.log('📝 从localStorage获取搜索参数:', searchData);

                // 使用最近的搜索参数重新搜索
                const searchResponse = await fetch('/api/flights/search', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        airportFrom: searchData.airportFrom,
                        airportTo: searchData.airportTo,
                        flightDate: searchData.flightDate || formattedDate,
                        userId: currentUser.userId
                    })
                });

                if (searchResponse.ok) {
                    const searchResult = await searchResponse.json();
                    console.log('🔍 搜索API响应:', searchResult);

                    if (searchResult.success && searchResult.data && searchResult.data.length > 0) {
                        // 查找匹配的航班
                        let matchedFlight = searchResult.data.find(flight =>
                            flight.flightrecordId === flightrecordId ||
                            flight.flightId === flightId
                        );

                        // 如果没找到完全匹配，使用第一个航班
                        if (!matchedFlight && searchResult.data.length > 0) {
                            console.warn('⚠️ 未找到完全匹配的航班，使用第一个航班');
                            matchedFlight = searchResult.data[0];
                            // 更新航班记录ID以匹配实际数据
                            matchedFlight.flightrecordId = flightrecordId;
                        }

                        if (matchedFlight) {
                            flightInfo = matchedFlight;
                            updateFlightDisplay();
                            console.log('✅ 从搜索API加载航班信息成功:', flightInfo);
                            return;
                        }
                    }
                }
            } catch (e) {
                console.warn('⚠️ 使用localStorage搜索失败:', e);
            }
        }

        // 方法3: 尝试智能解析航班ID获取机场信息
        console.log('📝 尝试智能解析航班ID');
        let airportFrom = 'PEK';
        let airportTo = 'SHA';

        // 常见的航班号格式解析
        if (flightId.length >= 4) {
            // 如果航班号以常见前缀开始，尝试解析
            const airlineCode = flightId.substring(0, 2);
            const flightNumber = flightId.substring(2);

            // 根据航空公司代码推测常见航线
            const commonRoutes = {
                'CZ': [['PEK', 'SHA'], ['PEK', 'CAN'], ['SHA', 'CAN']],
                'CA': [['PEK', 'SHA'], ['PEK', 'PVG'], ['PEK', 'CAN']],
                'MU': [['SHA', 'PEK'], ['PVG', 'PEK'], ['SHA', 'CAN']],
                'FM': [['SHA', 'PEK'], ['PVG', 'CTU'], ['SHA', 'SZX']]
            };

            if (commonRoutes[airlineCode]) {
                const routes = commonRoutes[airlineCode];
                const routeIndex = parseInt(flightNumber) % routes.length;
                [airportFrom, airportTo] = routes[routeIndex];
                console.log('📝 根据航空公司推测航线:', airlineCode, airportFrom, '->', airportTo);
            }
        }

        // 最后尝试用推测的机场信息搜索
        console.log('📝 最后尝试用推测信息搜索航班');
        try {
            const searchResponse = await fetch('/api/flights/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    airportFrom: airportFrom,
                    airportTo: airportTo,
                    flightDate: formattedDate,
                    userId: currentUser.userId
                })
            });

            if (searchResponse.ok) {
                const searchResult = await searchResponse.json();
                if (searchResult.success && searchResult.data && searchResult.data.length > 0) {
                    // 使用第一个找到的航班，并调整信息
                    flightInfo = searchResult.data[0];
                    flightInfo.flightId = flightId;
                    flightInfo.flightrecordId = flightrecordId;
                    flightInfo.flightDate = formattedDate;

                    updateFlightDisplay();
                    console.log('✅ 用推测信息搜索成功:', flightInfo);
                    return;
                }
            }
        } catch (e) {
            console.warn('⚠️ 推测搜索失败:', e);
        }

        // 方法4: 如果所有方法都失败，创建默认航班信息
        console.log('📝 所有搜索方法失败，创建默认航班信息');
        createDefaultFlightInfo();
        updateFlightDisplay();
        console.log('✅ 使用默认航班信息:', flightInfo);

    } catch (error) {
        console.error('❌ 加载航班信息失败:', error);
        // 创建默认信息作为最后的备选方案
        createDefaultFlightInfo();
        updateFlightDisplay();
        throw new Error('加载航班信息失败，已使用默认信息：' + error.message);
    }
}

// 创建默认航班信息
function createDefaultFlightInfo() {
    const flightId = flightrecordId.substring(0, flightrecordId.length - 8);
    const flightDate = flightrecordId.substring(flightrecordId.length - 8);
    const formattedDate = `${flightDate.substring(0,4)}-${flightDate.substring(4,6)}-${flightDate.substring(6,8)}`;

    // 根据航班号推测航空公司
    let airlineName = '中国南方航空';
    const airlineCode = flightId.substring(0, 2);
    const airlineNames = {
        'CZ': '中国南方航空',
        'CA': '中国国际航空',
        'MU': '中国东方航空',
        'FM': '上海航空',
        '3U': '四川航空',
        'HU': '海南航空',
        'ZH': '深圳航空',
        'SC': '山东航空'
    };

    if (airlineNames[airlineCode]) {
        airlineName = airlineNames[airlineCode];
    }

    flightInfo = {
        flightId: flightId,
        flightrecordId: flightrecordId,
        airlineName: airlineName,
        airportFrom: 'PEK',
        airportTo: 'SHA',
        timeTakeoff: '08:30',
        timeArrive: '11:15',
        flightDate: formattedDate,
        seat0Left: 50,  // 默认经济舱座位
        seat1Left: 10   // 默认商务舱座位
    };

    console.log('📝 创建默认航班信息:', flightInfo);
}

// 加载价格信息
async function loadPriceInfo() {
    try {
        console.log('💰 开始加载价格信息...');

        // 并行获取经济舱和商务舱价格
        const [economyResponse, businessResponse] = await Promise.all([
            fetch(`/api/booking/price?flightrecordId=${encodeURIComponent(flightrecordId)}&seatType=0&userId=${encodeURIComponent(currentUser.userId)}`),
            fetch(`/api/booking/price?flightrecordId=${encodeURIComponent(flightrecordId)}&seatType=1&userId=${encodeURIComponent(currentUser.userId)}`)
        ]);

        // 处理经济舱价格
        if (economyResponse.ok) {
            const economyData = await economyResponse.json();
            if (economyData.success) {
                priceInfo.economy.price = economyData.price;
                console.log('✅ 经济舱价格:', economyData.price);
            } else {
                console.warn('⚠️ 获取经济舱价格失败:', economyData.message);
                priceInfo.economy.price = 500; // 默认价格
            }
        } else {
            console.warn('⚠️ 经济舱价格API调用失败');
            priceInfo.economy.price = 500;
        }

        // 处理商务舱价格
        if (businessResponse.ok) {
            const businessData = await businessResponse.json();
            if (businessData.success) {
                priceInfo.business.price = businessData.price;
                console.log('✅ 商务舱价格:', businessData.price);
            } else {
                console.warn('⚠️ 获取商务舱价格失败:', businessData.message);
                priceInfo.business.price = 1200; // 默认价格
            }
        } else {
            console.warn('⚠️ 商务舱价格API调用失败');
            priceInfo.business.price = 1200;
        }

        // 检查VIP折扣
        if (flightInfo && flightInfo.hasDiscount && currentUser.vipState === '是') {
            priceInfo.economy.originalPrice = flightInfo.originalPrice0 || priceInfo.economy.price;
            priceInfo.business.originalPrice = flightInfo.originalPrice1 || priceInfo.business.price;
            priceInfo.economy.hasDiscount = true;
            priceInfo.business.hasDiscount = true;
            console.log('🎉 检测到VIP折扣');
        }

        // 更新价格显示
        updatePriceDisplay();

        console.log('✅ 价格信息加载完成:', priceInfo);

    } catch (error) {
        console.error('❌ 加载价格信息失败:', error);
        // 使用默认价格
        priceInfo.economy.price = 500;
        priceInfo.business.price = 1200;
        updatePriceDisplay();
        throw new Error('加载价格信息失败：' + error.message);
    }
}

// 显示用户信息
function displayUserInfo() {
    if (!currentUser) return;

    document.getElementById('passengerName').value = currentUser.userName || '';
    document.getElementById('passengerPhone').value = currentUser.userTelephone || '';
    document.getElementById('userId').value = currentUser.userId || '';
    document.getElementById('vipStatus').value = currentUser.vipState === '是' ? 'VIP会员' : '普通会员';

    // 显示VIP状态
    if (currentUser.vipState === '是') {
        const vipBadge = document.createElement('span');
        vipBadge.className = 'vip-badge';
        vipBadge.innerHTML = '<i class="fa fa-crown"></i> VIP会员';
        document.getElementById('vipBadgeContainer').appendChild(vipBadge);

        // 显示VIP折扣信息
        const discountInfo = document.getElementById('vipDiscountInfo');
        discountInfo.innerHTML = '<div class="discount-info"><i class="fa fa-star"></i> 恭喜！作为VIP会员，您将享受专属优惠价格</div>';
        discountInfo.style.display = 'block';
    }
}

// 更新航班信息显示
function updateFlightDisplay() {
    if (!flightInfo) return;

    console.log('🖼️ 更新航班显示:', flightInfo);

    document.getElementById('flightNumber').textContent = flightInfo.flightId;
    document.getElementById('airlineName').textContent = flightInfo.airlineName;
    document.getElementById('departureCode').textContent = flightInfo.airportFrom;
    document.getElementById('departureName').textContent = getAirportName(flightInfo.airportFrom);
    document.getElementById('arrivalCode').textContent = flightInfo.airportTo;
    document.getElementById('arrivalName').textContent = getAirportName(flightInfo.airportTo);
    document.getElementById('departureTime').textContent = flightInfo.timeTakeoff;
    document.getElementById('arrivalTime').textContent = flightInfo.timeArrive;
    document.getElementById('flightDate').textContent = flightInfo.flightDate;

    // 计算飞行时长
    const duration = calculateFlightDuration(flightInfo.timeTakeoff, flightInfo.timeArrive);
    document.getElementById('flightDuration').textContent = duration;

    // 更新座位可用性
    const economySeats = parseInt(flightInfo.seat0Left) || 0;
    const businessSeats = parseInt(flightInfo.seat1Left) || 0;

    console.log('💺 座位信息 - 经济舱:', economySeats, '商务舱:', businessSeats);

    document.getElementById('economyAvailability').textContent = `剩余 ${economySeats} 座位`;
    document.getElementById('businessAvailability').textContent = `剩余 ${businessSeats} 座位`;

    // 检查座位是否可选
    const economyOption = document.querySelector('[data-seat-type="0"]');
    const businessOption = document.querySelector('[data-seat-type="1"]');

    // 重置状态
    economyOption.classList.remove('disabled');
    businessOption.classList.remove('disabled');
    economyOption.onclick = () => selectSeat(0);
    businessOption.onclick = () => selectSeat(1);

    // 检查经济舱座位
    if (economySeats <= 0) {
        economyOption.classList.add('disabled');
        economyOption.onclick = null;
        document.getElementById('economyAvailability').textContent = '座位已满';
        document.getElementById('economyAvailability').style.color = '#e74c3c';
        console.log('❌ 经济舱座位已满');
    } else {
        document.getElementById('economyAvailability').style.color = '#28a745';
        console.log('✅ 经济舱座位可用:', economySeats);
    }

    // 检查商务舱座位
    if (businessSeats <= 0) {
        businessOption.classList.add('disabled');
        businessOption.onclick = null;
        document.getElementById('businessAvailability').textContent = '座位已满';
        document.getElementById('businessAvailability').style.color = '#e74c3c';
        console.log('❌ 商务舱座位已满');
    } else {
        document.getElementById('businessAvailability').style.color = '#28a745';
        console.log('✅ 商务舱座位可用:', businessSeats);
    }
}

// 更新价格显示
function updatePriceDisplay() {
    // 更新座位选项中的价格
    document.getElementById('economyPrice').textContent = '¥' + priceInfo.economy.price;
    document.getElementById('businessPrice').textContent = '¥' + priceInfo.business.price;

    // 更新当前选中座位的价格汇总
    const currentPrice = selectedSeatType === 0 ? priceInfo.economy.price : priceInfo.business.price;
    const hasDiscount = selectedSeatType === 0 ? priceInfo.economy.hasDiscount : priceInfo.business.hasDiscount;
    const originalPrice = selectedSeatType === 0 ? priceInfo.economy.originalPrice : priceInfo.business.originalPrice;

    document.getElementById('ticketPrice').textContent = '¥' + currentPrice;
    document.getElementById('totalPrice').textContent = '¥' + currentPrice;

    // 显示折扣信息
    if (hasDiscount && originalPrice && originalPrice > currentPrice) {
        document.getElementById('originalPriceRow').style.display = 'flex';
        document.getElementById('discountRow').style.display = 'flex';
        document.getElementById('originalPrice').textContent = '¥' + originalPrice;
        document.getElementById('discountAmount').textContent = '-¥' + (originalPrice - currentPrice);
        document.getElementById('discountAmount').style.color = '#28a745';
    } else {
        document.getElementById('originalPriceRow').style.display = 'none';
        document.getElementById('discountRow').style.display = 'none';
    }

    // 启用预订按钮
    updateBookButton();
}

// 更新预订按钮状态
function updateBookButton() {
    const bookBtn = document.getElementById('bookBtn');
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

    if (hasAvailableSeats) {
        bookBtn.disabled = false;
        bookBtn.innerHTML = '<i class="fa fa-check"></i> 确认预订';
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
    updatePriceDisplay();

    const seatTypeName = seatType === 0 ? '经济舱' : '商务舱';
    console.log('✅ 选择座位类型:', seatTypeName);
}

// 确认预订
async function confirmBooking() {
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

    // 显示加载状态
    showLoading(true);

    try {
        console.log('📤 提交预订请求...');

        const response = await fetch('/api/booking/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                flightrecordId: flightrecordId,
                userId: currentUser.userId,
                seatType: selectedSeatType
            })
        });

        const result = await response.json();

        if (result.success) {
            showMessage(`预订成功！\n订单号：${result.orderId}\n总费用：¥${result.totalPrice}\n订单状态：${result.orderState}`, 'success');

            console.log('✅ 预订成功:', result);

            // 延迟跳转
            setTimeout(() => {
                window.location.href = 'personal.html';
            }, 3000);

        } else {
            console.error('❌ 预订失败:', result.message);
            showMessage('预订失败：' + result.message, 'error');
        }

    } catch (error) {
        console.error('❌ 预订请求异常:', error);
        showMessage('预订失败，请检查网络连接后重试', 'error');
    } finally {
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
        loadingDiv.style.display = 'block';
        bookBtn.disabled = true;
    } else {
        loadingDiv.style.display = 'none';
        updateBookButton();
    }
}

// 显示消息
function showMessage(message, type = 'info') {
    const container = document.getElementById('messageContainer');
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
    messageDiv.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// 清除消息
function clearMessages() {
    document.getElementById('messageContainer').innerHTML = '';
}

// 检查是否为有效的机场代码
function isValidAirportCode(code) {
    const validCodes = ['PEK', 'SHA', 'PVG', 'CAN', 'SZX', 'CTU', 'KMG', 'XIY', 'HGH', 'NKG', 'TSN', 'WUH'];
    return validCodes.includes(code);
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
        return '计算中...';
    }
}