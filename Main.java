package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Flynn Gray
 */
public class Main {
    /** cwd. */
    static final File CWD = new File(".");
    /** cwd. */
    static final File GITLET = new File(".", ".gitlet");
    /** cwd. */
    static final File STAGEDFILES = new File(".gitlet", "Staging Area");
    /** cwd. */
    static final File COMMITS = new File(".gitlet", "Commits");
    /** cwd. */
    private static final File ACTIVEBRANCH =
            new File(".gitlet", "Active Branch");
    /** cwd. */
    static final File REMOVEDFILES = new File(".gitlet", "Removed Files");
    /** cwd. */
    static final File LATESTCOMMITID = new File(GITLET,
            "Latest Commit ID");
    /** cwd. */
    static final File BRANCHES = new File(GITLET, "Branches");
    /** cwd. */
    static final File MASTERBRANCH = new File(BRANCHES, "master");



    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws Exception {
        if (firstcheck(args)) {
            return;
        }
        switch (args[0]) {
        case "init":
            init();
            break;
        case "add":
            add(args[1]);
            break;
        case "commit":
            if (secondcheck(args)) {
                return;
            }
            commit(args[1]);
            break;
        case "checkout":
            if (args.length > 2) {
                if (args[2].equals("++")) {
                    System.out.println("Incorrect operands");
                    return;
                }
            }
            if (args.length == 3) {
                checkoutFileName(args[2]);
            } else if (args.length == 4) {
                checkoutCommitID(args[1] + " " + args[3]);
            } else if (args.length == 2) {
                checkoutBranch(args[1]);
            }
            break;
        case "log":
            log();
            break;
        default:
            otherMain(args);
        }
    }

    private static void otherMain(String[] args) throws IOException {
        switch (args[0]) {
        case "rm":
            rm(args[1]);
            break;
        case "global-log":
            globallog();
            break;
        case "find":
            findcommit(args[1]);
            break;
        case "status":
            status();
            break;
        case "branch":
            branch(args[1]);
            break;
        case "rm-branch":
            rmbranch(args[1]);
            break;
        case "reset":
            reset(args[1]);
            break;
        case "merge":
            merge(args[1]);
            break;
        default:
            System.out.print("No command with that name exists.");
            return;
        }
    }

    private static boolean secondcheck(String[] args) {
        if (args.length == 1 || args[1].equals("")) {
            System.out.print("Please enter a commit message.");
            return true;
        }
        return false;
    }

