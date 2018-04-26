package com.company;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) throws IOException, URISyntaxException {

		Scanner scanner = new Scanner(System.in);
		System.out.print("number of Threads: \n");
		int num_of_threads = scanner.nextInt();
//        System.out.print("Search: ");
//		String url="https://docs.oracle.com:443/javase/7/docs/api/java/net/URI.html?q=hebody";
//		String encoded_url=URLEncoder.encode(url, "UTF-8");
//		URI uri=new URI(url);
//		URI path= uri.relativize(new URI(uri.getPath()));
//		String uri=new URI(url).getHost();
//		String decoded_url=URLDecoder.decode(encoded_url, "UTF-8");
//		System.out.println(path);
        //String input = scanner.nextLine();
        /*try {
            new Thread(new QProcessor(input)).start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        new Thread(new CrawlerMain(num_of_threads)).start();
        new Thread(new Indexer()).start();
	}
}
