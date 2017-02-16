package com.kxy.tl.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.kxy.tl.R;
import com.vunke.tl.base.BaseActivity;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.bean.NotifyBean;
import com.vunke.tl.bean.NotifyBean.DataBean;
import com.vunke.tl.bean.NotifyBean.DataBean.ProductAttr;
import com.vunke.tl.service.NotifyService;
import com.vunke.tl.util.FileManager;
import com.vunke.tl.util.LogUtil;
import com.vunke.tl.util.RxBus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

/**
 * 广告图片播放
 * 
 * @author zhuxi
 */
public class TupianActivity extends BaseActivity {
	/** Called when the activity is first created. */
	private ImageView image = null;
	private AnimationDrawable animationDrawable = null;
	private static Handler handler;
	private static Runnable runnable;
	private Subscription subscribe;
	
	private NotifyBean bean2;
	
	private Handler handler2 = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0x1134:
				if (UIUtil.isNetConnected(getApplicationContext()) == true) {
					initUrl(bean2);
				} else {
					handler2.sendEmptyMessageDelayed(0x1134, 2000);
				}

				break;
			default:
				break;
			}
		};
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pic_dispaly);
		image = (ImageView) findViewById(R.id.imageview);
//		initService();
		initRX();
	}

	/**
	 * 获取用户信息和推送数据 service
	 */
	private void initService() {
		LogUtil.i("tv_launcher", "start notifyservice");
		Intent intent = new Intent(TupianActivity.this, NotifyService.class);
		intent.setAction(NotifyService.actionName);
		startService(intent);
	}

	/**
	 * 获取 用户信息 RxBus
	 */
	private void initRX() {
		subscribe = RxBus.getInstance().toObservable(NotifyBean.class)
				.filter(new Func1<NotifyBean, Boolean>() {

					@Override
					public Boolean call(NotifyBean arg0) {
						return arg0.getRxBuscode() == 20161207;
					}
				}).subscribe(new Subscriber<NotifyBean>() {

					@Override
					public void onCompleted() {
					}

					@Override
					public void onError(Throwable e) {
						LogUtil.e("tv_launcher", "RxBus error");
						e.printStackTrace();
						this.isUnsubscribed();
					}

					@Override
					public void onNext(final NotifyBean arg0) {
						if (arg0.getRxBuscode() == 20161207) {
							UserId = arg0.getUserId();
							LogUtil.i("tv_launcher", "UserId:" + UserId);
							bean2 = arg0;
							handler2.sendEmptyMessageDelayed(0x1134, 2000);
							//
						}
					}
				});
	}

	private void initAnimationDrawable() {
		animationDrawable = new AnimationDrawable();
		List<Drawable> picsPath = FileManager.getpicsPath(
				FileManager.isSdcard(TupianActivity.this), getResources());
		if (picsPath != null && picsPath.size() > 0) {
			for (int i = 0; i < picsPath.size(); i++) {
				animationDrawable.addFrame(picsPath.get(i), 5000);
			}
		} else {
			picsPath = FileManager.getPicFromAsset(this, getResources());
			for (int i = 0; i < picsPath.size(); i++) {
				animationDrawable.addFrame(picsPath.get(i), 5000);
			}
		}
		// 设置是否重复播放，false为重复
		animationDrawable.setOneShot(true);
		image.setImageDrawable(animationDrawable);
		animationDrawable.start();

	}

	// 验证当前数据类型是否出现多条
	private int notifdata = 0;
	// 验证当前网页是否正常
	private boolean initWebView = false;

	/**
	 * 解析数据 加载WebView
	 * 
	 * @param bean
	 */
	protected void initUrl(NotifyBean bean) {
		if (bean != null && bean.getData()!=null) {
			List<DataBean> data = bean.getData();
			if (data != null && data.size() != 0) {
				LogUtil.i("tv_launcher", "data数据条数" + data.size());
				// 首先判断数据中是否有 push_type = 6 的字段
				LogUtil.i("tv_launcher", "首先判断数据中是否有 push_type");
				boolean getCode = false;
				boolean hasCode = false;
				for (int i = 0; i < data.size(); i++) {
					getCode = data.get(i).getPush_type() == 6 ? true : false;
					LogUtil.i("tv_launcher", "push_type是否存在？\n" + getCode);
					if (getCode == true) {
						path = data.get(i).getSpecial_url();
						if (!TextUtils.isEmpty(path)) {
							ArrayList<ProductAttr> productAttr = data.get(i)
									.getProductAttr();
							if (productAttr != null) {// 商机单
								getCode = false;
								continue;
							} else {
								hasCode = true;
								notifdata++;
								if (notifdata == 1) {
									push_id = data.get(i).getPush_id();
									strategy_id = data.get(i).getStrategy_id();
									behavior_type = data.get(i).getBusiType();
									push_type = data.get(i).getPush_type();
									LogUtil.i("tv_launcher", path);
									// notfy_webView.requestFocus();
									new Thread(new Runnable() {

										@Override
										public void run() {
											int status = -1;
											HttpHead head = new HttpHead(path);
											HttpClient client = new DefaultHttpClient();
											HttpResponse resp;
											try {
												resp = client.execute(head);
												status = resp.getStatusLine()
														.getStatusCode();
												if (status != 200) {
													LogUtil.e("tv_launcher",
															"load url failed,status:"
																	+ status);
													initWebView = false;
												} else {
													LogUtil.i("tv_launcher",
															"load url success");
													initWebView = true;
												}
											} catch (Exception e) {
												initWebView = false;
												LogUtil.e("tv_launcher",
														"load url failed");
												e.printStackTrace();
											}
										}
									}).start();
									i = data.size();
								} else {
									LogUtil.i("tv_launcher",
											"path = 6 has two size");
								}
							}
						} else {
							LogUtil.e("tv_launcher", "path is null");
						}
					} else {
						LogUtil.i("tv_launcher", "push_type = 6 is false");
					}
				}
				if (hasCode) {

				} else {
					// StartEPGing();
				}
			} else {
				LogUtil.e("tv_launcher", "data is null");
			}
		} else {
			LogUtil.e("tv_launcher", "bean is null");
			// StartEPGing();
		}
	}

	private String path;
	private String UserId;
	private int push_id;
	private int strategy_id;
	private int behavior_type;
	private int push_type;

	@Override
	protected void onResume() {
		super.onResume();
		initService();
		initAnimationDrawable();
		startAnimationDrawable();
	}

	private void startAnimationDrawable() {
		int duration = 0;
		for (int i = 0; i < animationDrawable.getNumberOfFrames(); i++) {
			duration += animationDrawable.getDuration(i);
		}
		handler = new Handler();
		runnable = new Runnable() {
			public void run() {
				if (initWebView == true) {
					Intent intent = new Intent(TupianActivity.this,
							NotifyActivity.class);

					intent.putExtra("path", path);
					intent.putExtra("UserId", UserId);
					intent.putExtra("push_id", push_id);
					intent.putExtra("strategy_id", strategy_id);
					intent.putExtra("behavior_type", behavior_type);
					intent.putExtra("push_type", push_type);
					startActivity(intent);
					LogUtil.i("tv_launcher", "start notifyActivity");
				} else {
					UIUtil.StartMangGuoEPG(getApplicationContext(),UserId);
				}

			}
		};
		handler.postDelayed(runnable, duration);

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null && runnable != null) {
			handler.removeCallbacks(runnable);
			handler = null;
			runnable = null;
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (!subscribe.isUnsubscribed()) {
			subscribe.isUnsubscribed();
		}
		if (UIUtil.isServiceRunning(getApplicationContext(),
				NotifyService.class)) {
			Intent intent = new Intent(this, NotifyService.class);
			stopService(intent);
		}
	}
}