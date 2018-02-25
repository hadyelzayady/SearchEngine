package com.company;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;
import java.security.MessageDigest;
public class WebCrawler {
    Queue<String> pending_links;
    BufferedReader bufferedReader;
    void openSeedFile()throws IOException{
        bufferedReader =new BufferedReader(new FileReader("seed.txt"));
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
        return content.findAll(pat)
                .map(MatchResult::group)
                .collect(Collectors.toList());
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
    public boolean isPageDownloadedBefore(byte[] check_sum)
    {
        //if there is url in visited table with check_sum = check_sum -->do nothong
        //
        return true;

    }
    public boolean isUrlInSeed(String url)
    {
        //check url in seet table
        return true;
    }
    public boolean addLinkToSeedAndQue(String url)
    {
        return true;
    }
    public String getUnVisitedLink()
    {
        return "";
    }
    public void addUrlToDownloaded(String url, byte[] checksum)
    {

    }
    public boolean startCrawler() throws  IOException
    {
        String link=getUnVisitedLink();
        while (link!=null)
        {
            try
            {
                pending_links.add(link);
                while (!pending_links.isEmpty())
                {
                    String url = pending_links.poll();
                    BufferedReader page_buffer = downloadPage(url);
                    byte[] checksum=calcChecksum(page_buffer.toString());
                    if (isPageDownloadedBefore(checksum))
                        continue;
                    //page not downloaed before --> add to visited table with its checksum download its hyper links
                    addUrlToDownloaded(url,checksum);
                    List<String> inner_links=getInnerLinks(new Scanner(page_buffer));


                }

                link = getUnVisitedLink();
            }
            catch (Exception ex)
            {
                System.out.println(ex);
            }
        }
        return true;
    }

}
