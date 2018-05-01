package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.UpdateOptions;
import javafx.geometry.Pos;
import opennlp.tools.stemmer.Stemmer;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.Hashtable;

import static com.mongodb.client.model.Updates.push;

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

		//reading useless words
		ArrayList<String> stopping_words = new ArrayList<>();
		try {
			String line;
			stopping_words = new ArrayList<String>(2);
			FileReader file_out = new FileReader("stopwords_en.txt");
			BufferedReader bf = new BufferedReader(file_out);
			while ((line = bf.readLine()) != null) {
				stopping_words.add(line);
			}
			bf.close();
			file_out.close();
		} catch (Exception ex) {
			System.out.println("opening stopping words " + ex);
		}
		while (true) {
			try {
				while (controller.found_unindexed_pages()) {
					String[] urlfFilename = controller.getUnIndexedPageUrlFilenameAndSet();
					if (urlfFilename == null) {
						continue;
					}
					Integer count = 1;
					Hashtable<String, Integer> table = new Hashtable<String, Integer>();
					Hashtable<String, ArrayList<org.bson.Document>> Pos_type_table = new Hashtable<String, ArrayList<org.bson.Document>>();
					System.out.println("indexing: " + urlfFilename[0]);
					File input = new File("Pages/" + urlfFilename[1] + ".html");
					controller.deleteInvertedFile(urlfFilename[0]);
					Document doc = Jsoup.parse(input, "UTF-8", urlfFilename[0]);
					Elements body = doc.body().getAllElements();
					int pos = 0;
					for (Element element : body) {
						if (!element.tagName().equals("script")) {
							String text = element.ownText();
							String[] tokens = Tokenizer(text);
							ArrayList<String> normalized_words = Normalizer(tokens, stopping_words);
							int tag_rank;
							if (!normalized_words.isEmpty()) {
								if (element.tagName().equals("h1") || element.parents().is("h1")) {
									tag_rank = 1;
								} else if (element.tagName().equals("h2") || element.parents().is("h2")) {
									tag_rank = 2;

								} else if (element.tagName().equals("h3") || element.parents().is("h3")) {
									tag_rank = 3;

								} else if (element.tagName().equals("h4") || element.parents().is("h4")) {
									tag_rank = 4;

								} else if (element.tagName().equals("h5") || element.parents().is("h5")) {
									tag_rank = 5;

								} else if (element.tagName().equals("h6") || element.parents().is("h6")) {
									tag_rank = 6;

								} else {
									tag_rank = 7;
								}
								for (String word : normalized_words) {
									pos++;
									org.bson.Document pos_type_doc = new org.bson.Document("Position", pos).append("Tag_rank", tag_rank);
									if (Pos_type_table.containsKey(word)) {
										Pos_type_table.get(word).add(pos_type_doc);

									} else {
										ArrayList<org.bson.Document> temp = new ArrayList<org.bson.Document>();
										temp.add(pos_type_doc);
										Pos_type_table.put(word, temp);
									}
									if (table.containsKey(word)) {
										int value = table.get(word).intValue();
										table.replace(word, value, ++value);
									} else
										table.put(word, count);
								}
							}
						}
					}
					ArrayList<org.bson.Document> words_docs = new ArrayList<>();
					for (String word : Pos_type_table.keySet()
							) {
						ArrayList<org.bson.Document> tokens_arr = Pos_type_table.get(word);
						org.bson.Document link_doc = new org.bson.Document("Url_id", urlfFilename[0]).append("Position_type", tokens_arr).append("TF", table.get(word));
						org.bson.Document modifiedObject = new org.bson.Document();
						modifiedObject.put("$push", new BasicDBObject("token_info", link_doc));
						try {
							controller.Inverted_file.updateOne(new BasicDBObject("_id", word), modifiedObject, new UpdateOptions().upsert(true));
						} catch (Exception ex) {
							System.out.println("error in adding word to inverted file: " + ex);
						}

					}
					controller.AddTOWordFile(urlfFilename[0], table.keySet());
					controller.setIndexed(urlfFilename[0]);
					System.out.println("finished indexing:" + urlfFilename[0]);
				}
			} catch (Exception ex) {
				System.out.println(ex);
			}
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


