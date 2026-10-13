package com.novastar.sdkdemo;

import android.support.test.runner.AndroidJUnit4;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TextProgramPayloadTest {
    @Test public void textAndProgramIdReachCorrectSdkFields() throws Exception {
        String text = "第一行\n\"第二行\" \\ test";
        JSONObject page = new JSONObject(TextProgramPayload.page(7, text, "SimHei", 24).toString());
        assertEquals(7, page.getInt("programID"));
        JSONObject container = page.getJSONObject("pageInfo").getJSONArray("widgetContainers").getJSONObject(0);
        assertEquals("100%", container.getJSONObject("layout").getString("height"));
        assertEquals("100%", container.getJSONObject("layout").getString("width"));
        JSONObject widget = container.getJSONObject("contents").getJSONArray("widgets").getJSONObject(0);
        assertEquals("ARCH_TEXT", widget.getString("type"));
        assertEquals("100%", widget.getJSONObject("layout").getString("height"));
        assertEquals("100%", widget.getJSONObject("layout").getString("width"));
        assertEquals(text, widget.getJSONObject("metadata").getJSONObject("content")
                .getJSONArray("paragraphs").getJSONObject(0).getJSONArray("lines").getJSONObject(0)
                .getJSONArray("segs").getJSONObject(0).getString("content"));
        JSONObject font = widget.getJSONObject("metadata").getJSONObject("content")
                .getJSONArray("textAttributes").getJSONObject(0).getJSONObject("attributes")
                .getJSONObject("font");
        assertEquals("SimHei", font.getJSONArray("family").getString(0));
        assertEquals(24, font.getInt("size"));
        JSONObject program = TextProgramPayload.create(256, 64);
        assertEquals(256, program.getInt("width"));
        assertEquals(64, program.getInt("height"));
        // Real-device verification: without winInfo the terminal renders nothing.
        JSONObject winInfo = program.getJSONObject("winInfo");
        assertEquals(128, winInfo.getInt("width"));
        assertEquals(32, winInfo.getInt("height"));
        assertEquals(0, winInfo.getInt("left"));
        assertEquals(0, winInfo.getInt("top"));
        assertEquals(7, TextProgramPayload.make(7, new File("/tmp/sdk")).getInt("programID"));
        assertEquals("/tmp/sdk/program7", TextProgramPayload.transfer("SN", 7,
                new File("/tmp/sdk/program7")).getJSONObject("sendProgramFilePaths").getString("programPath"));
    }

    @Test public void loginRequestIncludesEnteredPassword() throws Exception {
        JSONObject login = TextProgramPayload.login("SN123", "192.0.2.1", "admin", "p\"ass");
        JSONObject parsed = new JSONObject(login.toString());
        assertEquals("SN123", parsed.getString("sn"));
        assertEquals("192.0.2.1", parsed.getString("ip"));
        assertEquals("p\"ass", parsed.getString("password"));
    }
}
