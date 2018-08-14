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

    public JsonLogParser() {
        logs = new ArrayList<>();
    }

    public JsonLogParser(String jsonMessage) {
        logs = genLogList(jsonMessage);
    }

    public List<List<String>> getLogs() {
        return logs;
    }

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

    private void readJson(JsonReader reader) throws IOException {
        reader.beginArray();
        while(reader.hasNext()) {
            logs.add(readLogs(reader));
        }
        reader.endArray();
    }

    private List<String> readLogs(JsonReader reader) throws IOException {
        List<String> text = new ArrayList<>();
        reader.beginObject();
        while(reader.hasNext()) {
            String name = reader.nextName();
            if(name.contentEquals("System")) {
                text.add(reader.nextString());
                break;
            } else if(name.contentEquals("Server")) {
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
