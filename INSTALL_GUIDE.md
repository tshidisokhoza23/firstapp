# TJ iOS App - GitHub Push & Local USB Installation Guide

This guide details all commands starting from pushing your code to GitHub, running CI/CD, downloading the artifact, USB device pairing, installing via `ideviceinstaller`, and trusting the app on iOS.

## Step 1: Push Code to GitHub

```bash
cd /home/pablito/StudioProjects/TJ
git push origin main
```

## Step 2: Check GitHub Actions Build & Download `.ipa`

```bash
# Check GitHub Actions build status:
gh run list --repo pikasop303-a11y/tj

# Download the compiled IPA artifact:
gh run download --repo pikasop303-a11y/tj --name TJ-iOS-App --dir ~/Downloads/
```

## Step 3: Verify the Signed `.ipa` File

```bash
# Verify the signed IPA file location and size (~12 MB):
ls -lh ~/Downloads/TJ-signed.ipa
```

## Step 4: USB Device Pair & Validation (Ubuntu)

```bash
# 1. Ensure USB deployment tools are installed:
sudo apt install usbmuxd libimobiledevice6 libimobiledevice-utils ideviceinstaller

# 2. Pair iPhone with Ubuntu over USB (Tap 'Trust' on iPhone screen):
idevicepair pair

# 3. Validate pairing status:
idevicepair validate
# Output: SUCCESS: Validated pairing with device 00008030-000E415C1446802E
```

## Step 5: Detect Connected Device UDID

```bash
# List connected device UDID:
idevice_id -l
# Output: 00008030-000E415C1446802E
```

## Step 6: Install `.ipa` File onto iPhone

```bash
# Install the signed IPA directly onto your iPhone:
ideviceinstaller -u 00008030-000E415C1446802E -i ~/Downloads/TJ-signed.ipa
# Output: Install: InstallComplete (100%)
```

## Step 7: Verify App Installation on Device

```bash
# Verify bundle ID com.tpsk.TJ on your iPhone:
ideviceinstaller -u 00008030-000E415C1446802E -l | grep com.tpsk.TJ
# Output: com.tpsk.TJ.PNX9HSN9VY, "1", "TJ"
```

## Step 8: Trust App & Launch on iPhone

1. Open iPhone **Settings → General → VPN & Device Management**.
2. Tap your Developer profile under **Developer App** and tap **Trust**.
3. Enable **Developer Mode** (iOS 16+) in **Settings → Privacy & Security → Developer Mode** and restart your device.
4. Launch **TJ** on your home screen!
