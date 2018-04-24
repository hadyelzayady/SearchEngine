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
import java.util.concurrent.atomic.AtomicLong;
import java.security.MessageDigest;

//as froniter is very large and we limited to 5000 somesites may not be crawled
//todo add offset to increment priority if not changed since long
//reset robot to updated=false
public class WebCrawler implements Runnable {

    /**
     *
     */
    private DBController controller;
    private final int crawler_limit = 30;
    private static final AtomicLong number_crawled = new AtomicLong(0);
    final private int lowest_priority = 5;

    WebCrawler() {
        controller = DBController.ContollerInit();
        long visited_count = controller.getVisitedCount();
        if (0 < visited_count && visited_count < crawler_limit)//interrupt happened
        {
            controller.setWorkOnPagesToUnVisited();
            number_crawled.set(visited_count);
        } else {
            controller.resetFrontier();
            controller.resetRobotStatus();
//            controller.resetVisited();
            number_crawled.set(0);
        }
    }

    int iter = 1;
    public void recrawlreset() {
        iter++;
        controller.resetFrontier();
//        controller.resetVisited();
        number_crawled.set(0);
        controller.resetRobotStatus();
    }
    public void run() {

        while (isCrawlerFinished())// && link!=null
        {
            org.bson.Document link_doc = controller.getLinkFromFrontierAndSetOnwork();
            if (link_doc != null) {
                String link = link_doc.getString("_id");
                String link_checksum = link_doc.getString("checksum");
                try {
                    if (isPageAllowedToCrawl(link)) {
                        Document page = Jsoup.connect(link).get();
                        String page_content = page.outerHtml();
                        String checksum = toHexString(calcChecksum(page_content));
                        System.out.println(Thread.currentThread().getName());
                        if (!isPageDownloadedBefore(checksum)) {
                            System.out.println(number_crawled);
                            savePageInFile(checksum, page_content);
                            setCrawlingPriority(checksum, link_checksum, link_doc);
                            addLinksToFrontier(link, page);
                            addUrlToVisited(link, checksum);
                            controller.setUrlVisited(link, checksum);
                            System.out.println("iter" + iter + " finished crawling " + link);
                        }
                    } else {
                        controller.deleteUrlFromFrontier(link);
                    }
                } catch (Exception ex) {
                    System.out.println(link + " " + ex);
                    number_crawled.decrementAndGet();
                    //this todo is wrong as the link may work later//todo this url must not be crawled again ,option:set checksum to null and in reseting keep visited for null checksum to true
                    controller.deleteUrlFromFrontier(link);// not html content type raises exception and we set it to visited to not visit it again
                }
            }
        }


    }

    private synchronized boolean isCrawlerFinished() {
        if (number_crawled.get() < crawler_limit)//todo change it later to normal integer
        {
            number_crawled.incrementAndGet();
            return true;
        }
        return false;

    }

    private void setCrawlingPriority(String new_checksum, String old_checksum, org.bson.Document link_doc) {
        String link = link_doc.getString("_id");
        if (!link_doc.containsKey("Priority") || old_checksum == null) {
            controller.setPriority(2, link, 2);
            return;
        }
        int link_priority = link_doc.getInteger("Priority");
        int link_offset = link_doc.getInteger("offset");
        boolean notchanged = new_checksum.equals(old_checksum);
        if (notchanged)//not change --> lower priority (higher number is lower priority:1 is highest priority and 5 is lowest
        {
            if (link_offset < lowest_priority)
                link_offset++;
            controller.setPriority(link_offset, link, link_offset);//lower
        } else if (!notchanged) {
            System.out.println("changed page" + link);
            if (link_offset > 2)
                --link_offset;
            controller.setPriority(link_offset, link, link_offset);//lower
        }
    }

    private void addLinksToFrontier(String url, Document page) {
        Elements links = page.select("a[href]");
        controller.linkdbAddOutLinks(url, links.size());
        for (Element link : links) {//todo try to use insertmany insteadof insert one by one
            String norm_link = normalizeLink(link.attr("abs:href"));
            controller.addUrlToFrontier(norm_link);
            controller.addInnLink(url, norm_link);
        }
    }

    private String normalizeLink(String url) {
        String normalized_url = url.replaceAll("#\\S*", "");
        normalized_url = normalized_url.toLowerCase();
        return normalized_url;
    }



    private byte[] calcChecksum(String content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(
                content.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isPageDownloadedBefore(String check_sum) {
        //if there is url in visited table with check_sum = check_sum -->do nothong
        return controller.isUrlWithCheckSumInVisited(check_sum);
    }


    private void addUrlToVisited(String url, String checksum) {
        controller.addUrlToVisited(url, checksum);
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


    private void savePageInFile(String checksum, String page_string) throws IOException {
        String filename = "Pages/" + checksum + ".html";
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            writer.write(page_string);
            writer.close();
        } catch (IOException ex) {
            if (writer != null) writer.close();

        }
    }


    private String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private boolean isPageAllowedToCrawl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            org.bson.Document robot_doc = controller.getRobot(host, path);
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

    private boolean addRobotAndCheckAllow(String text, String host, String checksum, String path) {
        try {

            String[] body = text.split("User-agent:\\s*\\*[^a-zA-Z]");
            ArrayList<String> allowed_doc_arr = new ArrayList<String>();
            ArrayList<String> disallowed_doc_arr = new ArrayList<String>();
            org.bson.Document allow_disallow_doc = new org.bson.Document();
            allow_disallow_doc.put("_id", normalizeLink(host));
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

    private boolean isPathAllowedInRobot(String path, org.bson.Document robot) {
        try {
            List<String> allow = (List<String>) robot.get("allow");
            List<String> disallow = (List<String>) robot.get("disallow");
            for (final String allowed_path : allow) {
                if (path.matches(allowed_path))
                    return true;
            }
            for (final String disallowed_path : disallow) {
                if (path.matches(disallowed_path))
                    return false;
            }
            return true;
        } catch (Exception ex) {
            return true;
        }
    }
}