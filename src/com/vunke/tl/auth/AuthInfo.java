package com.vunke.tl.auth;

/**
 * Created by zhuxi on 2016/10/14.
 */
public class AuthInfo {
	/**
	 *  拨号方式
	 */
    public String AccessMethod;
    /**
	 *  服务器 地址
	 */
    public String AuthServer;
    /**
	 *  IP 地址
	 */
    public String IpAddr;
    /**
	 *  MAC 地址
	 */
    public String MacAddr;
    /**
	 *  密码
	 */
    public String Password;
    /**
	 *  机顶盒 STB ID
	 */
    public String StbId;
    /**
	 *  用户ID
	 */
    public String UserId;
    /**
	 *   密钥
	 */
    public String EncryToken;

    public String toString()
    {
        return "AuthInfo {AuthServer='" + this.AuthServer + '\'' + ", MacAddr='" + this.MacAddr + '\'' + ", IpAddr='" + this.IpAddr + '\'' + ", StbId='" + this.StbId + '\'' + ", UserId='" + this.UserId + '\'' + ", Password='" + this.Password + '\'' + ", AccessMethod='" + this.AccessMethod + '\'' + '}';
    }
}
