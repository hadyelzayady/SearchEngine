package com.company;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import static com.mongodb.client.model.Projections.*;

import com.mongodb.util.JSON;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import com.mongodb.connection.QueryResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.company.Token_info;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.MongoCursor;

import javax.print.Doc;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class DBController {
    MongoCollection<Document> seed_collection;
    MongoCollection<Document> visited_collection;
    MongoCollection<Document> frontier_collection;
    MongoCollection<Document> metadata_collection;

    MongoCollection<Document> Inverted_file;
    MongoCollection<Document> Useless_words;
    MongoCollection<Document> robots_collection;
    MongoCollection<Document> linkdatabase_collection;
    MongoCollection<Document> queryResult_collection;
	MongoCollection<Document> domain_collection;

    private DBController() {
        ConnectToDB.DBinit();
        seed_collection = ConnectToDB.seed_collection;
        visited_collection = ConnectToDB.visited_collection;
        frontier_collection = ConnectToDB.frontier_collection;
        metadata_collection = ConnectToDB.metadata_collection;
        linkdatabase_collection = ConnectToDB.linkdatabase_collection;
	    domain_collection = ConnectToDB.domain_collection;

        Inverted_file =  ConnectToDB.Inverted_file;
       //addUrlToSeed("https://www.wikipedia.org/");
	    //addUrlToSeed("https://www.theguardian.com/international");
//        addUrlToSeed("http://www.bbc.com/news");
//        addUrlToSeed("https://www.history.com/");
        robots_collection = ConnectToDB.robots_collection;
        queryResult_collection = ConnectToDB.queryResult_collection;
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
	                // System.out.println("inside inner if");
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
    	}
    	else
    	{
    		boolean found=false;
    		Document check_doc=Inverted_file.find(filter).first();
    		if(check_doc!=null)
    			found=true;
    		if(!found)
    		{
    			Inverted_file.insertOne(document);
        		Inverted_file.updateOne(filter, change);
    		}
    		else
    		{
    			if(!check_doc.get("token_info").toString().contains(temp_doc0.toString()))
    				 Inverted_file.updateOne(filter, change);
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

	    AggregateIterable<Document> h = frontier_collection.aggregate(Arrays.asList(
			    Aggregates.sort(new Document("Priority", 1)),
			    Aggregates.lookup("Domain", "Domain_FK", "Domain", "mydomain"),
			    Aggregates.sort(new Document("mydomain.Domain_Constraint", 1)),
			    Aggregates.group("$Domain_FK"),
			    Aggregates.limit(50)
	    ));
//	    Aggregates.group("Domain_FK")
//	    Aggregates.match(Filters.ne("mydomain.Domain_Constraint",4))
	    for (Document doc : h) {
		    System.out.println(doc.toJson());
	    }
	    Document unvisited_link = frontier_collection.findOneAndUpdate(filter, updateOperationDocument, new FindOneAndUpdateOptions().sort(sort_doc));
        if (unvisited_link != null)
            return unvisited_link;
        return null;
    }

	public ArrayList<Document> getLinksFromFrontier() {
		ArrayList<Document> links_arr = new ArrayList<>();
		AggregateIterable<Document> links_it = frontier_collection.aggregate(Arrays.asList(
				Aggregates.lookup("Domain", "Domain_FK", "Domain", "mydomain"),
				Aggregates.sort(new Document("Priority", 1).append("mydomain.Domain_Constraint", 1)),
				Aggregates.match(Filters.and(Filters.lte("mydomain.Domain_Constraint", 1000), Filters.eq("Visited", false))),
				Aggregates.limit(50)
		));
		//set links to onwork(null)
		Bson newValue = new Document("Visited", null);
		Bson updateOperationDocument = new Document("$set", newValue);
		for (Document doc : links_it) {
			links_arr.add(doc);
			Bson filter = new Document("_id", doc.getString("_id"));
			frontier_collection.updateOne(filter, updateOperationDocument);
		}
		return links_arr;
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
	        domain_collection.deleteMany(document);
	        //todo use aggregate
	        FindIterable<Document> docs = seed_collection.find();
	        for (Document doc : docs) {
		        Document link_doc = doc;
		        link_doc.append("Priority", 1);
		        link_doc.append("Offset", 0).append("Domain_FK", doc.getString("Domain_FK"));
		        Document domain_doc = new Document("Domain", doc.getString("Domain_FK")).append("Domain_Constraint", 0);
		        frontier_collection.insertOne(link_doc);
		        domain_collection.insertOne(domain_doc);
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
	        //reset domain constraint
	        newValue = new Document("Domain_Constraint", -1);
	        filter = new Document("Domain_Constraint", new Document("$gt", 1));
	        updateOperationDocument2 = new Document("$inc", newValue);
	        domain_collection.updateMany(filter, updateOperationDocument2);
        }
    }

    /*private void resetRobot() {
        Bson newValue = new Document("Updated", false);
        Bson filter = new Document();
        Bson updateOperationDocument = new Document("$set", newValue);
        robots_collection.updateMany(filter, updateOperationDocument);
    }*/

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

	public Document getRobot(String home_url) {
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
	    Bson newValue = new Document("Priority", priority).append("Offset", offset);
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

/////farah

    public Document findInInvertedFile(String s) {
        Bson filter = eq("_id", s);
        Document doc = Inverted_file.find(filter).first();
        return doc;
    }

	public void addManyUrlToFrontier(ArrayList<Document> frontier_links) {
		try {

			//ordered = false to continue inserting rest of docs if duplicate key exception happens
			frontier_collection.insertMany(frontier_links, new InsertManyOptions().ordered(false));//TODO use async driver in insertion and update
		} catch (Exception e) {

		}
	}

	public void addManyDomain(ArrayList<Document> domain_links) {
		try {

			//ordered = false to continue inserting rest of docs if duplicate key exception happens
			domain_collection.insertMany(domain_links, new InsertManyOptions().ordered(false));//TODO use async driver in insertion and update
		} catch (Exception e) {

		}
	}

	public void incDomainPriority(String domain) {
		Bson filter = new Document("Domain", domain);
		Bson newValue = new Document("Domain_Constraint", 1);
		Bson updateOperationDocument = new Document("$inc", newValue);
		domain_collection.updateOne(filter, updateOperationDocument);
	}

   /* public Document findInQueryFile(String s) {
        Bson filter = eq("_id", s);
        Document doc = queryResult_collection.find(filter).first();
        return doc;
    }*/


}