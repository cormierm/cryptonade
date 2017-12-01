package com.mattcormier.cryptonade.lib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.mattcormier.cryptonade.MainActivity;
import com.mattcormier.cryptonade.clients.APIClient;
import com.mattcormier.cryptonade.clients.BinanceClient;
import com.mattcormier.cryptonade.clients.BitfinexClient;
import com.mattcormier.cryptonade.clients.BittrexClient;
import com.mattcormier.cryptonade.clients.CexioClient;
import com.mattcormier.cryptonade.clients.GDAXClient;
import com.mattcormier.cryptonade.clients.GeminiClient;
import com.mattcormier.cryptonade.clients.HitBTCClient;
import com.mattcormier.cryptonade.clients.PoloniexClient;
import com.mattcormier.cryptonade.clients.QuadrigacxClient;
import com.mattcormier.cryptonade.databases.CryptoDB;
import com.mattcormier.cryptonade.models.Exchange;

import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static android.content.Context.MODE_PRIVATE;

/**
 * Filename: Crypto.java
 * Description: Class that contains misc functions and tools.
 * Created by Matt Cormier on 10/18/2017.
 */

public class Crypto {
    private static final String TAG = "Crypto";

    public static APIClient getAPIClient(Exchange exchange) {
        if (exchange.getTypeId() == 1) {
            return new PoloniexClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        else if (exchange.getTypeId() == 2) {
            return new QuadrigacxClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret(), exchange.getAPIOther());
        }
        else if (exchange.getTypeId() == 3) {
            return new BitfinexClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        else if (exchange.getTypeId() == 4) {
            return new BittrexClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        else if (exchange.getTypeId() == 5) {
            return new CexioClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret(), exchange.getAPIOther());
        }
        else if (exchange.getTypeId() == 6) {
            return new GDAXClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret(), exchange.getAPIOther());
        }
        else if (exchange.getTypeId() == 7) {
            return new GeminiClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        else if (exchange.getTypeId() == 8) {
            return new HitBTCClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        else if (exchange.getTypeId() == 9) {
            return new BinanceClient((int)exchange.getId(), exchange.getName(), exchange.getAPIKey(), exchange.getAPISecret());
        }
        return null;
    }

    public static String formatDate(String dateString) {
        long longDate = (long)Double.parseDouble(dateString) * 1000;
        Date dateTime = new Date(longDate);
        SimpleDateFormat dateFormat = new SimpleDateFormat("y-MM-dd HH:mm:ss");
        return dateFormat.format(dateTime);
    }

    public static String createMD5Hash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(text.getBytes("utf-8"));
            byte[] hashText = md.digest();
            String result = new String(Hex.encodeHex(hashText));
            Log.d(TAG, "createMD5Hash: hashText: " + result);
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "createMD5Hash: " + e.getMessage() );
        }
        return null;
    }

    public static void setPassword(final Context c, final SharedPreferences prefs) {
        final EditText txtUrl = new EditText(c);
        txtUrl.setHint("Enter password");
        txtUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(c)
                .setTitle("Set Master Password")
                .setMessage("Enter strong password. (Recommend at least then 8 characters with letters and numbers.) This will be used to encrypt your api keys as well!")
                .setView(txtUrl)
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = txtUrl.getText().toString();
                        confirmPassword(c, password, prefs);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
    public static void confirmPassword(final Context c, final String pass, final SharedPreferences prefs) {
        final EditText txtUrl = new EditText(c);
        txtUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(c)
                .setMessage("Confirm Password")
                .setView(txtUrl)
                .setPositiveButton("Save Password", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String password = txtUrl.getText().toString();
                        Log.e(TAG, "onClick: " + password);
                        if (password.equals(pass)) {
                            CryptoDB db = new CryptoDB(c);
                            if (pass.equals("")) {
                                db.updatePasswordHash("");
                            } else {
                                Toast.makeText(c, "Password has been set", Toast.LENGTH_SHORT).show();
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("pref_set_master_password", true);
                                editor.commit();
                                String passwordHash = createMD5Hash(pass);
                                db.updatePasswordHash(passwordHash);
                            }
                        } else {
                            Toast.makeText(c, "Passwords did not match!", Toast.LENGTH_SHORT).show();
                            prefs.edit().putBoolean("pref_set_master_password", false);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    public static void loginPassword(final Context c) {
        final EditText txtPassword = new EditText(c);
        txtPassword.setHint("Enter password");
        txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(c)
                .setTitle("Master Password Required")
                .setMessage("Enter password to access Cryptonade.")
                .setView(txtPassword)
                .setPositiveButton("Login", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        CryptoDB db = new CryptoDB(c);
                        String password = txtPassword.getText().toString();
                        if (createMD5Hash(txtPassword.getText().toString()).equals(db.getPasswordHash())) {
                            Intent intent = new Intent(c, MainActivity.class);
                            intent.putExtra("password", password);
                            c.startActivity(intent);
                        } else {
                            System.exit(0);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        System.exit(0);
                    }
                })
                .show();
    }

    public static SecretKey createSecret(char[] password, byte[] salt) {
        SecretKeyFactory factory = null;
        String plaintext = "nothing";
        try {
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
            Log.e(TAG, "createSecret: " + e1.getMessage());
        } catch (InvalidKeySpecException e1) {
            e1.printStackTrace();
            Log.e(TAG, "createSecret: " + e1.getMessage());
        }
        return null;

    }

    public static HashMap<String, String> encryptString(String text, SecretKey secret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] ciphertext = cipher.doFinal(text.getBytes("UTF-8"));
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("iv", iv.toString());
            hashMap.put("ciphertext", ciphertext.toString());
            return hashMap;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (BadPaddingException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (InvalidParameterSpecException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            Log.e(TAG, "encryptString: " + e.getMessage());
        }
        return null;
    }

    public static String decryptString(String ciphertext, SecretKey secret, String iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv.getBytes("UTF-8")));
            return new String(cipher.doFinal(ciphertext.getBytes("UTF-8")), "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveCurrentScreen(Context c, String screenTag) {
        SharedPreferences sharedPreferences = c.getSharedPreferences("main", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("currentScreen", screenTag);
        editor.commit();
    }
}
