package edu.umich.globalchallenges.thirdeye;

import android.util.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JsonLogParser {

    private List<List<String>> logs;

    /**
     * Simple constructor
     */
    public JsonLogParser() {
        logs = new ArrayList<>();
    }

    /**
     * Constructor that builds logs
     * @param jsonMessage
     */
    public JsonLogParser(String jsonMessage) {
        logs = genLogList(jsonMessage);
    }

    /**
     * Gives an array of all the logs sent from server
     * @return ArrayList of logs with log type in spot 0 for each item
     */
    public List<List<String>> getLogs() {
        return logs;
    }

    /**
     * Gives an array of all the logs sent from server
     * @param jsonMessage
     * @return ArrayList of logs with log type in spot 0 for each item
     */
    public List<List<String>> genLogList(String jsonMessage){
        logs = new ArrayList<>();
        try {
            InputStream in = new ByteArrayInputStream(jsonMessage.getBytes("UTF-8"));
            JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            try {
                readJson(reader);
            } finally {
                reader.close();
            }
        }
        catch (IOException e) {
            // Swallow exception and don't worry about it
        }
        return logs;
    }

    /**
     * Handles adding lines to the logs from JSON message
     * @param reader
     * @throws IOException
     */
    private void readJson(JsonReader reader) throws IOException {
        reader.beginArray();
        while(reader.hasNext()) {
            logs.add(readLogs(reader));
        }
        reader.endArray();
    }

    /**
     * Builds a line of the logs
     * @param reader
     * @return ArrayList from one log with log type in spot 0
     * @throws IOException
     */
    private List<String> readLogs(JsonReader reader) throws IOException {
        List<String> text = new ArrayList<>();
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if(name.contentEquals("System")) {
                text.add("System");
                text.add(reader.nextString());
                break;
            } else if(name.contentEquals("Server")) {
                text.add("Server");
                text.add(reader.nextString());
                break;
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return text;
    }
}
