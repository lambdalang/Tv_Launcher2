package com.vunke.tl.service;

import com.vunke.tl.util.LogUtil;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class GroupStrategyProvide extends ContentProvider {
	private final static String AUTHORITH = "com.vunke.tvlauncher.provider2";
	private final static String PATH = "/group_strategy";
	private final static String PATHS = "/group_strategy/#";

	private final static String TABLE_NAME = "group_strategy";

	private final static UriMatcher mUriMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	private static final int CODE_DIR = 1;
	private static final int CODE_ITEM = 2;
	static {
		mUriMatcher.addURI(AUTHORITH, PATH, CODE_DIR);
		mUriMatcher.addURI(AUTHORITH, PATHS, CODE_ITEM);
	}
	private GroupStrategySql dbHelper;
	private SQLiteDatabase db;
//	private static final String EPG_CODE = "epg_code";
//	private static final String EPG_PACKAGE = "epg_package";
//	private static final String GROUP_ADDRESS = "group_address";
//	private static final String GROUP_NAME = "group_name";
//	private static final String GROUP_STATUS = "group_status";
//	private static final String GROUP_TYPE = "group_type";
//	private static final String GROUP_NUMBER = "group_number";
//	private static final String _ID = "_id";
	private static final String CREATE_TIME = "create_time";
	private static final String USER_ID = "user_id";

	@Override
	public boolean onCreate() {
		dbHelper = new GroupStrategySql(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor cursor = null;
		db = dbHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
		case 1:
			String sql;
			if (selectionArgs != null && selectionArgs.length != 0) {
				sql = "select * from " + TABLE_NAME + " where " + USER_ID
						+ "='" + selectionArgs[0] + "'";
			} else {
				sql = "select * from " + TABLE_NAME;
			}
			sql = sql + " order by "+CREATE_TIME+" desc";
			LogUtil.i("tv_launcher", "sql:"+sql);
			cursor = db.rawQuery(sql, null);
			/*
			 * cursor = db.query(TABLE_NAME, null, sql, null, null, null,
			 * "create_time des ");
			 */
			break;
		default:

			break;
		}

		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		switch (mUriMatcher.match(uri)) {
		case CODE_DIR:
			return "vnd.android.cursor.dir/" + TABLE_NAME;
		case CODE_ITEM:
			return "vnd.android.cursor.item/" + TABLE_NAME;

		default:
			throw new IllegalArgumentException("异常参数");
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db = dbHelper.getWritableDatabase();
		Cursor cursor = db.rawQuery("select "+USER_ID+" from " + TABLE_NAME
				+ " where "+USER_ID+" = '" + values.get(USER_ID) + "'", null);
		// db.query(true, TABLE_NAME, new String[] {
		// "body", "user_id", "create_time" }, null, null, "user_id", null,
		// null, null, null);
		if (cursor.moveToNext()) {// 有下一个 ，更新
			String userID = values.get(USER_ID).toString();
			db.update(TABLE_NAME, values, USER_ID+"=?", new String[] { userID });
		} else {// 否则 插入数据
			switch (mUriMatcher.match(uri)) {
			case 1:
				db.insert(TABLE_NAME, null, values);
				break;

			}
		}
		// db.execSQL("delete from groupinfo where rowid not in(select max(rowid) from groupinfo group by user_id)");
		return uri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int number = 0;
		db = dbHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
		case 1:
			number = db.delete(TABLE_NAME, selection, selectionArgs);
		case 2:
			long id = ContentUris.parseId(uri);
			selection = (selection != null || "".equals(selection.trim()) ? USER_ID
					+ "=" + id
					: selection + "and" + USER_ID + "=" + id);
			number = db.delete(TABLE_NAME, selection, selectionArgs);
		}
		return number;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int number = 0;
		db = dbHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
		case 1:
			number = db.update(TABLE_NAME, values, selection, selectionArgs);
			break;
		case 2:
			long id = ContentUris.parseId(uri);
			selection = (selection != null || "".equals(selection.trim()) ? USER_ID
					+ "=" + id
					: selection + "and" + USER_ID + "=" + id);
			number = db.update(TABLE_NAME, values, selection, selectionArgs);
		}
		return number;
	}

}
