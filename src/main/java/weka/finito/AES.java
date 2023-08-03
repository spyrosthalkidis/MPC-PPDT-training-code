package weka.finito;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public final class AES {
	private SecretKey key;
	private int iterations = 65536;
	private int key_length = 256;
	private final byte [] salt = "123456789".getBytes();
	private final SecureRandom random = new SecureRandom();
	
	private byte [] iv_bytes = new byte[16];
	private IvParameterSpec ivspec = null;  
	private String iv_string = null;
	private final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

	public AES(String password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
		getKeyFromPassword(password);
	}

	public AES(String password, int iterations, int key_length) throws NoSuchPaddingException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		this(password);
		this.iterations = iterations;
		this.key_length = key_length;
	}
	
	public String getIV() {
		return this.iv_string;
	}
	
	public void getKeyFromPassword(String password)
		    throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(password.toCharArray(), this.salt, this.iterations, this.key_length);
		this.key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
	}
	
	public String encrypt(String strToEncrypt)
			throws NoSuchAlgorithmException, NoSuchPaddingException, 
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {
		
		// Generate new IV
		random.nextBytes(iv_bytes);
		ivspec = new IvParameterSpec(iv_bytes);
		iv_string = Base64.getEncoder().encodeToString(iv_bytes);
		
		// Run Encryption
	    cipher.init(Cipher.ENCRYPT_MODE, this.key, ivspec);
		byte [] output = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(output);
	}

	public String decrypt(String strToDecrypt, String iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException, 
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		// Get IV
		iv_string = iv;
		iv_bytes = Base64.getDecoder().decode(iv);
		ivspec = new IvParameterSpec(iv_bytes);
		
		// Decrypt the value
	    cipher.init(Cipher.DECRYPT_MODE, this.key, ivspec);
		byte [] output = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
		return new String(output);
	}
}