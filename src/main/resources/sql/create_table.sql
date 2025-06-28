USE airdatabase;
CREATE TABLE `user`
(
    user_ID CHAR(6)  CHECK (user_ID REGEXP '^[0-9]{6}$') PRIMARY KEY NOT NULL,
    user_password VARCHAR(50) NOT NULL,
    user_name VARCHAR(50) NOT NULL,
    user_gender VARCHAR(10) CHECK (user_gender IN ('男', '女')),
    user_telephone CHAR(8) CHECK (user_telephone REGEXP '^[0-9]{8}$'),
    user_SignUpTime DATETIME NOT NULL CHECK (
        user_SignUpTime BETWEEN '2000-10-28 00:00:00' AND '9999-12-31 23:59:59'
        ),
    VIPstate VARCHAR(10) NOT NULL CHECK (VIPstate IN ('是', '否'))  DEFAULT '否',
    user_authority SMALLINT NOT NULL DEFAULT 1
);


CREATE TABLE `airlinecompany`
(
    airlinecompany_ID CHAR(5) PRIMARY KEY NOT NULL,
    airlinecompany_name VARCHAR(50) NOT NULL,
    airlinecompany_telephone CHAR(8) CHECK(airlinecompany_telephone REGEXP '^[0-9]{8}$')
    );

CREATE TABLE `flight`
(
    flight_ID CHAR(8) PRIMARY KEY NOT NULL,
    airlinecompany_ID CHAR(5) NOT NULL,
    airport_from VARCHAR(50) NOT NULL,
    airport_to VARCHAR(50) NOT NULL,
    time_takeoff TIME CHECK (time_takeoff BETWEEN '00:00:00' AND '23:59:59') NOT NULL,
    time_arrive TIME CHECK (time_arrive BETWEEN '00:00:00' AND '23:59:59') NOT NULL,
    seat0_capacity SMALLINT NOT NULL,
    seat1_capacity SMALLINT NOT NULL,
    seat0_price SMALLINT NOT NULL,
    seat1_price SMALLINT NOT NULL,
    discount FLOAT NOT NULL,
    FOREIGN KEY (airlinecompany_ID) REFERENCES airlinecompany(airlinecompany_ID)
);


CREATE TABLE `order`
(
    order_ID CHAR(8) PRIMARY KEY NOT NULL,
    user_ID CHAR(6) NOT NULL,
    flight_ID CHAR(8) NOT NULL,
    order_state VARCHAR(20) CHECK(order_state IN('正常','未支付','已完成','已取消')) NOT NULL,
    flight_time DATE CHECK(flight_time BETWEEN '2000-10-28' AND '9999-12-31') NOT NULL,
    order_time DATETIME CHECK(order_time BETWEEN '2000-10-28 00:00:00' AND '9999-12-31 23:59:59') NOT NULL,
    seat_id SMALLINT NOT NULL,
    seat_type SMALLINT CHECK(seat_type IN(0,1)) NOT NULL,
    FOREIGN KEY (user_ID) REFERENCES `user`(user_ID),
    FOREIGN KEY (flight_ID) REFERENCES flight(flight_ID)
);

CREATE TABLE payrecord
(
    pay_ID CHAR(10) PRIMARY KEY NOT NULL,
    order_ID CHAR(8) NOT NULL,
    payment SMALLINT CHECK(payment>=1 AND payment<=9999) NOT NULL,
    pay_method VARCHAR(20) NOT NULL,
    pay_state VARCHAR(20) CHECK(pay_state IN('已支付','已退款')) NOT NULL,
    pay_time DATETIME CHECK(pay_time BETWEEN '2000-10-28 00:00:00' AND '9999-12-31 23:59:59') NOT NULL,
    FOREIGN KEY (order_ID) REFERENCES `order`(order_ID)
);

CREATE TABLE airport
(
    airport_ID CHAR(3) PRIMARY KEY NOT NULL,
    airport_name VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL
);

CREATE TABLE luggage
(
    luggage_ID CHAR(7) PRIMARY KEY NOT NULL,
    order_ID CHAR(8) NOT NULL,
    luggage_weight SMALLINT CHECK(luggage_weight>=0 AND luggage_weight<=999) NOT NULL,
    luggage_num SMALLINT CHECK(luggage_num>=0 AND luggage_num<=99) NOT NULL,
    luggage_price SMALLINT CHECK(luggage_price>=0 AND luggage_price<=999) NOT NULL,
    FOREIGN KEY (order_ID) REFERENCES `order`(order_ID)
);

CREATE TABLE flightrecord
(
    flightrecord_ID CHAR(14) PRIMARY KEY NOT NULL,
    flight_ID CHAR(8) NOT NULL,
    flight_date DATE CHECK(flight_date BETWEEN '2000-10-28' AND '9999-12-31') NOT NULL,
    seat0_left SMALLINT NOT NULL,
    seat1_left SMALLINT NOT NULL,
    FOREIGN KEY (flight_ID) REFERENCES flight(flight_ID)
);