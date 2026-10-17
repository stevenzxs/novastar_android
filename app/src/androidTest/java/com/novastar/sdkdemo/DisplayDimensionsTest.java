package com.novastar.sdkdemo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DisplayDimensionsTest {
    @Test public void readsDisplayInfoTask() {
        DisplayDimensions found = DisplayDimensions.parse("{\"taskArray\":["
                + "{\"type\":\"OTHER\",\"data\":{\"width\":999,\"height\":999}},"
                + "{\"type\":\"DISPLAY_INFO\",\"data\":{\"width\":256,\"height\":64,\"rotation\":90}}]}");
        assertNotNull(found);
        assertEquals(256, found.width);
        assertEquals(64, found.height);
        assertEquals(90, found.rotation);
    }

    @Test public void rejectsMissingResolution() {
        assertNull(DisplayDimensions.parse("{\"taskArray\":[{\"type\":\"DISPLAY_INFO\",\"data\":{\"width\":0,\"height\":64}}]}"));
    }
}
