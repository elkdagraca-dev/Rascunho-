package com.plotpoint.dynamiccards;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.util.LruCache;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@DesignerComponent(
        version = 1,
        versionName = "1.0",
        description = "Cria cartões dinâmicos para catálogos.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = ""
)
@SimpleObject(external = true)
@UsesPermissions(
        permissionNames = "android.permission.INTERNET"
)
public class DynamicCards extends AndroidNonvisibleComponent {

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final LruCache<String, Bitmap> memoryCache =
            new LruCache<String, Bitmap>(20) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };

    private int cardWidth = 320;
    private int cardHeight = 460;
    private int imageHeight = 330;

    private int cornerRadius = 18;
    private int spacing = 8;

    private int titleColor = Color.WHITE;
    private int subtitleColor = Color.LTGRAY;

    private int cardBackgroundColor = Color.rgb(30, 30, 30);
    private int loadingColor = Color.rgb(95, 95, 95);

    private int titleSize = 14;
    private int subtitleSize = 12;

    private final Map<String, ViewGroup> cards = new HashMap<>();

    public DynamicCards(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
    }

    // ============================================================
    // CONFIGURAÇÃO
    // ============================================================

    @SimpleFunction(description = "Define a largura dos cartões em pixels.")
    public void SetCardWidth(int width) {
        if (width > 0) {
            cardWidth = width;
        }
    }

    @SimpleFunction(description = "Define a altura total dos cartões em pixels.")
    public void SetCardHeight(int height) {
        if (height > 0) {
            cardHeight = height;
        }
    }

    @SimpleFunction(description = "Define a altura da imagem.")
    public void SetImageHeight(int height) {
        if (height > 0) {
            imageHeight = height;
        }
    }

    @SimpleFunction(description = "Define o raio dos cantos.")
    public void SetCornerRadius(int radius) {
        if (radius >= 0) {
            cornerRadius = radius;
        }
    }

    @SimpleFunction(description = "Define o espaço entre cartões.")
    public void SetSpacing(int value) {
        if (value >= 0) {
            spacing = value;
        }
    }

    @SimpleFunction(description = "Define a cor do título.")
    public void SetTitleColor(int color) {
        titleColor = color;
    }

    @SimpleFunction(description = "Define a cor do subtítulo.")
    public void SetSubtitleColor(int color) {
        subtitleColor = color;
    }

    @SimpleFunction(description = "Define a cor de fundo do cartão.")
    public void SetCardBackgroundColor(int color) {
        cardBackgroundColor = color;
    }

    @SimpleFunction(description = "Define a cor exibida enquanto a imagem carrega.")
    public void SetLoadingColor(int color) {
        loadingColor = color;
    }

    // ============================================================
    // ADICIONAR CARTÃO
    // ============================================================

    @SimpleFunction(description = "Adiciona um cartão a um Arrangement.")
    public void AddCard(
            AndroidViewComponent container,
            String id,
            String imageUrl,
            String title,
            String subtitle) {

        if (container == null) {
            return;
        }

        View view = container.getView();

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup parent = (ViewGroup) view;

        LinearLayout card = createCard(
                parent,
                id,
                imageUrl,
                title,
                subtitle
        );

        parent.addView(card);
    }

    // ============================================================
    // JSON
    // ============================================================

    @SimpleFunction(description = "Cria vários cartões a partir de um JSON.")
    public void SetCards(
            AndroidViewComponent container,
            String json) {

        if (container == null || json == null) {
            return;
        }

        View view = container.getView();

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup parent = (ViewGroup) view;

        parent.removeAllViews();
        cards.clear();

        try {

            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {

                JSONObject item = array.getJSONObject(i);

                String id = item.optString("id", String.valueOf(i));

                String image =
                        item.optString("image", "");

                String title =
                        item.optString("title", "");

                String subtitle =
                        item.optString("subtitle", "");

                LinearLayout card = createCard(
                        parent,
                        id,
                        image,
                        title,
                        subtitle
                );

                parent.addView(card);
            }

        } catch (Exception e) {

            Error("JSON inválido: " + e.getMessage());
        }
    }

    // ============================================================
    // CRIAÇÃO DO CARTÃO
    // ============================================================

    private LinearLayout createCard(
            ViewGroup parent,
            final String id,
            final String imageUrl,
            String title,
            String subtitle) {

        LinearLayout card =
                new LinearLayout(context);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        card.setPadding(
                0,
                0,
                0,
                0
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                cardBackgroundColor
        );

        background.setCornerRadius(
                cornerRadius
        );

        card.setBackground(
                background
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        cardWidth,
                        cardHeight
                );

        cardParams.setMargins(
                spacing,
                spacing,
                spacing,
                spacing
        );

        card.setLayoutParams(cardParams);

        // ========================================================
        // ÁREA DA IMAGEM
        // ========================================================

        final FrameContainer imageContainer =
                new FrameContainer(context);

        imageContainer.setBackgroundColor(
                loadingColor
        );

        LinearLayout.LayoutParams imageContainerParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        imageHeight
                );

        imageContainer.setLayoutParams(
                imageContainerParams
        );

        card.addView(
                imageContainer
        );

        final ImageView imageView =
                new ImageView(context);

        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        imageView.setBackgroundColor(
                loadingColor
        );

        imageView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        imageContainer.addView(
                imageView
        );

        // ========================================================
        // TÍTULO
        // ========================================================

        TextView titleView =
                new TextView(context);

        titleView.setText(
                parseHtml(title)
        );

        titleView.setTextColor(
                titleColor
        );

        titleView.setTextSize(
                titleSize
        );

        titleView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        titleView.setMaxLines(2);

        titleView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        titleView.setPadding(
                5,
                6,
                5,
                0
        );

        card.addView(
                titleView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // ========================================================
        // SUBTÍTULO
        // ========================================================

        TextView subtitleView =
                new TextView(context);

        subtitleView.setText(
                parseHtml(subtitle)
        );

        subtitleView.setTextColor(
                subtitleColor
        );

        subtitleView.setTextSize(
                subtitleSize
        );

        subtitleView.setMaxLines(1);

        subtitleView.setEllipsize(
                android.text.TextUtils.TruncateAt.END
        );

        subtitleView.setPadding(
                5,
                0,
                5,
                5
        );

        card.addView(
                subtitleView
        );

        cards.put(
                id,
                card
        );

        // ========================================================
        // CLIQUE CURTO
        // ========================================================

        card.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {

                        CardClicked(id);
                    }
                }
        );

        // ========================================================
        // CLIQUE LONGO
        // ========================================================

        card.setOnLongClickListener(
                new View.OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {

                        CardLongClicked(id);

                        return true;
                    }
                }
        );

        // ========================================================
        // IMAGEM
        // ========================================================

        if (imageUrl != null &&
                !imageUrl.trim().isEmpty()) {

            loadImage(
                    imageUrl,
                    imageView
            );
        }

        return card;
    }

    // ============================================================
    // HTML
    // ============================================================

    private Spanned parseHtml(String value) {

        if (value == null) {
            return Html.fromHtml("");
        }

        if (BuildVersion() >= 24) {

            return Html.fromHtml(
                    value,
                    Html.FROM_HTML_MODE_LEGACY
            );

        } else {

            return Html.fromHtml(value);
        }
    }

    private int BuildVersion() {
        return android.os.Build.VERSION.SDK_INT;
    }

    // ============================================================
    // CARREGAMENTO DA IMAGEM
    // ============================================================

    private void loadImage(
            final String url,
            final ImageView imageView) {

        Bitmap cached =
                memoryCache.get(url);

        if (cached != null) {

            imageView.setImageBitmap(
                    cached
            );

            return;
        }

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        Bitmap bitmap =
                                getFromDisk(url);

                        if (bitmap == null) {

                            bitmap =
                                    downloadImage(url);

                            if (bitmap != null) {

                                saveToDisk(
                                        url,
                                        bitmap
                                );
                            }
                        }

                        final Bitmap result =
                                bitmap;

                        if (result != null) {

                            memoryCache.put(
                                    url,
                                    result
                            );
                        }

                        mainHandler.post(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        if (result != null) {

                                            imageView.setImageBitmap(
                                                    result
                                            );
                                        }
                                    }
                                }
                        );
                    }
                }
        ).start();
    }

    private Bitmap downloadImage(
            String urlString) {

        HttpURLConnection connection =
                null;

        InputStream input =
                null;

        try {

            URL url =
                    new URL(urlString);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setConnectTimeout(
                    10000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.connect();

            if (connection.getResponseCode()
                    != HttpURLConnection.HTTP_OK) {

                return null;
            }

            input =
                    connection.getInputStream();

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            options.inPreferredConfig =
                    Bitmap.Config.RGB_565;

            Bitmap bitmap =
                    BitmapFactory.decodeStream(
                            input,
                            null,
                            options
                    );

            return bitmap;

        } catch (Exception e) {

            return null;

        } finally {

            try {

                if (input != null) {
                    input.close();
                }

            } catch (Exception ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ============================================================
    // CACHE EM DISCO
    // ============================================================

    private File cacheDirectory() {

        File directory =
                new File(
                        context.getCacheDir(),
                        "dynamic_cards"
                );

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory;
    }

    private String hash(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "MD5"
                    );

            byte[] bytes =
                    digest.digest(
                            value.getBytes("UTF-8")
                    );

            StringBuilder builder =
                    new StringBuilder();

            for (byte b : bytes) {

                builder.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return builder.toString();

        } catch (Exception e) {

            return String.valueOf(
                    value.hashCode()
            );
        }
    }

    private File cacheFile(String url) {

        return new File(
                cacheDirectory(),
                hash(url) + ".jpg"
        );
    }

    private void saveToDisk(
            String url,
            Bitmap bitmap) {

        File file =
                cacheFile(url);

        FileOutputStream output =
                null;

        try {

            output =
                    new FileOutputStream(
                            file
                    );

            bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    85,
                    output
            );

            output.flush();

        } catch (Exception ignored) {

        } finally {

            try {

                if (output != null) {
                    output.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private Bitmap getFromDisk(
            String url) {

        File file =
                cacheFile(url);

        if (!file.exists()) {
            return null;
        }

        FileInputStream input =
                null;

        try {

            input =
                    new FileInputStream(
                            file
                    );

            return BitmapFactory.decodeStream(
                    input
            );

        } catch (Exception e) {

            return null;
          } finally {

            try {

                if (input != null) {
                    input.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    // ============================================================
    // EVENTOS
    // ============================================================

    @SimpleEvent(description = "Disparado quando um cartão é clicado.")
    public void CardClicked(String id) {

        EventDispatcher.dispatchEvent(
                this,
                "CardClicked",
                id
        );
    }

    @SimpleEvent(description = "Disparado quando um cartão recebe um clique longo.")
    public void CardLongClicked(String id) {

        EventDispatcher.dispatchEvent(
                this,
                "CardLongClicked",
                id
        );
    }

    @SimpleEvent(description = "Disparado quando ocorre um erro.")
    public void Error(String message) {

        EventDispatcher.dispatchEvent(
                this,
                "Error",
                message
        );
    }

    // ============================================================
    // CONTAINER INTERNO
    // ============================================================

    private static class FrameContainer
            extends android.widget.FrameLayout {

        public FrameContainer(Context context) {
            super(context);
        }
    }
}
       
