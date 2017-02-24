package com.vunke.tl.auth;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.vunke.tl.util.LogUtil;

/**
 * 用户分组信息 内容提供者
 * @author zhuxi
 */
public class UserGroupInfoProvider extends ContentProvider {
	// public static final Uri Group_INFO_USERID = Uri
	// .parse("content://com.vunke.tvlauncher.provider/groupinfo");
	private final static String AUTHORITH = "com.vunke.tvlauncher.provider";
	private final static String PATH = "/groupinfo";
	private final static String PATHS = "/groupinfo/#";

	private final static String TABLE_NAME = "groupinfo";

	private final static UriMatcher mUriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	private static final int CODE_DIR = 1;
	private static final int CODE_ITEM = 2;
	static {
		mUriMatcher.addURI(AUTHORITH, PATH, CODE_DIR);
		mUriMatcher.addURI(AUTHORITH, PATHS, CODE_ITEM);
	}
	private GroupInfoHelper dbHelper;
	private SQLiteDatabase db;
	private static final String USER_ID = "name";
	private static final String USER_TOKEN = "value";

//	private static final String BODY = "body";
//	private static final String _ID = "_id";


	@Override
	public boolean onCreate() {
		dbHelper = new GroupInfoHelper(getContext());
		return false;
	}

	/**
	 * 查询 (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 *      java.lang.String[], java.lang.String, java.lang.String[],
	 *      java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] columns, String selection,
			String[] selectionArgs, String orderBy) {
		Cursor cursor = null;
		db = dbHelper.getWritableDatabase();
		String sql = "select * from "+TABLE_NAME;
		cursor = db.rawQuery(sql , null);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case CODE_DIR:
			return "vnd.android.cursor.dir/groupinfo";
		case CODE_ITEM:
			return "vnd.android.cursor.item/groupinfo";

		default:
			throw new IllegalArgumentException("异常参数");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db = dbHelper.getWritableDatabase();

		//插入数据
			switch (mUriMatcher.match(uri)) {
			case 1:
				long c = db.insert(TABLE_NAME, null, values);
				LogUtil.i("tv_launcher","insert success:"+c);
				break;
		}
		// db.execSQL("delete from groupinfo where rowid not in(select max(rowid) from groupinfo group by user_id)");
		return uri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int number = 0;
		db = dbHelper.getWritableDatabase();
		number = db.delete(TABLE_NAME, selection, selectionArgs);
		return number;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int number = 0;
		db = dbHelper.getWritableDatabase();
		number = db.update(TABLE_NAME, values, selection, selectionArgs);
		return number;
	}

}
