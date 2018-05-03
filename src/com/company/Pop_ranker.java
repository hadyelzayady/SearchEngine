package com.company;

public class Pop_ranker implements Runnable {
	private DBController controller;

	public Pop_ranker() {
		controller = DBController.ContollerInit();
	}

	public void run() {
		while (true) {
			controller.popularityCacl();
		}
	}
}
