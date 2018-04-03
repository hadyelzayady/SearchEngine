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
		System.out.print("number of Threads: ");
		int num_of_threads = scanner.nextInt();
		new Thread(new CrawlerMain(num_of_threads)).start();
		new Thread(new Indexer()).start();
	}
}
