package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;
    MongoCollection<Document> frontier_collection;
    MongoCollection<Document> metadata_collection;
    MongoCollection<Document> robots_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
        frontier_collection = ConnectToDB.frontier_collection;
        metadata_collection = ConnectToDB.metadata_collection;
        robots_collection = ConnectToDB.robots_collection;
        addUrlToSeed("https://www.wikipedia.org/");
    }

    private static DBController handler = null;

    public static DBController ContollerInit() {
        if (handler == null) {
            handler = new DBController();
        }
        return handler;
    }

    public void addUrlToSeed(String url) {
        try {
            Document document = new Document("_id", url).append("Visited", false);
            seed_collection.insertOne(document);//TODO use async driver in insertion and update
        } catch (Exception ex) {

        }
    }

    public void addUrlToFrontier(String url) {
        try {
            Document document = new Document("_id", url).append("Visited", null);//null indicated just added under work or visited
            frontier_collection.insertOne(document);//TODO use async driver in insertion and update
        } catch (Exception ex) {
//            System.out.println(url + " already in frontier");
        }
    }

    public void addUrlToVisited(String url, String checksum) {
        Document document = new Document("_id", url).append("checksum", checksum).append("indexed", false);
        visited_collection.insertOne(document);
    }

    //
    public boolean isUrlWithCheckSumInVisited(String checksum) {
        BasicDBObject equal_query = new BasicDBObject();
        equal_query.put("_id", checksum);
        if (visited_collection.find(equal_query).limit(1).first() != null)//TODO I do not know if it returns null when not exist
            return true;
        return false;
    }


    public void deleteUrlFromSeed(String url) {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", url);
        seed_collection.findOneAndDelete(document);
    }

    public synchronized void setUrlVisited(String url) {
        Bson filter = new Document("_id", url);
        Bson newValue = new Document("Visited", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        frontier_collection.updateOne(filter, updateOperationDocument);
    }

    public synchronized String getLinkFromFrontierAndSetOnwork() {
        BasicDBObject equal_query = new BasicDBObject();
        BasicDBObject field = new BasicDBObject();
        equal_query.put("Visited", null);
        Bson newValue = new Document("Visited", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        Document unvisited_link = frontier_collection.findOneAndUpdate(equal_query, updateOperationDocument);
        if (unvisited_link != null)
            return unvisited_link.getString("_id");
        return null;
    }
    public void removeLink(String url) {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", url);
        seed_collection.findOneAndDelete(document);
    }

    public void resetFrontier() {
        BasicDBObject document = new BasicDBObject();
        frontier_collection.deleteMany(document);//TODO not sure if it does the desired behaviout(clean collection)
        for (Document doc : seed_collection.find()) {
            frontier_collection.insertOne(doc);
        }
    }

    public String[] getUnIndexedPageUrlFilenameAndSet() {
        Bson newValue = new Document("indexed", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        BasicDBObject document = new BasicDBObject();
        document.put("indexed", false);
        Document page=visited_collection.findOneAndUpdate(document,updateOperationDocument);
        if(page !=null)
            return new String[]{page.getString("_id"),page.getString("checksum")};
        return null;
    }

    public int getCrawledCount() {
        return 0;
    }

    //todo implement
    public boolean isUrlInRobot(String url) {
        return false;
    }

    public void addRobot(String url) {
//        String home_url=url.matches("(http)(s?)://");
    }

    public boolean isRobotsExist(String home_url) {
        return false;
    }

    public List<String> getRobot(String home_url) {
        BasicDBObject query = new BasicDBObject("url", home_url);
        Document urls = robots_collection.find(query).first();
        if (urls != null) {
            List<Document> urlss = (List<Document>) urls.get("disallowed_urls");
            urlss.get(0);
            return null;//todo should return array of strings
        }
        return null;
    }

    public void resetVisited() {
        visited_collection.drop();
    }
}
