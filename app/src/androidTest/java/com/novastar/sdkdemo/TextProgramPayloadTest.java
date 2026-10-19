package com.novastar.sdkdemo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
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
        assertFalse(container.has("layout"));
        JSONObject widget = container.getJSONObject("contents").getJSONArray("widgets").getJSONObject(0);
        assertEquals("ARCH_TEXT", widget.getString("type"));
        assertFalse(widget.has("layout"));
        assertEquals(text, widget.getJSONObject("metadata").getJSONObject("content")
                .getJSONArray("paragraphs").getJSONObject(0).getJSONArray("lines").getJSONObject(0)
                .getJSONArray("segs").getJSONObject(0).getString("content"));
        JSONObject font = widget.getJSONObject("metadata").getJSONObject("content")
                .getJSONArray("textAttributes").getJSONObject(0).getJSONObject("attributes")
                .getJSONObject("font");
        assertEquals("SimHei", font.getJSONArray("family").getString(0));
        assertEquals(24, font.getInt("size"));
        JSONObject program = TextProgramPayload.create("text_123", 256, 64);
        assertEquals("text_123", program.getString("name"));
        assertEquals(256, program.getInt("width"));
        assertEquals(64, program.getInt("height"));
        // Real-device verification: without winInfo the terminal renders nothing.
        // winInfo covers the full program area (full screen on the terminal).
        JSONObject winInfo = program.getJSONObject("winInfo");
        assertEquals(256, winInfo.getInt("width"));
        assertEquals(64, winInfo.getInt("height"));
        assertEquals(0, winInfo.getInt("left"));
        assertEquals(0, winInfo.getInt("top"));
        assertEquals(7, TextProgramPayload.make(7, new File("/tmp/sdk"), null, null).getInt("programID"));
        JSONObject transfer = TextProgramPayload.transfer("SN", "text_123", new File("/tmp/sdk/program7"), null, null);
        assertEquals("/tmp/sdk/program7", transfer.getJSONObject("sendProgramFilePaths").getString("programPath"));
        assertEquals("text_123", transfer.getString("programName"));
        assertTrue(transfer.getBoolean("startPlayAfterTransferred"));
        assertFalse(transfer.getBoolean("insertPlay"));
        assertEquals("abc", TextProgramPayload.startPlay("SN", "abc").getString("identifier"));
    }

    @Test public void loginRequestIncludesEnteredPassword() throws Exception {
        JSONObject login = TextProgramPayload.login("SN123", "192.0.2.1", "admin", "p\"ass");
        JSONObject parsed = new JSONObject(login.toString());
        assertEquals("SN123", parsed.getString("sn"));
        assertEquals("192.0.2.1", parsed.getString("ip"));
        assertEquals("p\"ass", parsed.getString("password"));
    }
}
