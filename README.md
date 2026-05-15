# HealthTracker — מעקב בריאות

אפליקציית מעקב משקל, קלוריות, מים, צעדים, שינה ועוד — עטופה ב-WebView של אנדרואיד עם גישה ל-Google Fit, התראות מקומיות ועוד.

## מבנה הפרויקט

```
HealthTracker/
├── .github/workflows/build-apk.yml    ← GitHub Actions: בונה APK אוטומטית
├── .gitignore
├── settings.gradle                    ← הגדרות Gradle הראשיות
├── gradle.properties
└── app/
    ├── build.gradle                   ← הגדרות בנייה של האפליקציה
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/rotempavel/bmi4/
        │   └── MainActivity.java       ← WebView wrapper, Bridge, OAuth, התראות
        ├── res/
        │   ├── values/                 ← strings.xml, colors.xml
        │   └── mipmap-*/               ← אייקוני האפליקציה
        └── assets/
            ├── index.html              ← האפליקציה עצמה (~3,900 שורות)
            ├── manifest.json           ← PWA manifest
            ├── sw.js                   ← Service Worker
            ├── icon-192.png
            └── icon-512.png
```

## איך לבנות APK דרך GitHub

1. **צור Repository חדש** (או השתמש בקיים שלך).
2. **העלה את כל הקבצים** מה-ZIP הזה לתוך השורש של ה-Repo.
3. **דחוף לענף main** (או master).
4. עבור ל-**Actions** ב-GitHub — אמורה להופיע ריצה בשם "Build APK".
5. כשהיא מסתיימת בהצלחה, גלול למטה ולחץ על **Artifacts → HealthTracker-debug-XX** — שם ה-APK.

### יצירת Release רשמי (אופציונלי)

אם תדחוף **תג** שמתחיל ב-`v` (למשל `v1.0` או `v2.1`), ה-workflow ייצור אוטומטית Release בעמוד ה-Releases של ה-Repo עם ה-APK מצורף. דוגמה:

```bash
git tag v2.0
git push origin v2.0
```

## עריכת הקוד

- **שינויים באפליקציה עצמה** (UI, פיצ'רים, לוגיקה): `app/src/main/assets/index.html`
- **שינויים בעטיפת אנדרואיד** (התראות native, Bridge, OAuth): `app/src/main/java/com/rotempavel/bmi4/MainActivity.java`
- **שינוי שם האפליקציה**: `app/src/main/res/values/strings.xml`
- **שינוי צבעים**: `app/src/main/res/values/colors.xml`
- **שינוי הרשאות**: `app/src/main/AndroidManifest.xml`
- **שינוי קוד גרסה / שם גרסה** (לפני release חדש): `app/build.gradle` — `versionCode` ו-`versionName`

## דרישות מערכת

- Android 7.0 (API 24) ומעלה
- אינטרנט (לסנכרון Google Fit ו-AI; שאר הפיצ'רים עובדים אופליין)

## הערות

- ה-APK שנבנה הוא **debug** ולא חתום למסחר — מספיק להתקנה אישית. ל-Google Play צריך build חתום (`assembleRelease` עם keystore).
- כשמעדכנים גרסה, חשוב להעלות את `versionCode` (מספר שלם עולה) ולא רק את `versionName`.
- שינויים ב-`index.html` משפיעים גם על גרסת ה-PWA ב-GitHub Pages (אם פרוסה).
