package org.wikipedia.savedpages;

import android.os.Parcel;
import android.os.Parcelable;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.Page;

import java.io.*;
import java.util.Date;

import static org.wikipedia.util.FileUtil.delete;
import static org.wikipedia.util.FileUtil.writeToFile;
import static org.wikipedia.util.FileUtil.readJSONFile;

public class SavedPage implements Parcelable {
    public static final SavedPageDatabaseTable DATABASE_TABLE = new SavedPageDatabaseTable();

    private final PageTitle title;
    private final Date timestamp;

    public SavedPage(PageTitle title, Date timestamp) {
        this.title = title;
        this.timestamp = timestamp;
    }

    public SavedPage(PageTitle title) {
        this(title, new Date());
    }

    public PageTitle getTitle() {
        return title;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SavedPage)) {
            return false;
        }
        SavedPage other = (SavedPage) o;
        return title.equals(other.title);
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }

    @Override
    public String toString() {
        return "SavedPage{"
                + "title=" + title
                + ", timestamp=" + timestamp.getTime()
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(getTitle(), flags);
        dest.writeLong(getTimestamp().getTime());
    }

    private SavedPage(Parcel in) {
        this.title = in.readParcelable(PageTitle.class.getClassLoader());
        this.timestamp = new Date(in.readLong());
    }

    public static final Creator<SavedPage> CREATOR
            = new Creator<SavedPage>() {
        public SavedPage createFromParcel(Parcel in) {
            return new SavedPage(in);
        }

        public SavedPage[] newArray(int size) {
            return new SavedPage[size];
        }
    };

    /**
     * Gets the base directory for all saved pages.
     * (will be something like /data/data/org.wikimedia/files/savedpages)
     * @return Base directory for saved pages, inside the app's private storage space.
     */
    public static String getSavedPagesDir() {
        return WikipediaApp.getInstance().getFilesDir() + "/savedpages";
    }

    /**
     * Gets the base directory for this page's saved files.
     * The name of the directory is the MD5 sum of the page title (to account for special
     * or unicode characters in the title).
     * @return Base directory for the saved files for this page, inside the
     * overall base directory for saved pages.
     */
    String getBaseDir() {
        // Make the folder name be based on the complete PageTitle object,
        // which includes title, site info, etc.
        String dir = getSavedPagesDir() + "/" + title.getIdentifier();
        (new File(dir)).mkdirs();
        return dir;
    }

    /**
     * Gets a File object that represents the JSON contents of this page.
     * @return File object used for reading/writing page contents.
     */
    private File getContentsFile() {
        return new File(getBaseDir() + "/content.json");
    }

    /**
     * Gets the file that has the URL mappings in JSON of this page.
     * @return File object used for reading/writing page contents.
     */
    private File getUrlMapFile() {
        return new File(getBaseDir() + "/urls.json");
    }

    /**
     * Writes the contents of this page to storage.
     * (Each page is stored in a separate directory)
     * @param page Page object with the contents of the page to be written.
     * @throws IOException
     */
    public void writeToFileSystem(Page page) throws IOException {
        writeToFile(getContentsFile(), page.toJSON());
    }

    /**
     * Writes a map of all URL mappings to a file inside the saved page directory.
     * @param jsonObject contains mapping of URLs (originals to file paths)
     * @throws IOException
     */
    public void writeUrlMap(JSONObject jsonObject) throws IOException {
        writeToFile(getUrlMapFile(), jsonObject);
    }

    /**
     * Reads the contents of this page from storage.
     * @return Page object with the contents of the page.
     * @throws IOException
     * @throws JSONException
     */
    public Page readFromFileSystem() throws IOException, JSONException {
        return new Page(readJSONFile(getContentsFile()));
    }

    public JSONObject readUrlMapFromFileSystem() throws IOException, JSONException {
        return readJSONFile(getUrlMapFile());
    }

    /**
     * Deletes any stored files that are associated with this page.
     * (Removes the entire directory and any files in it)
     */
    public void deleteFromFileSystem() {
        delete(new File(getBaseDir()), true);
    }
}
