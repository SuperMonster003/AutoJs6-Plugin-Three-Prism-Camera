<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Captura de fotos, análisis continuo de fotogramas y vista previa del visor con CameraX, con fotogramas listos para módulos de reconocimiento OCR, de códigos de barras, YOLO y otros</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Prism Camera es el complemento de cámara de AutoJs6. Basado en Jetpack CameraX, ofrece a los scripts captura de fotos, análisis continuo de fotogramas y vista previa del visor. Los fotogramas en vivo llegan al script como objetos `ImageWrapper` de AutoJs6, de modo que el script puede pasarlos directamente a módulos existentes como `ocr`, `barcode` o `yolo` para escanear códigos en vivo, reconocer texto y detectar objetos.

El complemento controla la cámara y se comunica con AutoJs6 por Binder: los fotogramas en vivo viajan por un búfer circular de memoria compartida (archivos temporales en Android 7.0 - 8.0), las fotos llegan a AutoJs6 en JPEG para decodificarlas o guardarlas sin cambios, y la vista previa se dibuja directamente en un `SurfaceView` de la interfaz del script, sin que ningún píxel pase por Binder. Mientras una sesión está abierta, el complemento ejecuta un servicio en primer plano de cámara, así que los scripts siguen recibiendo fotogramas en segundo plano o con la pantalla apagada.

> La versión 1.0.0 es la primera publicación. El módulo de script `camera` requiere AutoJs6 6.8.1 (versión interna 6641) o posterior; el visor y la página de ajustes del complemento no dependen de AutoJs6.

******

### Funciones Destacadas

******

- Fotos: devuelve un `ImageWrapper` enderezado según la orientación EXIF, o guarda el JPEG original (con EXIF) en la ruta que elija; el modo de flash, la calidad y la resolución son configurables.
- Fotogramas en vivo: fotogramas RGBA de 640x480 por defecto (se puede cambiar en los ajustes) y hasta 1920x1080, enderezados según la pantalla por defecto; cuando un script se retrasa, los fotogramas se descartan y se cuentan en lugar de acumularse.
- Vista previa: entregue a la sesión un `SurfaceView` de un script ui o de una ventana flotante para mostrar el visor, junto con los fotogramas en vivo y la captura de fotos.
- Controles: selección de lente, resolución, velocidad de fotogramas objetivo, linterna, modo de flash, zoom, compensación de exposición y enfoque al tocar, con rangos y estado actual legibles.
- Pantalla de captura: al abrir el complemento desde el lanzador se muestra la pantalla de captura, con cambio de lente, linterna, zoom y enfoque al tocar; las fotos tomadas ahí solo se muestran, no se guardan, y el botón de arriba a la derecha abre los ajustes; `camera.capture({ ui: true })` abre la misma pantalla, y el script recibe la foto cuando el usuario pulsa el obturador, o `null` si cancela.
- Funciona en segundo plano: un servicio en primer plano de cámara se ejecuta mientras hay una sesión abierta, así que los fotogramas siguen llegando en segundo plano o con la pantalla apagada; la cámara se libera cuando el script termina o AutoJs6 se detiene.

******

### Uso

******

1. Descargue desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) el APK que corresponde a la ABI del dispositivo e instálelo: la mayoría de los teléfonos necesitan `arm64-v8a`; si no está seguro, elija `universal`, que funciona en todos los dispositivos pero ocupa más.
2. Abra el complemento y conceda el permiso de cámara cuando se solicite; los scripts también pueden llamar a `camera.requestPermission()` para abrir la pantalla de permisos.
3. Para usar el módulo `camera` en scripts, actualice AutoJs6 a la versión interna 6641 (6.8.1) o posterior y active este complemento en el centro de complementos.

> El complemento es compatible con dispositivos con Android 7.0 (API 24) o posterior. Hay un APK por ABI y un paquete `universal` para todos los dispositivos; las pocas bibliotecas nativas que incluyen CameraX y Compose (unos 50 KB por ABI) están alineadas a páginas de memoria de 16 KB, por lo que los dispositivos nuevos con páginas de 16 KB también lo instalan.

******

### Compatibilidad

******

- Android 7.0 (API 24) y posterior, sin restricción de ABI: arm64-v8a, armeabi-v7a, x86_64 y x86 tienen cada uno un paquete, y el paquete `universal` sirve para todos los dispositivos.
- Android 7.0 - 8.0 (API 24 - 26) no tienen memoria compartida, así que los fotogramas llegan a AutoJs6 mediante archivos temporales y el flujo continuo tiene una frecuencia menor que en sistemas más nuevos.
- El módulo de script `camera` requiere AutoJs6 6.8.1 (versión interna 6641) o posterior; las versiones anteriores no tienen el módulo, pero el visor del plugin se sigue abriendo desde el lanzador.
- Algunos sistemas de fabricantes (por ejemplo ColorOS) restringen más la cámara para las apps en segundo plano; el apéndice F de la hoja de ruta registra los resultados por dispositivo.

