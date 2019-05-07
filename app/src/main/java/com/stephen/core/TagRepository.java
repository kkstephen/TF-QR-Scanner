package com.stephen.core;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.Tag;

import org.w3c.dom.ls.LSInput;

/**
 * Created by Stephen on 14/9/2017.
 */

public class TagRepository extends GenericRepository<TagLabel> {

	private String _name = "QRData";

	public TagRepository(Context context) {
		super(context);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE IF NOT EXISTS " + this.getTableName() +
				" (id integer primary key autoincrement," +
				" code nvarchar(512)," +
				" createdate DATETIME);"
		);
	}

	protected ContentValues createEntry(TagLabel entity) {
		ContentValues values = new ContentValues();

		values.put("code", entity.getCode());
		values.put("createdate", StringHelper.ToDateTime(entity.getCreatedate()));

		return values;
	}

	protected TagLabel getEntry(Cursor cur) {
		int id = cur.getInt(0);
		String code = cur.getString(1);
		String dt = cur.getString(2);

		Date date = StringHelper.GetDate(dt);
		TagLabel entity = new TagLabel(code, date);

		entity.setId(id);

		return entity;
	}

	public String getTableName() {
		return this._name;
	}
}
