package com.company;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.Arrays;
import java.io.IOException;
import java.util.List;

import com.mongodb.*;

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
			String[] urlfFilename = controller.getUnIndexedPageUrlFilenameAndSet();
            if (urlfFilename == null) {
                continue;
            }
			System.out.println(urlfFilename);
            System.out.println("indexer starts");
            File input = new File(urlfFilename[1] + ".html");
            System.out.println("indexer opens file ");
            try {
                Document doc = Jsoup.parse(input, "UTF-8", urlfFilename[0]);
                Elements links = doc.select("a[href]");

                String body = doc.body().text();
                String[] tokens = Tokenizer(body);
                String[] normalized_tokens = Normalizer(tokens);
                //printing tokens.
                String file_name = "tokens.txt";
                FileWriter file_in = new FileWriter(file_name);
                for (String normalized_token : normalized_tokens) {
                    file_in.write(normalized_token);
                    file_in.write("\n");
                }
                file_in.close();
                for (Element link : links) {
                    controller.addUrlToSeed(normalizeLink(link.attr("abs:href")));
                }
            } catch (Exception ex) {
                System.out.println(ex);
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


	public String[] Normalizer(String[] data_in)
	{
		String[] lowered_case=lower_case(data_in);
		return 	Remove_special_characters(lowered_case);
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