******

### API De Script

******

Los scripts usan estas funciones mediante el módulo global `camera` (también disponible como `$camera`); la referencia completa está en [Documentación de AutoJs6 > Camera](https://docs.autojs6.com/#/camera):

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

- `camera.capture(options)` abre, captura y cierra en un solo paso; `camera.open(options)` abre una sesión exclusiva cuyo objeto ofrece `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` y `close`.
- Vida de los fotogramas: el `img` de un callback de `analyze` solo es válido mientras se ejecuta el callback y se recicla en cuanto este regresa; para usarlo después, llame a `img.clone()` y haga `recycle()` de la copia usted mismo.
- `camera.list()` enumera las lentes con sus resoluciones y sus rangos de zoom y exposición; `camera.capabilities()` devuelve la versión del contrato, la lista de funciones y el estado del permiso.
- Una sesión a la vez: un segundo `open` falla con `CAMERA_BUSY` e indica quién la ocupa; la sesión se cierra cuando el script termina.

******

### Preguntas Frecuentes

******

#### Por qué la cámara es un complemento y no parte de AutoJs6?

La cámara necesita CameraX y los permisos de servicio en primer plano. Como complemento, los usuarios que no la necesitan no cargan ni con el tamaño ni con los permisos, y el complemento puede actualizarse de forma independiente de AutoJs6.

#### Pueden los scripts recibir fotogramas en segundo plano?

Sí, siempre que la sesión se abra con AutoJs6 en primer plano. Mientras una sesión está abierta, el complemento ejecuta un servicio en primer plano de cámara y la notificación indica que la cámara está en uso. Si AutoJs6 ya está en segundo plano o el teléfono permanece en la pantalla de bloqueo (Android 11 o posterior), el sistema no permite usar la cámara, y abrir la cámara en ese momento informa CAMERA_DISABLED: según el sistema, la apertura falla de inmediato, o la sesión se abre, informa el error y se cierra. Volver a abrirla cuando AutoJs6 regresa al primer plano funciona con normalidad, sin reiniciar el complemento. Algunos sistemas de fabricantes restringen aún más la cámara en segundo plano; consulte la matriz de dispositivos en la hoja de ruta.

#### El complemento guarda las fotos en la galería?

No. El complemento no solicita permisos de almacenamiento y nunca escribe en la galería del sistema; AutoJs6 solo escribe una foto en la `path` que pasa el script.

******

### Permisos Y Seguridad

******

El complemento reduce al máximo sus permisos:

- Puntos de entrada protegidos: el servicio de capacidades y el punto de activación requieren el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), por lo que otras aplicaciones no pueden llamarlos; el servicio en primer plano y la pantalla de inicio transparente no se exportan.
- Cámara: solo se solicitan `CAMERA`, los permisos de servicio en primer plano de cámara y, en Android 13 o posterior, el permiso de notificaciones para la notificación de cámara en uso; la cámara solo se usa mientras hay una sesión abierta y se libera en cuanto la sesión se cierra, el script termina o AutoJs6 se detiene.
- Red y almacenamiento: el permiso de red solo sirve para la búsqueda de actualizaciones de los ajustes (solo se lee el índice oficial de complementos de AutoJs6, búsqueda automática como máximo una vez cada 12 horas y desactivable), y no se pide permiso de almacenamiento; los fotogramas y las fotos solo pasan entre el complemento y AutoJs6, nunca se suben y nunca llegan a la galería del sistema.
- Uso exclusivo: una sesión a la vez (un script o la pantalla de captura del complemento), y el mensaje de error indica quién la ocupa.

******

### Interfaz Del Plugin

******

La siguiente información está dirigida a los desarrolladores del host AutoJs6 y de plugins; el host usa estos identificadores para descubrir el plugin y negociar capacidades:

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

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.0.0

_2026/11/08_

