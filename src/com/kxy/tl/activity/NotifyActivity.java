package com.kxy.tl.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.kxy.tl.R;
import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.callback.StringCallback;
import com.vunke.tl.base.BaseActivity;
import com.vunke.tl.base.Config;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.util.LogUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 消息推送
 * 
 * @author zhuxi
 * 
 */
@SuppressLint("NewApi")
public class NotifyActivity extends BaseActivity {
	private WebView notfy_webView;
	private LinearLayout notify_layout;
	private String name;
	/**
	 * 结束时间
	 */
	private long endtime;
	/**
	 * 开始时间
	 */
	private long starttime;
	/**
	 * 时间戳
	 */
	private long timestamp;
	/**
	 * 推送ID
	 */
	private int push_id;
	private int strategy_id;
	/**
	 * 推送类型
	 */
	private int push_type;
	private int behavior_type;
	/**
	 * 推送地址
	 */
	private String path;
	private String version_code;
	/**
	 * 键值
	 */
	private String keyvalue = "113";

	private String getVersion_vode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		LogUtil.i("tv_launcher", "onCreate");
		setContentView(R.layout.activity_notify);
		init();
		initView();
	}

	/**
	 * 初始化 意图 获取 用户信息
	 */
	public void init() {
		Intent intent = getIntent();
		if (intent.hasExtra("UserId")) {
			name = intent.getStringExtra("UserId");
			LogUtil.i("tv_launcher", "UserId:" + name);
		}else {
			LogUtil.e("tv_launcher", "NotifyActivity get user_id faied");
			name = "";
		}
		if (intent.hasExtra("path")) {
			path = intent.getStringExtra("path");
			LogUtil.i("tv_launcher", "path:" + path);
			if (TextUtils.isEmpty(path)) {
				StartEPGing();
			}
		} else {
			StartEPGing();
		}
		if (intent.hasExtra("push_id")) {
			push_id = intent.getIntExtra("push_id", 0);
			LogUtil.i("tv_launcher", "push_id:" + push_id);
		}
		if (intent.hasExtra("strategy_id")) {
			strategy_id = intent.getIntExtra("strategy_id", 0);
			LogUtil.i("tv_launcher", "strategy_id:" + strategy_id);
		}
		if (intent.hasExtra("behavior_type")) {
			behavior_type = intent.getIntExtra("behavior_type", 0);
			LogUtil.i("tv_launcher", "behavior_type:" + behavior_type);
		}
		if (intent.hasExtra("push_type")) {
			push_type = intent.getIntExtra("push_type", 0);
			LogUtil.i("tv_launcher", "push_type:" + push_type);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date(System.currentTimeMillis());
		String format = dateFormat.format(date);
		getVersion_vode = 1 + format;
	}

	private void StartEPGing() {
		UIUtil.StartMangGuoEPG(getApplicationContext(),name);
		finish();
	}

	/**
	 * 初始化 webView
	 */
	@SuppressLint({ "JavascriptInterface", "SetJavaScriptEnabled" })
	private void initView() {
		notify_layout = (LinearLayout) findViewById(R.id.notify_layout);
		notfy_webView = (WebView) findViewById(R.id.notfy_webView);

		// notfy_webView.clearCache(true);
		// notfy_webView.clearHistory();
		WebSettings settings = notfy_webView.getSettings();
		// 支持js
		settings.setJavaScriptEnabled(true);
		// 设置字符编码
		settings.setDefaultTextEncodingName("GBK");
		// 启用支持javascript
		settings.setJavaScriptEnabled(true);
		// 设置可以支持缩放
		settings.setBuiltInZoomControls(true);
		settings.setLightTouchEnabled(true);
		settings.setSupportZoom(true);
		// 不使用缓存，只从网络获取数据.
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		// settings.setLoadWithOverviewMode(true);
		// 支持JS交互
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		notfy_webView.addJavascriptInterface(new JavaScriptObject(),
				"tv_launcher");

		notfy_webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				LogUtil.i("tv_launcher", "网页加载中");
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				LogUtil.i("tv_launcher", "网页加载结束");
				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						notfy_webView.setVisibility(View.VISIBLE);
					}
				}, 200);
			}
			@Override
			 public boolean shouldOverrideUrlLoading(WebView view, String url) {
			 view.loadUrl(url);//在2.3上面不加这句话，可以加载出页面，在4.0上面必须要加入，不然出现白屏
			 return true;
			}
		});
		notfy_webView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if (newProgress < 1) { // 加载中
					LogUtil.i("tv_launcher", "网页加载进度：" + newProgress);
					notfy_webView.requestFocus();
					// notfy_webView.setVisibility(View.GONE);
				} else if (newProgress == 100) { // 网页加载完成
					LogUtil.i("tv_launcher", "网页加载进度：" + newProgress);
					notfy_webView.requestFocus();
					// notfy_webView.setVisibility(View.VISIBLE);
				}
				super.onProgressChanged(view, newProgress);
			}
		});
		notfy_webView.setDownloadListener(new MyWebViewDownLoadListener());
		String pasams = "account="+name;
		notfy_webView.postUrl(path, pasams.getBytes());
		//http://124.232.135.239:8088/scene_push_anzhuo/scene_1.jsp
	}

	/**
	 * WebView 点击下载监听
	 * 
	 * @author zhuxi
	 */
	private class MyWebViewDownLoadListener implements DownloadListener {

		@Override
		public void onDownloadStart(String url, String userAgent,
				String contentDisposition, String mimetype, long contentLength) {
			Uri uri = Uri.parse(url);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);
		}

	}

	/**
	 * 安卓与JS交互
	 * 
	 * @author zhuxi
	 */
	public class JavaScriptObject {
		@JavascriptInterface
		public void sendMessageToJAVA(String json) {
			LogUtil.i("tv_launcher", "getJavaScript:" + json);
			// Toast.makeText(getApplicationContext(), json,
			// Toast.LENGTH_SHORT).show();
			if (json.equals("0")) {
				LogUtil.i("tv_launcher", "用户没有操作");
				UIUtil.StartMangGuoEPG(getApplicationContext(),name);
				finish();
			} else if (json.equals("1")) {
				LogUtil.i("tv_launcher", "用户正在操作");
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		LogUtil.e("tv_launcher", "keyCode:" + keyCode);
		
		if (keyCode == KeyEvent.KEYCODE_HOME) {
			LogUtil.e("tv_launcher", "用户按HOME键");
			StartEPGing();
		}
		if ((keyCode == KeyEvent.KEYCODE_BACK) && notfy_webView.canGoBack()) {//尚未测试
			if (notfy_webView.getUrl().contains(path)) {
				StartEPGing();
				return false;
			} else {
				notfy_webView.goBack(); // goBack()表示返回WebView的上一页面
				return true;
			}
		}else if ((keyCode == KeyEvent.KEYCODE_BACK)) {// && notfy_webView.canGoBack()
			StartEPGing();
			// notfy_webView.goBack(); // goBack()表示返回WebView的上一页面
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}

	@Override
	protected void onStop() {
		super.onStop();
		endtime = System.currentTimeMillis() / 1000;
		timestamp = System.currentTimeMillis() / 1000;
		OkHttpUtils.post(Config.Notify_Url + Config.Behaviour).tag(this)
				.params("push_id", push_id + "").params("name", name)
				.params("stratey_id", strategy_id + "")
				.params("behavior_type", behavior_type + "")
				.params("push_type", push_type + "")
				.params("starttime", starttime + "")
				.params("endtime", endtime + "")
				.params("timestamp", timestamp + "")
				.params("keyvalue", keyvalue)
				.params("version_code", version_code)
				.params("business_type", "2").execute(new StringCallback() {

					@Override
					public void onResponse(boolean isFromCache, String t,
							Request request, @Nullable Response response) {
						// LogUtil.e(TAG, request.body());
						LogUtil.i("tv_launcher", "请求成功:" + t);
					}

					@Override
					public void onError(boolean isFromCache, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onError(isFromCache, call, response, e);
						LogUtil.e("tv_launcher", "Onerror");
					}
				});
	}

}
