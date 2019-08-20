package edu.umich.globalchallenges.thirdeye.adapter;

/**
 * Class that holds the information for each item in the list
 */
public class FileItem implements Comparable<FileItem> {
    private String filename;
    private String date;
    private boolean isDownloadable;

    public FileItem(String filename, String date, boolean isDownloadable) {
        this.filename = filename;
        this.date = date;
        this.isDownloadable = isDownloadable;
    }

    public String getFilename() {
        return filename;
    }

    public String getDate() {
        return date;
    }

    public boolean isDownloadable() {
        return isDownloadable;
    }

    @Override
    public int compareTo(FileItem o) {
        return this.toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof FileItem) {
            return this.date.equals(((FileItem) object).date) && this.filename.equals(((FileItem) object).filename);
        }
        return false;
    }

    @Override
    public String toString() {
        return date + " " + filename;
    }
}
