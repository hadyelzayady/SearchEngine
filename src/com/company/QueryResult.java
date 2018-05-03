package com.company;

import java.util.ArrayList;

public class QueryResult {
	ArrayList<String> snippets;
	ArrayList<String> links;

	QueryResult(ArrayList<String> snippets, ArrayList<String> links) {
		this.links = links;
		this.snippets = snippets;
	}
}
