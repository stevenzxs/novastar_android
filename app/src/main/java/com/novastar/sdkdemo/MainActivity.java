package com.novastar.sdkdemo;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.sun.jna.Native;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String TAG = "NovaStarTextDemo";
    private static final long STEP_TIMEOUT_MS = 30000;
    private static final long SEARCH_TIMEOUT_MS = 10000;
    private static final long TRANSFER_TIMEOUT_MS = 600000;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private EditText snInput, ipInput, userInput, passwordInput, textInput;
    private EditText programWidthInput, programHeightInput;
    private Spinner fontFamilySpinner, fontSizeSpinner;
    private TextView statusView;
    private ScrollView logScroll;
    private boolean touchingLog;
    private boolean passwordVisible;
    private Button sendButton;
    private ViplexCore core;
    private ViplexCore.CallBack searchCallback;
    private File sdkDir;

    private interface SdkCall { void invoke(ViplexCore.CallBack callback); }
    private static final class Result {
        final int code;
        final String data;
        Result(int code, String data) { this.code = code; this.data = data == null ? "" : data; }
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        snInput = findViewById(R.id.sn_input);
        ipInput = findViewById(R.id.ip_input);
        userInput = findViewById(R.id.user_input);
        passwordInput = findViewById(R.id.password_input);
        textInput = findViewById(R.id.text_input);
        programWidthInput = findViewById(R.id.program_width_input);
        programHeightInput = findViewById(R.id.program_height_input);
        fontFamilySpinner = findViewById(R.id.font_family_spinner);
        fontSizeSpinner = findViewById(R.id.font_size_spinner);
        ArrayAdapter<CharSequence> families = ArrayAdapter.createFromResource(this,
                R.array.font_families, android.R.layout.simple_spinner_item);
        families.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontFamilySpinner.setAdapter(families);
        ArrayAdapter<CharSequence> sizes = ArrayAdapter.createFromResource(this,
                R.array.font_sizes, android.R.layout.simple_spinner_item);
        sizes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSizeSpinner.setAdapter(sizes);
        statusView = findViewById(R.id.status);
        logScroll = findViewById(R.id.log_scroll);
        View.OnTouchListener logTouch = new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent event) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    touchingLog = true;
                    logScroll.getParent().requestDisallowInterceptTouchEvent(true);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    touchingLog = false;
                    logScroll.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            }
        };
        logScroll.setOnTouchListener(logTouch);
        statusView.setOnTouchListener(logTouch);
        sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendTextProgram(); }
        });
        final ImageButton passwordToggle = findViewById(R.id.password_visibility_toggle);
        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                passwordVisible = !passwordVisible;
                int selection = passwordInput.getSelectionEnd();
                if (passwordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    passwordToggle.setImageResource(R.drawable.ic_eye_off);
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                            | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordToggle.setImageResource(R.drawable.ic_eye_open);
                }
                // setInputType resets the typeface; restore it and keep the cursor position.
                passwordInput.setTypeface(Typeface.DEFAULT);
                if (selection >= 0)
                    passwordInput.setSelection(Math.min(selection, passwordInput.getText().length()));
            }
        });
    }

    private void sendTextProgram() {
        final String sn = snInput.getText().toString().trim();
        final String ip = ipInput.getText().toString().trim();
        final String username = userInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();
        final String text = textInput.getText().toString();
        final String fontFamily = fontFamilySpinner.getSelectedItemPosition() == 0
                ? "Helvetica" : fontFamilySpinner.getSelectedItem().toString();
        final int selectedFontSize = fontSizeSpinner.getSelectedItemPosition() == 0
                ? 0 : Integer.parseInt(fontSizeSpinner.getSelectedItem().toString());
        statusView.setText("");
        report("Send button clicked: font=" + fontFamily + ", selectedFontSize="
                + (selectedFontSize == 0 ? "auto" : selectedFontSize));
        if (TextUtils.isEmpty(sn)) {
            snInput.setError(getString(R.string.required));
            report("Validation failed: SN is empty");
            return;
        }
        if (TextUtils.isEmpty(text.trim())) {
            textInput.setError(getString(R.string.required));
            report("Validation failed: text is empty");
            return;
        }
        final int requestedWidth = optionalDimension(programWidthInput);
        if (requestedWidth < 0) return;
        final int requestedHeight = optionalDimension(programHeightInput);
        if (requestedHeight < 0) return;
        sendButton.setEnabled(false);
        new Thread(new Runnable() {
            @Override public void run() {
            try {
                publish(sn, ip, username, password, text, fontFamily, selectedFontSize,
                        requestedWidth, requestedHeight);
                report(getString(R.string.send_success));
            } catch (Exception | LinkageError e) {
                Log.e(TAG, "Text program failed", e);
                report(getString(R.string.send_failed, e.getMessage()) + "\n" + Log.getStackTraceString(e));
            } finally {
                mainHandler.post(new Runnable() {
                    @Override public void run() { sendButton.setEnabled(true); }
                });
            }
            }
        }, "novastar-text-send").start();
    }

    /** Zero means the corresponding dimension should be read from the terminal. */
    private int optionalDimension(EditText input) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) return 0;
        try {
            int dimension = Integer.parseInt(value);
            if (dimension > 0) return dimension;
        } catch (NumberFormatException ignored) {
            // The number is too large, or is not a decimal integer.
        }
        input.setError(getString(R.string.error_program_dimension));
        input.requestFocus();
        return -1;
    }

    private void publish(String sn, String ip, String username, String password, String text,
                         String fontFamily, int selectedFontSize,
                         int requestedWidth, int requestedHeight) throws Exception {
        sdkDir = getExternalFilesDir("novastar");
        if (sdkDir == null) sdkDir = new File(getFilesDir(), "novastar");
        if (!sdkDir.exists() && !sdkDir.mkdirs()) throw new Exception("Cannot create SDK directory");
        report("SDK directory: " + sdkDir.getAbsolutePath());
        if (core == null) {
            report("Initializing SDK");
            ViplexCore loaded = Native.loadLibrary("viplexcore", ViplexCore.class);
            loaded.nvSetDevLang("Java");
            report("nvSetDevLang(Java)");
            loaded.nvSetPlatform("Android");
            report("nvSetPlatform(Android)");
            int initCode = loaded.nvInit(sdkDir.getAbsolutePath(),
                    "{\"company\":\"NovaStar\",\"phone\":\"029-68216000\",\"email\":\"hr@novastar.tech\"}");
            if (initCode != 0) throw new Exception("nvInit failed: " + initCode);
            report("nvInit code=" + initCode);
            core = loaded;
        }

        report("Searching terminals for SN " + sn);
        DiscoveredTerminal discovered = searchTerminal(sn);
        if (discovered != null)
            report("Discovered tcpPort=" + discovered.tcpPort + " (SDK login has no port parameter)");

        report("Logging in with user-input credentials");
        final String loginRequest = TextProgramPayload.login(sn, ip, username, password).toString();
        report("nvLoginAsync request=" + loginRequest);
        Result login = await("nvLoginAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvLoginAsync(loginRequest, cb); }
        }, false);
        JSONObject loginData = new JSONObject(login.data);
        if (!loginData.optBoolean("logined") ||
                !(loginData.optBoolean("validation") || loginData.optBoolean("validition")))
            throw new Exception("nvLoginAsync did not confirm login: " + login.data);

        DisplayDimensions dimensions = null;
        if (requestedWidth == 0 || requestedHeight == 0) {
            try {
                report("Reading display resolution");
                final String displayRequest = TextProgramPayload.displayInfo(sn).toString();
                Result display = await("nvGetDisplayInfoAsync", STEP_TIMEOUT_MS, new SdkCall() {
                    @Override public void invoke(ViplexCore.CallBack cb) { core.nvGetDisplayInfoAsync(displayRequest, cb); }
                }, false);
                dimensions = DisplayDimensions.parse(display.data);
            } catch (Exception e) {
                report("Display info unavailable: " + e.getMessage());
            }
        }
        int screenWidth = requestedWidth > 0 ? requestedWidth
                : dimensions != null ? dimensions.width : discovered == null ? 0 : discovered.width;
        int screenHeight = requestedHeight > 0 ? requestedHeight
                : dimensions != null ? dimensions.height : discovered == null ? 0 : discovered.height;
        if (screenWidth <= 0 || screenHeight <= 0)
            throw new Exception("Cannot determine screen resolution from display info or search result");
        int fontSize = selectedFontSize > 0 ? selectedFontSize : Math.max(8, Math.min(60, screenHeight * 2 / 5));
        report("Program resolution=" + screenWidth + "x" + screenHeight
                + ", rotation=" + (dimensions == null ? "unknown" : dimensions.rotation)
                + ", font=" + fontFamily + ", fontSize=" + fontSize);
        if (selectedFontSize > 0 && selectedFontSize >= screenHeight)
            report("Selected font size is at least the display height; glyphs may be clipped");

        report("Creating program");
        final String programName = "text_" + System.currentTimeMillis();
        final String createRequest = TextProgramPayload.create(programName, screenWidth, screenHeight).toString();
        report("nvCreateProgramAsync request=" + createRequest);
        Result created = await("nvCreateProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvCreateProgramAsync(createRequest, cb); }
        }, false);
        int programId = new JSONObject(created.data).getJSONObject("onSuccess").getInt("programID");
        if (programId <= 0) throw new Exception("Invalid programID: " + programId);

        report("Setting text page (program " + programId + ")");
        final String pageRequest = TextProgramPayload.page(programId, text, fontFamily, fontSize).toString();
        report("nvSetPageProgramAsync request=" + pageRequest);
        await("nvSetPageProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvSetPageProgramAsync(pageRequest, cb); }
        }, false);
        report("Generating program");
        final String makeRequest = TextProgramPayload.make(programId, sdkDir).toString();
        report("nvMakeProgramAsync request=" + makeRequest);
        await("nvMakeProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvMakeProgramAsync(makeRequest, cb); }
        }, false);

        File programPath = new File(sdkDir, "program" + programId);
        if (!programPath.exists()) throw new Exception("Generated program missing: " + programPath);
        report("Transferring program");
        final String transferRequest = TextProgramPayload.transfer(sn, programName, programPath).toString();
        report("nvStartTransferProgramAsync request=" + transferRequest);
        await("nvStartTransferProgramAsync", TRANSFER_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvStartTransferProgramAsync(transferRequest, cb); }
        }, true);
        report("节目下发完成，正在回读设备播放状态");
        verifyPlayback(sn, programName);
    }

    private void verifyPlayback(final String sn, String programName) throws Exception {
        final String infoRequest = TextProgramPayload.displayInfo(sn).toString();
        boolean startRequested = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            Result info = await("nvGetProgramInfoAsync", STEP_TIMEOUT_MS, new SdkCall() {
                @Override public void invoke(ViplexCore.CallBack cb) {
                    core.nvGetProgramInfoAsync(infoRequest, cb);
                }
            }, false);
            ProgramPlayback playback = ProgramPlayback.find(info.data, programName);
            if (playback != null) {
                report("节目 " + programName + "：" + playback.statusText()
                        + "（statusCode=" + playback.statusCode + "）");
                if (playback.statusCode == 1) return;
                if (!startRequested && (playback.statusCode == 0 || playback.statusCode == 2)) {
                    final String startRequest = TextProgramPayload.startPlay(sn, playback.identifier).toString();
                    report("节目未在播放，调用 nvStartPlayAsync 启动清单");
                    await("nvStartPlayAsync", STEP_TIMEOUT_MS, new SdkCall() {
                        @Override public void invoke(ViplexCore.CallBack cb) {
                            core.nvStartPlayAsync(startRequest, cb);
                        }
                    }, false);
                    startRequested = true;
                }
            } else {
                report("清单中尚未找到刚下发的节目 " + programName);
            }
            Thread.sleep(1000);
        }
        throw new Exception("节目已下发，但未确认正在播放：" + programName);
    }

    private DiscoveredTerminal searchTerminal(final String sn) throws InterruptedException {
        final CountDownLatch finished = new CountDownLatch(1);
        final DiscoveredTerminal[] match = new DiscoveredTerminal[1];
        searchCallback = new ViplexCore.CallBack() {
            @Override public void dataCallBack(int code, String data) {
                String safeData = data == null ? "" : data;
                report("nvSearchTerminalAsync callback code=" + code + " data=" + safeData);
                if (code == 0) {
                    DiscoveredTerminal terminal = DiscoveredTerminal.find(safeData, sn);
                    if (terminal != null) {
                        match[0] = terminal;
                        finished.countDown();
                    }
                } else {
                    finished.countDown();
                }
            }
        };
        try {
            core.nvSearchTerminalAsync(searchCallback);
            if (!finished.await(SEARCH_TIMEOUT_MS, TimeUnit.MILLISECONDS))
                report("Search finished without matching SN; using configured login values");
        } catch (RuntimeException e) {
            report("Search failed: " + e.getMessage() + "; using configured login values");
        }
        if (match[0] != null) report("Matched terminal SN=" + match[0].sn + " IP=" + match[0].ip);
        return match[0];
    }

    private Result await(final String operation, long timeoutMs, SdkCall call, final boolean transfer) throws Exception {
        final CountDownLatch done = new CountDownLatch(1);
        final Result[] result = new Result[1];
        ViplexCore.CallBack callback = new ViplexCore.CallBack() {
            @Override public void dataCallBack(int code, String data) {
            String safeData = data == null ? "" : data;
            report(operation + " callback code=" + code + " data=" + safeData);
            if (transfer && !isTransferTerminal(code, safeData)) return;
            result[0] = new Result(code, safeData);
            done.countDown();
            }
        };
        call.invoke(callback);
        if (!done.await(timeoutMs, TimeUnit.MILLISECONDS)) throw new Exception(operation + " timed out");
        if (result[0].code != 0) throw new Exception(operation + " failed: " + result[0].code + " " + result[0].data);
        report(operation + " succeeded");
        return result[0];
    }

    /**
     * Terminal-event filter for program transfer callbacks, per TaurusSDK doc (发布节目):
     * - 65362 (MEDIA_UPLOADING): upload progress, not terminal
     * - code 2: terminal program-record event (planlist.json path etc.), not an error
     * - code 0: terminal only when data carries the completion marker; firmware builds
     *   differ: the doc shows "all media update finish" (with spaces), some send "Success"
     * - any other non-zero code (2062/2027/2048/65367...): terminal failure
     */
    private static boolean isTransferTerminal(int code, String data) {
        if (code == 65362) return false;
        if (code == 2) return false;
        if (code != 0) return true;
        String d = data.trim();
        return d.replace(" ", "").contains("allmediaupdatefinish") || d.equalsIgnoreCase("success");
    }

    private void report(final String message) {
        final String line = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date())
                + " " + message + "\n";
        Log.i(TAG, line);
        mainHandler.post(new Runnable() {
            @Override public void run() {
                boolean followBottom = !touchingLog && logScroll.getScrollY() >= Math.max(0,
                        statusView.getHeight() - logScroll.getHeight() - 8);
                statusView.append(line);
                if (followBottom) {
                    logScroll.post(new Runnable() {
                        @Override public void run() { logScroll.fullScroll(ScrollView.FOCUS_DOWN); }
                    });
                }
            }
        });
    }
}
