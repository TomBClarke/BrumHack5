package xyz.tomclarke.brainybird.android;

import android.util.Log;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;

public class BrainTap {
    
    private final GameView gameView;
    private final Calibration calibration;
    private boolean over = false;
    private final BrainyApplication app;
    
    public BrainTap(GameView gameView, Game game, Calibration calibration) {
        this.gameView = gameView;
        this.calibration = calibration;
        app = (BrainyApplication) game.getApplication();
        if (app.getMuse() != null) {
            
            app.getMuse().unregisterAllListeners();
            
            app.getMuse().registerDataListener(new MuseDataListener() {
        
                @Override
                public void receiveMuseDataPacket(MuseDataPacket museDataPacket, Muse muse) {
                    BrainTap.this.receiveMuseDataPacket(museDataPacket, muse);
                }
        
                @Override
                public void receiveMuseArtifactPacket(MuseArtifactPacket museArtifactPacket, Muse muse) {
            
                }
        
            }, MuseDataPacketType.EEG);
        }
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
                checkValue(p.getEegChannelValue(calibration.getEeg()));
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
            if (value < calibration.getLow()) {
                over = false;
            }
        } else if (value > calibration.getHigh()) {
            over = true;
            gameView.getPlayer().onTap();
        }
    }
    
}
