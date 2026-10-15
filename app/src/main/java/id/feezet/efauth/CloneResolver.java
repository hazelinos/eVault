package id.feezet.efauth;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Clone detection for eFootball installs whose data lives on shared storage
 * (internal storage / external SD card), e.g. Mochi Cloner clones.
 *
 * <p>How it works, mirroring Root Explorer: eVault holds All Files Access
 * (MANAGE_EXTERNAL_STORAGE), enumerates every storage volume, and scans for
 * folders that contain {@code SaveData/AUTH/online_user_id_data.dat}. No
 * folder picker, no SAF grant, no root, no Shizuku — the scan is fully
 * automatic, exactly like a file manager listing the SD card.</p>
 *
 * <p>What this deliberately does NOT cover: clone data inside a cloner's
 * private storage ({@code /data/data/...}), which Android blocks for every
 * non-root app. If a clone's data is not on shared storage, it cannot be
 * found by this scan and eVault says so instead of failing silently.</p>
 */
public class CloneResolver {

    /** The account file that identifies an eFootball data folder. */
    public static final String AUTH_FILE = "online_user_id_data.dat";

    /** Max directory depth scanned below each storage volume root. */
    private static final int MAX_SCAN_DEPTH = 5;

    /** Max directories visited per scan, so a huge tree cannot hang the UI. */
    private static final int MAX_SCAN_DIRS = 5000;

    /** One discovered clone: its AUTH dir, its game-data dir, and a display label. */
    public static class CloneTarget {
        /** The {@code SaveData/AUTH/} folder: becomes eVault's AUTH target. */
        public final File authDir;
        /** The game-data {@code files/} folder (holds {@code assetpacks/} + {@code SaveData/}). */
        public final File filesDir;
        public final String label;

        public CloneTarget(File authDir, File filesDir, String label) {
            this.authDir = authDir;
            this.filesDir = filesDir;
            this.label = label;
        }
    }

    /**
     * Lists storage volume roots: primary shared storage plus every mounted
     * volume (e.g. an external SD card), derived from
     * {@code getExternalFilesDirs()} by walking up from
     * {@code <volume>/Android/data/<pkg>/files}. Never returns null.
     */
    public static List<File> listStorageRoots(Context ctx) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        ArrayList<File> roots = new ArrayList<>();
        addRoot(roots, seen, Environment.getExternalStorageDirectory());
        File[] dirs = ctx.getExternalFilesDirs(null);
        if (dirs != null) {
            for (File d : dirs) {
                if (d == null) continue;
                File vol = d;
                for (int i = 0; i < 4 && vol != null; i++) vol = vol.getParentFile();
                addRoot(roots, seen, vol);
            }
        }
        return roots;
    }

    private static void addRoot(List<File> roots, LinkedHashSet<String> seen, File root) {
        if (root == null) return;
        try {
            File canonical = root.getCanonicalFile();
            if (!canonical.isDirectory()) return;
            if (seen.add(canonical.getAbsolutePath())) roots.add(canonical);
        } catch (Exception ignored) { }
    }

    /**
     * Scans storage roots for clone targets. A target is any folder whose
     * {@code SaveData/AUTH/online_user_id_data.dat} exists. The scan is
     * depth-limited and capped so it cannot hang on a huge tree.
     * Never returns null.
     */
    public static List<CloneTarget> findCloneTargets(List<File> roots) {
        ArrayList<CloneTarget> out = new ArrayList<>();
        if (roots == null) return out;
        int[] visited = new int[]{0};
        for (File root : roots) {
            scanDir(root, root, MAX_SCAN_DEPTH, visited, out);
        }
        return out;
    }

    private static void scanDir(File dir, File volumeRoot, int depthLeft,
                                int[] visited, List<CloneTarget> out) {
        if (dir == null || depthLeft < 0 || visited[0] > MAX_SCAN_DIRS) return;
        visited[0]++;
        if ("SaveData".equalsIgnoreCase(dir.getName())) {
            File auth = new File(dir, "AUTH");
            File dat = new File(auth, AUTH_FILE);
            if (auth.isDirectory() && dat.isFile()) {
                File filesDir = dir.getParentFile();
                if (filesDir != null) {
                    out.add(new CloneTarget(auth, filesDir, labelFor(volumeRoot, filesDir)));
                }
                return; // do not descend into a matched SaveData folder
            }
        }
        if (depthLeft == 0) return;
        File[] children;
        try {
            children = dir.listFiles();
        } catch (Exception e) {
            return;
        }
        if (children == null) return;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            String n = child.getName();
            if (n.startsWith(".")) continue;
            // Other apps' Android/data stays blocked even with All Files Access.
            if ("Android".equals(n)) continue;
            scanDir(child, volumeRoot, depthLeft - 1, visited, out);
        }
    }

    private static String labelFor(File volumeRoot, File filesDir) {
        String volume = "Internal";
        try {
            String primary = Environment.getExternalStorageDirectory().getCanonicalFile().getAbsolutePath();
            if (!volumeRoot.getCanonicalFile().getAbsolutePath().equals(primary)) {
                volume = "SD " + volumeRoot.getName();
            }
        } catch (Exception ignored) { }
        String rel = filesDir.getAbsolutePath();
        try {
            String rootPath = volumeRoot.getCanonicalFile().getAbsolutePath();
            if (rel.startsWith(rootPath)) rel = rel.substring(rootPath.length());
        } catch (Exception ignored) { }
        if (rel.startsWith("/")) rel = rel.substring(1);
        return volume + ": " + rel;
    }
}
