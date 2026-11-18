package id.feezet.efauth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import id.feezet.efauth.ui.HomeLayout;
import id.feezet.efauth.ui.NeoButton;
import id.feezet.efauth.ui.NeoSurface;
import id.feezet.efauth.ui.NeoIcons;
import id.feezet.efauth.ui.NeoDialogBuilder;

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
    private static final String KEY_CREATE_BACKUP = "create_auth_backup";
    private static final String KEY_AUTO_LOGIN_DELAY = "KEY_AUTO_LOGIN_DELAY";
    private static final String KEY_ACTIVITY_LOG = "activity_log";
    private static final String KEY_DISCORD_WEBHOOK = "discord_webhook";
    private static final String KEY_LOGIN_VERIFIED = "login_verified";
    private static final String KEY_AUTO_LOGIN_MODE = "auto_login_mode";
    private static final String KEY_AUTO_LOGIN_CATEGORY = "auto_login_category";
    private static final String KEY_AUTO_LOGIN_INDEX = "auto_login_index";
    private static final String KEY_AUTO_LOGIN_PENDING = "auto_login_pending";
    private static final String MODE_AUTO_LOGIN = "auto_login";
    // ---- Auto Login progress session persistence ----
    private static final String KEY_AL_SESSION_ID = "al_session_id";
    private static final String KEY_AL_TOTAL = "al_total";
    private static final String KEY_AL_SUCCESS = "al_success";
    private static final String KEY_AL_FAILED = "al_failed";
    private static final String KEY_AL_STATUS = "al_status";
    private static final String KEY_AL_CURRENT_ACCOUNT = "al_current_account";
    private static final String KEY_AL_NEXT_ACCOUNT = "al_next_account";
    private static final String KEY_AL_STARTED_AT = "al_started_at";
    private static final String KEY_AL_UPDATED_AT = "al_updated_at";
    private static final String KEY_AL_WEBHOOK_MSG_ID = "al_webhook_msg_id";
    private static final String KEY_AL_LAST_ERROR = "al_last_error";
    private static final String KEY_AL_FINISHED_AT = "al_finished_at";
    private static final String KEY_AL_SESSION_VERSION = "al_session_version";
    private static final String KEY_AL_WAITING_CONFIRMATION = "al_waiting_confirmation";
    private static final String KEY_AL_EFOOTBALL_STATE = "al_efootball_state";
    private static final String KEY_AL_ACCOUNTS = "al_accounts";
    private static final String AL_STATUS_RUNNING = "RUNNING";
    private static final String AL_STATUS_STOPPED = "STOPPED";
    private static final String AL_STATUS_PAUSED = "PAUSED";
    private static final String AL_STATUS_COMPLETED = "COMPLETED";
    private static final String AL_STATUS_FAILED = "FAILED";
    private static final String AL_STATUS_WAITING = "WAITING FOR LOGIN";
    private static final String AL_GAME_PREPARING = "MENYIAPKAN AUTH";
    private static final String AL_GAME_OPENED = "DIBUKA · menunggu konfirmasi";
    private static final String AL_GAME_WAITING = "MENUNGGU KONFIRMASI LOGIN";
    private static final String AL_GAME_VERIFIED = "LOGIN DIKONFIRMASI";
    private static final String AL_GAME_RETRY = "LOGIN BELUM BERHASIL · retry";
    private static final String AL_GAME_PAUSED = "DIJEDA";
    private static final String AL_GAME_STOPPED = "BERHENTI";
    private static final String AL_GAME_COMPLETED = "SELESAI";
    private static final int DEFAULT_AUTO_LOGIN_DELAY = 5;
    private static final int AUTO_MAX_DELAY_SECONDS = 120;
    private static final int MAX_ACTIVITY_LOGS = 300;
    private static final String NAMING_NUMERIC = "numeric";
    private static final String NAMING_LETTERED = "lettered";
    private static final String MODE_NONE = "none";
    private static final String MODE_CREATE = "create";
    /** The original eFootball package: excluded from clone auto-detect (never the default). */
    private static final String ORIGINAL_GAME_PACKAGE = "epl.idsphone";
    private static final String OFFICIAL_GAME_PACKAGE = "jp.konami.pesam";
    private static final String KEY_GAME_PACKAGE = "selected_game_package";
    // ---- AUTH target: SAF (manual folder) or a plain file path (set by an earlier clone build) ----
    private static final String KEY_TARGET_MODE = "target_mode";
    private static final String MODE_SAF = "saf";
    private static final String MODE_FILE = "file";
    private static final String KEY_TARGET_FILE = "target_file";

    private SharedPreferences prefs;
    private TextView status;
    private TextView gamePackageStatus;
    private Account selectedAccount;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private final ArrayList<Account> autoLoginAccounts = new ArrayList<>();
    private boolean workflowLaunchPending = false;
    private final Handler autoLoginHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService autoLoginIoExecutor = Executors.newSingleThreadExecutor();
    /** Serializes user-triggered storage work (Switch/Save/Create) off the UI thread. */
    private final ExecutorService uiIoExecutor = Executors.newSingleThreadExecutor();
    private boolean autoLoginRunning = false;
    private int autoLoginIndex = 0;
    private int autoLoginSuccessCount = 0;
    private int autoLoginFailedCount = 0;
    private volatile int autoLoginGeneration = 0;
    private String sessionLoginDay = null;
    private EditText autoLoginDelayEdit;
    private View settingsScreen;
    private View setupScreen;
    private View detailScreen;
    private TextView selectedAccountName;
    private TextView selectedAccountDetail;

    // ---- Auto Login progress screen ----
    private View mainScreen;
    private View autoLoginScreen;
    private FrameLayout screenHost;
    private TextView alProgressCount;
    private TextView alProgressBar;
    private TextView alPercent;
    private android.widget.ProgressBar alLoading;
    private TextView alCurrent;
    private TextView alSuccess;
    private TextView alFailed;
    private TextView alRemaining;
    private TextView alStatus;
    private TextView alStoppedAt;
    private TextView alResumeFrom;
    private TextView alDuration;
    private TextView alErrorReason;
    private Button alPauseButton;
    private Button alStopButton;
    private Button alContinueButton;
    private Button alExitButton;

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
                    prefs.edit().putString(KEY_TARGET_MODE, MODE_SAF).apply();
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
        if (AL_STATUS_PAUSED.equals(prefs.getString(KEY_AL_STATUS, ""))
                && prefs.getInt(KEY_AL_SESSION_VERSION, 0) < 2) {
            prefs.edit().putString(KEY_AL_STATUS, AL_STATUS_STOPPED).apply();
        }
        buildUi();
        loadAccounts();
        if (b != null) restoreSelectedAccount(b.getString(STATE_SELECTED_ACCOUNT, null));
        recoverAbandonedCreateBackup();
    }

    /** If the app died mid create-account, the target may have no AUTH but a .bak exists.
     *  Restore it so the user is never left without an installed AUTH. */
    private void recoverAbandonedCreateBackup(){
        if (!prefs.contains(KEY_CREATE_BACKUP)) return;
        DocumentFile target = getTarget();
        if (!validDir(target)) return;
        DocumentFile auth = target.findFile(AUTH_FILE);
        boolean authMissing = auth == null || !auth.isFile();
        if (authMissing) {
            restoreTargetAuthBackup();
            addActivityLog("Pemulihan otomatis: AUTH lama dikembalikan setelah proses terputus.");
        } else {
            cleanupCreateBackup();
        }
    }

    private static final String STATE_SELECTED_ACCOUNT = "selected_account";

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Account sel = selectedAccount;
        if (sel != null) outState.putString(STATE_SELECTED_ACCOUNT, sel.category + "\u001F" + sel.name);
    }

    /** Re-selects an account by "category\u001Fname" after a configuration change. */
    private void restoreSelectedAccount(String key){
        if(key==null)return;
        int sep = key.indexOf('\u001F');
        if(sep < 0) return;
        String category = key.substring(0, sep);
        String name = key.substring(sep + 1);
        for (Account a : accounts) {
            if (a.category.equals(category) && a.name.equals(name)) {
                selectedAccount = a;
                notifySelectedAccount(a);
                return;
            }
        }
    }

    private boolean createResumeDialogShown = false;
    private boolean autoLoginResumeDialogShown = false;

    @Override protected void onResume() {
        super.onResume();
        refreshSelectedAccountCard();

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

        // If eVault was closed/restarted while an Auto Login session was still RUNNING/PAUSED/FAILED,
        // offer to resume instead of starting over. Never restart from account 1.
        // A live session is already excluded by !autoLoginRunning, so the persisted workflow mode
        // must NOT be used as a guard here: it can still be MODE_AUTO_LOGIN after a process death
        // (it is set when the game is launched) and the resume dialog would then be skipped forever.
        if (!autoLoginRunning
                && !autoLoginResumeDialogShown
                && hasActiveSession()) {
            autoLoginResumeDialogShown = true;
            loadCountersFromSession();
            showResumeAutoLoginDialog();
        }
    }

    private void loadCountersFromSession(){
        autoLoginSuccessCount = prefs.getInt(KEY_AL_SUCCESS, 0);
        autoLoginFailedCount = prefs.getInt(KEY_AL_FAILED, 0);
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

        new NeoDialogBuilder(this)
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
        HomeLayout home = new HomeLayout(this);
        home.setTag("home");
        home.setBackgroundColor(color(R.color.bgBase));
        TextView logo = uiText("eVault //", 30, R.color.textPrimary, true);
        logo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        logo.setGravity(Gravity.CENTER_VERTICAL);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                logo, 20, 32, 1, TypedValue.COMPLEX_UNIT_SP);
        android.text.SpannableString wordmark = new android.text.SpannableString("eVault //");
        wordmark.setSpan(new android.text.style.ForegroundColorSpan(color(R.color.primary)), 7, 9, 0);
        logo.setText(wordmark);
        home.addView(logo);

        LinearLayout selected = card();
        selected.setPadding(dp(16), dp(12), dp(20), dp(16));
        selected.setBackground(neoSurface(R.color.primary, false, 4));
        selected.addView(uiText("AKUN TERPILIH", 11, R.color.white, true), fullLp());
        selectedAccountName = uiText("Pilih akun", 28, R.color.white, true);
        selectedAccountName.setMaxLines(1);
        selectedAccountName.setEllipsize(TextUtils.TruncateAt.END);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                selectedAccountName, 18, 30, 1, TypedValue.COMPLEX_UNIT_SP);
        selected.addView(selectedAccountName, new LinearLayout.LayoutParams(-1, 0, 1f));
        selectedAccountDetail = uiText("Buka library untuk memilih akun.", 12, R.color.white, false);
        selectedAccountDetail.setMaxLines(2);
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                selectedAccountDetail, 10, 13, 1, TypedValue.COMPLEX_UNIT_SP);
        selected.addView(selectedAccountDetail, fullLp());
        selected.addView(iconButton("Switch Akun", "switch", false, v -> switchSelected()),
                new LinearLayout.LayoutParams(-1, dp(48)));
        home.addView(selected);

        TextView authTitle = uiText("Kelola AUTH", 20, R.color.textPrimary, true);
        authTitle.setGravity(Gravity.CENTER_VERTICAL);
        home.addView(authTitle);
        LinearLayout auth = new LinearLayout(this);
        auth.setContentDescription("Kelola AUTH");
        auth.setOrientation(LinearLayout.HORIZONTAL);
        Button list = iconButton("Daftar\nAkun", "library", false, v -> showAccountListDialog());
        Button create = iconButton("Buat Akun\nBaru", "add", false, v -> showCreateDialog());
        Button save = iconButton("Simpan\nAUTH", "save", false, v -> showSaveAuthDialog());
        // Stack icons above labels so all three native touch targets remain wide enough.
        for (Button button : new Button[] {list, create, save}) {
            android.graphics.drawable.Drawable icon = button.getCompoundDrawables()[0];
            button.setCompoundDrawables(null, icon, null, null);
            button.setCompoundDrawablePadding(dp(2));
            button.setContentDescription(button.getText().toString().replace('\n', ' '));
        }
        auth.addView(list, homeCell(0)); auth.addView(create, homeCell(1)); auth.addView(save, homeCell(2));
        home.addView(auth);
        home.addView(iconButton("Masuk eFootball  →", "play", true, v -> {
            teardownAutoLogin(); clearWorkflow(); launchGame();
        }));
        LinearLayout secondary = new LinearLayout(this);
        secondary.setOrientation(LinearLayout.HORIZONTAL);
        secondary.addView(iconButton("Auto Login", "auto", false, v -> startAutoLogin()), weightLp(dp(4), 0));
        secondary.addView(iconButton("Pengaturan", "settings", false, v -> toggleSetupVisibility()), weightLp(dp(4), 1));
        home.addView(secondary);
        home.addView(iconButton("Log Aktivitas  →", "log", false, v -> showActivityLogDialog()));
        status = uiText("Siap digunakan.", 11, R.color.textSecondary, false);
        status.setGravity(Gravity.CENTER_VERTICAL);
        status.setMaxLines(1);
        status.setEllipsize(TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        status.setOnClickListener(v -> showInfoDialog("Status eVault", status.getText().toString()));
        home.addView(status);
        mainScreen = home;

        LinearLayout setup = pageColumn();
        setup.addView(secondaryButton("← Pengaturan", v -> closeSetup()), fullLp());
        setup.addView(uiText("Setup", 32, R.color.textPrimary, true), spacedLp(dp(24)));
        setup.addView(uiText("Konfigurasi data eFootball. Bisa dibuka kembali kapan saja.", 14, R.color.textSecondary, false), spacedLp(dp(8)));
        LinearLayout folders = card();
        folders.addView(sectionLabel("PENYIMPANAN"));
        folders.addView(secondaryButton("Pilih AUTH Library", v -> chooseLibrary()), fullLp());
        folders.addView(secondaryButton("Pilih folder AUTH", v -> chooseTree(pickTarget)), spacedLp(dp(8)));
        setup.addView(folders, spacedLp(dp(24)));
        LinearLayout game = card();
        game.addView(sectionLabel("EFOOTBALL CLONE"));
        gamePackageStatus = uiText("", 13, R.color.textSecondary, false);
        game.addView(gamePackageStatus, fullLp());
        game.addView(secondaryButton("Scan & pilih clone", v -> showGamePicker()), spacedLp(dp(16)));
        setup.addView(game, spacedLp(dp(16)));
        setupScreen = pageScroll(setup);
        setupScreen.setTag("setup");
        setupScreen.setVisibility(View.GONE);
        refreshGamePackageStatus();

        // Persistent delay editor is reused by the additional-options page.
        autoLoginDelayEdit = new EditText(this);
        autoLoginDelayEdit.setInputType(2);
        autoLoginDelayEdit.setSingleLine(true);
        autoLoginDelayEdit.setText(String.valueOf(prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY)));
        autoLoginDelayEdit.setHint(String.valueOf(DEFAULT_AUTO_LOGIN_DELAY));
        autoLoginDelayEdit.setContentDescription("Jeda Auto Login dalam detik");
        autoLoginDelayEdit.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) saveAutoLoginDelay(); });
        styleInput(autoLoginDelayEdit);

        LinearLayout settings = pageColumn();
        settings.addView(iconButton("Kembali", "back", false, v -> closeSettings()), fullLp());
        settings.addView(uiText("Pengaturan", 32, R.color.textPrimary, true), spacedLp(dp(24)));
        settings.addView(uiText("Atur dan kelola eVault sesuai kebutuhan.", 14, R.color.textSecondary, false), spacedLp(dp(8)));
        settings.addView(menuButton("Setup", "Konfigurasi data eFootball.", "settings", true, v -> openSetup()), spacedLp(dp(24)));
        settings.addView(menuButton("Kelola AUTH", "Daftar, buat, dan simpan akun.", "library", false, v -> openAuthActions()), spacedLp(dp(16)));
        settings.addView(menuButton("Switch Akun", "Pilih akun yang ingin digunakan.", "switch", false, v -> openSwitchActions()), spacedLp(dp(16)));
        settings.addView(menuButton("Opsi Tambahan", "Auto Login, Discord, dan log aktivitas.", "auto", false, v -> openAdditionalOptions()), spacedLp(dp(16)));
        settings.addView(menuButton("Tentang eVault", "Versi dan informasi aplikasi.", "info", false, v -> openAbout()), spacedLp(dp(16)));
        settingsScreen = pageScroll(settings);
        settingsScreen.setTag("settings");
        settingsScreen.setVisibility(View.GONE);

        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(color(R.color.bgBase));
        host.addView(mainScreen, new FrameLayout.LayoutParams(-1, -1));
        host.addView(settingsScreen, new FrameLayout.LayoutParams(-1, -1));
        host.addView(setupScreen, new FrameLayout.LayoutParams(-1, -1));
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(host, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        screenHost = host;
        setContentView(host);
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (autoLoginScreen != null && autoLoginScreen.getParent() != null) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                } else if (detailScreen != null) closeDetail();
                else if (setupScreen.getVisibility() == View.VISIBLE) closeSetup();
                else if (settingsScreen.getVisibility() == View.VISIBLE) closeSettings();
                else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private LinearLayout.LayoutParams homeCell(int index) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
        lp.leftMargin = index == 0 ? 0 : dp(4);
        lp.rightMargin = index == 2 ? 0 : dp(4);
        return lp;
    }

    private NeoSurface neoSurface(int fillRes, boolean interactive, int shadowDp) {
        return new NeoSurface(this,
                color(fillRes), color(R.color.border), interactive, shadowDp);
    }

    private Button iconButton(String label, String icon, boolean primary, View.OnClickListener listener) {
        Button button = primary ? primaryButton(label, listener) : secondaryButton(label, listener);
        button.setCompoundDrawables(new NeoIcons(this, icon, color(primary ? R.color.white : R.color.textPrimary)), null, null, null);
        button.setCompoundDrawablePadding(dp(8));
        return button;
    }

    private Button menuButton(String title, String subtitle, String icon, boolean primary, View.OnClickListener listener) {
        Button button = iconButton(title + "  ›\n" + subtitle, icon, primary, listener);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextSize(14);
        button.setMinHeight(dp(80));
        button.setPadding(dp(16), dp(12), dp(20), dp(16));
        return button;
    }

    private void reveal(View outgoing, View incoming) {
        if (outgoing != null) outgoing.setVisibility(View.GONE);
        incoming.setVisibility(View.VISIBLE);
        incoming.setAlpha(0f);
        incoming.animate().alpha(1f).setDuration(140).start();
    }

    private void openSetup() {
        reveal(settingsScreen, setupScreen);
        refreshGamePackageStatus();
    }

    private void closeSetup() { reveal(setupScreen, settingsScreen); }

    private LinearLayout detailPage(String title, String description) {
        closeDetail();
        LinearLayout page = pageColumn();
        page.addView(iconButton("Pengaturan", "back", false, v -> closeDetail()), fullLp());
        page.addView(uiText(title, 30, R.color.textPrimary, true), spacedLp(dp(24)));
        page.addView(uiText(description, 14, R.color.textSecondary, false), spacedLp(dp(8)));
        detailScreen = pageScroll(page);
        detailScreen.setTag("detail");
        screenHost.addView(detailScreen, new FrameLayout.LayoutParams(-1, -1));
        reveal(settingsScreen, detailScreen);
        return page;
    }

    private void closeDetail() {
        saveAutoLoginDelay();
        if (detailScreen != null) {
            autoLoginDelayEdit.clearFocus();
            hideKeyboard();
            screenHost.removeView(detailScreen);
            detailScreen = null;
            settingsScreen.setVisibility(View.VISIBLE);
        }
    }

    private void openAuthActions() {
        LinearLayout page = detailPage("Kelola AUTH", "Semua data akun tetap tersimpan di folder pilihanmu.");
        page.addView(iconButton("Daftar Akun", "library", false, v -> showAccountListDialog()), spacedLp(dp(24)));
        page.addView(iconButton("Buat Akun Baru", "add", false, v -> showCreateDialog()), spacedLp(dp(16)));
        page.addView(iconButton("Simpan AUTH", "save", true, v -> showSaveAuthDialog()), spacedLp(dp(16)));
    }

    private void openSwitchActions() {
        LinearLayout page = detailPage("Switch Akun", "Pilih akun dari library, lalu pasang AUTH akun tersebut.");
        page.addView(secondaryButton("Pilih akun dari library", v -> showAccountListDialog()), spacedLp(dp(24)));
        page.addView(iconButton("Switch Akun", "switch", true, v -> switchSelected()), spacedLp(dp(16)));
    }

    private void openAdditionalOptions() {
        LinearLayout page = detailPage("Opsi Tambahan", "Atur Auto Login dan integrasi yang sudah tersedia.");
        LinearLayout options = card();
        options.addView(uiText("Jeda Auto Login (detik)", 14, R.color.textPrimary, true), fullLp());
        if (autoLoginDelayEdit.getParent() instanceof ViewGroup)
            ((ViewGroup) autoLoginDelayEdit.getParent()).removeView(autoLoginDelayEdit);
        options.addView(autoLoginDelayEdit, spacedLp(dp(8)));
        options.addView(iconButton("Mulai Auto Login", "auto", true, v -> startAutoLogin()), spacedLp(dp(16)));
        page.addView(options, spacedLp(dp(24)));
        page.addView(secondaryButton("Pengaturan Discord", v -> showDiscordWebhookDialog()), spacedLp(dp(16)));
        page.addView(iconButton("Log Aktivitas", "log", false, v -> showActivityLogDialog()), spacedLp(dp(16)));
        page.addView(dangerButton("Hapus data AUTH", v -> showDeleteDataConfirmDialog()), spacedLp(dp(16)));
    }

    private void openAbout() {
        LinearLayout page = detailPage("Tentang eVault", "eFootball Account Manager");
        String version;
        try { version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (android.content.pm.PackageManager.NameNotFoundException e) { version = "—"; }
        LinearLayout info = card();
        info.addView(uiText("eVault  /  " + version, 22, R.color.textPrimary, true), fullLp());
        info.addView(uiText("Kelola library AUTH, berpindah akun, dan pantau Auto Login. File AUTH disalin tanpa mengubah isinya.", 14, R.color.textSecondary, false), spacedLp(dp(16)));
        info.addView(uiText("Modern Neo-Brutalist Blue", 12, R.color.primary, true), spacedLp(dp(16)));
        page.addView(info, spacedLp(dp(24)));
    }

    private LinearLayout pageColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(color(R.color.bgBase));
        column.setPadding(dp(24), dp(24), dp(24), dp(32));
        return column;
    }

    private ScrollView pageScroll(LinearLayout content) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(color(R.color.bgBase));
        scroll.addView(content);
        return scroll;
    }

    private TextView uiText(String text, int size, int colorId, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color(colorId));
        view.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        view.setLineSpacing(dp(2), 1f);
        return view;
    }

    private void closeSettings() {
        saveAutoLoginDelay();
        autoLoginDelayEdit.clearFocus();
        hideKeyboard();
        reveal(settingsScreen, mainScreen);
    }

    private void hideKeyboard() {
        android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(screenHost.getWindowToken(), 0);
    }

    private void refreshSelectedAccountCard() {
        if (selectedAccountName == null) return;
        Account account = selectedAccount;
        selectedAccountName.setText(account == null ? "Pilih akun" : account.name);
        selectedAccountName.setContentDescription(account == null ? "Belum ada akun terpilih" : account.name);
        selectedAccountDetail.setText(account == null
                ? "Buka library untuk memilih akun."
                : account.category + " · " + (isLoginVerifiedToday(account)
                    ? "Sudah login hari ini" : "Belum terverifikasi hari ini"));
    }

    // ================= Auto Login progress screen =================

    private void showAutoLoginScreen() {
        buildAutoLoginScreen();
        if (this.autoLoginScreen != null && this.autoLoginScreen.getParent() instanceof ViewGroup) {
            ((ViewGroup) this.autoLoginScreen.getParent()).removeView(this.autoLoginScreen);
        }
        if (this.screenHost != null) {
            this.screenHost.addView(this.autoLoginScreen, new FrameLayout.LayoutParams(-1, -1));
        }
        refreshAutoLoginScreen();
    }

    private void hideAutoLoginScreen() {
        if (this.autoLoginScreen != null && this.autoLoginScreen.getParent() instanceof ViewGroup) {
            ((ViewGroup) this.autoLoginScreen.getParent()).removeView(this.autoLoginScreen);
        }
        this.autoLoginScreen = null;
    }

    private void buildAutoLoginScreen() {
        if (this.autoLoginScreen != null) return;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.bgBase));
        root.setPadding(dp(16), dp(24), dp(16), dp(32));

        TextView title = new TextView(this);
        title.setText("Auto Login");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(color(R.color.textPrimary));
        LinearLayout.LayoutParams titleLp = fullLp();
        titleLp.bottomMargin = dp(18);
        root.addView(title, titleLp);

        LinearLayout progressCard = card();
        progressCard.setBackground(neoSurface(R.color.primary, false, 4));
        progressCard.addView(uiText("PROGRES AUTO LOGIN", 11, R.color.heroSecondary, true), fullLp());
        LinearLayout cardBox = card();

        alProgressCount = new TextView(this);
        alProgressCount.setTextSize(48);
        alProgressCount.setTypeface(Typeface.DEFAULT_BOLD);
        alProgressCount.setTextColor(color(R.color.heroText));
        alProgressCount.setGravity(Gravity.CENTER);
        progressCard.addView(alProgressCount, spacedLp(dp(16)));

        alProgressBar = new TextView(this);
        alProgressBar.setTextSize(18);
        alProgressBar.setTextColor(color(R.color.accent));
        alProgressBar.setGravity(Gravity.CENTER);
        alProgressBar.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams barLp = fullLp();
        barLp.topMargin = dp(10);
        progressCard.addView(alProgressBar, barLp);

        alPercent = new TextView(this);
        alPercent.setTextSize(14);
        alPercent.setTextColor(color(R.color.heroSecondary));
        alPercent.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pctLp = fullLp();
        pctLp.topMargin = dp(4);
        progressCard.addView(alPercent, pctLp);
        alLoading = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        alLoading.setIndeterminateTintList(ColorStateList.valueOf(color(R.color.white)));
        alLoading.setContentDescription("Auto Login sedang diproses");
        LinearLayout.LayoutParams loadingLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        loadingLp.gravity = Gravity.CENTER_HORIZONTAL;
        loadingLp.topMargin = dp(8);
        progressCard.addView(alLoading, loadingLp);
        root.addView(progressCard, cardLp());

        alCurrent = infoLine(cardBox, "Current:");
        alSuccess = infoLine(cardBox, "Successful:");
        alFailed = infoLine(cardBox, "Failed:");
        alRemaining = infoLine(cardBox, "Remaining:");
        alStoppedAt = infoLine(cardBox, "Stopped at:");
        alResumeFrom = infoLine(cardBox, "Resume from:");
        alDuration = infoLine(cardBox, "Duration:");
        alErrorReason = infoLine(cardBox, "Error:");

        alStatus = new TextView(this);
        alStatus.setTextSize(16);
        alStatus.setTypeface(Typeface.DEFAULT_BOLD);
        alStatus.setTextColor(color(R.color.primaryDark));
        alStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = fullLp();
        statusLp.topMargin = dp(14);
        cardBox.addView(alStatus, statusLp);

        root.addView(cardBox, cardLp());

        alPauseButton = secondaryButton("Jeda proses", v -> pauseAutoLogin());
        alStopButton = dangerButton("Hentikan", v -> stopAutoLogin());
        alContinueButton = primaryButton("Lanjutkan", v -> resumeAutoLogin());
        alExitButton = secondaryButton("Keluar", v -> exitAutoLogin());

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.addView(alPauseButton, weightLp(dp(8), 0));
        row1.addView(alStopButton, weightLp(dp(8), 1));
        LinearLayout.LayoutParams row1Lp = fullLp();
        row1Lp.topMargin = dp(8);
        root.addView(row1, row1Lp);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.addView(alContinueButton, weightLp(dp(8), 0));
        row2.addView(alExitButton, weightLp(dp(8), 1));
        LinearLayout.LayoutParams row2Lp = fullLp();
        row2Lp.topMargin = dp(8);
        root.addView(row2, row2Lp);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(color(R.color.bgBase));
        scroll.addView(root);
        this.autoLoginScreen = scroll;
    }

    private TextView infoLine(LinearLayout parent, String label) {
        TextView tv = new TextView(this);
        tv.setTextSize(14);
        tv.setTextColor(color(R.color.textSecondary));
        tv.setTag(label);
        LinearLayout.LayoutParams lp = fullLp();
        lp.topMargin = dp(10);
        parent.addView(tv, lp);
        return tv;
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
        drawable.setStroke(dp(2), color(R.color.border));
        return drawable;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(neoSurface(R.color.surface, false, 4));
        c.setPadding(dp(16), dp(16), dp(20), dp(20));
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
        NeoButton button = new NeoButton(this);
        button.setText(text);
        button.setOnClickListener(listener);
        return button;
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        NeoButton button = (NeoButton) baseButton(text, listener);
        button.setTone(color(R.color.primary), color(R.color.white));
        return button;
    }

    private Button secondaryButton(String text, View.OnClickListener listener) {
        NeoButton button = (NeoButton) baseButton(text, listener);
        button.setTone(color(R.color.surface), color(R.color.textPrimary));
        return button;
    }

    private Button dangerButton(String text, View.OnClickListener listener) {
        NeoButton button = (NeoButton) baseButton(text, listener);
        button.setTone(color(R.color.dangerBg), color(R.color.dangerText));
        return button;
    }

    private void styleInput(EditText input) {
        input.setBackground(neoSurface(R.color.surface, true, 0));
        input.setTextColor(color(R.color.textPrimary));
        input.setHintTextColor(color(R.color.textSecondary));
        input.setMinHeight(dp(48));
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
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
        launcher.launch(folderPickerIntent());
    }

    private Intent folderPickerIntent() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return i;
    }

    private void saveTree(String key, Uri uri) {
        boolean persisted = false;
        try {
            getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            persisted = true;
        } catch (Exception e) {
            addActivityLog("Peringatan: izin folder tidak permanen (" + e.getClass().getSimpleName() + ").");
        }
        prefs.edit().putString(key, uri.toString()).commit();
        if (!persisted) {
            updateStatus("Folder tersimpan, tapi provider tidak mendukung akses permanen. " +
                    "Jika akses hilang setelah restart, pilih folder lain (mis. penyimpanan internal).");
        }
    }

    private DocumentFile getTree(String key) {
        String s = prefs.getString(key, null);
        if (s == null) return null;
        try {
            return DocumentFile.fromTreeUri(this, Uri.parse(s));
        } catch (Exception e) {
            // Malformed / no-longer-grantable persisted URI: behave as if it was never set.
            return null;
        }
    }

    /**
     * The AUTH target, in whichever form it was set: a plain file path
     * (set by an earlier clone build), or a SAF tree when the user picked
     * a folder manually (PILIH FOLDER AUTH). File-backed targets work
     * with every existing DocumentFile operation because ContentResolver
     * opens file:// URIs directly.
     */
    private DocumentFile getTarget() {
        if (MODE_FILE.equals(prefs.getString(KEY_TARGET_MODE, MODE_SAF))) {
            String path = prefs.getString(KEY_TARGET_FILE, null);
            if (path == null || path.isEmpty()) return null;
            try {
                return DocumentFile.fromFile(new File(path));
            } catch (Exception e) {
                return null;
            }
        }
        return getTree(KEY_TARGET);
    }

    /** Shared account ordering: numeric name prefix first, then name alphabetically. */
    private static final Comparator<Account> ACCOUNT_NAME_ORDER = (a, b) -> {
        int sa = nameToSequence(a.name);
        int sb = nameToSequence(b.name);
        if (sa > 0 && sb > 0 && sa != sb) return Integer.compare(sa, sb);
        if (sa > 0 && sb <= 0) return -1;
        if (sa <= 0 && sb > 0) return 1;
        return a.name.compareToIgnoreCase(b.name);
    };

    private void loadAccounts() {
        accounts.clear();
        DocumentFile lib = getTree(KEY_LIBRARY);
        if (validDir(lib)) {
            for (DocumentFile category : safeList(lib)) {
                if (category.isDirectory()) collectAccounts(category, nameOf(category));
            }
        }
        Collections.sort(accounts, (a, b) -> {
            int categoryCompare = a.category.compareToIgnoreCase(b.category);
            if (categoryCompare != 0) return categoryCompare;
            return ACCOUNT_NAME_ORDER.compare(a, b);
        });
        // The list was rebuilt with fresh Account objects; re-point the selection so the
        // selected-account marker and Switch Account keep working after a reload.
        selectedAccount = resolveSelectedAccount();
        refreshSelectedAccountCard();
    }

    private void showAccountListDialog() {
        if (accounts.isEmpty()) {
            showInfoDialog("DAFTAR AKUN", "Belum ada akun di AUTH Library.");
            return;
        }

        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Cari akun atau kategori...");
        search.setTextSize(14);
        styleInput(search);

        ListView listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(dp(10));
        listView.setBackgroundColor(Color.TRANSPARENT);
        listView.setPadding(0, dp(2), 0, dp(2));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(6), dp(8), dp(2));
        box.addView(search, fullLp());
        LinearLayout.LayoutParams listLp = fullLp();
        listLp.topMargin = dp(8);
        box.addView(listView, listLp);

        ArrayList<Object> visibleItems = new ArrayList<>();

        Runnable rebuild = () -> {
            visibleItems.clear();
            String query = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            String lastCategory = null;

            for (Account account : accounts) {
                boolean matches = query.isEmpty()
                        || account.name.toLowerCase(Locale.ROOT).contains(query)
                        || account.category.toLowerCase(Locale.ROOT).contains(query);
                if (!matches) continue;

                if (!account.category.equals(lastCategory)) {
                    visibleItems.add(account.category);
                    lastCategory = account.category;
                }
                visibleItems.add(account);
            }
        };

        BaseAdapter adapter = new BaseAdapter() {
            public int getCount() { return visibleItems.size(); }
            public Object getItem(int position) { return visibleItems.get(position); }
            public long getItemId(int position) { return position; }

            public View getView(int position, View convertView, ViewGroup parent) {
                Object item = visibleItems.get(position);

                if (item instanceof String) {
                    TextView header = new TextView(MainActivity.this);
                    header.setText(((String) item).toUpperCase(Locale.ROOT));
                    header.setTextSize(11);
                    header.setTypeface(Typeface.DEFAULT_BOLD);
                    header.setTextColor(color(R.color.textSecondary));
                    header.setGravity(Gravity.CENTER_VERTICAL);
                    header.setPadding(dp(10), dp(8), dp(10), dp(6));
                    header.setBackgroundColor(Color.TRANSPARENT);
                    return header;
                }

                Account account = (Account) item;
                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(14), dp(16), dp(10));
                row.setMinimumHeight(dp(76));
                row.setBackground(neoSurface(selectedAccount == account ? R.color.lightBlue : R.color.surface, true, 4));

                TextView marker = new TextView(MainActivity.this);
                marker.setText("›");
                marker.setTextSize(18);
                marker.setTextColor(color(R.color.textSecondary));
                marker.setGravity(Gravity.CENTER);
                row.addView(marker, new LinearLayout.LayoutParams(dp(28), dp(44)));

                TextView name = new TextView(MainActivity.this);
                name.setText(account.name);
                name.setTextSize(15);
                name.setTextColor(color(R.color.textPrimary));
                name.setGravity(Gravity.CENTER_VERTICAL);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                LinearLayout details = new LinearLayout(MainActivity.this);
                details.setOrientation(LinearLayout.VERTICAL);
                details.addView(name, fullLp());
                details.addView(uiText(isLoginVerifiedToday(account)
                        ? "Sudah login hari ini" : "Belum terverifikasi hari ini",
                        12, R.color.textSecondary, false), spacedLp(dp(4)));
                row.addView(details, new LinearLayout.LayoutParams(0, -2, 1f));

                if (selectedAccount == account) {
                    TextView selected = new TextView(MainActivity.this);
                    selected.setText("✓");
                    selected.setTextSize(15);
                    selected.setTextColor(color(R.color.primaryDark));
                    selected.setGravity(Gravity.CENTER);
                    row.addView(selected, new LinearLayout.LayoutParams(dp(28), dp(44)));
                }

                row.setOnClickListener(v -> {
                    selectedAccount = account;
                    notifySelectedAccount(account);
                    View root = v.getRootView();
                    Object tag = root.getTag();
                    if (tag instanceof AlertDialog) ((AlertDialog) tag).dismiss();
                });

                return row;
            }
        };

        rebuild.run();
        listView.setAdapter(adapter);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuild.run();
                adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable s) {}
        });

        final AlertDialog dialog = new NeoDialogBuilder(this)
                .setTitle("Library akun")
                .setView(box)
                .setNegativeButton("TUTUP", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Object item = visibleItems.get(position);
            if (item instanceof Account) {
                selectedAccount = (Account) item;
                notifySelectedAccount(selectedAccount);
                dialog.dismiss();
            }
        });

        dialog.show();
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92f);
        int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.85f);
        dialog.getWindow().setLayout(width, Math.min(dp(620), maxHeight));
        dialog.getWindow().getDecorView().setTag(dialog);
    }

    private void notifySelectedAccount(Account account) {
        refreshSelectedAccountCard();
        updateStatus("Dipilih: " + account.category + " / " + account.name);
    }

    private void toggleSetupVisibility() {
        reveal(mainScreen, settingsScreen);
    }

    private void collectAccounts(DocumentFile categoryRoot, String categoryName) {
        for (DocumentFile child : safeList(categoryRoot)) {
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
        teardownAutoLogin(); clearWorkflow();
        if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        Account selected = resolveSelectedAccount();
        if (selected == null) { updateStatus("Pilih akun terlebih dahulu."); return; }
        updateStatus("Mengganti AUTH: " + selected.category + " / " + selected.name + "...");
        runOnIo(() -> copyAuth(selected), ok -> {
            if (Boolean.TRUE.equals(ok)) {
                updateStatus("✓ AUTH diganti: " + selected.category + " / " + selected.name);
            } else {
                updateStatus("Gagal mengganti AUTH. Periksa folder target.");
            }
        });
    }

    /** Re-resolves the currently selected account against the live account list.
     *  loadAccounts() rebuilds Account objects, so reference equality is not reliable. */
    private Account resolveSelectedAccount() {
        Account sel = selectedAccount;
        if (sel == null) return null;
        String key = accountKey(sel);
        for (Account a : accounts) {
            if (accountKey(a).equals(key)) {
                selectedAccount = a;
                return a;
            }
        }
        return null;
    }

    private void showSaveAuthDialog() {
        teardownAutoLogin();
        clearWorkflow();

        DocumentFile target = getTarget();
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

        new NeoDialogBuilder(this)
            .setTitle("PILIH KATEGORI PENYIMPANAN")
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

        AlertDialog dialog = new NeoDialogBuilder(this)
            .setTitle("BUAT KATEGORI BARU")
            .setMessage("Masukkan nama kategori untuk AUTH ini.")
            .setView(input)
            .setNegativeButton("BATAL", null)
            .setPositiveButton("LANJUT", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String categoryName = input.getText().toString().trim();
            String nameError = invalidFileNameReason(categoryName);
            if (nameError != null) {
                input.setError(nameError);
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

        AlertDialog dialog = new NeoDialogBuilder(this)
            .setTitle("NAMA AUTH")
            .setMessage("Kategori: " + categoryName + "\nMasukkan nama penyimpanan AUTH.")
            .setView(box)
            .setNegativeButton("KEMBALI", null)
            .setPositiveButton("SIMPAN", null)
            .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String accountName = input.getText().toString().trim();
            String nameError = invalidFileNameReason(accountName);
            if (nameError != null) {
                input.setError(nameError);
                return;
            }

            // Create the category only now that the user confirmed the save, so cancelling
            // the dialog doesn't leave an empty category folder behind. Folder creation and the
            // AUTH copy are blocking SAF calls, so they run on the IO executor.
            DocumentFile categoryDir = categoryRoot(library, categoryName);
            if (!validDir(categoryDir)) {
                input.setError("Gagal membuka kategori: " + categoryName);
                return;
            }

            DocumentFile duplicate = findChildIgnoreCase(categoryDir, accountName);
            if (duplicate != null) {
                input.setError("Nama \"" + accountName + "\" sudah ada di kategori ini.");
                input.requestFocus();
                return;
            }

            DocumentFile target = getTarget();
            runOnIo(() -> {
                DocumentFile accountDir = categoryDir.createDirectory(accountName);
                if (!validDir(accountDir)) return "Gagal membuat penyimpanan. Coba nama lain.";

                // Defensive: if a directory + AUTH somehow already exists (provider race or a
                // case-insensitive collision the earlier check missed), do not overwrite it.
                DocumentFile preexisting = accountDir.findFile(AUTH_FILE);
                if (preexisting != null && preexisting.isFile()) {
                    return "Nama \"" + accountName + "\" sudah berisi AUTH. Gunakan nama lain.";
                }

                DocumentFile installedAuth = validDir(target) ? target.findFile(AUTH_FILE) : null;
                if (installedAuth == null || !installedAuth.isFile()) {
                    accountDir.delete();
                    return "AUTH yang terpasang sudah tidak ditemukan di target eFootball.";
                }

                if (!copyFile(installedAuth, accountDir, AUTH_FILE)) {
                    accountDir.delete();
                    return "Gagal menyimpan AUTH. Periksa izin folder.";
                }
                return null; // success
            }, error -> {
                if (error != null) {
                    input.setError(error);
                    return;
                }
                addActivityLog("SIMPAN AUTH berhasil: " + categoryName + " / " + accountName);
                updateStatus("✓ AUTH tersimpan: " + categoryName + " / " + accountName);
                dialog.dismiss();
                loadAccounts();
            });
        }));

        dialog.show();
    }

    private DocumentFile findChildIgnoreCase(DocumentFile parent, String wantedName) {
        if (!validDir(parent) || wantedName == null) return null;
        for (DocumentFile child : safeList(parent)) {
            String name = child.getName();
            if (name != null && name.equalsIgnoreCase(wantedName)) return child;
        }
        return null;
    }

    /** Returns a human-readable reason a folder/file name cannot be used, or null if it is valid. */
    private static String invalidFileNameReason(String name) {
        if (name == null) return "Nama wajib diisi.";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "Nama wajib diisi.";
        if (trimmed.equals(".") || trimmed.equals("..")) return "Nama tidak valid.";
        if (trimmed.length() > 60) return "Nama terlalu panjang (maks 60 karakter).";
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) return "Nama tidak boleh mengandung karakter kontrol.";
            if (c == '/' || c == '\\' || c == ':' || c == '*' || c == '?'
                    || c == '"' || c == '<' || c == '>' || c == '|')
                return "Nama tidak boleh mengandung / \\ : * ? \" < > |";
        }
        return null;
    }

    private void showInfoDialog(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new NeoDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showDeleteDataConfirmDialog() {
        teardownAutoLogin(); clearWorkflow();
        new NeoDialogBuilder(this)
            .setTitle("Hapus Data")
            .setMessage("Hapus AUTH di folder AUTH eFootball?")
            .setNegativeButton("TIDAK", null)
            .setPositiveButton("YA", (d,w) -> executeDeleteData())
            .show();
    }

    private void executeDeleteData() {
        DocumentFile target = getTarget();
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE);
        if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH di folder AUTH eFootball."); return; }
        if (auth.delete()) updateStatus("✓ Auth sudah terhapus.");
        else updateStatus("Gagal menghapus AUTH. Periksa izin folder.");
    }

    private void showCreateDialog() {
        teardownAutoLogin(); clearWorkflow(); final EditText input=new EditText(this); input.setInputType(2); input.setSingleLine(true); input.setHint("Jumlah akun");
        new NeoDialogBuilder(this).setTitle("BUAT AKUN BARU").setMessage("Mau buat berapa akun?").setView(input).setNegativeButton("BATAL",null).setPositiveButton("LANJUT",(d,w)->{
            int count; try{count=Integer.parseInt(input.getText().toString().trim());}catch(Exception e){count=0;}
            if(count<1){updateStatus("Jumlah akun minimal 1.");return;} prefs.edit().putInt(KEY_CREATE_COUNT,count).apply(); showCreateNamingDialog(count);
        }).show();
    }

    private void showCreateNamingDialog(int count) {
        String[] modes = {
            "Angka (1-1000)",
            "Huruf batch (1-10, a1-a10, b1-10, ...)"
        };
        new NeoDialogBuilder(this).setTitle("Pola nama akun").setItems(modes,(d,which)->{
            prefs.edit().putString(KEY_CREATE_NAMING, which==0 ? NAMING_NUMERIC : NAMING_LETTERED).apply();
            showCreateLocationDialog(count);
        }).setNegativeButton("BATAL",null).show();
    }

    private void showCreateLocationDialog(int count) {
        DocumentFile existing=getTree(KEY_LIBRARY); String[] items=validDir(existing)?new String[]{"Gunakan folder yang sudah dipilih","Buat folder baru"}:new String[]{"Buat folder baru"};
        new NeoDialogBuilder(this).setTitle("Lokasi penyimpanan").setItems(items,(d,which)->{if(validDir(existing)&&which==0)startCreateWorkflow(existing,count);else pickCreateFolder.launch(folderPickerIntent());}).setNegativeButton("BATAL",null).show();
    }

    private void startCreateWorkflow(DocumentFile root,int count) {
        final DocumentFile accountRoot=accountRoot(root); if(!validDir(accountRoot)||count<1){updateStatus("Jumlah/folder penyimpanan tidak valid.");return;}
        final DocumentFile target=getTarget(); if(!validDir(target)){updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");return;}
        final List<String> categories = getLibraryCategories();
        if(categories.isEmpty()){updateStatus("Tidak ada kategori akun ditemukan di library.");return;}
        new NeoDialogBuilder(this).setTitle("Kategori akun baru").setItems(categories.toArray(new String[0]),(d,which)->{
            String category=categories.get(which); DocumentFile categoryDir=categoryRoot(accountRoot,category);
            if(!validDir(categoryDir)){updateStatus("Gagal membuka kategori: "+category);return;}
            prefs.edit().putString(KEY_CREATE_CATEGORY,category).apply();
            backupTargetAuth(target);
            DocumentFile auth=target.findFile(AUTH_FILE); if(auth!=null&&auth.isFile())auth.delete();
            prepareCreate(categoryDir,count);
        }).setNegativeButton("BATAL",null).show();
    }

    /** Before create-account deletes the installed AUTH, keep a backup so a cancel/
     *  crash cannot leave the target with no AUTH at all. */
    private void backupTargetAuth(DocumentFile target){
        if(!validDir(target))return;
        DocumentFile auth=target.findFile(AUTH_FILE);
        if(auth==null||!auth.isFile()){prefs.edit().remove(KEY_CREATE_BACKUP).apply();return;}
        DocumentFile bak=target.findFile(AUTH_FILE+".bak");
        if(bak!=null)bak.delete();
        DocumentFile created=target.createFile("application/octet-stream",AUTH_FILE+".bak");
        if(created==null){prefs.edit().remove(KEY_CREATE_BACKUP).apply();return;}
        try(InputStream in=getContentResolver().openInputStream(auth.getUri());
            OutputStream out=getContentResolver().openOutputStream(created.getUri(),"w")){
            if(in==null||out==null)throw new Exception("stream null");
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1)out.write(buf,0,n);
            out.flush();
            prefs.edit().putString(KEY_CREATE_BACKUP,created.getUri().toString()).apply();
        }catch(Exception e){
            if(created!=null)created.delete();
            prefs.edit().remove(KEY_CREATE_BACKUP).apply();
        }
    }

    /** Restores the AUTH that create-account backed up, if any. Safe to call when none. */
    private void restoreTargetAuthBackup(){
        String uri=prefs.getString(KEY_CREATE_BACKUP,null);
        prefs.edit().remove(KEY_CREATE_BACKUP).apply();
        if(uri==null)return;
        DocumentFile target=getTarget();
        if(!validDir(target))return;
        DocumentFile bak=target.findFile(AUTH_FILE+".bak");
        if(bak==null||!bak.isFile())return;
        boolean ok=copyFile(bak,target,AUTH_FILE);
        bak.delete();
        addActivityLog(ok?"AUTH lama dipulihkan setelah pembuatan akun dibatalkan.":"Gagal memulihkan AUTH lama.");
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
        prefs.edit().putString(KEY_CREATE_NAMES, TextUtils.join("|",names)).putInt(KEY_CREATE_INDEX,0).apply();
        setMode(MODE_CREATE);
        launchCurrentCreateAccount();
    }

    private void launchCurrentCreateAccount() {
        DocumentFile target=getTarget(); if(target!=null){DocumentFile old=target.findFile(AUTH_FILE);if(old!=null&&old.isFile())old.delete();}
        List<String> names=split(prefs.getString(KEY_CREATE_NAMES,"")); int index=prefs.getInt(KEY_CREATE_INDEX,0); if(index>=names.size()){finishCreate();return;}
        updateStatus("Buat akun baru ["+(index+1)+"/"+names.size()+"]: "+names.get(index)); if(launchGame())workflowLaunchPending=true;else clearWorkflow();
    }

    public static List<String> buildAvailableNames(DocumentFile root,int count,String naming) {
        if (count < 1) return Collections.emptyList();

        // Resume from the actual highest existing account name in the selected category.
        int startSequence = 0;
        if (root != null) {
            for (DocumentFile child : safeList(root)) {
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
            if (root == null || !childExistsIgnoreCase(root, name)) result.add(name);
            sequence++;
        }
        return result;
    }

    /** Case-insensitive child lookup that also survives providers where findFile is exact-match. */
    private static boolean childExistsIgnoreCase(DocumentFile parent, String wanted) {
        if (parent == null || wanted == null) return false;
        try {
            if (parent.findFile(wanted) != null) return true;
            DocumentFile[] children = parent.listFiles();
            if (children != null) {
                for (DocumentFile child : children) {
                    String n = child.getName();
                    if (n != null && n.equalsIgnoreCase(wanted)) return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
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
                // Clamp: a manually-created folder like "99999999" must not push the
                // auto-numbering into an absurd, slow range.
                long value = Long.parseLong(name);
                if (value <= 0) return 0;
                return (int) Math.min(value, 1000000L);
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
        new NeoDialogBuilder(this).setTitle("Akun Baru: "+current).setMessage("Simpan atau hapus AUTH?").setCancelable(false)
                .setNegativeButton("BATAL",(d,w)->{clearWorkflow();restoreTargetAuthBackup();updateStatus("Pembuatan akun baru dihentikan. AUTH lama dipulihkan bila ada.");})
                .setNeutralButton("HAPUS",(d,w)->{DocumentFile target=getTarget();DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth!=null&&auth.isFile())auth.delete();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();})
                .setPositiveButton("SIMPAN",(d,w)->{
                    DocumentFile target=getTarget();DocumentFile auth=target==null?null:target.findFile(AUTH_FILE);if(auth==null||!auth.isFile()){updateStatus("AUTH tidak ditemukan di Target.");clearWorkflow();return;}
                    DocumentFile library=getTree(KEY_LIBRARY);DocumentFile root=categoryRoot(library,prefs.getString(KEY_CREATE_CATEGORY,"AKUN"));if(!validDir(root)){updateStatus("Kategori penyimpanan tidak tersedia.");clearWorkflow();return;}
                    updateStatus("Menyimpan AUTH: "+current+"...");
                    // Folder creation + AUTH copy are blocking SAF calls: keep them off the UI thread.
                    runOnIo(() -> {
                        DocumentFile folder=root.findFile(current);
                        boolean created=false;
                        if(folder==null){folder=root.createDirectory(current);created=true;}
                        if(!validDir(folder)) return false;
                        if(!copyFile(auth,folder,AUTH_FILE)){
                            // Only remove a folder we just created; never delete a pre-existing one.
                            if(created) folder.delete();
                            return false;
                        }
                        return true;
                    }, saved -> {
                        if(!Boolean.TRUE.equals(saved)){updateStatus("Gagal menyimpan AUTH: "+current);clearWorkflow();return;}
                        auth.delete();loadAccounts();int next=index+1;prefs.edit().putInt(KEY_CREATE_INDEX,next).apply();if(next>=names.size())finishCreate();else launchCurrentCreateAccount();
                    });
                }).show();
    }

    private void finishCreate(){int total=split(prefs.getString(KEY_CREATE_NAMES,"")).size();cleanupCreateBackup();clearWorkflow();loadAccounts();updateStatus("✓ Pembuatan akun selesai. Total: "+total);}

    /** Removes the create-account AUTH backup once the workflow finished successfully. */
    private void cleanupCreateBackup(){
        prefs.edit().remove(KEY_CREATE_BACKUP).apply();
        DocumentFile target=getTarget();
        if(validDir(target)){DocumentFile bak=target.findFile(AUTH_FILE+".bak");if(bak!=null)bak.delete();}
    }

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

    private final Object activityLogLock = new Object();

    private void addActivityLog(String message) {
        if (prefs == null) return;
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        String entry = "[" + timestamp + "] " + message;
        // Serialize the read-modify-write: copyAuth logs from the IO thread while the
        // main thread can log too, and unsynchronized writers would drop entries.
        synchronized (activityLogLock) {
            String old = prefs.getString(KEY_ACTIVITY_LOG, "");
            ArrayList<String> lines = new ArrayList<>();
            if (old != null && !old.isEmpty()) {
                for (String line : old.split("\n")) {
                    if (!line.isEmpty()) lines.add(line);
                }
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
        logText.setBackground(makeDrawableWithStroke(color(R.color.bgBase), color(R.color.divider), dp(10)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(logText);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, dp(420));
        scrollLp.setMargins(dp(8), dp(4), dp(8), dp(4));

        new NeoDialogBuilder(this)
            .setTitle("LOG AKTIVITAS")
            .setView(scroll)
            .setNegativeButton("HAPUS LOG", (d, w) -> new NeoDialogBuilder(this)
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

        new NeoDialogBuilder(this)
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
                DiscordWebhook.sendEmbedTransaction(
                    url,
                    "🔔 eVault — WEBHOOK",
                    "Webhook berhasil terhubung.",
                    0,
                    "[{\"name\":\"Status\",\"value\":\"🟢 CONNECTED\",\"inline\":true}]",
                    null
                );
            })
            .show();
    }

    // ============ Auto Login progress session (persistent) ============

    private void beginSession(int total){
        autoLoginGeneration++;
        autoLoginResumeDialogShown = false;
        sessionLoginDay = loginDayKey();
        autoLoginSuccessCount = 0;
        autoLoginFailedCount = 0;
        String sessionId = String.valueOf(System.currentTimeMillis());
        String now = String.valueOf(System.currentTimeMillis());
        prefs.edit()
            .putString(KEY_AL_SESSION_ID, sessionId)
            .putInt(KEY_AL_SESSION_VERSION, 2)
            .putInt(KEY_AL_TOTAL, total)
            .putInt(KEY_AL_SUCCESS, 0)
            .putInt(KEY_AL_FAILED, 0)
            .putString(KEY_AL_STATUS, AL_STATUS_RUNNING)
            .putString(KEY_AL_CURRENT_ACCOUNT, "")
            .putString(KEY_AL_NEXT_ACCOUNT, "")
            .putString(KEY_AL_STARTED_AT, now)
            .putString(KEY_AL_UPDATED_AT, now)
            .remove(KEY_AL_FINISHED_AT)
            .remove(KEY_AL_LAST_ERROR)
            .remove(KEY_AL_WEBHOOK_MSG_ID)
            .putBoolean(KEY_AL_WAITING_CONFIRMATION, false)
            .putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_PREPARING)
            .putString(KEY_AL_ACCOUNTS, joinAccountKeys())
            .apply();

        // Create the single progress message once for this session. Webhook failures never
        // block or fail the login process; the message id stays null and edits are skipped.
        String url = currentWebhookUrl();
        if (!url.isEmpty()) {
            final int generation = autoLoginGeneration;
            renderWebhookEmbedData(AL_STATUS_RUNNING, null, data -> {
                DiscordWebhook.sendEmbedTransaction(url, data.title, data.description, data.color, data.fieldsJson, id -> {
                    // Ignore a late callback from a session that has already been replaced,
                    // and never store a "gone" sentinel as a message id.
                    if (id != null && !DiscordWebhook.GONE.equals(id) && generation == autoLoginGeneration)
                        prefs.edit().putString(KEY_AL_WEBHOOK_MSG_ID, id).apply();
                });
            });
        }
    }

    private String currentWebhookUrl(){
        String url = prefs.getString(KEY_DISCORD_WEBHOOK, "");
        return url == null ? "" : url.trim();
    }

    private boolean hasActiveSession(){
        String status = prefs.getString(KEY_AL_STATUS, "");
        return AL_STATUS_RUNNING.equals(status) || AL_STATUS_PAUSED.equals(status)
                || AL_STATUS_STOPPED.equals(status) || AL_STATUS_FAILED.equals(status);
    }

    private void persistProgress(String status, String current, String next, String error){
        int total = prefs.getInt(KEY_AL_TOTAL, autoLoginAccounts.size());
        SharedPreferences.Editor edit = prefs.edit()
            .putInt(KEY_AL_TOTAL, total)
            .putInt(KEY_AL_SUCCESS, autoLoginSuccessCount)
            .putInt(KEY_AL_FAILED, autoLoginFailedCount)
            .putString(KEY_AL_STATUS, status)
            .putString(KEY_AL_CURRENT_ACCOUNT, current == null ? "" : current)
            .putString(KEY_AL_NEXT_ACCOUNT, next == null ? "" : next)
            .putString(KEY_AL_UPDATED_AT, String.valueOf(System.currentTimeMillis()));
        if (error != null) edit.putString(KEY_AL_LAST_ERROR, error);
        edit.apply();
        if (AL_STATUS_COMPLETED.equals(status) || AL_STATUS_STOPPED.equals(status) || AL_STATUS_FAILED.equals(status)) {
            if (!prefs.contains(KEY_AL_FINISHED_AT) || AL_STATUS_COMPLETED.equals(status))
                prefs.edit().putString(KEY_AL_FINISHED_AT, String.valueOf(System.currentTimeMillis())).apply();
        }
    }

    /** Edits the single session webhook message (never creates a new one). */
    private void updateWebhookProgress(String status){
        String url = currentWebhookUrl();
        if (url.isEmpty()) return;
        String messageId = prefs.getString(KEY_AL_WEBHOOK_MSG_ID, null);
        if (messageId == null || messageId.isEmpty()) return;
        // Webhook is monitoring-only: failures are ignored, never retried in a loop.
        renderWebhookEmbedData(status, null, data ->
            DiscordWebhook.editEmbed(url, messageId, data.title, data.description, data.color, data.fieldsJson, result -> {
                // The message was deleted: drop the id so nothing edits a dead message.
                if (DiscordWebhook.GONE.equals(result)) prefs.edit().remove(KEY_AL_WEBHOOK_MSG_ID).apply();
            })
        );
    }

    private String nextAccountName(){
        int nextIndex = autoLoginIndex + 1;
        if (nextIndex < autoLoginAccounts.size()) {
            return autoLoginAccounts.get(nextIndex).name;
        }
        return "-";
    }

    private static final class WebhookEmbedData {
        final String title;
        final String description;
        final int color;
        final String fieldsJson;
        WebhookEmbedData(String title, String description, int color, String fieldsJson) {
            this.title = title;
            this.description = description;
            this.color = color;
            this.fieldsJson = fieldsJson;
        }
    }

    private interface WebhookEmbedCallback {
        void onData(WebhookEmbedData data);
    }

    private void renderWebhookEmbedData(String status, String errorReason, WebhookEmbedCallback callback) {
        int total = prefs.getInt(KEY_AL_TOTAL, autoLoginAccounts.size());
        int done = autoLoginSuccessCount + autoLoginFailedCount;
        int percent = progressPercent();
        String current = prefs.getString(KEY_AL_CURRENT_ACCOUNT, "");
        String next = prefs.getString(KEY_AL_NEXT_ACCOUNT, "");
        String gameState = prefs.getString(KEY_AL_EFOOTBALL_STATE, "-");
        String description = done + " / " + total + " accounts · " + percent + "%\n" + progressBar(percent);

        String statusText;
        if (AL_STATUS_COMPLETED.equals(status)) statusText = "✅ COMPLETED";
        else if (AL_STATUS_STOPPED.equals(status)) statusText = "🛑 STOPPED";
        else if (AL_STATUS_PAUSED.equals(status)) statusText = "⏸️ PAUSED";
        else if (AL_STATUS_FAILED.equals(status)) statusText = "🔴 FAILED";
        else if (AL_STATUS_WAITING.equals(status)) statusText = "🟡 WAITING FOR LOGIN";
        else statusText = "🟢 RUNNING";

        StringBuilder fields = new StringBuilder("[");
        appendEmbedField(fields, "✅ Successful", String.valueOf(autoLoginSuccessCount), true);
        appendEmbedField(fields, "❌ Failed", String.valueOf(autoLoginFailedCount), true);
        appendEmbedField(fields, "⏳ Remaining", String.valueOf(Math.max(0, total - done)), true);

        if (AL_STATUS_STOPPED.equals(status)) {
            appendEmbedField(fields, "👤 Last", current.isEmpty() ? "-" : current, true);
            appendEmbedField(fields, "➡️ Resume from", next.isEmpty() ? nextAccountName() : next, true);
        } else if (AL_STATUS_PAUSED.equals(status)) {
            appendEmbedField(fields, "👤 Current", current.isEmpty() ? "-" : current, true);
            appendEmbedField(fields, "➡️ Resume from", current.isEmpty() ? "-" : current, true);
        } else if (AL_STATUS_FAILED.equals(status)) {
            appendEmbedField(fields, "👤 Current", current.isEmpty() ? "-" : current, true);
            appendEmbedField(fields, "➡️ Resume from", next.isEmpty() ? nextAccountName() : next, true);
            if (errorReason != null && !errorReason.isEmpty()) appendEmbedField(fields, "⚠️ Error", errorReason, false);
        } else if (AL_STATUS_COMPLETED.equals(status)) {
            appendEmbedField(fields, "⏱️ Duration", formatDuration(), true);
        } else {
            appendEmbedField(fields, "👤 Current", current.isEmpty() ? "-" : current, true);
            appendEmbedField(fields, "⏱️ Delay", prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY) + " sec", true);
        }

        appendEmbedField(fields, "🎮 eFootball", gameState, false);
        appendEmbedField(fields, "📌 Status", statusText, false);
        fields.append("]");

        callback.onData(new WebhookEmbedData("⚡ eVault — AUTO LOGIN", description, 0, fields.toString()));
    }

    private void appendEmbedField(StringBuilder fields, String name, String value, boolean inline) {
        if (fields.length() > 1) fields.append(",");
        fields.append("{\"name\":\"")
                .append(DiscordWebhook.escapeJson(name))
                .append("\",\"value\":\"")
                .append(DiscordWebhook.escapeJson(value))
                .append("\",\"inline\":")
                .append(inline)
                .append("}");
    }

    private String statusEmoji(String status){
        if (AL_STATUS_COMPLETED.equals(status)) return "\uD83D\uDFE2";
        if (AL_STATUS_STOPPED.equals(status)) return "\uD83D\uDED1";
        if (AL_STATUS_PAUSED.equals(status)) return "\u23F8";
        if (AL_STATUS_FAILED.equals(status)) return "\uD83D\uDD34";
        return "\uD83D\uDFE2";
    }

    private String formatDuration(){
        String started = prefs.getString(KEY_AL_STARTED_AT, null);
        String finished = prefs.getString(KEY_AL_FINISHED_AT, null);
        long end = System.currentTimeMillis();
        if (finished != null && !finished.isEmpty()) {
            try { end = Long.parseLong(finished); } catch (Exception ignored) {}
        }
        long start = end;
        if (started != null && !started.isEmpty()) {
            try { start = Long.parseLong(started); } catch (Exception ignored) {}
        }
        long seconds = Math.max(0, (end - start) / 1000L);
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + "m " + secs + "s";
    }

    private int progressPercent(){
        int total = prefs.getInt(KEY_AL_TOTAL, autoLoginAccounts.size());
        if (total <= 0) return 0;
        int done = autoLoginSuccessCount + autoLoginFailedCount;
        int percent = (int) Math.round(done * 100.0 / total);
        return Math.max(0, Math.min(100, percent));
    }

    private String progressBar(int percent){
        int filled = Math.round(percent / 100f * 18f);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 18; i++) sb.append(i < filled ? "\u2588" : "\u2591");
        return sb.toString();
    }

    // ============ Auto Login progress screen refresh / navigation ============

    private void refreshAutoLoginScreen(){
        if (autoLoginScreen == null) return;
        String status = prefs.getString(KEY_AL_STATUS, AL_STATUS_RUNNING);
        int total = prefs.getInt(KEY_AL_TOTAL, autoLoginAccounts.size());
        int done = autoLoginSuccessCount + autoLoginFailedCount;
        int percent = progressPercent();
        String current = prefs.getString(KEY_AL_CURRENT_ACCOUNT, "");
        String next = prefs.getString(KEY_AL_NEXT_ACCOUNT, "");
        String error = prefs.getString(KEY_AL_LAST_ERROR, "");

        alProgressCount.setText(done + " / " + total);
        alProgressBar.setText(progressBar(percent));
        alPercent.setText(percent + "%");
        alCurrent.setText("Current: " + (current.isEmpty() ? "-" : current));
        alSuccess.setText("Successful: " + autoLoginSuccessCount);
        alFailed.setText("Failed: " + autoLoginFailedCount);
        alRemaining.setText("Remaining: " + Math.max(0, total - done));
        alStatus.setText("Status: " + statusEmoji(status) + " " + status);
        alStatus.setTextColor(color(AL_STATUS_FAILED.equals(status) ? R.color.dangerText : R.color.primaryDark));
        alLoading.setVisibility(AL_STATUS_RUNNING.equals(status) ? View.VISIBLE : View.GONE);

        boolean paused = AL_STATUS_PAUSED.equals(status) || AL_STATUS_STOPPED.equals(status) || AL_STATUS_FAILED.equals(status);
        alStoppedAt.setVisibility(paused ? View.VISIBLE : View.GONE);
        alResumeFrom.setVisibility(paused ? View.VISIBLE : View.GONE);
        alErrorReason.setVisibility(AL_STATUS_FAILED.equals(status) && !error.isEmpty() ? View.VISIBLE : View.GONE);
        alDuration.setVisibility(AL_STATUS_COMPLETED.equals(status) ? View.VISIBLE : View.GONE);

        if (paused) {
            alStoppedAt.setText((AL_STATUS_PAUSED.equals(status) ? "Paused on: " : "Stopped on: ")
                    + (current.isEmpty() ? "-" : current));
            alResumeFrom.setText("Resume from: " + (next.isEmpty() ? "-" : next));
        }
        if (AL_STATUS_FAILED.equals(status) && !error.isEmpty()) {
            alErrorReason.setText("Error: " + error);
        }
        if (AL_STATUS_COMPLETED.equals(status)) {
            alDuration.setText("Duration: " + formatDuration());
        }

        alPauseButton.setVisibility(AL_STATUS_RUNNING.equals(status) ? View.VISIBLE : View.GONE);
        alStopButton.setVisibility(AL_STATUS_RUNNING.equals(status) ? View.VISIBLE : View.GONE);
        alContinueButton.setVisibility(paused ? View.VISIBLE : View.GONE);
        alExitButton.setVisibility(paused || AL_STATUS_COMPLETED.equals(status) ? View.VISIBLE : View.GONE);
    }

    private void exitAutoLogin(){
        autoLoginResumeDialogShown = false;
        sessionLoginDay = null;
        workflowLaunchPending = false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
        prefs.edit()
            .remove(KEY_AL_SESSION_ID).remove(KEY_AL_SESSION_VERSION).remove(KEY_AL_STATUS).remove(KEY_AL_CURRENT_ACCOUNT)
            .remove(KEY_AL_NEXT_ACCOUNT).remove(KEY_AL_LAST_ERROR).remove(KEY_AL_WEBHOOK_MSG_ID)
            .remove(KEY_AL_WAITING_CONFIRMATION).remove(KEY_AL_EFOOTBALL_STATE)
            .apply();
        hideAutoLoginScreen();
        updateStatus("AUTO LOGIN ditutup. Progress dibuang.");
    }

    /** Offered on app restart when a session was left RUNNING/PAUSED/FAILED. */
    private void showResumeAutoLoginDialog(){
        String status = prefs.getString(KEY_AL_STATUS, "");
        int total = prefs.getInt(KEY_AL_TOTAL, 0);
        int success = prefs.getInt(KEY_AL_SUCCESS, 0);
        int failed = prefs.getInt(KEY_AL_FAILED, 0);
        int done = success + failed;
        String next = prefs.getString(KEY_AL_NEXT_ACCOUNT, "");
        if (next == null || next.isEmpty()) next = "-";

        new NeoDialogBuilder(this)
            .setTitle(AL_STATUS_PAUSED.equals(status) ? "AUTO LOGIN PAUSED"
                    : AL_STATUS_STOPPED.equals(status) ? "AUTO LOGIN STOPPED" : "RESUME AUTO LOGIN")
            .setMessage(done + " / " + total + " completed\n\nResume from: " + next)
            .setCancelable(false)
            .setPositiveButton("RESUME", (d, w) -> resumeAutoLogin())
            .setNegativeButton("EXIT", (d, w) -> exitAutoLogin())
            .show();
    }

    /**
     * Rebuilds the auto login account list for the saved session and continues from the saved
     * index. Used both for the CONTINUE button and for the app-restart RESUME flow.
     */
    private void resumeAutoLogin(){
        String category = prefs.getString(KEY_AUTO_LOGIN_CATEGORY, null);
        int savedIndex = prefs.getInt(KEY_AUTO_LOGIN_INDEX, 0);

        if(accounts.isEmpty()) loadAccounts();

        ArrayList<Account> rebuilt = new ArrayList<>();
        // savedIndex points into the original (unfiltered) session list; remap it to the
        // rebuilt list so already-verified accounts don't shift the resume position.
        int resumePos = savedIndex;
        if(category != null){
            List<String> savedKeys = split(prefs.getString(KEY_AL_ACCOUNTS, ""));
            resumePos = 0;
            for (int i = 0; i < savedKeys.size(); i++) {
                String savedKey = savedKeys.get(i);
                for (Account a : accounts) {
                    if (accountKey(a).equals(savedKey) && !isLoginVerifiedToday(a)) {
                        if (i < savedIndex) resumePos++;
                        rebuilt.add(a);
                        break;
                    }
                }
            }
            if (rebuilt.isEmpty()) {
                for(Account a : accounts){
                    if(!a.category.equals(category)) continue;
                    if(isLoginVerifiedToday(a)) continue;
                    rebuilt.add(a);
                }
                resumePos = savedIndex;
            }
            Collections.sort(rebuilt, ACCOUNT_NAME_ORDER);
        }

        if(rebuilt.isEmpty()){
            updateStatus("Tidak bisa resume: akun tidak ditemukan.");
            addActivityLog("AUTO LOGIN resume gagal: akun tidak ditemukan.");
            return;
        }

        // Restore counters from the persisted session.
        autoLoginSuccessCount = prefs.getInt(KEY_AL_SUCCESS, 0);
        autoLoginFailedCount = prefs.getInt(KEY_AL_FAILED, 0);
        int index = Math.max(0, Math.min(resumePos, rebuilt.size() - 1));

        autoLoginAccounts.clear();
        autoLoginAccounts.addAll(rebuilt);
        autoLoginIndex = index;
        autoLoginRunning = true;
        autoLoginGeneration++;
        sessionLoginDay = loginDayKey();
        boolean waitingConfirmation = prefs.getBoolean(KEY_AL_WAITING_CONFIRMATION, false);
        // Keep the session's original total: success/failed counters are cumulative and the
        // rebuilt list may be smaller after filtering out verified accounts.
        prefs.edit()
            .putInt(KEY_AL_SUCCESS, autoLoginSuccessCount)
            .putInt(KEY_AL_FAILED, autoLoginFailedCount)
            .putString(KEY_AL_STATUS, AL_STATUS_RUNNING)
            .putString(KEY_AL_EFOOTBALL_STATE, waitingConfirmation ? AL_GAME_WAITING : AL_GAME_PREPARING)
            .remove(KEY_AL_LAST_ERROR)
            .apply();

        setMode(MODE_AUTO_LOGIN);
        showAutoLoginScreen();
        addActivityLog("AUTO LOGIN dilanjutkan dari akun:" + autoLoginAccounts.get(index).name);
        updateStatus("AUTO LOGIN dilanjutkan [" + (index + 1) + "/" + autoLoginAccounts.size() + "]");
        refreshAutoLoginScreen();
        updateWebhookProgress(waitingConfirmation ? AL_STATUS_WAITING : AL_STATUS_RUNNING);
        if (waitingConfirmation) {
            showAutoLoginConfirmationDialog();
        } else {
            runNextAutoLogin();
        }
    }

    private void startAutoLogin(){
        if(accounts.isEmpty()){updateStatus("Library kosong.");return;}
        if(autoLoginRunning)return;
        List<String> categories = getLibraryCategories();
        if(categories.isEmpty()){updateStatus("Tidak ada kategori akun ditemukan di library.");return;}

        new NeoDialogBuilder(this)
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
        final AlertDialog[] dlgHolder = new AlertDialog[1];
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
            Button b = secondaryButton(label, v -> {
                if (dlgHolder[0] != null) dlgHolder[0].dismiss();
                startAutoLoginForCategory(cat);
            });
            box.addView(b, spacedLp(dp(6)));
        }
        final AlertDialog dlg = new NeoDialogBuilder(this)
            .setTitle("PILIH KATEGORI")
            .setView(box)
            .setNegativeButton("BATAL", null)
            .create();
        dlgHolder[0] = dlg;
        dlg.show();
    }

    private List<String> getLibraryCategories(){
        List<String> result = new ArrayList<>();
        DocumentFile lib = getTree(KEY_LIBRARY);
        if(!validDir(lib)) return result;
        for(DocumentFile child : safeList(lib)){
            if(child.isDirectory()) result.add(nameOf(child));
        }
        Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private void startAutoLoginForCategory(String category){
        addActivityLog("Kategori AUTO LOGIN dipilih: " + category);

        clearWorkflow();
        autoLoginAccounts.clear();
        for(Account a : accounts){
            if(!a.category.equals(category)) continue;
            // A confirmed login is authoritative for the current login day,
            // regardless of the selected Auto Login mode.
            if(isLoginVerifiedToday(a)) continue;
            autoLoginAccounts.add(a);
        }

        Collections.sort(autoLoginAccounts, ACCOUNT_NAME_ORDER);

        if(autoLoginAccounts.isEmpty()){
            updateStatus("✓ Semua akun " + category + " sudah login hari ini.");
            addActivityLog("AUTO LOGIN tidak punya akun yang perlu diproses: " + category);
            return;
        }

        autoLoginRunning=true;
        autoLoginIndex=0;
        addActivityLog("AUTO LOGIN mulai: " + category + " (" + autoLoginAccounts.size() + " akun)");
        prefs.edit()
            .putString(KEY_AUTO_LOGIN_CATEGORY, category)
            .putInt(KEY_AUTO_LOGIN_INDEX, 0)
            .apply();
        beginSession(autoLoginAccounts.size());
        showAutoLoginScreen();
        runNextAutoLogin();
    }

    private void runNextAutoLogin(){
        if(!autoLoginRunning) return;
        if(autoLoginIndex>=autoLoginAccounts.size()){
            finishAutoLoginCompleted();
            return;
        }

        Account account=autoLoginAccounts.get(autoLoginIndex);
        final int generation = autoLoginGeneration;
        prefs.edit().putString(KEY_AUTO_LOGIN_PENDING, accountKey(account)).putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex).apply();
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_PREPARING).apply();
        persistProgress(AL_STATUS_RUNNING, account.name, nextAccountName(), null);
        addActivityLog("AUTO LOGIN memproses akun: " + account.category + " / " + account.name);
        updateStatus("AUTO LOGIN ["+(autoLoginIndex+1)+"/"+autoLoginAccounts.size()+"]: "+account.category+" / "+account.name);
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_RUNNING);

        autoLoginIoExecutor.execute(() -> {
            // Discard work from a superseded session before it touches the AUTH file.
            if (generation != autoLoginGeneration) return;
            boolean copied = copyAuth(account);
            autoLoginHandler.post(() -> {
                if (!autoLoginRunning || generation != autoLoginGeneration) return;
                if (!copied) {
                    autoLoginFailedCount++;
                    failAutoLogin("COPY AUTH gagal untuk " + account.name);
                    return;
                }
                continueAutoLoginAfterCopy(account);
            });
        });
    }

    private void continueAutoLoginAfterCopy(Account account){
        if (!autoLoginRunning) return;
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_OPENED).apply();
        updateWebhookProgress(AL_STATUS_RUNNING);
        if (!launchGame()) {
            autoLoginFailedCount++;
            failAutoLogin("Gagal membuka eFootball untuk " + account.name);
            return;
        }

        // onResume() will show the login confirmation when eFootball returns.
        workflowLaunchPending=true;
        setMode(MODE_AUTO_LOGIN);
    }

    private void finishAutoLoginCompleted(){
        autoLoginRunning=false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
        prefs.edit().remove(KEY_AUTO_LOGIN_PENDING).remove(KEY_AUTO_LOGIN_CATEGORY).remove(KEY_AUTO_LOGIN_INDEX).apply();
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_COMPLETED)
            .putBoolean(KEY_AL_WAITING_CONFIRMATION, false).apply();
        persistProgress(AL_STATUS_COMPLETED, "", "", null);
        addActivityLog("AUTO LOGIN selesai. Berhasil: " + autoLoginSuccessCount + ", Gagal: " + autoLoginFailedCount);
        updateStatus("✓ AUTO LOGIN selesai. Berhasil: "+autoLoginSuccessCount+", Gagal: "+autoLoginFailedCount);
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_COMPLETED);
    }

    private void failAutoLogin(String reason){
        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_STOPPED)
            .putBoolean(KEY_AL_WAITING_CONFIRMATION, false).apply();
        persistProgress(AL_STATUS_FAILED, prefs.getString(KEY_AL_CURRENT_ACCOUNT, ""), nextAccountName(), reason);
        addActivityLog("AUTO LOGIN FAILED: " + reason);
        updateStatus("AUTO LOGIN FAILED: " + reason);
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_FAILED);
    }

    private void showAutoLoginConfirmationDialog(){
        if(!autoLoginRunning || !MODE_AUTO_LOGIN.equals(getMode())) return;
        Account account = findAutoLoginPendingAccount();
        if(account == null){
            addActivityLog("AUTO LOGIN error: akun pending tidak ditemukan.");
            teardownAutoLogin();
            return;
        }

        updateStatus("Konfirmasi login: " + account.category + " / " + account.name);
        prefs.edit().putBoolean(KEY_AL_WAITING_CONFIRMATION, true).putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_WAITING).apply();
        updateWebhookProgress(AL_STATUS_WAITING);

        new NeoDialogBuilder(this)
            .setTitle("KONFIRMASI LOGIN")
            .setMessage("Apakah akun " + account.name + " sudah berhasil login di eFootball?")
            .setCancelable(false)
            .setPositiveButton("LOGIN BERHASIL", (d,w) -> confirmAutoLoginSuccess(account))
            .setNegativeButton("BELUM LOGIN", (d,w) -> retryAutoLogin(account))
            .show();
    }

    private void confirmAutoLoginSuccess(Account account){
        markLoginVerifiedToday(account);
        autoLoginSuccessCount++;
        prefs.edit().putBoolean(KEY_AL_WAITING_CONFIRMATION, false)
                .putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_VERIFIED).apply();
        persistProgress(AL_STATUS_RUNNING, account.name, nextAccountName(), null);
        addActivityLog("LOGIN VERIFIED hari ini: " + account.category + " / " + account.name);
        updateStatus("✓ " + account.name + " tercatat sudah login hari ini.");
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_RUNNING);

        new NeoDialogBuilder(this)
            .setTitle("LANJUT AUTO LOGIN?")
            .setMessage("Akun " + account.name + " sudah berhasil login dan tercatat hari ini.\n\nLanjut ke akun berikutnya?")
            .setCancelable(false)
            .setNegativeButton("STOP", (d, w) -> stopAutoLogin())
            .setPositiveButton("LANJUT", (d, w) -> advanceAutoLoginAfterDelay(account, false))
            .show();
    }

    private void retryAutoLogin(Account account){
        prefs.edit().putBoolean(KEY_AL_WAITING_CONFIRMATION, false)
                .putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_RETRY).apply();
        persistProgress(AL_STATUS_RUNNING, account.name, account.name, null);
        addActivityLog("LOGIN BELUM BERHASIL: " + account.category + " / " + account.name);
        updateStatus("Login belum terverifikasi: " + account.name + ". Menunggu delay sebelum coba lagi.");
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_RUNNING);
        advanceAutoLoginAfterDelay(account, true);
    }

    private void advanceAutoLoginAfterDelay(Account account, boolean retrySame){
        if(!autoLoginRunning) return;
        autoLoginHandler.removeCallbacksAndMessages(null);
        autoLoginHandler.postDelayed(() -> {
            if(!autoLoginRunning) return;
            if(!retrySame){
                autoLoginIndex++;
                prefs.edit().putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex).apply();
            }
            runNextAutoLogin();
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

    /** Login day rolls over every day at 09:00 WIB (UTC+7). */
    private String loginDayKey(){
        long now = System.currentTimeMillis();
        // 09:00 WIB == 02:00 UTC. Shift the UTC clock back by 2 hours,
        // then use the UTC date as the eFootball login day key.
        Date loginDayDate = new Date(now - (2L * 60L * 60L * 1000L));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(loginDayDate);
    }

    /** Login-day used for the current session; frozen at session start to avoid
     *  a mid-session 09:00 WIB rollover splitting verification records. */
    private String currentLoginDay(){
        return sessionLoginDay != null ? sessionLoginDay : loginDayKey();
    }

    private boolean isLoginVerifiedToday(Account account){
        String key = accountKey(account) + "\u001F" + currentLoginDay();
        Set<String> verified = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        return verified != null && verified.contains(key);
    }

    private void markLoginVerifiedToday(Account account){
        String today = currentLoginDay();
        String key = accountKey(account) + "\u001F" + today;
        Set<String> old = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        HashSet<String> updated = new HashSet<>();
        if(old != null) updated.addAll(old);

        // Drop only records strictly older than the current login day (never drop
        // today's records, even if the wall clock rolls over mid-session).
        Iterator<String> it = updated.iterator();
        while(it.hasNext()){
            String entry = it.next();
            int split = entry.lastIndexOf("\u001F");
            if(split < 0 || entry.substring(split + 1).compareTo(today) < 0) it.remove();
        }

        updated.add(key);
        prefs.edit().putStringSet(KEY_LOGIN_VERIFIED, updated).apply();
    }

    private long getAutoLoginDelayMs(){
        try{
            if(autoLoginDelayEdit==null) throw new IllegalStateException("no delay view");
            int seconds=Integer.parseInt(autoLoginDelayEdit.getText().toString().trim());
            seconds=Math.min(AUTO_MAX_DELAY_SECONDS, Math.max(2,seconds));
            prefs.edit().putInt(KEY_AUTO_LOGIN_DELAY, seconds).apply();
            return seconds*1000L;
        }catch(Exception e){
            int saved=prefs.getInt(KEY_AUTO_LOGIN_DELAY, DEFAULT_AUTO_LOGIN_DELAY);
            saved=Math.min(AUTO_MAX_DELAY_SECONDS, Math.max(2, saved));
            return saved*1000L;
        }
    }

    /** Stops Auto Login. The resume point stays on the current account unless it was already
     * confirmed (marked login-verified today), in which case the next account becomes
     * the resume point. */
    private void stopAutoLogin(){
        if (!autoLoginRunning) return;

        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);

        String current = prefs.getString(KEY_AL_CURRENT_ACCOUNT, "");
        boolean currentDone = autoLoginIndex >= 0 && autoLoginIndex < autoLoginAccounts.size()
                && isLoginVerifiedToday(autoLoginAccounts.get(autoLoginIndex));
        autoLoginIndex = Math.min(autoLoginIndex + (currentDone ? 1 : 0), autoLoginAccounts.size());
        String next = autoLoginIndex < autoLoginAccounts.size() ? autoLoginAccounts.get(autoLoginIndex).name : "-";
        prefs.edit()
            .putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex)
            .remove(KEY_AUTO_LOGIN_PENDING)
            .putBoolean(KEY_AL_WAITING_CONFIRMATION, false)
            .putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_STOPPED)
            .apply();

        persistProgress(AL_STATUS_STOPPED, current, next, null);
        addActivityLog("AUTO LOGIN dihentikan. Terakhir: " + current + ". Resume dari: " + next);
        updateStatus("AUTO LOGIN STOPPED. Resume dari: " + next);
        showAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_STOPPED);
    }

    /** Temporarily pauses Auto Login without advancing the current account. */
    private void pauseAutoLogin(){
        if(!autoLoginRunning) return;

        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);

        Account pending = findAutoLoginPendingAccount();
        String current = pending != null ? pending.name : prefs.getString(KEY_AL_CURRENT_ACCOUNT, "");
        boolean waitingConfirmation = prefs.getBoolean(KEY_AL_WAITING_CONFIRMATION, false);
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_PAUSED).apply();
        persistProgress(AL_STATUS_PAUSED, current, current, null);
        addActivityLog("AUTO LOGIN PAUSED: " + (current.isEmpty() ? "belum ada akun aktif" : current));
        updateStatus("AUTO LOGIN PAUSED. Resume dari: " + (current.isEmpty() ? "akun saat ini" : current));
        showAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_PAUSED);

        // Keep the confirmation state intact so Resume returns to the same confirmation/account.
        prefs.edit().putBoolean(KEY_AL_WAITING_CONFIRMATION, waitingConfirmation).apply();
    }

    private String joinAccountKeys(){
        StringBuilder result = new StringBuilder();
        for (Account account : autoLoginAccounts) {
            if (result.length() > 0) result.append("|");
            result.append(accountKey(account));
        }
        return result.toString();
    }

    /** Hard reset of auto login state without touching the persisted progress session. */
    private void teardownAutoLogin(){
        autoLoginGeneration++;
        autoLoginRunning=false;
        autoLoginResumeDialogShown = false;
        sessionLoginDay = null;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        prefs.edit().remove(KEY_AUTO_LOGIN_PENDING).remove(KEY_AUTO_LOGIN_CATEGORY).remove(KEY_AUTO_LOGIN_INDEX).remove(KEY_AUTO_LOGIN_MODE)
            .remove(KEY_AL_WAITING_CONFIRMATION).remove(KEY_AL_EFOOTBALL_STATE).apply();
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
    }

    private boolean copyAuth(Account account){
        DocumentFile target=getTarget();
        if(!validDir(target)){
            addActivityLog("COPY AUTH gagal: folder target belum dipilih.");
            updateStatus("Pilih folder AUTH eFootball terlebih dahulu.");
            return false;
        }
        boolean ok=copyFile(account.file,target,AUTH_FILE);
        addActivityLog("COPY AUTH " + (ok ? "berhasil: " : "gagal: ") + account.category + " / " + account.name);
        return ok;
    }
    private boolean copyFile(DocumentFile source, DocumentFile parent, String name){
        // Write the new content to a temp file first so the installed AUTH stays intact
        // if the copy fails. Only replace the real file once the new copy is complete.
        String tmpName = name + ".tmp";
        DocumentFile stale = parent.findFile(tmpName);
        if (stale != null) stale.delete();
        DocumentFile tmp = parent.createFile("application/octet-stream", tmpName);
        if (tmp == null) return false;

        // Some SAF providers return a document whose display name differs from the requested
        // one (they may append an extension or dedupe). renameTo(name) would then fail and the
        // user would be stuck with "Gagal menyimpan AUTH". Detect it and write the real file
        // directly instead of relying on a rename.
        String actualTmpName = tmp.getName();
        if (actualTmpName == null || !actualTmpName.equals(tmpName)) {
            tmp.delete();
            return replaceOrCreate(source, parent, name);
        }

        if (!streamInto(source, tmp, "w")) { tmp.delete(); return false; }

        // Try an atomic rename first.
        DocumentFile old = parent.findFile(name);
        if (old == null) {
            if (tmp.renameTo(name)) return true;
            // renameTo is not supported by this provider: create the final file directly.
            tmp.delete();
            return replaceOrCreate(source, parent, name);
        }
        // Replace in place by truncating the existing document. This is safer than
        // delete-then-rename because many SAF providers do not support renameTo, and a
        // failed rename after a delete would leave the target with no AUTH at all.
        if (!streamInto(tmp, old, "wt")) { tmp.delete(); return false; }
        tmp.delete();
        return true;
    }

    /** Streams source content into dest using the given open mode. Returns false on any error. */
    private boolean streamInto(DocumentFile source, DocumentFile dest, String mode){
        int bytes = 0;
        try (InputStream in = getContentResolver().openInputStream(source.getUri());
             OutputStream out = getContentResolver().openOutputStream(dest.getUri(), mode)) {
            if (in == null || out == null) throw new Exception("stream null");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) { out.write(buf, 0, n); bytes += n; }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return bytes > 0;
    }

    /** Creates (or reuses) `name` under parent and streams the source into it. */
    private boolean replaceOrCreate(DocumentFile source, DocumentFile parent, String name){
        DocumentFile dest = parent.findFile(name);
        if (dest == null) dest = parent.createFile("application/octet-stream", name);
        if (dest == null) return false;
        return streamInto(source, dest, "wt");
    }
    /**
     * Lists installed eFootball clones. The official Konami app and the legacy
     * epl.idsphone package are excluded from clone selection.
     */
    private ArrayList<String> installedClonePackages(){
        Intent mainIntent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps=getPackageManager().queryIntentActivities(mainIntent,0);
        ArrayList<String> clones=new ArrayList<>();
        for(android.content.pm.ResolveInfo ri:apps){
            if(ri.activityInfo==null) continue;
            String pn=ri.activityInfo.packageName;
            String label=String.valueOf(ri.loadLabel(getPackageManager()));
            if(!OFFICIAL_GAME_PACKAGE.equals(pn)
                    && !ORIGINAL_GAME_PACKAGE.equals(pn)
                    && isEfootballPackage(pn,label)
                    && !clones.contains(pn)){
                clones.add(pn);
            }
        }
        Collections.sort(clones);
        return clones;
    }

    /** Returns only the explicitly selected, currently installed clone. */
    private String gamePackage(){
        ArrayList<String> clones=installedClonePackages();
        String selected=prefs.getString(KEY_GAME_PACKAGE,"");
        return !selected.isEmpty() && clones.contains(selected) ? selected : null;
    }

    /** Shows detected clone packages from Setup and saves the selected target. */
    private void showGamePicker(){
        ArrayList<String> clones=installedClonePackages();
        if(clones.isEmpty()){
            prefs.edit().remove(KEY_GAME_PACKAGE).apply();
            refreshGamePackageStatus();
            updateStatus("Tidak ada eFootball clone yang terdeteksi.");
            return;
        }
        String[] labels=new String[clones.size()];
        String selected=prefs.getString(KEY_GAME_PACKAGE,"");
        for(int n=0;n<clones.size();n++){
            String pkg=clones.get(n);
            String appLabel=pkg;
            try{
                appLabel=String.valueOf(getPackageManager().getApplicationLabel(
                        getPackageManager().getApplicationInfo(pkg,0)));
            }catch(Exception ignored){}
            labels[n]=(pkg.equals(selected) ? "✓ " : "")+appLabel+"\n"+pkg;
        }
        new NeoDialogBuilder(this)
            .setTitle("PILIH EFOOTBALL CLONE")
            .setItems(labels,(dialog,which)->{
                String pkg=clones.get(which);
                prefs.edit().putString(KEY_GAME_PACKAGE,pkg).apply();
                refreshGamePackageStatus();
                updateStatus("Clone eFootball dipilih: "+pkg);
                addActivityLog("Target eFootball clone dipilih: "+pkg);
            })
            .setNegativeButton("TUTUP",null)
            .show();
    }

    /** Refreshes the selected clone and live detection count shown in Setup. */
    private void refreshGamePackageStatus(){
        if(gamePackageStatus==null) return;
        ArrayList<String> clones=installedClonePackages();
        String selected=gamePackage();
        if(selected==null){
            gamePackageStatus.setText("Terdeteksi: "+clones.size()+" package\nBelum ada clone yang dipilih.");
            return;
        }
        String label=selected;
        try{
            label=String.valueOf(getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(selected,0)));
        }catch(Exception ignored){}
        gamePackageStatus.setText("Terdeteksi: "+clones.size()+" package\nDipilih: "+label+" ("+selected+")");
    }

    private boolean launchGame(){
        String pkg=gamePackage();
        if(pkg==null||pkg.isEmpty()){
            addActivityLog("Gagal membuka eFootball: clone belum dipilih di Setup.");
            updateStatus("Pilih eFootball clone terlebih dahulu melalui Setup.");
            return false;
        }
        return launchGame(pkg);
    }

    private boolean launchGame(String pkg){
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){
            addActivityLog("Gagal membuka eFootball: package " + pkg + " tidak ditemukan.");
            updateStatus("Package "+pkg+" tidak ditemukan.");
            return false;
        }
        try{
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            addActivityLog("Berhasil membuka eFootball ("+pkg+").");
            updateStatus("Membuka eFootball: "+pkg);
            return true;
        }catch(Exception e){
            addActivityLog("Gagal membuka eFootball: " + e.getClass().getSimpleName());
            updateStatus("Gagal membuka eFootball.");
            return false;
        }
    }

    /** Detects eFootball clones by their visible label or known package families. */
    private boolean isEfootballPackage(String pkg,String label){
        String p=pkg==null?"":pkg.toLowerCase(Locale.ROOT);
        String l=label==null?"":label.toLowerCase(Locale.ROOT);
        if(l.contains("efootball")) return true;
        if(p.contains("idsphone")) return true;
        if(p.contains("idspatch") || p.contains("ids.patch")) return true;
        if(p.contains("konami")) return true;
        if(p.contains("efootball")) return true;
        if(p.contains("pes20")) return true;
        return false;
    }
    /** Null-safe directory check that also swallows provider errors (revoked/expired SAF grant). */
    private boolean validDir(DocumentFile d){
        if(d==null) return false;
        try{
            return d.isDirectory();
        }catch(Exception e){
            return false;
        }
    }

    /** Safe directory listing: never returns null and swallows provider errors
     *  (e.g. a revoked/expired SAF permission) so callers cannot crash. */
    private static DocumentFile[] safeList(DocumentFile dir){
        if(dir==null) return new DocumentFile[0];
        try{
            if(!dir.isDirectory()) return new DocumentFile[0];
            DocumentFile[] children=dir.listFiles();
            return children==null?new DocumentFile[0]:children;
        }catch(Exception e){
            return new DocumentFile[0];
        }
    }
    private String getMode(){return prefs.getString(KEY_MODE,MODE_NONE);}
    private void setMode(String mode){prefs.edit().putString(KEY_MODE,mode).apply();}
    private void clearWorkflow(){workflowLaunchPending=false;prefs.edit().putString(KEY_MODE,MODE_NONE).remove(KEY_CREATE_NAMES).remove(KEY_CREATE_INDEX).remove(KEY_CREATE_COUNT).remove(KEY_CREATE_CATEGORY).remove(KEY_CREATE_NAMING).apply();}
    private List<String> split(String value){return value==null||value.isEmpty()?Collections.emptyList():Arrays.asList(value.split("\\|",-1));}
    private void updateStatus(String message) {
        if (status == null) return;
        status.post(() -> {
            if (status == null) return;
            status.setText(message);
            status.setContentDescription(message);
            String lower = message.toLowerCase(Locale.ROOT);
            boolean error = lower.contains("gagal") || lower.contains("tidak valid") || lower.contains("failed");
            boolean success = message.startsWith("✓");
            status.setTextColor(color(error ? R.color.dangerText : success ? R.color.successText : R.color.textSecondary));
            status.setBackgroundColor(color(error ? R.color.dangerBg : success ? R.color.successBg : R.color.bgBase));
            refreshSelectedAccountCard();
        });
    }

    /** Runs blocking storage work off the UI thread, then delivers the result on the UI thread. */
    private <T> void runOnIo(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> onDone){
        try{
            uiIoExecutor.execute(() -> {
                T result;
                try{
                    result = work.call();
                }catch(Exception e){
                    result = null;
                }
                final T r = result;
                autoLoginHandler.post(() -> { if(!isFinishing() && !isDestroyed()) onDone.accept(r); });
            });
        }catch(java.util.concurrent.RejectedExecutionException e){
            // Activity is shutting down; drop the work silently.
        }
    }
    @Override protected void onDestroy(){
        saveAutoLoginDelay();
        // A configuration change (rotation, etc.) must NOT tear down Auto Login: the activity is
        // recreated and any in-flight session would silently abort. AndroidManifest declares
        // configChanges so the activity is normally not recreated at all; this guard covers the
        // remaining cases (e.g. a change we do not handle) where isChangingConfigurations() is true.
        if (!isChangingConfigurations()) {
            // Cancel pending auto-login callbacks: the queued Runnables belong to this instance
            // and must not run against a shut-down executor after destroy.
            autoLoginHandler.removeCallbacksAndMessages(null);
            autoLoginIoExecutor.shutdownNow();
        }
        // User-triggered copy work belongs to this instance; dropping it is safe because the
        // callback checks isFinishing()/isDestroyed() before touching any view.
        uiIoExecutor.shutdownNow();
        // Release view references so a detached hierarchy is not retained.
        status = null;
        autoLoginDelayEdit = null;
        mainScreen = null;
        autoLoginScreen = null;
        screenHost = null;
        super.onDestroy();
    }

    static class Account{final String category;final String name;final DocumentFile file;Account(String category,String name,DocumentFile file){this.category=category;this.name=name;this.file=file;}}
}
