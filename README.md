# מעקב בריאות — APK Build

## 🚀 איך להוציא APK תוך 5 דקות

### צעד 1 — צור Repository חדש (או החלף את הישן)

**אפשרות א' — Repo חדש (המלצה!):**
1. כנס ל-[github.com](https://github.com) → **New repository**
2. שם: `bmi4-v2` (או כל שם אחר)
3. **Create repository**

**אפשרות ב' — להחליף את הישן (bmi4):**
1. כנס לrepo הקיים
2. מחק את כל הקבצים (Settings → ...או פשוט מחק קובץ-קובץ)

### צעד 2 — העלה את כל הקבצים האלה
1. בrepo החדש → **uploading an existing file**
2. **גרור את כל התיקייה הזו** (כל הקבצים שבתוכה)
3. ודא שיש לך:
   - `app/` (folder)
   - `.github/workflows/build.yml`
   - `build.gradle`
   - `settings.gradle`
   - `gradle.properties`
   - `.gitignore`
4. **Commit changes**

### צעד 3 — חכה לבנייה (3-5 דקות)
1. לחץ על **Actions** בתפריט העליון
2. תראה תהליך רץ עם נקודה צהובה 🟡
3. חכה שיהפוך לירוק ✅
4. לחץ עליו → גלול למטה → **Artifacts**
5. הורד **HealthTracker-APK** (קובץ ZIP)

### צעד 4 — התקן בטלפון
1. חלץ את ה-ZIP → תקבל `HealthTracker.apk`
2. שלח את ה-APK לטלפון (WhatsApp/Email)
3. **הסר את הAPK הישן** (חשוב!)
4. התקן את החדש
5. פתח → תפריט → Google Fit → התחבר ✅

---

## 🔧 איפה לעדכן URL (אם שינית את שם הrepo)

אם שינית את שם ה-repo שלך מ-`bmi4` למשהו אחר, **עדכן את הקישורים בקבצים:**

**ב-`app/src/main/java/com/rotempavel/bmi4/MainActivity.java` (שורה 16):**
```java
private static final String APP_URL = "https://rotempavel1-svg.github.io/[שם-הריפו-החדש]/index.html";
```

**ב-`app/src/main/AndroidManifest.xml` (שורה 35-37):**
```xml
<data
    android:scheme="https"
    android:host="rotempavel1-svg.github.io"
    android:pathPrefix="/[שם-הריפו-החדש]" />
```

---

## 📋 הגדרת Google Cloud Console

**Authorized redirect URIs:**
```
https://rotempavel1-svg.github.io/bmi4/index.html
https://rotempavel1-svg.github.io/bmi4/
```

**Authorized JavaScript origins:**
```
https://rotempavel1-svg.github.io
```

---

## 🎯 מבנה הפרויקט

```
bmi4-project/
├── .github/
│   └── workflows/
│       └── build.yml              ← GitHub Actions (auto-build)
├── app/
│   ├── build.gradle               ← הגדרות בנייה
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml    ← הרשאות + deep links
│       ├── assets/                ← קבצי WEB (index.html, sw.js)
│       ├── java/com/rotempavel/bmi4/
│       │   └── MainActivity.java  ← קוד Java שעוטף את WebView
│       └── res/                   ← אייקונים + צבעים
├── gradle.properties
├── settings.gradle
├── build.gradle
└── .gitignore
```

---

## 🐛 בעיות נפוצות

### ❌ Build נכשל ב-GitHub Actions
- בדוק שלא חסר אף קובץ
- בדוק את הלוג של ה-Actions לפרטים

### ❌ "Google blocked OAuth"
- ודא שעדכנת את ה-Authorized URIs בGoogle Cloud
- ודא שאתה משתמש ב-APK החדש (לא הישן)

### ❌ "Open with: [App]" dialog
- זה תקין! בחר את האפליקציה ולחץ "Always"

---

## 💡 איך זה עובד?

1. **APK טוען** את האפליקציה מ-GitHub Pages (לא מקומית!)
2. **WebView מתחזה** ל-Chrome רגיל (שינוי User Agent)
3. **OAuth נפתח** ב-Chrome Custom Tab (לא חסום על ידי גוגל)
4. **Token חוזר** דרך deep link אל האפליקציה
5. **JavaScript מקבל** את ה-token ועושה sync

✅ **תוצאה: Google Fit עובד גם בטלפון!**
