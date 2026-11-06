3-Prism Camera es el complemento de cámara de AutoJs6. Basado en Jetpack CameraX, ofrece a los scripts captura de fotos, análisis continuo de fotogramas y vista previa del visor. Los fotogramas en vivo llegan al script como objetos `ImageWrapper` de AutoJs6, de modo que el script puede pasarlos directamente a módulos existentes como `ocr`, `barcode` o `yolo` para escanear códigos en vivo, reconocer texto y detectar objetos.

> El núcleo de la cámara, el visor y la página de ajustes de la 1.0.0 están terminados, y las comprobaciones de publicación están en curso. El módulo de script `camera` requiere AutoJs6 6.8.1 (versión interna 6641) o posterior.

### Funciones Destacadas

- Fotos: devuelve un `ImageWrapper` enderezado según la orientación EXIF, o guarda el JPEG original (con EXIF) en la ruta que elija; el modo de flash, la calidad y la resolución son configurables.
- Fotogramas en vivo: fotogramas RGBA de 640x480 por defecto (se puede cambiar en los ajustes) y hasta 1920x1080, enderezados según la pantalla por defecto; cuando un script se retrasa, los fotogramas se descartan y se cuentan en lugar de acumularse.
- Vista previa: entregue a la sesión un `SurfaceView` de un script ui o de una ventana flotante para mostrar el visor, junto con los fotogramas en vivo y la captura de fotos.
- Controles: selección de lente, resolución, velocidad de fotogramas objetivo, linterna, modo de flash, zoom, compensación de exposición y enfoque al tocar, con rangos y estado actual legibles.
- Pantalla de captura: al abrir el complemento desde el lanzador se muestra la pantalla de captura, con cambio de lente, linterna, zoom y enfoque al tocar; las fotos tomadas ahí solo se muestran, no se guardan, y el botón de arriba a la derecha abre los ajustes; `camera.capture({ ui: true })` abre la misma pantalla, y el script recibe la foto cuando el usuario pulsa el obturador, o `null` si cancela.
- Funciona en segundo plano: un servicio en primer plano de cámara se ejecuta mientras hay una sesión abierta, así que los fotogramas siguen llegando en segundo plano o con la pantalla apagada; la cámara se libera cuando el script termina o AutoJs6 se detiene.

La API de script está en [Documentación de AutoJs6 > Camera](https://docs.autojs6.com/#/camera), y el progreso y el diseño en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
