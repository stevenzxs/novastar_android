package com.novastar.sdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String TAG = "NovaStarTextDemo";
    private static final int MODE_TEXT = 0;
    private static final int MODE_DOC = 1;
    private static final int MODE_PICTURE = 2;
    private static final String PREFS_FORM = "form_state";
    private static final String KEY_SN = "sn";
    private static final String KEY_IP = "ip";
    private static final String KEY_USER = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TEXT = "text";
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
    private Button sendDocButton;
    private Button sendPictureButton;
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
            @Override public void onClick(View v) { sendTextProgram(MODE_TEXT); }
        });
        sendDocButton = findViewById(R.id.send_doc_button);
        sendDocButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendTextProgram(MODE_DOC); }
        });
        sendPictureButton = findViewById(R.id.send_picture_button);
        sendPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendTextProgram(MODE_PICTURE); }
        });
        Button logUp = findViewById(R.id.log_scroll_up);
        logUp.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                logScroll.smoothScrollBy(0, -logScroll.getHeight());
            }
        });
        Button logDown = findViewById(R.id.log_scroll_down);
        logDown.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                logScroll.smoothScrollBy(0, logScroll.getHeight());
            }
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
        restoreForm();
    }

    /** 重启应用后回填上次发送时保存的表单内容（密码以明文存于本应用私有目录）。 */
    private void restoreForm() {
        SharedPreferences prefs = getSharedPreferences(PREFS_FORM, Context.MODE_PRIVATE);
        snInput.setText(prefs.getString(KEY_SN, ""));
        ipInput.setText(prefs.getString(KEY_IP, ""));
        userInput.setText(prefs.getString(KEY_USER, ""));
        passwordInput.setText(prefs.getString(KEY_PASSWORD, ""));
        textInput.setText(prefs.getString(KEY_TEXT, ""));
    }

    private void saveForm(String sn, String ip, String username, String password, String text) {
        getSharedPreferences(PREFS_FORM, Context.MODE_PRIVATE).edit()
                .putString(KEY_SN, sn)
                .putString(KEY_IP, ip)
                .putString(KEY_USER, username)
                .putString(KEY_PASSWORD, password)
                .putString(KEY_TEXT, text)
                .apply();
    }

    private void sendTextProgram(final int mode) {
        final String sn = snInput.getText().toString().trim();
        final String ip = ipInput.getText().toString().trim();
        final String username = userInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();
        final String text = textInput.getText().toString();
        final String fontFamily = fontFamilySpinner.getSelectedItemPosition() == 0
                ? "Helvetica" : fontFamilySpinner.getSelectedItem().toString();
        final int selectedFontSize = fontSizeSpinner.getSelectedItemPosition() == 0
                ? 0 : Integer.parseInt(fontSizeSpinner.getSelectedItem().toString());
        saveForm(sn, ip, username, password, text);
        statusView.setText("");
        report((mode == MODE_DOC ? "Doc-example send clicked"
                : mode == MODE_PICTURE ? "Picture send clicked" : "Send button clicked")
                + ": font=" + fontFamily + ", selectedFontSize="
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
        sendDocButton.setEnabled(false);
        sendPictureButton.setEnabled(false);
        new Thread(new Runnable() {
            @Override public void run() {
            try {
                publish(sn, ip, username, password, text, fontFamily, selectedFontSize,
                        requestedWidth, requestedHeight, mode);
                report(getString(R.string.send_success));
            } catch (Exception | LinkageError e) {
                Log.e(TAG, "Text program failed", e);
                report(getString(R.string.send_failed, e.getMessage()) + "\n" + Log.getStackTraceString(e));
            } finally {
                mainHandler.post(new Runnable() {
                    @Override public void run() {
                        sendButton.setEnabled(true);
                        sendDocButton.setEnabled(true);
                        sendPictureButton.setEnabled(true);
                    }
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
                         int requestedWidth, int requestedHeight, int mode) throws Exception {
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

        login(sn, ip, username, password);

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

        // 文字转图模式：本地把文字渲染成透明底 PNG，设备端按 PICTURE 显示
        String mediaOldPath = null;
        String mediaNewPath = null;
        if (mode == MODE_PICTURE) {
            report("Rendering text to PNG (" + screenWidth + "x" + screenHeight + ")");
            File png = renderTextToPng(text, fontFamily, fontSize, screenWidth, screenHeight);
            mediaOldPath = png.getAbsolutePath();
            mediaNewPath = md5Hex(png) + ".png";
            report("Text PNG: " + mediaOldPath + " -> " + mediaNewPath + " (" + png.length() + " bytes)");
        }

        report("Creating program");
        final String programName = "text_" + System.currentTimeMillis();
        final String createRequest = TextProgramPayload.create(programName, screenWidth, screenHeight).toString();
        report("nvCreateProgramAsync request=" + createRequest);
        Result created = await("nvCreateProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvCreateProgramAsync(createRequest, cb); }
        }, false);
        int programId = new JSONObject(created.data).getJSONObject("onSuccess").getInt("programID");
        if (programId <= 0) throw new Exception("Invalid programID: " + programId);

        report("Setting text page (program " + programId + ")"
                + (mode == MODE_DOC ? " per doc example 4.30.1.4"
                : mode == MODE_PICTURE ? " as text PNG" : ""));
        final String pageRequest = (mode == MODE_DOC
                ? TextProgramPayload.pageDocExample(programId)
                : mode == MODE_PICTURE
                    ? TextProgramPayload.pagePicture(programId, mediaNewPath, mediaOldPath)
                    : TextProgramPayload.page(programId, text, fontFamily, fontSize)).toString();
        report("nvSetPageProgramAsync request=" + pageRequest);
        await("nvSetPageProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvSetPageProgramAsync(pageRequest, cb); }
        }, false);
        report("Generating program");
        final String makeRequest = TextProgramPayload.make(programId, sdkDir, mediaOldPath, mediaNewPath).toString();
        report("nvMakeProgramAsync request=" + makeRequest);
        await("nvMakeProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvMakeProgramAsync(makeRequest, cb); }
        }, false);

        File programPath = new File(sdkDir, "program" + programId);
        if (!programPath.exists()) throw new Exception("Generated program missing: " + programPath);
        report("Transferring program");
        final String transferRequest = TextProgramPayload.transfer(sn, programName, programPath, mediaOldPath, mediaNewPath).toString();
        report("nvStartTransferProgramAsync request=" + transferRequest);
        await("nvStartTransferProgramAsync", TRANSFER_TIMEOUT_MS, new SdkCall() {
            @Override public void invoke(ViplexCore.CallBack cb) { core.nvStartTransferProgramAsync(transferRequest, cb); }
        }, true);
        queryLocalPrograms();
        report("节目下发完成，正在回读设备播放状态");
        verifyPlayback(sn, ip, username, password, programName);
    }

    private void login(String sn, String ip, String username, String password) throws Exception {
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
    }

    /** 发送成功后查询 SDK 本地节目库，仅打印返回值，失败不影响下发流程。 */
    private void queryLocalPrograms() {
        try {
            Result local = await("nvGetProgramAsync", STEP_TIMEOUT_MS, new SdkCall() {
                @Override public void invoke(ViplexCore.CallBack cb) { core.nvGetProgramAsync("", cb); }
            }, false);
            report("本地节目查询返回值=" + local.data);
        } catch (Exception e) {
            report("本地节目查询失败（不影响下发）: " + e.getMessage());
        }
    }

    /** 把文字绘制到不透明黑底 PNG（水平/垂直居中，支持多行），返回 PNG 文件。 */
    private File renderTextToPng(String text, String fontFamily, int fontSize,
                                 int width, int height) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // 不透明黑底：mercury/T20 老播放器可能无法合成 alpha 通道，透明底 PNG 会被丢弃成黑屏
        canvas.drawColor(0xFF000000);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFF0000);
        paint.setTextSize(fontSize);
        Typeface typeface = Typeface.create(fontFamily, Typeface.NORMAL);
        paint.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        paint.setTextAlign(Paint.Align.CENTER);
        String[] lines = text.split("\n", -1);
        float lineHeight = fontSize * 1.2f;
        float blockHeight = lineHeight * lines.length;
        float centerOffset = -(paint.descent() + paint.ascent()) / 2f;
        for (int i = 0; i < lines.length; i++) {
            float lineCenterY = height / 2f - blockHeight / 2f + lineHeight * (i + 0.5f);
            canvas.drawText(lines[i], width / 2f, lineCenterY + centerOffset, paint);
        }
        File file = new File(sdkDir, "text_" + System.currentTimeMillis() + ".png");
        FileOutputStream out = new FileOutputStream(file);
        try {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                throw new Exception("PNG compression failed");
        } finally {
            out.close();
            bitmap.recycle();
        }
        return file;
    }

    /** 文件内容 MD5 小写十六进制，作为设备侧文件名（与官方发布包规则一致）。 */
    private static String md5Hex(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
        } finally {
            in.close();
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private void verifyPlayback(String sn, String ip, String username, String password,
                                String programName) throws Exception {
        final String infoRequest = TextProgramPayload.displayInfo(sn).toString();
        boolean startRequested = false;
        int reloginAttempts = 0;
        for (int attempt = 0; attempt < 10; attempt++) {
            Result info;
            try {
                info = await("nvGetProgramInfoAsync", STEP_TIMEOUT_MS, new SdkCall() {
                    @Override public void invoke(ViplexCore.CallBack cb) {
                        core.nvGetProgramInfoAsync(infoRequest, cb);
                    }
                }, false);
            } catch (Exception e) {
                // 节目传输完成后设备会重建播放服务，TCP 会话失效（65350 not logined），
                // 重新登录后继续回读，而不是让整个发送失败。
                if (reloginAttempts < 3 && e.getMessage() != null
                        && e.getMessage().contains("not logined")) {
                    reloginAttempts++;
                    report("会话已失效（下发后设备重建连接），重新登录（第 " + reloginAttempts + " 次）");
                    try {
                        login(sn, ip, username, password);
                    } catch (Exception loginError) {
                        report("重新登录失败: " + loginError.getMessage());
                    }
                    continue;
                }
                throw e;
            }
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
