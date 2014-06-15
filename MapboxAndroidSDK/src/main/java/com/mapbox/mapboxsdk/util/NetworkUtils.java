/**
 * @author Brad Leege <bleege@gmail.com>
 * Created on 2/15/14 at 3:26 PM
 */

package com.mapbox.mapboxsdk.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.views.MapView;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkHttpClient;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URL;

public class NetworkUtils implements MapboxConstants {
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static HttpURLConnection getHttpURLConnection(final URL url) {
        return getHttpURLConnection(url, null, null);
    }

    public static HttpURLConnection getHttpURLConnection(final URL url, final ResponseCache cache) {
        return getHttpURLConnection(url, cache, null);
    }

    public static HttpURLConnection getHttpURLConnection(final URL url, final ResponseCache cache, final SSLSocketFactory sslSocketFactory) {
        OkHttpClient client = new OkHttpClient();
        if (cache != null) {
            client.setResponseCache(cache);
        }
        if (sslSocketFactory != null) {
            client.setSslSocketFactory(sslSocketFactory);
        }
        HttpURLConnection connection = client.open(url);
        connection.setRequestProperty("User-Agent", MapboxConstants.USER_AGENT);
        return connection;
    }

    public static ResponseCache getResponseCache(final File cacheDir, final int maxSize) throws IOException {
        return new HttpResponseCache(cacheDir, maxSize);
    }

    public static String parseUrlForTile(final MapView mapView, final MapTile aTile, boolean hdpi) {
        String url = MAPBOX_BASE_URL + mapView.mapId + "/{z}/{x}/{y}{2x}.png";

        return url.replace("{z}", String.valueOf(aTile.getZ()))
                .replace("{x}", String.valueOf(aTile.getX()))
                .replace("{y}", String.valueOf(aTile.getY()))
                .replace("{2x}", hdpi ? "@2x" : "@2x");
    }

}
