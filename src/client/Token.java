package client;
/* code complet pour Token ici */
/**
 * Token class representing permission to download blocks from a trusted client.
 */
public class Token {
    private final String tokenId;
    private final int allowedBlocks;

    public Token(String tokenId, int allowedBlocks) {
        this.tokenId = tokenId;
        this.allowedBlocks = allowedBlocks;
    }

    public String getTokenId() {
        return tokenId;
    }

    public int getAllowedBlocks() {
        return allowedBlocks;
    }
}
