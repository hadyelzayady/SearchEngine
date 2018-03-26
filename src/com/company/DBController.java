package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;
    MongoCollection<Document> frontier_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
        frontier_collection = ConnectToDB.frontier_collection;
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
        Document document = new Document("_id", url).append("Visited", false);
        seed_collection.insertOne(document);//TODO use async driver in insertion and update
    }

    public void addUrlToVisited(String url, String checksum) {
        Document document = new Document("_id", url).append("checksum", checksum);
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

    public void checkUrl(String url) {
        Bson filter = new Document("_id", url);
        Bson newValue = new Document("Visited", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        seed_collection.updateOne(filter, updateOperationDocument);
    }

    public synchronized String getUnVisitedLinkAndDelete() {
        BasicDBObject equal_query = new BasicDBObject();
        BasicDBObject field = new BasicDBObject();
        equal_query.put("Visited", false);
        Document unvisited_link = seed_collection.findOneAndDelete(equal_query);
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
//        frontier_collection.aggregate({"$out":"newCollection"});
    }

    public String[] getUnIndexedPageUrlFilenameAndSet() {
        Bson newValue = new Document("Indexed", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        BasicDBObject document = new BasicDBObject();
        document.put("Indexed", false);
        Document page=visited_collection.findOneAndUpdate(document,updateOperationDocument);
        if(page !=null)
            return new String[]{page.getString("_id"),page.getString("checksum")};
        return null;
    }
}
