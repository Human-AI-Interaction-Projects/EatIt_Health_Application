package org.tensorflow.lite.examples.classification.tflite;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

class RetrieveFeedTask extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        if (!isCancelled()) // when the thread has been cancelled
        {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                Log.d("Mike", url.toString());
                connection.setRequestMethod("GET");    // GRABS THE URL TO OBTAIN INFORMATION FROM THE URL
                Log.d("Mike", "Getmethod");
                connection.setRequestProperty("x-app-id", "c0fab9f9");  // set the headers for required keys and id
                connection.setRequestProperty("x-app-key", "cbde0a2dd183b2900e236b5c7f52b6b8");
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                Log.d("Mike", "BufferedReader");
                // get the buffered reader data
                StringBuilder total = new StringBuilder();
                for (String line; (line = in.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                // parses the string as a JSONElement
                JsonElement jelement = new JsonParser().parse(total.toString());    // PARSES THE JSON LINE AND TURNS IT TO A JSON ELEMENT
                JsonObject jobject = jelement.getAsJsonObject();    // CONVERTS THE JSON ELEMENT INTO A JSON OBJECT
                JsonArray jarray = jobject.get("branded").getAsJsonArray();    // grabs the list object from the json object

                // Grab Json Element and turn to a integer to track calories
                String caloriesString = jarray.get(2).getAsJsonObject().get("nf_calories").toString();
                connection.disconnect();
                return caloriesString;
            } catch (Exception e) {
                this.exception = e;
                e.printStackTrace();
            }
        }
        return null;
    }
}