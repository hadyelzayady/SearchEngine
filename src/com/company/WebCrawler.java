package com.company;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WebCrawler {
	Queue<String> pending_links;

	public List<String> getInnerLinks(String url) throws IOException {
		Scanner content = new Scanner((new URL(url)).openStream());
		String pat = "(?i)(href)(\\s*)=\\s*(.+?)>"; //(?i)(<\s*a)(.+?)(href)(\s*)=\s*(.+?)> to look only in <a tags
		return content.findAll(pat)
				.map(MatchResult::group)
				.collect(Collectors.toList());
	}
}
