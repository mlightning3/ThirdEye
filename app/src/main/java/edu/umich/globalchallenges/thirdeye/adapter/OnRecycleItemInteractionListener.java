package edu.umich.globalchallenges.thirdeye.adapter;

/**
 * Interface for the different types of interaction events that may happen to a list
 */
public interface OnRecycleItemInteractionListener{

    void onRecycleItemClick(int position);

    void onRecycleItemLongClick(int position);
}
