package com.company;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		// write your code here
		WebCrawler web = new WebCrawler();
		try {
//            System.out.println(web.calcChecksum("SHA-256 Hashing in Java"));
//            String temp="www.GOOGLe.com#row=0";
//			String normalizedlink=web.normalizeLink(temp);
//			System.out.println(normalizedlink);
			DBController cont = DBController.ContollerInit();
			web.startCrawler();
//			List<String> links = web.getInnerLinks("https://www.wikipedia.org/");
//			for (String link : links) {
//				web.normalizeLink(link);
//			}
		}// catch (IOException ex) {
//			System.err.println(ex);
//		}
		catch (Exception ex)
        {
            System.err.println(ex);
        }
	}
}
