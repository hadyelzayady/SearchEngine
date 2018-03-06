package com.company;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;

public class ConnectToDB {
    public static MongoCollection<Document> visited_collection;
    public static MongoCollection<Document> seed_collection;

    public static void DBinit() {

        // Creating a Mongo client
        MongoClient mongo = new MongoClient("localhost", 27017);
        // Accessing the database
        MongoDatabase database = mongo.getDatabase("SearchEngineDB");
//        //Creating a collection
//        database.createCollection("WebCrawler");
//        System.out.println("Collection created successfully");
        // Retieving a collection
        try {
            database.getCollection("Seed").drop();
            database.createCollection("Seed");
        } catch (Exception ex) {
            seed_collection = database.getCollection("Seed");

        }
        try {
            database.getCollection("Visited").drop();
            database.createCollection("Visited");
        } catch (Exception ex) {
            visited_collection = database.getCollection("Visited");

        } finally {
            seed_collection = database.getCollection("Seed");
            visited_collection = database.getCollection("Visited");
        }

    }

}
