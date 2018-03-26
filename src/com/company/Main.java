package com.company;
import java.net.URL;
import java.io.*;
import java.io.IOException;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		// write your code here
		Scanner scanner = new Scanner(System.in);
		System.out.print("number of rows: ");
		int num_of_threads = scanner.nextInt();
		Thread[] crawler_threads = new Thread[num_of_threads];
		WebCrawler web_crawler = new WebCrawler();
		for (int i = 0; i < num_of_threads; ++i) {
			crawler_threads[i] = new Thread(web_crawler);
			crawler_threads[i].start();
		}
		new Thread(new Indexer()).start();
	}
}
