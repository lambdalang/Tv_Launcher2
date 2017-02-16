package com.vunke.tl.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 *  获取机顶盒用户信息
 * Created by zhuxi on 2016/12/24.
 */
public class UserInfoUtil {
    /**
     * 向机顶盒发送读取机顶盒相关配置信息广播
     */
    public final static String REQUEST_USER_INFO_ACTION = "android.bellmann.REQUEST_USER_INFO";
    /**
     * 监听读取机顶盒相关配置信息广播
     */
    public final static String LOAD_USER_INFO_ACTION = "android.bellmann.LOAD_USER_INFO";
    /**
     * 监听下发平台业务工单是否对匹配成功广播
     */
    public final static String REGISTER_STATUS_ACTION = "android.bellmann.REGISTER_STATUS";

    /**
     * 监听下发结果信息广播
     */
    public final static String REGISTER_RESULT_ACTION = "android.bellmann.REGISTER_RESULT";

    /**
     * 监听机顶盒回应SetParameterValuesResponse
     */
    public final static String REGISTER_REBOOT_ACTION = "android.bellmann.REGISTER_REBOOT";

    /**
     *  检测配置信息
     *  先注册获取用户信息的广播 ，再发送广播通知机顶盒返回数据
     *  如果不执行此方法，无法获取用户信息
     */
    public  static  void initUserInfo(Context context) {
//        LogUtil.i("UserInfoUtil",
//                "send BroadCast to request user info,start time:" + new Date());
        sendBroadCast(context,REQUEST_USER_INFO_ACTION,
                new Intent());
    }
    public static void sendBroadCast(Context mcontext,String Action,Intent intent){
        intent.setAction(Action);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        mcontext.sendBroadcast(intent);
    }

    /**
     * 注册获取用户信息的广播
     */
    public void registerBoradcastReceiver(Context context,BroadcastReceiver mBroadcastReceiver) {
//        LogUtil.e("UserInfoUtil", "registerBoradcast:request user info");
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(LOAD_USER_INFO_ACTION);
        myIntentFilter.addAction(REGISTER_STATUS_ACTION);
        myIntentFilter.addAction(REGISTER_RESULT_ACTION);
        myIntentFilter.addAction(REGISTER_REBOOT_ACTION);
        // 注册广播
        context.registerReceiver(mBroadcastReceiver, myIntentFilter);
    }


    /**
     * 获取用户信息的广播
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(LOAD_USER_INFO_ACTION)) {
                String userName = intent.getStringExtra("userName");// 用户名
                String userID = intent.getStringExtra("userID");//用户ID
                String password = intent.getStringExtra("password");//用户密码

            }
        }
    };
}
