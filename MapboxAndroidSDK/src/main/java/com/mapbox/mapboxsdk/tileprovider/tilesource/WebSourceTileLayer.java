package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import com.mapbox.mapboxsdk.tileprovider.MapTile;
import com.mapbox.mapboxsdk.tileprovider.MapTileCache;
import com.mapbox.mapboxsdk.tileprovider.modules.MapTileDownloader;
import com.mapbox.mapboxsdk.tileprovider.util.StreamUtils;
import com.mapbox.mapboxsdk.util.NetworkUtils;
import com.mapbox.mapboxsdk.util.constants.UtilConstants;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import com.squareup.picasso.Picasso;

/**
 * An implementation of {@link TileLayer} that pulls tiles from the internet.
 */
public class WebSourceTileLayer extends TileLayer {
    private static final String TAG = "WebSourceTileLayer";
    private Context context;
    private String aUrl;

    // Tracks the number of threads active in the getBitmapFromURL method.
    private AtomicInteger activeThreads = new AtomicInteger(0);
    protected boolean mEnableSSL = false;

    public WebSourceTileLayer(Context context, final String pId, final String url) {
        this(context, pId, url, false);
    }

    public WebSourceTileLayer(Context context, final String pId, final String url, final boolean enableSSL) {
        super(pId, url);
        this.context = context;
        initialize(pId, url, enableSSL);
    }

    private boolean checkThreadControl() {
        return activeThreads.get() == 0;
    }

    @Override
    public TileLayer setURL(final String aUrl) {
        this.aUrl = aUrl;
        if (aUrl.contains(String.format("http%s://", (mEnableSSL ? "" : "s")))) {
            super.setURL(aUrl.replace(String.format("http%s://", (mEnableSSL ? "" : "s")),
                    String.format("http%s://", (mEnableSSL ? "s" : ""))));
        } else {
            super.setURL(aUrl);
        }
        return this;
    }

    protected void initialize(String pId, String aUrl, boolean enableSSL) {
        mEnableSSL = enableSSL;
        this.setURL(aUrl);
    }

    /**
     * Gets a list of Tile URLs used by this layer for a specific tile.
     *
     * @param aTile a map tile
     * @param hdpi a boolean that indicates whether the tile should be at 2x or retina size
     * @return a list of tile URLS
     */
    public String[] getTileURLs(final MapTile aTile, boolean hdpi) {
        String url = getTileURL(aTile, hdpi);
        if (url != null) {
            return new String[] { url };
        }
        return null;
    }

    /**
     * Get a single Tile URL for a single tile.
     *
     * @param aTile a map tile
     * @param hdpi a boolean that indicates whether the tile should be at 2x or retina size
     * @return a list of tile URLs
     */
    public String getTileURL(final MapTile aTile, boolean hdpi) {
        return parseUrlForTile(mUrl, aTile, hdpi);
    }

    protected String parseUrlForTile(String url, final MapTile aTile, boolean hdpi) {
        return url.replace("{z}", String.valueOf(aTile.getZ()))
                .replace("{x}", String.valueOf(aTile.getX()))
                .replace("{y}", String.valueOf(aTile.getY()))
                .replace("{2x}", hdpi ? "@2x" : "");
    }

    private static final Paint compositePaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Bitmap compositeBitmaps(final Bitmap source, Bitmap dest) {
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, 0, 0, compositePaint);
        return dest;
    }

    @Override
    public Drawable getDrawableFromTile(final MapTileDownloader downloader, final MapTile aTile, boolean hdpi) {
        if (downloader.isNetworkAvailable()) {

            String url = getTileURL(aTile, hdpi);
            Log.i(getClass().getCanonicalName(), "getDrawableFromTile() with url = '" + url + "'");

            ImageView imageView = new ImageView(context);
            Picasso.with(context).load(url).into(imageView);
            return imageView.getDrawable();

/*
            TilesLoadedListener listener = downloader.getTilesLoadedListener();

            String[] urls = getTileURLs(aTile, hdpi);
            CacheableBitmapDrawable result = null;
            Bitmap resultBitmap = null;
            if (urls != null) {
                MapTileCache cache = downloader.getCache();
                if (listener != null) {
                    listener.onTilesLoadStarted();
                }
                for (final String url : urls) {
                    Bitmap bitmap = getBitmapFromURL(url, cache);
                    if (bitmap == null) {
                        continue;
                    }
                    if (resultBitmap == null) {
                        resultBitmap = bitmap;
                    } else {
                        resultBitmap = compositeBitmaps(bitmap, resultBitmap);
                    }
                }
                if (resultBitmap != null) {
                    //get drawable by putting it into cache (memory and disk)
                    result = cache.putTileBitmap(aTile, resultBitmap);
                }
                if (checkThreadControl()) {
                    if (listener != null) {
                        listener.onTilesLoaded();
                    }
                }
            }

            if (result != null) {
                TileLoadedListener listener2 = downloader.getTileLoadedListener();
                result = listener2 != null ? listener2.onTileLoaded(result) : result;
            }

            return result;
*/
        } else {
            if (UtilConstants.DEBUGMODE) {
                Log.d(TAG, "Skipping tile " + aTile.toString() + " due to NetworkAvailabilityCheck.");
            }
        }
        return null;
    }

    /**
     * Requests and returns a bitmap object from a given URL, using aCache to decode it.
     *
     * @param url the map tile url. should refer to a valid bitmap resource.
     * @param aCache a cache, an instance of MapTileCache
     * @return the tile if valid, otherwise null
     */
    public Bitmap getBitmapFromURL(final String url, final MapTileCache aCache) {
        // We track the active threads here, every exit point should decrement this value.
        Log.i(getClass().getCanonicalName(), "getBitmapFormURL() called with url = '" + url + "'");
        activeThreads.incrementAndGet();
        InputStream in = null;
        OutputStream out = null;

        if (TextUtils.isEmpty(url)) {
            activeThreads.decrementAndGet();
            return null;
        }

        try {
            HttpURLConnection connection = NetworkUtils.getHttpURLConnection(new URL(url));
            in = connection.getInputStream();

            if (in == null) {
                if (UtilConstants.DEBUGMODE) {
                    Log.d(TAG, "No content downloading MapTile: " + url);
                }
                return null;
            }

            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, StreamUtils.IO_BUFFER_SIZE);
            StreamUtils.copy(in, out);
            out.flush();
            final byte[] data = dataStream.toByteArray();
            return aCache.decodeBitmap(data, null);
        } catch (final Throwable e) {
            if (UtilConstants.DEBUGMODE) {
                Log.d(TAG, "Error downloading MapTile: " + url + ":" + e);
            }
        } finally {
            StreamUtils.closeStream(in);
            StreamUtils.closeStream(out);
            activeThreads.decrementAndGet();
        }
        return null;
    }
}
