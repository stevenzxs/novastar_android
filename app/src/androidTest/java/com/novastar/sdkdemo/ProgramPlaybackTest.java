package com.novastar.sdkdemo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ProgramPlaybackTest {
    @Test public void findsLocalProgramAndItsPlaybackState() throws Exception {
        String data = "{\"programInfos\":["
                + "{\"name\":\"text_123\",\"identifier\":\"cloud\",\"statusCode\":1,\"source\":1},"
                + "{\"name\":\"old\",\"identifier\":\"old\",\"statusCode\":1,\"source\":0},"
                + "{\"name\":\"text_123\",\"identifier\":\"local\",\"statusCode\":0,\"source\":0}]}";
        ProgramPlayback playback = ProgramPlayback.find(data, "text_123");
        assertNotNull(playback);
        assertEquals("local", playback.identifier);
        assertEquals(0, playback.statusCode);
        assertEquals("已停止", playback.statusText());
        assertNull(ProgramPlayback.find(data, "missing"));
    }
}
