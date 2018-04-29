package com.company;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//as froniter is very large and we limited to 5000 somesites may not be crawled
//todo domain restriction
//todo link_db
//TODO  password forget is chnaged every time as different token is assigned
public class WebCrawler implements Runnable {

    private DBController controller;
	private final int crawler_limit = 300;
    private static final AtomicLong number_crawled = new AtomicLong(0);
	private static final AtomicInteger next = new AtomicInteger(0);
	final private int lowest_priority = 100;
	private static final Pattern url_pattern = Pattern.compile("(https?://)([^:^/^?]*)(:\\d*)?(.*)?");
	private List<org.bson.Document> working_arr = new ArrayList<>();
	private List<org.bson.Document> empty_arr = new ArrayList<>();
	private boolean otherIsFilling = false;
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
		    controller.resetVisited();
            number_crawled.set(0);
        }
	    arrayInit();
    }

	private void arrayInit() {
//		ArrayList<org.bson.Document> main_arr = controller.getLinksFromFrontier();
//		working_arr=main_arr.subList(0,(main_arr.size()/2));
		working_arr = controller.getLinksFromFrontier();
//		empty_arr = main_arr.subList(main_arr.size()/2,main_arr.size());
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
	        org.bson.Document link_doc = getNextLinkToCrawl();
            if (link_doc != null) {
	            String link = link_doc.getString("url");
                String link_checksum = link_doc.getString("checksum");
	            try {
		            Matcher matcher = url_pattern.matcher(link);
		            matcher.find();
		            String protocole = matcher.group(1);
		            String domain = matcher.group(2);
		            String port = matcher.group(3);
		            String path = matcher.group(4);
		            if (path == null)
			            path = "";
		            if (port == null)
			            port = "";
		            if (isPageAllowedToCrawl(link, protocole, domain, port, path)) {
			            Document page = Jsoup.connect(link).get();
			            String page_content = page.outerHtml();
			            String checksum = toHexString(calcChecksum(page_content));
			            boolean ischanged = isChanged(checksum, link_doc);
			            if (ischanged) {
				            if (!isPageDownloadedBefore(checksum)) {
					            System.out.println("number_crawled " + number_crawled + "  " + link);
					            savePageInFile(checksum, page_content);
					            int priority = getCrawlingPriority(checksum, link_checksum, link_doc);
					            addLinksToFrontier(link, page);
					            incDomainConstraint(domain);
//	                        new Thread(new AsyncaddUrlToVisited(link, checksum, controller)).start();
					            controller.addUrlToVisited(link, checksum);
					            controller.updateLinkAndSetVisited(link, priority, checksum);
				            }
			            } else //not changed page
			            {
				            int pr = getPriortyForNotChanged(link, link_doc.getInteger("Offset"));
				            incDomainConstraint(domain);
				            controller.addUrlToVisited(link, checksum);
				            controller.updateLinkAndSetVisited(link, pr, checksum);
			            }
		            } else {
			            controller.deleteUrlFromFrontier(link);
		            }
	            } catch (MalformedURLException ex) {
		            System.out.println("crawler run " + ex);
		            controller.deleteUrlFromFrontier(link);

	            } catch (HttpStatusException ex) {
		            controller.updateErroredUrl(link, lowest_priority);
	            } catch (Exception ex) {
		            System.out.println("crawler run2" + link + " " + ex);
                    number_crawled.decrementAndGet();
                    //this todo is wrong as the link may work later//todo this url must not be crawled again ,option:set checksum to null and in reseting keep visited for null checksum to true
                    controller.deleteUrlFromFrontier(link);// not html content type raises exception and we set it to visited to not visit it again
                }
            }
        }


    }

	private int getPriortyForNotChanged(String link, int link_offset) {
		if (link_offset < lowest_priority)
			link_offset++;
		return link_offset;
	}

	private boolean isChanged(String checksum, org.bson.Document link_doc) {
		if (link_doc.containsKey("checksum")) {
			if (checksum.equals(link_doc.getString("checksum")))
				return false;
		}
		return true;
	}

	private org.bson.Document getNextLinkToCrawl() {
		synchronized (working_arr) {
			if (working_arr.isEmpty()) {
				//if two array are empty ,this happens rarely on the start of the search engine or filling array taken more time than consuming other array
				if (otherIsFilling == true) {
					return null;
				}
				//swap

				List<org.bson.Document> temp = working_arr;
				working_arr = empty_arr;
				empty_arr = temp;
			} else {
				return working_arr.remove(0);
			}
		}
		fillEmptyArray();
		otherIsFilling = false;
		return null;
	}

	private void fillEmptyArray() {
		empty_arr = controller.getLinksFromFrontier();
	}

	private void incDomainConstraint(String domain) {
		controller.incDomainPriority(domain);
	}

	private synchronized boolean isCrawlerFinished() {
        if (number_crawled.get() < crawler_limit)//todo change it later to normal integer
        {
            number_crawled.incrementAndGet();
            return true;
        }
        return false;

    }

	private int getCrawlingPriority(String new_checksum, String old_checksum, org.bson.Document link_doc) {
        if (!link_doc.containsKey("Priority") || old_checksum == null) {
	        return 2;
        }
	    int link_offset = link_doc.getInteger("Offset");
		if (link_offset > 2)
                --link_offset;
		return link_offset;
    }

	private void addLinksToFrontier(String url, Document page) throws UnsupportedEncodingException {
        Elements links = page.select("a[href]");
        controller.linkdbAddOutLinks(url, links.size());
		ArrayList<org.bson.Document> frontier_links = new ArrayList<>();
		ArrayList<org.bson.Document> domain_links = new ArrayList<>();
		for (Element link : links) {
            String norm_link = normalizeLink(link.attr("abs:href"));
			Matcher matcher = url_pattern.matcher(url);
			if (matcher.find()) {
				String domain = matcher.group(2);//protocol + domain as in join with frontier we can join
				org.bson.Document document = new org.bson.Document("_id", norm_link).append("Visited", false).append("Priority", 2).append("Domain_FK", domain);
				org.bson.Document domain_doc = new org.bson.Document("Domain", domain).append("Domain_Constraint", 0);
				frontier_links.add(document);
				domain_links.add(domain_doc);
//				org.bson.Document linkdb_document = new org.bson.Document("_id", norm_link).append("Visited", false).append("Priority", 2);
			}
			//link db
//            controller.addInnLink(url, norm_link);
        }
		controller.addManyUrlToFrontier(frontier_links);
		controller.addManyDomain(domain_links);
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


//    private void addUrlToVisited(String url, String checksum) {
//        controller.addUrlToVisited(url, checksum);
//    }

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

	private boolean isPageAllowedToCrawl(String url, String protocol, String domain, String port, String path) {
        try {
	        org.bson.Document robot_doc = controller.getRobot(domain);
            if (robot_doc != null && robot_doc.getBoolean("updated")) {
                return isPathAllowedInRobot(path, robot_doc);
            } else {
	            String link = protocol + port + domain + "/robots.txt";
	            String text = downloadRobot(link);
                String checksum = toHexString(calcChecksum(text));
                if (robot_doc != null && checksum.equals(robot_doc.getString("checksum"))) {
	                controller.setRobotUpdated(domain);
                    return isPathAllowedInRobot(path, robot_doc);
                }
	            return addRobotAndCheckAllow(text, domain, checksum, path);
//              String robot="User-agent: * Allow: /w/api.php?action=mobileview&Disallow: /api/Allow: /w/load.php?Allow: /api/rest_v1/?docDisallow: /w/Disallow: /trap/Disallow: /wiki/Special:Disallow: /wiki/Spezial:";

            }
        } catch (Exception ex) {
            System.out.println("webcrawler-> isPageALlowedToCrawl:" + ex);
        }
        return true;
    }

	private String downloadRobot(String robot_link) {
        try {
	        Document doc = Jsoup.parse(new URL(robot_link).openStream(), "UTF-8", "");
            return doc.text();
        } catch (Exception ex) {
	        System.out.println("download robot " + ex);
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