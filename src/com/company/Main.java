package com.company;
import java.net.URL;
import java.io.*;
import java.io.IOException;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class Main {

	public static void main(String[] args) {
		// write your code here
//		WebCrawler web = new WebCrawler();
		try {
//            System.out.println(web.calcChecksum("SHA-256 Hashing in Java"));
//            String temp="www.GOOGLe.com#row=0";
//			String normalizedlink=web.normalizeLink(temp);
//			System.out.println(normalizedlink);
//			DBController cont = DBController.ContollerInit();
//			web.startCrawler();
			URL obj = new URL("http://mkyong.com");
			URLConnection conn = obj.openConnection();
			Map<String, List<String>> map = conn.getHeaderFields();
			System.out.println(map.get("Content-Type").get(0).contains("text/html"));
			System.out.println(map);
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
