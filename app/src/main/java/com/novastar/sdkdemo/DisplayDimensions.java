package com.novastar.sdkdemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Width and height reported by nvGetDisplayInfoAsync. */
final class DisplayDimensions {
    final int width;
    final int height;
    final int rotation;

    private DisplayDimensions(int width, int height, int rotation) {
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    static DisplayDimensions parse(String data) {
        if (data == null || data.trim().isEmpty()) return null;
        try {
            return find(new JSONTokener(data).nextValue(), 0);
        } catch (JSONException e) {
            return null;
        }
    }

    private static DisplayDimensions find(Object value, int depth) throws JSONException {
        if (depth > 4) return null;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            int width = object.optInt("width", 0);
            int height = object.optInt("height", 0);
            if (width > 0 && height > 0)
                return new DisplayDimensions(width, height, object.optInt("rotation", 0));
            Object tasks = object.opt("taskArray");
            if (tasks instanceof JSONArray) {
                JSONArray array = (JSONArray) tasks;
                for (int i = 0; i < array.length(); i++) {
                    JSONObject task = array.optJSONObject(i);
                    if (task != null && "DISPLAY_INFO".equals(task.optString("type"))) {
                        DisplayDimensions found = find(task.opt("data"), depth + 1);
                        if (found != null) return found;
                    }
                }
            }
            String[] wrappers = {"onSuccess", "result", "data"};
            for (String key : wrappers) {
                DisplayDimensions found = find(object.opt(key), depth + 1);
                if (found != null) return found;
            }
        } else if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("{")) return find(new JSONTokener(text).nextValue(), depth + 1);
        }
        return null;
    }
}
