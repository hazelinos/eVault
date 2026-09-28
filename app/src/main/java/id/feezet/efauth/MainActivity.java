package id.feezet.efauth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private static final String AUTH_FILE = "online_user_id_data.dat";
    private static final String PREFS = "paths";
    private static final String KEY_LIBRARY = "library";
    private static final String KEY_TARGET = "target";
    private static final String KEY_MODE = "workflow_mode";
    private static final String KEY_CREATE_NAMES = "create_names";
    private static final String KEY_CREATE_INDEX = "create_index";
    private static final String KEY_CREATE_COUNT = "create_count";
    private static final String KEY_CREATE_CATEGORY = "create_category";
    private static final String KEY_CREATE_NAMING = "create_naming";
    private static final String KEY_AUTO_LOGIN_DELAY = "KEY_AUTO_LOGIN_DELAY";
    private static final String KEY_ACTIVITY_LOG = "activity_log";
    private static final String KEY_DISCORD_WEBHOOK = "discord_webhook";
    private static final String KEY_LOGIN_VERIFIED = "login_verified";
    private static final String KEY_AUTO_LOGIN_MODE = "auto_login_mode";
    private static final String KEY_AUTO_LOGIN_CATEGORY = "auto_login_category";
    private static final String KEY_AUTO_LOGIN_INDEX = "auto_login_index";
    private static final String KEY_AUTO_LOGIN_PENDING = "auto_login_pending";
    private static final String MODE_AUTO_LOGIN = "auto_login";
    private static final int DEFAULT_AUTO_LOGIN_DELAY = 5;
    private static final int MAX_ACTIVITY_LOGS = 300;
    private static final String NAMING_NUMERIC = "numeric";
    private static final String NAMING_LETTERED = "lettered";
    private static final String MODE_NONE = "none";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";

    private SharedPreferences prefs;
    private TextView status;
    private TextView accountsText;
    private Spinner accountSpinner;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private final ArrayList<Account> filteredAccounts = new ArrayList<>();
    private final ArrayList<Account> autoLoginAccounts = new ArrayList<>();
    private boolean workflowLaunchPending = false;
    private final Handler autoLoginHandler = new Handler(Looper.getMainLooper());
    private boolean autoLoginRunning = false;
    private int autoLoginIndex = 0;
    private EditText autoLoginDelayEdit;
    private int setupCardId = View.NO_ID;
    private String accountFilter = "";

    private final ActivityResultLauncher<Intent> pickLibrary = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                    saveTree(KEY_LIBRARY, r.getData().getData());
                    loadAccounts();
                    if (accounts.isEmpty()) {
                        // Retry after a short delay: DocumentFile may not be ready immediately
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            loadAccounts();
                            updateStatus(accounts.isEmpty()
                                ? "✓ Library tersimpan, tapi 0 akun ditemukan. Cek struktur folder."
                                : "✓ AUTH Library tersimpan. " + accounts.size() + " akun.");
                        }, 800);
                    } else {
                        updateStatus("✓ AUTH Library tersimpan. " + accounts.size() + " akun.");
                    }
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

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        loadAccounts();
    }

    private boolean createResumeDialogShown = false;

    @Override protected void onResume() {
        super.onResume();

        if (workflowLaunchPending) {
            workflowLaunchPending = false;
            if (MODE_CREATE.equals(getMode())) {
                showCreateExitDialog();
            } else if (MODE_AUTO_LOGIN.equals(getMode())) {
                showAutoLoginConfirmationDialog();
            }
            return;
        }

        // If eVault was closed/restarted while Create Account was in progress,
        // restore the saved progress instead of starting from the beginning.
        if (MODE_CREATE.equals(getMode())
                && !createResumeDialogShown
                && hasPendingCreateWorkflow()) {
            createResumeDialogShown = true;
            showCreateResumeDialog();
        }
    }

    private boolean hasPendingCreateWorkflow() {
        List<String> names = split(prefs.getString(KEY_CREATE_NAMES, ""));
        int index = prefs.getInt(KEY_CREATE_INDEX, 0);
        return !names.isEmpty() && index >= 0 && index < names.size();
    }

    private void showCreateResumeDialog() {
        List<String> names = split(prefs.getString(KEY_CREATE_NAMES, ""));
        int index = prefs.getInt(KEY_CREATE_INDEX, 0);
        String category = prefs.getString(KEY_CREATE_CATEGORY, "AKUN");
        if (names.isEmpty() || index < 0 || index >= names.size()) {
            createResumeDialogShown = false;
            return;
        }

        String current = names.get(index);
        int total = names.size();

        addActivityLog("CREATE ACCOUNT resume tersedia: [" + (index + 1) + "/" + total + "] "
                + current + " (" + category + ")");

        new AlertDialog.Builder(this)
            .setTitle("LANJUTKAN BUAT AKUN?")
            .setMessage("Progress: " + (index + 1) + "/" + total
                    + "\nAkun berikutnya: " + current
                    + "\nKategori: " + category)
            .setCancelable(false)
            .setPositiveButton("LANJUTKAN", (d, w) -> {
                createResumeDialogShown = false;
                addActivityLog("CREATE ACCOUNT dilanjutkan dari: " + current);
                launchCurrentCreateAccount();
            })
            .setNegativeButton("NANTI", (d, w) -> {
                createResumeDialogShown = false;
                updateStatus("CREATE ACCOUNT tersimpan. Berikutnya: " + current);
            })
            .show();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.bgBase));
        root.setPadding(dp(20), dp(28), dp(20), dp(24));

        // ---- Header ----
        TextView title = new TextView(this);
        title.setText("eVault");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(color(R.color.textPrimary));
        root.addView(title, fullLp());

        TextView subtitle = new TextView(this);
        subtitle.setText("eFootball AUTH Manager");
        subtitle.setTextSize(13);
        subtitle.setTextColor(color(R.color.accent));
        subtitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams subLp = fullLp();
        subLp.bottomMargin = dp(24);
        root.addView(subtitle, subLp);

        // ---- Setup (collapsed) ----
        TextView setupToggle = new TextView(this);
        setupToggle.setText("⚙️ Setup");
        setupToggle.setTextSize(13);
        setupToggle.setTextColor(color(R.color.textSecondary));
        setupToggle.setPadding(0, 0, 0, dp(8));
        setupToggle.setOnClickListener(v -> toggleSetupVisibility());
        root.addView(setupToggle, fullLp());

        LinearLayout setupCard = card();
        setupCard.setVisibility(View.GONE);
        setupCard.setId(View.generateViewId());
        this.setupCardId = setupCard.getId();
        Button library = secondaryButton("PILIH AUTH LIBRARY", v -> chooseLibrary());
        setupCard.addView(library, spacedLp(dp(4)));
        Button target = secondaryButton("PILIH FOLDER AUTH EFOOTBALL", v -> chooseTree(pickTarget));
        setupCard.addView(target, spacedLp(dp(10)));
        Button webhookBtn = secondaryButton("🔔 DISCORD WEBHOOK", v -> showDiscordWebhookDialog());
        setupCard.addView(webhookBtn, spacedLp(dp(10)));

        // Delay input (moved to Setup)
        TextView delayLabel = new TextView(this);
        delayLabel.setText("Delay AUTO LOGIN (detik)");
        delayLabel.setTextSize(12);
        delayLabel.setTextColor(color(R.color.textSecondary));
        setupCard.addView(delayLabel, spacedLp(dp(14)));
        autoLoginDelayEdit = new EditText(this);
        autoLoginDelayEdit.setInputType(2);
        autoLoginDelayEdit.setSingleLine(true);
        autoLoginDelayEdit.setText(String.valueOf(prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY)));
        autoLoginDelayEdit.setHint(String.valueOf(DEFAULT_AUTO_LOGIN_DELAY));
        autoLoginDelayEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveAutoLoginDelay();
        });
        styleInput(autoLoginDelayEdit);
        setupCard.addView(autoLoginDelayEdit, spacedLp(dp(6)));

        root.addView(setupCard, cardLp());

        // ---- Akun (main focus) ----
        accountsText = new TextView(this);
        accountsText.setText("Akun: 0 ditemukan");
        accountsText.setTextColor(color(R.color.textSecondary));
        accountsText.setTextSize(13);
        accountsText.setPadding(0, dp(4), 0, dp(8));
        root.addView(accountsText, fullLp());

        // Search input
        EditText searchInput = new EditText(this);
        searchInput.setHint("Cari akun...");
        searchInput.setSingleLine(true);
        styleInput(searchInput);
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterAccounts(s.toString()); }
            public void afterTextChanged(android.text.Editable s) {}
        });
        LinearLayout.LayoutParams searchLp = fullLp();
        searchLp.bottomMargin = dp(8);
        root.addView(searchInput, searchLp);

        accountSpinner = new Spinner(this);
        accountSpinner.setBackground(makeDrawableWithStroke(Color.parseColor("#16213E"), Color.parseColor("#2A3A5A"), dp(6)));
        accountSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams spinnerLp = fullLp();
        spinnerLp.bottomMargin = dp(12);
        root.addView(accountSpinner, spinnerLp);

        // Actions row 1: SWITCH + AUTO LOGIN
        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        Button switchBtn = primaryButton("SWITCH", v -> switchSelected());
        row1.addView(switchBtn, weightLp(dp(8), 0));
        Button autoBtn = primaryButton("AUTO LOGIN", v -> startAutoLogin());
        row1.addView(autoBtn, weightLp(dp(8), 1));
        LinearLayout.LayoutParams row1Lp = fullLp();
        row1Lp.bottomMargin = dp(8);
        root.addView(row1, row1Lp);

        // Actions row 2: HAPUS DATA + BUAT BARU
        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        Button deleteDataBtn = dangerButton("HAPUS DATA", v -> showDeleteDataConfirmDialog());
        row2.addView(deleteDataBtn, weightLp(dp(8), 0));
        Button createBtn = secondaryButton("BUAT BARU", v -> showCreateDialog());
        row2.addView(createBtn, weightLp(dp(8), 1));
        LinearLayout.LayoutParams row2Lp = fullLp();
        row2Lp.bottomMargin = dp(16);
        root.addView(row2, row2Lp);

        // ---- Save AUTH ----
        Button saveAuthBtn = primaryButton("SIMPAN AUTH", v -> showSaveAuthDialog());
        LinearLayout.LayoutParams saveAuthLp = fullLp();
        saveAuthLp.bottomMargin = dp(8);
        root.addView(saveAuthBtn, saveAuthLp);

        // ---- Activity Log ----
        Button activityLogBtn = secondaryButton("📋 LOG AKTIVITAS", v -> showActivityLogDialog());
        LinearLayout.LayoutParams activityLogLp = fullLp();
        activityLogLp.bottomMargin = dp(16);
        root.addView(activityLogBtn, activityLogLp);

        // ---- Enter game (big primary) ----
        Button enterBtn = primaryButton("MASUK EFOOTBALL", v -> { stopAutoLogin(); clearWorkflow(); launchGame(); });
        enterBtn.setTextSize(15);
        LinearLayout.LayoutParams enterLp = fullLp();
        enterLp.topMargin = dp(6);
        enterLp.height = dp(52);
        root.addView(enterBtn, enterLp);

        // ---- Status ----
        status = new TextView(this);
        status.setText("Siap.");
        status.setTextColor(color(R.color.textSecondary));
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = fullLp();
        statusLp.topMargin = dp(16);
        root.addView(status, statusLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.bgBase));
        scroll.addView(root);
        setContentView(scroll);
    }

    // ---------- UI helpers ----------
    private int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private int color(int resId) { 
        return ContextCompat.getColor(this, resId); 
    }

    private GradientDrawable makeDrawable(int color, int cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(cornerRadius);
        drawable.setColor(color);
        return drawable;
    }

    private GradientDrawable makeDrawableWithStroke(int fillColor, int strokeColor, int cornerRadius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(cornerRadius);
        drawable.setColor(fillColor);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(makeDrawableWithStroke(Color.parseColor("#16213E"), Color.parseColor("#2A3A5A"), dp(8)));
        c.setPadding(dp(16), dp(14), dp(16), dp(16));
        return c;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        return lp;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setLetterSpacing(0.12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(color(R.color.textSecondary));
        LinearLayout.LayoutParams lp = fullLp();
        lp.bottomMargin = dp(10);
        tv.setLayoutParams(lp);
        return tv;
    }

    private Button baseButton(String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(true);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
        b.setMinHeight(dp(46));
        b.setPadding(dp(12), dp(10), dp(12), dp(10));
        b.setOnClickListener(listener);
        return b;
    }

    private Button primaryButton(String text, View.OnClickListener l) {
        Button b = baseButton(text, l);
        b.setBackground(makeDrawable(Color.parseColor("#058CD7"), dp(6)));
        b.setTextColor(Color.WHITE);
        return b;
    }

    private Button secondaryButton(String text, View.OnClickListener l) {
        Button b = baseButton(text, l);
        b.setBackground(makeDrawable(Color.parseColor("#552CB7"), dp(6)));
        b.setTextColor(Color.parseColor("#F8F9FA"));
        return b;
    }

    private Button dangerButton(String text, View.OnClickListener l) {
        Button b = baseButton(text, l);
        b.setBackground(makeDrawable(Color.parseColor("#FD5A46"), dp(6)));
        b.setTextColor(Color.WHITE);
        return b;
    }

    private void styleInput(EditText e) {
        e.setBackground(makeDrawableWithStroke(Color.parseColor("#16213E"), Color.parseColor("#2A3A5A"), dp(6)));
        e.setTextColor(color(R.color.textPrimary));
        e.setHintTextColor(color(R.color.textSecondary));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
    }

    private LinearLayout.LayoutParams fullLp() { return new LinearLayout.LayoutParams(-1, -2); }

    private LinearLayout.LayoutParams spacedLp(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = topMargin;
        return lp;
    }

    private LinearLayout.LayoutParams weightLp(int gap, int side) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        if (side == 0) lp.rightMargin = gap; else lp.leftMargin = gap;
        return lp;
    }
    private void chooseLibrary() { chooseTree(pickLibrary); }

    private void chooseTree(ActivityResultLauncher<Intent> launcher) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        launcher.launch(i);
    }

    private Intent folderPickerIntent() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return i;
    }

    private void saveTree(String key, Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) {}
        prefs.edit().putString(key, uri.toString()).commit();
    }

    private DocumentFile getTree(String key) {
        String s = prefs.getString(key, null);
        return s == null ? null : DocumentFile.fromTreeUri(this, Uri.parse(s));
    }

    private void loadAccounts() {
        accounts.clear();
        DocumentFile lib = getTree(KEY_LIBRARY);
        if (validDir(lib)) {
            for (DocumentFile category : lib.listFiles()) {
                if (category.isDirectory()) collectAccounts(category, nameOf(category));
            }
        }
        Collections.sort(accounts, (a, b) -> {
            int categoryCompare = a.category.compareToIgnoreCase(b.category);
            if (categoryCompare != 0) return categoryCompare;
            int sa = nameToSequence(a.name);
            int sb = nameToSequence(b.name);
            if (sa > 0 && sb > 0 && sa != sb) return Integer.compare(sa, sb);
            if (sa > 0 && sb <= 0) return -1;
            if (sa <= 0 && sb > 0) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        refreshAccountSpinner();
    }

    private void refreshAccountSpinner() {
        filteredAccounts.clear();
        String q = accountFilter == null ? "" : accountFilter.trim().toLowerCase();
        for (Account a : accounts) {
            String label = (a.category + " / " + a.name).toLowerCase();
            if (q.isEmpty() || label.contains(q)) filteredAccounts.add(a);
        }
        ArrayList<String> names = new ArrayList<>();
        for (Account a : filteredAccounts) names.add(a.category + " / " + a.name);
        accountSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        if (q.isEmpty()) {
            accountsText.setText("Akun: " + accounts.size() + " ditemukan");
        } else {
            accountsText.setText("Akun: " + filteredAccounts.size() + " / " + accounts.size() + " cocok");
        }
    }

    private void filterAccounts(String query) {
        accountFilter = query;
        refreshAccountSpinner();
    }

    private void toggleSetupVisibility() {
        View card = findViewById(setupCardId);
        if (card != null) card.setVisibility(card.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
    }

    private void collectAccounts(DocumentFile categoryRoot, String categoryName) {
        for (DocumentFile child : categoryRoot.listFiles()) {
            if (!child.isDirectory()) continue;
            DocumentFile auth = child.findFile(AUTH_FILE);
            if (auth != null && auth.isFile()) accounts.add(new Account(categoryName, nameOf(child), auth));
            else collectAccounts(child, categoryName);
        }
    }

    private DocumentFile accountRoot(DocumentFile library) { return validDir(library) ? library : null; }

    private DocumentFile categoryRoot(DocumentFile library, String categoryName) {
        if (!validDir(library) || categoryName == null || categoryName.trim().isEmpty()) return null;
        DocumentFile category = library.findFile(categoryName);
        return validDir(category) ? category : library.createDirectory(categoryName);
    }

    private String nameOf(DocumentFile d) { return d.getName() == null ? "Unnamed" : d.getName(); }

    private void switchSelected() {
        stopAutoLogin(); clearWorkflow();
        if (filteredAccounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        int pos = accountSpinner.getSelectedItemPosition();
        if (pos >= 0 && pos < filteredAccounts.size() && copyAuth(filteredAccounts.get(pos))) launchGame();
    }

    private void deleteCurrentAuth() {
        stopAutoLogin(); clearWorkflow(); DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; }
        new AlertDialog.Builder(this).setTitle("Hapus AUTH Saat Ini").setMessage("Hapus file AUTH yang sedang terpasang di target?").setNegativeButton("BATAL", null).setPositiveButton("HAPUS", (d,w) -> updateStatus(auth.delete() ? "✓ AUTH saat ini dihapus." : "Gagal menghapus AUTH. Periksa izin folder.")).show();
    }

    private void showSaveAuthDialog() {
        stopAutoLogin();
        clearWorkflow();

        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) {
            showInfoDialog("SIMPAN AUTH", "Pilih folder AUTH eFootball terlebih dahulu di Setup.");
            return;
        }

        DocumentFile installedAuth = target.findFile(AUTH_FILE);
        if (installedAuth == null || !installedAuth.isFile()) {
            showInfoDialog("SIMPAN AUTH", "Tidak ada AUTH yang terpasang di target eFootball.");
            updateStatus("Tidak ada AUTH terpasang di target eFootball.");
            return;
        }

        DocumentFile library = getTree(KEY_LIBRARY);
        if (!validDir(library)) {
            showInfoDialog("SIMPAN AUTH", "Pilih AUTH Library terlebih dahulu di Setup.");
            return;
        }

        List<String> categories = getLibraryCategories();
        ArrayList<String> choices = new ArrayList<>(categories);
        choices.add("＋ BUAT KATEGORI BARU");

        new AlertDialog.Builder(this)
            .setTitle("SIMPAN AUTH")
            .setMessage("Pilih kategori penyimpanan.")
            .setItems(choices.toArray(new String[0]), (d, which) -> {
                if (which == choices.size() - 1) {
                    showNewSaveCategoryDialog();
                } else {
                    showSaveAuthNameDialog(choices.get(which));
                }
            })
            .setNegativeButton("BATAL", null)
            .show();
    }

    private void showNewSaveCategoryDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nama kategori");
        styleInput(input);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("BUAT KATEGORI BARU")
            .setMessage("Masukkan nama kategori untuk AUTH ini.")
            .setView(input)
            .setNegativeButton("BATAL", null)
            .setPositiveButton("LANJUT", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String categoryName = input.getText().toString().trim();
            if (categoryName.isEmpty()) {
                input.setError("Nama kategori wajib diisi.");
                return;
            }
            if (categoryName.contains("/") || categoryName.contains("\\")) {
                input.setError("Nama kategori tidak boleh mengandung / atau \\.");
                return;
            }

            DocumentFile library = getTree(KEY_LIBRARY);
            if (!validDir(library)) {
                dialog.dismiss();
                updateStatus("AUTH Library belum dipilih.");
                return;
            }

            DocumentFile existing = findChildIgnoreCase(library, categoryName);
            if (existing != null && existing.isDirectory()) {
                input.setError("Kategori sudah ada. Pilih kategori tersebut dari menu sebelumnya.");
                return;
            }

            dialog.dismiss();
            showSaveAuthNameDialog(categoryName);
        }));

        dialog.show();
    }

    private void showSaveAuthNameDialog(String categoryName) {
        DocumentFile library = getTree(KEY_LIBRARY);
        if (!validDir(library)) {
            updateStatus("AUTH Library belum dipilih.");
            return;
        }

        DocumentFile categoryDir = categoryRoot(library, categoryName);
        if (!validDir(categoryDir)) {
            updateStatus("Gagal membuka kategori: " + categoryName);
            return;
        }

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Nama file / akun");
        input.setText("");
        input.setSelectAllOnFocus(true);
        styleInput(input);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(4));
        box.addView(input, fullLp());

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("NAMA AUTH")
            .setMessage("Kategori: " + categoryName + "\nMasukkan nama penyimpanan AUTH.")
            .setView(box)
            .setNegativeButton("KEMBALI", null)
            .setPositiveButton("SIMPAN", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String accountName = input.getText().toString().trim();
            if (accountName.isEmpty()) {
                input.setError("Nama wajib diisi.");
                return;
            }
            if (accountName.contains("/") || accountName.contains("\\")) {
                input.setError("Nama tidak boleh mengandung / atau \\.");
                return;
            }

            DocumentFile duplicate = findChildIgnoreCase(categoryDir, accountName);
            if (duplicate != null) {
                input.setError("Nama "" + accountName + "" sudah ada di kategori ini.");
                input.requestFocus();
                return;
            }

            DocumentFile accountDir = categoryDir.createDirectory(accountName);
            if (!validDir(accountDir)) {
                input.setError("Gagal membuat penyimpanan. Coba nama lain.");
                return;
            }

            DocumentFile target = getTree(KEY_TARGET);
            DocumentFile installedAuth = validDir(target) ? target.findFile(AUTH_FILE) : null;
            if (installedAuth == null || !installedAuth.isFile()) {
                accountDir.delete();
                dialog.dismiss();
                showInfoDialog("SIMPAN AUTH", "AUTH yang terpasang sudah tidak ditemukan di target eFootball.");
                updateStatus("Tidak ada AUTH terpasang di target eFootball.");
                return;
            }

            boolean saved = copyFile(installedAuth, accountDir, AUTH_FILE);
            if (!saved) {
                accountDir.delete();
                input.setError("Gagal menyimpan AUTH. Periksa izin folder.");
                return;
            }

            addActivityLog("SIMPAN AUTH berhasil: " + categoryName + " / " + accountName);
            updateStatus("✓ AUTH tersimpan: " + categoryName + " / " + accountName);
            dialog.dismiss();
            loadAccounts();
        }));

        dialog.show();
    }

    private DocumentFile findChildIgnoreCase(DocumentFile parent, String wantedName) {
        if (!validDir(parent) || wantedName == null) return null;
        for (DocumentFile child : parent.listFiles()) {
            String name = child.getName();
            if (name != null && name.equalsIgnoreCase(wantedName)) return child;
        }
        return null;
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showDeleteDataConfirmDialog() {
        stopAutoLogin(); clearWorkflow();
        new AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Hapus AUTH di folder AUTH eFootball?")
            .setNegativeButton("TIDAK", null)
            .setPositiveButton("YA", (d,w) -> executeDeleteData())
            .show();
    }

    private void executeDeleteData() {
        DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH di folder AUTH eFootball."); return; }
        if (auth.delete()) updateStatus("✓ Auth sudah terhapus.");
        else updateStatus("Gagal menghapus AUTH. Periksa izin folder.");
    }

    private void showCreateDialog() {
        stopAutoLogin(); clearWorkflow(); final EditText input=new EditText(this); input.setInputType(2); input.setSingleLine(true); input.setHint("Jumlah akun");
        new AlertDialog.Builder(this).setTitle("BUAT AKUN BARU").setMessage("Mau buat berapa akun?").setView(input).setNegativeButton("BATAL",null).setPositiveButton("LANJUT",(d,w)->{
            int count; try{count=Integer.parseInt(input.getText().toString().trim());}catch(Exception e){count=0;}
            if(count<1){updateStatus("Jumlah akun minimal 1.");return;} prefs.edit().putInt(KEY_CREATE_COUNT,count).apply(); showCreateNamingDialog(count);
        }).show();
    }

    private void showCreateNamingDialog(int count) {
        String[] modes = {
            "Angka (1-1000)",
            "Huruf batch (1-10, a1-a10, b1-10, ...)"
        };
        new AlertDialog.Builder(this).setTitle("Pola nama akun").setItems(modes,(d,which)->{
            prefs.edit().putString(KEY_CREATE_NAMING, which==0 ? NAMING_NUMERIC : NAMING_LETTERED).apply();
            showCreateLocationDialog(count);
        }).setNegativeButton("BATAL",null).show();
    }

    private void showCreateLocationDialog(int count) {
        DocumentFile existing=getTree(KEY_LIBRARY); String[] items=validDir(existing)?new String[]{"Gunakan folder yang sudah dipilih","Buat folder baru"}:new String[]{"Buat folder baru"};
        new AlertDialog.Builder(this).setTitle("Lokasi penyimpanan").setItems(items,(d,which)->{if(validDir(existing)&&which==0)startCreateWorkflow(existing,count);else pickCreateFolder.launch(folderPickerIntent());}).setNegativeButton("BATAL",null).show();
    }

    private void startCreateWorkflow(DocumentFile root,int count) {
        final DocumentFile accountRoot=accountRoot(root); if(!validDir(accountRoot)||count<1){updateStatus("Jumlah/folder penyimpanan tidak valid.");return;}
        final DocumentFile target=getTree(KEY_TARGET); if(!validDir(target)){updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");return;}
        final List<String> categories = getLibraryCategories();
        if(categories.isEmpty()){updateStatus("Tidak ada kategori akun ditemukan di library.");return;}
        new AlertDialog.Builder(this).setTitle("Kategori akun baru").setItems(categories.toArray(new String[0]),(d,which)->{
            String category=categories.get(which); DocumentFile categoryDir=categoryRoot(accountRoot,category);
            if(!validDir(categoryDir)){updateStatus("Gagal membuka kategori: "+category);return;}
            prefs.edit().putString(KEY_CREATE_CATEGORY,category).apply();
            DocumentFile auth=target.findFile(AUTH_FILE); if(auth!=null&&auth.isFile())auth.delete(); prepareCreate(categoryDir,count);
        }).setNegativeButton("BATAL",null).show();
    }

    private void prepareCreate(DocumentFile root,int count) {
        String naming = prefs.getString(KEY_CREATE_NAMING, NAMING_NUMERIC);
        List<String> names=buildAvailableNames(root,count,naming);
        if(names.size()!=count){
            updateStatus("Tidak cukup nama akun yang tersedia.");
            addActivityLog("CREATE ACCOUNT gagal: nama akun tidak cukup.");
            return;
        }
        String resumeFrom = names.isEmpty() ? "-" : names.get(0);
        addActivityLog("CREATE ACCOUNT target: " + prefs.getString(KEY_CREATE_CATEGORY, "AKUN"));
        addActivityLog("CREATE ACCOUNT mode: " + naming + ", lanjut dari: " + resumeFrom + ", jumlah: " + count);
        prefs.edit().putString(KEY_CREATE_NAMES,android.text.TextUtils.join("|",names)).putInt(KEY_CREATE_INDEX,0).apply();
        setMode(MODE_CREATE);
        launchCurrentCreateAccount();
    }

    private void launchCurrentCreateAccount() {
        DocumentFile target=getTree(KEY_TARGET); if(target!=null){DocumentFile old=target.findFile(AUTH_FILE);if(old!=null&&old.isFile())old.delete();}
        List<String> names=split(prefs.getString(KEY_CREATE_NAMES,"")); int index=prefs.getInt(KEY_CREATE_INDEX,0); if(index>=names.size()){finishCreate();return;}
        updateStatus("Buat akun baru ["+(index+1)+"/"+names.size()+"]: "+names.get(index)); if(launchGame())workflowLaunchPending=true;else clearWorkflow();
    }

    public static List<String> buildAvailableNames(DocumentFile root,int count,String naming) {
        if (count < 1) return Collections.emptyList();

        // Resume from the actual highest existing account name in the selected category.
        int startSequence = 0;
        if (root != null) {
            for (DocumentFile child : root.listFiles()) {
                if (!child.isDirectory()) continue;
                int seq = nameToSequence(nameOfStatic(child));
                if (seq > startSequence) startSequence = seq;
            }
        }

        ArrayList<String> result = new ArrayList<>();
        int sequence = startSequence + 1;
        int guard = 0;
        while (result.size() < count && guard++ < 1000000) {
            String name = sequenceToName(sequence, naming);
            if (name == null) break;
            if (root == null || root.findFile(name) == null) result.add(name);
            sequence++;
        }
        return result;
    }

    private static String nameOfStatic(DocumentFile d) {
        return d.getName() == null ? "" : d.getName();
    }

    private static int nameToSequence(String name) {
        if (name == null || name.isEmpty()) return 0;

        boolean numeric = true;
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                numeric = false;
                break;
            }
        }
        if (numeric) {
            try {
                int value = Integer.parseInt(name);
                return value > 0 ? value : 0;
            } catch (Exception ignored) {
                return 0;
            }
        }

        int split = 0;
        while (split < name.length() && Character.isLetter(name.charAt(split))) split++;
        if (split == 0 || split == name.length()) return 0;

        String prefix = name.substring(0, split).toLowerCase(Locale.ROOT);
        String suffix = name.substring(split);
        int slot;
        try {
            slot = Integer.parseInt(suffix);
        } catch (Exception ignored) {
            return 0;
        }
        if (slot < 1 || slot > 10) return 0;

        int prefixIndex = 0;
        for (int i = 0; i < prefix.length(); i++) {
            char ch = prefix.charAt(i);
            if (ch < 'a' || ch > 'z') return 0;
            prefixIndex = prefixIndex * 26 + (ch - 'a' + 1);
        }
        return prefixIndex * 10 + slot;
    }

    private static String sequenceToName(int sequence, String naming) {
        if (sequence < 1) return null;
        if (NAMING_NUMERIC.equals(naming)) return String.valueOf(sequence);

        if (sequence <= 10) return String.valueOf(sequence);
        int prefixNumber = (sequence - 1) / 10;
        int slot = ((sequence - 1) % 10) + 1;
        return toLetters(prefixNumber) + slot;
    }

    private static String toLetters(int n){StringBuilder sb=new StringBuilder();while(n>0){n--;sb.insert(0,(char)('a'+n%26));n/=26;}return sb.toString();}

    private void showCreateExitDialog() {
        List<String> names=split(prefs.getString(KEY_CREATE_NAMES,""));int index=prefs.getInt(KEY_CREATE_INDEX,0);if(index>=names.size()){finishCreate();return;}String current=names.get(index);
        new AlertDialog.Builder(this).setTitle("Akun Baru: "+current).setMessage("Simpan atau hapus AUTH?").setCancelable(false)
                .setNegativeButton("BATAL",(d,w)->{clearWorkflow();updateStatus("Pembuatan akun baru dihentikan.");})
                .setNeutralButton("HAPUS",(d,w)->{DocumentFile target=getTree(KEY_TARGET);DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth!=null&&auth.isFile())auth.delete();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();})
                .setPositiveButton("SIMPAN",(d,w)->{
                    DocumentFile target=getTree(KEY_TARGET);DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth==null||!auth.isFile()){updateStatus("AUTH tidak ditemukan di Target.");clearWorkflow();return;}
                    DocumentFile library=getTree(KEY_LIBRARY);DocumentFile root=categoryRoot(library,prefs.getString(KEY_CREATE_CATEGORY,"AKUN"));if(!validDir(root)){updateStatus("Kategori penyimpanan tidak tersedia.");clearWorkflow();return;}                    DocumentFile folder=root.findFile(current);if(folder==null)folder=root.createDirectory(current);if(folder==null){updateStatus("Gagal membuat folder: "+current);clearWorkflow();return;}
                    if(!copyFile(auth,folder,AUTH_FILE)){folder.delete();updateStatus("Gagal menyimpan AUTH: "+current);clearWorkflow();return;}auth.delete();loadAccounts();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();
                }).show();
    }

    private void finishCreate(){int total=split(prefs.getString(KEY_CREATE_NAMES,"")).size();clearWorkflow();loadAccounts();updateStatus("✓ Pembuatan akun selesai. Total: "+total);}

    private void saveAutoLoginDelay() {
        if (autoLoginDelayEdit == null) return;
        try {
            int seconds = Integer.parseInt(autoLoginDelayEdit.getText().toString().trim());
            if (seconds < 2) {
                seconds = 2;
                autoLoginDelayEdit.setText("2");
            }
            prefs.edit().putInt(KEY_AUTO_LOGIN_DELAY, seconds).apply();
        } catch (Exception ignored) {
            autoLoginDelayEdit.setText(String.valueOf(prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY)));
        }
    }

    private void addActivityLog(String message) {
        if (prefs == null) return;
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = "[" + timestamp + "] " + message;
        String old = prefs.getString(KEY_ACTIVITY_LOG, "");
        ArrayList<String> lines = new ArrayList<>();
        if (old != null && !old.isEmpty()) {
            lines.addAll(Arrays.asList(old.split("\\n", -1)));
        }
        lines.add(entry);
        int from = Math.max(0, lines.size() - MAX_ACTIVITY_LOGS);
        StringBuilder kept = new StringBuilder();
        for (int i = from; i < lines.size(); i++) {
            if (kept.length() > 0) kept.append("\n");
            kept.append(lines.get(i));
        }
        prefs.edit().putString(KEY_ACTIVITY_LOG, kept.toString()).apply();
    }

    private void clearActivityLogs() {
        prefs.edit().remove(KEY_ACTIVITY_LOG).apply();
        updateStatus("✓ Log aktivitas dihapus.");
    }

    private void showActivityLogDialog() {
        String logs = prefs.getString(KEY_ACTIVITY_LOG, "");
        if (logs == null || logs.isEmpty()) logs = "Belum ada aktivitas.";
        TextView logText = new TextView(this);
        logText.setText(logs);
        logText.setTextSize(12);
        logText.setTextColor(color(R.color.textPrimary));
        logText.setTextIsSelectable(true);
        logText.setPadding(dp(12), dp(12), dp(12), dp(12));
        logText.setBackground(makeDrawableWithStroke(Color.parseColor("#16213E"), Color.parseColor("#2A3A5A"), dp(6)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(logText);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, dp(420));
        scrollLp.setMargins(dp(8), dp(4), dp(8), dp(4));

        new AlertDialog.Builder(this)
            .setTitle("LOG AKTIVITAS")
            .setView(scroll)
            .setNegativeButton("HAPUS LOG", (d, w) -> new AlertDialog.Builder(this)
                .setTitle("Hapus Log")
                .setMessage("Hapus semua log aktivitas?")
                .setNegativeButton("BATAL", null)
                .setPositiveButton("HAPUS", (d2, w2) -> clearActivityLogs())
                .show())
            .setPositiveButton("TUTUP", null)
            .show();
    }

    private void showDiscordWebhookDialog(){
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://discord.com/api/webhooks/...");
        input.setText(prefs.getString(KEY_DISCORD_WEBHOOK, ""));
        styleInput(input);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(4));
        box.addView(input, fullLp());

        new AlertDialog.Builder(this)
            .setTitle("DISCORD WEBHOOK")
            .setMessage("Masukkan URL webhook Discord untuk menerima notifikasi eVault.")
            .setView(box)
            .setNegativeButton("BATAL", null)
            .setNeutralButton("HAPUS", (d, w) -> {
                prefs.edit().remove(KEY_DISCORD_WEBHOOK).apply();
                updateStatus("Discord webhook dihapus.");
            })
            .setPositiveButton("SIMPAN & TEST", (d, w) -> {
                String url = input.getText().toString().trim();
                if (!DiscordWebhook.isValidUrl(url)) {
                    updateStatus("URL Discord webhook tidak valid.");
                    return;
                }
                prefs.edit().putString(KEY_DISCORD_WEBHOOK, url).apply();
                updateStatus("Mengirim test webhook...");
                notifyDiscord("🔔 **eVault**\nWebhook berhasil terhubung.");
            })
            .show();
    }

    private void notifyDiscord(String message){
        String url = prefs.getString(KEY_DISCORD_WEBHOOK, "").trim();
        if (!url.isEmpty()) DiscordWebhook.send(url, message);
    }

    private void startAutoLogin(){
        if(accounts.isEmpty()){updateStatus("Library kosong.");return;}
        if(autoLoginRunning)return;
        List<String> categories = getLibraryCategories();
        if(categories.isEmpty()){updateStatus("Tidak ada kategori akun ditemukan di library.");return;}

        new AlertDialog.Builder(this)
            .setTitle("AUTO LOGIN")
            .setItems(new String[]{
                "LOGIN SEMUA AKUN",
                "LOGIN YANG BELUM LOGIN HARI INI"
            }, (d, which) -> {
                String mode = which == 0 ? "all" : "unverified";
                prefs.edit().putString(KEY_AUTO_LOGIN_MODE, mode).apply();
                addActivityLog("AUTO LOGIN mode: " + (which == 0 ? "SEMUA AKUN" : "BELUM LOGIN HARI INI"));
                showCategoryPickerDialog(categories);
            })
            .setNegativeButton("BATAL", null)
            .show();
    }

    private void showCategoryPickerDialog(final List<String> categories){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        String mode = prefs.getString(KEY_AUTO_LOGIN_MODE, "all");
        for (final String cat : categories) {
            int total = 0;
            int verified = 0;
            for (Account a : accounts) {
                if (a.category.equals(cat)) {
                    total++;
                    if (isLoginVerifiedToday(a)) verified++;
                }
            }
            int shown = "unverified".equals(mode) ? total - verified : total;
            String label = cat + "  (" + shown + ("unverified".equals(mode) ? " belum login" : " akun") + ")";
            Button b = secondaryButton(label, v -> startAutoLoginForCategory(cat));
            box.addView(b, spacedLp(dp(6)));
        }
        final AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle("PILIH KATEGORI")
            .setView(box)
            .setNegativeButton("BATAL", null)
            .create();
        dlg.show();
    }

    private List<String> getLibraryCategories(){
        List<String> result = new ArrayList<>();
        DocumentFile lib = getTree(KEY_LIBRARY);
        if(!validDir(lib)) return result;
        for(DocumentFile child : lib.listFiles()){
            if(child.isDirectory()) result.add(nameOf(child));
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void startAutoLoginForCategory(String category){
        addActivityLog("Kategori AUTO LOGIN dipilih: " + category);

        String mode = prefs.getString(KEY_AUTO_LOGIN_MODE, "all");
        if ("single".equals(mode)) {
            showAccountPickerDialog(category);
            return;
        }

        clearWorkflow();
        autoLoginAccounts.clear();
        for(Account a : accounts){
            if(!a.category.equals(category)) continue;
            if("unverified".equals(mode) && isLoginVerifiedToday(a)) continue;
            autoLoginAccounts.add(a);
        }

        Collections.sort(autoLoginAccounts, (a, b) -> {
            int sa = nameToSequence(a.name);
            int sb = nameToSequence(b.name);
            if (sa > 0 && sb > 0 && sa != sb) return Integer.compare(sa, sb);
            if (sa > 0 && sb <= 0) return -1;
            if (sa <= 0 && sb > 0) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        if(autoLoginAccounts.isEmpty()){
            updateStatus("✓ Semua akun " + category + " sudah login hari ini.");
            addActivityLog("AUTO LOGIN tidak punya akun yang perlu diproses: " + category);
            return;
        }

        autoLoginRunning=true;
        autoLoginIndex=0;
        notifyDiscord("🟢 **AUTO LOGIN START**\nKategori: **" + category + "**\nAkun: **" + autoLoginAccounts.size() + "**");
        prefs.edit()
            .putString(KEY_AUTO_LOGIN_CATEGORY, category)
            .putInt(KEY_AUTO_LOGIN_INDEX, 0)
            .apply();
        runNextAutoLogin();
    }

    private void showAccountPickerDialog(final String category){
        ArrayList<Account> categoryAccounts = new ArrayList<>();
        for (Account a : accounts) {
            if (a.category.equals(category)) categoryAccounts.add(a);
        }

        Collections.sort(categoryAccounts, (a, b) -> {
            int sa = nameToSequence(a.name);
            int sb = nameToSequence(b.name);
            if (sa > 0 && sb > 0 && sa != sb) return Integer.compare(sa, sb);
            if (sa > 0 && sb <= 0) return -1;
            if (sa <= 0 && sb > 0) return 1;
            return a.name.compareToIgnoreCase(b.name);
        });

        if (categoryAccounts.isEmpty()) {
            updateStatus("Kategori " + category + " tidak punya akun.");
            return;
        }

        String[] labels = new String[categoryAccounts.size()];
        for (int i = 0; i < categoryAccounts.size(); i++) {
            Account a = categoryAccounts.get(i);
            labels[i] = a.name + (isLoginVerifiedToday(a) ? "  ✓ sudah login hari ini" : "");
        }

        new AlertDialog.Builder(this)
            .setTitle("PILIH AKUN")
            .setItems(labels, (d, which) -> startAutoLoginForSingleAccount(categoryAccounts.get(which)))
            .setNegativeButton("BATAL", null)
            .show();
    }

    private void startAutoLoginForSingleAccount(Account account){
        clearWorkflow();
        autoLoginAccounts.clear();
        autoLoginAccounts.add(account);
        autoLoginRunning = true;
        autoLoginIndex = 0;
        notifyDiscord("🟢 **AUTO LOGIN START**\nKategori: **" + account.category + "**\nAkun: **1**");
        prefs.edit()
            .putString(KEY_AUTO_LOGIN_MODE, "single")
            .putString(KEY_AUTO_LOGIN_CATEGORY, account.category)
            .putInt(KEY_AUTO_LOGIN_INDEX, 0)
            .apply();

        addActivityLog("AUTO LOGIN akun dipilih: " + account.category + " / " + account.name);
        runNextAutoLogin();
    }

    private void runNextAutoLogin(){
        if(!autoLoginRunning) return;
        if(autoLoginIndex>=autoLoginAccounts.size()){
            int total = autoLoginAccounts.size();
            autoLoginRunning=false;
            prefs.edit().remove(KEY_AUTO_LOGIN_PENDING).remove(KEY_AUTO_LOGIN_CATEGORY).remove(KEY_AUTO_LOGIN_INDEX).apply();
            addActivityLog("AUTO LOGIN selesai. Total diproses: " + total);
            updateStatus("✓ AUTO LOGIN selesai. Total: "+total);
            notifyDiscord("✅ **AUTO LOGIN SELESAI**\nDiproses: **"+total+"/"+total+"**");
            return;
        }

        Account account=autoLoginAccounts.get(autoLoginIndex);
        prefs.edit().putString(KEY_AUTO_LOGIN_PENDING, accountKey(account)).putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex).apply();
        addActivityLog("AUTO LOGIN memproses akun: " + account.category + " / " + account.name);
        updateStatus("AUTO LOGIN ["+(autoLoginIndex+1)+"/"+autoLoginAccounts.size()+"]: "+account.category+" / "+account.name);
        notifyDiscord("🔵 **AUTO LOGIN** ["+(autoLoginIndex+1)+"/"+autoLoginAccounts.size()+"]\\nAkun: **"+account.name+"**");

        if(!copyAuth(account)){
            autoLoginRunning=false;
            addActivityLog("AUTO LOGIN gagal saat COPY AUTH: " + account.category + " / " + account.name);
            updateStatus("AUTO LOGIN gagal COPY AUTH: " + account.name);
            notifyDiscord("❌ **AUTO LOGIN GAGAL**\nAkun: **" + account.name + "**\nPenyebab: COPY AUTH gagal.");
            return;
        }

        if (!launchGame()) {
            autoLoginRunning=false;
            addActivityLog("AUTO LOGIN gagal membuka eFootball untuk: " + account.name);
            return;
        }

        // onResume() will show the login confirmation when eFootball returns.
        workflowLaunchPending=true;
        setMode(MODE_AUTO_LOGIN);
    }

    private void showAutoLoginConfirmationDialog(){
        if(!autoLoginRunning || !MODE_AUTO_LOGIN.equals(getMode())) return;
        Account account = findAutoLoginPendingAccount();
        if(account == null){
            addActivityLog("AUTO LOGIN error: akun pending tidak ditemukan.");
            stopAutoLogin();
            return;
        }

        updateStatus("Konfirmasi login: " + account.category + " / " + account.name);
        new AlertDialog.Builder(this)
            .setTitle("KONFIRMASI LOGIN")
            .setMessage("Apakah akun " + account.name + " sudah berhasil login di eFootball?")
            .setCancelable(false)
            .setPositiveButton("LOGIN BERHASIL", (d,w) -> confirmAutoLoginSuccess(account))
            .setNegativeButton("BELUM LOGIN", (d,w) -> retryAutoLogin(account))
            .setNeutralButton("STOP", (d,w) -> stopAutoLogin())
            .show();
    }

    private void confirmAutoLoginSuccess(Account account){
        markLoginVerifiedToday(account);
        addActivityLog("LOGIN VERIFIED hari ini: " + account.category + " / " + account.name);
        updateStatus("✓ " + account.name + " tercatat sudah login hari ini.");

        new AlertDialog.Builder(this)
            .setTitle("LANJUT AUTO LOGIN?")
            .setMessage("Akun " + account.name + " sudah berhasil login dan tercatat hari ini.\n\nLanjut ke akun berikutnya?")
            .setCancelable(false)
            .setNegativeButton("STOP", (d, w) -> stopAutoLogin())
            .setPositiveButton("LANJUT", (d, w) -> advanceAutoLoginAfterDelay(account, false))
            .show();
    }

    private void retryAutoLogin(Account account){
        addActivityLog("LOGIN BELUM BERHASIL: " + account.category + " / " + account.name);
        updateStatus("Login belum terverifikasi: " + account.name + ". Menunggu delay sebelum coba lagi.");
        advanceAutoLoginAfterDelay(account, true);
    }

    private void advanceAutoLoginAfterDelay(Account account, boolean retrySame){
        if(!autoLoginRunning) return;
        autoLoginHandler.removeCallbacksAndMessages(null);
        autoLoginHandler.postDelayed(() -> {
            if(!autoLoginRunning) return;
            if(retrySame){
                runNextAutoLogin();
            } else {
                autoLoginIndex++;
                prefs.edit().putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex).apply();
                runNextAutoLogin();
            }
        }, getAutoLoginDelayMs());
    }

    private Account findAutoLoginPendingAccount(){
        String key = prefs.getString(KEY_AUTO_LOGIN_PENDING, null);
        if(key == null) return null;
        for(Account a : accounts) if(accountKey(a).equals(key)) return a;
        return null;
    }

    private String accountKey(Account account){
        return account.category + "\u001F" + account.name;
    }

    private String utcDate(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private boolean isLoginVerifiedToday(Account account){
        String today = utcDate();
        String key = accountKey(account) + "\u001F" + today;
        java.util.Set<String> verified = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        return verified != null && verified.contains(key);
    }

    private void markLoginVerifiedToday(Account account){
        String key = accountKey(account) + "\u001F" + utcDate();
        java.util.Set<String> old = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        java.util.HashSet<String> updated = new java.util.HashSet<>();
        if(old != null) updated.addAll(old);
        // Keep today's records plus recent entries; stale records are harmless but pruned here.
        String today = utcDate();
        java.util.Iterator<String> it = updated.iterator();
        while(it.hasNext()){
            String entry = it.next();
            int split = entry.lastIndexOf("\u001F");
            if(split < 0 || !entry.substring(split + 1).equals(today)) it.remove();
        }
        updated.add(key);
        prefs.edit().putStringSet(KEY_LOGIN_VERIFIED, updated).apply();
    }

    private long getAutoLoginDelayMs(){
        try{
            int seconds=Integer.parseInt(autoLoginDelayEdit.getText().toString().trim());
            seconds=Math.max(2,seconds);
            prefs.edit().putInt(KEY_AUTO_LOGIN_DELAY, seconds).apply();
            return seconds*1000L;
        }catch(Exception e){
            return prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY)*1000L;
        }
    }

    private void stopAutoLogin(){
        boolean wasRunning=autoLoginRunning;
        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        prefs.edit().remove(KEY_AUTO_LOGIN_PENDING).remove(KEY_AUTO_LOGIN_CATEGORY).remove(KEY_AUTO_LOGIN_INDEX).remove(KEY_AUTO_LOGIN_MODE).apply();
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
        if(wasRunning){
            addActivityLog("AUTO LOGIN dihentikan.");
            updateStatus("AUTO LOGIN dihentikan.");
            notifyDiscord("🛑 **AUTO LOGIN DIHENTIKAN**");
        }
    }

    private boolean copyAuth(Account account){
        DocumentFile target=getTree(KEY_TARGET);
        if(!validDir(target)){
            addActivityLog("COPY AUTH gagal: folder target belum dipilih.");
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return false;
        }
        DocumentFile old=target.findFile(AUTH_FILE);if(old!=null)old.delete();
        boolean ok=copyFile(account.file,target,AUTH_FILE);
        addActivityLog("COPY AUTH " + (ok ? "berhasil: " : "gagal: ") + account.category + " / " + account.name);
        return ok;
    }
    private boolean copyFile(DocumentFile source,DocumentFile parent,String name){DocumentFile dest=parent.findFile(name);if(dest!=null)dest.delete();dest=parent.createFile("application/octet-stream",name);if(dest==null)return false;try(InputStream in=getContentResolver().openInputStream(source.getUri());OutputStream out=getContentResolver().openOutputStream(dest.getUri(),"w")){if(in==null||out==null)throw new Exception("stream null");byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);out.flush();return true;}catch(Exception e){dest.delete();return false;}}
    private boolean launchGame(){
        Intent i=getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if(i==null){
            addActivityLog("Gagal membuka eFootball: package " + PACKAGE + " tidak ditemukan.");
            updateStatus("Package "+PACKAGE+" tidak ditemukan.");
            return false;
        }
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            addActivityLog("Berhasil membuka eFootball.");
            notifyDiscord("🎮 **EFOOTBALL DIBUKA**\nPackage: `"+PACKAGE+"`");
            return true;
        } catch(Exception e) {
            addActivityLog("Gagal membuka eFootball: " + e.getClass().getSimpleName());
            updateStatus("Gagal membuka eFootball.");
            return false;
        }
    }
    private boolean validDir(DocumentFile d){return d!=null&&d.isDirectory();}
    private String getMode(){return prefs.getString(KEY_MODE,MODE_NONE);}
    private void setMode(String mode){prefs.edit().putString(KEY_MODE,mode).apply();}
    private void clearWorkflow(){workflowLaunchPending=false;prefs.edit().putString(KEY_MODE,MODE_NONE).remove(KEY_CREATE_NAMES).remove(KEY_CREATE_INDEX).remove(KEY_CREATE_COUNT).remove(KEY_CREATE_CATEGORY).remove(KEY_CREATE_NAMING).apply();}
    private List<String> split(String value){return value==null||value.isEmpty()?Collections.emptyList():Arrays.asList(value.split("\\|",-1));}
    private void updateStatus(String s){if(status!=null)status.setText(s);}
    @Override protected void onDestroy(){saveAutoLoginDelay(); if(!isChangingConfigurations()) stopAutoLogin(); super.onDestroy();}

    static class Account{final String category;final String name;final DocumentFile file;Account(String category,String name,DocumentFile file){this.category=category;this.name=name;this.file=file;}}
}
