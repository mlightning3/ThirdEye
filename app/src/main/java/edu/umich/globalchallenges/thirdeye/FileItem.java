package edu.umich.globalchallenges.thirdeye;

import android.view.View;
import android.widget.TextView;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

public class FileItem  extends AbstractFlexibleItem<FileItem.FileViewHolder> {

    private String filename;
    private String date;

    public FileItem(String date, String filename) {
        this.date = date;
        this.filename = filename;
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
        return R.layout.text_view;
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

        public FileViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            this.date = (TextView) view.findViewById(R.id.date);
            this.filename = (TextView) view.findViewById(R.id.filename);
        }
    }
}
