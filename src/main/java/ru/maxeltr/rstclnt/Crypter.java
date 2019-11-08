/*
 * The MIT License
 *
 * Copyright 2019 Maxim Eltratov <Maxim.Eltratov@yandex.ru>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.maxeltr.rstclnt;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import ru.maxeltr.rstclnt.Config.Config;

/**
 *
 * @author Maxim Eltratov <Maxim.Eltratov@yandex.ru>
 */
public class Crypter {

    private Config config;
    private final String password = "1234";
    private final byte[] salt = "12345678".getBytes();
    private int iterationCount = 4000;
    private int keyLength = 128;
    private SecretKeySpec key;
    private Cipher pbeCipher;

    public Crypter(Config config) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException {
        this.config = config;
        this.pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        this.key = createSecretKey(this.password, this.salt, this.iterationCount, this.keyLength);
    }

    public SecretKeySpec createSecretKey(String password, byte[] salt, int iterationCount, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    public String encrypt(String value) throws UnsupportedEncodingException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        if (value.isEmpty()) {
            return value;
        }
        this.pbeCipher.init(Cipher.ENCRYPT_MODE, this.key);
        AlgorithmParameters parameters = this.pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = this.pbeCipher.doFinal(value.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    public String decrypt(String value) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
        if (value.isEmpty()) {
            return value;
        }
        String iv = value.split(":")[0];
        String cryptoText = value.split(":")[1];
        this.pbeCipher.init(Cipher.DECRYPT_MODE, this.key, new IvParameterSpec(base64Decode(iv)));
        return new String(this.pbeCipher.doFinal(base64Decode(cryptoText)), "UTF-8");
    }

    private String base64Encode(byte[] value) {
        return Base64.getEncoder().encodeToString(value);
    }

    private byte[] base64Decode(String value) {
        return Base64.getDecoder().decode(value);
    }
}
