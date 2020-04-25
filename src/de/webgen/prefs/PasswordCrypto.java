/* Copyright (C) 2020 Christoph Theis */
package de.webgen.prefs;

import de.webgen.prefs.Properties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author chtheis
 */
public final class PasswordCrypto {
    
    // No instance allowed
    private PasswordCrypto() {}
    
    public static String encryptPassword(String pwd) {
        if (pwd == null)
            return null;
        else if (pwd.isEmpty())
            return "";
        
        File keyFile = Properties.findPath("private.key");
        if (keyFile == null)
            keyFile = new File(System.getProperty("user.dir"), "private.key");

        if (!keyFile.exists() || keyFile.length() == 0) {
            try {
                // Plain AES is good enough for passwords
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                // Key size 128 bits is good for the forseeable future
                keyGen.init(128);
                
                // Write our bse64 encoded key to a file
                SecretKey key = keyGen.generateKey();
                FileWriter fw = new FileWriter(keyFile);
                fw.write(Base64.getEncoder().encodeToString(key.getEncoded()));
                fw.flush();
                
                // Now make the file radable to the owner only
                if (keyFile.toPath().getFileSystem().supportedFileAttributeViews().contains("posix"))
                    Files.setPosixFilePermissions(keyFile.toPath(), PosixFilePermissions.fromString("rw-------"));
                else if (keyFile.toPath().getFileSystem().supportedFileAttributeViews().contains("acl")) {
                    // With ACL try to lookup the ACL for the owner
                    List<AclEntry> aclList = Files.getFileAttributeView(keyFile.toPath(), AclFileAttributeView.class).getAcl();
                    UserPrincipal owner = Files.getOwner(keyFile.toPath());
                    AclEntry ownerAcl = null;
                    for (AclEntry acl : aclList) {
                        if (acl.principal().equals(owner)) {
                            ownerAcl = acl;
                            break;
                        }                        
                    }
                    
                    // If not found, create one (hopefully we got all required permissions
                    if (ownerAcl == null) 
                        ownerAcl = AclEntry.newBuilder()
                                .setType(AclEntryType.ALLOW)
                                .setPrincipal(owner)
                                .setPermissions(
                                        AclEntryPermission.READ_DATA, 
                                        AclEntryPermission.WRITE_DATA,
                                        AclEntryPermission.APPEND_DATA,
                                        AclEntryPermission.READ_NAMED_ATTRS,
                                        AclEntryPermission.WRITE_NAMED_ATTRS,
                                        AclEntryPermission.EXECUTE,
                                        AclEntryPermission.READ_ATTRIBUTES,
                                        AclEntryPermission.WRITE_ATTRIBUTES,
                                        AclEntryPermission.DELETE,
                                        AclEntryPermission.READ_ACL,
                                        AclEntryPermission.SYNCHRONIZE
                                )
                                .build();
                    aclList.clear();
                    aclList.add(ownerAcl);
                    
                    // This will be the only ACL, which means no one but user, not even admin, may read the file
                    Files.getFileAttributeView(keyFile.toPath(), AclFileAttributeView.class).setAcl(aclList);
                }                    
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(PasswordCrypto.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PasswordCrypto.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Throwable t) {    
                // Anything else like WindowsExceptions (e.g. access denied)
                Logger.getLogger(PasswordCrypto.class.getName()).log(Level.SEVERE, null, t);
            }
        }

        try {
            SecretKeySpec sks = new SecretKeySpec(Base64.getDecoder().decode(Files.readAllBytes(keyFile.toPath())), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, sks, cipher.getParameters());
            return Base64.getEncoder().encodeToString(cipher.doFinal(pwd.getBytes()));
        } catch (Exception ex) {
                Logger.getLogger(PasswordCrypto.class.getName()).log(Level.SEVERE, null, ex);
        }

        // In case of an error return the password to encrypt itself
        return pwd;        
    }
    
    public static String decryptPassword(String pwd) {
        if (pwd == null)
            return null;
        else if (pwd.isEmpty())
            return "";
        
        File keyFile = Properties.findPath("private.key");
        if (keyFile == null || !keyFile.exists() || keyFile.length() == 0)
            return pwd;

        try {
            SecretKeySpec sks = new SecretKeySpec(Base64.getDecoder().decode(Files.readAllBytes(keyFile.toPath())), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, sks, cipher.getParameters());
            return new String(cipher.doFinal(Base64.getDecoder().decode(pwd)));
        } catch (Exception ex) {
            Logger.getLogger(PasswordCrypto.class.getName()).log(Level.SEVERE, null, ex); 
        }
        
        // In case of an error return the password to decrypt itself
        return pwd;        
    }
    
    
}
