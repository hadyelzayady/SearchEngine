package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
    }

    private static DBController handler = null;

    public static DBController ContollerInit() {
        if (handler == null) {
            handler = new DBController();
        }
        return handler;
    }

    public void addUrlToSeed(String normalized_url) {
        Document document = new Document("_id", normalized_url);
        seed_collection.insertOne(document);
    }

    public void addUrlToVisited(String normalized_url) {
        Document document = new Document("_id", normalized_url);
        seed_collection.insertOne(document);
    }

    public boolean isUrlWithCheckSumInVisited(String checksum) {
        BasicDBObject equal_query = new BasicDBObject();
        equal_query.put("_id", checksum);
        if (visited_collection.find(equal_query).limit(1) == null)//TODO I do not know if it returns null when not exist
            return false;
        return true;
    }


}
