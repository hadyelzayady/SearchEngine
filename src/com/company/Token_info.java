package com.company;

public class Token_info extends Object {
private int Url_id;
private int position;
public Token_info(int Url_id,int position)
{
	this.Url_id=Url_id;
	this.position=position;
}
public int get_id()
{
	return this.Url_id;
}
public int position()
{
	return this.position;
}
	
}
