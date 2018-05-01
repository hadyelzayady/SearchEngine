package com.company;

import java.util.ArrayList;

import org.bson.Document;
import org.bson.conversions.Bson;

import static java.lang.Math.log;

public class token_rank {
	private int TF;
	private int IDF;
	private String url;
	private String token;
	private DBController controller = DBController.ContollerInit();

	public token_rank(String url, String token) {
		this.url = url;
		this.token = token;
	}

	public double calculate_TF() {
		int count = 0;
//		Document doc =controller.get_url(this.url);
//		if(doc!=null) {
//			ArrayList<String> words = (ArrayList<String>) doc.get("words");
//			for (int i = 0; i < words.size(); i++) {
//				if (words.get(i).equals(this.token))
//					count++;
//			}
//			double tem = count / (double) words.size();
//			return (tem);
//		}
		return 0;
//	}

//	public double  calculate_IDF()
//	{
//		double count=controller.get_url_cout();
//		double token_count=controller.get_token_cout(this.token);
//		return log(count/token_count);
//	}
	}
}
