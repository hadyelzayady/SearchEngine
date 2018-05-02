package com.company;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import org.bson.Document;


public class QProcessor implements Runnable  {

    private DBController controller = DBController.ContollerInit();
    private String[] tokenized;
	private ArrayList<String> normalized;

    String line;
	ArrayList<String> stopping_words = new ArrayList<String>(2);
    FileReader file_out = new FileReader("stopwords_en.txt");
    BufferedReader bf = new BufferedReader(file_out);

    QProcessor(String searchQuery) throws IOException {

        while ((line = bf.readLine()) != null) {
            stopping_words.add(line);
        }
        bf.close();
        file_out.close();


        tokenized = Indexer.Tokenizer(searchQuery);
        normalized = Indexer.Normalizer(tokenized,stopping_words);

    }


    public void run() {
	    ArrayList<String> Urls = new ArrayList<>(2);
	    String out = "test.txt";
//	    try {
//		    FileWriter file_out = new FileWriter(out);
//    	for(int i=0;i<normalized.size();i++)
//    	{
//
	    FindIterable<Document> result = controller.findInInvertedFile(normalized);
//	    FindIterable<Document> result = controller.FindPhraseSearching(normalized);
//	    filterByPosition(result);
	    double total_docs = controller.getTotalDocsCount();
	    Ranker r = new Ranker(result, total_docs);
	    ArrayList<String> sorted_links = r.rank_pages();
	    for (String link : sorted_links) {
		    Object file_name = result.filter(new Document("_id", "author")).first();
		    System.out.println(file_name);
	    }
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

