package com.company;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class WebCrawler implements Runnable {

    BufferedReader bufferedReader;
    DBController controller = DBController.ContollerInit();
    static final AtomicInteger number_crawled = new AtomicInteger(0);

    public void run() {
        String link = controller.getUnVisitedLinkAndSet();
        while (number_crawled.getAndAdd(1) != 5000)// && link!=null
        {
            try {
                if (!isPageDownloadedBefore(toHexString(calcChecksum(link)))) {
                    downloadPage(link);
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
            link = controller.getUnVisitedLinkAndSet();
        }

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

    public List<String> getInnerLinks(Scanner content){
        String pat = "(?i)(href)(\\s*)=\\s*(.+?)>"; //(?i)(<\s*a)(.+?)(href)(\s*)=\s*(.+?)> to look only in <a tags
        return content.findAll(pat).map(MatchResult::group).collect(Collectors.toList());
    }
    public String readNextURLFromSeed() throws IOException
    {
        return bufferedReader.readLine();

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
    public void addUrlToDownloaded(String url, byte[] checksum)
    {

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


    private void checkUrlToVisitedInSeed(String url) {
        controller.checkUrl(url);
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

}
