#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
SDK="${ANDROID_HOME:-/workspace/android-sdk}"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35/android.jar"
BUILD="$PWD/build/android"
rm -rf "$BUILD/classes" "$BUILD/dex" "$BUILD/assets" "$BUILD/generated"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$BUILD/assets/web" "$PWD/dist" /workspace/.uroki
cp public/* "$BUILD/assets/web/"
"$BT/aapt2" compile --dir android/res -o "$BUILD/resources.zip"
"$BT/aapt2" link -o "$BUILD/base.apk" "$BUILD/resources.zip" -I "$PLATFORM" --java "$BUILD/generated" --manifest android/AndroidManifest.xml -A "$BUILD/assets" --min-sdk-version 26 --target-sdk-version 35
java -jar /workspace/tools/ecj.jar -encoding UTF-8 -source 8 -target 8 -classpath "$PLATFORM" -d "$BUILD/classes" android/src/ru/uroki/app/*.java "$BUILD/generated/ru/uroki/app/R.java"
find "$BUILD/classes" -name '*.class' -print0 | xargs -0 "$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$BUILD/dex"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
python - "$BUILD/unsigned.apk" "$BUILD/dex/classes.dex" <<'PYZIP'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1],'a',compression=zipfile.ZIP_DEFLATED) as z:z.write(sys.argv[2],'classes.dex')
PYZIP
"$BT/zipalign" -f -p 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
# Local development signing key: retained outside the repository so rebuilds can update the app.
KEY="${UROKI_KEYSTORE:-/workspace/.uroki/development.keystore}"
if [ ! -f "$KEY" ]; then
 keytool -genkeypair -keystore "$KEY" -storepass android -keypass android -alias uroki -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Uroki Development' -noprompt
fi
"$BT/apksigner" sign --ks "$KEY" --ks-pass pass:android --key-pass pass:android --ks-key-alias uroki --out dist/uroki.apk "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose dist/uroki.apk
sha256sum dist/uroki.apk
