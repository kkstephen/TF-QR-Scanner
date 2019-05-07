package com.stephen.core;

/**
 * Created by Stephen on 1/9/2017.
 */
import java.util.*;
import java.text.*;
import java.util.UUID;

public class StringHelper {
	public static Date DateNow() {
		Calendar cal = Calendar.getInstance();

		return cal.getTime();
	}

	public static Date GetDate(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

		try {
			Date dt = sdf.parse(str);

			return dt;
		}
		catch (Exception e) {

		}

		return null;
	}

	public static String ToDateTime(Date d) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

		return sdf.format(d);
	}

	public static String Guid()
	{
		UUID uuid = UUID.randomUUID();
		return uuid.toString();
	}
}


