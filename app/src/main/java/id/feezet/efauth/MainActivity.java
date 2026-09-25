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

    @Override protected void onResume() {
        super.onResume();
        if (!workflowLaunchPending) return;
        workflowLaunchPending = false;
        if (MODE_CREATE.equals(getMode())) showCreateExitDialog();
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
        Collections.sort(accounts, Comparator.comparing(a -> (a.category + "/" + a.name).toLowerCase()));
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

    private void startAutoLogin(){
        addActivityLog("AUTO LOGIN dimulai.");
        List<String> categories = getLibraryCategories();
        if(accounts.isEmpty()){updateStatus("Library kosong.");return;}
        if(autoLoginRunning)return;
        if(categories.isEmpty()){updateStatus("Tidak ada kategori akun ditemukan di library.");return;}
        showCategoryPickerDialog(categories);
    }

    private void showCategoryPickerDialog(final List<String> categories){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));
        for (final String cat : categories) {
            int count = 0;
            for (Account a : accounts) if (a.category.equals(cat)) count++;
            Button b = secondaryButton(cat + "  (" + count + ")", v -> startAutoLoginForCategory(cat));
            box.addView(b, spacedLp(dp(6)));
        }
        final AlertDialog dlg = new AlertDialog.Builder(this)
            .setTitle("AUTO LOGIN")
            .setMessage("Pilih kategori akun untuk auto login:")
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
        clearWorkflow();
        autoLoginAccounts.clear();
        for(Account a:accounts){ if(a.category.equals(category)) autoLoginAccounts.add(a); }
        if(autoLoginAccounts.isEmpty()){updateStatus("Tidak ada akun di kategori: "+category);return;}
        autoLoginRunning=true;autoLoginIndex=0;runNextAutoLogin();
    }

    private void runNextAutoLogin(){
        if(!autoLoginRunning||autoLoginIndex>=autoLoginAccounts.size()){
            int total = autoLoginAccounts.size();
            autoLoginRunning=false;
            addActivityLog("AUTO LOGIN selesai. Total: " + total);
            updateStatus("✓ AUTO LOGIN selesai. Total: "+total);
            return;
        }
        Account account=autoLoginAccounts.get(autoLoginIndex);
        addActivityLog("AUTO LOGIN memproses akun: " + account.category + " / " + account.name);
        updateStatus("AUTO LOGIN ["+(autoLoginIndex+1)+"/"+autoLoginAccounts.size()+"]: "+account.category+" / "+account.name);
        if(!copyAuth(account)){autoLoginRunning=false;addActivityLog("AUTO LOGIN gagal saat COPY AUTH: " + account.category + " / " + account.name);return;}
        boolean launched = launchGame();
        if (!launched) {
            addActivityLog("AUTO LOGIN gagal membuka eFootball untuk: " + account.name);
            autoLoginRunning=false;
            return;
        }
        autoLoginIndex++;
        autoLoginHandler.postDelayed(this::runNextAutoLogin,getAutoLoginDelayMs());
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
        if(wasRunning){
            addActivityLog("AUTO LOGIN dihentikan.");
            updateStatus("AUTO LOGIN dihentikan.");
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
    @Override protected void onDestroy(){saveAutoLoginDelay();stopAutoLogin();super.onDestroy();}

    static class Account{final String category;final String name;final DocumentFile file;Account(String category,String name,DocumentFile file){this.category=category;this.name=name;this.file=file;}}
}
