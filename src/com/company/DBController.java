package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import static com.mongodb.client.model.Projections.*;
import java.util.Iterator;

import com.mongodb.client.model.Projections;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.company.Token_info;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import java.util.List;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;
    MongoCollection<Document> frontier_collection;
    MongoCollection<Document> metadata_collection;

    MongoCollection<Document> Inverted_file;

    MongoCollection<Document> robots_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
        frontier_collection = ConnectToDB.frontier_collection;
        metadata_collection = ConnectToDB.metadata_collection;

        Inverted_file =  ConnectToDB.Inverted_file;
//        addUrlToSeed("https://www.wikipedia.org/");

        robots_collection = ConnectToDB.robots_collection;
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
    
    public void AddToInvertedFile(String token,String name1,String name2,String Url_id,int position)
    {
    	Document document=new Document("_id",token);
    	Bson filter = eq("_id", token);
        Bson change = push("token_info", new Document(name1, Url_id).append(name2, position));
    	if(Inverted_file.count()==0)
    	{
    		Inverted_file.insertOne(document);
    		Inverted_file.updateOne(filter, change);
    		System.out.println("passed");
    	}
    	else
    	{
    		Document temp_doc = null;
    		FindIterable<Document> docs = Inverted_file.find();
    		boolean found=false;
    		for(Document doc:docs)
    		{
    			if(doc.getString("_id").equals(token))
    			{
    				found=true;
    				temp_doc=doc;
    				break;
    			}
    		}
    		if(!found)
    		{
    			Inverted_file.insertOne(document);
    		    Inverted_file.updateOne(filter, change);
    		}
    		else
    		{
    			//Inverted_file.deleteOne(Filters.eq("_id", token));
    			//temp_doc.append("token_info",new Document(name1,Url_id).append(name2, position));
    			//String s1=temp_doc.toString();
    			//System.out.println(s1);
    			//Bson filter = eq("_id", token);
    		    //Bson change = push("token_info", new Document(name1,Url_id).append(name2, position));
    		    Inverted_file.updateOne(filter, change);
    		}
    	}
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



    public boolean isRobotsExist(String home_url) {
        return false;
    }

    public Document getRobot(String home_url, String url) {
        BasicDBObject query = new BasicDBObject("_id", home_url);
//        Document url_doc = robots_collection.find(query).filter(Filters.elemMatch("allow", Filters.regex("url", url))).first();
        Document url_doc = robots_collection.find(query).projection(exclude("_id")).first();
        return url_doc;
    }

    public void resetVisited() {
        visited_collection.drop();
    }

    public void addRobot(Document allow_disallow_doc) {
        robots_collection.insertOne(allow_disallow_doc);
    }
}
