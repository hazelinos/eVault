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
    private static final String AL_STATUS_LEGACY_PAUSED = "PAUSED";
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
    private static final int MAX_ACTIVITY_LOGS = 300;
    private static final String NAMING_NUMERIC = "numeric";
    private static final String NAMING_LETTERED = "lettered";
    private static final String MODE_NONE = "none";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";

    private SharedPreferences prefs;
    private TextView status;
    private Account selectedAccount;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private final ArrayList<Account> autoLoginAccounts = new ArrayList<>();
    private boolean workflowLaunchPending = false;
    private final Handler autoLoginHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService autoLoginIoExecutor = Executors.newSingleThreadExecutor();
    private boolean autoLoginRunning = false;
    private int autoLoginIndex = 0;
    private int autoLoginSuccessCount = 0;
    private int autoLoginFailedCount = 0;
    private EditText autoLoginDelayEdit;
    private int setupCardId = View.NO_ID;

    // ---- Auto Login progress screen ----
    private View mainScreen;
    private View autoLoginScreen;
    private FrameLayout screenHost;
    private TextView alProgressCount;
    private TextView alProgressBar;
    private TextView alPercent;
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
        if (AL_STATUS_LEGACY_PAUSED.equals(prefs.getString(KEY_AL_STATUS, ""))
                && prefs.getInt(KEY_AL_SESSION_VERSION, 0) < 2) {
            prefs.edit().putString(KEY_AL_STATUS, AL_STATUS_STOPPED).apply();
        }
        buildUi();
        loadAccounts();
    }

    private boolean createResumeDialogShown = false;
    private boolean autoLoginResumeDialogShown = false;

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

        // If eVault was closed/restarted while an Auto Login session was still RUNNING/PAUSED/FAILED,
        // offer to resume instead of starting over. Never restart from account 1.
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
        // Setup actions: 2 rows x 2 columns.
        LinearLayout setupRow1 = new LinearLayout(this);
        setupRow1.setOrientation(LinearLayout.HORIZONTAL);
        Button library = secondaryButton("PILIH AUTH LIBRARY", v -> chooseLibrary());
        setupRow1.addView(library, weightLp(dp(6), 0));
        Button target = secondaryButton("PILIH FOLDER AUTH", v -> chooseTree(pickTarget));
        setupRow1.addView(target, weightLp(dp(6), 1));
        setupCard.addView(setupRow1, fullLp());

        LinearLayout setupRow2 = new LinearLayout(this);
        setupRow2.setOrientation(LinearLayout.HORIZONTAL);
        Button webhookBtn = secondaryButton("🔔 WEBHOOK", v -> showDiscordWebhookDialog());
        setupRow2.addView(webhookBtn, weightLp(dp(6), 0));
        Button activityLogBtn = secondaryButton("📋 LOG AKTIVITAS", v -> showActivityLogDialog());
        setupRow2.addView(activityLogBtn, weightLp(dp(6), 1));
        LinearLayout.LayoutParams setupRow2Lp = fullLp();
        setupRow2Lp.topMargin = dp(8);
        setupCard.addView(setupRow2, setupRow2Lp);

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

        // ---- Daftar akun ----
        Button accountListBtn = secondaryButton("DAFTAR AKUN", v -> showAccountListDialog());
        LinearLayout.LayoutParams accountListBtnLp = fullLp();
        accountListBtnLp.bottomMargin = dp(16);
        root.addView(accountListBtn, accountListBtnLp);

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

        // Actions row 3: SIMPAN AUTH + MASUK EFOOTBALL
        LinearLayout row3 = new LinearLayout(this);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        Button saveAuthBtn = primaryButton("SIMPAN AUTH", v -> showSaveAuthDialog());
        row3.addView(saveAuthBtn, weightLp(dp(8), 0));

        Button enterBtn = primaryButton("MASUK EFOOTBALL", v -> {
            teardownAutoLogin();
            clearWorkflow();
            launchGame();
        });
        row3.addView(enterBtn, weightLp(dp(8), 1));

        LinearLayout.LayoutParams row3Lp = fullLp();
        row3Lp.bottomMargin = dp(16);
        root.addView(row3, row3Lp);

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
        this.mainScreen = scroll;

        // A single FrameLayout host lets us swap between the main screen and the
        // Auto Login progress screen without rebuilding the whole UI.
        FrameLayout host = new FrameLayout(this);
        host.setBackgroundColor(color(R.color.bgBase));
        host.addView(this.mainScreen, new FrameLayout.LayoutParams(-1, -1));
        this.screenHost = host;
        setContentView(host);
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
        root.setPadding(dp(20), dp(28), dp(20), dp(24));

        TextView title = new TextView(this);
        title.setText("AUTO LOGIN");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(color(R.color.textPrimary));
        LinearLayout.LayoutParams titleLp = fullLp();
        titleLp.bottomMargin = dp(18);
        root.addView(title, titleLp);

        LinearLayout cardBox = card();

        alProgressCount = new TextView(this);
        alProgressCount.setTextSize(22);
        alProgressCount.setTypeface(Typeface.DEFAULT_BOLD);
        alProgressCount.setTextColor(color(R.color.textPrimary));
        alProgressCount.setGravity(Gravity.CENTER);
        cardBox.addView(alProgressCount, fullLp());

        alProgressBar = new TextView(this);
        alProgressBar.setTextSize(18);
        alProgressBar.setTextColor(color(R.color.accent));
        alProgressBar.setGravity(Gravity.CENTER);
        alProgressBar.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams barLp = fullLp();
        barLp.topMargin = dp(10);
        cardBox.addView(alProgressBar, barLp);

        alPercent = new TextView(this);
        alPercent.setTextSize(14);
        alPercent.setTextColor(color(R.color.textSecondary));
        alPercent.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pctLp = fullLp();
        pctLp.topMargin = dp(4);
        cardBox.addView(alPercent, pctLp);

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
        alStatus.setTextColor(color(R.color.accent));
        alStatus.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusLp = fullLp();
        statusLp.topMargin = dp(14);
        cardBox.addView(alStatus, statusLp);

        root.addView(cardBox, cardLp());

        alPauseButton = secondaryButton("PAUSE", v -> pauseAutoLogin());
        alStopButton = dangerButton("STOP", v -> stopAutoLogin());
        alContinueButton = primaryButton("CONTINUE", v -> resumeAutoLogin());
        alExitButton = secondaryButton("EXIT", v -> exitAutoLogin());

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
        lp.topMargin = dp(6);
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
        listView.setDividerHeight(0);
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
                    header.setText("📁  " + ((String) item).toUpperCase(Locale.ROOT));
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
                row.setPadding(dp(10), 0, dp(10), 0);
                row.setMinimumHeight(dp(44));

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
                row.addView(name, new LinearLayout.LayoutParams(0, dp(44), 1f));

                if (selectedAccount == account) {
                    TextView selected = new TextView(MainActivity.this);
                    selected.setText("✓");
                    selected.setTextSize(15);
                    selected.setTextColor(color(R.color.accent));
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

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("DAFTAR AKUN")
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
        dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                dp(620)
        );
        dialog.getWindow().getDecorView().setTag(dialog);
    }

    private Account selectedAccount() {
        return selectedAccount;
    }

    private void notifySelectedAccount(Account account) {
        updateStatus("Dipilih: " + account.category + " / " + account.name);
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
        teardownAutoLogin(); clearWorkflow();
        if (accounts.isEmpty()) { updateStatus("Library kosong / belum dipilih."); return; }
        Account selected = selectedAccount();
        if (selected != null && accounts.contains(selected)) {
            if (copyAuth(selected)) {
                updateStatus("✓ AUTH diganti: " + selected.category + " / " + selected.name);
            }
        } else {
            updateStatus("Pilih akun terlebih dahulu.");
        }
    }

    private void deleteCurrentAuth() {
        teardownAutoLogin(); clearWorkflow(); DocumentFile target = getTree(KEY_TARGET);
        if (!validDir(target)) { updateStatus("Pilih folder AUTH eFootball terlebih dahulu."); return; }
        DocumentFile auth = target.findFile(AUTH_FILE); if (auth == null || !auth.isFile()) { updateStatus("Tidak ada AUTH saat ini."); return; }
        new AlertDialog.Builder(this).setTitle("Hapus AUTH Saat Ini").setMessage("Hapus file AUTH yang sedang terpasang di target?").setNegativeButton("BATAL", null).setPositiveButton("HAPUS", (d,w) -> updateStatus(auth.delete() ? "✓ AUTH saat ini dihapus." : "Gagal menghapus AUTH. Periksa izin folder.")).show();
    }

    private void showSaveAuthDialog() {
        teardownAutoLogin();
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
                input.setError("Nama \"" + accountName + "\" sudah ada di kategori ini.");
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
        teardownAutoLogin(); clearWorkflow();
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
        teardownAutoLogin(); clearWorkflow(); final EditText input=new EditText(this); input.setInputType(2); input.setSingleLine(true); input.setHint("Jumlah akun");
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
        prefs.edit().putString(KEY_CREATE_NAMES, TextUtils.join("|",names)).putInt(KEY_CREATE_INDEX,0).apply();
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

    private void notifyDiscord(String message){
        String url = prefs.getString(KEY_DISCORD_WEBHOOK, "").trim();
        if (!url.isEmpty()) DiscordWebhook.send(url, message);
    }

    // ============ Auto Login progress session (persistent) ============

    private void beginSession(int total){
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
            renderWebhookEmbedData(AL_STATUS_RUNNING, null, data -> {
                DiscordWebhook.sendEmbedTransaction(url, data.title, data.description, data.color, data.fieldsJson, id -> {
                    if (id != null) prefs.edit().putString(KEY_AL_WEBHOOK_MSG_ID, id).apply();
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
        prefs.edit()
            .putInt(KEY_AL_TOTAL, total)
            .putInt(KEY_AL_SUCCESS, autoLoginSuccessCount)
            .putInt(KEY_AL_FAILED, autoLoginFailedCount)
            .putString(KEY_AL_STATUS, status)
            .putString(KEY_AL_CURRENT_ACCOUNT, current == null ? "" : current)
            .putString(KEY_AL_NEXT_ACCOUNT, next == null ? "" : next)
            .putString(KEY_AL_UPDATED_AT, String.valueOf(System.currentTimeMillis()))
            .apply();
        if (error != null) prefs.edit().putString(KEY_AL_LAST_ERROR, error).apply();
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
        // Webhook is monitoring-only: ignore/failure silently, never spam retries.
        renderWebhookEmbedData(status, null, data ->
            DiscordWebhook.editEmbed(url, messageId, data.title, data.description, data.color, data.fieldsJson, null)
        );
    }

    private String nextAccountName(){
        int nextIndex = autoLoginIndex + 1;
        if (nextIndex >= 0 && nextIndex < autoLoginAccounts.size()) {
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
        String description = done + " / " + total + " accounts · " + percent + "%\\n" + progressBar(percent);

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
                .append(escapeWebhookJson(name))
                .append("\",\"value\":\"")
                .append(escapeWebhookJson(value))
                .append("\",\"inline\":")
                .append(inline)
                .append("}");
    }

    private String escapeWebhookJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String statusEmoji(String status){
        if (AL_STATUS_COMPLETED.equals(status)) return "\uD83D\uDFE2";
        if (AL_STATUS_STOPPED.equals(status)) return "\uD83D\uDED1";
        if (AL_STATUS_PAUSED.equals(status)) return "\u23F8";
        if (AL_STATUS_FAILED.equals(status)) return "\uD83D\uDD34";
        return "\uD83D\uDFE2";
    }

    private String formatClock(String millis){
        if (millis == null || millis.isEmpty()) return "--:--";
        try {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(Long.parseLong(millis)));
        } catch (Exception e) {
            return "--:--";
        }
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

        boolean paused = AL_STATUS_PAUSED.equals(status) || AL_STATUS_STOPPED.equals(status) || AL_STATUS_FAILED.equals(status);
        alStoppedAt.setVisibility(paused ? View.VISIBLE : View.GONE);
        alResumeFrom.setVisibility(paused ? View.VISIBLE : View.GONE);
        alErrorReason.setVisibility(AL_STATUS_FAILED.equals(status) && !error.isEmpty() ? View.VISIBLE : View.GONE);
        alDuration.setVisibility(AL_STATUS_COMPLETED.equals(status) ? View.VISIBLE : View.GONE);

        if (paused) {
            alStoppedAt.setText((AL_STATUS_PAUSED.equals(status) ? "Paused at: " : "Stopped at: ")
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
        alStopButton.setVisibility(AL_STATUS_RUNNING.equals(status) && autoLoginSuccessCount > 0 ? View.VISIBLE : View.GONE);
        alContinueButton.setVisibility(paused ? View.VISIBLE : View.GONE);
        alExitButton.setVisibility(paused || AL_STATUS_COMPLETED.equals(status) ? View.VISIBLE : View.GONE);
    }

    private void exitAutoLogin(){
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

        new AlertDialog.Builder(this)
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
        String mode = prefs.getString(KEY_AUTO_LOGIN_MODE, "all");
        int savedIndex = prefs.getInt(KEY_AUTO_LOGIN_INDEX, 0);

        if(accounts.isEmpty()) loadAccounts();

        ArrayList<Account> rebuilt = new ArrayList<>();
        if("single".equals(mode)){
            String pendingKey = prefs.getString(KEY_AUTO_LOGIN_PENDING, null);
            for(Account a : accounts){
                if(pendingKey != null && accountKey(a).equals(pendingKey)) rebuilt.add(a);
            }
            savedIndex = 0;
        } else if(category != null){
            String savedAccounts = prefs.getString(KEY_AL_ACCOUNTS, "");
            for (String savedKey : split(savedAccounts)) {
                for (Account a : accounts) {
                    if (accountKey(a).equals(savedKey)) {
                        rebuilt.add(a);
                        break;
                    }
                }
            }
            if (rebuilt.isEmpty()) {
                for(Account a : accounts){
                    if(!a.category.equals(category)) continue;
                    if("unverified".equals(mode) && isLoginVerifiedToday(a)) continue;
                    rebuilt.add(a);
                }
            }
            Collections.sort(rebuilt, (a, b) -> {
                int sa = nameToSequence(a.name);
                int sb = nameToSequence(b.name);
                if (sa > 0 && sb > 0 && sa != sb) return Integer.compare(sa, sb);
                if (sa > 0 && sb <= 0) return -1;
                if (sa <= 0 && sb > 0) return 1;
                return a.name.compareToIgnoreCase(b.name);
            });
        }

        if(rebuilt.isEmpty()){
            updateStatus("Tidak bisa resume: akun tidak ditemukan.");
            addActivityLog("AUTO LOGIN resume gagal: akun tidak ditemukan.");
            return;
        }

        // Restore counters from the persisted session.
        autoLoginSuccessCount = prefs.getInt(KEY_AL_SUCCESS, 0);
        autoLoginFailedCount = prefs.getInt(KEY_AL_FAILED, 0);
        int index = Math.max(0, Math.min(savedIndex, rebuilt.size() - 1));

        autoLoginAccounts.clear();
        autoLoginAccounts.addAll(rebuilt);
        autoLoginIndex = index;
        autoLoginRunning = true;
        boolean waitingConfirmation = prefs.getBoolean(KEY_AL_WAITING_CONFIRMATION, false);
        prefs.edit()
            .putInt(KEY_AL_SUCCESS, autoLoginSuccessCount)
            .putInt(KEY_AL_FAILED, autoLoginFailedCount)
            .putInt(KEY_AL_TOTAL, autoLoginAccounts.size())
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
        addActivityLog("AUTO LOGIN mulai: " + category + " (" + autoLoginAccounts.size() + " akun)");
        prefs.edit()
            .putString(KEY_AUTO_LOGIN_CATEGORY, category)
            .putInt(KEY_AUTO_LOGIN_INDEX, 0)
            .apply();
        beginSession(autoLoginAccounts.size());
        showAutoLoginScreen();
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
        prefs.edit()
            .putString(KEY_AUTO_LOGIN_MODE, "single")
            .putString(KEY_AUTO_LOGIN_CATEGORY, account.category)
            .putInt(KEY_AUTO_LOGIN_INDEX, 0)
            .apply();

        addActivityLog("AUTO LOGIN akun dipilih: " + account.category + " / " + account.name);
        beginSession(1);
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
        prefs.edit().putString(KEY_AUTO_LOGIN_PENDING, accountKey(account)).putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex).apply();
        prefs.edit().putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_PREPARING).apply();
        persistProgress(AL_STATUS_RUNNING, account.name, nextAccountName(), null);
        addActivityLog("AUTO LOGIN memproses akun: " + account.category + " / " + account.name);
        updateStatus("AUTO LOGIN ["+(autoLoginIndex+1)+"/"+autoLoginAccounts.size()+"]: "+account.category+" / "+account.name);
        refreshAutoLoginScreen();
        updateWebhookProgress(AL_STATUS_RUNNING);

        autoLoginIoExecutor.execute(() -> {
            boolean copied = copyAuth(account);
            autoLoginHandler.post(() -> {
                if (!autoLoginRunning) return;
                if (!copied) {
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
            failAutoLogin("Gagal membuka eFootball untuk " + account.name);
            return;
        }

        // onResume() will show the login confirmation when eFootball returns.
        workflowLaunchPending=true;
        setMode(MODE_AUTO_LOGIN);
    }

    private void finishAutoLoginCompleted(){
        int total = autoLoginAccounts.size();
        autoLoginRunning=false;
        autoLoginFailedCount = Math.max(autoLoginFailedCount, Math.max(0, total - autoLoginSuccessCount));
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

        new AlertDialog.Builder(this)
            .setTitle("KONFIRMASI LOGIN")
            .setMessage("Apakah akun " + account.name + " sudah berhasil login di eFootball?")
            .setCancelable(false)
            .setPositiveButton("LOGIN BERHASIL", (d,w) -> confirmAutoLoginSuccess(account))
            .setNegativeButton("BELUM LOGIN", (d,w) -> retryAutoLogin(account))
            .setNeutralButton("PAUSE", (d,w) -> pauseAutoLogin())
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

        new AlertDialog.Builder(this)
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

    private String utcDate(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
}

    private boolean isLoginVerifiedToday(Account account){
        String today = utcDate();
        String key = accountKey(account) + "\u001F" + today;
        Set<String> verified = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        return verified != null && verified.contains(key);
    }

    private void markLoginVerifiedToday(Account account){
        String key = accountKey(account) + "\u001F" + utcDate();
        Set<String> old = prefs.getStringSet(KEY_LOGIN_VERIFIED, Collections.emptySet());
        HashSet<String> updated = new HashSet<>();
        if(old != null) updated.addAll(old);
        // Keep today's records plus recent entries; stale records are harmless but pruned here.
        String today = utcDate();
        Iterator<String> it = updated.iterator();
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

    /** Stop after a successful login. The confirmed account remains counted and the next
     * account becomes the resume point. */
    private void stopAutoLogin(){
        if (!autoLoginRunning || autoLoginSuccessCount <= 0) return;

        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);

        String current = prefs.getString(KEY_AL_CURRENT_ACCOUNT, "");
        String next = nextAccountName();
        autoLoginIndex = Math.min(autoLoginIndex + 1, Math.max(0, autoLoginAccounts.size()));
        prefs.edit()
            .putInt(KEY_AUTO_LOGIN_INDEX, autoLoginIndex)
            .remove(KEY_AUTO_LOGIN_PENDING)
            .putBoolean(KEY_AL_WAITING_CONFIRMATION, false)
            .putString(KEY_AL_EFOOTBALL_STATE, AL_GAME_STOPPED)
            .apply();

        persistProgress(AL_STATUS_STOPPED, current, next, null);
        addActivityLog("AUTO LOGIN dihentikan setelah login berhasil. Terakhir: " + current + ". Berikutnya: " + next);
        updateStatus("AUTO LOGIN STOPPED. Berikutnya: " + next);
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

    /** Hard reset of auto login state without touching the persisted progress session. */
    private String joinAccountKeys(){
        StringBuilder result = new StringBuilder();
        for (Account account : autoLoginAccounts) {
            if (result.length() > 0) result.append("|");
            result.append(accountKey(account));
        }
        return result.toString();
    }

    private void teardownAutoLogin(){
        autoLoginRunning=false;
        autoLoginHandler.removeCallbacksAndMessages(null);
        workflowLaunchPending=false;
        prefs.edit().remove(KEY_AUTO_LOGIN_PENDING).remove(KEY_AUTO_LOGIN_CATEGORY).remove(KEY_AUTO_LOGIN_INDEX).remove(KEY_AUTO_LOGIN_MODE)
            .remove(KEY_AL_WAITING_CONFIRMATION).remove(KEY_AL_EFOOTBALL_STATE).apply();
        if(MODE_AUTO_LOGIN.equals(getMode())) setMode(MODE_NONE);
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
    private void updateStatus(String s){if(status!=null)status.post(() -> status.setText(s));}
    @Override protected void onDestroy(){
        saveAutoLoginDelay();
        autoLoginIoExecutor.shutdownNow();
        super.onDestroy();
    }

    static class Account{final String category;final String name;final DocumentFile file;Account(String category,String name,DocumentFile file){this.category=category;this.name=name;this.file=file;}}
}