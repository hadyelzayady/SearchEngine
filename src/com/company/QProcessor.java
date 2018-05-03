package com.company;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.jsoup.Jsoup;


public class QProcessor {

    private DBController controller = DBController.ContollerInit();
    private String[] tokenized;
	private ArrayList<String> normalized;
	private int offset;

    String line;
	ArrayList<String> stopping_words = new ArrayList<String>(2);
    FileReader file_out = new FileReader("stopwords_en.txt");
    BufferedReader bf = new BufferedReader(file_out);

	QProcessor() throws IOException {

        while ((line = bf.readLine()) != null) {
            stopping_words.add(line);
        }
        bf.close();
        file_out.close();


    }

    public int min(int num1,int num2)
    {
    	if(num1<num2)
    		return num1;
    	return num2;
    }


	public QueryResult getQueryResult(String searchQuery, int offset) {
	    ArrayList<String> Urls = new ArrayList<>(2);
		this.offset = offset;
		tokenized = Indexer.Tokenizer(searchQuery);
		normalized = Indexer.Normalizer(tokenized, stopping_words);
//	    try {
//		    FileWriter file_out = new FileWriter(out);
//    	for(int i=0;i<normalized.size();i++)
//    	{
//
	    AggregateIterable<Document> result = controller.findInInvertedFile(normalized);
//	    FindIterable<Document> result = controller.FindPhraseSearching(normalized);
//	    filterByPosition(result);
	    double total_docs = controller.getTotalDocsCount();
	    Ranker r = new Ranker(result, total_docs);
		UrlFilename url_filename = new UrlFilename();
		ArrayList<String> sorted_links = r.rank_pages(url_filename);
		ArrayList<String> snippets = new ArrayList<>();
	    if (this.offset >= sorted_links.size()) {
		    this.offset = 0;
	    }
	    int min_size = min((this.offset + 9), sorted_links.size());
	    sorted_links = new ArrayList<>(sorted_links.subList(this.offset, min_size));
		for (String url : sorted_links) {
			File input = new File("Pages/" + url_filename.url_filename.get(url) + ".html");
			try {
//			    Pattern search_regex = Pattern.compile(normalized);
				org.jsoup.nodes.Document doc = Jsoup.parse(input, "UTF-8", url);
				String text = doc.text();
				String snippet = "";
				for (String word : normalized) {
					int index = doc.text().indexOf(word);
					if (index != -1) {
						int start = index - 50;
						int end = index + 50;
						if (index < 50)
							start = 0;
						if (end > text.length())
							end = text.length();
						snippet += text.substring(start, end) + ";\n";
					}
//				    System.out.println(snippet);
				}
				snippets.add(snippet);
			} catch (Exception e) {
				System.out.println("Error Qprocessor snippets for loop: " + e);
			}
		}
		return new QueryResult(snippets, sorted_links);
//	   for(int i=(this.offset*10);i<=min_size;i++) {
//		    System.out.println(file_name);
//	    }
//	    result.filter("")

//	    ArrayList
//	    for (Document doc : result) {
//		    System.out.println(doc);
//		    ArrayList<Document> docs = (ArrayList<Document>) doc.get("token_info");
//		    for (Document doc2 : docs) {
//			    urls.add(doc2.getString("Url_id"));
//		    }
//	    }
//	    double[] h = new Ranker(urls, normalized).get_ranks();
//	    for (int i = 0; i < h.length; i++) {
//		    System.out.println(h[i] + "  :" + urls.get(i));
//	    }
//		    ArrayList<Document> ds = (ArrayList<Document>) result;
//		    for (Document doc : ds) {
//			    System.out.println(doc.get("Url_id"));
//		    }
//		    new Ranker()
//    	for (int i=0;i<Urls.size();i++)
//    	{
//    		file_out.write(Urls.get(i));
//    	}
//    	file_out.close();
//    	}
//    	 catch (IOException e) {
// 			// TODO Auto-generated catch block
// 			e.printStackTrace();
// 		}
        /*Document result1 = controller.findInInvertedFile(normalized.get(0));
        controller.queryResult_collection.insertOne(result1);*/
//    }
    }


	private void filterByPosition(FindIterable<Document> result) {
		for (Document doc : result) {
			System.out.println(doc);
		}
	}
}

