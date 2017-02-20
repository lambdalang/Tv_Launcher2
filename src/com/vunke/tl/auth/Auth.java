package com.vunke.tl.auth;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.lzy.okhttputils.OkHttpUtils;
import com.lzy.okhttputils.callback.StringCallback;
import com.vunke.tl.base.Config;
import com.vunke.tl.service.GroupStrategy.GroupStrategyBean;
import com.vunke.tl.util.LogUtil;

import org.json.JSONObject;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class Auth {

	private static final String TAG = Auth.class.getSimpleName();
	public static final int AUTH_CODE_AUTH_NOT_AUTH =   0x98000;
	public static final int AUTH_CODE_AUTH_ERROR =   0x98002;
	public static final int AUTH_CODE_AUTH_SUCCESS =   0x98001;
	public static final int AUTH_CODE_AUTH_INIT = 0x98003;
	/**
	 * 通过查询数据库获取认证信息
	 * 
	 * @param paramContext
	 *            上下文
	 * @param paramAuthInfo
	 *            JavaBean
	 */
	public static void queryDeviceInfo(Context paramContext,
			AuthInfo paramAuthInfo) {
		Uri localUri = Uri
				.parse("content://com.starcor.mango.hndx.provider/deviceinfo");
		Cursor localCursor = paramContext.getContentResolver().query(localUri,
				null, null, null, null);
		try {
			if (localCursor.moveToFirst()) {
				paramAuthInfo.AuthServer = localCursor.getString(localCursor
						.getColumnIndex("auth_url"));
				paramAuthInfo.StbId = localCursor.getString(localCursor
						.getColumnIndex("stb_id"));
				paramAuthInfo.UserId = localCursor.getString(localCursor
						.getColumnIndex("user_id"));
				paramAuthInfo.Password = localCursor.getString(localCursor
						.getColumnIndex("user_password"));
				paramAuthInfo.Password = DecodePassword
						.decode(paramAuthInfo.Password);
				paramAuthInfo.AccessMethod = localCursor.getString(localCursor
						.getColumnIndex("access_method"));
			}
			return;
		} finally {
			if (localCursor != null)
				localCursor.close();
		}
	}

	/**
	 * 获取密码
	 * 
	 * @param password
	 * @return
	 */
	public static byte[] getKeyCode(String password) {
		byte[] keyCode = new byte[24];
		byte[] arrByte = password.getBytes();
		for (int i = 0; i < keyCode.length; i++) {
			if (i < 24) {
				if (i < arrByte.length) {
					keyCode[i] = arrByte[i];
				} else {
					keyCode[i] = 48;
				}
			}
		}
		return keyCode;
	}

	/**
	 * 生成随机数
	 * 
	 * @return
	 */
	public static String getRandom() {
		// 随机数
		Random localRandom = new Random(Calendar.getInstance()
				.getTimeInMillis());
		Object[] arrayOfObject = new Object[1];
		arrayOfObject[0] = Integer.valueOf(localRandom.nextInt(99999999));
		String random = String.format("%08d", arrayOfObject);

		return random;
	}

	/**
	 * DESede 加密
	 * 
	 * @param paramString
	 *            加密信息
	 * @param paramArrayOfByte
	 *            加密密钥
	 * @return 十六进制代码
	 * @throws Exception
	 */
	public static String DesEncrypt(String paramString, byte[] paramArrayOfByte)
			throws Exception {
		Cipher localCipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
		DESedeKeySpec localDESedeKeySpec = new DESedeKeySpec(paramArrayOfByte);
		localCipher.init(1, SecretKeyFactory.getInstance("desede")
				.generateSecret(localDESedeKeySpec));
		String str = "";
		byte[] arrayOfByte = localCipher.doFinal(paramString.getBytes("ASCII"));
		return bytesToHexString(arrayOfByte);
	}

	/**
	 * Convert byte[] to hex
	 * string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
	 * 
	 * @param src
	 *            byte[] data
	 * 
	 * @return hex string
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	/**
	 * 更新本地Cookie信息
	 */
	public static void responseUpdateCookieHttpURL(CookieStore store) {
		boolean needUpdate = false;
		List<HttpCookie> cookies = store.getCookies();
		HashMap<String, String> cookieMap = null;
		if (cookieMap == null) {
			cookieMap = new HashMap<String, String>();
		}
		for (HttpCookie cookie : cookies) {
			String key = cookie.getName();
			String value = cookie.getValue();
			if (cookieMap.size() == 0 || !value.equals(cookieMap.get(key))) {
				needUpdate = true;
			}
			cookieMap.put(key, value);
			// BDebug.e(HTTP_COOKIE, cookie.getName() + "---->" +
			// cookie.getDomain() + "------>" + cookie.getPath());
			LogUtil.i("cookie", cookie.getName() + "---->\n" + cookie.getDomain()
					+ "---->" + cookie.getPath());
		}

	}

	private static String Action = "Login";

	/**
	 * 获取 UserToken
	 * 
	 * @param authInfo
	 */
	public static void GetUserToken(AuthInfo authInfo, Context context,
			StringCallback callback) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("userId", authInfo.UserId);
			jsonObject.put("action", Action);
			LogUtil.i("tv_launcher", jsonObject.toString());
			OkHttpUtils.post(Config.BASE_WS_URL2 + Config.AUTH).tag(Auth.TAG)
					.params("json", jsonObject.toString()).connTimeOut(60000)
					.readTimeOut(60000).execute(callback
					// new StringCallback() {
					// @Override
					// public void onResponse(boolean isFromCache, String t,
					// Request request, @Nullable Response response) {
					// try {
					// LogUtil.e(TAG, "getData" + t);
					// JSONObject jsonData = new JSONObject(t);
					// int code = jsonData.getInt("code");
					// switch (code) {
					// case 200:
					// LogUtil.e("tv_launcher", "200");
					// String encryToken = jsonData
					// .getString("encryToken");
					// System.out.println(encryToken);
					// authInfo.EncryToken = encryToken;
					// LogUtil.e(TAG, "EncryToken=" + encryToken);
					// // GetAuthInfo(authInfo, context);
					// // init3(authInfo);
					// break;
					// case 400:
					// LogUtil.e("tv_launcher", "400");
					// break;
					// case 500:
					// LogUtil.e("tv_launcher", "500");
					// break;
					//
					// default:
					// break;
					// }
					//
					// } catch (Exception e) {
					// e.printStackTrace();
					// }
					// }
					//
					// @Override
					// public void onError(boolean isFromCache, Call call,
					// Response response, Exception e) {
					// super.onError(isFromCache, call, response, e);
					// LogUtil.e("tv_launcher", "error");
					//
					// }
					//
					// @Override
					// public void onAfter(boolean isFromCache,
					// @Nullable String t, Call call,
					// @Nullable Response response,
					// @Nullable Exception e) {
					// super.onAfter(isFromCache, t, call, response, e);
					//
					// }
					// }
					);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取认证信息
	 * 
	 * @param authInfo
	 */
	public static void GetAuthInfo(AuthInfo authInfo, Context context,
			StringCallback callback) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("userId", authInfo.UserId);
			jsonObject.put("accessMethod", "pppoe");
			jsonObject.put("encryToken", authInfo.EncryToken);
			jsonObject.put("stbId", authInfo.StbId);
			jsonObject.put("mac", authInfo.MacAddr.trim());
			jsonObject.put("passWord", authInfo.Password);
			LogUtil.i("tv_launcher", jsonObject.toString());
			OkHttpUtils.post(Config.BASE_WS_URL2 + Config.UPLOAD_AUTH_INFO)
					.tag(Auth.TAG).params("json", jsonObject.toString())
					.connTimeOut(60000).readTimeOut(60000).execute(callback
					// new StringCallback() {

					// @Override
					// public void onResponse(boolean isFromCache, String t,
					// Request request, Response response) {
					// LogUtil.e(TAG, "获取成功" + t);
					// try {
					// JSONObject json = new JSONObject(t);
					// int code = json.getInt("code");
					// switch (code) {
					// case 200:
					// Gson gson = new Gson();
					// GroupInfo groupInfo = gson.fromJson(t,
					// GroupInfo.class);
					// if (!TextUtils.isEmpty(groupInfo
					// .getUserGroupNmb())) {
					// INSERT(context, t, authInfo);
					// }
					// // 根据不同的userGroupNum 启动不同的EPG
					// StartEPG(context, groupInfo, authInfo);
					// // JSONObject jsonObject = new JSONObject();
					// // jsonObject.put("userToken",
					// // groupInfo.getUserToken());
					// // jsonObject.put("epgUrl",
					// // groupInfo.getEpgDomain());
					// // GetEpgHomeAuth(Config.BASE_WS_URL2
					// // + Config.EPG_HOME_AUTH,
					// // jsonObject.toString());
					//
					// break;
					// case 400:
					// LogUtil.e("tv_launcher", "400");
					// break;
					// case 500:
					// LogUtil.e("tv_launcher", "500");
					// break;
					//
					// default:
					// break;
					// }
					//
					// } catch (Exception e) {
					// e.printStackTrace();
					// }
					// }
					//
					// @Override
					// public void onError(boolean isFromCache, Call call,
					// Response response, Exception e) {
					// super.onError(isFromCache, call, response, e);
					// LogUtil.e("tv_launcher", "error");
					// }
					//
					// @Override
					// public void onAfter(boolean isFromCache, String t,
					// Call call, Response response, Exception e) {
					// super.onAfter(isFromCache, t, call, response, e);
					//
					// }
					//
					// }
					);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void StartEPG(Context context, GroupInfo groupInfo,
			AuthInfo authInfo, StringCallback callback) {
		// String userGroupNum = groupInfo.getUserGroupNmb();
		// if (userGroupNum.equals("2a000025")) {//芒果帐号
		//
		// } else if(userGroupNum.equals("2a000142")){//酒店帐号
		//
		// }else {
		//
		// }
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("userGroupId", groupInfo.getUserGroupNmb());
			OkHttpUtils.post(Config.BASE_WS_URL2 + Config.GetStartInfo)
					.tag(context).params("json", jsonObject.toString())
					.execute(callback
					// new StringCallback() {
					//
					// @Override
					// public void onResponse(boolean isFromCache, String t,
					// Request request, Response response) {
					// LogUtil.e(TAG, "获取成功" + t);
					// try {
					// JSONObject js = new JSONObject(t);
					// if (js.has("code")) {
					// int code = js.getInt("code");
					// switch (code) {
					// case 200:
					// Gson gson = new Gson();
					// StartInfoBean startInfoBean = gson
					// .fromJson(t,
					// StartInfoBean.class);
					// String startPackger = startInfoBean
					// .getData().getStartPackage();
					// String startActivity = startInfoBean
					// .getData().getStartActivity();
					// GoActivity(context, startPackger,
					// startActivity);
					// break;
					// case 400:
					// LogUtil.e(TAG, "请求失败,ErrorCode:400");
					// break;
					// case 500:
					// LogUtil.e(TAG, "请求失败,ErrorCode:500");
					// break;
					//
					// default:
					// break;
					// }
					// }
					// } catch (Exception e) {
					// e.printStackTrace();
					// }
					// }
					//
					// @Override
					// public void onError(boolean isFromCache, Call call,
					// Response response, Exception e) {
					// super.onError(isFromCache, call, response, e);
					// LogUtil.e("tv_launcher", "error");
					// }
					//
					// }
					);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static PackageInfo GetPackageInfo(Context context, String packageName) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(packageName, 0);
			if (packageInfo == null) {
				Toast.makeText(context, "没有安装该应用", Toast.LENGTH_SHORT).show();
				return null;
			} else {
				// Intent intent = new Intent(Intent.ACTION_MAIN,null);
				// intent.addCategory(Intent.CATEGORY_LAUNCHER);
				// intent.setPackage("com.vunken.tv_sharehome");
				// List<ResolveInfo> resolveInfoList =
				// getPackageManager().queryIntentActivities(intent, 0);
				// ResolveInfo resolveInfo = resolveInfoList.iterator().next();
				// if (resolveInfo!=null) {
				// String packageName = resolveInfo.activityInfo.packageName;
				// Intent intent2 = new Intent(Intent.ACTION_MAIN,null);
				// intent2.addCategory(intent.CATEGORY_LAUNCHER);
				// ComponentName componentName = new ComponentName(packageName,
				// "com.vunken.tv_sharehome.activity.LoginActivity");
				// intent.setComponent(componentName);
				// startActivity(intent2);
				// }
				// Intent intent = new Intent();
				// ComponentName componentName = new ComponentName(
				// packageName,
				// packageActivity);
				// intent.setComponent(componentName);
				// context.startActivity(intent);
				return packageInfo;
			}
		} catch (Exception e) {
//			e.printStackTrace();
		}
		return null;
	}

	public static void GOtoActivity(Context context, String packageName) {
		PackageInfo pi;
		try {
			pi = context.getPackageManager().getPackageInfo(packageName, 0);
			Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
			resolveIntent.setPackage(pi.packageName);
			PackageManager pManager = context.getPackageManager();
			List apps = pManager.queryIntentActivities(resolveIntent, 0);
			ResolveInfo ri = (ResolveInfo) apps.iterator().next();
			if (ri != null) {
				packageName = ri.activityInfo.packageName;
				String className = ri.activityInfo.name;
				Intent intent = new Intent(Intent.ACTION_MAIN);
				ComponentName cn = new ComponentName(packageName, className);
				intent.setComponent(cn);
				context.startActivity(intent);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 插入用户信息到数据库，内容提供者 ，方便应用商城查询
	 * 
	 * @param context
	 * @param t
	 * @param authinfo
	 */
	public static void INSERT(Context context, String t, AuthInfo authinfo) {

		Uri uri = Uri
				.parse("content://com.vunke.tvlauncher.provider/groupinfo");
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put("body", t);
		values.put("user_id", authinfo.UserId);
		values.put("create_time", System.currentTimeMillis());
		Uri uri2 = resolver.insert(uri, values); // 内部调用内容提供者的insert方法
		LogUtil.i("tv_launcher", "insert date to group_info：" + uri2);
		// 查询数据库的代码
		// Cursor query = resolver.query(uri, null, null, null, null);
		// while (query.moveToNext()) {
		// System.err.println(query.getString( query.getColumnIndex("body")));
		// System.err.println(query.getString(
		// query.getColumnIndex("user_id")));
		// }
	}

	/**
	 * 插入用户分组策略到数据库，内容提供者
	 * 
	 * @param context
	 * @param bean
	 */
	public static void InsertGroupStrategy(Context context, GroupStrategyBean bean) {
		Uri uri = Uri
				.parse("content://com.vunke.tvlauncher.provider2/group_strategy");
		ContentResolver resolver = context.getContentResolver();
		ContentValues values = new ContentValues();
		values.put("epg_code", bean.getEPGcode());
		values.put("epg_package", bean.getEPGpackage());
		values.put("group_address", bean.getGroupAddress());
		values.put("group_name", bean.getGroupName());
		values.put("group_status", bean.getGroupStatus());
		values.put("group_type", bean.getGroupType());
		values.put("group_number", bean.getGrpupNumber());
		values.put("create_time", System.currentTimeMillis());
		values.put("user_id", bean.getUserId());
		Uri uri2 = resolver.insert(uri, values); // 内部调用内容提供者的insert方法
		LogUtil.i("tv_launcher", "insert date to group_strategy：" + uri2);
		// 查询数据库的代码
		// Cursor query = resolver.query(uri, null, null, null, null);
		// while (query.moveToNext()) {
		// // System.err.println(query.getString(query.getColumnIndex("body")));
		// // System.err
		// // .println(query.getString(query.getColumnIndex("user_id")));
		// }
	}
	public static int getAuthCode(Context context,int defultVaule){
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		int AuthCode = sp.getInt("AuthCode",defultVaule);
		if(AuthCode == AUTH_CODE_AUTH_SUCCESS){
			LogUtil.i("tv_launcher","auth success code:"+AuthCode);
			return AUTH_CODE_AUTH_SUCCESS;
		}else if(AuthCode ==AUTH_CODE_AUTH_NOT_AUTH){
			return AUTH_CODE_AUTH_NOT_AUTH;
		}else if(AuthCode == AUTH_CODE_AUTH_ERROR){
			return  AUTH_CODE_AUTH_ERROR;
		}else{
			LogUtil.i("tv_launcher","auth failed code:"+AuthCode);
			return  AUTH_CODE_AUTH_ERROR;
		}
	}
	public static void setAuthCode(Context context,int vaule){
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		sp.edit().putInt("AuthCode",vaule).commit();
	}
	public static void setAuthErrCode(Context context, String errorCode) {
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		sp.edit().putString("AuthErrCode",errorCode).commit();
	}
	public static String getAuthErrCode(Context context,String defultVaule){
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		String AuthErrCode = sp.getString("AuthErrCode",defultVaule);
		return AuthErrCode;
	}
	public static void RemoveAuthCode(Context context){
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		sp.edit().remove("AuthCode").commit();
	}
	public static void RemoveAuthErrCode(Context context){
		SharedPreferences sp = context.getSharedPreferences(context.getPackageName(),Context.MODE_APPEND);
		sp.edit().remove("AuthErrCode").commit();

	}
	/**
	 * 被弃用 获取认证信息
	 * 
	 * @param url
	 * @param json
	 */
	protected static void GetEpgHomeAuth(String url, String json) {
		LogUtil.i("tv_launcher", "请求数据:" + json);
		// detail_data.append("\n " + userToken);
		OkHttpUtils.post(url).tag(Auth.TAG).params("json", json)
		// .addCookie("UserToken", userToken)
		// .addCookie("JSSESSIONID", "ADE12314DA23")
				.execute(new StringCallback() {

					@Override
					public void onResponse(boolean isFromCache, String t,
							Request request, @Nullable Response response) {
						LogUtil.i("tv_launcher", "DATA:" + t);
						try {

						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(boolean isFromCache, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onError(isFromCache, call, response, e);
						LogUtil.e("tv_launcher", "ERROR");
					}

					@Override
					public void onAfter(boolean isFromCache,
							@Nullable String t, Call call,
							@Nullable Response response, @Nullable Exception e) {
						super.onAfter(isFromCache, t, call, response, e);
					}
				});

	}

	/**
	 * 调用联创的接口 测试用的 暂不使用
	 * 
	 * @param authInfo
	 */
	protected static void init3(final AuthInfo authInfo) {

		try {
			String path = authInfo.AuthServer;
			path = path.substring(0, path.lastIndexOf('/')) + "/uploadAuthInfo";

			LogUtil.i("tv_launcher", "PATH:" + path);

			// 密钥
			byte[] keyCode = Auth.getKeyCode(authInfo.Password);
			LogUtil.i("tv_launcher", "KEY:" + new String(keyCode));

			// 随机数
			String random = Auth.getRandom();

			LogUtil.i("tv_launcher", "Random:" + random);
			// Auth 信息
			String authData = random + "$" + authInfo.EncryToken + "$"
					+ authInfo.UserId + "$" + authInfo.StbId + "$"
					+ authInfo.IpAddr + "$" + authInfo.MacAddr.trim() + "$"
					+ "990070$CTC";

			LogUtil.i("tv_launcher", "AUTH_DATA:" + authData);

			// Auth 加密信息
			String Authenticator = Auth.DesEncrypt(authData, keyCode)
					.toUpperCase();
			// URL url = new URL(path);
			// java.net.CookieManager manager = new java.net.CookieManager();
			// manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			// CookieHandler.setDefault(manager);
			//
			// HttpURLConnection connection = (HttpURLConnection) url
			// .openConnection();
			// connection.setRequestMethod("POST");
			// connection.setDoOutput(true);
			// connection.setDoInput(true);
			// connection.setUseCaches(false);
			// connection.setAllowUserInteraction(false);
			// connection.setRequestProperty("Content-Type",
			// "application/x-www-form-urlencoded");
			// String data = "UserID=" + authInfo.UserId + "$AccessMethod="
			// + authInfo.AccessMethod + "$Authenticator=" + Authenticator;
			// connection.setRequestProperty("Content-Length", "" +
			// data.length());
			// LogUtil.e(TAG, data);
			// DataOutputStream outputStream = new
			// DataOutputStream(connection.getOutputStream());
			// outputStream.write(data.getBytes());
			// outputStream.close();
			// connection.connect();
			// connection.getHeaderFields();
			// CookieStore store = manager.getCookieStore();
			// int resultCode = connection.getResponseCode();
			// Auth.responseUpdateCookieHttpURL(store);
			//
			// connection.connect();
			// InputStream inputStream = null;
			// BufferedReader reader = null;
			// // 如果应答码为200的时候，表示成功的请求带了，这里的HttpURLConnection.HTTP_OK就是200
			// if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			// // 获得连接的输入流
			// inputStream = connection.getInputStream();
			// // 转换成一个加强型的buffered流
			// reader = new BufferedReader(new InputStreamReader(inputStream));
			// // 把读到的内容赋值给result
			// final String result = reader.readLine();
			// LogUtil.e(TAG, "result"+result);
			// reader.close();
			// inputStream.close();
			// connection.disconnect();
			// }
			LogUtil.i("tv_launcher", "Authenticator" + Authenticator.toUpperCase() + "长度"
					+ Authenticator.length());

			// JSONObject jsonObject = new JSONObject();
			// jsonObject.put("UserID", authInfo.UserId);
			// jsonObject.put("AccessMethod", authInfo.AccessMethod);
			// jsonObject.put("Authenticator", Authenticator);
			// LogUtil.e(TAG, "JSON_DATA" + jsonObject.toString());
			OkHttpUtils.post(path).tag(Auth.TAG)
					.params("UserID", authInfo.UserId)
					.params("AccessMethod", authInfo.AccessMethod)
					.params("Authenticator", Authenticator)
					.execute(new StringCallback() {

						@Override
						public void onResponse(boolean isFromCache, String t,
								Request request, @Nullable Response response) {
							LogUtil.i("tv_launcher", "DATA:" + t);

						}

						@Override
						public void onError(boolean isFromCache, Call call,
								@Nullable Response response,
								@Nullable Exception e) {
							super.onError(isFromCache, call, response, e);
							LogUtil.e("tv_launcher", "ERROR");
						}

						@Override
						public void onAfter(boolean isFromCache,
								@Nullable String t, Call call,
								@Nullable Response response,
								@Nullable Exception e) {
							super.onAfter(isFromCache, t, call, response, e);
						}
					});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String getAccessMethod(Context context){
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivityManager != null) {
				NetworkInfo networkInfo=connectivityManager.getActiveNetworkInfo();
				String typeName = networkInfo.getTypeName();
				return typeName;
			}
		}catch(Exception e){
			e.printStackTrace();;
		}
		return "";
	}

}