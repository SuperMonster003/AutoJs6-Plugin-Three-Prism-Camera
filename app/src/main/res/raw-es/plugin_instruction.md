3-Prism Camera es el complemento de cámara de AutoJs6: los scripts pueden tomar fotos, recibir fotogramas en vivo y mostrar la vista previa del visor. Los fotogramas se pueden pasar directamente a módulos de reconocimiento como `ocr`, `barcode` o YOLO para leer códigos, reconocer texto o detectar objetos en tiempo real.

### Primeros pasos

1. Abre 3-Prism Camera desde el lanzador y toca "Conceder permiso" en el visor para que el complemento pueda usar la cámara.
2. Toca "Tomar foto" para hacer una prueba. La foto solo se muestra en "Vista previa de la foto" y no se guarda.
3. Activa este complemento en el "Centro de plugins" de AutoJs6.
4. Llama al módulo `camera` en un script para tomar fotos o recibir fotogramas, como en los ejemplos de abajo.

### Funciones principales

- **Visor**: al abrir el complemento desde el lanzador aparece el visor, donde puedes "Cambiar de cámara", encender o apagar la linterna, hacer zoom con el control deslizante o pellizcando y tocar la imagen para enfocar.
- **Fotos**: los scripts reciben una imagen enderezada y también pueden guardar el JPEG original en una ruta; con `camera.capture({ ui: true })` es el usuario quien toma la foto en el visor.
- **Fotogramas en vivo**: 640x480 de forma predeterminada y hasta 1920x1080, enderezados según la pantalla; los fotogramas que un script no alcanza a procesar se omiten y se cuentan en lugar de acumularse.
- **Vista previa**: entrega a la sesión un `SurfaceView` de un diseño ui o de una ventana flotante para mostrar la imagen del visor, junto con los fotogramas en vivo y las fotos.
- **Controles**: los scripts controlan la linterna, el modo de flash, el zoom, la compensación de exposición y el enfoque al tocar, y leen el rango y el valor actual de cada uno.
- **Una sesión a la vez**: solo una sesión usa la cámara a la vez. Cuando un script la ocupa, el visor muestra cuál es, y "Detener este script" permite tomar el control de la cámara.
- **Fotogramas en segundo plano**: mientras hay una sesión abierta se muestra la notificación "Cámara en uso", y los fotogramas siguen llegando con AutoJs6 en segundo plano o la pantalla apagada; la cámara se libera en cuanto termina el script.
- **Valores predeterminados**: en "Ajustes", arriba a la derecha del visor, define "Cámara predeterminada", "Tamaño de análisis predeterminado" y "Velocidad de análisis predeterminada"; los valores que pasa un script siempre tienen prioridad.
- **Diagnóstico y actualizaciones**: la sección "Diagnóstico" de los ajustes muestra lo que admite cada cámara y el "Titular actual"; "Buscar actualizaciones" busca versiones nuevas en el índice oficial de complementos.
- **Apariencia**: "Idioma", "Modo nocturno" y "Color del tema" siguen a AutoJs6 de forma predeterminada y también se pueden ajustar por separado; además se puede cambiar el "Icono del lanzador".

### Uso en scripts

Los scripts Rhino de AutoJs6 usan la cámara mediante el módulo global `camera`. El ejemplo siguiente toma una foto y guarda el JPEG como prism.jpg en la raíz del almacenamiento interno:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

El ejemplo siguiente recibe fotogramas de 1280x720, reconoce códigos de barras y cierra la sesión cuando lee uno. El reconocimiento de códigos de barras necesita el complemento ML Kit Barcode activado en el "Centro de plugins":

```js
let session = camera.open({ resolution: '1280x720' });
session.analyze((img) => {
    let text = barcode.recognizeText(img);
    if (text !== null) {
        toastLog(text);
        session.close();
    }
});
events.on('exit', () => session.close());
```

- Un fotograma recibido en la devolución de llamada de `analyze` solo es válido mientras esta se ejecuta; para usarlo después, llama a `img.clone()` para obtener una copia y a `recycle()` al terminar.
- `camera.capture` no se puede llamar en el hilo de la interfaz; en las devoluciones de llamada de la interfaz de los scripts ui, usa `camera.captureAsync`.
- Los scripts de Node.js usan los mismos métodos mediante `require('autojs6:camera')`, que devuelven Promise; Node.js y Bun no pueden mostrar la vista previa.
- El uso completo y los códigos de error están en [Documentación de AutoJs6 > Camera](https://docs.autojs6.com/#/camera).

### Requisitos

- Android 7.0 o posterior. El visor y los ajustes no necesitan AutoJs6.
- El módulo `camera` necesita AutoJs6 6.8.1 o posterior, con este complemento activado en el "Centro de plugins".
- El permiso de cámara lo tiene el propio complemento; el permiso de cámara de AutoJs6 no lo sustituye. Concédelo en la fila "Permiso de cámara" de los ajustes, o llama a `camera.requestPermission()` desde un script.
- El permiso de notificaciones (Android 13 o posterior) sirve para mostrar la notificación "Cámara en uso"; el acceso a la red solo se usa para buscar actualizaciones, y las imágenes de la cámara y las fotos nunca salen del dispositivo.

### Preguntas frecuentes

**Motivos por los que abrir la cámara informa `CAMERA_DISABLED`**

En Android 11 y posteriores, el sistema no permite usar la cámara mientras AutoJs6 está en segundo plano o el teléfono está en la pantalla de bloqueo. Vuelve a poner AutoJs6 en primer plano y abre la cámara de nuevo; una sesión abierta en primer plano sigue recibiendo fotogramas en segundo plano o con la pantalla apagada.

**Casos en que la cámara está ocupada (`CAMERA_BUSY`)**

Solo una sesión puede usar la cámara a la vez, y `details.holder` en el error indica quién la ocupa: la ruta de un script, o `pluginUi` para el visor. Cierra esa sesión o el visor y vuelve a abrir la cámara; en el visor también puedes tocar "Detener este script".

**Ubicación donde se guardan las fotos**

El complemento no guarda fotos ni escribe en la galería del sistema. AutoJs6 escribe el JPEG solo cuando un script pasa `path`.

La compatibilidad con los sistemas de cada fabricante y los permisos se detallan en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
