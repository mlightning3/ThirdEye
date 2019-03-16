package edu.umich.globalchallenges.thirdeye;

/**
 * Used to allow for any fragments to communicate with main activity to request wifi functions
 */

public interface FragmentWifiManager {

    /**
     * Sends request to connect to the wifi network with the camera streaming server.
     */
    boolean connected_to_network();

    /**
     * Tests if we are connected to the right network to view the camera stream.
     *
     * @return true if connected to the right network, false otherwise
     */
    void wifi_connect();

    /**
     * Disconnects the phone from the camera streaming computer's network
     */
    void wifi_disconnect();
}
