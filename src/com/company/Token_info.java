package com.company;

public class Token_info extends Object {
private String Url;
private int number_of_occurances;
public Token_info(String Url,int number_of_occurances)
{
	this.Url=Url;
	this.number_of_occurances=number_of_occurances;
}
public String get_url()
{
	return this.Url;
}
public int get_number_of_occurrances()
{
	return this.number_of_occurances;
}
	
}
