package edu.umich.globalchallenges.thirdeye;

/**
 * Used to allow for any fragments to communicate with main activity to request messages to be sent
 * to the user.
 */

public interface FragmentCommManager {

    /**
     * Displays a snackbar with a message
     *
     * @param message The message we want displayed
     */
    void snack_message(String message);

    /**
     * Displays a toast with a message
     *
     * @param message The message we want displayed
     */
    void toast_message(String message);
}
