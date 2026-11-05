3-Prism Camera est le plugin caméra d'AutoJs6. Basé sur Jetpack CameraX, il offre aux scripts la prise de photos, l'analyse continue des images et l'aperçu du viseur. Les images en direct arrivent dans le script sous forme d'objets `ImageWrapper` d'AutoJs6, que le script peut transmettre directement aux modules existants comme `ocr`, `barcode` ou `yolo` pour la lecture de codes en direct, la reconnaissance de texte et la détection d'objets.

> Ceci est une préversion de développement: le centre de plugins d'AutoJs6 reconnaît le plugin et AutoJs6 peut déjà utiliser le cœur caméra (ouverture de la caméra, images en direct, prise de photos, aperçu et réglages), mais l'écran de prise de vue et le module de script `camera` ne sont pas encore livrés. Les sections suivantes décrivent les objectifs de conception de la 1.0.0; consultez la feuille de route pour l'avancement.

### Points Forts

- Photos: renvoie un `ImageWrapper` redressé selon l'orientation EXIF, ou enregistre le JPEG d'origine (avec EXIF) au chemin choisi; le mode flash, la qualité et la résolution sont réglables.
- Images en direct: images RGBA en 640x480 par défaut et jusqu'à 1920x1080, redressées selon l'écran par défaut; quand un script prend du retard, les images sont abandonnées et comptées au lieu de s'accumuler.
- Aperçu: confiez à la session une `SurfaceView` d'un script ui ou d'une fenêtre flottante pour afficher le viseur, en parallèle des images en direct et de la prise de photos.
- Commandes: choix de l'objectif, résolution, fréquence d'images cible, lampe, mode flash, zoom, compensation d'exposition et mise au point au toucher, avec lecture des plages et de l'état courant.
- Écran de prise de vue: `camera.capture({ ui: true })` ouvre l'écran de prise de vue du plugin; le script reçoit la photo quand l'utilisateur appuie sur le déclencheur, ou `null` s'il annule.
- Fonctionne en arrière-plan: un service de premier plan caméra tourne tant qu'une session est ouverte, les images continuent donc d'arriver en arrière-plan ou écran éteint; la caméra est libérée quand le script se termine ou qu'AutoJs6 est arrêté.

L'avancement et la conception sont disponibles sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
