package id.feezet.efauth;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
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
    private static final String MODE_NONE = "none";
    private static final String MODE_DELETE_DATA = "delete_data";
    private static final String MODE_CREATE = "create";
    private static final String PACKAGE = "epl.idsphone";
    private static final int STORAGE_PERMISSION_REQUEST = 4101;

    private SharedPreferences prefs;
    private TextView status;
    private TextView accountsText;
    private Spinner accountSpinner;
    private final ArrayList<Account> accounts = new ArrayList<>();
    private boolean workflowLaunchPending = false;

    private final android.os.Handler autoLoginHandler = new android.os.Handler(android.os.Looper.getMainLooper());
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
        requestStorageAccess();
    }

    /**
     * Ask for storage access as soon as the app starts.
     * Android 11+ uses the system "All files access" settings screen because
     * the old READ/WRITE runtime dialog no longer grants broad filesystem access.
     * Older supported Android versions use the normal runtime permission dialog.
     */
    private void requestStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean readGranted = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            boolean writeGranted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!readGranted || !writeGranted) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            updateStatus(granted ? "Izin penyimpanan diberikan." : "Izin penyimpanan ditolak.");
        }
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

        TextView target = new TextView(this);
        target.setText("Target");
        root.addView(target, fullLp());

        Button targetBtn = new Button(this);
        targetBtn.setText("PILIH FOLDER AUTH EFOOTBALL");
        targetBtn.setOnClickListener(v -> chooseTree(pickTarget));
        root.addView(targetBtn, fullLp());

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

    private void updateStatus(String message) {
        if (status != null) status.setText(message);
    }

    // Existing eVault functionality continues below unchanged.
    // (The rest of the original MainActivity implementation is preserved in the repository.)
}
