<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Фотосъёмка, непрерывный анализ кадров и предпросмотр видоискателя через CameraX, кадры можно сразу передавать модулям распознавания OCR, штрихкодов, YOLO и другим</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Prism Camera - это плагин камеры для AutoJs6. Он построен на Jetpack CameraX и даёт скриптам фотосъёмку, непрерывный анализ кадров и предпросмотр видоискателя. Живые кадры приходят в скрипт как объекты `ImageWrapper` AutoJs6, поэтому их можно сразу передать существующим модулям, таким как `ocr`, `barcode` или `yolo`, для сканирования кодов в реальном времени, распознавания текста и обнаружения объектов.

Плагин владеет камерой и общается с AutoJs6 через Binder: живые кадры передаются через кольцевой буфер в общей памяти (на Android 7.0 - 8.0 через временные файлы), фотографии приходят в AutoJs6 в формате JPEG для декодирования или сохранения без изменений, а предпросмотр выводится прямо в `SurfaceView` интерфейса скрипта, так что пиксели не проходят через Binder. Пока сеанс открыт, плагин держит службу камеры переднего плана, поэтому скрипты продолжают получать кадры в фоне и при выключенном экране.

> Это предварительная версия для разработки: центр плагинов AutoJs6 распознаёт плагин, и AutoJs6 уже может пользоваться ядром камеры (открытие камеры, живые кадры, фотосъёмка, предпросмотр и управление), но экран съёмки и модуль скриптов `camera` ещё не готовы. Ниже описаны цели проектирования версии 1.0.0; ход работ см. в дорожной карте.

******

### Основные Возможности

******

- Фото: возвращает `ImageWrapper`, выровненный по ориентации EXIF, или сохраняет исходный JPEG (с EXIF) по выбранному пути; режим вспышки, качество и разрешение настраиваются.
- Живые кадры: кадры RGBA по умолчанию 640x480, максимум 1920x1080, по умолчанию выровнены по ориентации экрана; если скрипт не успевает, кадры отбрасываются и подсчитываются, а не копятся в очереди.
- Предпросмотр: передайте сеансу `SurfaceView` из ui-скрипта или плавающего окна, чтобы показать видоискатель одновременно с живыми кадрами и фотосъёмкой.
- Управление: выбор объектива, разрешение, целевая частота кадров, фонарик, режим вспышки, масштаб, экспокоррекция и фокусировка касанием, с чтением диапазонов и текущего состояния.
- Экран съёмки: `camera.capture({ ui: true })` открывает собственный экран съёмки плагина; скрипт получает фото, когда пользователь нажимает затвор, или `null` при отмене.
- Работа в фоне: пока сеанс открыт, работает служба камеры переднего плана, поэтому кадры поступают в фоне и при выключенном экране; камера освобождается при завершении скрипта или остановке AutoJs6.

******

### Использование

******

1. Скачайте из [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) APK для ABI устройства и установите его: большинству телефонов нужен `arm64-v8a`; если не уверены, выберите `universal`, он подходит для всех устройств, но больше по размеру.
2. Откройте плагин и выдайте разрешение на камеру по запросу; скрипты также могут вызвать `camera.requestPermission()`, чтобы открыть экран разрешений.
3. Чтобы использовать модуль `camera` в скриптах, обновите AutoJs6 до сборки 6641 (6.8.1) или новее и включите этот плагин в центре плагинов.

> Плагин поддерживает устройства с Android 7.0 (API 24) и новее. Для каждого ABI есть свой APK, а также пакет `universal` для всех устройств; несколько нативных библиотек, входящих в CameraX и Compose (около 50 KB на ABI), выровнены по страницам памяти 16 KB, поэтому плагин устанавливается и на новые устройства со страницами 16 KB.

******

### API Скриптов

******

Скрипты используют эти возможности через глобальный модуль `camera` (также доступен как `$camera`; это черновик проекта 1.0.0, окончательной считается документация выпущенной версии):

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

- `camera.capture(options)` открывает камеру, снимает и закрывает её за один шаг; `camera.open(options)` открывает монопольный сеанс, объект которого предоставляет `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` и `close`.
- Время жизни кадра: `img` в обработчике `analyze` действителен только во время выполнения обработчика и освобождается сразу после возврата; чтобы использовать его позже, вызовите `img.clone()` и сами выполните `recycle()` для копии.
- `camera.list()` перечисляет объективы с их разрешениями, диапазонами масштаба и экспозиции; `camera.capabilities()` возвращает версию контракта, список функций и состояние разрешения.
- Только один сеанс одновременно: повторный `open` завершается ошибкой `CAMERA_BUSY` с указанием владельца; сеанс закрывается при завершении скрипта.

******

### Частые Вопросы

******

#### Почему камера сделана плагином, а не частью AutoJs6?

