/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.core.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.plugin.PluginLoaderListener;
import eu.geopaparazzi.library.plugin.menu.MenuLoader;
import eu.geopaparazzi.library.plugin.types.IMenuEntry;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.TextRunnable;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.webproject.WebProjectsListActivity;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.ui.activities.tantomapurls.TantoMapurlsActivity;
import eu.geopaparazzi.core.utilities.Constants;
import gov.nasa.worldwind.AddWMSDialog;
import gov.nasa.worldwind.ogc.OGCBoundingBox;
import gov.nasa.worldwind.ogc.wms.WMSCapabilityInformation;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;

/**
 * Activity for export tasks.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportActivity extends AppCompatActivity implements IActivityStupporter {

    public static final int START_REQUEST_CODE = 666;

    private SparseArray<IMenuEntry> menuEntriesMap = new SparseArray<>();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_import);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.core.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        MenuLoader loader = new MenuLoader(this);
        loader.addListener(new PluginLoaderListener<MenuLoader>() {
            @Override
            public void pluginLoaded(MenuLoader loader) {
                addMenuEntries(loader.getEntries());
            }
        });
        loader.connect();
    }

    protected void addMenuEntries(List<IMenuEntry> entries) {
        menuEntriesMap.clear();
        int code = START_REQUEST_CODE + 1;
        for (final eu.geopaparazzi.library.plugin.types.IMenuEntry entry : entries) {
            final Context context = this;
            Button button = new Button(this);
            button.setText(entry.getLabel());
            entry.setRequestCode(code);
            menuEntriesMap.put(code, entry);
            LinearLayout container = (LinearLayout) findViewById(R.id.scrollView);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            container.addView(button, lp);
            button.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    entry.onClick(ImportActivity.this);
                }
            });
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IMenuEntry entry = menuEntriesMap.get(requestCode);
        if (entry != null) {
            entry.onActivityResultExecute(requestCode, resultCode, data);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }


}
