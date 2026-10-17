package com.novastar.sdkdemo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DiscoveredTerminalTest {
    @Test public void selectsMatchingSnFromSearchArray() {
        String data = "[{\"sn\":\"OTHER\",\"ip\":\"192.168.3.8\"},"
                + "{\"sn\":\"TARGET\",\"ip\":\"192.168.3.7\",\"tcpPort\":16603,"
                + "\"username\":[\"\",\"admin\"],\"password\":\" secret \"}]";
        DiscoveredTerminal found = DiscoveredTerminal.find(data, "TARGET");
        assertNotNull(found);
        assertEquals("192.168.3.7", found.ip);
        assertEquals(16603, found.tcpPort);
        assertEquals("admin", found.username);
        assertEquals(" secret ", found.password);
    }

    @Test public void ignoresOtherSnAndAcceptsWrappedResult() {
        assertNull(DiscoveredTerminal.find("[{\"sn\":\"OTHER\"}]", "TARGET"));
        DiscoveredTerminal found = DiscoveredTerminal.find(
                "{\"onSuccess\":{\"data\":\"{\\\"sn\\\":\\\"TARGET\\\",\\\"ip\\\":\\\"192.168.3.7\\\"}\"}}",
                "TARGET");
        assertNotNull(found);
        assertEquals("192.168.3.7", found.ip);
        assertEquals("", found.username);
        assertEquals("", found.password);
    }
}
