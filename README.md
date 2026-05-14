# 🏃 מעקב בריאות — APK עם תכונות מערכת מלאות

## ✨ מה חדש בגרסה 2.0

האפליקציה עכשיו עם **גישה מלאה למערכת Android** דרך Native Bridge:

| תכונה | איך זה עובד |
|------|------------|
| 🔔 **התראות מערכת** | התראות אמיתיות של Android (לא Web Notifications) - עובדות גם כשהאפליקציה סגורה |
| 💾 **ייצוא קבצים** | נשמר ישירות לתיקיית ההורדות + Share Sheet |
| 🔗 **Google Fit OAuth** | Chrome Custom Tab עם deep link callback |
| 📳 **רטט** | Vibration API של Android |
| 🍞 **Toasts** | הודעות מערכת של Android |

---

## 🚀 איך להוציא APK תוך 5 דקות

### צעד 1 — צור Repository חדש ב-GitHub
1. כנס ל-[github.com](https://github.com) → **New repository**
2. שם: `bmi4-v2` (או כל שם)
3. **Create repository**

### צעד 2 — העלה את הקבצים
1. ב-repo החדש → **uploading an existing file**
2. **גרור את כל התיקייה הזו** (אבל את התוכן של `bmi4-project/`, לא את התיקייה עצמה!)
3. **Commit changes**

### צעד 3 — חכה לבנייה (3-5 דקות)
1. לחץ **Actions** בתפריט העליון
2. תראה תהליך רץ 🟡 → ירוק ✅
3. לחץ עליו → גלול ל-**Artifacts**
4. הורד **HealthTracker-APK** (קובץ ZIP)

### צעד 4 — התקן בטלפון
1. חלץ את ה-ZIP → קבל `HealthTracker.apk`
2. **הסר את ה-APK הישן** (חשוב!)
3. התקן את החדש
4. בעת הפתיחה הראשונה - **אשר הרשאות**:
   - 🔔 התראות
   - 💾 גישה לאחסון

---

## 📋 הגדרת Google Cloud Console

**Authorized redirect URIs** — חובה לעדכן:
```
https://rotempavel1-svg.github.io/bmi4/index.html
https://rotempavel1-svg.github.io/bmi4/
```

**Authorized JavaScript origins:**
```
https://rotempavel1-svg.github.io
```

---

## 🎯 איך עובד החיבור לגוגל פיט בAPK

```
1. אתה לוחץ "התחבר ל-Google Fit" באפליקציה
   ↓
2. AndroidBridge פותח Chrome Custom Tab (לא WebView!)
   ↓
3. Chrome נפתח עם דף ההתחברות של גוגל
   ↓
4. אתה מתחבר ומאשר הרשאות
   ↓
5. גוגל מפנה אל: rotempavel1-svg.github.io/bmi4/...#access_token=XYZ
   ↓
6. Android רואה את ה-URL ומפעיל את האפליקציה (deep link)
   ↓
7. MainActivity מקבל את ה-token ומזריק אותו ל-WebView
   ↓
8. ✅ מחובר!
```

---

## 🔔 איך עובדות ההתראות

האפליקציה משתמשת ב-**Android Notification System** האמיתי:
- ✅ עובדות גם כשהאפליקציה סגורה
- ✅ מופיעות בpanel ההתראות
- ✅ עם רטט וצליל
- ✅ לחיצה פותחת את האפליקציה

**JS API:**
```javascript
window.AndroidBridge.showNotification(title, body, tag);
window.AndroidBridge.hasNotificationPermission();
window.AndroidBridge.requestNotificationPermission();
```

---

## 💾 איך עובד ייצוא קבצים

קבצים נשמרים **ישירות בתיקיית ההורדות**:
- ✅ נראים מיד באפליקציית "קבצים"
- ✅ אפשר לפתוח/לשתף דרך Share Sheet
- ✅ Toast מאשר שהקובץ נשמר

**JS API:**
```javascript
window.AndroidBridge.saveFile('export.csv', 'content...', 'text/csv');
```

---

## 🐛 פתרון בעיות

### ❌ Build נכשל ב-GitHub Actions
- בדוק את הלוג של Actions לפרטים
- ודא שכל הקבצים הועלו במיקום הנכון

### ❌ "Google blocked OAuth"
- ודא שעדכנת את **Authorized URIs** בGoogle Cloud Console
- הקישור חייב להיות **בדיוק** `https://rotempavel1-svg.github.io/bmi4/index.html`

### ❌ "התראות לא עובדות"
- ודא שאישרת הרשאות בעת הפתיחה הראשונה
- הגדרות → אפליקציות → מעקב בריאות → הרשאות → התראות (אשר)

### ❌ "Open with: [App]" dialog
- בחר את האפליקציה ולחץ "Always"

---

## 🎯 מבנה הפרויקט

```
bmi4-project/
├── .github/workflows/build.yml       ← GitHub Actions
├── app/
│   ├── build.gradle                  ← הגדרות בנייה + dependencies
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml       ← הרשאות + deep links + FileProvider
│       ├── assets/
│       │   ├── index.html            ← האפליקציה (עם AndroidBridge)
│       │   ├── manifest.json
│       │   ├── sw.js
│       │   └── icon-*.png
│       ├── java/com/rotempavel/bmi4/
│       │   └── MainActivity.java     ← הליבה - WebView + Native Bridge
│       └── res/
│           ├── mipmap-*/             ← אייקונים
│           ├── values/               ← strings + colors
│           └── xml/file_paths.xml    ← FileProvider config
├── build.gradle                      ← project-level
├── settings.gradle
├── gradle.properties
└── .gitignore
```

✅ **הכל מוכן! העלה ל-GitHub וקבל APK שעובד!**
