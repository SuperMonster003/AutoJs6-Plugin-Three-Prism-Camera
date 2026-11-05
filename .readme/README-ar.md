<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>التقاط الصور وتحليل الإطارات المتتابعة ومعاينة العدسة عبر CameraX مع تمرير الإطارات مباشرة إلى وحدات التعرف مثل OCR والرموز الشريطية و YOLO</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

⁨3-Prism Camera⁩ هي إضافة الكاميرا في AutoJs6. تعتمد على Jetpack CameraX وتوفر للنصوص البرمجية التقاط الصور وتحليل الإطارات المتتابعة ومعاينة العدسة. تصل الإطارات الحية إلى النص البرمجي ككائنات `ImageWrapper` من AutoJs6, لذا يمكن تمريرها مباشرة إلى الوحدات الموجودة مثل `ocr` أو `barcode` أو `yolo` لمسح الرموز وقراءة النصوص واكتشاف الأجسام في الوقت الحقيقي.

تمتلك الإضافة الكاميرا وتتواصل مع AutoJs6 عبر Binder: تنتقل الإطارات الحية عبر مخزن حلقي في ذاكرة مشتركة (ملفات مؤقتة على Android 7.0 - 8.0), وتصل الصور إلى AutoJs6 بصيغة JPEG لفك ترميزها أو حفظها كما هي, وتُعرض المعاينة مباشرة في `SurfaceView` ضمن واجهة النص البرمجي فلا تمر أي بكسلات عبر Binder. وما دامت الجلسة مفتوحة تشغّل الإضافة خدمة كاميرا في المقدمة, فتواصل النصوص البرمجية تلقي الإطارات في الخلفية أو والشاشة مطفأة.

> هذا إصدار معاينة للتطوير: يتعرف مركز الإضافات في AutoJs6 على الإضافة, ويستطيع AutoJs6 الآن استخدام نواة الكاميرا (فتح الكاميرا والإطارات الحية والتقاط الصور والمعاينة وعناصر التحكم), لكن شاشة التصوير ووحدة النصوص البرمجية `camera` لم تُسلَّم بعد. تصف الأقسام التالية أهداف تصميم الإصدار 1.0.0, ويمكن متابعة التقدم في خارطة الطريق.

******

### أبرز الميزات

******

- الصور: تُعيد `ImageWrapper` معدَّلًا وفق اتجاه EXIF, أو تحفظ ملف JPEG الأصلي (مع EXIF) في المسار الذي تختاره; ويمكن ضبط وضع الفلاش والجودة والدقة.
- الإطارات الحية: إطارات RGBA بدقة 640x480 افتراضيًا وحتى 1920x1080, ومعدَّلة وفق اتجاه الشاشة افتراضيًا; وعندما يتأخر النص البرمجي تُسقط الإطارات وتُعدّ بدل أن تتراكم.
- المعاينة: سلّم الجلسة `SurfaceView` من نص ui أو من نافذة عائمة لعرض العدسة, بالتزامن مع الإطارات الحية والتقاط الصور.
- التحكم: اختيار العدسة والدقة ومعدل الإطارات المستهدف والمصباح ووضع الفلاش والتكبير وتعويض التعريض والتركيز باللمس, مع قراءة النطاقات والحالة الحالية.
- شاشة التصوير: يفتح `camera.capture({ ui: true })` شاشة التصوير الخاصة بالإضافة; ويتلقى النص البرمجي الصورة عندما يضغط المستخدم زر الالتقاط, أو `null` عند الإلغاء.
- العمل في الخلفية: تعمل خدمة كاميرا في المقدمة ما دامت الجلسة مفتوحة, فتواصل الإطارات الوصول في الخلفية أو والشاشة مطفأة; وتُحرَّر الكاميرا عند انتهاء النص البرمجي أو إيقاف AutoJs6.

******

### الاستخدام

******

1. نزّل من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) ملف APK المطابق لـ ABI الجهاز وثبّته: تحتاج معظم الهواتف إلى `arm64-v8a`; وإن لم تكن متأكدًا فاختر `universal` الذي يعمل على جميع الأجهزة لكنه أكبر حجمًا.
2. افتح الإضافة وامنح إذن الكاميرا عند الطلب; ويمكن للنصوص البرمجية أيضًا استدعاء `camera.requestPermission()` لفتح شاشة الأذونات.
3. لاستخدام الوحدة `camera` في النصوص البرمجية, حدّث AutoJs6 إلى الإصدار الداخلي 6641 (6.8.1) أو أحدث, وفعّل هذه الإضافة في مركز الإضافات.

