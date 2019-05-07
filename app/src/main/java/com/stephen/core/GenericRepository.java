package com.stephen.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * modify by Stephen on 7/5/2019.
 */

public abstract class GenericRepository<T extends IEntity> extends SQLiteOpenHelper implements IGenericRepository<T>, Iterable<T> {
	protected SQLiteDatabase db;
	public boolean IsTransaction = false;

	public GenericRepository(Context context) {
		super(context, "data.db3", null, 1);
	}

	protected abstract String getTableName();
	protected abstract ContentValues createEntry(T entity);
	protected abstract T getEntry(Cursor cur);

	public boolean IsOpen() {
		return this.db != null && this.db.isOpen();
	}

	public void Begin() {
		db.beginTransactionNonExclusive();

		IsTransaction = true;
	}

	public void Commit() {
		db.setTransactionSuccessful();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + this.getTableName());

		onCreate(db);
	}

	public void Open() {
		this.db = this.getWritableDatabase();
	}

	public void Close() {
		//close transaction
		if (IsTransaction) {
			this.db.endTransaction();
			IsTransaction = false;
		}

		if (this.db != null)
			this.db.close(); // Closing database connection
	}

	private List<T> getList(Cursor cur)
	{
		List<T> list = new ArrayList<>();

		Iterator<T> iteor = new InnerIterator(cur);

		while (iteor.hasNext()) {
			list.add(iteor.next());
		}

		return list;
	}

	//Iterable interface
	public Iterator<T> iterator() {
		String sql = "SELECT * FROM " + this.getTableName();

		Cursor cur = this.db.rawQuery(sql, null);

		return new InnerIterator(cur);
	}

	private final class InnerIterator implements Iterator<T> {
		private Cursor cursor;
		private int index;

		public InnerIterator(Cursor cur) {
			this.cursor = cur;
			index = -1;
		}

		public boolean hasNext() {
			index++;

			if (index == 0) {
				return this.cursor.moveToFirst();
			}

			if (!this.cursor.moveToNext())
			{
				this.cursor.close();

				return false;
			}

			return true;
		}

		public T next() {
			try {
				T t = getEntry(this.cursor);

				return t;
			}
			catch (Exception e) {

			}

			return null;
		}

		public void remove() {}
	}

    public List<T> ToList(String order) {
        String selectQuery = "SELECT * FROM " + this.getTableName() + " order by " + order;
        Cursor cur = this.db.rawQuery(selectQuery, null);

        return getList(cur);
    }

    public T Get(int id) {
        Cursor cur = db.query(this.getTableName(), new String[] { "*" }, "id = ?",
                new String[] { String.valueOf(id) }, null, null, null, null);

        List<T> list = getList(cur);

        if (list.size() > 0)
            return list.get(0);

        return null;
    }

	public boolean Add(T entity) {
		ContentValues values = this.createEntry(entity);

		// Inserting Row
		return this.db.insert(this.getTableName(), null, values) > 0;
	}

	public boolean Update(T entity) {
		ContentValues values = this.createEntry(entity);

		String where = "id = " + entity.getId();

		return db.update(this.getTableName(), values, where, null) > 0;
	}

	public boolean Delete(T t) {
		return this.db.delete(this.getTableName(), "id = ?",
				new String[] { String.valueOf(t.getId()) }) > 0;
	}

	public void Clear() {
		this.db.delete(this.getTableName(), "id > 0", null);
	}
}
