package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gitlet.Main.getfilewherewesaveHeadCommitName;

public class Commit implements Serializable {

    Commit(String message) throws IOException {
        _message = message;
        if (shaname == null) {
            setShaname();
        }
        if (!Utils.join("./.gitlet/Commits", shaname).exists()) {
            setupthisCommit();
        } else {
            thisCommit = Utils.join("./.gitlet/Commits", shaname);
        }
    }

    private void setShaname() {
        String currentCommitIDnum = Utils.readContentsAsString(LATESTCOMMITID);
        shaname = Utils.sha1(currentCommitIDnum.getBytes());
    }

    public void setupthisCommit() throws IOException {
        thisCommit = new File("./.gitlet/Commits", shaname);
        thisCommit.mkdir();
        setTime();
        setParent();
        setMessage();
    }

    private void setMessage() throws IOException {
        File messagefile = new File(thisCommit, "Message");
        messagefile.createNewFile();
        Utils.writeContents(messagefile, _message);
    }

    private void setParent() throws IOException {
        _parentShaname =
                Utils.readContentsAsString(
                        getfilewherewesaveHeadCommitName());
        File parentfile = new File(thisCommit, "Parent");
        parentfile.createNewFile();
        Utils.writeContents(parentfile, _parentShaname);
    }


    private void setTime() throws IOException {
        SimpleDateFormat sdf =
                new SimpleDateFormat(
                        "EEE MMM d kk:mm:ss yyyy Z");
        if (thisisthefirst()) {
            Date date = new Date(0);
            commitTime = sdf.format(date);
        } else {
            Date date = new Date();
            commitTime = sdf.format(date);
        }
        File timefile = new File(thisCommit, "Time");
        timefile.createNewFile();
        Utils.writeContents(timefile, commitTime);
    }

    /** cwd. */
    static final File GITLET = Utils.join(".", ".gitlet");
    /** cwd. */
    static final File STAGEDFILES = Utils.join(".gitlet", "Staging Area");
    /** cwd. */
    static final File REMOVEDFILES = new File(".gitlet", "Removed Files");
    /** cwd. */
    static final File LATESTCOMMITID = new File(GITLET, "Latest Commit ID");


    void commitFiles() throws Exception {
        String[] stagedfilesList = STAGEDFILES.list();
        if (stagedfilesList.length == 0
                && !thisisthefirst()
                && REMOVEDFILES.list().length == 0) {
            System.out.print("No changes added to the commit.");
            return;
        }
        copyLastCommit();
        for (int i = 0; i < stagedfilesList.length; i++) {
            File stagedfile =
                    Utils.join("./.gitlet/Staging Area",
                            stagedfilesList[i]);
            String stagedfileContent = Utils.readContentsAsString(stagedfile);

            File committedFile = new File(thisCommit, stagedfilesList[i]);
            committedFile.createNewFile();
            Utils.writeContents(committedFile, stagedfileContent);
            stagedfile.delete();
        }
    }

    public boolean thisisthefirst() {
        return shaname.equals(Utils.sha1(zero.getBytes()));
    }

    private void copyLastCommit() throws IOException {
        if (!thisisthefirst()) {
            String[] lastcommitedfilesList = this.getParent().list();
            ArrayList<String> removedFilesNames
                    = new ArrayList<String>(
                            List.of(REMOVEDFILES.list()));
            if (lastcommitedfilesList != null) {
                for (int i = 0; i < lastcommitedfilesList.length; i++) {
                    if (!lastcommitedfilesList[i].equals("Parent")
                            && !lastcommitedfilesList[i].equals("Time")
                            && !lastcommitedfilesList[i].equals("Message")
                            && !removedFilesNames
                            .contains
                                    (lastcommitedfilesList[i])) {
                        File oldcommittedFile =
                                Utils.join("./.gitlet/Commits/"
                                        + _parentShaname,
                                        lastcommitedfilesList[i]);
                        String oldcommittedfileContent =
                                Utils.readContentsAsString(
                                        oldcommittedFile);

                        File newcommittedFile =
                                new File(thisCommit,
                                        lastcommitedfilesList[i]);
                        newcommittedFile.createNewFile();
                        Utils.writeContents(
                                newcommittedFile,
                                oldcommittedfileContent);
                    }
                }
            }
            for (int i = 0; i < removedFilesNames.size(); i++) {
                Utils.join(REMOVEDFILES, removedFilesNames.get(i)).delete();
            }
        }
    }

    File getFile() {
        return thisCommit;
    }

    File getParent() {
        _parentShaname = Utils.
                readContentsAsString(
                        Utils.join(thisCommit, "Parent"));
        return Utils.join(".gitlet/Commits", _parentShaname);
    }

    Commit getParentasCommit() throws IOException {
        _parentShaname = Utils.
                readContentsAsString(
                Utils.join(thisCommit, "Parent"));
        return new Commit(_parentShaname);
    }

    String getmessage() {
        return _message; }

    void checkoutFileName(String fileName) {
        if (!Utils.join(thisCommit, fileName).exists()) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            File versionweWant = Utils.join(thisCommit, fileName);
            String contentfromVersion =
                    Utils.readContentsAsString(versionweWant);
            File versionincwd = Utils.join(".", fileName);
            Utils.writeContents(versionincwd, contentfromVersion);
        }
    }

    public String getZero() {
        return zero;
    }

    /** cwd. */
    private String _message;

    /** cwd. */
    private String _parentShaname;

    public String getparentshaname() {
        return _parentShaname;
    }

    /** cwd. */
    private File thisCommit;

    public File getthiscommit() {
        return thisCommit;
    }

    /** cwd. */
    private String commitTime;

    public String getCommittime() {
        return commitTime;
    }

    /** cwd. */
    private String shaname;

    public String getshaname() {
        return shaname;
    }

    /** cwd. */
    private String zero = "0";
}
