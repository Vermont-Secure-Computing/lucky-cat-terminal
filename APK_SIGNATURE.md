# APK Signature Verification

All official **Lucky Cat Terminal (LCTerm)** Android APK releases are digitally signed to help users verify their authenticity and integrity.

## Official Signing Certificate

| Property | Value |
|---|---|
| Organization | VTSCC (Vermont Secure Computing Consultancy) |
| Certificate holder | Robert Gludo II |
| Key algorithm | RSA |
| Key size | 2048 bits |
| APK Signature Scheme | v2 |
| Number of signers | 1 |

### Official SHA-256 Signing Certificate Fingerprint

```text
FE:40:1E:95:49:05:AD:8D:7B:27:00:A5:4B:5C:DD:51:69:8C:19:1A:0E:D5:80:16:32:A2:52:0B:73:F1:44:B6
```

This fingerprint identifies the signing certificate used for the official LCTerm APK.

## How to Verify an APK

1. Download the APK from the [official GitHub Releases page](https://github.com/Vermont-Secure-Computing/lucky-cat-terminal/releases).
2. Ensure Android SDK Build Tools are installed.
3. Open a terminal in the directory containing the downloaded APK.
4. Run the following command, replacing the filename if necessary:

```bash
apksigner verify --verbose --print-certs app-full-release.apk
```

### Expected Verification Results

```text
Verifies
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
Signer #1 certificate SHA-256 digest: fe401e954905ad8d7b2700a54b5cdd51698c191a0ed5801632a2520b73f144b6
```

The certificate SHA-256 digest must match the official fingerprint published above. The hexadecimal characters are case-insensitive, and the colons are only formatting separators.

Other APK signature schemes may report `false` because the official APK is signed using v2.

## Security Notice

If APK verification fails or the signing certificate fingerprint does not match, **do not install the APK**.

Download a fresh copy from the official repository and report any discrepancy.

Never rely solely on an APK filename to determine authenticity.

## Official Releases

https://github.com/Vermont-Secure-Computing/lucky-cat-terminal/releases