> تدعم الإضافة الأجهزة التي تعمل بنظام Android 7.0 (API 24) أو أحدث. لكل ABI ملف APK خاص به, إضافة إلى حزمة `universal` لجميع الأجهزة; والمكتبات الأصلية القليلة المرفقة مع CameraX و Compose (نحو 50 KB لكل ABI) محاذاة لصفحات ذاكرة 16 KB, لذا تُثبَّت الإضافة أيضًا على الأجهزة الحديثة ذات صفحات 16 KB.

******

### واجهة برمجة النصوص

******

تستخدم النصوص البرمجية هذه الميزات عبر الوحدة العامة `camera` (متاحة أيضًا باسم `$camera`; وهذه مسودة تصميم للإصدار 1.0.0, والمرجع هو توثيق الإصدار المنشور):

```javascript
// Take one photo and keep the original JPEG.
let photo = camera.capture({ path: files.join(files.getSdcardPath(), "prism.jpg") });
toastLog(photo.getWidth() + "x" + photo.getHeight());
photo.recycle();

// Scan barcodes from live frames; a frame is valid only inside the callback.
let session = camera.open({ resolution: "1280x720" });
session.analyze((img, meta) => {
    let codes = barcode.detect(img);
    if (codes.length > 0) {
        toastLog(codes[0].rawValue);
        session.close();
    }
});
events.on("exit", () => session.close());

// Let the user take the picture on the plugin's capture screen; cancelling returns null.
let shot = camera.capture({ ui: true });
```

- يفتح `camera.capture(options)` الكاميرا ويلتقط ويغلق في خطوة واحدة; ويفتح `camera.open(options)` جلسة حصرية يوفر كائنها `preview` و `analyze` و `capture` و `torch` و `zoom` و `exposure` و `focusAt` و `state` و `close`.
- عمر الإطار: يكون `img` في دالة `analyze` صالحًا أثناء تنفيذ الدالة فقط, ويُستعاد فور عودتها; ولاستخدامه بعد ذلك استدعِ `img.clone()` ثم نفّذ `recycle()` على النسخة بنفسك.
- يسرد `camera.list()` العدسات مع دقاتها ونطاقات التكبير والتعريض; ويُعيد `camera.capabilities()` إصدار العقد وقائمة الميزات وحالة الإذن.
- جلسة واحدة في كل مرة: يفشل `open` الثاني بالرمز `CAMERA_BUSY` مع ذكر الجهة الشاغلة; وتُغلق الجلسة عند انتهاء النص البرمجي.

******

### الأسئلة الشائعة

******

#### لماذا جاءت الكاميرا إضافة بدل أن تكون جزءًا من AutoJs6?

تحتاج الكاميرا إلى CameraX وأذونات خدمة المقدمة. وكونها إضافة يعني أن المستخدمين الذين لا يحتاجون إليها لا يتحملون حجمها ولا أذوناتها, ويمكن تحديث الإضافة بمعزل عن AutoJs6.

#### هل يمكن للنصوص البرمجية تلقي الإطارات في الخلفية?

نعم. ما دامت الجلسة مفتوحة تشغّل الإضافة خدمة كاميرا في المقدمة ويُظهر الإشعار أن الكاميرا قيد الاستخدام. تفرض بعض أنظمة الشركات المصنعة قيودًا إضافية على الكاميرا في الخلفية; راجع مصفوفة الأجهزة في خارطة الطريق.

#### هل تحفظ الإضافة الصور في المعرض?

لا. لا تطلب الإضافة أي إذن تخزين ولا تكتب أبدًا في معرض النظام; ولا يكتب AutoJs6 الصورة إلا في المسار `path` الذي يمرره النص البرمجي.

******

### الأذونات والأمان

******

تبقي الإضافة أذوناتها في أضيق نطاق ممكن:

