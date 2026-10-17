package com.novastar.sdkdemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Playback state returned by nvGetProgramInfoAsync (TaurusSDK, 4.18.1). */
final class ProgramPlayback {
    final String identifier;
    final int statusCode;

    private ProgramPlayback(String identifier, int statusCode) {
        this.identifier = identifier;
        this.statusCode = statusCode;
    }

    String statusText() {
        switch (statusCode) {
            case 0: return "已停止";
            case 1: return "正在播放";
            case 2: return "已暂停";
            default: return "未知状态";
        }
    }

    static ProgramPlayback find(String data, String programName) throws JSONException {
        JSONArray programs = new JSONObject(data).getJSONArray("programInfos");
        for (int i = 0; i < programs.length(); i++) {
            JSONObject program = programs.optJSONObject(i);
            if (program != null && programName.equals(program.optString("name"))
                    && program.optInt("source", -1) == 0) {
                return new ProgramPlayback(program.getString("identifier"),
                        program.getInt("statusCode"));
            }
        }
        return null;
    }
}
