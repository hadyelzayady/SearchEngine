package com.company;

import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;

public class token_rank {
	private String url;
	private String token;
	private DBController controller = DBController.ContollerInit();
	private ArrayList<Document> Url_tokens;
	
	public token_rank(String url,String token)
	{
		this.url=url;
		this.token=token;
		this.Url_tokens=controller.get_tokens();
	}
	
	public double calculate_TF()
	{
		double count=0;
		Document temp_doc=new Document("_id",this.url);
		int index=this.Url_tokens.indexOf(temp_doc);
		System.out.println(index);
		Document doc =this.Url_tokens.get(index);
		ArrayList<String> words=(ArrayList<String>)doc.get("words");
		for(int i=0;i<words.size();i++)
		{
			if(words.get(i)==this.token)
				count++;
		}
		return (count/(double)words.size());
	}
	
	public double  calculate_IDF()
	{
		double count=this.Url_tokens.size();
		double token_count=controller.get_token_cout(this.token);
		double temp=Math.log10((count/token_count));
		return temp;
	}
}
