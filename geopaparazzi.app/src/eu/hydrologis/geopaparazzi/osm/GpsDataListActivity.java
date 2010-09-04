/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
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
package eu.hydrologis.geopaparazzi.osm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Gpx listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataListActivity extends ListActivity {
    private static final int MERGE_SELECTED = 1;

    private static List<String> colorList;
    private static List<String> widthsList;
    private MapItem[] gpslogItems;

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslogslist);
        getResourcesAndColors();

        handleNotes();

        List<MapItem> logsList = null;
        try {
            logsList = DaoGpsLog.getGpslogs();
            gpslogItems = (MapItem[]) logsList.toArray(new MapItem[logsList.size()]);
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }

        ArrayAdapter<MapItem> arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.gpslog_row, gpslogItems){

            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.gpslog_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.filename);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final MapItem item = gpslogItems[position];
                rowView.setBackgroundColor(Color.parseColor(item.getColor()));
                nameView.setText(item.getName());

                visibleView.setChecked(item.isVisible());
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        item.setVisible(isChecked);
                        item.setDirty(true);
                    }
                });

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
        // getListView().setTextFilterEnabled(true);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        Intent intent = new Intent(Constants.GPSLOG_PROPERTIES);
        intent.putExtra(Constants.PREFS_KEY_GPSLOG4PROPERTIES, gpslogItems[position]);
        startActivity(intent);
        finish();
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MERGE_SELECTED, 1, R.string.mainmenu_merge).setIcon(R.drawable.merge);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MERGE_SELECTED:
            try {
                mergeSelected();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void mergeSelected() throws IOException {
        List<MapItem> gpslogs = DaoGpsLog.getGpslogs();
        List<MapItem> selected = new ArrayList<MapItem>();
        for( MapItem mapItem : gpslogs ) {
            if (mapItem.isVisible()) {
                selected.add(mapItem);
            }
        }

        if (selected.size() < 2) {
            return;
        }

        long mainId = selected.get(0).getId();
        for( int i = 0; i < selected.size(); i++ ) {
            if (i == 0) {
                continue;
            }
            MapItem mapItem = selected.get(i);
            long id = mapItem.getId();
            DaoGpsLog.mergeLogs(id, mainId);
        }
        finish();

    }

    @Override
    protected void onPause() {
        try {
            boolean oneVisible = false;
            for( MapItem item : gpslogItems ) {
                if (item.isDirty()) {
                    DaoGpsLog.updateLogProperties(item.getId(), item.getColor(), item.getWidth(), item.isVisible(), null);
                    item.setDirty(false);
                }
                if (!oneVisible && item.isVisible()) {
                    oneVisible = true;
                }
            }
            DataManager.getInstance().setLogsVisible(oneVisible);
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    private void handleNotes() {
        // notes selection
        CheckBox notesView = (CheckBox) findViewById(R.id.notesvisible);
        notesView.setChecked(DataManager.getInstance().areNotesVisible());
        notesView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                DataManager.getInstance().setNotesVisible(isChecked);
            }
        });

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String color = preferences.getString(Constants.PREFS_KEY_NOTES_COLOR, "red"); //$NON-NLS-1$
        DataManager.getInstance().setNotesColor(color);
        final Spinner colorView = (Spinner) findViewById(R.id.notescolor_spinner);
        ArrayAdapter< ? > colorSpinnerAdapter = ArrayAdapter.createFromResource(GpsDataListActivity.this,
                R.array.array_colornames, android.R.layout.simple_spinner_item);
        colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorView.setAdapter(colorSpinnerAdapter);
        int colorIndex = colorList.indexOf(color);
        colorView.setSelection(colorIndex);
        colorView.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                Object selectedItem = colorView.getSelectedItem();
                String colorStr = selectedItem.toString();

                DataManager.getInstance().setNotesColor(colorStr);
                Editor editor = preferences.edit();
                editor.putString(Constants.PREFS_KEY_NOTES_COLOR, colorStr);
                editor.commit();
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

        final Spinner widthView = (Spinner) findViewById(R.id.noteswidthText);
        String width = preferences.getString(Constants.PREFS_KEY_NOTES_WIDTH, "5"); //$NON-NLS-1$
        DataManager.getInstance().setNotesWidth(Float.parseFloat(width));
        int widthIndex = widthsList.indexOf(width);
        ArrayAdapter< ? > widthSpinnerAdapter = ArrayAdapter.createFromResource(GpsDataListActivity.this, R.array.array_widths,
                android.R.layout.simple_spinner_item);
        widthSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widthView.setAdapter(widthSpinnerAdapter);
        widthView.setSelection(widthIndex);
        widthView.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                Object selectedItem = widthView.getSelectedItem();
                String widthStr = selectedItem.toString();

                DataManager.getInstance().setNotesWidth(Float.parseFloat(widthStr));
                Editor editor = preferences.edit();
                editor.putString(Constants.PREFS_KEY_NOTES_WIDTH, widthStr);
                editor.commit();
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

    }

    private void getResourcesAndColors() {
        if (colorList == null) {
            String[] colorArray = getResources().getStringArray(R.array.array_colornames);
            colorList = Arrays.asList(colorArray);
            String[] widthsArray = getResources().getStringArray(R.array.array_widths);
            widthsList = Arrays.asList(widthsArray);
        }

    }

}