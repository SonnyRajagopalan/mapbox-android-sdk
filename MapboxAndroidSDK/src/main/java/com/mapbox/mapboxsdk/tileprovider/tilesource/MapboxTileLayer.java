package com.mapbox.mapboxsdk.tileprovider.tilesource;

import android.content.Context;
import com.google.common.base.Strings;
import com.mapbox.mapboxsdk.constants.MapboxConstants;
import com.mapbox.mapboxsdk.views.util.constants.MapViewConstants;

import java.util.Locale;

/**
 * A convenience class to initialize tile layers that use Mapbox services.
 * Underneath, this initializes a WebSourceTileLayer, but provides conveniences
 * for retina tiles, initialization by ID, and loading over SSL.
 */
public class MapboxTileLayer extends TileJsonTileLayer implements MapViewConstants, MapboxConstants {
    private static final String TAG = "MapboxTileLayer";
    private String mId;

    /**
     * Initialize a new tile layer, directed at a hosted Mapbox tilesource.
     *
     * @param context Mapview Context
     * @param pId a valid mapid, of the form account.map
     */
    public MapboxTileLayer(Context context, String pId) {
        this(context, pId, true);
    }

    public MapboxTileLayer(Context context, String pId, boolean enableSSL) {
        super(context, pId, pId, enableSSL);
    }

    @Override
    protected void initialize(String pId, String aUrl, boolean enableSSL) {
        mId = pId;
        super.initialize(pId, aUrl, enableSSL);
    }

    @Override
    public TileLayer setURL(final String aUrl) {
        if (!Strings.isNullOrEmpty(aUrl) &&
                !aUrl.toLowerCase(Locale.US).contains("http://") && !aUrl.toLowerCase(Locale.US)
                .contains("https://")) {
            super.setURL(MAPBOX_BASE_URL + aUrl + "/{z}/{x}/{y}{2x}.png");
        } else {
            super.setURL(aUrl);
        }
        return this;
    }

    @Override
    protected String getBrandedJSONURL() {
        return String.format("http%s://api.tiles.mapbox.com/v3/%s.json%s", (mEnableSSL ? "s" : ""),
                mId, (mEnableSSL ? "?secure" : ""));
    }

    public String getCacheKey() {
        return mId;
    }
}
