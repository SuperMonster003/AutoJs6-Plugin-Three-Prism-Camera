3-Prism Camera est le plugin caméra d'AutoJs6. Basé sur Jetpack CameraX, il offre aux scripts la prise de photos, l'analyse continue des images et l'aperçu du viseur. Les images en direct arrivent dans le script sous forme d'objets `ImageWrapper` d'AutoJs6, que le script peut transmettre directement aux modules existants comme `ocr`, `barcode` ou `yolo` pour la lecture de codes en direct, la reconnaissance de texte et la détection d'objets.

> La version 1.0.0 est la première publication. Le module de script `camera` nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure; le viseur et la page des paramètres du plugin ne dépendent pas d'AutoJs6.

### Points Forts

- Photos: renvoie un `ImageWrapper` redressé selon l'orientation EXIF, ou enregistre le JPEG d'origine (avec EXIF) au chemin choisi; le mode flash, la qualité et la résolution sont réglables.
- Images en direct: images RGBA en 640x480 par défaut (modifiable dans les paramètres) et jusqu'à 1920x1080, redressées selon l'écran par défaut; quand un script prend du retard, les images sont abandonnées et comptées au lieu de s'accumuler.
- Aperçu: confiez à la session une `SurfaceView` d'un script ui ou d'une fenêtre flottante pour afficher le viseur, en parallèle des images en direct et de la prise de photos.
- Commandes: choix de l'objectif, résolution, fréquence d'images cible, lampe, mode flash, zoom, compensation d'exposition et mise au point au toucher, avec lecture des plages et de l'état courant.
- Écran de prise de vue: ouvrir le plugin depuis le lanceur affiche l'écran de prise de vue, avec changement d'objectif, lampe, zoom et mise au point au toucher; les photos prises ici sont seulement affichées, pas enregistrées, et le bouton en haut à droite ouvre les paramètres; `camera.capture({ ui: true })` ouvre le même écran, et le script reçoit la photo quand l'utilisateur appuie sur le déclencheur, ou `null` s'il annule.
- Fonctionne en arrière-plan: un service de premier plan caméra tourne tant qu'une session est ouverte, les images continuent donc d'arriver en arrière-plan ou écran éteint; la caméra est libérée quand le script se termine ou qu'AutoJs6 est arrêté.

L'API de script est décrite dans [Documentation AutoJs6 > Camera](https://docs.autojs6.com/#/camera), l'avancement et la conception sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
