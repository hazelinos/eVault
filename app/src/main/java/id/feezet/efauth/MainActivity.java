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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String AUTH_FILE = "online_user_id_data.dat";
    private static final String PREFS = "paths";
    private static final String KEY_LIBRARY = "library";
    private static final String KEY_TARGET = "target";
    private static final String PACKAGE = "epl.idsphone";

    private SharedPreferences prefs;
    private TextView status;
    private TextView accountsText;
    private EditText delayEdit;
    private Spinner accountSpinner;
    private ArrayList<Account> accounts = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean autoRunning = false;
    private int autoIndex = 0;

    private ActivityResultLauncher<Intent> pickLibrary =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    saveTree(KEY_LIBRARY, r.getData().getData());
                    loadAccounts();
                }
            });

    private ActivityResultLauncher<Intent> pickTarget =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    saveTree(KEY_TARGET, r.getData().getData());
                    updateStatus("Target AUTH tersimpan.");
                }
            });

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        loadAccounts();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 28, 32, 24);

        TextView title = new TextView(this);
        title.setText("eFootball AUTH Manager");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        root.addView(title);

        Button library = new Button(this);
        library.setText("📁 Pilih AUTH Library");
        library.setOnClickListener(v -> chooseTree(pickLibrary));
        root.addView(library);

        Button target = new Button(this);
        target.setText("🎯 Pilih Folder AUTH eFootball");
        target.setOnClickListener(v -> chooseTree(pickTarget));
        root.addView(target);

        accountsText = new TextView(this);
        accountsText.setText("Akun: memuat...");
        accountsText.setPadding(0, 18, 0, 8);
        root.addView(accountsText);

        accountSpinner = new Spinner(this);
        root.addView(accountSpinner);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        Button switchBtn = new Button(this);
        switchBtn.setText("SWITCH");
        switchBtn.setOnClickListener(v -> switchSelected());
        row.addView(switchBtn, new LinearLayout.LayoutParams(0, -2, 1));

        Button autoBtn = new Button(this);
        autoBtn.setText("AUTO 1→100");
        autoBtn.setOnClickListener(v -> startAuto());
        row.addView(autoBtn, new LinearLayout.LayoutParams(0, -2, 1));

        Button stopBtn = new Button(this);
        stopBtn.setText("STOP");
        stopBtn.setOnClickListener(v -> stopAuto());
        row.addView(stopBtn, new LinearLayout.LayoutParams(0, -2, 1));

        root.addView(row);

        LinearLayout manageRow = new LinearLayout(this);
        manageRow.setOrientation(LinearLayout.HORIZONTAL);

        Button deleteDataBtn = new Button(this);
        deleteDataBtn.setText("HAPUS DATA");
        deleteDataBtn.setOnClickListener(v -> deleteSelectedAccount());
        manageRow.addView(deleteDataBtn, new LinearLayout.LayoutParams(0, -2, 1));

        Button deleteAuthBtn = new Button(this);
        deleteAuthBtn.setText("HAPUS AUTH SAAT INI");
        deleteAuthBtn.setOnClickListener(v -> deleteCurrentAuth());
        manageRow.addView(deleteAuthBtn, new LinearLayout.LayoutParams(0, -2, 1));

        root.addView(manageRow);

        delayEdit = new EditText(this);
        delayEdit.setHint("Delay antar akun (detik)");
        delayEdit.setInputType(2);
        delayEdit.setText("10");
        root.addView(delayEdit);

        status = new TextView(this);
        status.setText("Siap.");
        status.setPadding(0, 18, 0, 0);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void chooseTree(ActivityResultLauncher<Intent> launcher) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                   Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                   Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                   Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        launcher.launch(i);
    }

    private void saveTree(String key, Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) {}
        prefs.edit().putString(key, uri.toString()).apply();
    }

    private DocumentFile getTree(String key) {
        String s = prefs.getString(key, null);
        if (s == null) return null;
        return DocumentFile.fromTreeUri(this, Uri.parse(s));
    }

    private void loadAccounts() {
        DocumentFile lib = getTree(KEY_LIBRARY);
        accounts.clear();
        if (lib != null && lib.isDirectory()) {
            DocumentFile[] children = lib.listFiles();
            for (DocumentFile d : children) {
                if (!d.isDirectory()) continue;
                DocumentFile auth = d.findFile(AUTH_FILE);
                if (auth != null && auth.isFile()) {
                    accounts.add(new Account(d.getName() == null ? "Unnamed" : d.getName(), auth));
                }
            }
        }

        accounts.sort((a,b) -> {
            int na = leadingNumber(a.name), nb = leadingNumber(b.name);
            if (na != nb) return Integer.compare(na, nb);
            return a.name.compareToIgnoreCase(b.name);
        });

        ArrayList<String> names = new ArrayList<>();
        for (Account a : accounts) names.add(a.name);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, names);
        accountSpinner.setAdapter(adapter);
        accountsText.setText("Akun ditemukan: " + accounts.size());
    }

    private int leadingNumber(String s) {
        Matcher m = Pattern.compile("^\\s*(\\d+)").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    private void switchSelected() {
        if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        int pos = accountSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= accounts.size()) return;
        if (copyAuth(accounts.get(pos))) launchGame();
    }

    private void deleteSelectedAccount() {
        if (accounts.isEmpty()) {
            updateStatus("Library kosong / belum dipilih.");
            return;
        }

        int pos = accountSpinner.getSelectedItemPosition();
        if (pos < 0 || pos >= accounts.size()) return;

        Account account = accounts.get(pos);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hapus Data")
                .setMessage("Hapus data akun \"" + account.name + "\" dari library?\n\nFile AUTH akun ini akan ikut dihapus dari library.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (d, w) -> {
                    DocumentFile folder = account.file.getParentFile();
                    if (folder != null && folder.delete()) {
                        updateStatus("✓ Data dihapus: " + account.name);
                        loadAccounts();
                    } else {
                        updateStatus("Gagal menghapus data akun.");
                    }
                })
                .show();
    }

    private void deleteCurrentAuth() {
        DocumentFile target = getTree(KEY_TARGET);
        if (target == null || !target.isDirectory()) {
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return;
        }

        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth == null || !auth.isFile()) {
            updateStatus("Tidak ada AUTH saat ini.");
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Hapus AUTH Saat Ini")
                .setMessage("Hapus file AUTH yang sedang terpasang di target?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (d, w) -> {
                    if (auth.delete()) updateStatus("✓ AUTH saat ini dihapus.");
                    else updateStatus("Gagal menghapus AUTH. Periksa izin folder.");
                })
                .show();
    }

    private boolean copyAuth(Account account) {
        DocumentFile target = getTree(KEY_TARGET);
        if (target == null || !target.isDirectory()) {
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return false;
        }

        DocumentFile old = target.findFile(AUTH_FILE);
        if (old != null) old.delete();

        DocumentFile dest = target.createFile("application/octet-stream", AUTH_FILE);
        if (dest == null) {
            updateStatus("Gagal membuat file target. Periksa izin folder.");
            return false;
        }

        try {
            java.io.InputStream in = getContentResolver().openInputStream(account.file.getUri());
            java.io.OutputStream out = getContentResolver().openOutputStream(dest.getUri(), "w");
            if (in == null || out == null) throw new Exception("stream null");

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            out.flush();
            out.close();

            updateStatus("✓ AUTH dipasang: " + account.name);
            return true;
        } catch (Exception e) {
            updateStatus("Gagal copy AUTH: " + e.getMessage());
            return false;
        }
    }

    private void launchGame() {
        Intent i = getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if (i == null) {
            updateStatus("Package " + PACKAGE + " tidak ditemukan.");
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void startAuto() {
        if (accounts.isEmpty()) { updateStatus("Library kosong."); return; }
        if (autoRunning) return;

        autoRunning = true;
        autoIndex = 0;
        runNext();
    }

    private void runNext() {
        if (!autoRunning || autoIndex >= accounts.size()) {
            autoRunning = false;
            updateStatus("✓ Auto selesai. Total: " + accounts.size());
            return;
        }

        Account a = accounts.get(autoIndex);
        updateStatus("[" + (autoIndex + 1) + "/" + accounts.size() + "] " + a.name);

        if (copyAuth(a)) {
            launchGame();
            autoIndex++;
            long delay = getDelayMs();
            handler.postDelayed(this::runNext, delay);
        } else {
            autoRunning = false;
        }
    }

    private long getDelayMs() {
        try {
            int sec = Integer.parseInt(delayEdit.getText().toString().trim());
            return Math.max(2, sec) * 1000L;
        } catch (Exception e) {
            return 10000L;
        }
    }

    private void stopAuto() {
        autoRunning = false;
        handler.removeCallbacksAndMessages(null);
        updateStatus("AUTO dihentikan.");
    }

    private void updateStatus(String s) {
        if (status != null) status.setText(s);
    }

    @Override protected void onDestroy() {
        autoRunning = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    static class Account {
        String name;
        DocumentFile file;
        Account(String n, DocumentFile f) { name = n; file = f; }
    }
}