- `Aviso` API de script: [Documentación de AutoJs6 > Camera](https://docs.autojs6.com/#/camera)
- `Aviso` El módulo camera en scripts requiere AutoJs6 6.8.1 (versión interna 6641) o posterior; el visor y la página de ajustes del complemento no dependen de AutoJs6
- `Aviso` El icono actual es un diseño candidato y aún puede cambiar
- `Función` Esqueleto del complemento: protocolo de activación Wake y servicio de identidad del centro de complementos
- `Función` Servicio de capacidad de cámara: solo una sesión puede usar la cámara a la vez y, si está ocupada, se indica quién la ocupa; mientras la cámara está en uso se muestra una notificación en primer plano, y cuando AutoJs6 se cierra la cámara se libera de inmediato y la notificación desaparece
- `Función` Cuando AutoJs6 está en segundo plano o el teléfono permanece en la pantalla de bloqueo (Android 11 o posterior), el sistema no permite usar la cámara: abrir la cámara en ese momento informa CAMERA_DISABLED (según el sistema, la apertura falla de inmediato, o la sesión lo informa primero y luego se cierra), y volver a abrirla cuando AutoJs6 regresa al primer plano funciona con normalidad; una sesión abierta en primer plano sigue entregando fotogramas después de que AutoJs6 pase a segundo plano o se apague la pantalla
- `Función` Fotogramas en vivo: los fotogramas RGBA llegan a AutoJs6 por turnos a través de 3 bloques de memoria compartida (Android 8.1 o posterior) o 3 archivos temporales (Android 7.0 - 8.0), con la resolución y la frecuencia elegidas; los fotogramas se enderezan según la orientación de la pantalla ya en la conversión de formato, así AutoJs6 solo los refleja cuando se pide y cada fotograma tarda menos; los fotogramas que no se pueden procesar a tiempo se omiten y se cuentan
- `Función` Captura de fotos: el JPEG llega a AutoJs6 como un archivo de solo lectura junto con sus dimensiones, la orientación EXIF, el número de bytes y si está reflejado; se pueden ajustar la calidad, la resolución y el modo de flash; en una foto reflejada solo se reescribe la orientación EXIF, sin volver a codificarla; si una cámara no escribe la orientación de la foto (por ejemplo, el emulador de Android 7.0), el complemento escribe la orientación de 90 / 270 grados para que la foto se muestre tal como se sostenía el teléfono (no se puede detectar una omisión de 180 grados)
- `Función` Vista previa: la imagen se muestra en una superficie que proporciona AutoJs6 y sigue la rotación de la pantalla; la vista previa de la lente frontal se refleja por defecto, los fotogramas y las fotos no; cuando desaparece la superficie que muestra la vista previa, esta se detiene sola y los fotogramas y las fotos continúan
- `Función` Controles: linterna, modo de flash, zoom (proporción o lineal), compensación de exposición, enfoque y medición por toque; lo que el dispositivo no admite (como el flash de la lente frontal) devuelve UNSUPPORTED
- `Función` Permiso de cámara: abrir la cámara sin él devuelve PERMISSION_DENIED y se puede solicitar; en Android 13 o posterior también se solicita el permiso de notificaciones, para la notificación de cámara en uso
- `Función` Visor: se abre desde el lanzador y permite cambiar de lente, usar la linterna, hacer zoom con el control deslizante o pellizcando y tocar para enfocar; una foto tomada aquí solo muestra la imagen y su resolución y no se guarda; la imagen de la lente frontal se ve reflejada en pantalla, la foto no
- `Función` camera.capture({ ui: true }) en un script abre el mismo visor: la foto se entrega directamente al script y al cancelar se devuelve null; si el visor no puede abrirse porque AutoJs6 está en segundo plano, la llamada devuelve UI_REQUIRES_FOREGROUND a los 10 segundos en lugar de esperar indefinidamente
- `Función` Cuando un script está usando la cámara, el visor indica cuál es y puede detenerlo para tomar la cámara
- `Función` Ajustes (arriba a la derecha del visor): apariencia (idioma, modo nocturno, color del tema, icono del lanzador), permisos de cámara y notificaciones, cámara predeterminada, tamaño y velocidad de análisis predeterminados, notificación del servicio en primer plano, diagnóstico (versión de CameraX, capacidades de cada lente, quién usa la cámara) e historial de versiones
- `Función` Cuando un script no indica cámara, resolución ni velocidad de fotogramas, se aplican los valores predeterminados de los ajustes (al principio la cámara trasera, 640x480 y la velocidad propia del dispositivo); el valor que indica el script siempre tiene prioridad
- `Función` Búsqueda de actualizaciones: desde los ajustes se ve una versión más reciente publicada en el índice oficial de complementos y sus notas, y se puede ignorar una versión; al abrir los ajustes se busca automáticamente (como máximo una vez cada 12 horas, se puede desactivar); el permiso de red solo sirve para esta búsqueda y el complemento nunca sube imágenes de la cámara ni fotos
- `Función` Un paquete por ABI (arm64-v8a, armeabi-v7a, x86_64, x86) y un paquete universal para todos los dispositivos; las bibliotecas nativas que incluyen CameraX y Compose están alineadas a páginas de memoria de 16 KB
- `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
- `Dependencia` Se agrega Jetpack CameraX 1.6.2 (abre la cámara, toma fotos, entrega fotogramas y el visor)
- `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
- `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
- `Dependencia` Se agrega AndroidX Core KTX 1.16.0
- `Dependencia` Se agrega AndroidX ExifInterface 1.4.2 (escribe la orientación EXIF de las fotos)
- `Dependencia` Se agrega Material Color Utilities (MaterialKolor) 4.1.1 (color del tema de las pantallas del complemento)

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). Las licencias de los componentes de terceros figuran en los avisos de terceros.

- [Avisos de terceros (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
