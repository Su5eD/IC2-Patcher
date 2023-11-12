package mods.su5ed.ic2patcher.util;

public class VersionComparison {
    /**
     * Compares two versions separated by dots. Doesn't work with versions schema containing letters.
     * Truth table:<br>
     *  v1 > v2 => true<br>
     *  v1 == v2 => true<br>
     *  v1 < v2 => false<br>
     *  v1 // v2 contain chars -> Integer parsing Exception<br>
     */
    public static boolean compareVersions(String v1, String v2) {
        String[] v1s = v1.split("\\.");
        String[] v2s = v2.split("\\.");

        for (int i = 0; i < Math.min(v1s.length, v2s.length); i++) {
            int v1i = Integer.parseInt(v1s[i]);
            int v2i = Integer.parseInt(v2s[i]);
            if (v1i > v2i) {
                return true;
            } else if (v2i > v1i) {
                return false;
            }
        }
        return v1s.length >= v2s.length;
    }
}
