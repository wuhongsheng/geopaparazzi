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

package eu.geopaparazzi.library.core.activities;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.database.GPLog;

/**
 * @author javacodegeeks
 */
public class ProximityIntentReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1000;

    @Override
    public void onReceive( Context context, Intent intent ) {

        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        Boolean entering = intent.getBooleanExtra(key, false);

        if (entering) {
            if (GPLog.LOG)
                GPLog.addLogEntry(getClass().getSimpleName(), "entering proximity radius"); //$NON-NLS-1$


            // TODO upgrade to 6
//            NotificationManager notificationManager = (NotificationManager) context
//                    .getSystemService(Context.NOTIFICATION_SERVICE);
//
//            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
//
//            Notification notification = createNotification();
//            notification.setLatestEventInfo(context, context.getString(R.string.proximity_alert),
//                    context.getString(R.string.approaching_poi), pendingIntent);
//            notificationManager.notify(NOTIFICATION_ID, notification);
//
//            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//            locationManager.removeProximityAlert(pendingIntent);
//            context.unregisterReceiver(this);
        } else {
            if (GPLog.LOG)
                GPLog.addLogEntry(getClass().getSimpleName(), "exiting proximity radius"); //$NON-NLS-1$
        }

    }

    private static Notification createNotification() {

        Notification notification = new Notification();

        notification.icon = R.drawable.current_position;
        notification.when = System.currentTimeMillis();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notification.defaults |= Notification.DEFAULT_SOUND;

        notification.ledARGB = Color.RED;
        notification.ledOnMS = 1500;
        notification.ledOffMS = 1500;

        return notification;
    }

}
