# IBM FileNet GCD Recovery Tool

A utility to decrypt IBM FileNet P8 binary configurations (GCD), extract hidden encryption keys via steganography analysis, and recover service account credentials (`SystemUser`, `DirectoryServer`).

> ⚠️ **DISCLAIMER**
>
> This tool is designed **exclusively for system administrators** to restore system functionality (e.g., fixing LDAP sync issues or recovering lost access).
> The authors are not responsible for any misuse. Use only on systems you own or are authorized to administer.

## 🚀 Features

* 📖 **Decode:** Converts the binary GCD BLOB into human-readable XML.
* 🗝️ **Key Extraction:** Automatically recovers the AES Master Key
* 🔓 **Recovery:** Decrypts and reveals `SystemPassword` and `DirectoryServerPassword`.
* 🔐 **Patching:** Generates a valid encrypted blob for a *new* password, allowing you to manually update the configuration if the LDAP password has changed.

## 🛠 Technical Details

This tool implements algorithms recovered via reverse-engineering of the FileNet Engine core.

## 📦 Build (Maven)

Requirements: JDK 8 or higher.

```bash
mvn clean package