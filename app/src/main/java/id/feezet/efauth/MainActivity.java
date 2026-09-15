package id.feezet.efauth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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
    private static final String KEY_CREATE_CATEGORY = "create_category";
    private static final String MODE_NONE = "none";
    private static final String MODE_DELETE_DATA = "delete_data";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";
    private static final String[] DEFAULT_CATEGORIES = {"AKUN", "Asset", "EPIC NEW"};

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
                    updateStatus("✓ AUTH Library tersimpan.");
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
        String mode = getMode();
        if (MODE_DELETE_DATA.equals(mode)) showDeleteDataExitDialog();
        else if (MODE_CREATE.equals(mode)) showCreateExitDialog();
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
        subtitle.setTextColor(color(R.color.teal));
        subtitle.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams subLp = fullLp();
        subLp.bottomMargin = dp(20);
        root.addView(subtitle, subLp);

        // ---- Card: Setup ----
        LinearLayout setupCard = card();
        setupCard.addView(sectionLabel("SETUP"));
        Button library = secondaryButton("PILIH AUTH LIBRARY", v -> chooseLibrary());
        setupCard.addView(library, spacedLp(dp(4)));
        Button target = secondaryButton("PILIH FOLDER AUTH EFOOTBALL", v -> chooseTree(pickTarget));
        setupCard.addView(target, spacedLp(dp(10)));
        root.addView(setupCard, cardLp());

        // ---- Card: Akun ----
        LinearLayout accountCard = card();
        accountCard.addView(sectionLabel("AKUN"));
        accountsText = new TextView(this);
        accountsText.setText("Akun ditemukan: 0");
        accountsText.setTextColor(color(R.color.textSecondary));
        accountsText.setTextSize(13);
        accountCard.addView(accountsText, spacedLp(dp(8)));

        accountSpinner = new Spinner(this);
        accountSpinner.setBackground(getResources().getDrawable(R.drawable.input_bg));
        accountSpinner.setPadding(dp(12), dp(10), dp(12), dp(10));
        accountCard.addView(accountSpinner, spacedLp(dp(12)));

        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        Button switchBtn = primaryButton("SWITCH", v -> switchSelected());
        row1.addView(switchBtn, weightLp(dp(4), 0));
        Button deleteDataBtn = dangerButton("HAPUS DATA", v -> showDeleteDataConfirmDialog());
        row1.addView(deleteDataBtn, weightLp(dp(4), 1));
        accountCard.addView(row1, spacedLp(0));
        root.addView(accountCard, cardLp());

        // ---- Card: Auto Login ----
        LinearLayout autoCard = card();
        autoCard.addView(sectionLabel("AUTO LOGIN"));
        autoLoginDelayEdit = new EditText(this);
        autoLoginDelayEdit.setInputType(2);
        autoLoginDelayEdit.setSingleLine(true);
        autoLoginDelayEdit.setText("10");
        autoLoginDelayEdit.setHint("Delay (detik)");
        styleInput(autoLoginDelayEdit);
        autoCard.addView(autoLoginDelayEdit, spacedLp(dp(10)));

        LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        Button autoBtn = primaryButton("MULAI", v -> startAutoLogin());
        row2.addView(autoBtn, weightLp(dp(4), 0));
        Button stopBtn = secondaryButton("STOP", v -> stopAutoLogin());
        row2.addView(stopBtn, weightLp(dp(4), 1));
        autoCard.addView(row2, spacedLp(0));
        root.addView(autoCard, cardLp());

        // ---- Card: Kelola ----
        LinearLayout manageCard = card();
        manageCard.addView(sectionLabel("KELOLA"));
        LinearLayout row3 = new LinearLayout(this); row3.setOrientation(LinearLayout.HORIZONTAL);
        Button deleteAuthBtn = dangerButton("HAPUS AUTH INI", v -> deleteCurrentAuth());
        row3.addView(deleteAuthBtn, weightLp(dp(4), 0));
        Button createBtn = secondaryButton("BUAT AKUN BARU", v -> showCreateDialog());
        row3.addView(createBtn, weightLp(dp(4), 1));
        manageCard.addView(row3, spacedLp(0));
        root.addView(manageCard, cardLp());

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

    private int color(int resId) { return getResources().getColor(resId); }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(getResources().getDrawable(R.drawable.card_bg));
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
        b.setBackground(getResources().getDrawable(R.drawable.btn_primary));
        b.setTextColor(color(R.color.white));
        return b;
    }

    private Button secondaryButton(String text, View.OnClickListener l) {
        Button b = baseButton(text, l);
        b.setBackground(getResources().getDrawable(R.drawable.btn_secondary));
        b.setTextColor(color(R.color.textPrimary));
        return b;
    }

    private Button dangerButton(String text, View.OnClickListener l) {
        Button b = baseButton(text, l);
        b.setBackground(getResources().getDrawable(R.drawable.btn_danger));
        b.setTextColor(color(R.color.dangerText));
        return b;
    }

    private void styleInput(EditText e) {
        e.setBackground(getResources().getDrawable(R.drawable.input_bg));
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
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); } catch (Exception ignored) {}
        prefs.edit().putString(key, uri.toString()).apply();
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
        ArrayList<String> names = new ArrayList<>();
        for (Account a : accounts) names.add(a.category + " / " + a.name);
        accountSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        accountsText.setText("Akun ditemukan: " + accounts.size());
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
        if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        int pos = accountSpinner.getSelectedItemPosition();
        if (pos >= 0 && pos < accounts.size() && copyAuth(accounts.get(pos))) launchGame();
    }

    private void deleteCurrentAuth() {
        stopAutoLogin(); clearWorkflow(); DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; }
        new AlertDialog.Builder(this).setTitle("Hapus AUTH Saat Ini").setMessage("Hapus file AUTH yang sedang terpasang di target?").setNegativeButton("BATAL", null).setPositiveButton("HAPUS", (d,w) -> updateStatus(auth.delete() ? "✓ AUTH saat ini dihapus." : "Gagal menghapus AUTH. Periksa izin folder.")).show();
    }

    private void showDeleteDataConfirmDialog() {
        stopAutoLogin(); clearWorkflow(); new AlertDialog.Builder(this).setTitle("Hapus Data").setMessage("Hapus AUTH saat ini?").setNegativeButton("TIDAK", null).setPositiveButton("YA", (d,w) -> executeDeleteDataWorkflow()).show();
    }

    private void executeDeleteDataWorkflow() {
        DocumentFile target = getTree(KEY_TARGET); if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE); if (auth != null && auth.isFile() && !auth.delete()) { updateStatus("Gagal menghapus AUTH saat ini."); return; }
        setMode(MODE_DELETE_DATA); updateStatus("AUTH saat ini dihapus. Memulai AUTO LOGIN..."); startDeleteDataAutoLogin();
    }

    private void startDeleteDataAutoLogin() { if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); clearWorkflow(); return; } autoLoginRunning = true; autoLoginIndex = 0; runNextDeleteDataAutoLogin(); }

    private void runNextDeleteDataAutoLogin() {
        if (!autoLoginRunning || autoLoginIndex >= accounts.size()) { autoLoginRunning = false; updateStatus("✓ AUTO LOGIN selesai."); return; }
        Account account = accounts.get(autoLoginIndex); updateStatus("AUTO LOGIN [" + (autoLoginIndex + 1) + "/" + accounts.size() + "]: " + account.category + " / " + account.name);
        if (!copyAuth(account) || !launchGame()) { autoLoginRunning = false; clearWorkflow(); return; }
        workflowLaunchPending = true; autoLoginIndex++;
    }

    private void showDeleteDataExitDialog() {
        stopAutoLogin();
        new AlertDialog.Builder(this).setTitle("Keluar eFootball").setMessage("Simpan atau hapus AUTH?").setCancelable(false)
                .setNegativeButton("BATAL", (d,w) -> { clearWorkflow(); updateStatus("Workflow HAPUS DATA dibatalkan."); })
                .setNeutralButton("HAPUS", (d,w) -> { DocumentFile target=getTree(KEY_TARGET); DocumentFile auth=target==null?null:target.findFile(AUTH_FILE); if(auth!=null&&auth.isFile())auth.delete(); updateStatus("AUTH dihapus. Melanjutkan AUTO LOGIN..."); if(autoLoginIndex>=accounts.size())autoLoginIndex=0; autoLoginRunning=true; runNextDeleteDataAutoLogin(); })
                .setPositiveButton("SIMPAN", (d,w) -> showDeleteDataSaveDialog()).show();
    }

    private void showDeleteDataSaveDialog() {
        final EditText input=new EditText(this); input.setSingleLine(true); input.setHint("Nama akun / folder");
        new AlertDialog.Builder(this).setTitle("Simpan AUTH").setMessage("Nama akun:").setView(input).setCancelable(false)
                .setNegativeButton("BATAL", (d,w)->showDeleteDataExitDialog())
                .setPositiveButton("SIMPAN", (d,w)->{
                    String name=input.getText().toString().trim();
                    if(name.isEmpty()){updateStatus("Nama tidak boleh kosong.");showDeleteDataSaveDialog();return;}
                    DocumentFile lib=getTree(KEY_LIBRARY), target=getTree(KEY_TARGET);
                    if(!validDir(lib)){updateStatus("AUTH Library belum dipilih.");showDeleteDataSaveDialog();return;}
                    DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);
                    if(auth==null||!auth.isFile()){updateStatus("AUTH tidak ditemukan di Target.");clearWorkflow();return;}
                    showCategorySaveDialog(lib,name,auth);
                }).show();
    }

    private void showCategorySaveDialog(DocumentFile library,String accountName,DocumentFile auth) {
        new AlertDialog.Builder(this).setTitle("Simpan ke kategori").setItems(DEFAULT_CATEGORIES,(d,which)->{
            String categoryName=DEFAULT_CATEGORIES[which]; DocumentFile category=categoryRoot(library,categoryName);
            if(!validDir(category)){updateStatus("Gagal membuka kategori: "+categoryName);return;}
            if(category.findFile(accountName)!=null){updateStatus("Nama folder sudah digunakan: "+accountName);return;}
            DocumentFile folder=category.createDirectory(accountName);
            if(folder==null){updateStatus("Gagal membuat folder akun.");return;}
            if(copyFile(auth,folder,AUTH_FILE)){clearWorkflow();loadAccounts();updateStatus("✓ AUTH disimpan: "+categoryName+" / "+accountName);}else{folder.delete();updateStatus("Gagal menyimpan AUTH.");}
        }).setNegativeButton("BATAL",null).show();
    }

    private void showCreateDialog() {
        stopAutoLogin(); clearWorkflow(); final EditText input=new EditText(this); input.setInputType(2); input.setSingleLine(true); input.setHint("Jumlah akun (1-10)");
        new AlertDialog.Builder(this).setTitle("BUAT AKUN BARU").setMessage("Mau buat berapa akun?").setView(input).setNegativeButton("BATAL",null).setPositiveButton("LANJUT",(d,w)->{
            int count; try{count=Integer.parseInt(input.getText().toString().trim());}catch(Exception e){count=0;}
            if(count<1||count>10){updateStatus("Jumlah akun harus 1 sampai 10.");return;} prefs.edit().putInt(KEY_CREATE_COUNT,count).apply(); showCreateLocationDialog(count);
        }).show();
    }

    private void showCreateLocationDialog(int count) {
        DocumentFile existing=getTree(KEY_LIBRARY); String[] items=validDir(existing)?new String[]{"Gunakan folder yang sudah dipilih","Buat folder baru"}:new String[]{"Buat folder baru"};
        new AlertDialog.Builder(this).setTitle("Lokasi penyimpanan").setItems(items,(d,which)->{if(validDir(existing)&&which==0)startCreateWorkflow(existing,count);else pickCreateFolder.launch(folderPickerIntent());}).setNegativeButton("BATAL",null).show();
    }

    private void startCreateWorkflow(DocumentFile root,int count) {
        root=accountRoot(root); if(!validDir(root)||count<1||count>10){updateStatus("Jumlah/folder penyimpanan tidak valid.");return;}
        DocumentFile target=getTree(KEY_TARGET); if(!validDir(target)){updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");return;}
        new AlertDialog.Builder(this).setTitle("Kategori akun baru").setItems(DEFAULT_CATEGORIES,(d,which)->{
            String category=DEFAULT_CATEGORIES[which]; DocumentFile categoryDir=categoryRoot(root,category);
            if(!validDir(categoryDir)){updateStatus("Gagal membuka kategori: "+category);return;}
            prefs.edit().putString(KEY_CREATE_CATEGORY,category).apply();
            DocumentFile auth=target.findFile(AUTH_FILE); if(auth!=null&&auth.isFile())auth.delete(); prepareCreate(categoryDir,count);
        }).setNegativeButton("BATAL",null).show();
    }

    private void prepareCreate(DocumentFile root,int count) {
        List<String> names=buildAvailableNames(root,count); if(names.size()!=count){updateStatus("Tidak cukup nama akun yang tersedia.");return;}
        prefs.edit().putString(KEY_CREATE_NAMES,android.text.TextUtils.join("|",names)).putInt(KEY_CREATE_INDEX,0).apply(); setMode(MODE_CREATE); launchCurrentCreateAccount();
    }

    private void launchCurrentCreateAccount() {
        DocumentFile target=getTree(KEY_TARGET); if(target!=null){DocumentFile old=target.findFile(AUTH_FILE);if(old!=null&&old.isFile())old.delete();}
        List<String> names=split(prefs.getString(KEY_CREATE_NAMES,"")); int index=prefs.getInt(KEY_CREATE_INDEX,0); if(index>=names.size()){finishCreate();return;}
        updateStatus("Buat akun baru ["+(index+1)+"/"+names.size()+"]: "+names.get(index)); if(launchGame())workflowLaunchPending=true;else clearWorkflow();
    }

    public static List<String> buildAvailableNames(DocumentFile root,int count) {
        for(int round=0;round<27;round++){String prefix=round==0?"":String.valueOf((char)('a'+round-1));for(int batch=1;batch<=1000;batch++){boolean allFree=true;ArrayList<String> candidates=new ArrayList<>();for(int slot=1;slot<=count;slot++){String name=prefix+batch+"-"+slot;if(root!=null&&root.findFile(name)!=null){allFree=false;break;}candidates.add(name);}if(allFree)return candidates;}}return Collections.emptyList();
    }

    private void showCreateExitDialog() {
        List<String> names=split(prefs.getString(KEY_CREATE_NAMES,""));int index=prefs.getInt(KEY_CREATE_INDEX,0);if(index>=names.size()){finishCreate();return;}String current=names.get(index);
        new AlertDialog.Builder(this).setTitle("Akun Baru: "+current).setMessage("Simpan atau hapus AUTH?").setCancelable(false)
                .setNegativeButton("BATAL",(d,w)->{clearWorkflow();updateStatus("Pembuatan akun baru dihentikan.");})
                .setNeutralButton("HAPUS",(d,w)->{DocumentFile target=getTree(KEY_TARGET);DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth!=null&&auth.isFile())auth.delete();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();})
                .setPositiveButton("SIMPAN",(d,w)->{
                    DocumentFile target=getTree(KEY_TARGET);DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth==null||!auth.isFile()){updateStatus("AUTH tidak ditemukan di Target.");clearWorkflow();return;}
                    DocumentFile library=getTree(KEY_LIBRARY);DocumentFile root=categoryRoot(library,prefs.getString(KEY_CREATE_CATEGORY,"AKUN"));if(!validDir(root)){updateStatus("Kategori penyimpanan tidak tersedia.");clearWorkflow();return;}
                    DocumentFile folder=root.findFile(current);if(folder==null)folder=root.createDirectory(current);if(folder==null){updateStatus("Gagal membuat folder: "+current);clearWorkflow();return;}
                    if(!copyFile(auth,folder,AUTH_FILE)){folder.delete();updateStatus("Gagal menyimpan AUTH: "+current);clearWorkflow();return;}auth.delete();loadAccounts();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();
                }).show();
    }

    private void finishCreate(){int total=split(prefs.getString(KEY_CREATE_NAMES,"")).size();clearWorkflow();loadAccounts();updateStatus("✓ Pembuatan akun selesai. Total: "+total);}

    private void startAutoLogin(){if(accounts.isEmpty()){updateStatus("Library kosong.");return;}if(autoLoginRunning)return;clearWorkflow();autoLoginRunning=true;autoLoginIndex=0;runNextAutoLogin();}
    private void runNextAutoLogin(){if(!autoLoginRunning||autoLoginIndex>=accounts.size()){autoLoginRunning=false;updateStatus("✓ AUTO LOGIN selesai. Total: "+accounts.size());return;}Account account=accounts.get(autoLoginIndex);updateStatus("AUTO LOGIN ["+(autoLoginIndex+1)+"/"+accounts.size()+"]: "+account.category+" / "+account.name);if(!copyAuth(account)){autoLoginRunning=false;return;}launchGame();autoLoginIndex++;autoLoginHandler.postDelayed(this::runNextAutoLogin,getAutoLoginDelayMs());}
    private long getAutoLoginDelayMs(){try{int seconds=Integer.parseInt(autoLoginDelayEdit.getText().toString().trim());return Math.max(2,seconds)*1000L;}catch(Exception e){return 10000L;}}
    private void stopAutoLogin(){autoLoginRunning=false;autoLoginHandler.removeCallbacksAndMessages(null);updateStatus("AUTO LOGIN dihentikan.");}

    private boolean copyAuth(Account account){DocumentFile target=getTree(KEY_TARGET);if(!validDir(target)){updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");return false;}DocumentFile old=target.findFile(AUTH_FILE);if(old!=null)old.delete();return copyFile(account.file,target,AUTH_FILE);}
    private boolean copyFile(DocumentFile source,DocumentFile parent,String name){DocumentFile dest=parent.findFile(name);if(dest!=null)dest.delete();dest=parent.createFile("application/octet-stream",name);if(dest==null)return false;try(InputStream in=getContentResolver().openInputStream(source.getUri());OutputStream out=getContentResolver().openOutputStream(dest.getUri(),"w")){if(in==null||out==null)throw new Exception("stream null");byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);out.flush();return true;}catch(Exception e){dest.delete();return false;}}
    private boolean launchGame(){Intent i=getPackageManager().getLaunchIntentForPackage(PACKAGE);if(i==null){updateStatus("Package "+PACKAGE+" tidak ditemukan.");return false;}i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;}
    private boolean validDir(DocumentFile d){return d!=null&&d.isDirectory();}
    private String getMode(){return prefs.getString(KEY_MODE,MODE_NONE);}
    private void setMode(String mode){prefs.edit().putString(KEY_MODE,mode).apply();}
    private void clearWorkflow(){workflowLaunchPending=false;prefs.edit().putString(KEY_MODE,MODE_NONE).remove(KEY_CREATE_NAMES).remove(KEY_CREATE_INDEX).remove(KEY_CREATE_COUNT).remove(KEY_CREATE_CATEGORY).apply();}
    private List<String> split(String value){return value==null||value.isEmpty()?Collections.emptyList():Arrays.asList(value.split("\\|",-1));}
    private void updateStatus(String s){if(status!=null)status.setText(s);}
    @Override protected void onDestroy(){stopAutoLogin();super.onDestroy();}

    static class Account{final String category;final String name;final DocumentFile file;Account(String category,String name,DocumentFile file){this.category=category;this.name=name;this.file=file;}}
}
