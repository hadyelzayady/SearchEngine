package com.company;

import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;

public class token_rank {
	private int TF;
	private int IDF;
	private String url;
	private String token;
	private DBController controller = DBController.ContollerInit();
	
	public token_rank(String url,String token)
	{
		this.url=url;
		this.token=token;
	}
	
	public long calculate_TF()
	{
		int count=0;
		Document doc =controller.get_url(this.url);
		ArrayList<String> words=(ArrayList<String>)doc.get("words");
		for(int i=0;i<words.size();i++)
		{
			if(words.get(i)==token)
				count++;
		}
		return (count/words.size());
	}
	
	public long  calculate_IDF()
	{
		long count=controller.get_url_cout();
		long token_count=controller.get_token_cout(this.token);
		return (count/token_count);
	}
}
