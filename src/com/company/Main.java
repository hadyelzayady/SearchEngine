package com.company;

import java.io.IOException;
import java.util.List;

public class Main {

	public static void main(String[] args) {
	// write your code here
		WebCrawler web = new WebCrawler();
		try {
			List<String> links = web.getInnerLinks("https://www.wikipedia.org/");
			for (String link : links) {
				System.out.println(link);
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
    }
}
