package com.kxy.tl.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kxy.tl.R;
import com.kxy.tl.download.DownLoadService;
import com.kxy.tl.versionUp.VersionUpApi;
import com.vunke.tl.base.BaseActivity;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.util.Constants;
import com.vunke.tl.util.FileManager;
import com.vunke.tl.util.LogUtil;
import com.vunke.tl.util.SharedPreferencesUtil;

import java.util.Date;

/**
 * 首页 用户信息认证
 * @author zhuxi
 *
 */
public class MainActivity extends BaseActivity implements OnClickListener {
	private EditText userNameEdt/* , pwdEdt */;
	private Button loginBtn;
	private String regStatus;
	private String regResult;
	private String regReboot;
	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000; // 需要自己定义标志
	private ProgressDialog MyDialog;
	private RelativeLayout root_ly;
	private TextView main_VersionCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED,
				FLAG_HOMEKEY_DISPATCHED);// 关键代码
		setContentView(R.layout.activity_main);
		root_ly = (RelativeLayout) findViewById(R.id.root_ly);
		userNameEdt = (EditText) findViewById(R.id.user_name);
		// pwdEdt = (EditText) findViewById(R.id.user_pwd);
		loginBtn = (Button) findViewById(R.id.login_btn);
		loginBtn.setOnClickListener(this);
		main_VersionCode = (TextView) findViewById(R.id.main_VersionCode);
		main_VersionCode.setText("版本号:"
				+ UIUtil.getVersionName(getApplicationContext()));
		LogUtil.i("tv_launcher",  "reset play advert");
		SharedPreferencesUtil.setBooleanValue(this,
				SharedPreferencesUtil.IS_PALYED_ADVERT, false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		LogUtil.i("tv_launcher", "注册广播:request user info");
		registerBoradcastReceiver();
		VersionUpApi.getUpdateVersion(this);
		checkLogin();
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if (mBroadcastReceiver != null) {
				LogUtil.i("tv_launcher", "注销广播:request user info");
				unregisterReceiver(mBroadcastReceiver);
			}
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			LogUtil.i("tv_launcher", "不需要注销");
		}
	}

	/**
	 *  检测配置信息
	 *  先注册获取用户信息的广播 ，再发送广播通知机顶盒返回数据
	 *  如果不执行此方法，无法获取用户信息
	 */
	private void checkLogin() {
		// if(MyDialog == null){
		// MyDialog = ProgressDialog.show(MainActivity.this,"提示" ,
		// "检测配置信息中.请稍等...", true);
		// }else if(!MyDialog.isShowing()){
		// MyDialog.show();
		// }
		root_ly.setVisibility(View.INVISIBLE);
		LogUtil.i("tv_launcher","send BroadCast to request user info,start time:" + new Date());
		UIUtil.sendBroadCast(this, Constants.REQUEST_USER_INFO_ACTION,
				new Intent());

	}

	/**
	 * 注册获取用户信息的广播
	 */
	public void registerBoradcastReceiver() {
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
				// if(MyDialog != null && MyDialog.isShowing()){
				// MyDialog.cancel();
				// MyDialog = null;
				// }
				LogUtil.i("tv_launcher","get action LOAD_USER_INFO_ACTION");
				String userName = intent.getStringExtra("userName");
				String userID = intent.getStringExtra("userID");
				String password = intent.getStringExtra("password");
				String status = intent.getStringExtra("status");
				if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(userID)
						&& !TextUtils.isEmpty(password) && status.equals("1")) {
					// // 如果用户信息完整就跳转到IPTV业务平台，进行节目播放
					// UIUtil.sendBroadCast(context,
					// Constants.ADVERTISING_ACTION);
					// 如果信息完整，则在广告展示结束后直接连接至IPTV业务平台，进行节目播放。
					playAdvert();
				} else {
					root_ly.setVisibility(View.VISIBLE);
				}
			} else if (action.equals(Constants.REGISTER_STATUS_ACTION)) {
				LogUtil.i("tv_launcher","get action REGISTER_STATUS_ACTION");
				String status = intent.getStringExtra("status");
				/*
				 * 0：成功；1：用 户认证码不存在、2：用户逻辑 ID 不存 在；3：用户逻辑 ID 与用户认证码匹配
				 * 失败；4：超时；5：已经注册过且无新 的工单要执行；99：缺省值，表示无认 证结果信息
				 */
				regStatus = status;
				if (!TextUtils.isEmpty(status)) {
					if (regStatus.equals("0")) {
						UIUtil.showWaitDialog(MainActivity.this);
					} else if (regStatus.equals("1")) {
						UIUtil.showTipDlg(MainActivity.this, "用户认证码不存在,"
								+ Constants.ERROR_HELP, null);
					} else if (regStatus.equals("2")) {
						UIUtil.showTipDlg(MainActivity.this, "用户逻辑 ID 不存在,"
								+ Constants.ERROR_HELP, null);
					} else if (regStatus.equals("3")) {
						UIUtil.showTipDlg(MainActivity.this,
								"用户逻辑 ID 与用户认证码匹配失败," + Constants.ERROR_HELP,
								null);
					} else if (regStatus.equals("4")) {
						UIUtil.showTipDlg(MainActivity.this, "超时,"
								+ Constants.ERROR_HELP, null);
					} else if (regStatus.equals("5")) {
						UIUtil.showTipDlg(MainActivity.this, "已经注册过且无新的工单要执行,"
								+ Constants.ERROR_HELP, null);
					} else if (regStatus.equals("99")) {
						UIUtil.showTipDlg(MainActivity.this, "无认证结果信息,"
								+ Constants.ERROR_HELP, null);
					}
				} else {
					UIUtil.showTipDlg(MainActivity.this, "无认证结果信息,"
							+ Constants.ERROR_HELP, null);
				}
			} else if (action.equals(Constants.REGISTER_RESULT_ACTION)) {
				LogUtil.i("tv_launcher","get action REGISTER_RESULT_ACTION");
				regResult = intent.getStringExtra("result");
				if (!TextUtils.isEmpty(regResult)) {
					if (regResult.equals("1") && !TextUtils.isEmpty(regReboot)
							&& regReboot.equals("0")) {
						UIUtil.dismissDlg();
						// 播放广告
						playAdvert();
					}
					if (regResult.equals("2")) {
						UIUtil.dismissDlg();
						UIUtil.showTipDlg(MainActivity.this, "业务下发失败,"
								+ Constants.ERROR_HELP, null);
					} else if (regResult.equals("99")) {
						UIUtil.dismissDlg();
						UIUtil.showTipDlg(MainActivity.this, "无业务下发,"
								+ Constants.ERROR_HELP, null);
					}
				}

			} else if (action.equals(Constants.REGISTER_REBOOT_ACTION)) {
				LogUtil.i("tv_launcher","get action REGISTER_REBOOT_ACTION");
				regReboot = intent.getStringExtra("reboot");
				if (!TextUtils.isEmpty(regReboot)) {
					if (regReboot.equals("0") && !TextUtils.isEmpty(regResult)
							&& regResult.equals("1")) {
						UIUtil.dismissDlg();
						// 播放广告
						playAdvert();
					}
				}
			}
		}

	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if (mBroadcastReceiver != null) {
				LogUtil.i("tv_launcher", "注销广播:request user info");
				unregisterReceiver(mBroadcastReceiver);
			}
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			LogUtil.i("tv_launcher", "不需要注销");
		}
		if (MyDialog != null) {
			MyDialog.cancel();
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.login_btn) {

			// VersionUpApi.getUpdateVersion(this);
			String userName = userNameEdt.getText().toString();
			// String pwd = pwdEdt.getText().toString();
			if (TextUtils.isEmpty(userName)) {
				UIUtil.showToast(this, R.string.empty_username_tip);
				return;
			}

			if (!UIUtil.isNetworkAvailable(MainActivity.this)) {
				UIUtil.showTipDlg(MainActivity.this, "网络未连接,请稍后再试。", null);
				return;
			}

			if (!UIUtil.isNetworkConnected(MainActivity.this)) {
				UIUtil.showTipDlg(MainActivity.this, "网络还未连接,请稍后再试。", null);
				return;
			}
			// if (TextUtils.isEmpty(pwd)) {
			// UIUtil.showToast(this, R.string.empty_pwd_tip);
			// return;
			// }
			// UIUtil.showWaitDialog(this);
			Intent it = new Intent();
			it.putExtra("userName", userName);
			// it.putExtra("password", pwd);
			it.putExtra("remark", "");

			UIUtil.sendBroadCast(this, Constants.REGISTER_USER_ACTION, it);
			regReboot = "";
			regStatus = "";
			regResult = "";
			// playAdvert();
		}
	}

	/**
	 * 播放广告和更新图片
	 */
	private void playAdvert() {
		Boolean isPlayAdvert = SharedPreferencesUtil.getBooleanValue(this,
				SharedPreferencesUtil.IS_PALYED_ADVERT, false);
		if (isPlayAdvert) {
			UIUtil.sendBroadCast(this, Constants.ADVERTISING_ACTION,
					new Intent());
			LogUtil.d("tv_launcher", "send broadcast -- android.bellmann.ADVERTISING");
			return;
		}

		// 开始更新广告图片或者视频
		startService(new Intent(this, DownLoadService.class));

		if (FileManager.isSdcard(MainActivity.this) != null) {
			int type = FileManager.getFileType(FileManager
					.isSdcard(MainActivity.this));
			if (type == FileManager.FILE_TYPE_PIC) {
				Intent it = new Intent(this, TupianActivity.class);
				startActivity(it);
			} else if (type == FileManager.FILE_TYPE_VIDEO) {
				Intent it = new Intent(this, VideoSurfaceActivity.class);
				startActivity(it);
			} else {
				Intent it = new Intent(this, TupianActivity.class);
				startActivity(it);
			}
		} else {
			Intent it = new Intent(this, TupianActivity.class);
			startActivity(it);
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_HOME) {
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}



}
