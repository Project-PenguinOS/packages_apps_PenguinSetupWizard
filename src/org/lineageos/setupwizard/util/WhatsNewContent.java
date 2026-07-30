/*
 * SPDX-FileCopyrightText: The PenguinOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.util;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lineageos.setupwizard.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Release highlights for the running build, served by the OTA repo alongside the builds
 * themselves. Mirrors the Updater's network data source: the base URL comes from a system
 * property with a build-time default, has the same {device}/{type}/{incr} placeholders, and
 * must be HTTPS.
 */
public final class WhatsNewContent {

    public static final class Highlight {
        public final String icon;
        public final String title;
        public final String summary;

        Highlight(String icon, String title, String summary) {
            this.icon = icon;
            this.title = title;
            this.summary = summary;
        }
    }

    public final String title;
    public final String helloSubtitle;
    public final List<Highlight> highlights;

    private WhatsNewContent(String title, String helloSubtitle, List<Highlight> highlights) {
        this.title = title;
        this.helloSubtitle = helloSubtitle;
        this.highlights = highlights;
    }

    private static final String TAG = WhatsNewContent.class.getSimpleName();

    private static final String PROP_WHATS_NEW_URI = "penguin.whatsnew.uri";
    private static final String PROP_DEVICE = "ro.aospa.device";
    private static final String PROP_RELEASE_TYPE = "ro.aospa.build.variant";

    private static final String KEY_CACHED_CONTENT = "whats_new_content";
    private static final String KEY_CACHED_FINGERPRINT = "whats_new_content_fingerprint";

    private static final int TIMEOUT_MS = 10000;
    // The payload is a handful of one-liners; anything larger is not ours.
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    /** Content last fetched for this build, or {@code null} if there is none. */
    public static WhatsNewContent cached(Context context) {
        if (!Build.FINGERPRINT.equals(
                SetupWizardUtils.getPrefs(context).getString(KEY_CACHED_FINGERPRINT, ""))) {
            return null;
        }
        final String json = SetupWizardUtils.getPrefs(context).getString(KEY_CACHED_CONTENT, "");
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return parse(new JSONObject(json));
        } catch (Exception e) {
            Log.w(TAG, "Discarding unreadable cached content", e);
            return null;
        }
    }

    /**
     * Fetches the highlights for the running build. Blocking; call off the main thread.
     * Returns {@code null} when the content is unavailable, which is the expected outcome on
     * a first boot with no connectivity.
     */
    public static WhatsNewContent fetch(Context context) {
        final String url = serverUrl(context);
        if (url == null) {
            return null;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(false);

            final int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Unexpected HTTP status: " + status);
                return null;
            }

            final String body;
            try (InputStream in = connection.getInputStream()) {
                body = readBody(in);
            }

            final WhatsNewContent content = parse(new JSONObject(body));
            SetupWizardUtils.getPrefs(context).edit()
                    .putString(KEY_CACHED_CONTENT, body)
                    .putString(KEY_CACHED_FINGERPRINT, Build.FINGERPRINT)
                    .apply();
            return content;
        } catch (Exception e) {
            Log.w(TAG, "Couldn't fetch what's new content", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String serverUrl(Context context) {
        String base = SystemProperties.get(PROP_WHATS_NEW_URI, "").trim();
        if (TextUtils.isEmpty(base)) {
            base = context.getString(R.string.whats_new_server_url);
        }
        if (TextUtils.isEmpty(base)) {
            return null;
        }
        if (!base.startsWith("https://")) {
            Log.w(TAG, "What's new URL must use HTTPS: " + base);
            return null;
        }
        return base
                .replace("{device}", SystemProperties.get(PROP_DEVICE, ""))
                .replace("{type}", SystemProperties.get(PROP_RELEASE_TYPE, "").toLowerCase())
                .replace("{incr}", Build.VERSION.INCREMENTAL);
    }

    private static String readBody(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out.size() + read > MAX_RESPONSE_BYTES) {
                throw new IOException("Response exceeds " + MAX_RESPONSE_BYTES + " bytes");
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    private static WhatsNewContent parse(JSONObject root) {
        final List<Highlight> highlights = new ArrayList<>();
        final JSONArray array = root.optJSONArray("highlights");
        for (int i = 0; array != null && i < array.length(); i++) {
            final JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            final String title = item.optString("title");
            if (TextUtils.isEmpty(title)) {
                continue;
            }
            highlights.add(new Highlight(item.optString("icon"), title,
                    item.optString("summary")));
        }
        return new WhatsNewContent(root.optString("title"), root.optString("hello_subtitle"),
                highlights);
    }
}
