package com.company;

public class Token_info extends Object {
	private String name;
	private String Type;
	private String Url;
	private int position;
	
	public Token_info(String name,String Url,String Type,int pos)
	{
		this.name=name;
		this.position=pos;
		this.Type=Type;
		this.Url=Url;
	}
	public String get_token_name()
	{
		return this.name;
	}
	public String get_token_Url()
	{
		return this.Url;
	}
	public String get_token_type()
	{
		return this.Type;
	}
	public int get_token_position()
	{
		return this.position;
	}
}