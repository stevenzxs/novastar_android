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
        JSONObject value = obj("sn", sn, "username", username,
                "rememberPwd", 1, "password", password,
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
    static JSONObject create(String name, int width, int height) {
        return obj("name", name, "width", width, "height", height,
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

    /**
     * TaurusSDK 4.30.1.4 文本节目官方示例的原样复刻（不带 layout、段落背景透明、
     * 示例固定文字、Helvetica 60），仅 programID 取创建接口的返回值。用于和
     * {@link #page(int, String, String, int)} 做对照测试。
     */
    static JSONObject pageDocExample(int programId) {
        JSONObject font = obj("family", array("Helvetica"), "isUnderline", false, "size", 60, "style", "NORMAL");
        JSONObject attributes = obj("font", font, "letterSpacing", 0, "textColor", "#ffff0000");
        JSONObject content = obj("autoPaging", true,
                "backgroundMusic", obj("duration", 0, "isTextSync", false),
                "displayStyle", obj("scrollAttributes", obj("effects", obj("animation", "MARQUEE_LEFT", "speed", 3)), "type", "STATIC"),
                "paragraphs", array(obj("backgroundColor", "#00000000", "horizontalAlignment", "CENTER",
                        "letterSpacing", 0, "lineSpacing", 0,
                        "lines", array(obj("segs", array(obj("content", "中国制造，惠及全球！测试测测试是是是是是是试测")))), "verticalAlignment", "CENTER")),
                "textAttributes", array(obj("backgroundColor", "#ff000000", "attributes", attributes)));
        JSONObject widget = obj("constraints", array(obj("cron", array("0 0 0 ? * 1,2,3,4,5,6,7"),
                        "endTime", "4017-12-30T23:59:59Z+8:00", "startTime", "1970-01-01T00:00:00Z+8:00")),
                "duration", 10000, "metadata", obj("content", content), "name", "文本", "type", "ARCH_TEXT");
        JSONObject container = obj("contents", obj("widgets", array(widget)), "id", 1,
                "name", "widgetContainers1");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("name", "节目 01", "widgetContainers", array(container)));
    }

    /**
     * 文字转图方案（与 ViPlex Express 的「单行文本」一致）：文字已在本地渲染成 PNG，
     * 设备端只按 PICTURE 控件显示图片，绕开 T20/mercury 固件 ARCH_TEXT 渲染缺陷。
     * dataSource 为设备侧文件名（文件内容的 md5 + .png）。
     *
     * 注意（本地 make 实验实锤）：name 必须等于 originalDataSource 的文件名，
     * 否则老 SDK 会把 dataSource 的 MD5 剥掉（设备端变成找 ".png" → 黑屏）。
     * originalDataSource 必须传 "./文件名" 形式，不能传绝对路径。
     */
    static JSONObject pagePicture(int programId, String dataSource, String originalName) {
        JSONObject widget = obj("constraints", array(obj("cron", array("0 0 0 ? * 1,2,3,4,5,6,7"),
                        "endTime", "4017-12-30T23:59:59Z+8:00", "startTime", "1970-01-01T00:00:00Z+8:00")),
                "duration", 10000, "dataSource", dataSource, "displayRatio", "FULL",
                "type", "PICTURE", "name", originalName, "originalDataSource", "./" + originalName);
        JSONObject container = obj("contents", obj("widgets", array(widget)), "id", 1,
                "name", "widgetContainers1");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("name", "page1", "widgetContainers", array(container)));
    }

    /** TaurusSDK 4.30.1.1 图片示例的原样复刻（name 用原始文件名、无 displayRatio、duration 5000）。 */
    static JSONObject pagePictureDocExample(int programId, String dataSource, String originalName) {
        JSONObject widget = obj("constraints", array(obj("cron", array("0 0 0 ? * 1,2,3,4,5,6,7"),
                        "endTime", "4017-12-30T23:59:59Z+8:00", "startTime", "1970-01-01T00:00:00Z+8:00")),
                "duration", 5000, "dataSource", dataSource, "type", "PICTURE",
                "name", originalName, "originalDataSource", "./" + originalName);
        JSONObject container = obj("contents", obj("widgets", array(widget)), "id", 1,
                "name", "widgetContainers1");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("name", "节目 01", "widgetContainers", array(container)));
    }

    /** TaurusSDK 4.30.2.6 简单天气预报「编辑复杂节目」示例的原样复刻（仅 programID 动态）。 */
    static JSONObject pageWeatherComplex(int programId) {
        JSONObject constraints = obj("cron", array("0 0 0 ? * 1,2,3,4,5,6,7"),
                "endTime", "4017-12-30T23:59:59Z 8:00", "startTime", "1970-01-01T00:00:00Z 8:00");
        JSONObject borderEffects = obj("animation", "CLOCK_WISE", "headTailSpacing", "10",
                "isHeadTail", false, "speed", 3, "speedByPixelEnable", false);
        JSONObject metadata = obj(
                "area", obj("city", "西安", "cityCode", "CN101110101", "country", "中国",
                        "county", "西安", "province", "陕西省"),
                "cityNewCode", "CN101110101",
                "effects", obj("animation", "NONE", "headTailSpacing", "100",
                        "isHeadTail", true, "speed", 3.0, "speedByPixelEnable", false),
                "isShowInOneLine", false,
                "isShowUnit", true,
                "language", "zh-cn",
                "refreshPeriod", 300000,
                "regular", "天气:$w\n 温度:$t\n 风力:$b 级\n 当前温度:$c\n 湿度:$h",
                "temperatureUnit", "Celsius",
                "textAttributes", obj(
                        "ShadowDx", 2, "ShadowRadius", 10, "backgroundColor", "#000",
                        "font", obj("family", array("Times", "Georia", "New York"),
                                "isUnderline", false, "size", 20, "style", "Bold"),
                        "letterSpacing", 0,
                        "shadowColor", "#00ff00", "shadowDy", 0, "shadowEnable", false,
                        "strokeEnable", false, "strokeWidth", -2147483648,
                        "textColor", "#ffff0000"),
                "weatherSource", 1,
                "windSpeedUnit", "m",
                "windType", "wind_sc");
        JSONObject widget = obj(
                "backgroundColor", "#00000000",
                "backgroundDrawable", "",
                "backgroundMusic", "",
                "border", obj("backgroundColor", "#FF000000",
                        "borderThickness", "0px,0px,0px,0px", "cornerRadius", "2%",
                        "effects", borderEffects, "name", "border", "style", 0),
                "constraints", array(constraints),
                "dataSource", "",
                "displayRatio", "FULL",
                "duration", 5000,
                "enable", true,
                "id", 1,
                "inAnimation", obj("duration", 1000, "type", 0),
                "layout", obj("height", "100%", "width", "100%", "x", "0", "y", "0"),
                "metadata", metadata,
                "name", "天气",
                "originalDataSource", "",
                "outAnimation", obj("duration", 0, "type", 0),
                "repeatCount", 1,
                "type", "SIMPLE_WEATHER",
                "zOrder", 0);
        JSONObject container = obj(
                "audioGroup", "",
                "backgroundColor", "#00000000",
                "backgroundDrawable", "",
                "contents", obj("widgets", array(widget)),
                "enable", true,
                "id", 1,
                "itemsSource", "",
                "layout", obj("height", "100.00%", "width", "100.00%", "x", "0.00%", "y", "0.00%"),
                "name", "widgetContainers1",
                "pickCount", 0,
                "pickPolicy", "ORDER",
                "zOrder", 0);
        JSONObject scene = obj(
                "backgroundColor", "#00000000",
                "backgroundDrawable", "",
                "backgroundMusic", "",
                "constraints", array(constraints),
                "duration", 10000,
                "enable", true,
                "id", 1,
                "name", "scene1",
                "page", obj("id", 1, "name", "节目 07",
                        "widgetContainers", array(container),
                        "widgets", array()),
                "repeatCount", 1,
                "rules", "TIMES",
                "type", "PAGE");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("id", 1, "name", "节目 01", "pickPolicy", "ORDER_MODE",
                        "sceneItems", array(scene), "uuid", ""));
    }

    /** TaurusSDK 4.30.2.9 炫彩字媒体「编辑复杂节目」示例的原样复刻（仅 programID 动态）。 */
    static JSONObject pageColorfulComplex(int programId) {
        JSONObject borderEffects = obj("speed", 3, "animation", "CLOCK_WISE",
                "isHeadTail", false, "headTailSpacing", "10", "speedByPixelEnable", false);
        JSONObject border = obj("foregroundColor", "#FF008000", "width", 1, "name", "border",
                "borderThickness", "0px,0px,0px,0px", "cornerRadius", "2%",
                "backgroundColor", "#FF000000", "style", 0, "styleForExpress", 0,
                "effects", borderEffects);
        JSONObject widget = obj(
                "layout", obj("x", "0%", "y", "0%", "width", "100%", "height", "100%"),
                "inAnimation", obj("type", 0, "duration", 1000),
                "outAnimation", obj("type", 0, "duration", 1000),
                "border", border,
                "constraints", array(obj("startTime", "1970-01-01T00:00:00Z 8:00",
                        "endTime", "4012-01-01T23:59:59Z 8:00",
                        "cron", array("0 0 0 ? * 1,2,3,4,5,6,7"))),
                "metadata", obj("content", obj(
                        "content", "请输入文字!",
                        "scrollAttributes", obj("effects", obj(
                                "speed", 5, "animation", "MARQUEE_LEFT",
                                "isHeadTail", false, "headTailSpacing", "10",
                                "speedByPixelEnable", false)),
                        "textAttributes", obj(
                                "effects", obj("type", "GRADIENT_HORIZONTAL",
                                        "colors", array("#FF0000", "#FF00B050", "#FFC00000",
                                                "#FF007BD4", "#FFFFFF00", "#FF00FF"),
                                        "texture", ""),
                                "backgroundColor", "#00FFFFFF",
                                "textColor", "#FF0000",
                                "font", obj("family", array("Arial"), "style", "NORMAL",
                                        "size", 50, "isUnderline", false),
                                "shadowEnable", false,
                                "ShadowRadius", 0,
                                "ShadowDx", 2,
                                "shadowDy", 2,
                                "shadowColor", "#6128bf",
                                "strokeEnable", false,
                                "strokeWidth", 2,
                                "strokeColor", "#6128bf",
                                "letterSpacing", 1))),
                "displayRatio", "FULL",
                "filesize", 1024,
                "zOrder", 1,
                "backgroundColor", "#FF1F8787",
                "backgroundDrawable", "",
                "backgroundMusic", "",
                "name", "炫彩字",
                "enable", true,
                "type", "COLORFUL_TEXT",
                "duration", 10000,
                "repeatCount", 1,
                "id", 100000,
                "uuid", "3dc02eef-b736-4817-b107-ea4c696e61f1");
        JSONObject container = obj(
                "zOrder", 1,
                "layout", obj("x", "0", "y", "0", "width", "1", "height", "1"),
                "border", border,
                "contents", obj("widgets", array(widget), "widgetContainer", array()),
                "PCType", 0,
                "name", "页面 1",
                "enable", true,
                "pickPolicy", "ORDER",
                "id", 200000,
                "uuid", "9e2a4114-6bec-4cb0-8d3e-93652ff0bb50");
        return obj("programID", programId, "pageID", 1,
                "pageInfo", obj("name", "pageNew", "widgetContainers", array(container)));
    }

    static JSONObject make(int programId, File sdkDir, String mediaOldPath, String mediaNewPath) {
        JSONArray medias = new JSONArray();
        if (mediaOldPath != null)
            medias.put(obj("oldPath", mediaOldPath, "newPath", mediaNewPath));
        return obj("programID", programId, "outPutPath", sdkDir.getAbsolutePath(), "mediasPath", medias);
    }

    static JSONObject transfer(String sn, String programName, File programPath,
                               String mediaOldPath, String mediaNewPath) {
        JSONObject medias = new JSONObject();
        if (mediaOldPath != null) {
            try { medias.put(mediaOldPath, mediaNewPath); }
            catch (JSONException e) { throw new IllegalArgumentException(e); }
        }
        return obj("sn", sn, "iconPath", "", "iconName", "",
                "sendProgramFilePaths", obj("programPath", programPath.getAbsolutePath(), "mediasPath", medias),
                "programName", programName, "deviceIdentifier", "SDKDemo",
                "startPlayAfterTransferred", true, "insertPlay", false);
    }

    static JSONObject startPlay(String sn, String identifier) {
        return obj("sn", sn, "identifier", identifier);
    }
}
