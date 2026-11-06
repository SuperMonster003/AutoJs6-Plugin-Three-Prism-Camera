******

### Historial De Versiones

******

# v1.0.0

###### 2026/11/05

* `Aviso` Versión preliminar de desarrollo: AutoJs6 ya puede usar el núcleo de cámara y el visor (abrir la cámara, fotogramas en vivo, captura de fotos, vista previa, los controles y la captura desde el visor); algunos detalles aún pueden cambiar antes de la publicación
* `Aviso` Requiere AutoJs6 6.8.1 (6641) o posterior (la versión mínima exacta se indicará cuando se integren los cambios del anfitrión)
* `Aviso` El icono actual es un diseño candidato y aún puede cambiar
* `Función` Esqueleto del complemento: protocolo de activación Wake y servicio de identidad del centro de complementos
* `Función` Servicio de capacidad de cámara: solo una sesión puede usar la cámara a la vez y, si está ocupada, se indica quién la ocupa; mientras la cámara está en uso se muestra una notificación en primer plano, y cuando AutoJs6 se cierra la cámara se libera de inmediato y la notificación desaparece
* `Función` Fotogramas en vivo: los fotogramas RGBA llegan a AutoJs6 por turnos a través de 3 bloques de memoria compartida (Android 8.1 o posterior) o 3 archivos temporales (Android 7.0 - 8.0), con la resolución y la frecuencia elegidas; los fotogramas que no se pueden procesar a tiempo se omiten y se cuentan
* `Función` Captura de fotos: el JPEG llega a AutoJs6 como un archivo de solo lectura junto con sus dimensiones, la orientación EXIF, el número de bytes y si está reflejado; se pueden ajustar la calidad, la resolución y el modo de flash; en una foto reflejada solo se reescribe la orientación EXIF, sin volver a codificarla; si una cámara no escribe la orientación de la foto (por ejemplo, el emulador de Android 7.0), el complemento escribe la orientación de 90 / 270 grados para que la foto se muestre tal como se sostenía el teléfono (no se puede detectar una omisión de 180 grados)
* `Función` Vista previa: la imagen se muestra en una superficie que proporciona AutoJs6 y sigue la rotación de la pantalla; la vista previa de la lente frontal se refleja por defecto, los fotogramas y las fotos no; cuando desaparece la superficie que muestra la vista previa, esta se detiene sola y los fotogramas y las fotos continúan
* `Función` Controles: linterna, modo de flash, zoom (proporción o lineal), compensación de exposición, enfoque y medición por toque; lo que el dispositivo no admite (como el flash de la lente frontal) devuelve UNSUPPORTED
* `Función` Permiso de cámara: abrir la cámara sin él devuelve PERMISSION_DENIED y se puede solicitar; en Android 13 o posterior también se solicita el permiso de notificaciones, para la notificación de cámara en uso
* `Función` Visor: se abre desde el lanzador y permite cambiar de lente, usar la linterna, hacer zoom con el control deslizante o pellizcando y tocar para enfocar; una foto tomada aquí solo muestra la imagen y su resolución y no se guarda; la imagen de la lente frontal se ve reflejada en pantalla, la foto no
* `Función` camera.capture({ ui: true }) en un script abre el mismo visor: la foto se entrega directamente al script y al cancelar se devuelve null; si el visor no puede abrirse porque AutoJs6 está en segundo plano, la llamada devuelve UI_REQUIRES_FOREGROUND a los 10 segundos en lugar de esperar indefinidamente
* `Función` Cuando un script está usando la cámara, el visor indica cuál es y puede detenerlo para tomar la cámara
* `Función` Ajustes (arriba a la derecha del visor): apariencia (idioma, modo nocturno, color del tema, icono del lanzador), permisos de cámara y notificaciones, cámara predeterminada, tamaño y velocidad de análisis predeterminados, notificación del servicio en primer plano, diagnóstico (versión de CameraX, capacidades de cada lente, quién usa la cámara) e historial de versiones
* `Función` Cuando un script no indica cámara, resolución ni velocidad de fotogramas, se aplican los valores predeterminados de los ajustes (al principio la cámara trasera, 640x480 y la velocidad propia del dispositivo); el valor que indica el script siempre tiene prioridad
* `Función` Búsqueda de actualizaciones: desde los ajustes se ve una versión más reciente publicada en el índice oficial de complementos y sus notas, y se puede ignorar una versión; al abrir los ajustes se busca automáticamente (como máximo una vez cada 12 horas, se puede desactivar); el permiso de red solo sirve para esta búsqueda y el complemento nunca sube imágenes de la cámara ni fotos
* `Función` Un paquete por ABI (arm64-v8a, armeabi-v7a, x86_64, x86) y un paquete universal para todos los dispositivos; las bibliotecas nativas que incluyen CameraX y Compose están alineadas a páginas de memoria de 16 KB
* `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
* `Dependencia` Se agrega Jetpack CameraX 1.6.2 (abre la cámara, toma fotos, entrega fotogramas y el visor)
* `Dependencia` Se agrega Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
* `Dependencia` Se agrega AndroidX Activity Compose 1.12.2
* `Dependencia` Se agrega AndroidX Core KTX 1.16.0
* `Dependencia` Se agrega AndroidX ExifInterface 1.4.2 (escribe la orientación EXIF de las fotos)
* `Dependencia` Se agrega Material Color Utilities (MaterialKolor) 4.1.1 (color del tema de las pantallas del complemento)
