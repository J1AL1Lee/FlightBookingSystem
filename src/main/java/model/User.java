package model;

import java.time.LocalDateTime;

public class User {
    private String userId;          // user_ID - 身份证号（用户输入的唯一标识）
    private String userPassword;    // user_password VARCHAR(50)
    private String userName;        // user_name VARCHAR(50) - 真实姓名（可重复）
    private String userGender;      // user_gender VARCHAR(10) - '男'/'女'
    private String userTelephone;   // user_telephone CHAR(8) - 8位数字电话
    private LocalDateTime userSignUpTime; // user_SignUpTime DATETIME - 注册时间
    private String vipState;        // VIPstate VARCHAR(10) - '是'/'否'
    private Integer userAuthority;  // user_authority SMALLINT - 默认1

    // 构造函数
    public User() {
        this.vipState = "否";
        this.userAuthority = 1;
        this.userSignUpTime = LocalDateTime.now(); // 注册时间设为当前时间
    }

    public User(String userId, String userPassword, String userName) {
        this();
        this.userId = userId;        // 身份证号
        this.userPassword = userPassword;
        this.userName = userName;    // 真实姓名
    }

    // Getter和Setter
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserPassword() { return userPassword; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserGender() { return userGender; }
    public void setUserGender(String userGender) { this.userGender = userGender; }

    public String getUserTelephone() { return userTelephone; }
    public void setUserTelephone(String userTelephone) { this.userTelephone = userTelephone; }

    public LocalDateTime getUserSignUpTime() { return userSignUpTime; }
    public void setUserSignUpTime(LocalDateTime userSignUpTime) { this.userSignUpTime = userSignUpTime; }

    public String getVipState() { return vipState; }
    public void setVipState(String vipState) { this.vipState = vipState; }

    public Integer getUserAuthority() { return userAuthority; }
    public void setUserAuthority(Integer userAuthority) { this.userAuthority = userAuthority; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +      // 身份证号
                ", userName='" + userName + '\'' + // 真实姓名
                ", userGender='" + userGender + '\'' +
                ", vipState='" + vipState + '\'' +
                ", signUpTime=" + userSignUpTime +
                '}';
    }
}