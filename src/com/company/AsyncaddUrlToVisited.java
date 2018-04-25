package com.company;

public class AsyncaddUrlToVisited implements Runnable{
	private String link,checksum;
	private DBController controller;
	public AsyncaddUrlToVisited(String link, String checksum,DBController controller) {
		this.link=link;
		this.checksum=checksum;
		this.controller=controller;
	}

	public void run()
	{
		controller.addUrlToVisited(link,checksum);
	}
}
