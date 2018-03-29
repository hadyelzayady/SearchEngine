package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;

//todo robot.txt
//todo restart crawler after finishing
//todo resume after interrupt
//todo visit frequency of specific pages
public class WebCrawler implements Runnable {

    DBController controller;
    static final AtomicInteger number_crawled = new AtomicInteger(0);

    public WebCrawler() {
        controller = DBController.ContollerInit();
        if (controller.getCrawledCount() == 0 || controller.getCrawledCount() == 5000) {
            controller.resetFrontier();
            controller.resetVisited();
            //todo make Visited in Frontier to null so we can use it ,null--> unvisited and not under work,false--> somethread works on it,true --> visited
        }
    }
    public void run() {
        String link = controller.getLinkFromFrontierAndSetOnwork();
        while (number_crawled.getAndAdd(1) != 5000 && link !=null)// && link!=null
        {
            //todo use ispagehtml to get html pages only
            try {
                if (isPageAllowedToCrawl(link)) {
                    System.out.println(number_crawled);
//                    BufferedReader page_buffer = downloadPage(link);
//                    String page_content = getString(page_buffer);
                    Document page = Jsoup.connect(link).get();
                    String page_content = page.outerHtml();
                    String checksum = toHexString(calcChecksum(page_content));
                    System.out.println(Thread.currentThread().getName());
                    if (!isPageDownloadedBefore(checksum)) {
                        savePageInFile(checksum, page_content);//todo we should limit added links to 5000 as we won't parse them
                        addLinksToFrontier(page);
                        addUrlToVisited(link, checksum);
                        controller.setUrlVisited(link);
                        System.out.println("finished crawling " + link);
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
            link = controller.getLinkFromFrontierAndSetOnwork();
        }

    }

    private void addLinksToFrontier(Document page) {
        Elements links = page.select("a[href]");
        for (Element link : links) {//todo try to use insertmany insteadof insert one by one
            controller.addUrlToFrontier(normalizeLink(link.attr("abs:href")));
        }
    }

    private String getString(BufferedReader page_buffer) {
        String line = null;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = page_buffer.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean doesLinkExistInSeed(String link)
    {
        return false;
    }

    public String normalizeLink(String url)
    {
        String normalized_url=url.replaceAll("#\\S*","");
        normalized_url=normalized_url.toLowerCase();
        return normalized_url;
    }

<<<<<<< HEAD
//    public List<String> getInnerLinks(Scanner content){
//        String pat = "(?i)(href)(\\s*)=\\s*(.+?)>"; //(?i)(<\s*a)(.+?)(href)(\s*)=\s*(.+?)> to look only in <a tags
//        return content.findAll(pat).map(MatchResult::group).collect(Collectors.toList());
//    }
    public String readNextURLFromSeed() throws IOException
    {
        return bufferedReader.readLine();

=======
    public List<String> getInnerLinks(Scanner content){
        String pat = "(?i)(href)(\\s*)=\\s*(.+?)>"; //(?i)(<\s*a)(.+?)(href)(\s*)=\s*(.+?)> to look only in <a tags
        return content.findAll(pat).map(MatchResult::group).collect(Collectors.toList());
>>>>>>> 316a92fa5b5d76d1f4cd61ccfb00dad1bef30ba1
    }
    public BufferedReader downloadPage(String url) throws IOException
    {
        return new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
    }
    public byte[] calcChecksum(String content) throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(
                content.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isPageDownloadedBefore(String check_sum)
    {
        //if there is url in visited table with check_sum = check_sum -->do nothong
        return controller.isUrlWithCheckSumInVisited(check_sum);
    }
    public boolean isUrlInSeed(String url)
    {
        //check url in seet table
        return true;
    }

    public void addLinksToSeed(List<String> links)
    {
        for (String link : links) {
            String normalized_link = normalizeLink(link);
            controller.addUrlToSeed(normalized_link);
        }
    }
    public void addUrlToVisited(String url, String checksum)
    {
        controller.addUrlToVisited(url,checksum);
    }
    public boolean isPageHtml(String url)
    {
        try {
            URL obj = new URL(url);
            URLConnection conn = obj.openConnection();
            Map<String, List<String>> map = conn.getHeaderFields();
            return map.get("Content-Type").get(0).contains("text/html");
        }
        catch(Exception ex)
        {
            return false;
        }

    }



    public void savePageInFile(String checksum, String page_string) throws IOException {
        String filename = checksum + ".html";
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            writer.write(page_string);
            writer.close();
        } catch (IOException ex) {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public boolean isPageAllowedToCrawl(String url) {
        try {
//            URI uri = new URI(url);
//            String host=uri.getHost();
//            List<String> robot = controller.getRobot(host);
//            if(robot !=null)
//            {
////                isUrlAllowed(robot);
//            }
//            else
//            {
//
//                Document doc = Jsoup.parse(new URL(uri.getScheme()+"://"+host + "/robots.txt").openStream(), "UTF-8", url);
//                String[] body = doc.text().split("User-agent:");
//                System.out.println(body.length);
//                for (String line:body) {
//                    line=line.trim();
//                    if(line.startsWith("*"))
//                    {
//                        System.out.println(line);
//                    }
//                }
//            }
            return true;
        } catch (Exception ex) {
            System.out.println("webcrawler-> isPageALlowedToCrawl:" + ex);
        }
        return true;

    }
}
