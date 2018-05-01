package com.company;

import opennlp.tools.stemmer.PorterStemmer;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	public static void main(String[] args) throws IOException, URISyntaxException {

		Scanner scanner = new Scanner(System.in);
//		System.out.print("number of Threads: \n");
//		int num_of_threads = scanner.nextInt();
//		Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
//		String url="https://docs.oracle.com/javase/7/docs/api/j ava/net/URI.html?q=h%20ebody";
//		Matcher matcher = pattern.matcher(url);
//		matcher.find();
//		String host=matcher.group(2);
//		String port     = matcher.group(3);
//		String protocol = matcher.group(1);
//		String path=matcher.group(4);
//		String uri = matcher.group(4);
//		String x=protocol+port+host+path;
//		port.isEmpty();
//		        System.out.print("Search: ");
//		String encoded_url=URLEncoder.encode(url, "UTF-8");
//		URI uri=new URI(url);
//		URI path= uri.relativize(new URI(uri.getPath()));
//		String uri=new URI(url).getHost();
//		String decoded_url=URLDecoder.decode(encoded_url, "UTF-8");
//		System.out.println(path);
//        String input = scanner.nextLine();
//        try {
//            new Thread(new QProcessor(input)).start();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//		new Thread(new CrawlerMain(num_of_threads)).start();
//		new Thread(new Indexer()).start();
		new Thread(new QProcessor("top english")).start();
	}
}
