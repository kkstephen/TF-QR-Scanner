package com.stephen.core;

import java.util.List;
import java.text.ParseException;

/**
 * Created by Stephen on 14/9/2017.
 */

public interface IGenericRepository<T> {
	T Get(int id);
	boolean Add(T t);
	boolean Update(T t);
	boolean Delete(T t);

	List<T> ToList(String order);
}
