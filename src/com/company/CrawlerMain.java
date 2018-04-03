package com.company;

public class CrawlerMain implements Runnable {
    int num_of_threads;

    CrawlerMain(int num_of_threads) {
        this.num_of_threads = num_of_threads;
    }

    public void run() {
        WebCrawler web_crawler = new WebCrawler();
        while (true) {
            try {
                Thread[] crawler_threads = new Thread[num_of_threads];
                for (int i = 0; i < num_of_threads; ++i) {
                    crawler_threads[i] = new Thread(web_crawler);
                    crawler_threads[i].setName("thread" + i);
                    crawler_threads[i].start();
                }
                for (int i = 0; i < num_of_threads; ++i) {
                    crawler_threads[i].join();
                }
                System.out.println("recrawl");
                web_crawler.recrawlreset();
            } catch (Exception ex) {
                System.out.println(ex);
            }
        }
    }

}
