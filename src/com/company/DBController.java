package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;

import static com.mongodb.client.model.Projections.*;
import java.util.Iterator;

//import com.sun.javadoc.Doc;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.company.Token_info;
import org.jsoup.nodes.Element;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import java.util.List;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;
    MongoCollection<Document> frontier_collection;
    MongoCollection<Document> metadata_collection;

    MongoCollection<Document> Inverted_file;
    MongoCollection<Document> Useless_words;
    MongoCollection<Document> robots_collection;
    MongoCollection<Document> linkdatabase_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
        frontier_collection = ConnectToDB.frontier_collection;
        metadata_collection = ConnectToDB.metadata_collection;
        linkdatabase_collection = ConnectToDB.linkdatabase_collection;

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

    private int getOffsetAndUpdate() {
//   metadata_collection.findOneAndUpdate()
        return 1;
    }
    public void addUrlToFrontier(String url) {
        try {
            Document document = new Document("_id", url).append("Visited", false).append("Priority", 2);//null indicated just added under work or visited
            frontier_collection.insertOne(document);//TODO use async driver in insertion and update
        } catch (Exception ex) {
//            System.out.println(url + " already in frontier");
        }
    }
    

    public void addUrlToVisited(String url, String checksum) {
        try {
            Bson doc = new Document("_id", url);
            Document old_visited = visited_collection.find(doc).first();
            if (old_visited != null) {
                String old_checksum = old_visited.getString("checksum");
                if (!old_checksum.equals(checksum)) {
                    System.out.println("inside inner if");
                    Document document = new Document("_id", url).append("checksum", checksum).append("indexed", false);
                    visited_collection.replaceOne(doc, document);
                }
            } else {
                Document document = new Document("_id", url).append("checksum", checksum).append("indexed", false);
                visited_collection.insertOne(document);
            }
        } catch (Exception ex) {
            System.out.println("add url to visited " + ex);
        }
    }

    //
    public boolean isUrlWithCheckSumInVisited(String checksum) {
        BasicDBObject equal_query = new BasicDBObject();
        equal_query.put("_id", checksum);
        if (visited_collection.find(equal_query).limit(1).first() != null)//TODO I do not know if it returns null when not exist
            return true;
        return false;
    }


    public void AddToInvertedFile(Token_info token,String name1,String name2,String name3)
    {
    	//System.out.println("Hady Zyady");
    	Document document=new Document("_id",token.get_token_name());
    	Bson filter = eq("_id", token.get_token_name());
    	Document temp_doc0= new Document(name1,token.get_token_Url())
        		.append(name2,token.get_token_position());
    	temp_doc0.append(name3, token.get_token_type());
        Bson change = push("token_info",temp_doc0);
    	if(Inverted_file.count()==0)
    	{
    		Inverted_file.insertOne(document);
    		Inverted_file.updateOne(filter, change);
    		System.out.println("passed");
    	}
    	else
    	{
    		Document temp_doc = new Document();
    		/*Bson filter2 = Filters.in("token_info",change);
    		Bson query = Filters.elemMatch("token_info", filter2);
    		Bson update = new org.bson.Document("$pull", filter2); 
    		Inverted_file.updateMany(query, update);*/
    		FindIterable<Document> docs = Inverted_file.find();
    		boolean found=false;
    		for(Document doc:docs)
    		{
    			if(doc.getString("_id").equals(token.get_token_name()))
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
    			if(!temp_doc.get("token_info").toString().contains(temp_doc0.toString()))
    			/*Bson filter5 =new Document("token_info",temp_doc0);
    			if(Inverted_file.find(filter5)==null)*/ {
                    Inverted_file.updateOne(filter, change);
                }
            }
        }
    }

    public boolean found_unindexed_pages() {
        boolean found = false;
        Bson filter = new Document("indexed", false);
        if (visited_collection.find(filter) != null)
            found = true;
        return found;
    }

    public void deleteUrlFromSeed(String url) {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", url);
        seed_collection.findOneAndDelete(document);
    }

    public synchronized void setUrlVisited(String url, String checksum) {
        Bson filter = new Document("_id", url);
        Bson newValue = new Document("Visited", true).append("checksum", checksum);
        Bson updateOperationDocument = new Document("$set", newValue);
        frontier_collection.updateOne(filter, updateOperationDocument);
    }

    public synchronized Document getLinkFromFrontierAndSetOnwork() {
        Bson filter = new Document("Visited", false);
        Bson newValue = new Document("Visited", null);
        Bson sort_doc = new Document("Priority", 1);
        Bson updateOperationDocument = new Document("$set", newValue);
        Bson Doc = new FindOneAndUpdateOptions().getSort();
        Document unvisited_link = frontier_collection.findOneAndUpdate(filter, updateOperationDocument, new FindOneAndUpdateOptions().sort(sort_doc));
        if (unvisited_link != null)
            return unvisited_link;
        return null;
    }
    public void removeLink(String url) {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", url);
        seed_collection.findOneAndDelete(document);
    }

    public void resetFrontier() {
        if (frontier_collection.count() == 0) {
            BasicDBObject document = new BasicDBObject();
            frontier_collection.deleteMany(document);
            for (Document doc : seed_collection.find()) {
                doc.append("Priority", 1);
                doc.append("Offset", 0);
                frontier_collection.insertOne(doc);
            }
        } else {
            //dec priority of not visited pages in last crawler
            Bson newValue = new Document("Priority", -1);
            Bson filter = new Document("Visited", false).append("Priority", new Document("$ne", 1));
            Bson updateOperationDocument = new Document("$inc", newValue);
            frontier_collection.updateMany(filter, updateOperationDocument);
            //reset all pages to not visited
            Bson visited_newValue = new Document("Visited", false);
            Bson filter2 = new Document();
            Bson updateOperationDocument2 = new Document("$set", visited_newValue);
            frontier_collection.updateMany(filter2, updateOperationDocument2);
        }
    }

    private void resetRobot() {
        Bson newValue = new Document("Updated", false);
        Bson filter = new Document();
        Bson updateOperationDocument = new Document("$set", newValue);
        robots_collection.updateMany(filter, updateOperationDocument);
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

    public void addRobot(Document allow_disallow_doc, String url) {
//        Bson filter = eq("_id", url);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", url);
        robots_collection.replaceOne(query, allow_disallow_doc, new UpdateOptions().upsert(true));
    }

    public void setRobotUpdated(String url) {
        Bson filter = new Document("_id", url);
        Bson newValue = new Document("updated", true);
        Bson updateOperationDocument = new Document("$set", newValue);
        robots_collection.updateOne(filter, updateOperationDocument);
    }

    public void resetRobotStatus() {
        Bson newValue = new Document("updated", false);
        Bson filter = new Document();
        Bson updateOperationDocument = new Document("$set", newValue);
        robots_collection.updateMany(filter, updateOperationDocument);
    }

    public void setPriority(int priority, String url, int offset) {
        Bson filter = new Document("_id", url);
        Bson newValue = new Document("Priority", priority).append("offset", offset);
        Bson updateOperationDocument = new Document("$set", newValue);
        frontier_collection.updateOne(filter, updateOperationDocument);
    }

    public long getVisitedCount() {
        BasicDBObject query = new BasicDBObject("Visited", true);
//        Document url_doc = robots_collection.find(query).filter(Filters.elemMatch("allow", Filters.regex("url", url))).first();
        return frontier_collection.count(query);
    }

    public void setWorkOnPagesToUnVisited() {
        Bson newValue = new Document("Visited", false);
        Bson filter = new Document("Visited", null);
        Bson updateOperationDocument = new Document("$set", newValue);
        frontier_collection.updateMany(filter, updateOperationDocument);
    }

    public int getminPriority() {
        Bson filter = new Document("Visited", false);
        Document doc = frontier_collection.find(filter).sort(Sorts.ascending("Priority")).first();
        if (doc != null) {
            return doc.getInteger("Priority");
        }
        return 1;
    }

    public void deleteUrlFromFrontier(String link) {
        BasicDBObject document = new BasicDBObject();
        document.put("_id", link);
        frontier_collection.findOneAndDelete(document);
    }

    public void linkdbAddOutLinks(String url, int size) {
        Document newValue = new Document("outLinks", size);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", url);
        linkdatabase_collection.replaceOne(query, newValue, new UpdateOptions().upsert(true));
    }

    public void addInnLink(String url, String mylink) {

    }

    public void deleteInvertedFile(String link) {
        Bson match = new BasicDBObject(); // to match your document
        BasicDBObject update = new BasicDBObject("token_info", new BasicDBObject("Url_id", link));
        Inverted_file.updateMany(match, new BasicDBObject("$pull", update));
    }
}