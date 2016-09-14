package br.com.maiconribeiro.mysunshineapp.sync;

import android.app.SearchManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.BaseColumns;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import br.com.maiconribeiro.mysunshineapp.model.City;

/**
 * Created by maiconwillianribeiro on 12/09/16.
 */
public class BuscarCidadesPorNome extends AsyncTask<String, Void, Cursor> {

    private static final String[] sAutocompleteColNames = new String[] {
            BaseColumns._ID,                         // necessary for adapter
            SearchManager.SUGGEST_COLUMN_TEXT_1      // the full search term
    };

    private final String URL_BASE = "http://api.openweathermap.org/data/2.5/find?";

    private final String QUERY_PARAM = "q";

    private final String ID_PARAM = "APPID";

    private final String ID_VALUE = "42d13b377d61960f0f34f716c57a98d6";

    private final String DAYS_PARAM = "cnt";

    private final String DAYS_VALUE = "10";

    private final String TYPE_PARAM = "type";

    private final String TYPE_VALUE = "like";

    private final String FORMAT_PARAM = "mode";

    private final String FORMAT_VALUE = "json";

    private URL url = null;

    @Override
    protected Cursor doInBackground(String... params) {

        List<City> cityList = new ArrayList<City>();

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }

        this.createUrl(params);

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String cidades = null;

        try {

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                cidades = null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                cidades = null;
            }

            cidades = buffer.toString();

        } catch (IOException e) {

            cidades = null;

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }

        MatrixCursor cursor = new MatrixCursor(sAutocompleteColNames);

        try {
            cityList = getCityList(cidades);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for(int index = 0; index < cityList.size(); index++){
            String term = cityList.get(index).getName();
            Object[] row = new Object[] { index, term };
            cursor.addRow(row);
        }


        return cursor;

    }

    private void createUrl(String... params) {

        try {

            Uri urlServico = Uri.parse(URL_BASE).buildUpon().appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, FORMAT_VALUE)
                    .appendQueryParameter(TYPE_PARAM, TYPE_VALUE)
                    .appendQueryParameter(DAYS_PARAM, DAYS_VALUE)
                    .appendQueryParameter(ID_PARAM, ID_VALUE)
                    .build();

            //http://api.openweathermap.org/data/2.5/find?mode=json&type=like&q=Roma&cnt=10&APPID=42d13b377d61960f0f34f716c57a98d6

            Log.i("UrlCities", urlServico.toString());
            url = new URL(urlServico.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public static List<City> getCityList(String data) throws JSONException {
        JSONObject jObj = new JSONObject(data);
        JSONArray jArr = jObj.getJSONArray("list");

        List<City> cityList = new ArrayList<City>();

        for (int i=0; i < jArr.length(); i++) {
            JSONObject obj = jArr.getJSONObject(i);

            String name = obj.getString("name");
            String id = obj.getString("id");

            JSONObject sys = obj.getJSONObject("sys");
            String country = sys.getString("country");

            City c = new City(id,name,country);

            cityList.add(c);
        }

        return cityList;
    }
}

