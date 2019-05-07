package com.stephen.core;

import java.util.Date;
/**
 * Created by Stephen on 14/9/2017.
 */

public class TagLabel implements IEntity {
	private int id;
	private String code;
	private Date createdate;

	public TagLabel(String code, Date dt) {
		this.setCode(code);
		this.setCreatedate(dt);
	}

	public void setId(int id) { this.id = id; }
	public int getId() { return this.id; }

	public void setCode(String code) { this.code = code; }
	public String getCode() { return this.code; }

	public void setCreatedate(Date dt) { this.createdate = dt; }
	public Date getCreatedate() { return this.createdate; }
}
