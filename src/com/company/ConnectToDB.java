package com.company;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


import org.bson.Document;

public class ConnectToDB {
    public static MongoCollection<Document> visited_collection;
    public static MongoCollection<Document> seed_collection;
    public static MongoCollection<Document> frontier_collection;
    public static MongoCollection<Document> metadata_collection;
    public static MongoCollection<Document> Inverted_file;
    public static MongoCollection<Document> robots_collection;
    public static MongoCollection<Document> linkdatabase_collection;
    public static MongoCollection<Document> Url_tokens;

    public static void DBinit() {

        // Creating a Mongo client
        MongoClient mongo = new MongoClient("localhost", 27017);
        // Accessing the database
        MongoDatabase database = mongo.getDatabase("SearchEngineDB");
//        //Creating a collection
//        database.createCollection("WebCrawler");
//        System.out.println("Collection created successfully");
        // Retieving a collection
//            database.getCollection("Seed").drop();
            seed_collection = database.getCollection("Seed");

//        database.getCollection("Frontier").drop();
        frontier_collection = database.getCollection("Frontier");
        linkdatabase_collection = database.getCollection("Link_db");
            database.getCollection("Visited").drop();
            visited_collection = database.getCollection("Visited");
        metadata_collection = database.getCollection("Metadata");
       // database.createCollection("Inverted_file");
        Inverted_file= database.getCollection("Inverted_file");
        Url_tokens= database.getCollection("Url_tokens");
        robots_collection = database.getCollection("Robots");


    }

}
