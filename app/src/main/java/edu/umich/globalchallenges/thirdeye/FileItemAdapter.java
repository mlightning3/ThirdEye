package edu.umich.globalchallenges.thirdeye;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * This holds all the information of one file that lives on the server so that it can be used in a
 * RecycleView.
 */
public class FileItemAdapter extends RecyclerView.Adapter<FileItemAdapter.FileItemViewHolder> {

    private final List<FileItem> fileItemList;
    private final OnRecycleItemInteractionListener listener;
    private final Fragment parentFragment;

    FileItemAdapter(Fragment parentFragment, @NonNull OnRecycleItemInteractionListener listener) {
        fileItemList = new ArrayList<>();
        this.listener = listener;
        this.parentFragment = parentFragment;
    }

    @NonNull
    @Override
    public FileItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item_view, parent, false);
        return new FileItemViewHolder(view, parentFragment, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull FileItemViewHolder holder, int position) {
        holder.setData(fileItemList.get(position));
        holder.setPosition(position);
    }

    @Override
    public int getItemCount() {
        return fileItemList.size();
    }

    /**
     * Add a FileItem to our list that will be displayed to the user
     * @param fileItem Item with information from server
     */
    public void addItem(FileItem fileItem) {
        if(fileItemList.contains(fileItem)) {
            return;
        }
        fileItemList.add(fileItem);
        notifyDataSetChanged();
    }

    /**
     * Get an item from the list of all FileItems
     * @param position Location in the list we want to get our item
     * @return The item, or null if not a valid position
     */
    public FileItem getItem(int position) {
        if(position >= 0 && position < getItemCount()) {
            return fileItemList.get(position);
        } else {
            return null;
        }
    }

    /**
     * Class defining the how the items will be viewed
     */
    public class FileItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final View view;
        private Fragment parentFragment;
        private OnRecycleItemInteractionListener listener;
        private int position;

        FileItemViewHolder(View view, Fragment parentFragment, OnRecycleItemInteractionListener listener) {
            super(view);
            this.view = view;
            this.parentFragment = parentFragment;
            this.listener = listener;
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        void setData(FileItem fileItem) {
            ((TextView) view.findViewById(R.id.date)).setText(fileItem.getDate());
            ((TextView) view.findViewById(R.id.filename)).setText(fileItem.getFilename());
            ImageView image = (ImageView) view.findViewById(R.id.image);
            if(fileItem.isDownloadable()) {
                String url = "http://stream.pi:5000/media/" + fileItem.getFilename();
                //Glide.with(parentFragment).load(url).into(image); // TODO: Fix this boi (probably only needs the library to update and we be good, but we will see)
            }
        }

        void setPosition(int position) {
            this.position = position;
        }

        @Override
        public void onClick(View view) {
            listener.onRecycleItemClick(position);
        }

        @Override
        public boolean onLongClick(View v) {
            listener.onRecycleItemLongClick(position);
            return true;
        }
    }
}
