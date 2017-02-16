package com.kxy.tl.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;

import com.kxy.tl.R;
import com.vunke.tl.base.UIUtil;
import com.vunke.tl.bean.NotifyBean;
import com.vunke.tl.bean.NotifyBean.DataBean;
import com.vunke.tl.bean.NotifyBean.DataBean.ProductAttr;
import com.vunke.tl.service.NotifyService;
import com.vunke.tl.util.Constants;
import com.vunke.tl.util.FileManager;
import com.vunke.tl.util.LogUtil;
import com.vunke.tl.util.RxBus;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

/**
 * 使用MediaPlayer完成播放，同时界面使用SurfaceView来实现
 * 
 * 这里我们实现MediaPlayer中很多状态变化时的监听器
 * 
 * 使用Mediaplayer时，也可以使用MediaController类，但是需要实现MediaController.mediaController接口
 * 实现一些控制方法。
 * 
 * 然后，设置controller.setMediaPlayer(),setAnchorView(),setEnabled(),show()就可以了，
 * 这里不再实现
 * 
 */
public class VideoSurfaceActivity extends Activity implements
		OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPreparedListener, OnSeekCompleteListener, OnVideoSizeChangedListener,
		SurfaceHolder.Callback {
	private Display currDisplay;
	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	private MediaPlayer mplayer;
	private int vWidth, vHeight;

	// private boolean readyToPlay = false;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.video_surface);
		initService();
		initRX();
		surfaceView = (SurfaceView) this.findViewById(R.id.video_surface);
		// 给SurfaceView添加CallBack监听
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		// 为了可以播放视频或者使用Camera预览，我们需要指定其Buffer类型
		// 设置surfaceview不维护自己的缓冲区，而是等待屏幕的渲染引擎将内容推送到用户面前
		// holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		// 下面开始实例化MediaPlayer对象
		mplayer = new MediaPlayer();
		// MediaPlayer.create(this,
		// Uri.parse("storage/sdcard0/music.mp3"));
		mplayer.setOnCompletionListener(this);
		mplayer.setOnErrorListener(this);
		mplayer.setOnInfoListener(this);
		mplayer.setOnPreparedListener(this);
		mplayer.setOnSeekCompleteListener(this);
		mplayer.setOnVideoSizeChangedListener(this);
		LogUtil.v("Begin:::", "surfaceDestroyed called");
		// 然后指定需要播放文件的路径，初始化MediaPlayer
		String dataPath = FileManager.VideoPath(FileManager.isSdcard(VideoSurfaceActivity.this));

		 if(dataPath == null){
		 UIUtil.sendBroadCast(this, Constants.ADVERTISING_ACTION,new Intent());//节目播放业务
		 this.finish();
		 }

		try {
			mplayer.reset();

//			if (dataPath == null) {
//	            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.stellar);
//	            if (afd == null) 
//	            	return;
//
//	            mplayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
//	            afd.close();
//			} else {
				mplayer.setDataSource(dataPath);
//			}
			LogUtil.v("Next:::", "surfaceDestroyed called");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 然后，我们取得当前Display对象
		currDisplay = this.getWindowManager().getDefaultDisplay();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// 当Surface尺寸等参数改变时触发
		LogUtil.v("Surface Change:::", "surfaceChanged called");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mplayer != null) {
			// 当SurfaceView中的Surface被创建的时候被调用
			// 在这里我们指定MediaPlayer在当前的Surface中进行播放
			mplayer.setDisplay(holder);
			// player.setAudioStreamType(AudioManager.STREAM_MUSIC);//设置音乐流的类型

			// 在指定了MediaPlayer播放的容器后，我们就可以使用prepare或者prepareAsync来准备播放了
			mplayer.prepareAsync();
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mplayer != null && mplayer.isPlaying()) {
			// prosition=mediaPlayer.getCurrentPosition();
			mplayer.stop();
		}
		LogUtil.v("Surface Destory:::", "surfaceDestroyed called");
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
		// 当video大小改变时触发
		// 这个方法在设置player的source后至少触发一次
		LogUtil.v("Video Size Change", "onVideoSizeChanged called");

	}

	@Override
	public void onSeekComplete(MediaPlayer arg0) {
		// seek操作完成时触发
		LogUtil.v("Seek Completion", "onSeekComplete called");
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		// 当prepare完成后，该方法触发，在这里我们播放视频

		// 首先取得video的宽和高
		vWidth = player.getVideoWidth();
		vHeight = player.getVideoHeight();
		Point outSize = new Point();
		currDisplay.getSize(outSize);
		if (vWidth > outSize.x || vHeight > outSize.y) {
			// 如果video的宽或者高超出了当前屏幕的大小，则要进行缩放
			float wRatio = (float) vWidth / (float) outSize.x;
			float hRatio = (float) vHeight / (float) outSize.y;

			// 选择大的一个进行缩放
			float ratio = Math.max(wRatio, hRatio);

			vWidth = (int) Math.ceil((float) vWidth / ratio);
			vHeight = (int) Math.ceil((float) vHeight / ratio);

			// 设置surfaceView的布局参数
			surfaceView.setLayoutParams(new LinearLayout.LayoutParams(vWidth,
					vHeight));

		}
		// 然后开始播放视频
		player.start();
	}

	@Override
	public boolean onInfo(MediaPlayer player, int whatInfo, int extra) {
		// 当一些特定信息出现或者警告时触发
		switch (whatInfo) {
		case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
			break;
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			break;
		}
		return false;
	}

	@Override
	public boolean onError(MediaPlayer player, int whatError, int extra) {
		LogUtil.v("Play Error:::", "onError called");
		switch (whatError) {
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			LogUtil.v("Play Error:::", "MEDIA_ERROR_SERVER_DIED");
			break;
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			LogUtil.v("Play Error:::", "MEDIA_ERROR_UNKNOWN");
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onCompletion(MediaPlayer player) {
		// 当MediaPlayer播放完成后触发
		LogUtil.v("Play Over:::", "onComletion called");
		if (mplayer != null) {
			player.release();
			mplayer = null;
		}
		if (initWebView == true) {
			Intent intent = new Intent(VideoSurfaceActivity.this,
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
		this.finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mplayer != null) {
			if (mplayer.isPlaying()) {
				mplayer.stop();
			}
			mplayer.release();
			mplayer = null;
		}
		if (!subscribe.isUnsubscribed()) {
			subscribe.isUnsubscribed();
		}
		if (UIUtil.isServiceRunning(getApplicationContext(),
				NotifyService.class)) {
			Intent intent = new Intent(this, NotifyService.class);
			stopService(intent);
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
	/**
	 * 获取用户信息和推送数据 service
	 */
	private void initService() {
		LogUtil.i("tv_launcher", "start notifyservice");
		Intent intent = new Intent(VideoSurfaceActivity.this, NotifyService.class);
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

	// 验证当前数据类型是否出现多条
		private int notifdata = 0;
		// 验证当前网页是否正常
		private boolean initWebView = false;
		private String path;
		private String UserId;
		private int push_id;
		private int strategy_id;
		private int behavior_type;
		private int push_type;
		/**
		 * 解析数据 加载WebView
		 * 
		 * @param bean
		 */
		protected void initUrl(NotifyBean bean) {
			if (bean != null && bean.getData().size() != 0) {
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
}