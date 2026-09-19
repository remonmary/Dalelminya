# دليل أرقام المنيا — نسخة منقحة

## Firebase
- Project ID: `dalel-elminya`
- Android package: `com.aistudio.nazletobeid.dirapp`
- ملف `app/google-services.json` هو إعداد Firebase الخاص بالمشروع.

## Admin
1. فعّل Email/Password في Firebase Authentication.
2. أنشئ حساب الأدمن في Firebase Authentication.
3. حساب الأدمن يجب أن يكون مخولًا حسب Firestore Rules: custom claim (`admin` أو `role`) أو البريد الإداري المسموح في القواعد.
4. لا يوجد زر لتحويل المستخدم إلى Admin داخل التطبيق.
5. لا توجد كلمة مرور Admin داخل APK.

## Firestore
Public numbers are read from `numbers` only when `status == approved` and `isActive == true`.
New submissions are `pending`, inactive, and unverified.
Admin approval updates Firestore first, then Room.

## Local cache
Room stores the public approved data for offline use. Demo users and demo phone numbers are not seeded.

## Build
The project source is included. Build it with a current Android/Gradle environment compatible with the Gradle configuration in `app/build.gradle.kts`.
