package com.company;

import org.bson.*;

import java.util.ArrayList;

public class Ranker {
	private ArrayList<String>popular_urls;
	private ArrayList<String>urls_comming;
	private long[] ranked_urls;
	public Ranker(ArrayList<String> urls_comming,String[] query)
	{
		this.urls_comming=urls_comming;
		this.ranked_urls=rank_pages(query);
		//this.popular_urls=Popular_pages(ranked_urls);
	}
	private long[] rank_pages(String[] query)
	{
		long[] ranks=new long[this.urls_comming.size()];
		long [] tokens_rank=new long[query.length];
		for(int i=0;i<this.urls_comming.size();i++)
		{
			long sum=0;
			for(int j=0;j<query.length;j++)
			{
				token_rank temp=new token_rank(this.urls_comming.get(i),query[j]);
				long TFi= temp.calculate_TF();
				long IDFi= temp.calculate_IDF();
				sum+=(TFi*IDFi);
			}
			ranks[i]=sum;
		}
		return ranks;
	}
	/*private ArrayList<String>Popular_pages(long[] ranks_input)
	{
		ArrayList<String> urls=new ArrayList<String>();
		return urls_input;
	}*/
	public ArrayList<String> urls()
	{
		return this.popular_urls;
	}
	
	public long[] get_ranks()
	{
		return this.ranked_urls;
	}
	
}
