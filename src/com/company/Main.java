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

	public static void main(String[] args) throws IOException {

		Scanner scanner = new Scanner(System.in);
		System.out.print("number of Threads: \n");
		int num_of_threads = scanner.nextInt();
        //System.out.print("Search: ");
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
