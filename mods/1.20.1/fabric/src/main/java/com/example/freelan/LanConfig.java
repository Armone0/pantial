package com.example.freelan;

public class LanConfig {
    /**
     * Varsayilan acik: e4mc tuneli dunyayi internete acar. Kimlik dogrulama kapaliyken
     * adresi bilen herkes istedigi isimle (ornegin sahibin ismiyle) baglanabilir.
     */
    public static volatile boolean onlineMode = true;
    public static volatile boolean enableTunnel = true;
    public static volatile String tunnelUrl = "";
    public static volatile String tunnelStatus = "Hazır";
}
