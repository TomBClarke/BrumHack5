package xyz.tomclarke.brainybird.android;

import android.app.Application;
import android.nfc.Tag;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class BrainyApplication extends Application {
    
    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "TestLibMuseAndroid";
    
    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;
    
    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;
    
    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     *
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private MuseConnectionListener connectionListener;
    
    /**
     * To save data to a file, you should use a MuseFileWriter.  The MuseFileWriter knows how to
     * serialize the data packets received from the headband into a compact binary format.
     * To read the file back, you would use a MuseFileReader.
     */
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();
    
    /**
     * We don't want file operations to slow down the UI, so we will defer those file operations
     * to a handler on a separate thread.
     */
    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();
    
    private ArrayList<ConnectionListener> connectionListeners = new ArrayList<>();
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    
    public Muse getMuse() {
        return muse;
    }
    
    //--------------------------------------
    // Lifecycle / Connection code
    
    
    @Override
    public void onCreate() {
        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);
        
        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());
        
        // Register a listener to receive connection state changes.
        connectionListener = new MuseConnectionListener() {
    
            @Override
            public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
                BrainyApplication.this.receiveMuseConnectionPacket(p, muse);
            }
            
        };
        
        manager.startListening();
    }
    
    public void connect() {
        if (connectionState != ConnectionState.DISCONNECTED) {
            return;
        }
        updateConnectionState(ConnectionState.CONNECTING);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                doConnect();
            }
        }).start();
    }
        
    public void doConnect() {
        while (muse == null || muse.getConnectionState() != ConnectionState.CONNECTED) {
            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.
    
            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();
    
            List<Muse> availableMuses = manager.getMuses();
    
            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            } else {
        
                // Cache the Muse that the user has selected.
                muse = availableMuses.get(0);
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.
                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
        
                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();
            }
            
            manager.startListening();
            
            SystemClock.sleep(5_000);
            do {
                SystemClock.sleep(2_000);
            } while (muse != null && muse.getConnectionState() == ConnectionState.CONNECTING);
            
            SystemClock.sleep(10_000);
        }
        updateConnectionState(ConnectionState.CONNECTED);
    }
    
    
    //--------------------------------------
    // Listeners
    
    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     * @param p     A packet containing the current and prior connection states
     * @param muse  The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        
        final ConnectionState current = p.getCurrentConnectionState();
        
        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);
        
        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            saveFile();
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
        }
    }
    
    /**
     * Flushes all the data to the file and closes the file writer.
     */
    private void saveFile() {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override public void run() {
                    MuseFileWriter w = fileWriter.get();
                    // Annotation strings can be added to the file to
                    // give context as to what is happening at that point in
                    // time.  An annotation can be an arbitrary string or
                    // may include additional AnnotationData.
                    w.addAnnotationString(0, "Disconnected");
                    w.flush();
                    w.close();
                }
            });
        }
    }
    
    public void updateConnectionState(ConnectionState connectionState) {
        this.connectionState = connectionState;
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.onConnectionStateChange(connectionState);
        }
    }
    
    public void subscribeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
        connectionListener.onConnectionStateChange(connectionState);
    }
    
}
