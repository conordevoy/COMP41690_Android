package com.eventapp.eventapp;

import android.app.Fragment;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ListEvents extends Fragment {

    //This is declared in the class so it can be accessed by the inner public class
    private EventAdapter eventLists;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_list_events, container, false);

        // Get ListView object from xml
        ListView listView = (ListView) rootView.findViewById(R.id.activity_list);

        // Defined Array values to show in ListView


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        eventLists = new EventAdapter(getActivity(), R.layout.event_list_entry, new ArrayList<EventListing>());


        // Assign adapter to ListView
        listView.setAdapter(eventLists);

        //FetchEventInfo fetch = new FetchEventInfo();
        //fetch.execute("hi");
        return rootView;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.get_info){

            getEventInfo();
            return true;
        }
        Log.e("trying", "to do it");
        return super.onOptionsItemSelected(item);

    }

    public void getEventInfo(){
        //Here we will create a new async FetchEventInfo class and tell it to do it's stuff
        FetchEventInfo fetch = new FetchEventInfo();
        fetch.execute("test");
    }




    //The class represents an asynchronous task to be carried out when the activity is loaded
    //In here we must define the URL, perform a JSON request with the parameters and parse it.
    //objects that have been defined in the parent class can be accessed here!
    public class FetchEventInfo extends AsyncTask<String, Void, EventListing[]> {

        private final String LOG_TAG = FetchEventInfo.class.getSimpleName();

        @Override
        protected EventListing[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String eventJsonStr = null;




            try {

                //Querying test URL:
                final String testurl = "http://api.eventful.com/json/events/search?app_key=p3tDfpd3dKGs2HBD&sort_order=popularity&image_sizes=large,block250&location=Dublin";
                Uri builtUri = Uri.parse(testurl);


                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());

                //URL url = new URL("http://http://api.openweathermap.org/data/2.5/forecast/daily?id=524901&mode=json&units=metric&ctn=7");

                // Create the request to Eventful, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                eventJsonStr = readStream(inputStream);

                //read
                Log.v(LOG_TAG, "Event JSON String: " + eventJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try{
                return getEventInfoFromJson(eventJsonStr);
            }
            catch(JSONException e){
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }


            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }


        public EventListing[] getEventInfoFromJson(String eventInfo) throws JSONException{
            JSONObject eventJson = new JSONObject(eventInfo);
            JSONObject eventlistings = eventJson.getJSONObject("events");
            JSONArray events = eventlistings.getJSONArray("event");
            EventListing[] resultStrs = new EventListing[events.length()];

            for(int i = 0; i < events.length(); i++) {

                String img_url;
                JSONObject currEvent = events.getJSONObject(i);
                Log.e(LOG_TAG, currEvent.toString());
                EventListing newEvent = new EventListing(currEvent.getString("title"));

                if (currEvent.isNull("image")){
                    Log.v(LOG_TAG, "is null");
                    img_url = "";
                }
                else{
                    img_url = currEvent.getJSONObject("image").getJSONObject("large").getString("url");
                    newEvent.setBitmapFromURL(img_url);
                }
                resultStrs[i] = newEvent;
            }
            return resultStrs;
        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            StringBuffer data = new StringBuffer("");
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException");
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return data.toString();
        }

        @Override
        protected void onPostExecute(EventListing[] results) {
            Log.e(LOG_TAG, "Got here");

            if (results != null) {
                eventLists.clear();
                //mForecastAdapter.addAll(result);
                for (int i = 0; i < results.length; i++) {
                    eventLists.add(results[i]);
                }
                //Log.e(LOG_TAG, "First Item: " + eventLists.getItem(0));
            }

        }
    }
}
