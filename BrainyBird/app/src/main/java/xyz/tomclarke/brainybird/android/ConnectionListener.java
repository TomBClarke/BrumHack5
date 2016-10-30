package xyz.tomclarke.brainybird.android;

import com.choosemuse.libmuse.ConnectionState;

public interface ConnectionListener {
    
    void onConnectionStateChange(ConnectionState connectionState);
    
}
