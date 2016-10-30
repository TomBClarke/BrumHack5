package xyz.tomclarke.brainybird.android;

import android.app.Application;
import android.nfc.Tag;
import android.os.Handler;
import android.util.Log;

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
    private ConnectionListener connectionListener;
    
    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     *
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;
    
    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */
    private final double[] eegBuffer = new double[6];
    private final double[] alphaBuffer = new double[6];
    private final double[] accelBuffer = new double[3];
    
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
        connectionListener = new ConnectionListener();
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener();
        
        manager.startListening();
    }
    
    public void connect() {
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
            muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
            muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
            muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
            muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
            muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
            muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);
            
            // Initiate a connection to the headband and stream the data asynchronously.
            muse.runAsynchronously();
        }
    
        manager.startListening();
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
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        
        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(eegBuffer,p);
//                Log.d(TAG, "eegBuffer");
//                Log.d(TAG, Arrays.toString(eegBuffer));
                break;
            case ACCELEROMETER:
                assert(accelBuffer.length >= n);
                getAccelValues(p);
//                Log.d(TAG, "accelBuffer");
//                Log.d(TAG, Arrays.toString(accelBuffer));
                break;
            case ALPHA_RELATIVE:
                assert(alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer,p);
//                Log.d(TAG, "alphaBuffer");
//                Log.d(TAG, Arrays.toString(alphaBuffer));
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }
    
    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
    }
    
    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     *
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }
    
    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
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
    
    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    
    class ConnectionListener extends MuseConnectionListener {
        
        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            BrainyApplication.this.receiveMuseConnectionPacket(p, muse);
        }
        
    }
    
    class DataListener extends MuseDataListener {
        
        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            BrainyApplication.this.receiveMuseDataPacket(p, muse);
        }
        
        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            BrainyApplication.this.receiveMuseArtifactPacket(p, muse);
        }
        
    }
    
}
