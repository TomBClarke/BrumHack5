package xyz.tomclarke.brainybird.android;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;

public class Brainibration extends Dialog {
    
    private static Eeg[] EEGS = new Eeg[] { Eeg.EEG1, Eeg.EEG2, Eeg.EEG3, Eeg.EEG4 };
    
    private boolean calibrationStarted = false;
    private boolean stateOpen = true;
    private int timesFlipped = 0;
    private final EnumMap<Eeg, ArrayList<Double>> open = new EnumMap<>(Eeg.class);
    private final EnumMap<Eeg, ArrayList<Double>> openAvgs = new EnumMap<>(Eeg.class);
    private final EnumMap<Eeg, ArrayList<Double>> blink = new EnumMap<>(Eeg.class);
    private final EnumMap<Eeg, ArrayList<Double>> blinkHighs = new EnumMap<>(Eeg.class);
    private CalibrationListener callback;
    
    public Brainibration(Context context) {
        super(context);
    }
    
    public Brainibration(Context context, int themeResId) {
        super(context, themeResId);
    }
    
    protected Brainibration(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.brainibration);
    }
    
    public void calibrate(Muse muse, CalibrationListener callback) {
        if (muse == null) {
            throw new IllegalArgumentException("'muse' is null.");
        }
        if (calibrationStarted) {
            throw new IllegalStateException("Calibration started already.");
        }
        calibrationStarted = true;
        
        show();
        
        this.callback = callback;
        
        for (Eeg eeg : EEGS) {
            open.put(eeg, new ArrayList<Double>());
            openAvgs.put(eeg, new ArrayList<Double>());
            blink.put(eeg, new ArrayList<Double>());
            blinkHighs.put(eeg, new ArrayList<Double>());
        }
        
        muse.registerDataListener(new MuseDataListener() {
    
            @Override
            public void receiveMuseDataPacket(MuseDataPacket museDataPacket, Muse muse) {
                Brainibration.this.receiveMuseDataPacket(museDataPacket, muse);
            }
    
            @Override
            public void receiveMuseArtifactPacket(MuseArtifactPacket museArtifactPacket, Muse muse) {
                
            }
    
        }, MuseDataPacketType.EEG);
        
        doDelayedFlips();
    }
    
    private void doDelayedFlips() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                flipStateOpen();
                if (timesFlipped++ <= 6) {
                    doDelayedFlips();
                } else {
                    dismiss();
                    callback.onCalibrationResult(calculateCalibration());
                }
            }
        }, stateOpen ? 3000 : 1000);
    }
    
    private Calibration calculateCalibration() {
        if (EEGS.length == 0) {
            throw new IllegalStateException();
        }
        
        Double greatestDiffAverageOpen = null;
        Double greatestDiffLowestBlink = null;
        Eeg greatestDiffEeg = null;
        for (Eeg eeg : EEGS) {
            double averageOpen = getAverage(openAvgs, eeg);
            double lowestBlink = getLowest(blinkHighs, eeg);
            Log.d("a low", String.valueOf(averageOpen));
            Log.d("a high", String.valueOf(lowestBlink));
            if (greatestDiffAverageOpen == null ||
                    lowestBlink - averageOpen >= greatestDiffLowestBlink - greatestDiffAverageOpen) {
                Log.d("rep", eeg.name());
                greatestDiffAverageOpen = averageOpen;
                greatestDiffLowestBlink = lowestBlink;
                greatestDiffEeg = eeg;
            }
        }
        Log.d("low", String.valueOf(greatestDiffAverageOpen));
        Log.d("high", String.valueOf(greatestDiffLowestBlink));
        
        double percentClosen = (greatestDiffLowestBlink - greatestDiffAverageOpen) * 0.2;
        
        return new Calibration(greatestDiffEeg,
                greatestDiffAverageOpen + percentClosen,
                greatestDiffLowestBlink - percentClosen);
    }
    
    private void flipStateOpen() {
        EnumMap<Eeg, ArrayList<Double>> enumMap = stateOpen ? open : blink;
        EnumMap<Eeg, ArrayList<Double>> enumMapStore = stateOpen ? openAvgs : blinkHighs;
        
        for (Eeg eeg : EEGS) {
            double value = stateOpen ? getAverage(enumMap, eeg) : getHighest(enumMap, eeg);
            Log.d("value", String.valueOf(value));
            enumMapStore.get(eeg).add(value);
        }
        
        stateOpen = !stateOpen;
        
        TextView textView = (TextView) findViewById(R.id.ibration_blink);
        textView.setText(stateOpen ? R.string.open_eyes : R.string.blink_wink);
        Log.d("do", textView.getText().toString());
    }
    
    private double getAverage(EnumMap<Eeg, ArrayList<Double>> enumMap, Eeg eeg) {
        ArrayList<Double> doubles = enumMap.get(eeg);
        double total = 0;
        for (Double d : doubles) {
            total += d;
        }
        return total / doubles.size();
    }
    
    private double getHighest(EnumMap<Eeg, ArrayList<Double>> enumMap, Eeg eeg) {
        double highest = 0;
        for (Double d : enumMap.get(eeg)) {
            if (d > highest) {
                highest = d;
            }
        }
        return highest;
    }
    
    private double getLowest(EnumMap<Eeg, ArrayList<Double>> enumMap, Eeg eeg) {
        double lowest = Double.MAX_VALUE;
        for (Double d : enumMap.get(eeg)) {
            if (d < lowest) {
                lowest = d;
            }
        }
        return lowest;
    }
    
    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    private void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        switch (p.packetType()) {
            case EEG:
                storeValues(p);
                break;
            case ACCELEROMETER:
            case ALPHA_RELATIVE:
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }
    
    private void storeValues(MuseDataPacket p) {
        for (Eeg eeg : EEGS) {
            double value = p.getEegChannelValue(eeg);
            if (Double.isNaN(value)) continue;
            if (stateOpen) {
                open.get(eeg).add(value);
            } else {
                blink.get(eeg).add(value);
            }
        }
    }
    
    public interface CalibrationListener {
        
        void onCalibrationResult(Calibration calibration);
        
    }
    
    public static class Calibration implements Serializable {
        
        public static final long serialVersionUID = 754338848373432L;
        public static final Calibration DEFAULT = new Calibration(Eeg.EEG3, 840, 850);
        
        private Eeg eeg;
        private double low;
        private double high;
        
        public Calibration(Eeg eeg, double low, double high) {
            this.eeg = eeg;
            this.low = low;
            this.high = high;
        }
        
        public Eeg getEeg() {
            return eeg;
        }
        
        public double getLow() {
            return low;
        }
        
        public double getHigh() {
            return high;
        }
        
        @Override
        public String toString() {
            return "Calibration { eeg = " + eeg + ", low = " + low + ", high = " + high + " }";
        }
    
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(eeg);
            out.writeDouble(low);
            out.writeDouble(high);
        }
        
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            eeg = (Eeg) in.readObject();
            low = in.readDouble();
            high = in.readDouble();
        }
        
    }
    
}
