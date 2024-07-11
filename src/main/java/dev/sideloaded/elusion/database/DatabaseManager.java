package dev.sideloaded.elusion.database;

import com.mongodb.client.*;
import org.bson.Document;
import java.time.Instant;
import java.util.Date;

public class DatabaseManager {
    private static final String DATABASE_NAME = "elusionBot";
    private static final String COLLECTION_NAME = "users";
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> usersCollection;

    public static void connect(String connectionString) {
        try {
            mongoClient = MongoClients.create(connectionString);
            database = mongoClient.getDatabase(DATABASE_NAME);
            usersCollection = database.getCollection(COLLECTION_NAME);

            usersCollection.createIndex(new Document("discordID", 1));

            System.out.println("Connected to MongoDB successfully!");
        } catch (Exception e) {
            System.err.println("Error connecting to MongoDB: " + e.getMessage());
        }
    }

    public static void addOrUpdateUser(String discordID, String discordName, String whitelistType) {
    Document existingUser = getUser(discordID);
    if (existingUser == null) {
        Document user = new Document("discordID", discordID)
                .append("discordName", discordName)
                .append("whitelisted", false)
                .append("whitelistType", null)
                .append("invites", 0)
                .append("altsGenerated", 0)
                .append("credits", 0);
        usersCollection.insertOne(user);
    } else {
        updateUser(discordID, "discordName", discordName);
    }
}

public static void updateUserWhitelist(String discordID, boolean whitelisted, String whitelistType, Instant expirationTime) {
    Document update = new Document("whitelisted", whitelisted)
            .append("whitelistType", whitelistType)
            .append("whitelistExpiration", expirationTime != null ? Date.from(expirationTime) : null);
    
    usersCollection.updateOne(
            new Document("discordID", discordID),
            new Document("$set", update)
    );
}

public static void removeExpiredWhitelists() {
    usersCollection.updateMany(
            new Document("whitelistExpiration", new Document("$lt", new Date())),
            new Document("$set", new Document("whitelisted", false)
                    .append("whitelistType", null)
                    .append("whitelistExpiration", null))
    );
}

public static void updateUser(String discordID, String field, Object value) {
    usersCollection.updateOne(
            new Document("discordID", discordID),
            new Document("$set", new Document(field, value))
    );
}

public static Document getUser(String discordID) {
    return usersCollection.find(new Document("discordID", discordID)).first();
}

public static void incrementField(String discordID, String field) {
    usersCollection.updateOne(
            new Document("discordID", discordID),
            new Document("$inc", new Document(field, 1))
    );
}

public static void decrementField(String discordID, String field) {
    usersCollection.updateOne(
            new Document("discordID", discordID),
            new Document("$inc", new Document(field, -1))
    );
}

public static void addCredit(String discordID) {
    incrementField(discordID, "credits");
    incrementField(discordID, "invites");
}

public static void removeCredit(String discordID) {
    decrementField(discordID, "credits");
}

public static void close() {
    if (mongoClient != null) {
        mongoClient.close();
    }
}
}