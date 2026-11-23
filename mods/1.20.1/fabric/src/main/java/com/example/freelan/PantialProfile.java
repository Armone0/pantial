package com.example.freelan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** config/pantial_profile.json: kendi kodumuz, gizli anahtarimiz, arkadas kodlari ve sunucu adresi. */
public class PantialProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final Pattern CODE_PATTERN = Pattern.compile("PANT-[" + CHARS + "]{4}");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_SERVER = "http://localhost:3000";
    private static PantialProfile instance;

    public String myUserCode = "";
    /** Sunucuya ilk kayitta baglanir; baskasi ayni kodu sahiplenemesin diye. Paylasmayin. */
    public String secret = "";
    public List<String> friendCodes = new ArrayList<>();
    public String serverUrl = DEFAULT_SERVER;

    public static synchronized PantialProfile getInstance() {
        if (instance == null) {
            instance = new PantialProfile();
        }
        return instance;
    }

    public static Path getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("pantial_profile.json");
    }

    public synchronized void load() {
        Path file = getConfigFile();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                PantialProfile loaded = GSON.fromJson(reader, PantialProfile.class);
                if (loaded != null) {
                    this.myUserCode = loaded.myUserCode != null ? normalize(loaded.myUserCode) : "";
                    this.secret = loaded.secret != null ? loaded.secret : "";
                    this.friendCodes = new ArrayList<>();
                    if (loaded.friendCodes != null) {
                        for (String c : loaded.friendCodes) {
                            String n = normalize(c);
                            if (isValidCode(n) && !this.friendCodes.contains(n)) this.friendCodes.add(n);
                        }
                    }
                    this.serverUrl = loaded.serverUrl != null && !loaded.serverUrl.isBlank()
                            ? loaded.serverUrl.replaceAll("/+$", "") : DEFAULT_SERVER;
                }
            } catch (IOException | JsonParseException e) {
                FreeLAN.LOGGER.error("[Pantial] Profil dosyasi okunamadi, varsayilanlar kullaniliyor", e);
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
            secret = HexFormat.of().formatHex(bytes);
            changed = true;
        }
        if (changed || !Files.exists(file)) {
            save();
        }
    }

    public synchronized void save() {
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            FreeLAN.LOGGER.error("[Pantial] Profil dosyasi kaydedilemedi", e);
        }
    }

    /** @return eklendiyse true; gecersiz, kendi kodumuz veya zaten ekliyse false. */
    public synchronized boolean addFriend(String code) {
        String n = normalize(code);
        if (!isValidCode(n) || n.equals(myUserCode) || friendCodes.contains(n)) return false;
        friendCodes.add(n);
        save();
        return true;
    }

    public synchronized void removeFriend(String code) {
        if (friendCodes.remove(normalize(code))) save();
    }

    public synchronized List<String> getFriendCodes() {
        return List.copyOf(friendCodes);
    }

    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValidCode(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    public static String generateRandomCode() {
        StringBuilder sb = new StringBuilder("PANT-");
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