- نقاط دخول محمية: تتطلب خدمة القدرات ونقطة التفعيل إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`), فلا تستطيع التطبيقات الأخرى استدعاءها; ولا تُصدَّر خدمة المقدمة ولا شاشة البدء الشفافة.
- الكاميرا: لا تُطلب سوى `CAMERA` وأذونات خدمة الكاميرا في المقدمة, وعلى Android 13 أو أحدث إذن الإشعارات لعرض إشعار استخدام الكاميرا; ولا تُستخدم الكاميرا إلا أثناء جلسة مفتوحة, وتُحرَّر فور إغلاق الجلسة أو انتهاء النص البرمجي أو إيقاف AutoJs6.
- الشبكة والتخزين: لا أذونات للشبكة أو التخزين; تنتقل الإطارات والصور بين الإضافة و AutoJs6 فقط ولا تصل أبدًا إلى معرض النظام.
- الاستخدام الحصري: جلسة واحدة في كل مرة (نص برمجي أو شاشة التصوير في الإضافة), وتذكر رسالة الخطأ الجهة الشاغلة.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون والتفاوض على القدرات:

```text
application id: io.github.supermonster003.autojs6.plugin.three.prism.camera
plugin id: three-prism-camera
engine: camera
variant: default
info service: org.autojs.plugin.INFO (category three-prism-camera)
capability service: org.autojs.plugin.CAMERA (category camera)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libimage_processing_util_jni.so, libsurface_util_jni.so (CameraX), libandroidx.graphics.path.so (Compose); one APK per ABI plus a universal APK, stored uncompressed and 16 KB page-aligned
```

******

### خارطة الطريق

******

تتابع الخطط وقرارات التصميم والتقدم في شكل قائمة تحقق في ROADMAP.md. العناصر غير المحددة خطط وليست ميزات في الإصدار الحالي.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/11/05_

- `تلميح` معاينة للتطوير: يستطيع AutoJs6 الآن استخدام نواة الكاميرا (فتح الكاميرا والإطارات الحية والتقاط الصور والمعاينة وعناصر التحكم), ولم تُسلَّم بعد شاشة التصوير ووحدة السكربت camera
- `تلميح` يتطلب AutoJs6 6.8.1 (6641) أو أحدث (سيُحدَّد الحد الأدنى الدقيق للإصدار بعد اكتمال تغييرات المضيف)
- `تلميح` الأيقونة الحالية تصميم مرشح وقد تتغير لاحقا
- `ميزة` هيكل الإضافة: بروتوكول التفعيل Wake وخدمة الهوية في مركز الإضافات
- `ميزة` خدمة قدرات الكاميرا: لا يستطيع استخدام الكاميرا إلا جلسة واحدة في كل مرة, وعند انشغالها تُذكر الجهة الشاغلة; ويظهر إشعار في المقدمة ما دامت الكاميرا قيد الاستخدام, وعند خروج AutoJs6 تُحرَّر الكاميرا فورا ويختفي الإشعار
- `ميزة` الإطارات الحية: تصل إطارات RGBA إلى AutoJs6 بالتناوب عبر 3 كتل من الذاكرة المشتركة (Android 8.1 أو أحدث) أو 3 ملفات مؤقتة (Android 7.0 - 8.0), بالدقة ومعدل الإطارات المختارين; وتُتخطّى الإطارات التي لا يمكن معالجتها في الوقت المناسب وتُعدّ
- `ميزة` التقاط الصور: تصل صورة JPEG إلى AutoJs6 كملف للقراءة فقط مع أبعادها واتجاه EXIF وعدد البايتات وما إذا كانت معكوسة; ويمكن ضبط الجودة والدقة ووضع الفلاش
- `ميزة` المعاينة: تُعرض الصورة على سطح يوفره AutoJs6 وتتبع تدوير الشاشة; وتكون معاينة العدسة الأمامية معكوسة افتراضيا, أما الإطارات والصور فلا; وعندما يختفي السطح الذي يعرض المعاينة تتوقف المعاينة تلقائيا, ويستمر التقاط الإطارات والصور
- `ميزة` عناصر التحكم: المصباح, ووضع الفلاش, والتكبير (بالنسبة أو خطيا), وتعويض التعريض, والتركيز والقياس باللمس; وما لا يدعمه الجهاز (مثل فلاش العدسة الأمامية) يُرجع UNSUPPORTED
- `ميزة` إذن الكاميرا: يُرجع فتح الكاميرا دونه PERMISSION_DENIED ويمكن طلب الإذن; وعلى Android 13 أو أحدث يُطلب إذن الإشعارات أيضا لعرض إشعار استخدام الكاميرا
- `ميزة` حزمة لكل ABI (arm64-v8a و armeabi-v7a و x86_64 و x86) وحزمة universal لجميع الأجهزة; والمكتبات الأصلية المرفقة مع CameraX و Compose محاذاة لصفحات ذاكرة 16 KB
- `ميزة` ملفات README وتعليمات مركز الإضافات وسجلات التغييرات بعشر لغات, مولَّدة من مصادر JSON
- `تبعية` إرفاق Jetpack CameraX 1.6.2 (لفتح الكاميرا والتقاط الصور وتقديم الإطارات والعدسة)
- `تبعية` إرفاق Jetpack Compose BOM 2026.09.00 (شاشات الإضافة)
- `تبعية` إرفاق AndroidX Activity Compose 1.12.2
- `تبعية` إرفاق AndroidX Core KTX 1.16.0

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

هذا القسم موجه للمطورين الراغبين في بناء المكون من المصدر.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### الترخيص

******

شيفرة المشروع مرخصة بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). راجع إشعارات الأطراف الثالثة لمعرفة تراخيص مكوناتها.

- [إشعارات الأطراف الثالثة (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
