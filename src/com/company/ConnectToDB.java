package com.company;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


import org.bson.Document;

public class ConnectToDB {
    public static MongoCollection<Document> visited_collection;
    public static MongoCollection<Document> seed_collection;
    public static MongoCollection<Document> frontier_collection;

    public static void DBinit() {

        // Creating a Mongo client
        MongoClient mongo = new MongoClient("localhost", 27017);
        // Accessing the database
        MongoDatabase database = mongo.getDatabase("SearchEngineDB");
//        //Creating a collection
//        database.createCollection("WebCrawler");
//        System.out.println("Collection created successfully");
        // Retieving a collection
            database.getCollection("Seed").drop();
            seed_collection = database.getCollection("Seed");

        database.getCollection("Frontier").drop();
        frontier_collection = database.getCollection("Frontier");

            database.getCollection("Visited").drop();
            visited_collection = database.getCollection("Visited");


    }

}
