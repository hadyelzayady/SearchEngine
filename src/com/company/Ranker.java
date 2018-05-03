package com.company;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
import org.bson.*;

import javax.print.Doc;
import java.util.*;

import static java.lang.Math.log;

public class Ranker {
	private ArrayList<Document> popular_urls;
	private AggregateIterable<Document> words_urls;
	private ArrayList<Document> ranked_urls;
	double total_docs;

	public Ranker(AggregateIterable<Document> urls_comming, double total_docs) {
		this.words_urls = urls_comming;
		this.total_docs = total_docs;
		//this.popular_urls=Popular_pages(ranked_urls);
	}

	public ArrayList<String> rank_pages() {
		Hashtable<String, Double> url_rank_table = new Hashtable<String, Double>();
		for (Document word_url : words_urls) {
			ArrayList<Document> word_urls = (ArrayList<Document>) word_url.get("token_info");
			ArrayList<Document> pop_ranks = (ArrayList<Document>) word_url.get("page_rank");
			double IDF = log(total_docs / (double) word_urls.size());
			for (Document link_doc : word_urls) {
				String url = link_doc.getString("Url_id");
				double TF = link_doc.getDouble("NormalizedTF");
				double pop_rank = getRank(url, pop_ranks);
				double tag_rank = link_doc.getInteger("Max_rank");
				double rank = IDF * TF * tag_rank * pop_rank;
				if (url_rank_table.containsKey(url)) {
					url_rank_table.put(url, url_rank_table.get(url) + rank);
				} else {
					url_rank_table.put(url, rank);
				}
			}
		}
		ArrayList<String> l = sortByValues(url_rank_table);
		return l;
	}

	private double getRank(String url, ArrayList<Document> pop_ranks) {
		for (Document doc : pop_ranks) {
			if (doc.getString("_id").equals(url)) {
				return doc.getDouble("rank");
			}
		}
		return 0;
	}

	private static ArrayList<String> sortByValues(Hashtable map) {
		List list = new LinkedList(map.entrySet());
		// Defined Custom Comparator here
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o2)).getValue())
						.compareTo(((Map.Entry) (o1)).getValue());
			}
		});
		// Here I am copying the sorted list in HashMap
		// using LinkedHashMap to preserve the insertion order
		HashMap sortedHashMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		ArrayList<String> h = new ArrayList<String>(sortedHashMap.keySet());
		return h;
	}
	/*private ArrayList<String>Popular_pages(long[] ranks_input)
	{
		ArrayList<String> urls=new ArrayList<String>();
		return urls_input;
	}*/
//	public ArrayList<String> urls()
//	{
//		return this.popular_urls;
//	}
//
//	public double[] get_ranks()
//	{
//		return this.ranked_urls;
//	}

}
