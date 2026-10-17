package com.novastar.sdkdemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/** JSON requests for the ViPlexCore text-program API. */
final class TextProgramPayload {
    private TextProgramPayload() {}

    private static JSONObject obj(Object... pairs) {
        JSONObject value = new JSONObject();
        try {
            for (int i = 0; i < pairs.length; i += 2) value.put((String) pairs[i], pairs[i + 1]);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid program data", e);
        }
        return value;
    }

    private static JSONArray array(Object... values) {
        JSONArray result = new JSONArray();
        for (Object value : values) result.put(value);
        return result;
    }

    static JSONObject login(String sn, String ip, String username, String password) {
        JSONObject value = obj("sn", sn, "username", username.isEmpty() ? "admin" : username,
                "rememberPwd", 1, "password", password.isEmpty() ? "123456" : password,
                "loginType", 0);
        if (!ip.isEmpty()) {
            try { value.put("ip", ip); } catch (JSONException e) { throw new IllegalArgumentException(e); }
        }
        return value;
    }

    static JSONObject displayInfo(String sn) {
        return obj("sn", sn);
    }

    /** The terminal needs an explicit play window to render the program. */
    static JSONObject create(int width, int height) {
        return obj("name", "program1", "width", width, "height", height,
                "winInfo", obj("height", height, "width", width,
                        "left", 0, "top", 0, "zindex", 0, "index", 0));
    }

    static JSONObject page(int programId, String text, String fontFamily, int fontSize) {
        JSONObject font = obj("family", array(fontFamily), "isUnderline", false, "size", fontSize, "style", "NORMAL");
        JSONObject attributes = obj("font", font, "letterSpacing", 0, "textColor", "#ffff0000");
        JSONObject content = obj("autoPaging", true,
                "backgroundMusic", obj("duration", 0, "isTextSync", false),
                "displayStyle", obj("scrollAttributes", obj("effects", obj("animation", "MARQUEE_LEFT", "speed", 3)), "type", "STATIC"),
                "paragraphs", array(obj("backgroundColor", "#ff000000", "horizontalAlignment", "CENTER",
                        "letterSpacing", 0, "lineSpacing", 0,
                        "lines", array(obj("segs", array(obj("content", text)))), "verticalAlignment", "CENTER")),
                "textAttributes", array(obj("backgroundColor", "#ff000000", "attributes", attributes)));
        JSONObject widget = obj("constraints", array(obj("cron", array("0 0 0 ? * 1,2,3,4,5,6,7"),
                        "endTime", "4017-12-30T23:59:59Z+8:00", "startTime", "1970-01-01T00:00:00Z+8:00")),
                "duration", 10000, "metadata", obj("content", content), "name", "文本", "type", "ARCH_TEXT");
        JSONObject container = obj("contents", obj("widgets", array(widget)), "id", 1,
                "name", "widgetContainers1");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("name", "page1", "widgetContainers", array(container)));
    }

    static JSONObject make(int programId, File sdkDir) {
        return obj("programID", programId, "outPutPath", sdkDir.getAbsolutePath(), "mediasPath", array());
    }

    static JSONObject transfer(String sn, int programId, File programPath) {
        return obj("sn", sn, "iconPath", "", "iconName", "",
                "sendProgramFilePaths", obj("programPath", programPath.getAbsolutePath(), "mediasPath", obj()),
                "programName", "program1", "deviceIdentifier", "SDKDemo",
                "startPlayAfterTransferred", true, "insertPlay", true);
    }
}
