package xyz.tomclarke.brainybird.android;

import com.choosemuse.libmuse.Eeg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class Calibration implements Serializable {
        
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
