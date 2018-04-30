package com.company;

import com.mongodb.client.MongoCursor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
    	Vector<String>Urls=new Vector<String>(2);
    	String out="test.txt";
    	try {
			FileWriter file_out=new FileWriter(out);
    	for(int i=0;i<normalized.size();i++)
    	{
    		Document result= controller.findInInvertedFile(normalized.get(i));
    		String Url=result.get("Url_id").toString();
    		Urls.addElement(Url);
    	}
    	for (int i=0;i<Urls.size();i++)
    	{
    		file_out.write(Urls.get(i));
    	}
    	file_out.close();
    	}
    	 catch (IOException e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}
        /*Document result1 = controller.findInInvertedFile(normalized.get(0));
        controller.queryResult_collection.insertOne(result1);*/
    }
}

