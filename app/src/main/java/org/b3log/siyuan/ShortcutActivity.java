/*
 * SiYuan - 源于思考，饮水思源
 * Copyright (c) 2020-present, b3log.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.siyuan;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.StringUtils;
import com.zackratos.ultimatebarx.ultimatebarx.java.UltimateBarX;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mobile.Mobile;

/**
 * 追加到日记的快捷方式.
 *
 * @author <a href="https://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Sep 4, 2025
 * @since 3.1.26
 */
public class ShortcutActivity extends AppCompatActivity {

    private static final int REQUEST_SELECT_FILE = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcut);

        final EditText input = findViewById(R.id.full_screen_input);
        UltimateBarX.statusBarOnly(this).transparent().apply();
        BarUtils.setNavBarVisibility(this, false);
        ((ViewGroup) input.getParent()).setPadding(0, UltimateBarX.getStatusBarHeight(), 0, 0);
        BarUtils.setNavBarVisibility(this, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Full screen display in landscape mode on Android https://github.com/siyuan-note/siyuan/issues/14448
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(final Intent intent) {
        setupFullScreenInput();

        if (Intent.ACTION_MAIN.equals(intent.getAction())) { // 来自桌面快捷方式
            final EditText input = findViewById(R.id.full_screen_input);
            input.postDelayed(() -> {
                input.requestFocus();
                KeyboardUtils.showSoftInput(input);
            }, 500);
            return;
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) { // 来自菜单快捷方式
            final String data = intent.getDataString();
            if (StringUtils.equals(data, "shorthand")) {
                final EditText input = findViewById(R.id.full_screen_input);
                input.postDelayed(() -> {
                    input.requestFocus();
                    KeyboardUtils.showSoftInput(input);
                }, 500);
                return;
            }

            Log.w("shortcut", "Unknown data [" + data + "]");
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) { // 来自其他应用分享/发送到
            final String type = intent.getType();
            if (type == null) {
                Log.w("shortcut", "Unknown type [null]");
                return;
            }

            if ("text/plain".equals(type)) {
                final String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (sharedText != null) {
                    final EditText input = findViewById(R.id.full_screen_input);
                    input.append(sharedText);
                    input.setSelection(sharedText.length());
                }
            } else {
                // 支持所有文件类型：image/*, video/*, audio/*, application/*, 以及其他类型
                final Uri assetUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (assetUri != null) {
                    final List<Uri> assets = List.of(assetUri);
                    writeAssets(assets, type);
                    return;
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            // 支持多个文件发送到
            final String type = intent.getType();
            if (type == null) {
                Log.w("shortcut", "Unknown type [null]");
                return;
            }

            final List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null && !uris.isEmpty()) {
                writeAssets(uris, type);
                return;
            }
        }
    }

    private void writeAssets(final List<Uri> assetUris, final String type) {
        if (null == assetUris || assetUris.isEmpty()) {
            return;
        }

        final EditText input = findViewById(R.id.full_screen_input);
        input.requestFocus();
        final String shorthandsDir = getShorthandsDir();
        final File assetsDir = new File(shorthandsDir, "assets");
        assetsDir.mkdirs(); // 确保assets文件夹存在
        
        for (final Uri uri : assetUris) {
            final String p = uri.getLastPathSegment();
            String baseName = Mobile.filepathBase(p);
            baseName = Mobile.filterUploadFileName(baseName);
            final String fileName = Mobile.assetName(baseName);
            final File f = new File(assetsDir, fileName);
            try {
                FileUtils.copyInputStreamToFile(getContentResolver().openInputStream(uri), f);
                String content = "";
                if (type != null && type.startsWith("image/")) {
                    // 图片使用Markdown图片语法
                    content = "![" + baseName + "](assets/" + fileName + ")";
                } else {
                    // 其他文件使用Markdown链接语法
                    content = "[" + baseName + "](assets/" + fileName + ")";
                }
                content += "\n\n";
                input.append(content);
                input.setSelection(input.getText().length());
            } catch (final Exception e) {
                Utils.logError("shortcut", "copy file failed", e);
                Utils.showToast(this, "Failed to copy file [" + e.getMessage() + "]");
            }
        }
    }

    private void setupFullScreenInput() {
        initUploadFileButton();

        final EditText input = findViewById(R.id.full_screen_input);
        final Button submitButton = findViewById(R.id.submit_button);
        submitButton.setEnabled(false);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                submitButton.setEnabled(!StringUtils.isEmpty(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        submitButton.setOnClickListener(v -> {
            final String userInput = input.getText().toString().trim();
            if (StringUtils.isEmpty(userInput)) {
                return;
            }

            final long now = System.currentTimeMillis();
            final String shorthandsDir = getShorthandsDir();
            final File f = new File(shorthandsDir, now + ".md");
            try {
                FileUtils.writeStringToFile(f, userInput, "UTF-8");
            } catch (final Exception e) {
                Utils.logError("shortcut", "Write file failed", e);
                Utils.showToast(this, "Failed to write to file [" + e.getMessage() + "]");
            }

            finish();
        });
    }

    private void initUploadFileButton() {
        final Button uploadFileButton = findViewById(R.id.add_to_home_button);
        uploadFileButton.setOnClickListener(v -> openFileChooser());
    }

    private void openFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.add_to_home)), REQUEST_SELECT_FILE);
        } catch (final Exception e) {
            Utils.logError("shortcut", "Cannot open file chooser", e);
            Utils.showToast(this, "无法打开文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        if (requestCode == REQUEST_SELECT_FILE && resultCode == RESULT_OK && intent != null) {
            ClipData clipData = intent.getClipData();
            String stringData = intent.getDataString();
            
            List<Uri> uris = new ArrayList<>();
            
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            } else if (stringData != null) {
                uris.add(Uri.parse(stringData));
            }
            
            if (!uris.isEmpty()) {
                // 为每个文件单独处理，因为可能有不同的MIME类型
                for (Uri uri : uris) {
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                    writeAssets(List.of(uri), mimeType);
                }
            }
        }
    }

    private String getShorthandsDir() {
        final String ret = getExternalFilesDir(null).getAbsolutePath() + "/home/.config/siyuan/shortcuts/shorthands/";
        new File(ret).mkdirs();
        return ret;
    }
}
