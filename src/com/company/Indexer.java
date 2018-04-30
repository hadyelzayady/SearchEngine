package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import javafx.geometry.Pos;
import opennlp.tools.stemmer.Stemmer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.Hashtable;
//todo threads
//todo stemming
//todo new collection  for stopping words
//todo remove headers from body
//todo inverted file part 2
public class Indexer implements Runnable {
	private DBController controller = DBController.ContollerInit();
	public String normalizeLink(String url)
	{
		String normalized_url=url.replaceAll("#\\S*","");
		normalized_url=normalized_url.toLowerCase();
		return normalized_url;
	}
	public void run() {
		try
		{
			//reading useless words
            String line;
			ArrayList<String> stopping_words = new ArrayList<String>(2);
            FileReader file_out = new FileReader("stopwords_en.txt");
            BufferedReader bf = new BufferedReader(file_out);
			  while ((line = bf.readLine()) != null) {
                stopping_words.add(line);
            }
            bf.close();
            file_out.close();
            Integer count=1;
            while(true)
            {
	            while (controller.found_unindexed_pages()) {
		            String[] urlfFilename = controller.getUnIndexedPageUrlFilenameAndSet();
		            if (urlfFilename == null) {
			            continue;
		            }
		            Hashtable<String, Integer> table = new Hashtable<String, Integer>();
		            Hashtable<String, ArrayList<Pos_type>> Pos_type_table = new Hashtable<String, ArrayList<Pos_type>>();
		            System.out.println("indexing: " + urlfFilename[0]);
		            File input = new File("Pages/" + urlfFilename[1] + ".html");
//                        controller.deleteInvertedFile(urlfFilename[0]);
		            Document doc = Jsoup.parse(input, "UTF-8", urlfFilename[0]);
		            Elements body = doc.body().getAllElements();
		            for (Element element : body) {
			            String text = element.ownText();
			            String[] tokens = Tokenizer(text);
			            ArrayList<String> normalized_words = Normalizer(tokens, stopping_words);
			            int tag_rank;
			            int pos = 0;
			            if (!normalized_words.isEmpty()) {

				            if (element.tagName().equals("h1")) {
					            tag_rank = 1;
				            } else if (element.tagName().equals("h2")) {
					            tag_rank = 2;

				            } else if (element.tagName().equals("h3")) {
					            tag_rank = 3;

				            } else if (element.tagName().equals("h4")) {
					            tag_rank = 4;

				            } else if (element.tagName().equals("h5")) {
					            tag_rank = 5;

				            } else if (element.tagName().equals("h6")) {
					            tag_rank = 6;

				            } else {
					            tag_rank = 7;
				            }
				            for (String word : normalized_words) {
					            pos++;
					            Pos_type word_pos_type = new Pos_type(pos, tag_rank);
					            if (table.containsKey(word)) {
						            Pos_type_table.get(word).add(word_pos_type);
					            } else {
						            ArrayList<Pos_type> temp = new ArrayList<Pos_type>();
						            temp.add(word_pos_type);
						            Pos_type_table.put(word, temp);
					            }
//					            org.bson.Document word_doc = new org.bson.Document("Url_id",urlfFilename[0]).append("Positions",pos);
//					            DBObject modifiedObject =new BasicDBObject();
//					            modifiedObject.put("$push", new BasicDBObject().append("token_info", word_doc));
					            if (table.containsKey(word)) {
						            int value = table.get(word).intValue();
						            table.replace(word, value, ++value);
					            } else
						            table.put(word, count);
				            }
			            }

		            }
		            String[] tokens = null;
		            ArrayList<String> no_space_tokens = Normalizer(tokens, stopping_words);
                        controller.AddTOWordFile( urlfFilename[0], no_space_tokens);
                        for(int i=0;i<no_space_tokens.size();i++)
                        {
                        	String temp_key=no_space_tokens.get(i);
                        	if(table.containsKey(temp_key))
                        	{
                        		int value=table.get(temp_key).intValue();

		                        table.replace(temp_key, value, ++value);
                        	}
                        	else
                        		table.put(temp_key, count);
                        }
                        //controller.AddTOWordFile( urlfFilename[0], no_space_tokens);
                        System.out.println("passed Inverted file2");
                        Elements headers = doc.select("h1,h2,h3,h4,h5,h6");
		            ArrayList<String> no_space_headers;
                        String tag;
                        for (Element header : headers) {
                            tag = header.tagName().toLowerCase();
                            String header_name = header.text();
                            String[] header_tokens = Tokenizer(header_name);
                            no_space_headers = Normalizer(header_tokens,stopping_words);
                            for (int i = 0; i < no_space_headers.size(); i++) {
                                Token_info temp_token = new Token_info(no_space_headers.get(i),
                                		urlfFilename[0], tag, i);
                                controller.AddToInvertedFile(temp_token, "Url_id", "Position", "Type");
                                no_space_tokens.remove(temp_token.get_token_name());
                            }
                        }
		            // System.out.println("finshed indexing: " + urlfFilename[0]);
    					for (int i = 0; i < no_space_tokens.size(); i++) {
    						Token_info temp_token = new Token_info(no_space_tokens.get(i), urlfFilename[0], "text", i);
    						controller.AddToInvertedFile(temp_token, "Url_id", "Position", "Type");
    					}
		            controller.setIndexed(urlfFilename[0]);
		            //System.out.println("after add to inverted");
            	}
            }
		}
		catch (Exception ex)
		{
			System.out.println(ex);
		}	
	}

	public static String[] Tokenizer(String body)
	{
		return body.split("\\s");
	}

	private static String[] lower_case(String[] data_in)
	{
		String[] out=new String[data_in.length];
		for(int i=0;i<data_in.length;i++)
			out[i]=data_in[i].toLowerCase();

		return out;
	}

	private static ArrayList<String> remove_spaces(String[] data_in) {
		ArrayList<String> temp = new ArrayList<>(2);
		for(int i=0;i<data_in.length;i++)
		{
			if(data_in[i].equals(""))
				continue;
			else
				temp.add(data_in[i]);
		}
		return temp;
	}

	private static ArrayList<String> remove_stopping_words(ArrayList<String> v1, ArrayList<String> v2) {
        v1.removeAll(v2);
        return v1;
    }

	public static int number_of_occurance(Vector<String>arr,String token)
	{
		int count=0;
		for(int i=0;i<arr.size();i++)
		{
			if(arr.get(i)==token)
				count++;
		}
		return count;
	}

	public static ArrayList<String> Normalizer(String[] data_in, ArrayList<String> stopping_vector)
	{
		String[] lowered_case=lower_case(data_in);
		String[] NO_special_char=Remove_special_characters(lowered_case);
		ArrayList<String> removed_spaces = remove_spaces(NO_special_char);
		return remove_stopping_words(removed_spaces,stopping_vector);
	}
	private static String[] Remove_special_characters(String[] data_in)
	{
		String[] out=new String[data_in.length];
		for(int i=0;i<data_in.length;i++)
		{
			StringBuilder str=new StringBuilder(data_in[i]);
			for(int j=0;j<data_in[i].length();j++)
			{
				if(!((data_in[i].charAt(j) - 'A' >= 0 && data_in[i].charAt(j) - 'A' < 26)||(data_in[i].charAt(j) - 'A' >= 32 && data_in[i].charAt(j) - 'A' < 58)))
					str.deleteCharAt(j-(data_in[i].length()-str.length()));
			}
			out[i]=str.toString();
		}
		return out;
	}
}


