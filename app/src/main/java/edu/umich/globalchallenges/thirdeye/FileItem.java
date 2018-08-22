package edu.umich.globalchallenges.thirdeye;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * This holds all the information of one file that lives on the server so that it can be used in a
 * RecycleView.
 */
public class FileItem  extends AbstractFlexibleItem<FileItem.FileViewHolder> {

    private String filename;
    private String date;
    private boolean isDownloadable;
    private Fragment fragment;

    public FileItem(Fragment fragment, String date, String filename, boolean isDownloadable) {
        this.date = date;
        this.filename = filename;
        this.fragment = fragment;
        this.isDownloadable = isDownloadable;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof FileItem) {
            return this.date.equals(((FileItem) object).date) && this.filename.equals(((FileItem) object).filename);
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.file_item_view;
    }

    @Override
    public FileViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new FileViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, FileViewHolder holder, int position, List<Object> payloads) {
        holder.date.setText(date);
        holder.filename.setText(filename);
        holder.date.setEnabled(isEnabled());
        holder.filename.setEnabled(isEnabled());
        if(isDownloadable) {
            String url = "http://stream.pi:5000/media/" + filename;
            Glide.with(fragment).load(url).into(holder.image);
        }
    }

    @Override
    public String toString() {
        return date + " " + filename;
    }

    /**
     * Class defining the how the items will be viewed
     */
    public class FileViewHolder extends FlexibleViewHolder {

        public TextView date;
        public TextView filename;
        public ImageView image;

        public FileViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.date = (TextView) view.findViewById(R.id.date);
            this.filename = (TextView) view.findViewById(R.id.filename);
            this.image = (ImageView) view.findViewById(R.id.image);
        }
    }
}
