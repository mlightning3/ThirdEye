package edu.umich.globalchallenges.thirdeye.json;

import android.util.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses the database information sent by the server into a list of strings so an
 * activity/fragment is able to display the information
 */
public class JsonDatabaseParser {

    private List<List<String>> database;

    /**
     * Creates an empty database array
     */
    public JsonDatabaseParser() {
        database = new ArrayList<>();
    }

    /**
     * Creates a database array based on a given JSON message
     * @param jsonMessage The JSON message
     * @throws IOException
     */
    public JsonDatabaseParser(String jsonMessage) throws IOException {
        database = genDatabaseArray(jsonMessage);
    }

    /**
     * Gives the database array
     * @return Database array
     */
    public List<List<String>> getDatabase() {
        return database;
    }

    /**
     * Builds the database array from a JSON message
     * @param jsonMessage The JSON message
     * @return Database array
     * @throws IOException
     */
    public List<List<String>> genDatabaseArray(String jsonMessage) throws IOException {
        InputStream in = new ByteArrayInputStream(jsonMessage.getBytes("UTF-8"));
        database = new ArrayList<>();
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            readJson(reader);
        } finally {
            reader.close();
        }
        return database;
    }

    /**
     * Handles adding lines to the database from JSON message
     * @param reader
     * @throws IOException
     */
    private void readJson(JsonReader reader) throws IOException {
        reader.beginArray();
        while(reader.hasNext()) {
            database.add(readDatabase(reader));
        }
        reader.endArray();
    }

    /**
     * Builds a line of the database
     * @param reader
     * @return
     * @throws IOException
     */
    private List<String> readDatabase(JsonReader reader) throws IOException {
        String date = "null";
        String filename = "null";
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if(name.contentEquals("date")) {
                date = reader.nextString();
            } else if(name.contentEquals("filename")) {
                filename = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        List<String> text = new ArrayList<>();
        text.add(date);
        text.add(filename);
        return text;
    }
}
