package xyz.tomclarke.brainybird.android;

import android.util.Log;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;

public class BrainTap {
    
    private final GameView gameView;
    private boolean over = false;
    
    public BrainTap(GameView gameView, Game game) {
        this.gameView = gameView;
        BrainyApplication app = (BrainyApplication) game.getApplication();
        app.getMuse().registerDataListener(new DataListener(), MuseDataPacketType.EEG);
    }
    
    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    private void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        
        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                checkValue(p.getEegChannelValue(Eeg.EEG3));
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
    
    private void checkValue(double value) {
        Log.d("Value: ", String.valueOf(value));
        if (over) {
            if (value < 840) {
                over = false;
            }
        } else if (value > 850) {
            over = true;
            gameView.getPlayer().onTap();
        }
    }
    
    private class DataListener extends MuseDataListener {
        
        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            BrainTap.this.receiveMuseDataPacket(p, muse);
        }
        
        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
        }
        
    }
    
}
