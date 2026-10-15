package id.feezet.efauth;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Clone detection + AUTH-target resolution for eFootball installs that live
 * inside cloner apps (Mochi Cloner, Dual Space, Parallel Space, ...).
 *
 * <p>Clones do NOT appear as separate installed packages: they run inside the
 * cloner app's virtual space. So eVault cannot find them through
 * PackageManager alone. Instead it:</p>
 * <ol>
 *   <li>detects which cloner apps are installed (label heuristics + a small
 *       list of known cloner package ids),</li>
 *   <li>scans the cloner's data root (granted once via SAF) for directories
 *       that contain {@code SaveData/AUTH/online_user_id_data.dat},</li>
 *   <li>uses the found {@code SaveData/AUTH/} folder as the AUTH target, so
 *       every existing AUTH operation (SWITCH, SIMPAN, AUTO LOGIN, ...)
 *       works against the clone unchanged.</li>
 * </ol>
 *
 * <p>No cloner data location is ever hardcoded: the data root comes from the
 * user's one-time SAF grant, and clone folders are discovered by scanning
 * for the AUTH file. Launch behaviour is intentionally untouched — the
 * "MASUK EFOOTBALL" button keeps launching the selected game package.</p>
 */
public class CloneResolver {

    /** The account file that identifies an eFootball data folder. */
    public static final String AUTH_FILE = "online_user_id_data.dat";

    /** Cloner package ids this code is sure about. Label heuristics cover the rest. */
    private static final Set<String> KNOWN_CLONER_PACKAGES = new HashSet<>(Arrays.asList(
            "com.lbe.parallel.intl",    // Parallel Space
            "com.lbe.parallel.intl64",  // Parallel Space 64-bit
            "com.ludashi.dualspace"    // Dual Space
    ));

    /** Lowercase label fragments that identify a cloner app. */
    private static final String[] CLONER_LABEL_HINTS = {
            "mochi",
            "parallel space",
            "dual space",
            "dualspace",
            "clone",
            "multi parallel",
            "2accounts",
            "super clone",
            "clone master",
            "multi app",
            "app cloner"
    };

    /** Max directory depth scanned below the cloner's data root. */
    private static final int MAX_SCAN_DEPTH = 5;

    /** Max directories visited per scan, so a huge tree cannot hang the UI. */
    private static final int MAX_SCAN_DIRS = 3000;

    /** One installed cloner app. */
    public static class ClonerApp {
        public final String packageName;
        public final String label;

        public ClonerApp(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }

    /** One discovered clone: its AUTH dir, its game-data dir, and a display label. */
    public static class CloneTarget {
        /** The {@code SaveData/AUTH/} folder: becomes eVault's AUTH target. */
        public final DocumentFile authDir;
        /** The game-data {@code files/} folder (holds {@code assetpacks/} + {@code SaveData/}). */
        public final DocumentFile filesDir;
        public final String label;

        public CloneTarget(DocumentFile authDir, DocumentFile filesDir, String label) {
            this.authDir = authDir;
            this.filesDir = filesDir;
            this.label = label;
        }
    }

    /** Lists installed apps that look like cloners. Never returns null. */
    public static List<ClonerApp> detectCloners(Context ctx) {
        ArrayList<ClonerApp> result = new ArrayList<>();
        List<ApplicationInfo> apps;
        try {
            apps = ctx.getPackageManager().getInstalledApplications(0);
        } catch (Exception e) {
            return result;
        }
        String self = ctx.getPackageName();
        for (ApplicationInfo app : apps) {
            if (app == null || app.packageName == null) continue;
            if (app.packageName.equals(self)) continue;
            String label;
            try {
                label = String.valueOf(ctx.getPackageManager().getApplicationLabel(app));
            } catch (Exception e) {
                label = app.packageName;
            }
            if (isCloner(app.packageName, label)) {
                result.add(new ClonerApp(app.packageName, label));
            }
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }

    /** Lists ALL installed launchable apps (for the "APP LAIN…" fallback). */
    public static List<ClonerApp> listAllApps(Context ctx) {
        ArrayList<ClonerApp> result = new ArrayList<>();
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps;
        try {
            apps = pm.getInstalledApplications(0);
        } catch (Exception e) {
            return result;
        }
        String self = ctx.getPackageName();
        for (ApplicationInfo app : apps) {
            if (app == null || app.packageName == null) continue;
            if (app.packageName.equals(self)) continue;
            if (pm.getLaunchIntentForPackage(app.packageName) == null) continue;
            String label;
            try {
                label = String.valueOf(pm.getApplicationLabel(app));
            } catch (Exception e) {
                label = app.packageName;
            }
            result.add(new ClonerApp(app.packageName, label));
        }
        result.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        return result;
    }

    private static boolean isCloner(String pkg, String label) {
        if (KNOWN_CLONER_PACKAGES.contains(pkg)) return true;
        String l = label == null ? "" : label.toLowerCase(Locale.ROOT);
        for (String hint : CLONER_LABEL_HINTS) {
            if (l.contains(hint)) return true;
        }
        return false;
    }

    /**
     * Scans a cloner data root for clone targets. A target is any folder
     * whose {@code SaveData/AUTH/online_user_id_data.dat} exists. The scan
     * is depth-limited and capped so it cannot hang on a huge tree.
     * Never returns null.
     */
    public static List<CloneTarget> findCloneTargets(DocumentFile root) {
        ArrayList<CloneTarget> out = new ArrayList<>();
        if (root == null) return out;
        int[] visited = new int[]{0};
        scanDir(root, null, MAX_SCAN_DEPTH, visited, out);
        return out;
    }

    private static void scanDir(DocumentFile dir, DocumentFile parent, int depthLeft,
                                int[] visited, List<CloneTarget> out) {
        if (dir == null || depthLeft < 0 || visited[0] > MAX_SCAN_DIRS) return;
        visited[0]++;
        String name = dir.getName();
        if ("SaveData".equalsIgnoreCase(name)) {
            DocumentFile auth = safeFind(dir, "AUTH");
            if (auth != null && auth.isDirectory()) {
                DocumentFile dat = safeFind(auth, AUTH_FILE);
                if (dat != null && dat.isFile() && parent != null) {
                    out.add(new CloneTarget(auth, parent, labelFor(parent)));
                    return; // do not descend into a matched SaveData folder
                }
            }
        }
        if (depthLeft == 0) return;
        for (DocumentFile child : safeList(dir)) {
            if (child.isDirectory()) {
                scanDir(child, dir, depthLeft - 1, visited, out);
            }
        }
    }

    private static String labelFor(DocumentFile filesDir) {
        String name = filesDir.getName();
        return (name == null || name.isEmpty()) ? "Clone" : name;
    }

    private static DocumentFile safeFind(DocumentFile parent, String name) {
        try {
            return parent.findFile(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static DocumentFile[] safeList(DocumentFile dir) {
        if (dir == null) return new DocumentFile[0];
        try {
            if (!dir.isDirectory()) return new DocumentFile[0];
            DocumentFile[] children = dir.listFiles();
            return children == null ? new DocumentFile[0] : children;
        } catch (Exception e) {
            return new DocumentFile[0];
        }
    }
}
