package edu.cmu.cc.webtier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import java.util.Map;
import javax.crypto.Cipher;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.math.BigInteger;

public class UtilsBlock {

	public static BigInteger N = new BigInteger("1561906343821");

	public static BigInteger D = new BigInteger("343710770439");

	public static BigInteger E = new BigInteger("1097844002039");

	public static String CCHash(String message) {
		try {
			String hexString = Sha256(message);
			return hexString.substring(0, 8);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return "";
	}

	public static String Sha256(String message) {
		try {

			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			byte[] encodedhash = shaDigest.digest(message.getBytes(StandardCharsets.UTF_8));
			String hexString = new String(Hex.encodeHex(encodedhash));
			return hexString;
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return "";
	}

	// public static boolean validateSignature(String sig, int transHash, String
	// publicKey) {
	// try {
	// KeyFactory factory = KeyFactory.getInstance("RSA");
	// Cipher cipher = Cipher.getInstance("RSA");
	// RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(N, new
	// BigInteger(publicKey));
	// PublicKey pubKey = factory.generatePublic(pubSpec);
	// cipher.init(Cipher.DECRYPT_MODE, pubKey);
	// byte[] digitalSignature = cipher.doFinal(sig.getBytes());
	// int value = new BigInteger(digitalSignature).intValue();
	// return value == transHash;
	// } catch (Exception e) {
	// e.printStackTrace(System.out);
	// }
	// return false;
	// }

	public static boolean validateSignature(String sig, String transHash, String publicKey) {
		BigInteger signInt = new BigInteger(sig);
		BigInteger transInt = new BigInteger(transHash, 16);
		BigInteger keyInt = new BigInteger(publicKey);
		BigInteger veriTrans = signInt.modPow(keyInt, N);
		return veriTrans.equals(transInt);
	}

	// public static String generateSignature(int transHash) {
	// try {
	// KeyFactory factory = KeyFactory.getInstance("RSA");
	// Cipher cipher = Cipher.getInstance("RSA");
	// RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(N, E);
	// PrivateKey privateKey = factory.generatePrivate(privateSpec);
	// cipher.init(Cipher.ENCRYPT_MODE, privateKey);
	// byte[] digitalSignature =
	// cipher.doFinal(ByteBuffer.allocate(4).putInt(transHash).array(););
	// return new String(Hex.encodeHex(digitalSignature));
	// } catch (Exception e) {
	// e.printStackTrace(System.out);
	// }
	// return "";
	// }

	public static long generateSignature(String transHash) {
		BigInteger transInt = new BigInteger(transHash, 16);
		BigInteger sign = transInt.modPow(D, N);
    
		// return sign.toString(10);
    return sign.longValue();
	}

	public static JsonObject decompress(String raw_request) throws Exception {
		// Steps:
		// 1. first use urlDecoder to decode into byte array
		// 2. second use zlib inflater to uncompress the array
		// Potential overflow, just set a reasonable 4096 bytes there for right now
		Inflater inflater = new Inflater();
		byte[] byte_decoded = Base64.getUrlDecoder().decode(raw_request);
		inflater.setInput(byte_decoded, 0, byte_decoded.length);
		byte[] result = new byte[4096];
		int resultLength = inflater.inflate(result);
		inflater.end();
		String message = new String(result, 0, resultLength, "UTF-8");
		JsonObject requestJson = (JsonObject) JsonParser.parseString(message);
		return requestJson;
	}

	public static String compress(JsonObject chain) throws Exception {
		Deflater deflater = new Deflater();
		byte[] byte_to_encode = chain.toString().getBytes("UTF-8");
		byte[] result = new byte[4096];
		deflater.setInput(byte_to_encode);
		deflater.finish();
		int length = deflater.deflate(result);
		deflater.end();
    byte[] toEncode = new byte[length + 1];
    System.arraycopy(result, 0, toEncode, 0, length);
		String toReturn = new String(Base64.getUrlEncoder().encode(toEncode));
		return toReturn;
	}

	/**
	 * @param counter specific counter used to generate the PoW string
	 * @return the actual PoW string
	 */
	public static String pow(int counter) {
		return String.valueOf(counter);
	}
}