    private static boolean firstcheck(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return true;
        }
        if (!args[0].equals("init") && !GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return true;
        }
        return false;
    }

    private static void merge(String branchName) throws IOException {
        File splitpointCommit = findsplitpoint(branchName);
        if (splitpointCommit.getName().equals(branchName)) {
            System.out.println("Given branch"
                    + " is an ancestor of the current branch.");
            return;
        }
        if (splitpointCommit.getName().equals(getheadcommit().getName())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
    }

    private static File findsplitpoint(String branchName) {
        ArrayList<String> parentlistactive = getparentlist(getheadcommit());
        File splitpoint = matchmostrecentancestor(branchName, parentlistactive);
        return splitpoint;
    }

    private static File matchmostrecentancestor(
            String branchName, ArrayList<String> parentlistactive) {
        File branchNamehead = getheadcommit(branchName);
        for (File currCommitFile = branchNamehead;
             !currCommitFile.getName().
                     equals("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
             currCommitFile = Utils.join(COMMITS,
                     Utils.readContentsAsString(
                             Utils.join(currCommitFile, "Parent")))) {
            if (parentlistactive.contains(currCommitFile.getName())) {
                return currCommitFile;
            }
        }
        return Utils.join(COMMITS,
                "b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
    }

    private static File getheadcommit(String branchName) {
        File branch = Utils.join(BRANCHES, branchName);
        String headcommitName =
                Utils.readContentsAsString(Utils.join(branch, "Head Commit"));
        return Utils.join(COMMITS, headcommitName);
    }

    private static ArrayList<String> getparentlist(File headcommit) {
        ArrayList<String> returnlist = new ArrayList<>();
        for (File currCommitFile = headcommit;
             !currCommitFile.getName().
                     equals("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
             currCommitFile = Utils.join(COMMITS,
                     Utils.readContentsAsString(
                             Utils.join(currCommitFile, "Parent")))) {
            returnlist.add(currCommitFile.getName());
        }
        return returnlist;
    }

    private static void reset(String commitID) throws IOException {

        File thisCommit = Utils.join(COMMITS, commitID);
        if (!thisCommit.exists()) {
            System.out.println("No commit with that id exists.");
            return;
        }

        if (untrackedfileincommitID(commitID)) {
            System.out.println("There is an "
                    + "untracked file in the way; delete it, "
                    + "or add and commit it first.");
            return;
        }

        String[] thisCommitFilesList = thisCommit.list();
        for (int i = 0; i < thisCommitFilesList.length; i++) {
            checkoutCommitID(commitID + " "
                    +
                    thisCommitFilesList[i]);
        }
        String[] currentHeadCommitFilesList =
                Utils.join(COMMITS,
                        Utils.readContentsAsString(
                                getfilewherewesaveHeadCommitName())).list();
        ArrayList<String> thisCommitFiles =
                new ArrayList<String>(List.of(thisCommitFilesList));
        for (int i = 0; i < currentHeadCommitFilesList.length; i++) {
            if (!thisCommitFiles.contains(currentHeadCommitFilesList)) {
                File filecwdversion = Utils.join(CWD,
                        currentHeadCommitFilesList[i]);
                if (filecwdversion.exists()) {
                    filecwdversion.delete();
                }
            }
        }
        clearStagingarea();
        Utils.writeContents(getfilewherewesaveHeadCommitName(), commitID);
    }

    private static void clearStagingarea() {
        String[] tempstagedfiles = STAGEDFILES.list();
        for (int i = 0; i < tempstagedfiles.length; i++) {
            File thisStagedfile =
                    Utils.join(STAGEDFILES, tempstagedfiles[i]);
            thisStagedfile.delete();
        }
    }


    private static boolean untrackedfileincommitID(String commitID) {
        File thisCommit = Utils.join(COMMITS, commitID);
        for (File thisFile : thisCommit.listFiles()) {
            if (!isastapleofeverycommit(thisFile.getName())) {
                File cWDversion = Utils.join(CWD, thisFile.getName());
                if (cWDversion.exists()) {
                    String cWDcontent =
                            Utils.readContentsAsString(cWDversion);
                    String thisFilecontent =
                            Utils.readContentsAsString(thisFile);
                    if (!cWDcontent.equals(thisFilecontent)) {
                        return true;
                    }
                }
                File hcVersion = Utils.join(getheadcommit(),
                        thisFile.getName());
                if (hcVersion.exists()) {
                    String hccontent =
                            Utils.readContentsAsString(hcVersion);
                    String thisFilecontent =
                            Utils.readContentsAsString(thisFile);
                    if (!hccontent.equals(thisFilecontent)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static File getheadcommit() {
        String hcname = Utils.
                readContentsAsString(getfilewherewesaveHeadCommitName());
        File headCommit = Utils.join(COMMITS, hcname);
        return headCommit;
    }


    private static void rmbranch(String branchName) {
        ArrayList<String> branchNames = new
                ArrayList<String>(List.of(BRANCHES.list()));
        if (!branchNames.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(Utils.readContentsAsString(ACTIVEBRANCH))) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        File thisbranch = Utils.join(BRANCHES, branchName);
        File thisbranchonlyfile = Utils.join(thisbranch, "Head Commit");
        thisbranchonlyfile.delete();
        thisbranch.delete();
    }

    private static void checkoutBranch(String branchName) throws IOException {
        if (checkbrancherrors(branchName)) {
            return;
        }
        File currentActiveHeadCommit = getheadcommit();
        File thisBranch = Utils.join(BRANCHES, branchName);
        File thisHeadCommit = Utils.join(thisBranch, "Head Commit");
        String thisHeadCommitname = Utils.readContentsAsString(thisHeadCommit);
        File thisCommit = Utils.join(COMMITS, thisHeadCommitname);
        ArrayList<String> checkedoutfilenames;
        if (thisCommit.list() != null) {
            gruntwork(thisCommit);

            for (int i = 0; i < thisCommit.list().length; i++) {
                String thisFile = thisCommit.list()[i];
                if (!isastapleofeverycommit(thisFile)) {
                    String commitIDfileName =
                            thisCommit.getName() + " " + thisFile;
                    checkoutCommitID(commitIDfileName);
                }
            }

        }
        if (currentActiveHeadCommit.list() != null) {
            for (int i = 0; i
                    < currentActiveHeadCommit.list().length; i++) {
                if (!Utils.join(thisCommit,
                        currentActiveHeadCommit.list()[i]).exists()
                        && !isastapleofeverycommit(
                                currentActiveHeadCommit.list()[i])) {
                    File verisonfromcwd =
                            Utils.join(CWD,
                                    currentActiveHeadCommit.list()[i]);
                    if (verisonfromcwd.exists()) {
                        verisonfromcwd.delete();
                    }
                }
            }
        }
        clearStagingarea();
        Utils.writeContents(ACTIVEBRANCH, branchName);
    }

    private static void gruntwork(File thisCommit) {
        for (File thisheadfile : thisCommit.listFiles()) {
            File cwdversion = Utils.join(CWD, thisheadfile.getName());
            if (cwdversion.exists()) {
                String cwdversioncontent =
                        Utils.readContentsAsString(cwdversion);
                String thisheadfilecontent =
                        Utils.readContentsAsString(thisheadfile);
                if (!thisheadfilecontent.equals(cwdversioncontent)) {
                    File currheadverison = Utils.join(getheadcommit(),
                            thisheadfile.getName());
                    if (!currheadverison.exists()) {
                        System.out.
                                print("There is an untracked "
                                        + "file in the way; "
                                        + "delete it, or add "
                                        +
                                        "and commit it first.");
                        return;
                    }
                    String currheadversioncontent =
                            Utils.readContentsAsString(currheadverison);
                    if (!currheadversioncontent.
                            equals(currheadversioncontent)) {
                        System.out.print("There is an untracked"
                                + " file in the way; "
                                + "delete it, or add and commit it first.");
                        return;
                    }
                }
            }
        }
    }

    private static boolean checkbrancherrors(String branchName) {
        if (Utils.readContentsAsString(ACTIVEBRANCH).equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return true;
        }
        ArrayList<String> branchNames =
                new ArrayList<String>(List.of(BRANCHES.list()));
        if (!branchNames.contains(branchName)) {
            System.out.println("No such branch exists.");
            return true;
        }
        return false;
    }

    private static boolean isastapleofeverycommit(String filename) {
        return filename.equals("Time")
                ||
                filename.equals("Parent")
                ||
                filename.equals("Message");
    }

    private static void branch(String branchName) {
        ArrayList<String> branchNames =
                new ArrayList<String>(List.of(BRANCHES.list()));
        if (branchNames.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        File newBranch = new File(BRANCHES, branchName);
        newBranch.mkdir();
        File headCommit = new File(newBranch, "Head Commit");
        String currHeadCommit =
                Utils.readContentsAsString(getfilewherewesaveHeadCommitName());
        Utils.writeContents(headCommit, currHeadCommit);
    }

    private static void status() {
        System.out.println("=== Branches ===");
        if (BRANCHES.list() != null) {
            for (int i = 0; i < BRANCHES.list().length; i++) {
                if (BRANCHES.list()[i].
                        equals(Utils.
                                readContentsAsString(ACTIVEBRANCH))) {
                    System.out.print("*");
                }
                System.out.println(BRANCHES.list()[i]);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        if (STAGEDFILES.list() != null) {
            for (int i = 0; i < STAGEDFILES.list().length; i++) {
                System.out.println(STAGEDFILES.list()[i]);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (REMOVEDFILES.list() != null) {
            for (int i = 0; i < REMOVEDFILES.list().length; i++) {
                System.out.println(REMOVEDFILES.list()[i]);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    private static void findcommit(String fileMessage) {
        boolean foundOne = false;
        File[] commitsList = COMMITS.listFiles();
        for (int i = 0; i < commitsList.length; i++) {
            if (!commitsList[i].
                    getName().
                    equals("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c")) {
                File thisMessageFile =
                        Utils.join(commitsList[i], "Message");
                String thisMessage =
                        Utils.readContentsAsString(thisMessageFile);
                if (thisMessage.equals(fileMessage)) {
                    System.out.println(commitsList[i].getName());
                    foundOne = true;
                }
            }
        }
        if (fileMessage.equals("initial commit")) {
            System.out.println("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
            foundOne = true;
        }
        if (!foundOne) {
            System.out.println("Found no commit with that message.");
        }
    }

    private static void globallog() throws IOException {
        String[] commitNames = COMMITS.list();
        for (int i = 0; i < commitNames.length; i++) {
            File currCommit = Utils.join(COMMITS, commitNames[i]);
            if (currCommit.getName().
                    equals("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c")) {
                printinitiallog();
            } else {
                printlog(currCommit);
            }
        }
    }

    private static void printinitiallog() {
        System.out.println("===");
        System.out.println("commit b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
        SimpleDateFormat sdf =
                new SimpleDateFormat("EEE MMM d kk:mm:ss yyyy Z");
        Date date = new Date(0);
        String commitTime = sdf.format(date);
        System.out.println("Date: " + commitTime);
        System.out.println("initial commit");
        System.out.println();
    }

    private static void printlog(File currCommitFile) {
        File currMessageFile = Utils.join(currCommitFile, "Message");
        String currCommitMessage = Utils.readContentsAsString(currMessageFile);

        File currTimeFile = Utils.join(currCommitFile, "Time");
        String currCommitTime = Utils.readContentsAsString(currTimeFile);

        System.out.println("===");
        System.out.println("commit " + currCommitFile.getName());
        System.out.println("Date: " + currCommitTime);
        System.out.println(currCommitMessage);
        System.out.println();
    }

    private static void rm(String fileName) throws IOException {
        File bouttaRm = Utils.join(STAGEDFILES, fileName);
        String headCommitShaname =
                Utils.readContentsAsString(getfilewherewesaveHeadCommitName());
        File headCommit = Utils.join(COMMITS, headCommitShaname);
        File headCommitVersion = Utils.join(headCommit, fileName);
        if (!bouttaRm.exists() && !headCommitVersion.exists()) {
            System.out.print("No reason to remove the file.");
            return;
        }
        if (bouttaRm.exists()) {
            bouttaRm.delete();
        }
        if (headCommitVersion.exists()) {
            File willBeRm = new File(REMOVEDFILES, fileName);
            willBeRm.createNewFile();
            if (Utils.join(".", fileName).exists()) {
                Utils.join(".", fileName).delete();
            }
        }
    }

    private static void log() throws IOException {
        String headCommitShaname =
                Utils.readContentsAsString(getfilewherewesaveHeadCommitName());
        File headCommitFile = Utils.join(COMMITS, headCommitShaname);

        for (File currCommitFile = headCommitFile;
             !currCommitFile.getName()
                     .equals("b6589fc6ab0dc82cf12099d1c2d40ab994e8410c");
             currCommitFile = Utils.join(COMMITS,
                     Utils.readContentsAsString(
                             Utils.join(currCommitFile, "Parent")))) {

            printlog(currCommitFile);
        }
        printinitiallog();
    }

    private static void checkoutCommitID(String commitIDfileName)
            throws IOException {
        String[] fileNameCommitIDList = commitIDfileName.split(" ");
        String fileName = fileNameCommitIDList[1];
        String commitID = fileNameCommitIDList[0];
        if (commitID.length() < 10) {
            shortenedcommitID(commitIDfileName);
            return;
        }
        File rightCommit = Utils.join(COMMITS, commitID);
        if (!rightCommit.exists()) {
            System.out.print("No commit with that id exists.");
            return;
        }
        if (!Utils.join(rightCommit, fileName).exists()) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            File versionweWant = Utils.join(rightCommit, fileName);
            String contentfromVersion =
                    Utils.readContentsAsString(versionweWant);
            File versionincwd = Utils.join(".", fileName);
            Utils.writeContents(versionincwd, contentfromVersion);
        }
    }

    private static void shortenedcommitID(String commitIDfileName)
            throws IOException {
        String[] fileNameCommitIDList = commitIDfileName.split(" ");
        String fileName = fileNameCommitIDList[1];
        String commitID = fileNameCommitIDList[0];
        int lengthofID = commitID.length();
        for (File thisCommit : COMMITS.listFiles()) {
            String shortenedname =
                    thisCommit.getName().substring(0, lengthofID);
            if (shortenedname.equals(commitID)) {
                checkoutCommitID(thisCommit.getName() + " "
                        + fileName);
                return;
            }
        }
        System.out.println("No commit with that id exists.");
    }

    private static void checkoutFileName(String fileName) throws IOException {
        String headCommitShaname =
                Utils.readContentsAsString(
                        getfilewherewesaveHeadCommitName());
        File headCommit = Utils.join(COMMITS, headCommitShaname);
        if (!Utils.join(headCommit, fileName).exists()) {
            System.out.print("File does not exist in that commit.");
            return;
        } else {
            File versionweWant = Utils.join(headCommit, fileName);
            String contentfromVersion =
                    Utils.readContentsAsString(versionweWant);
            File versionincwd = Utils.join(".", fileName);
            Utils.writeContents(versionincwd, contentfromVersion);
        }
    }

    /**
     * function has to:
     * 1. Creates a new Gitlet version-control system in the current directory
     * 2. One commit with the commit message "initial commit"
     * 3. A branch called "master" which points to the intial commit
     * and will be the current branch
     *
     */
    public static void init() throws Exception {
        if (GITLET.exists()) {
            System.out.print(
                    "A Gitlet version-control system "
                            + "already exists in the current directory.");
            return;
        } else {
            setupPersistence();
            createinitialcommit();
            increaselastestcommitIDbyone();
        }
    }

    private static void createinitialcommit() throws IOException {
        String firstshaname = Utils.sha1("0".getBytes());
        initial = new File(COMMITS, firstshaname);
        initial.createNewFile();
        File firstHeadCommit = Utils.join(MASTERBRANCH, "Head Commit");
        Utils.writeContents(firstHeadCommit, firstshaname);
    }

    private static void increaselastestcommitIDbyone() {
        int currentCommitIDnum =
                Integer.parseInt(
                        Utils.readContentsAsString(LATESTCOMMITID));
        currentCommitIDnum++;
        Utils.writeContents(LATESTCOMMITID,
                String.valueOf(currentCommitIDnum));
    }

    public static void add(String fileName) throws IOException {
        if (!Utils.join(".", fileName).exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String headCommitName =
                Utils.readContentsAsString(
                        getfilewherewesaveHeadCommitName());
        File headCommitasdirectory = Utils.join(COMMITS, headCommitName);
        if (Utils.join(headCommitasdirectory, fileName).exists()) {
            String headCommitversion =
                    Utils.
                            readContentsAsString(
                                    Utils.join(
                                            headCommitasdirectory, fileName));
            String cwdversion =
                    Utils.
                            readContentsAsString(
                                    Utils.join(".", fileName));
            if (cwdversion.equals(headCommitversion)) {
                if (Utils.join(STAGEDFILES, fileName).exists()) {
                    Utils.join(STAGEDFILES, fileName).delete();
                }
                if (Utils.join(REMOVEDFILES, fileName).exists()) {
                    Utils.join(REMOVEDFILES, fileName).delete();
                }
                return;
            }
        }
        File blobfile = new File(STAGEDFILES, fileName);
        blobfile.createNewFile();
        String blobfromcwd = fromFile(fileName);
        Utils.writeContents(blobfile, blobfromcwd);
    }

    public static String fromFile(String fileName) {
        File blobfile = Utils.join(".", fileName);
        String blobbert = Utils.readContentsAsString(blobfile);
        return blobbert;
    }

    private static void setupPersistence() throws IOException {
        GITLET.mkdir();
        STAGEDFILES.mkdir();
        COMMITS.mkdir();
        REMOVEDFILES.mkdir();

        BRANCHES.mkdir();
        MASTERBRANCH.mkdir();
        File headCommitName = new File(MASTERBRANCH, "Head Commit");
        headCommitName.createNewFile();

        LATESTCOMMITID.createNewFile();
        Utils.writeContents(LATESTCOMMITID, "0");
        ACTIVEBRANCH.createNewFile();
        Utils.writeContents(ACTIVEBRANCH, "master");
    }

    public static void commit(String message) throws Exception {
        Commit thisCommit = new Commit(message);
        thisCommit.commitFiles();
        Utils.writeContents(
                getfilewherewesaveHeadCommitName(),
                thisCommit.getshaname());
        increaselastestcommitIDbyone();
    }

    public static File getfilewherewesaveHeadCommitName() {
        String activebranchname = Utils.readContentsAsString(ACTIVEBRANCH);
        File activebranchy = Utils.join(BRANCHES, activebranchname);
        File currentHeadCommit = Utils.join(activebranchy, "Head Commit");
        return currentHeadCommit;
    }

    /*public static void persistParent(String shaname) throws IOException {
        String parentShaname =  Utils.readContentsAsString(_headCommit);
        File parentfile = new File(thisCommit, "Parent");
        parentfile.createNewFile();
        Utils.writeContents(parentfile, parentMessage);
    } */

    /** cwd. */
    private static File initial;


}
