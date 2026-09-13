package id.feezet.efauth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String AUTH_FILE = "online_user_id_data.dat";
    private static final String PREFS = "paths";
    private static final String KEY_LIBRARY = "library";
    private static final String KEY_TARGET = "target";
    private static final String KEY_MODE = "workflow_mode";
    private static final String KEY_CREATE_NAMES = "create_names";
    private static final String KEY_CREATE_INDEX = "create_index";
    private static final String KEY_CREATE_COUNT = "create_count";
    private static final String MODE_NONE = "none";
    private static final String MODE_DELETE_DATA = "delete_data";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";

    private SharedPreferences prefs;
    private TextView status;
    private TextView accountsText;
    private Spinner accountSpinner;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private boolean workflowLaunchPending = false;

    private final Handler autoLoginHandler = new Handler(Looper.getMainLooper());
    private boolean autoLoginRunning = false;
    private int autoLoginIndex = 0;
    private EditText autoLoginDelayEdit;

    private final ActivityResultLauncher<Intent> pickLibrary = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    saveTree(KEY_LIBRARY, r.getData().getData());
                    loadAccounts();
                }
            });

    private final ActivityResultLauncher<Intent> pickTarget = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    saveTree(KEY_TARGET, r.getData().getData());
                    updateStatus("Target AUTH tersimpan.");
                }
            });

    private final ActivityResultLauncher<Intent> pickCreateFolder = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    Uri uri = r.getData().getData();
                    saveTree(KEY_LIBRARY, uri);
                    startCreateWorkflow(DocumentFile.fromTreeUri(this, uri), prefs.getInt(KEY_CREATE_COUNT, 0));
                }
            });

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        loadAccounts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!workflowLaunchPending) return;
        workflowLaunchPending = false;

        String mode = getMode();
        if (MODE_DELETE_DATA.equals(mode)) {
            showDeleteDataExitDialog();
        } else if (MODE_CREATE.equals(mode)) {
            showCreateExitDialog();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 56, 32, 28);

        TextView title = new TextView(this);
        title.setText("eVault");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 22);
        root.addView(title, fullLp());

        Button library = new Button(this);
        library.setText("PILIH AUTH LIBRARY");
        library.setOnClickListener(v -> chooseTree(pickLibrary));
        root.addView(library, fullLp());

        Button target = new Button(this);
        target.setText("PILIH FOLDER AUTH EFOOTBALL");
        target.setOnClickListener(v -> chooseTree(pickTarget));
        root.addView(target, fullLp());

        accountsText = new TextView(this);
        accountsText.setText("Akun ditemukan: 0");
        accountsText.setPadding(0, 18, 0, 8);
        root.addView(accountsText, fullLp());

        accountSpinner = new Spinner(this);
        root.addView(accountSpinner, fullLp());

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button switchBtn = new Button(this);
        switchBtn.setText("SWITCH");
        switchBtn.setOnClickListener(v -> switchSelected());
        row1.addView(switchBtn, weightLp());
        Button deleteDataBtn = new Button(this);
        deleteDataBtn.setText("HAPUS DATA");
        deleteDataBtn.setOnClickListener(v -> showDeleteDataConfirmDialog());
        row1.addView(deleteDataBtn, weightLp());
        root.addView(row1, fullLp());

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button autoBtn = new Button(this);
        autoBtn.setText("AUTO LOGIN");
        autoBtn.setOnClickListener(v -> startAutoLogin());
        row2.addView(autoBtn, weightLp());
        Button stopBtn = new Button(this);
        stopBtn.setText("STOP");
        stopBtn.setOnClickListener(v -> stopAutoLogin());
        row2.addView(stopBtn, weightLp());
        root.addView(row2, fullLp());

        autoLoginDelayEdit = new EditText(this);
        autoLoginDelayEdit.setInputType(2);
        autoLoginDelayEdit.setSingleLine(true);
        autoLoginDelayEdit.setText("10");
        autoLoginDelayEdit.setHint("Delay AUTO LOGIN (detik)");
        root.addView(autoLoginDelayEdit, fullLp());

        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        Button deleteAuthBtn = new Button(this);
        deleteAuthBtn.setText("HAPUS AUTH SAAT INI");
        deleteAuthBtn.setOnClickListener(v -> deleteCurrentAuth());
        row3.addView(deleteAuthBtn, weightLp());
        Button createBtn = new Button(this);
        createBtn.setText("BUAT AKUN BARU");
        createBtn.setOnClickListener(v -> showCreateDialog());
        row3.addView(createBtn, weightLp());
        root.addView(row3, fullLp());

        Button enterBtn = new Button(this);
        enterBtn.setText("MASUK EFOOTBALL");
        enterBtn.setOnClickListener(v -> {
            stopAutoLogin();
            clearWorkflow();
            launchGame();
        });
        LinearLayout.LayoutParams enterLp = fullLp();
        enterLp.topMargin = 14;
        root.addView(enterBtn, enterLp);

        status = new TextView(this);
        status.setText("Siap.");
        status.setPadding(0, 18, 0, 0);
        root.addView(status, fullLp());

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams fullLp() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weightLp() { return new LinearLayout.LayoutParams(0, -2, 1f); }

    private void chooseTree(ActivityResultLauncher<Intent> launcher) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        launcher.launch(i);
    }

    private void saveTree(String key, Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) {}
        prefs.edit().putString(key, uri.toString()).apply();
    }

    private DocumentFile getTree(String key) {
        String s = prefs.getString(key, null);
        return s == null ? null : DocumentFile.fromTreeUri(this, Uri.parse(s));
    }

    private void loadAccounts() {
        accounts.clear();
        DocumentFile lib = getTree(KEY_LIBRARY);
        if (lib != null && lib.isDirectory()) {
            for (DocumentFile d : lib.listFiles()) {
                if (!d.isDirectory()) continue;
                DocumentFile auth = d.findFile(AUTH_FILE);
                if (auth != null && auth.isFile()) accounts.add(new Account(nameOf(d), auth));
            }
        }
        Collections.sort(accounts, Comparator.comparing(a -> a.name.toLowerCase()));
        ArrayList<String> names = new ArrayList<>();
        for (Account a : accounts) names.add(a.name);
        accountSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        accountsText.setText("Akun ditemukan: " + accounts.size());
    }

    private String nameOf(DocumentFile d) { return d.getName() == null ? "Unnamed" : d.getName(); }

    private void switchSelected() {
        stopAutoLogin();
        clearWorkflow();
        if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        int pos = accountSpinner.getSelectedItemPosition();
        if (pos >= 0 && pos < accounts.size() && copyAuth(accounts.get(pos))) launchGame();
    }

    private void deleteCurrentAuth() {
        stopAutoLogin();
        clearWorkflow();
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; }
        new AlertDialog.Builder(this)
                .setTitle("Hapus AUTH Saat Ini")
                .setMessage("Hapus file AUTH yang sedang terpasang di target?")
                .setNegativeButton("BATAL", null)
                .setPositiveButton("HAPUS", (d, w) -> updateStatus(auth.delete() ? "✓ AUTH saat ini dihapus." : "Gagal menghapus AUTH. Periksa izin folder."))
                .show();
    }

    // =========================================================================
    // E. HAPUS DATA — WORKFLOW
    // =========================================================================

    private void showDeleteDataConfirmDialog() {
        stopAutoLogin();
        clearWorkflow();
        new AlertDialog.Builder(this)
                .setTitle("Hapus Data")
                .setMessage("Hapus AUTH saat ini?")
                .setNegativeButton("TIDAK", null)
                .setPositiveButton("YA", (d, w) -> executeDeleteDataWorkflow())
                .show();
    }

    private void executeDeleteDataWorkflow() {
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) {
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return;
        }
        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth != null && auth.isFile()) {
            if (!auth.delete()) {
                updateStatus("Gagal menghapus AUTH saat ini.");
                return;
            }
        }
        setMode(MODE_DELETE_DATA);
        updateStatus("AUTH saat ini dihapus. Memulai AUTO LOGIN...");
        startDeleteDataAutoLogin();
    }

    private void startDeleteDataAutoLogin() {
        if (accounts.isEmpty()) {
            updateStatus("Library kosong / belum dipilih.");
            clearWorkflow();
            return;
        }
        autoLoginRunning = true;
        autoLoginIndex = 0;
        runNextDeleteDataAutoLogin();
    }

    private void runNextDeleteDataAutoLogin() {
        if (!autoLoginRunning || autoLoginIndex >= accounts.size()) {
            autoLoginRunning = false;
            updateStatus("✓ AUTO LOGIN selesai.");
            return;
        }
        Account account = accounts.get(autoLoginIndex);
        updateStatus("AUTO LOGIN [" + (autoLoginIndex + 1) + "/" + accounts.size() + "]: " + account.name);
        if (!copyAuth(account)) {
            autoLoginRunning = false;
            clearWorkflow();
            return;
        }
        if (launchGame()) {
            workflowLaunchPending = true;
        } else {
            autoLoginRunning = false;
            clearWorkflow();
            return;
        }
        autoLoginIndex++;
    }

    private void showDeleteDataExitDialog() {
        stopAutoLogin();
        new AlertDialog.Builder(this)
                .setTitle("Keluar eFootball")
                .setMessage("Simpan atau hapus AUTH?")
                .setCancelable(false)
                .setNegativeButton("BATAL", (d, w) -> {
                    clearWorkflow();
                    updateStatus("Workflow HAPUS DATA dibatalkan.");
                })
                .setNeutralButton("HAPUS", (d, w) -> {
                    DocumentFile target = getTree(KEY_TARGET);
                    DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE);
                    if (auth != null && auth.isFile()) {
                        auth.delete();
                    }
                    updateStatus("AUTH dihapus. Melanjutkan AUTO LOGIN...");
                    if (autoLoginIndex >= accounts.size()) {
                        autoLoginIndex = 0;
                    }
                    autoLoginRunning = true;
                    runNextDeleteDataAutoLogin();
                })
                .setPositiveButton("SIMPAN", (d, w) -> showDeleteDataSaveDialog())
                .show();
    }

    private void showDeleteDataSaveDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nama file / folder");
        new AlertDialog.Builder(this)
                .setTitle("Simpan AUTH")
                .setMessage("Nama file:")
                .setView(input)
                .setCancelable(false)
                .setNegativeButton("BATAL", (d, w) -> showDeleteDataExitDialog())
                .setPositiveButton("SIMPAN", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        updateStatus("Nama tidak boleh kosong.");
                        showDeleteDataSaveDialog();
                        return;
                    }
                    DocumentFile lib = getTree(KEY_LIBRARY);
                    DocumentFile target = getTree(KEY_TARGET);
                    if (!validDir(lib)) {
                        updateStatus("Pilih AUTH Library terlebih dahulu.");
                        showDeleteDataSaveDialog();
                        return;
                    }
                    DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE);
                    if (auth == null || !auth.isFile()) {
                        updateStatus("AUTH tidak ditemukan di Target.");
                        clearWorkflow();
                        return;
                    }
                    if (lib.findFile(name) != null) {
                        updateStatus("Nama folder sudah digunakan: " + name);
                        showDeleteDataSaveDialog();
                        return;
                    }
                    DocumentFile folder = lib.createDirectory(name);
                    if (folder == null) {
                        updateStatus("Gagal membuat folder akun.");
                        return;
                    }
                    if (copyFile(auth, folder, AUTH_FILE)) {
                        clearWorkflow();
                        loadAccounts();
                        updateStatus("✓ AUTH disimpan: " + name);
                    } else {
                        folder.delete();
                        updateStatus("Gagal menyimpan AUTH.");
                    }
                })
                .show();
    }

    // =========================================================================
    // F & G. BUAT AKUN BARU — WORKFLOW
    // =========================================================================

    private void showCreateDialog() {
        stopAutoLogin();
        clearWorkflow();
        final EditText input = new EditText(this);
        input.setInputType(2);
        input.setSingleLine(true);
        input.setHint("Jumlah akun (1-10)");
        new AlertDialog.Builder(this)
                .setTitle("BUAT AKUN BARU")
                .setMessage("Mau buat berapa akun?")
                .setView(input)
                .setNegativeButton("BATAL", null)
                .setPositiveButton("LANJUT", (d, w) -> {
                    int count;
                    try {
                        count = Integer.parseInt(input.getText().toString().trim());
                    } catch (Exception e) {
                        count = 0;
                    }
                    if (count < 1 || count > 10) {
                        updateStatus("Jumlah akun harus 1 sampai 10.");
                        return;
                    }
                    prefs.edit().putInt(KEY_CREATE_COUNT, count).apply();
                    showCreateLocationDialog(count);
                })
                .show();
    }

    private void showCreateLocationDialog(int count) {
        DocumentFile existing = getTree(KEY_LIBRARY);
        String[] items = validDir(existing)
                ? new String[]{"Gunakan folder yang sudah dipilih", "Buat folder baru"}
                : new String[]{"Buat folder baru"};
        new AlertDialog.Builder(this)
                .setTitle("Lokasi penyimpanan")
                .setItems(items, (d, which) -> {
                    if (validDir(existing) && which == 0) startCreateWorkflow(existing, count);
                    else pickCreateFolder.launch(folderPickerIntent());
                })
                .setNegativeButton("BATAL", null)
                .show();
    }

    private Intent folderPickerIntent() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return i;
    }

    private void startCreateWorkflow(DocumentFile root, int count) {
        if (!validDir(root) || count < 1 || count > 10) {
            updateStatus("Jumlah/folder penyimpanan tidak valid.");
            return;
        }
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) {
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return;
        }
        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth != null && auth.isFile()) {
            auth.delete();
        }
        prepareCreate(root, count);
    }

    private void prepareCreate(DocumentFile root, int count) {
        List<String> names = buildAvailableNames(root, count);
        if (names.size() != count) {
            updateStatus("Tidak cukup nama akun yang tersedia.");
            return;
        }
        prefs.edit()
                .putString(KEY_CREATE_NAMES, android.text.TextUtils.join("|", names))
                .putInt(KEY_CREATE_INDEX, 0)
                .apply();
        setMode(MODE_CREATE);
        launchCurrentCreateAccount();
    }

    private void launchCurrentCreateAccount() {
        DocumentFile target = getTree(KEY_TARGET);
        if (target != null) {
            DocumentFile old = target.findFile(AUTH_FILE);
            if (old != null && old.isFile()) old.delete();
        }
        List<String> names = split(prefs.getString(KEY_CREATE_NAMES, ""));
        int index = prefs.getInt(KEY_CREATE_INDEX, 0);
        if (index >= names.size()) {
            finishCreate();
            return;
        }
        updateStatus("Buat akun baru [" + (index + 1) + "/" + names.size() + "]: " + names.get(index));
        if (launchGame()) {
            workflowLaunchPending = true;
        } else {
            clearWorkflow();
        }
    }

    public static List<String> buildAvailableNames(DocumentFile root, int count) {
        for (int round = 0; round < 27; round++) {
            String prefix = round == 0 ? "" : String.valueOf((char) ('a' + round - 1));
            for (int batch = 1; batch <= 1000; batch++) {
                boolean allFree = true;
                ArrayList<String> candidateBatch = new ArrayList<>();
                for (int slot = 1; slot <= count; slot++) {
                    String name = prefix + batch + "-" + slot;
                    if (root != null && root.findFile(name) != null) {
                        allFree = false;
                        break;
                    }
                    candidateBatch.add(name);
                }
                if (allFree) {
                    return candidateBatch;
                }
            }
        }
        return Collections.emptyList();
    }

    private void showCreateExitDialog() {
        List<String> names = split(prefs.getString(KEY_CREATE_NAMES, ""));
        int index = prefs.getInt(KEY_CREATE_INDEX, 0);
        if (index >= names.size()) {
            finishCreate();
            return;
        }
        String currentAccountName = names.get(index);

        new AlertDialog.Builder(this)
                .setTitle("Akun Baru: " + currentAccountName)
                .setMessage("Simpan atau hapus AUTH?")
                .setCancelable(false)
                .setNegativeButton("BATAL", (d, w) -> {
                    clearWorkflow();
                    updateStatus("Pembuatan akun baru dihentikan.");
                })
                .setNeutralButton("HAPUS", (d, w) -> {
                    DocumentFile target = getTree(KEY_TARGET);
                    DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE);
                    if (auth != null && auth.isFile()) auth.delete();

                    int next = index + 1;
                    prefs.edit().putInt(KEY_CREATE_INDEX, next).apply();
                    if (next >= names.size()) {
                        finishCreate();
                    } else {
                        launchCurrentCreateAccount();
                    }
                })
                .setPositiveButton("SIMPAN", (d, w) -> {
                    DocumentFile target = getTree(KEY_TARGET);
                    DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE);
                    if (auth == null || !auth.isFile()) {
                        updateStatus("AUTH tidak ditemukan di Target.");
                        clearWorkflow();
                        return;
                    }
                    DocumentFile root = getTree(KEY_LIBRARY);
                    if (!validDir(root)) {
                        updateStatus("Library penyimpanan tidak tersedia.");
                        clearWorkflow();
                        return;
                    }
                    DocumentFile folder = root.findFile(currentAccountName);
                    if (folder == null) {
                        folder = root.createDirectory(currentAccountName);
                    }
                    if (folder == null) {
                        updateStatus("Gagal membuat folder: " + currentAccountName);
                        clearWorkflow();
                        return;
                    }
                    if (!copyFile(auth, folder, AUTH_FILE)) {
                        folder.delete();
                        updateStatus("Gagal menyimpan AUTH: " + currentAccountName);
                        clearWorkflow();
                        return;
                    }
                    auth.delete();
                    loadAccounts();

                    int next = index + 1;
                    prefs.edit().putInt(KEY_CREATE_INDEX, next).apply();
                    if (next >= names.size()) {
                        finishCreate();
                    } else {
                        launchCurrentCreateAccount();
                    }
                })
                .show();
    }

    private void finishCreate() {
        int total = split(prefs.getString(KEY_CREATE_NAMES, "")).size();
        clearWorkflow();
        loadAccounts();
        updateStatus("✓ Pembuatan akun selesai. Total: " + total);
    }

    // =========================================================================
    // C. AUTO LOGIN
    // =========================================================================

    private void startAutoLogin() {
        if (accounts.isEmpty()) { updateStatus("Library kosong."); return; }
        if (autoLoginRunning) return;
        clearWorkflow();
        autoLoginRunning = true;
        autoLoginIndex = 0;
        runNextAutoLogin();
    }

    private void runNextAutoLogin() {
        if (!autoLoginRunning || autoLoginIndex >= accounts.size()) {
            autoLoginRunning = false;
            updateStatus("✓ AUTO LOGIN selesai. Total: " + accounts.size());
            return;
        }
        Account account = accounts.get(autoLoginIndex);
        updateStatus("AUTO LOGIN [" + (autoLoginIndex + 1) + "/" + accounts.size() + "]: " + account.name);
        if (!copyAuth(account)) { autoLoginRunning = false; return; }
        launchGame();
        autoLoginIndex++;
        autoLoginHandler.postDelayed(this::runNextAutoLogin, getAutoLoginDelayMs());
    }

    private long getAutoLoginDelayMs() {
        try {
            int seconds = Integer.parseInt(autoLoginDelayEdit.getText().toString().trim());
            return Math.max(2, seconds) * 1000L;
        } catch (Exception e) {
            return 10000L;
        }
    }

    private void stopAutoLogin() {
        autoLoginRunning = false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        updateStatus("AUTO LOGIN dihentikan.");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private boolean copyAuth(Account account) {
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return false; }
        DocumentFile old = target.findFile(AUTH_FILE);
        if (old != null) old.delete();
        return copyFile(account.file, target, AUTH_FILE);
    }

    private boolean copyFile(DocumentFile source, DocumentFile parent, String name) {
        DocumentFile dest = parent.findFile(name);
        if (dest != null) dest.delete();
        dest = parent.createFile("application/octet-stream", name);
        if (dest == null) return false;
        try (InputStream in = getContentResolver().openInputStream(source.getUri());
             OutputStream out = getContentResolver().openOutputStream(dest.getUri(), "w")) {
            if (in == null || out == null) throw new Exception("stream null");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
            return true;
        } catch (Exception e) {
            dest.delete();
            return false;
        }
    }

    private boolean launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if (i == null) {
            updateStatus("Package " + PACKAGE + " tidak ditemukan.");
            return false;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        return true;
    }

    private boolean validDir(DocumentFile d) { return d != null && d.isDirectory(); }
    private String getMode() { return prefs.getString(KEY_MODE, MODE_NONE); }
    private void setMode(String mode) { prefs.edit().putString(KEY_MODE, mode).apply(); }

    private void clearWorkflow() {
        workflowLaunchPending = false;
        prefs.edit().putString(KEY_MODE, MODE_NONE).remove(KEY_CREATE_NAMES).remove(KEY_CREATE_INDEX).remove(KEY_CREATE_COUNT).apply();
    }

    private List<String> split(String value) {
        if (value == null || value.isEmpty()) return Collections.emptyList();
        return Arrays.asList(value.split("\\|", -1));
    }

    private void updateStatus(String s) {
        if (status != null) status.setText(s);
    }

    @Override
    protected void onDestroy() {
        stopAutoLogin();
        super.onDestroy();
    }

    static class Account {
        final String name;
        final DocumentFile file;
        Account(String name, DocumentFile file) {
            this.name = name;
            this.file = file;
        }
    }
}
