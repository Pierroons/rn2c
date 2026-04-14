package se.lublin.mumla.tor;

/**
 * Configuration hardcodée du serveur RN2C.
 * L'adresse .onion et le port sont fixes — pas de sélection de serveur.
 */
public final class RN2CConfig {
    public static final String ONION_ADDRESS = "avwdwxhbcr3kh44ifxj3kznxlhrageh6xs2i6vcfcfxr4z5te27fewid.onion";
    public static final int PORT = 64738;
    public static final String DEFAULT_CHANNEL = "Root";
    public static final int TOR_SOCKS_PORT = 9050;
    public static final String WEB_ONION_URL = "http://bjpgjcnmixogz4ym2aekln5fdbdqg46rptvxtzpyalu45ywax626i3qd.onion";

    // Package Tor Browser (officiel)
    public static final String TOR_BROWSER_PACKAGE = "org.torproject.torbrowser";
    public static final String TOR_BROWSER_FDROID = "https://f-droid.org/packages/org.torproject.torbrowser/";

    private RN2CConfig() {}
}
