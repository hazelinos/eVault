package id.feezet.efauth;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
    private static final String KEY_ACTIVITY_LOG = "activity_log";
    private static final String MODE_NONE = "none";
    private static final String MODE_DELETE_DATA = "delete_data";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";

    private SharedPreferences prefs;
    private TextView status;
    private TextView accountsText;
    private Spinner accountSpinner;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private boolean workflowLaunchPending;

    private Handler autoLoginHandler = new Handler(Looper.getMainLooper());
    private boolean autoLoginRunning = false;
    private int autoLoginIndex = 0;
    private EditText autoLoginDelayEdit;

    private final ActivityResultLauncher<Intent> pickLibrary = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == RESULT_OK && r.getData() != null) { saveTree(KEY_LIBRARY, r.getData().getData()); loadAccounts(); }
    });
    private final ActivityResultLauncher<Intent> pickTarget = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == RESULT_OK && r.getData() != null) { saveTree(KEY_TARGET, r.getData().getData()); updateStatus("Target AUTH tersimpan."); }
    });
    private final ActivityResultLauncher<Intent> pickCreateFolder = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == RESULT_OK && r.getData() != null) { Uri uri = r.getData().getData(); saveTree(KEY_LIBRARY, uri); startCreateWorkflow(DocumentFile.fromTreeUri(this, uri), prefs.getInt(KEY_CREATE_COUNT, 0)); }
    });

    @Override protected void onCreate(Bundle b) { super.onCreate(b); prefs = getSharedPreferences(PREFS, MODE_PRIVATE); buildUi(); loadAccounts(); }
    @Override protected void onResume() { super.onResume(); if (!workflowLaunchPending) return; workflowLaunchPending = false; if (MODE_DELETE_DATA.equals(getMode())) showNewAuthDialog(); else if (MODE_CREATE.equals(getMode())) handleCreateExit(); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 56, 32, 28);
        TextView title = new TextView(this); title.setText("eVault"); title.setTextSize(25); title.setGravity(Gravity.CENTER); title.setPadding(0, 0, 0, 22); root.addView(title, fullLp());
        Button library = new Button(this); library.setText("PILIH AUTH LIBRARY"); library.setOnClickListener(v -> chooseTree(pickLibrary)); root.addView(library, fullLp());
        Button target = new Button(this); target.setText("PILIH FOLDER AUTH EFOOTBALL"); target.setOnClickListener(v -> chooseTree(pickTarget)); root.addView(target, fullLp());
        accountsText = new TextView(this); accountsText.setText("Akun ditemukan: 0"); accountsText.setPadding(0, 18, 0, 8); root.addView(accountsText, fullLp());
        accountSpinner = new Spinner(this); root.addView(accountSpinner, fullLp());

        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button switchBtn = new Button(this); switchBtn.setText("SWITCH"); switchBtn.setOnClickListener(v -> switchSelected()); row.addView(switchBtn, weightLp());
        Button autoBtn = new Button(this); autoBtn.setText("AUTO LOGIN"); autoBtn.setOnClickListener(v -> startAutoLogin()); row.addView(autoBtn, weightLp());
        Button stopBtn = new Button(this); stopBtn.setText("STOP"); stopBtn.setOnClickListener(v -> stopAutoLogin()); row.addView(stopBtn, weightLp());
        root.addView(row, fullLp());

        autoLoginDelayEdit = new EditText(this);
        autoLoginDelayEdit.setInputType(2);
        autoLoginDelayEdit.setSingleLine(true);
        autoLoginDelayEdit.setText("10");
        autoLoginDelayEdit.setHint("Delay AUTO LOGIN (detik)");
        root.addView(autoLoginDelayEdit, fullLp());

        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        Button deleteAuthBtn = new Button(this); deleteAuthBtn.setText("HAPUS AUTH SAAT INI"); deleteAuthBtn.setOnClickListener(v -> deleteCurrentAuth()); row2.addView(deleteAuthBtn, weightLp());
        Button createBtn = new Button(this); createBtn.setText("BUAT AKUN BARU"); createBtn.setOnClickListener(v -> showCreateDialog()); row2.addView(createBtn, weightLp()); root.addView(row2, fullLp());
        Button enterBtn = new Button(this); enterBtn.setText("MASUK EFOOTBALL"); enterBtn.setOnClickListener(v -> { stopAutoLogin(); clearWorkflow(); launchGame(); }); LinearLayout.LayoutParams enterLp = fullLp(); enterLp.topMargin = 14; root.addView(enterBtn, enterLp);
        Button logBtn = new Button(this); logBtn.setText("📋 LOG AKTIVITAS"); logBtn.setOnClickListener(v -> showActivityLog()); root.addView(logBtn, fullLp());
        status = new TextView(this); status.setText("Siap."); status.setPadding(0, 18, 0, 0); root.addView(status, fullLp());
        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
    }
    private LinearLayout.LayoutParams fullLp() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams weightLp() { return new LinearLayout.LayoutParams(0, -2, 1f); }
    private void chooseTree(ActivityResultLauncher<Intent> launcher) { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); launcher.launch(i); }
    private void saveTree(String key, Uri uri) { try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (Exception ignored) {} prefs.edit().putString(key, uri.toString()).apply(); }
    private DocumentFile getTree(String key) { String s = prefs.getString(key, null); return s == null ? null : DocumentFile.fromTreeUri(this, Uri.parse(s)); }
    private void loadAccounts() { accounts.clear(); DocumentFile lib = getTree(KEY_LIBRARY); if (lib != null && lib.isDirectory()) for (DocumentFile d : lib.listFiles()) { if (!d.isDirectory()) continue; DocumentFile auth = d.findFile(AUTH_FILE); if (auth != null && auth.isFile()) accounts.add(new Account(nameOf(d), auth)); } Collections.sort(accounts, Comparator.comparing(a -> a.name.toLowerCase())); ArrayList<String> names = new ArrayList<>(); for (Account a : accounts) names.add(a.name); accountSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names)); accountsText.setText("Akun ditemukan: " + accounts.size()); }
    private String nameOf(DocumentFile d) { return d.getName() == null ? "Unnamed" : d.getName(); }
    private void switchSelected() { stopAutoLogin(); clearWorkflow(); if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; } int pos = accountSpinner.getSelectedItemPosition(); if (pos >= 0 && pos < accounts.size() && copyAuth(accounts.get(pos))) launchGame(); }
    private void deleteCurrentAuth() { stopAutoLogin(); clearWorkflow(); DocumentFile target = getTree(KEY_TARGET); if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; } DocumentFile auth = target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; } new android.app.AlertDialog.Builder(this).setTitle("Hapus AUTH Saat Ini").setMessage("Hapus file AUTH yang sedang terpasang di target?").setNegativeButton("BATAL", null).setPositiveButton("HAPUS", (d, w) -> updateStatus(auth.delete() ? "✓ AUTH saat ini dihapus." : "Gagal menghapus AUTH. Periksa izin folder.")).show(); }
    private void deleteData() { stopAutoLogin(); clearWorkflow(); DocumentFile target = getTree(KEY_TARGET); if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; } DocumentFile auth = target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; } new android.app.AlertDialog.Builder(this).setTitle("Hapus Data").setMessage("Hapus AUTH saat ini dan masuk eFootball?").setNegativeButton("BATAL", null).setPositiveButton("HAPUS", (d, w) -> { if (!auth.delete()) { updateStatus("Gagal menghapus AUTH. Periksa izin folder."); return; } setMode(MODE_DELETE_DATA); if (launchGame()) workflowLaunchPending = true; else clearWorkflow(); }).show(); }
    private void showNewAuthDialog() { DocumentFile target = getTree(KEY_TARGET); DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH baru di Target."); return; } new android.app.AlertDialog.Builder(this).setTitle("AUTH baru terdeteksi").setMessage("Pilih tindakan untuk AUTH yang baru dibuat.").setNegativeButton("BATAL", (d, w) -> clearWorkflow()).setNeutralButton("HAPUS", (d, w) -> { if (auth.delete()) { updateStatus("AUTH dihapus. Membuka eFootball lagi..."); if (launchGame()) workflowLaunchPending = true; else clearWorkflow(); } else updateStatus("Gagal menghapus AUTH."); }).setPositiveButton("SIMPAN", (d, w) -> showSaveAuthDialog()).show(); }
    private void showSaveAuthDialog() { final EditText input = new EditText(this); input.setSingleLine(true); input.setHint("Nama file / folder"); new android.app.AlertDialog.Builder(this).setTitle("Simpan AUTH").setMessage("Nama file:").setView(input).setNegativeButton("BATAL", null).setPositiveButton("SIMPAN", (d, w) -> saveAuthWithName(input.getText().toString().trim())).show(); }
    private void saveAuthWithName(String name) { if (name.isEmpty()) { updateStatus("Nama tidak boleh kosong."); showSaveAuthDialog(); return; } DocumentFile lib = getTree(KEY_LIBRARY), target = getTree(KEY_TARGET); if (!validDir(lib)) { updateStatus("Pilih AUTH Library terlebih dahulu."); return; } DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("AUTH tidak ditemukan di Target."); return; } if (lib.findFile(name) != null) { updateStatus("Nama folder sudah digunakan: " + name); showSaveAuthDialog(); return; } DocumentFile folder = lib.createDirectory(name); if (folder == null) { updateStatus("Gagal membuat folder akun."); return; } if (copyFile(auth, folder, AUTH_FILE)) { clearWorkflow(); loadAccounts(); updateStatus("✓ AUTH disimpan: " + name); } else { folder.delete(); updateStatus("Gagal menyimpan AUTH."); } }

    private void showCreateDialog() {
        final EditText input = new EditText(this); input.setInputType(2); input.setSingleLine(true); input.setHint("Jumlah akun (1-10)");
        new android.app.AlertDialog.Builder(this).setTitle("BUAT AKUN BARU").setMessage("Mau membuat berapa akun? (1-10)").setView(input).setNegativeButton("BATAL", null).setPositiveButton("LANJUT", (d, w) -> {
            int count; try { count = Integer.parseInt(input.getText().toString().trim()); } catch (Exception e) { count = 0; }
            if (count < 1 || count > 10) { updateStatus("Jumlah akun harus 1-10."); return; }
            prefs.edit().putInt(KEY_CREATE_COUNT, count).apply(); showCreateLocationDialog(count);
        }).show();
    }
    private void showCreateLocationDialog(int count) { DocumentFile existing = getTree(KEY_LIBRARY); String[] items = validDir(existing) ? new String[]{"Gunakan folder yang sudah dipilih", "Buat folder baru"} : new String[]{"Buat folder baru"}; new android.app.AlertDialog.Builder(this).setTitle("Lokasi penyimpanan").setItems(items, (d, which) -> { if (validDir(existing) && which == 0) startCreateWorkflow(existing, count); else pickCreateFolder.launch(folderPickerIntent()); }).setNegativeButton("BATAL", null).show(); }
    private Intent folderPickerIntent() { Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION); return i; }
    private void startCreateWorkflow(DocumentFile root, int count) { if (!validDir(root) || count < 1 || count > 10) { updateStatus("Jumlah/folder penyimpanan tidak valid."); return; } DocumentFile target = getTree(KEY_TARGET); if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; } DocumentFile auth = target.findFile(AUTH_FILE); if (auth != null && auth.isFile()) new android.app.AlertDialog.Builder(this).setTitle("AUTH saat ini ditemukan").setMessage("AUTH saat ini akan dihapus agar akun baru dimulai dari AUTH kosong.").setNegativeButton("BATAL", null).setPositiveButton("HAPUS & MULAI", (d, w) -> { if (auth.delete()) prepareCreate(root, count); else updateStatus("Gagal menghapus AUTH saat ini."); }).show(); else prepareCreate(root, count); }
    private void prepareCreate(DocumentFile root, int count) { List<String> names = buildAvailableNames(root, count); if (names.size() != count) { updateStatus("Tidak cukup nama akun yang tersedia."); return; } prefs.edit().putString(KEY_CREATE_NAMES, android.text.TextUtils.join("|", names)).putInt(KEY_CREATE_INDEX, 0).apply(); setMode(MODE_CREATE); updateStatus("Akun baru 1/" + count + ": " + names.get(0)); if (launchGame()) workflowLaunchPending = true; else clearWorkflow(); }
    private List<String> buildAvailableNames(DocumentFile root, int count) { for (int batch = 0; batch < 10000; batch++) { String prefix = batch == 0 ? "" : letters(batch - 1); ArrayList<String> result = new ArrayList<>(); boolean free = true; for (int i = 1; i <= count; i++) { String name = prefix + "1-" + i; if (root.findFile(name) != null) { free = false; break; } result.add(name); } if (free) return result; } return Collections.emptyList(); }
    private String letters(int n) { StringBuilder s = new StringBuilder(); do { s.insert(0, (char)('a' + n % 26)); n = n / 26 - 1; } while (n >= 0); return s.toString(); }
    private void handleCreateExit() { DocumentFile target = getTree(KEY_TARGET); DocumentFile auth = target == null ? null : target.findFile(AUTH_FILE); List<String> names = split(prefs.getString(KEY_CREATE_NAMES, "")); int index = prefs.getInt(KEY_CREATE_INDEX, 0); if (index >= names.size()) { finishCreate(); return; } if (auth == null || !auth.isFile()) { updateStatus("AUTH tidak ditemukan untuk " + names.get(index) + ". Workflow dihentikan."); clearWorkflow(); return; } DocumentFile root = getTree(KEY_LIBRARY); if (!validDir(root)) { updateStatus("Library penyimpanan tidak tersedia."); clearWorkflow(); return; } DocumentFile folder = root.createDirectory(names.get(index)); if (folder == null) { updateStatus("Gagal membuat folder: " + names.get(index)); clearWorkflow(); return; } if (!copyFile(auth, folder, AUTH_FILE)) { folder.delete(); updateStatus("Gagal menyimpan AUTH: " + names.get(index)); clearWorkflow(); return; } auth.delete(); int next = index + 1; prefs.edit().putInt(KEY_CREATE_INDEX, next).apply(); if (next >= names.size()) { finishCreate(); return; } updateStatus("✓ Tersimpan " + names.get(index) + ". Membuka akun " + names.get(next) + "..."); if (launchGame()) workflowLaunchPending = true; else clearWorkflow(); }
    private void finishCreate() { int total = split(prefs.getString(KEY_CREATE_NAMES, "")).size(); clearWorkflow(); loadAccounts(); updateStatus("✓ Pembuatan akun selesai. Total: " + total); }
    private boolean copyAuth(Account account) {
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) {
            logActivity("✗ COPY gagal | " + account.name + " | Target AUTH belum dipilih.");
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return false;
        }
        String targetName = nameOf(target);
        logActivity("COPY START | Source: Library/" + account.name + "/" + AUTH_FILE + " | Target: " + targetName + "/" + AUTH_FILE);
        DocumentFile old = target.findFile(AUTH_FILE);
        if (old != null) {
            logActivity("TARGET AUTH lama ditemukan | " + targetName + "/" + AUTH_FILE + " | Menghapus sebelum copy.");
            old.delete();
        }
        boolean success = copyFile(account.file, target, AUTH_FILE);
        if (success) {
            logActivity("✓ COPY SUCCESS | " + account.name + " -> " + targetName + "/" + AUTH_FILE);
        } else {
            logActivity("✗ COPY FAILED | " + account.name + " -> " + targetName + "/" + AUTH_FILE);
        }
        return success;
    }
    private boolean copyFile(DocumentFile source, DocumentFile parent, String name) { DocumentFile dest = parent.findFile(name); if (dest != null) dest.delete(); dest = parent.createFile("application/octet-stream", name); if (dest == null) return false; try (InputStream in = getContentResolver().openInputStream(source.getUri()); OutputStream out = getContentResolver().openOutputStream(dest.getUri(), "w")) { if (in == null || out == null) throw new Exception("stream null"); byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n); out.flush(); return true; } catch (Exception e) { dest.delete(); return false; } }
    private boolean launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if (i == null) {
            logActivity("✗ LAUNCH EFOOTBALL gagal | Package " + PACKAGE + " tidak ditemukan.");
            updateStatus("Package " + PACKAGE + " tidak ditemukan.");
            return false;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        logActivity("✓ LAUNCH EFOOTBALL | Package: " + PACKAGE);
        return true;
    }
    private boolean validDir(DocumentFile d) { return d != null && d.isDirectory(); }
    private String getMode() { return prefs.getString(KEY_MODE, MODE_NONE); }
    private void setMode(String mode) { prefs.edit().putString(KEY_MODE, mode).apply(); }
    private void clearWorkflow() { workflowLaunchPending = false; prefs.edit().putString(KEY_MODE, MODE_NONE).remove(KEY_CREATE_NAMES).remove(KEY_CREATE_INDEX).remove(KEY_CREATE_COUNT).apply(); }
    private List<String> split(String value) { if (value == null || value.isEmpty()) return Collections.emptyList(); return Arrays.asList(value.split("\\|", -1)); }
    private void updateStatus(String s) { if (status != null) status.setText(s); }

    private void logActivity(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = time + "  " + message;
        String old = prefs.getString(KEY_ACTIVITY_LOG, "");
        String combined = old.isEmpty() ? entry : old + "\n" + entry;
        String[] lines = combined.split("\\n");
        int start = Math.max(0, lines.length - 300);
        StringBuilder trimmed = new StringBuilder();
        for (int i = start; i < lines.length; i++) {
            if (trimmed.length() > 0) trimmed.append("\n");
            trimmed.append(lines[i]);
        }
        prefs.edit().putString(KEY_ACTIVITY_LOG, trimmed.toString()).apply();
    }

    private void showActivityLog() {
        TextView logText = new TextView(this);
        logText.setText(prefs.getString(KEY_ACTIVITY_LOG, "Belum ada aktivitas."));
        logText.setTextIsSelectable(true);
        logText.setTextSize(13);
        logText.setPadding(24, 16, 24, 16);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logText);

        new android.app.AlertDialog.Builder(this)
                .setTitle("📋 LOG AKTIVITAS")
                .setView(scroll)
                .setNegativeButton("TUTUP", null)
                .setNeutralButton("HAPUS LOG", (d, w) -> {
                    prefs.edit().remove(KEY_ACTIVITY_LOG).apply();
                    updateStatus("Log aktivitas dihapus.");
                })
                .show();
    }

    private void startAutoLogin() {
        if (accounts.isEmpty()) { updateStatus("Library kosong."); return; }
        if (autoLoginRunning) return;
        clearWorkflow();
        autoLoginRunning = true;
        autoLoginIndex = 0;
        logActivity("=== AUTO LOGIN START | Akun ditemukan: " + accounts.size() + " ===");
        runNextAutoLogin();
    }

    private void runNextAutoLogin() {
        if (!autoLoginRunning || autoLoginIndex >= accounts.size()) {
            if (autoLoginRunning) {
                logActivity("=== AUTO LOGIN FINISH | Diproses: " + autoLoginIndex + " | Akun ditemukan: " + accounts.size() + " ===");
            } else {
                logActivity("=== AUTO LOGIN STOP | Diproses sampai index: " + autoLoginIndex + " ===");
            }
            autoLoginRunning = false;
            updateStatus("✓ AUTO LOGIN selesai. Total: " + accounts.size());
            return;
        }
        Account account = accounts.get(autoLoginIndex);
        logActivity("AUTO LOGIN [" + (autoLoginIndex + 1) + "/" + accounts.size() + "] | Akun: " + account.name);
        updateStatus("AUTO LOGIN [" + (autoLoginIndex + 1) + "/" + accounts.size() + "]: " + account.name);
        if (!copyAuth(account)) {
            logActivity("AUTO LOGIN berhenti karena COPY gagal | Akun: " + account.name);
            autoLoginRunning = false;
            return;
        }
        boolean launched = launchGame();
        logActivity("AUTO LOGIN | " + account.name + " | Launch result: " + (launched ? "SUCCESS" : "FAILED"));
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
        boolean wasRunning = autoLoginRunning;
        autoLoginRunning = false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        if (wasRunning) logActivity("AUTO LOGIN STOP ditekan | Diproses: " + autoLoginIndex);
    }

    @Override protected void onDestroy() {
        stopAutoLogin();
        super.onDestroy();
    }

    static class Account { final String name; final DocumentFile file; Account(String name, DocumentFile file) { this.name = name; this.file = file; } }
}
