package com.novastar.sdkdemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Extracts one matching terminal from the shapes returned by ViPlexCore search callbacks. */
final class DiscoveredTerminal {
    final String sn;
    final String ip;
    final String username;
    final String password;
    final int tcpPort;
    final int width;
    final int height;

    private DiscoveredTerminal(JSONObject value) {
        sn = string(value.opt("sn"));
        ip = string(value.opt("ip"));
        username = firstString(value.opt("username"));
        password = rawString(value.opt("password"));
        tcpPort = value.optInt("tcpPort", 0);
        width = value.optInt("width", 0);
        height = value.optInt("height", 0);
    }

    static DiscoveredTerminal find(String data, String targetSn) {
        if (data == null || data.trim().isEmpty()) return null;
        try {
            return findValue(new JSONTokener(data).nextValue(), targetSn, 0);
        } catch (JSONException e) {
            return null;
        }
    }

    private static DiscoveredTerminal findValue(Object value, String targetSn, int depth) throws JSONException {
        if (depth > 4) return null;
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                DiscoveredTerminal found = findValue(array.opt(i), targetSn, depth + 1);
                if (found != null) return found;
            }
        } else if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (targetSn.equals(string(object.opt("sn")))) return new DiscoveredTerminal(object);
            String[] wrappers = {"onSuccess", "result", "data", "terminals", "devices"};
            for (String key : wrappers) {
                DiscoveredTerminal found = findValue(object.opt(key), targetSn, depth + 1);
                if (found != null) return found;
            }
        } else if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("{") || text.startsWith("["))
                return findValue(new JSONTokener(text).nextValue(), targetSn, depth + 1);
        }
        return null;
    }

    private static String firstString(Object value) {
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                String candidate = string(array.opt(i));
                if (!candidate.isEmpty()) return candidate;
            }
            return "";
        }
        return string(value);
    }

    private static String string(Object value) {
        return rawString(value).trim();
    }

    private static String rawString(Object value) {
        if (value == null || value == JSONObject.NULL || value instanceof JSONObject || value instanceof JSONArray)
            return "";
        return String.valueOf(value);
    }
}
