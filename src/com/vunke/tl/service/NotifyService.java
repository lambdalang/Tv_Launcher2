package com.vunke.tl.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.callback.StringCallback;
import com.vunke.tl.auth.Auth;
import com.vunke.tl.auth.AuthInfo;
import com.vunke.tl.auth.GroupInfo;
import com.vunke.tl.base.Config;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.bean.NotifyBean;
import com.vunke.tl.service.GroupStrategy.GroupStrategyBean;
import com.vunke.tl.util.Constants;
import com.vunke.tl.util.LogUtil;
import com.vunke.tl.util.MACUtil;
import com.vunke.tl.util.RxBus;

import org.json.JSONObject;

import java.util.Date;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class NotifyService extends Service {
	public static final String actionName = "com.kxy.tl.notify";
	private AuthInfo authInfo;
	private GroupStrategyBean bean;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getAction().equals(actionName)) {
			Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_NOT_AUTH);
			registerBoradcastReceiver();
			checkLogin();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 检测配置信息 先注册获取用户信息的广播 ，再发送广播通知机顶盒返回数据 如果不执行此方法，无法获取用户信息
	 */
	private void checkLogin() {
		LogUtil.i("tv_launcher",
				"send BroadCast to request user info,start time:" + new Date());
		UIUtil.sendBroadCast(this, Constants.REQUEST_USER_INFO_ACTION,
				new Intent());
	}

	/**
	 * 注册获取用户信息的广播
	 */
	public void registerBoradcastReceiver() {
		LogUtil.i("tv_launcher", "registerBoradcast:request user info");
		IntentFilter myIntentFilter = new IntentFilter();
		myIntentFilter.addAction(Constants.LOAD_USER_INFO_ACTION);
		myIntentFilter.addAction(Constants.REGISTER_STATUS_ACTION);
		myIntentFilter.addAction(Constants.REGISTER_RESULT_ACTION);
		myIntentFilter.addAction(Constants.REGISTER_REBOOT_ACTION);
		// 注册广播
		registerReceiver(mBroadcastReceiver, myIntentFilter);
	}

	/**
	 * 获取用户信息的广播
	 */
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(Constants.LOAD_USER_INFO_ACTION)) {
				String userName = intent.getStringExtra("userName");
				String userID = intent.getStringExtra("userID");
				String password = intent.getStringExtra("password");
				LogUtil.i("tv_launcher", "userName:" + userName);
				LogUtil.i("tv_launcher", "userID:" + userID);
				boolean initNotfy = TextUtils.isEmpty(userID);
				if (initNotfy) {
					LogUtil.e("tv_launcher", "get UserId is null");
				} else {
					authInfo = new AuthInfo();
					authInfo.UserId = userID;
					authInfo.Password = password;
					LogUtil.i("tv_launcher", "get UserId:" + authInfo.UserId);
					handler.sendEmptyMessageDelayed(0x1211, 2000);
				}
			}
		}
	};
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0x1211:
				// 判断网络链接
				if (UIUtil.isNetConnected(getApplicationContext()) == true) {
					GetUserToken();
					initData();
				} else {
					LogUtil.i("tv_launcher", "network not connect");
					handler.sendEmptyMessageDelayed(0x1211, 2000);
				}
				break;
			default:
				break;
			}
		};
	};

	/**
	 * 请求获取推送消息
	 */
	private void initData() {
		try {
			// SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			// Date date = new Date(System.currentTimeMillis());
			// String format = dateFormat.format(date);
			// String version_code = UIUtil
			// .getVersionCode(getApplicationContext())
			// + Build.MODEL
			// + Build.VERSION.RELEASE;

			LogUtil.i("tv_launcher", "get notification ");
			String getVersion_vode = "420161208";
			OkHttpUtils.post(Config.Notify_Url + Config.Push).tag(this)
					.params("name", authInfo.UserId)
					.params("version_code", getVersion_vode)
					.params("business_type", "2").execute(new StringCallback() {

						@Override
						public void onResponse(boolean isFromCache, String t,
								Request request, @Nullable Response response) {
							// LogUtil.i(TAG, "get data:" + t);
							try {
								String data = "{\"data\":";
								String data2 = "}";
								StringBuffer buffer = new StringBuffer();
								buffer.append(data);
								buffer.append(t);
								buffer.append(data2);
								Gson gson = new Gson();
								LogUtil.i("tv_launcher", buffer.toString());
								NotifyBean bean = gson.fromJson(
										buffer.toString(), NotifyBean.class);
								bean.setUserId(authInfo.UserId);
								bean.setRxBuscode(20161207);
								LogUtil.i("tv_launcher", "send notifybean to TupianActivity,user_id = "+ bean.getUserId());
								RxBus.getInstance().post(bean);

							} catch (Exception e) {
								LogUtil.e("tv_launcher",
										"get path error to gson.fromJson");
								e.printStackTrace();
							}
						}

						@Override
						public void onError(boolean isFromCache, Call call,
								@Nullable Response response,
								@Nullable Exception e) {
							super.onError(isFromCache, call, response, e);
							LogUtil.e("tv_launcher", "get path error");
							NotifyBean bean = new NotifyBean();
							bean.setUserId(authInfo.UserId);
							bean.setRxBuscode(20161207);
							RxBus.getInstance().post(bean);
						}
					});
		} catch (Exception e) {
			LogUtil.e("tv_launcher", "initData():get path onError");
			e.printStackTrace();
		}
	}

	/**
	 * 获取UserToken
	 */
	private void GetUserToken() {
		Auth.GetUserToken(authInfo, getApplicationContext(),
				new StringCallback() {

					@Override
					public void onResponse(boolean isFromCache, String t,
							Request request, @Nullable Response response) {
						try {
							LogUtil.i("tv_launcher", "getData:" + t);
							JSONObject jsonData = new JSONObject(t);
							int code = jsonData.getInt("code");
							switch (code) {
							case 200:
								LogUtil.i("tv_launcher",
										"get userToken success");
								String encryToken = jsonData
										.getString("encryToken");
								LogUtil.i("tv_launcher", "EncryToken="
										+ encryToken);

								authInfo.EncryToken = encryToken;

								authInfo.MacAddr = MACUtil
										.getLocalMacAddressFromBusybox();
								authInfo.IpAddr = MACUtil.getHostIP();
								authInfo.StbId = Build.SERIAL;
								authInfo.AccessMethod = Auth.getAccessMethod(getApplicationContext());
								Auth.queryDeviceInfo(getApplicationContext(),
										authInfo);
								GetAuthInfo();
								break;
							case 400:
							case 500:
							default:
								LogUtil.e("tv_launcher",
										"get userToken error,code:" + code);
								Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
								if(jsonData.has("ErrorCode")){
									Auth.setAuthErrCode(getApplication(),jsonData.getString("ErrorCode"));
								}
								break;
							}
						} catch (Exception e) {
							LogUtil.e("tv_launcher", "get userToken error");
							Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
							e.printStackTrace();
						}
					}

					@Override
					public void onError(boolean isFromCache, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onError(isFromCache, call, response, e);
						Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
						LogUtil.e("tv_launcher", "get userToken request error");
					}
				});
	}

	/**
	 * 获取用户信息
	 */
	private void GetAuthInfo() {
		Auth.GetAuthInfo(authInfo, getApplicationContext(),
				new StringCallback() {

					@Override
					public void onResponse(boolean isFromCache, String t,
							Request request, @Nullable Response response) {
						LogUtil.i("tv_launcher", "get data:" + t);
						try {
							JSONObject json = new JSONObject(t);
							int code = json.getInt("code");
							switch (code) {
							case 200:
								LogUtil.i("tv_launcher", "get userinfo success");
								Gson gson = new Gson();
								GroupInfo groupInfo = gson.fromJson(t,
										GroupInfo.class);
								if (!TextUtils.isEmpty(groupInfo
										.getUserGroupNmb())) {
									Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_SUCCESS);
									Auth.INSERT(getApplicationContext(), t,
											authInfo);
								}
								// 根据不同的userGroupNum 启动不同的EPG
								GetGroupInfo(groupInfo);
								break;
							case 400:
							case 500:
							default:
								LogUtil.e("tv_launcher",
										"get authInfo error,code:" + code);
								Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
								if(json.has("ErrorCode")){
									Auth.setAuthErrCode(getApplication(),json.getString("ErrorCode"));
								}
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
							LogUtil.e("tv_launcher",
									"get authInfo error gson.fromJson");
							Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
						}
					}

					@Override
					public void onError(boolean isFromCache, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onError(isFromCache, call, response, e);
						LogUtil.e("tv_launcher", "get authInfo request error");
						Auth.setAuthCode(getApplicationContext(),Auth.AUTH_CODE_AUTH_ERROR);
					}
				});

	}

	private void GetGroupInfo(final GroupInfo groupInfo) {
		LogUtil.i("tv_launcher", "get group info");
		OkHttpUtils.post(Config.BASE_WS_URL3 + Config.GROUP_STRATEGY).tag(this)
				.execute(new StringCallback() {

					@Override
					public void onResponse(boolean isFromCache, String t,
							Request request, @Nullable Response response) {
						 LogUtil.i("tv_launcher", "get groupfile success" +
						 t);
						try {
							// saveFile(t);
							if (!TextUtils.isEmpty(t)) {
								Gson gson = new Gson();
								GroupStrategy strategy = gson.fromJson(t,
										GroupStrategy.class);
								if (!strategy.getJson().isEmpty()
										&& strategy.getJson() != null) {
									for (int i = 0; i < strategy.getJson()
											.size(); i++) {
										GroupStrategyBean js = strategy
												.getJson().get(i);
										String groupNumber = js
												.getGrpupNumber();
										if (groupNumber.equals(groupInfo
												.getUserGroupNmb())) {
											LogUtil.i("tv_launcher",
													"get group strategy success :"
															+ groupNumber);
											bean = new GroupStrategyBean();
											bean.setGrpupNumber(groupNumber);
											bean.setEPGcode(js.getEPGcode());
											bean.setEPGpackage(js
													.getEPGpackage());
											bean.setGroupAddress(js
													.getGroupAddress());
											bean.setGroupName(js.getGroupName());
											bean.setGroupStatus(js
													.getGroupStatus());
											bean.setGroupType(js.getGroupType());
											bean.setUserId(authInfo.UserId);
											LogUtil.e("tv_launcher",
													bean.toString());
											Auth.InsertGroupStrategy(
													getApplicationContext(),
													bean);
										}else {
											LogUtil.i("tv_launcher","get group strategy failed,get groupNumber != groupinfo.getUserGroupUmb");
										}
									}
								}else{
									LogUtil.e("tv_launcher","get group strategy error,get jsonarray isEmpty ");
								}

							}else{
								LogUtil.e("tv_launcher","get group strategy error,response data is null ");
							}
						} catch (Exception e) {
							e.printStackTrace();
							LogUtil.e("tv_launcher",
									"get group strategy failed : gson.fronJson");
						}
					}

					@Override
					public void onError(boolean isFromCache, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onError(isFromCache, call, response, e);
						LogUtil.e("tv_launcher",
								"get group strategy failed request error");
					}
				});
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		LogUtil.i("tv_launcher", "注销广播:request user info");
		try {
			if (mBroadcastReceiver != null) {
				unregisterReceiver(mBroadcastReceiver);
			}
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			LogUtil.e("tv_launcher", "can't unregister receiver");
		}
	}
}
