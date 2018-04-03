package com.company;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mongodb.client.FindIterable;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.io.IOException;
import java.util.List;
//import org.bson.Document;
//import org.bson.conversions.Bson;
import java.util.Set;
import java.util.Vector;



//todo remove 'a','an', spaces ,tabs
public class Indexer implements Runnable {

	private DBController controller = DBController.ContollerInit();

	public String normalizeLink(String url)
	{
		String normalized_url=url.replaceAll("#\\S*","");
		normalized_url=normalized_url.toLowerCase();
		return normalized_url;
	}
	public void run() {
		while (true) {
			while (controller.found_unindexed_pages()) {
				String[] urlfFilename = controller.getUnIndexedPageUrlFilenameAndSet();
				if (urlfFilename == null) {
					continue;
				}
				System.out.println("indexing: " + urlfFilename[0]);
//            System.out.println("indexer starts");
				File input = new File("Pages/" + urlfFilename[1] + ".html");
//            System.out.println("indexer opens file ");
                try {
                    //reading useless words
                    String line;
                    Vector<String> stopping_words = new Vector(2);
                    FileReader file_out = new FileReader("stopwords_en.txt");
                    BufferedReader bf = new BufferedReader(file_out);
                    controller.deleteInvertedFile(urlfFilename[0]);
                    while ((line = bf.readLine()) != null) {
                        stopping_words.add(line);
                    }
                    bf.close();
                    file_out.close();

                    String file_name6 = "tokens4.txt";
                    FileWriter file_in6 = new FileWriter(file_name6);
                    for (int i = 0; i < stopping_words.size(); i++) {
                        file_in6.write(stopping_words.get(i));
                        file_in6.write("\n");
                    }
                    file_in6.close();

                    Document doc = Jsoup.parse(input, "UTF-8", urlfFilename[0]);
                    String body = doc.body().text();
                    String[] tokens = Tokenizer(body);
                    String[] normalized_tokens = Normalizer(tokens);
                    Vector<String> no_space_tokens = remove_spaces(normalized_tokens);
                    Elements headers = doc.select("h1,h2,h3,h4,h5,h6");
                    Vector<String> no_space_headers;
                    String tag;
                    for (Element header : headers) {
                        tag = header.tagName().toLowerCase();
                        String header_name = header.text();
                        String[] header_tokens = Tokenizer(header_name);
                        String[] normalized_headers = Normalizer(header_tokens);
                        no_space_headers = remove_spaces(normalized_headers);
                        no_space_headers = remove_stopping_words(no_space_headers, stopping_words);
                        no_space_headers = remove_stopping_words(no_space_tokens, stopping_words);
                        for (int i = 0; i < no_space_headers.size(); i++) {
                            Token_info temp_token = new Token_info(no_space_headers.get(i), urlfFilename[0], tag, i);
                            controller.AddToInvertedFile(temp_token, "Url_id", "Position", "Type");
                            no_space_tokens.remove(temp_token.get_token_name());
                        }
                    }
                    //printing tokens.
                    String file_name = "tokens.txt";
                    FileWriter file_in = new FileWriter(file_name);
                    for (String normalized_token : normalized_tokens) {
                        file_in.write(normalized_token);
                        file_in.write("\n");
                    }
                    System.out.println("finshed indexing: " + urlfFilename[0]);
                    file_in.close();

					System.out.println("Hello");
					//Set<String> uniqueWords = new HashSet<String>(Arrays.asList(normalized_tokens));
					//String[] unique_Words = uniqueWords.toArray(new String[uniqueWords.size()]);
					for (int i = 0; i < no_space_tokens.size(); i++) {
						Token_info temp_token = new Token_info(no_space_tokens.get(i), urlfFilename[0], "text", i);
						controller.AddToInvertedFile(temp_token, "Url_id", "Position", "Type");
                	/*tokens_info[i]=new Token_info(urlfFilename[0],
                			number_of_occurance(no_space_tokens,no_space_tokens.get(i)));*/
					}
					System.out.println("after add to inverted");
               /* for (Element link : links) {
                    controller.addUrlToFrontier(normalizeLink(link.attr("abs:href")));
                }*/
                /*String file_name3 = "tokens3.txt";
                FileWriter file_in3 = new FileWriter(file_name3);
                for (int i=0;i<tokens_info.length;i++) {
                    file_in3.write(tokens_info[i].get_url());
                    file_in3.write("");
                    file_in3.write(tokens_info[i].get_number_of_occurrances());
                    file_in2.write("\n");
                }
                file_in3.close();*/
				} catch (Exception ex) {
					System.out.println(ex);
				}
			}
		}
	}

	public String[] Tokenizer(String body)
	{
		return body.split("\\s");
	}

	private String[] lower_case(String[] data_in)
	{
		String[] out=new String[data_in.length];
		for(int i=0;i<data_in.length;i++)
			out[i]=data_in[i].toLowerCase();

		return out;

	}
	
	public Vector<String> remove_spaces(String[] data_in)
	{
		Vector<String>temp=new Vector<String>(2);
		for(int i=0;i<data_in.length;i++)
		{
			if(data_in[i].equals(""))
				continue;
			else
				temp.addElement(data_in[i]);
		}
		return temp;
	}

    public Vector<String> remove_stopping_words(Vector<String> v1, Vector<String> v2) {
        v1.removeAll(v2);
        return v1;
    }

	public int number_of_occurance(Vector<String>arr,String token)
	{
		int count=0;
		for(int i=0;i<arr.size();i++)
		{
			if(arr.get(i)==token)
				count++;
		}
		return count;
	}
	
	public String[] Normalizer(String[] data_in)
	{
		String[] lowered_case=lower_case(data_in);
		return Remove_special_characters(lowered_case);
	}
	private String[] Remove_special_characters(String[] data_in)
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