Камере нужны CameraX и разрешения службы переднего плана. В виде плагина пользователи, которым камера не нужна, не несут ни её размер, ни разрешения, а сам плагин обновляется независимо от AutoJs6.

#### Могут ли скрипты получать кадры в фоне?

Да. Пока сеанс открыт, плагин держит службу камеры переднего плана, а уведомление показывает, что камера используется. Некоторые системы производителей дополнительно ограничивают камеру в фоне; совместимость см. в матрице устройств в дорожной карте.

#### Сохраняет ли плагин фото в галерею?

Нет. Плагин не запрашивает разрешений на хранилище и не пишет в системную галерею; AutoJs6 записывает фото только по пути `path`, который передал скрипт.

******

### Разрешения И Безопасность

******

Плагин запрашивает минимально необходимые разрешения:

- Защищённые точки входа: служба возможностей и точка активации требуют разрешения плагинов AutoJs6 (`org.autojs.permission.PLUGIN`), поэтому другие приложения не могут их вызвать; служба переднего плана и прозрачный стартовый экран не экспортируются.
- Камера: запрашиваются только `CAMERA`, разрешения службы камеры переднего плана и, на Android 13 и новее, разрешение на уведомления для уведомления об использовании камеры; камера используется только при открытом сеансе и освобождается сразу после закрытия сеанса, завершения скрипта или остановки AutoJs6.
- Сеть и хранилище: разрешения на сеть и хранилище не запрашиваются; кадры и фото передаются только между плагином и AutoJs6 и не попадают в системную галерею.
- Монопольное использование: одновременно открыт только один сеанс (скрипт или экран съёмки плагина), а владелец указывается в сообщении об ошибке.

******

### Интерфейс Плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования возможностей:

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

### Дорожная Карта

******

Планы, проектные решения и ход работ ведутся в виде списка с отметками в ROADMAP.md. Неотмеченные пункты являются планами, а не возможностями текущей версии.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### История Выпусков

******

#### v1.0.0

_2026/11/05_

- `Подсказка` Предварительная версия для разработки: AutoJs6 уже может пользоваться ядром камеры (открытие камеры, живые кадры, фотосъёмка, предпросмотр и управление); экран съёмки и модуль скриптов camera ещё не готовы
- `Подсказка` Требуется AutoJs6 6.8.1 (6641) или новее (точная минимальная версия будет указана после внесения изменений в хост)
- `Подсказка` Текущий значок - вариант-кандидат и ещё может измениться
- `Функция` Каркас плагина: протокол активации Wake и служба идентификации для центра плагинов
- `Функция` Служба возможностей камеры: камерой может пользоваться только один сеанс одновременно, а занятая камера сообщает владельца; пока камера используется, показывается уведомление переднего плана, а после выхода AutoJs6 камера сразу освобождается и уведомление исчезает
- `Функция` Живые кадры: кадры RGBA поочерёдно передаются в AutoJs6 через 3 блока общей памяти (Android 8.1 и новее) или 3 временных файла (Android 7.0 - 8.0) с выбранными разрешением и частотой; кадры, которые не успевают обработать, пропускаются и подсчитываются
- `Функция` Фотосъёмка: JPEG передаётся в AutoJs6 как файл только для чтения вместе с размерами, ориентацией EXIF, числом байтов и признаком зеркального отражения; можно задать качество, разрешение и режим вспышки
- `Функция` Предпросмотр: изображение выводится на поверхность, которую предоставляет AutoJs6, и следует за поворотом экрана; предпросмотр фронтальной камеры по умолчанию зеркальный, кадры и фото - нет
- `Функция` Управление: фонарик, режим вспышки, зум (кратность или линейный), экспокоррекция, фокусировка и замер по касанию; то, что устройство не поддерживает (например, вспышка фронтальной камеры), возвращает UNSUPPORTED
- `Функция` Разрешение камеры: открытие камеры без него возвращает PERMISSION_DENIED, разрешение можно запросить; на Android 13 и новее заодно запрашивается разрешение на уведомления - для уведомления об использовании камеры
- `Функция` Отдельный пакет для каждого ABI (arm64-v8a, armeabi-v7a, x86_64, x86) и пакет universal для всех устройств; нативные библиотеки, входящие в CameraX и Compose, выровнены по страницам памяти 16 KB
- `Функция` README, инструкции для центра плагинов и журналы изменений на 10 языках, создаваемые из исходников JSON
- `Зависимость` Добавлен Jetpack CameraX 1.6.2 (открывает камеру, делает фото, передаёт кадры и видоискатель)
- `Зависимость` Добавлен Jetpack Compose BOM 2026.09.00 (экраны плагина)
- `Зависимость` Добавлен AndroidX Activity Compose 1.12.2
- `Зависимость` Добавлен AndroidX Core KTX 1.16.0

##### Больше истории выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходников.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). Лицензии сторонних компонентов см. в уведомлениях о сторонних компонентах.

- [Сведения о сторонних компонентах (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
