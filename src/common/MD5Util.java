package common;
/* code complet pour MD5Util ici */


import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * MD5Util provides utilities to compute MD5 checksums.
 */
public class MD5Util {

    public static String computeMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] dataBytes = new byte[1024];
            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
        }
        byte[] mdbytes = md.digest();

        // Convert to hex format
        StringBuilder sb = new StringBuilder();
        for (byte mdbyte : mdbytes) {
            sb.append(Integer.toHexString((mdbyte & 0xff) | 0x100).substring(1));
        }
        return sb.toString();
    }
}
