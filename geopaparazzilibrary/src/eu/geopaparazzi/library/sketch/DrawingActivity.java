package eu.geopaparazzi.library.sketch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.sketch.brush.Brush;
import eu.geopaparazzi.library.sketch.brush.PenBrush;
import eu.geopaparazzi.library.sketch.commands.DrawingPath;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;

/**
 * Main drawing activity.
 * 
 * <p>Adapted for geopaparazzi.</p>
 * 
 * @author almondmendoza (http://www.tutorialforandroid.com/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DrawingActivity extends Activity implements View.OnTouchListener {
    private static final int MENU_SAVE = Menu.FIRST;
    private static final int MENU_CANCEL = 2;

    private DrawingSurface drawingSurface;
    private DrawingPath currentDrawingPath;
    private Paint currentPaint;
    private Paint previewPaint;

    private Button redoBtn;
    private Button undoBtn;

    private Brush currentBrush;
    private int currentColor;
    private float currentWidth;

    private Spinner colorSpinner;
    private Spinner widthSpinner;
    private String imageSavePath;
    private double lon = -9999.0;
    private double lat = -9999.0;
    private double elevation = -9999.0;
    private File imageFile;

    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            imageSavePath = extras.getString(LibraryConstants.PREFS_KEY_PATH);
            lon = extras.getDouble(LibraryConstants.LONGITUDE);
            lat = extras.getDouble(LibraryConstants.LATITUDE);
            elevation = extras.getDouble(LibraryConstants.ELEVATION);
        } else {
            throw new RuntimeException("Not implemented yet...");
        }

        currentBrush = new PenBrush();
        currentWidth = 3f;

        drawingSurface = (DrawingSurface) findViewById(R.id.drawingSurface);
        drawingSurface.setOnTouchListener(this);
        drawingSurface.previewPath = new DrawingPath();
        drawingSurface.previewPath.path = new Path();

        redoBtn = (Button) findViewById(R.id.redoButton);
        undoBtn = (Button) findViewById(R.id.undoButton);

        colorSpinner = (Spinner) findViewById(R.id.colorspinner);
        ArrayAdapter< ? > colorSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_colornames,
                android.R.layout.simple_spinner_item);
        colorSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorSpinnerAdapter);
        colorSpinner.setSelection(0);
        colorSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                onClick(colorSpinner);
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

        widthSpinner = (Spinner) findViewById(R.id.widthspinner);
        ArrayAdapter< ? > widthSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.array_widths,
                android.R.layout.simple_spinner_item);
        widthSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        widthSpinner.setAdapter(widthSpinnerAdapter);
        widthSpinner.setSelection(2);
        widthSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
            public void onItemSelected( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
                onClick(widthSpinner);
            }
            public void onNothingSelected( AdapterView< ? > arg0 ) {
            }
        });

        setCurrentPaint();
        drawingSurface.previewPath.paint = previewPaint;

        redoBtn.setEnabled(false);
        undoBtn.setEnabled(false);
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MENU_SAVE, 1, R.string.save).setIcon(android.R.drawable.ic_menu_save);
        menu.add(Menu.NONE, MENU_CANCEL, 2, R.string.cancel).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_SAVE:
            try {
                saveImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        case MENU_CANCEL:
            finish();
            return true;
        default: {
        }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void saveImage() throws IOException {
        Date currentDate = new Date();
        String currentDatestring = LibraryConstants.TIMESTAMPFORMATTER.format(currentDate);
        String imageName = "SKETCH_" + currentDatestring + ".jpg";
        String imagePropertiesName = "SKETCH_" + currentDatestring + ".properties";
        File imageSaveFolder = ResourcesManager.getInstance(this).getMediaDir();

        File imagePropertiesFile = null;

        if (imageSavePath == null || imageSavePath.length() == 0) {
            imageFile = new File(imageSaveFolder, imageName);
            imagePropertiesFile = new File(imageSaveFolder, imagePropertiesName);
        } else {
            imageFile = new File(imageSavePath);
            String propFileName = FileUtilities.getNameWithoutExtention(imageFile) + ".properties";
            imageSaveFolder = imageFile.getParentFile();
            imagePropertiesFile = new File(imageSaveFolder, propFileName);
        }

        if (!imageSaveFolder.exists()) {
            if (!imageSaveFolder.mkdirs()) {
                Runnable runnable = new Runnable(){
                    public void run() {
                        finish();
                    }
                };
                Utilities.messageDialog(this, getString(R.string.cantcreate_img_folder), runnable);
                return;
            }
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(imageFile);
            drawingSurface.getBitmap().compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            if (out != null)
                out.close();
        }

        // create props file
        BufferedWriter bW = null;
        try {
            bW = new BufferedWriter(new FileWriter(imagePropertiesFile));
            bW.write("latitude=");
            bW.write(String.valueOf(lat));
            bW.write("\nlongitude=");
            bW.write(String.valueOf(lon));
            bW.write("\naltim=");
            bW.write(String.valueOf(elevation));
            bW.write("\nutctimestamp=");
            bW.write(currentDatestring);
        } finally {
            if (bW != null)
                bW.close();
        }

        Intent intent = getIntent();
        intent.putExtra(LibraryConstants.PREFS_KEY_PATH, imageFile.getAbsolutePath());
        intent.putExtra(LibraryConstants.LATITUDE, lat);
        intent.putExtra(LibraryConstants.LONGITUDE, lon);
        intent.putExtra(LibraryConstants.ELEVATION, elevation);
        setResult(Activity.RESULT_OK, intent);

        finish();
    }

    private void setCurrentPaint() {
        checkColor();
        checkWidth();

        currentPaint = new Paint();
        currentPaint.setDither(true);
        currentPaint.setColor(currentColor);
        currentPaint.setAlpha(255);
        currentPaint.setStyle(Paint.Style.STROKE);
        currentPaint.setStrokeJoin(Paint.Join.ROUND);
        currentPaint.setStrokeCap(Paint.Cap.ROUND);
        currentPaint.setStrokeWidth(currentWidth);

        previewPaint = new Paint();
        previewPaint.setDither(true);
        previewPaint.setColor(currentColor);
        previewPaint.setAlpha(255);
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeJoin(Paint.Join.ROUND);
        previewPaint.setStrokeCap(Paint.Cap.ROUND);
        previewPaint.setStrokeWidth(currentWidth);

    }

    public boolean onTouch( View view, MotionEvent motionEvent ) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        // if (Debug.D) {
        //            Logger.i(this, "Drawing: " + x + "/" + y); //$NON-NLS-1$//$NON-NLS-2$
        // }
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            DrawingSurface.isDrawing = true;

            currentDrawingPath = new DrawingPath();
            currentDrawingPath.paint = currentPaint;
            currentDrawingPath.path = new Path();
            currentBrush.mouseDown(currentDrawingPath.path, x, y);
            currentBrush.mouseDown(drawingSurface.previewPath.path, x, y);

        } else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            DrawingSurface.isDrawing = true;
            currentBrush.mouseMove(currentDrawingPath.path, x, y);
            currentBrush.mouseMove(drawingSurface.previewPath.path, x, y);

        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

            currentBrush.mouseUp(drawingSurface.previewPath.path, x, y);
            drawingSurface.previewPath.path = new Path();
            drawingSurface.addDrawingPath(currentDrawingPath);

            currentBrush.mouseUp(currentDrawingPath.path, x, y);

            undoBtn.setEnabled(true);
            redoBtn.setEnabled(false);

        }

        return true;
    }

    public void onClick( View view ) {
        int id = view.getId();
        if (id == R.id.colorspinner || id == R.id.widthspinner) {
            setCurrentPaint();
            drawingSurface.previewPath.paint = previewPaint;
        } else if (id == R.id.undoButton) {
            drawingSurface.undo();
            if (drawingSurface.hasMoreUndo() == false) {
                undoBtn.setEnabled(false);
            }
            redoBtn.setEnabled(true);
        } else if (id == R.id.redoButton) {
            drawingSurface.redo();
            if (drawingSurface.hasMoreRedo() == false) {
                redoBtn.setEnabled(false);
            }

            undoBtn.setEnabled(true);
        }
        // else if (id == R.id.saveBtn) {
        // final Activity currentActivity = this;
        // Handler saveHandler = new Handler(){
        // @Override
        // public void handleMessage( Message msg ) {
        // final AlertDialog alertDialog = new AlertDialog.Builder(currentActivity).create();
        // alertDialog.setTitle("Saved 1");
        // alertDialog.setMessage("Your drawing had been saved :)");
        // alertDialog.setButton("OK", new DialogInterface.OnClickListener(){
        // public void onClick( DialogInterface dialog, int which ) {
        // return;
        // }
        // });
        // alertDialog.show();
        // }
        // };
        // new ExportBitmapToFile(this, saveHandler, drawingSurface.getBitmap()).execute();
        // } else if (id == R.id.circleBtn) {
        // currentBrush = new CircleBrush();
        // } else if (id == R.id.pathBtn) {
        // currentBrush = new PenBrush();
        // }
    }

    private void checkWidth() {
        Object selectedItem = widthSpinner.getSelectedItem();
        float newWidth = Float.parseFloat(selectedItem.toString());
        currentWidth = newWidth;
    }

    private void checkColor() {
        Object selectedItem = colorSpinner.getSelectedItem();
        String newColorStr = selectedItem.toString();
        // if (newColorStr.equals("red")) {
        // currentColor = 0xFF0000FF;
        // currentColor = 0xFFC1C1C1;
        // } else {
        currentColor = Color.parseColor(newColorStr.trim());
        // }
    }

}