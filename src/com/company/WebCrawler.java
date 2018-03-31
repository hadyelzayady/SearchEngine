package com.company;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URI;
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
            controller.resetRobotStatus();
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
                controller.setUrlVisited(link);// not html content type raises exception and we set it to visited to not visit it again
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

    public boolean doesLinkExistInSeed(String link) {
        return false;
    }

    public String normalizeLink(String url) {
        String normalized_url=url.replaceAll("#\\S*","");
        normalized_url=normalized_url.toLowerCase();
        return normalized_url;
    }


    public List<String> getInnerLinks(Scanner content){
        String pat = "(?i)(href)(\\s*)=\\s*(.+?)>"; //(?i)(<\s*a)(.+?)(href)(\s*)=\s*(.+?)> to look only in <a tags
        return content.findAll(pat).map(MatchResult::group).collect(Collectors.toList());
    }

    public BufferedReader downloadPage(String url) throws IOException {
        return new BufferedReader(new InputStreamReader((new URL(url)).openStream()));
    }

    public byte[] calcChecksum(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(
                content.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isPageDownloadedBefore(String check_sum) {
        //if there is url in visited table with check_sum = check_sum -->do nothong
        return controller.isUrlWithCheckSumInVisited(check_sum);
    }

    public boolean isUrlInSeed(String url) {
        //check url in seet table
        return true;
    }

    public void addLinksToSeed(List<String> links) {
        for (String link : links) {
            String normalized_link = normalizeLink(link);
            controller.addUrlToSeed(normalized_link);
        }
    }

    public void addUrlToVisited(String url, String checksum) {
        controller.addUrlToVisited(url,checksum);
    }

    public boolean isPageHtml(String url) {
        try {
            URL obj = new URL(url);
            URLConnection conn = obj.openConnection();
            Map<String, List<String>> map = conn.getHeaderFields();
            return map.get("Content-Type").get(0).contains("text/html");
        } catch (Exception ex) {
            return false;
        }

    }



    public void savePageInFile(String checksum, String page_string) throws IOException {
        String filename = "Pages/" + checksum + ".html";
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

    //todo normalize url in robots
    public boolean isPageAllowedToCrawl(String url) {
        try {
            //todo if not robots set allow and disallow null
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            org.bson.Document robot_doc = controller.getRobot(host, path);
//            Boolean s=robot_doc.getBoolean("updated");
            if (robot_doc != null && robot_doc.getBoolean("updated")) {

                return isPathAllowedInRobot(path, robot_doc);
            } else {
                String text = downloadRobot(uri, url);
                String checksum = toHexString(calcChecksum(text));
                if (robot_doc != null && checksum.equals(robot_doc.getString("checksum"))) {
                    controller.setRobotUpdated(host);
                    return isPathAllowedInRobot(path, robot_doc);
                }
                return addRobotAndCheckAllow(text, host, checksum, path);
//              String robot="User-agent: * Allow: /w/api.php?action=mobileview&Disallow: /api/Allow: /w/load.php?Allow: /api/rest_v1/?docDisallow: /w/Disallow: /trap/Disallow: /wiki/Special:Disallow: /wiki/Spezial:";

            }
        } catch (Exception ex) {
            System.out.println("webcrawler-> isPageALlowedToCrawl:" + ex);
        }
        return true;
    }

    private String downloadRobot(URI uri, String url) {
        try {
            Document doc = Jsoup.parse(new URL(uri.getScheme() + "://" + uri.getHost() + "/robots.txt").openStream(), "UTF-8", url);
            return doc.text();
        } catch (Exception ex) {
            System.out.println(ex);
            return "";
        }

    }

    public boolean addRobotAndCheckAllow(String text, String host, String checksum, String path) {
        try {
            //text="";//todo test that

            String[] body = text.split("User-agent:\\s*\\*[^a-zA-Z]");
            ArrayList<String> allowed_doc_arr = new ArrayList<String>();
            ArrayList<String> disallowed_doc_arr = new ArrayList<String>();
            org.bson.Document allow_disallow_doc = new org.bson.Document();
            allow_disallow_doc.put("_id", host);
            allow_disallow_doc.put("updated", true);
            if (text.equals("")) {
                allow_disallow_doc.put("allow", allowed_doc_arr);
                allow_disallow_doc.put("disallow", disallowed_doc_arr);
                allow_disallow_doc.put("checksum", checksum);
                controller.addRobot(allow_disallow_doc, host);
                return true;
            }
            boolean is_allowed = true;
            boolean allowed = false;//for allow: in robot
            if (body.length == 2) {
                String disallow_allow = body[1].split("User-agent")[0];
                String[] disallow = disallow_allow.split("Disallow:\\s*");
                for (String word : disallow) {
                    String[] allows = word.split("Allow:\\s*");
                    if (allows.length > 1)//it contains allows
                    {
                        for (int i = 1; i < allows.length; i++) {
                            org.bson.Document document = new org.bson.Document();
                            String allowed_path = allows[i].trim().replaceAll("\\*", ".*");
                            document.put("path", allowed_path);
                            if (!allowed && path.matches(allowed_path))
                                allowed = true;
                            allowed_doc_arr.add(allowed_path);
                        }
                    }
                    if (!allows[0].equals("")) {
                        org.bson.Document document = new org.bson.Document();
                        String disallowed_path = allows[0].trim().replaceAll("\\*", ".*");
                        document.put("path", disallowed_path);
                        if (is_allowed && path.matches(disallowed_path))//first condition for performance improvement
                            is_allowed = false;
                        disallowed_doc_arr.add(disallowed_path);
                    }
                }
                allow_disallow_doc.put("allow", allowed_doc_arr);
                allow_disallow_doc.put("disallow", disallowed_doc_arr);
                allow_disallow_doc.put("checksum", checksum);
                controller.addRobot(allow_disallow_doc, host);
                return is_allowed || allowed;
            }
        } catch (Exception ex) {
            System.out.println("webcrawler->addRobot: " + ex);
        }
        return true;
    }

    protected boolean isPathAllowedInRobot(String path, org.bson.Document robot) {
        List<String> allow = (List<String>) robot.get("allow");
        List<String> disallow = (List<String>) robot.get("disallow");
        for (int i = 0; i < allow.size(); i++) {
            final String allowed_path = allow.get(i);
            if (path.matches(allowed_path))
                return true;
        }
        for (int i = 0; i < disallow.size(); i++) {
            final String disallowed_path = disallow.get(i);
            ;
            if (path.matches(disallowed_path))
                return false;
        }
        return true;
    }


}