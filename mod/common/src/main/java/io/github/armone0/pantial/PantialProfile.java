package io.github.armone0.pantial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** config/pantial.json: kendi kodumuz, gizli anahtarimiz, arkadaslar ve ayarlar. */
public class PantialProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Pattern CODE_PATTERN = Pattern.compile("PANT-[" + CHARS + "]{4}");
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String DEFAULT_SERVER = "http://localhost:3000";
    private static PantialProfile instance;

    public String myUserCode = "";
    /** Sunucuya ilk kayitta baglanir; baskasi ayni kodu sahiplenemesin diye. Paylasmayin. */
    public String secret = "";
    public List<String> friendCodes = new ArrayList<>();
    public String serverUrl = DEFAULT_SERVER;
    public boolean notifications = true;
    public boolean titleButton = true;

    public static synchronized PantialProfile getInstance() {
        if (instance == null) {
            instance = new PantialProfile();
        }
        return instance;
    }

    private static Path file() {
        return Pantial.platform().configDir().resolve("pantial.json");
    }

    public synchronized void load() {
        Path file = file();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                PantialProfile loaded = GSON.fromJson(reader, PantialProfile.class);
                if (loaded != null) {
                    myUserCode = normalize(loaded.myUserCode);
                    secret = loaded.secret != null ? loaded.secret : "";
                    friendCodes = new ArrayList<>();
                    if (loaded.friendCodes != null) {
                        for (String c : loaded.friendCodes) {
                            String n = normalize(c);
                            if (isValidCode(n) && !friendCodes.contains(n)) friendCodes.add(n);
                        }
                    }
                    serverUrl = cleanUrl(loaded.serverUrl);
                    notifications = loaded.notifications;
                    titleButton = loaded.titleButton;
                }
            } catch (IOException | JsonParseException e) {
                Pantial.LOGGER.error("[Pantial] Profil okunamadi, varsayilanlar kullaniliyor", e);
            }
        }

        boolean changed = false;
        if (!isValidCode(myUserCode)) {
            myUserCode = generateRandomCode();
            changed = true;
        }
        if (secret.length() < 16) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            secret = sb.toString();
            changed = true;
        }
        if (changed || !Files.exists(file)) save();
    }

    public synchronized void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            Pantial.LOGGER.error("[Pantial] Profil kaydedilemedi", e);
        }
    }

    public enum AddResult { ADDED, INVALID, SELF, DUPLICATE }

    public synchronized AddResult addFriend(String code) {
        String n = normalize(code);
        if (!isValidCode(n)) return AddResult.INVALID;
        if (n.equals(myUserCode)) return AddResult.SELF;
        if (friendCodes.contains(n)) return AddResult.DUPLICATE;
        friendCodes.add(n);
        save();
        return AddResult.ADDED;
    }

    public synchronized void removeFriend(String code) {
        if (friendCodes.remove(normalize(code))) save();
    }

    public synchronized List<String> getFriendCodes() {
        return new ArrayList<>(friendCodes);
    }

    public synchronized void setServerUrl(String url) {
        serverUrl = cleanUrl(url);
        save();
    }

    public static String cleanUrl(String url) {
        if (url == null || url.isBlank()) return DEFAULT_SERVER;
        String u = url.trim().replaceAll("/+$", "");
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
        return u;
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    public static String generateRandomCode() {
        StringBuilder sb = new StringBuilder("PANT-");
        for (int i = 0; i < 4; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }
}
