package com.example.freelan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PantialProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static PantialProfile instance;

    public String myUserCode = "";
    public List<String> friendCodes = new ArrayList<>();
    public String serverUrl = "http://localhost:3000";

    public static synchronized PantialProfile getInstance() {
        if (instance == null) {
            instance = new PantialProfile();
        }
        return instance;
    }

    public static File getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("pantial_profile.json").toFile();
    }

    public void load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                PantialProfile loaded = GSON.fromJson(reader, PantialProfile.class);
                if (loaded != null) {
                    this.myUserCode = loaded.myUserCode != null ? loaded.myUserCode : "";
                    this.friendCodes = loaded.friendCodes != null ? loaded.friendCodes : new ArrayList<>();
                    this.serverUrl = loaded.serverUrl != null ? loaded.serverUrl : "http://localhost:3000";
                }
            } catch (IOException e) {
                FreeLAN.LOGGER.error("[Pantial] Profil yapilandirma dosyasi okunamadi", e);
            }
        } else {
            if (myUserCode.isEmpty()) {
                myUserCode = generateRandomCode();
            }
            save();
        }
    }

    public void save() {
        File file = getConfigFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            FreeLAN.LOGGER.error("[Pantial] Profil yapilandirma dosyasi kaydedilemedi", e);
        }
    }

    public static String generateRandomCode() {
        Random rand = new Random();
        StringBuilder sb = new StringBuilder("PANT-");
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(rand.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
