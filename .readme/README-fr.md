<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Prise de photos, analyse continue des images et aperçu du viseur via CameraX, avec des images prêtes pour les modules de reconnaissance OCR, codes-barres, YOLO et autres</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Prism Camera est le plugin caméra d'AutoJs6. Basé sur Jetpack CameraX, il offre aux scripts la prise de photos, l'analyse continue des images et l'aperçu du viseur. Les images en direct arrivent dans le script sous forme d'objets `ImageWrapper` d'AutoJs6, que le script peut transmettre directement aux modules existants comme `ocr`, `barcode` ou `yolo` pour la lecture de codes en direct, la reconnaissance de texte et la détection d'objets.

Le plugin détient la caméra et communique avec AutoJs6 par Binder: les images en direct passent par un tampon circulaire en mémoire partagée (des fichiers temporaires sous Android 7.0 - 8.0), les photos arrivent dans AutoJs6 en JPEG pour être décodées ou enregistrées telles quelles, et l'aperçu s'affiche directement dans une `SurfaceView` de l'interface du script, sans faire passer de pixels par Binder. Tant qu'une session est ouverte, le plugin exécute un service de premier plan caméra, si bien que les scripts continuent de recevoir des images en arrière-plan ou écran éteint.

> Ceci est une préversion de développement: le centre de plugins d'AutoJs6 reconnaît le plugin et AutoJs6 peut déjà utiliser le cœur caméra (ouverture de la caméra, images en direct, prise de photos, aperçu et réglages), mais l'écran de prise de vue et le module de script `camera` ne sont pas encore livrés. Les sections suivantes décrivent les objectifs de conception de la 1.0.0; consultez la feuille de route pour l'avancement.

******

### Points Forts

******

- Photos: renvoie un `ImageWrapper` redressé selon l'orientation EXIF, ou enregistre le JPEG d'origine (avec EXIF) au chemin choisi; le mode flash, la qualité et la résolution sont réglables.
- Images en direct: images RGBA en 640x480 par défaut et jusqu'à 1920x1080, redressées selon l'écran par défaut; quand un script prend du retard, les images sont abandonnées et comptées au lieu de s'accumuler.
- Aperçu: confiez à la session une `SurfaceView` d'un script ui ou d'une fenêtre flottante pour afficher le viseur, en parallèle des images en direct et de la prise de photos.
- Commandes: choix de l'objectif, résolution, fréquence d'images cible, lampe, mode flash, zoom, compensation d'exposition et mise au point au toucher, avec lecture des plages et de l'état courant.
- Écran de prise de vue: `camera.capture({ ui: true })` ouvre l'écran de prise de vue du plugin; le script reçoit la photo quand l'utilisateur appuie sur le déclencheur, ou `null` s'il annule.
- Fonctionne en arrière-plan: un service de premier plan caméra tourne tant qu'une session est ouverte, les images continuent donc d'arriver en arrière-plan ou écran éteint; la caméra est libérée quand le script se termine ou qu'AutoJs6 est arrêté.

******

### Utilisation

******

1. Téléchargez depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) l'APK qui correspond à l'ABI de l'appareil et installez-le: la plupart des téléphones utilisent `arm64-v8a`; en cas de doute, choisissez `universal`, qui convient à tous les appareils mais est plus volumineux.
2. Ouvrez le plugin et accordez l'autorisation caméra quand elle est demandée; les scripts peuvent aussi appeler `camera.requestPermission()` pour ouvrir l'écran d'autorisation.
3. Pour utiliser le module `camera` dans les scripts, mettez AutoJs6 à jour vers la version interne 6641 (6.8.1) ou ultérieure et activez ce plugin dans le centre de plugins.

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Il existe un APK par ABI et un paquet `universal` pour tous les appareils; les quelques bibliothèques natives fournies avec CameraX et Compose (environ 50 KB par ABI) sont alignées sur des pages mémoire de 16 KB, si bien que les appareils récents à pages de 16 KB l'installent aussi.

******

### API De Script

******

Les scripts utilisent ces fonctions via le module global `camera` (aussi disponible sous `$camera`; projet de conception de la 1.0.0, la documentation publiée fait foi):

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

- `camera.capture(options)` ouvre, photographie et ferme en une étape; `camera.open(options)` ouvre une session exclusive dont l'objet propose `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state` et `close`.
- Durée de vie des images: l'`img` d'un rappel `analyze` n'est valide que pendant l'exécution du rappel et est recyclée dès son retour; pour l'utiliser ensuite, appelez `img.clone()` puis `recycle()` sur la copie vous-même.
- `camera.list()` liste les objectifs avec leurs résolutions et leurs plages de zoom et d'exposition; `camera.capabilities()` renvoie la version du contrat, la liste des fonctions et l'état de l'autorisation.
- Une session à la fois: un second `open` échoue avec `CAMERA_BUSY` et indique le détenteur; la session se ferme quand le script se termine.

******

### FAQ

******

#### Pourquoi la caméra est-elle un plugin plutôt qu'une partie d'AutoJs6?

La caméra nécessite CameraX et les autorisations de service de premier plan. Sous forme de plugin, les utilisateurs qui n'en ont pas besoin n'en supportent ni la taille ni les autorisations, et le plugin peut être mis à jour indépendamment d'AutoJs6.

#### Les scripts reçoivent-ils des images en arrière-plan?

Oui. Tant qu'une session est ouverte, le plugin exécute un service de premier plan caméra et la notification indique que la caméra est utilisée. Certains systèmes de constructeurs restreignent davantage la caméra en arrière-plan; consultez la matrice des appareils dans la feuille de route.

#### Le plugin enregistre-t-il les photos dans la galerie?

Non. Le plugin ne demande aucune autorisation de stockage et n'écrit jamais dans la galerie du système; AutoJs6 n'écrit une photo qu'au `path` transmis par le script.

******

### Permissions Et Sécurité

******

Le plugin réduit autant que possible ses autorisations:

- Points d'entrée protégés: le service de capacités et le point d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler; le service de premier plan et l'écran de démarrage transparent ne sont pas exportés.
- Caméra: seules les autorisations `CAMERA`, de service de premier plan caméra et, sur Android 13 ou ultérieur, des notifications (pour la notification de caméra en cours d'utilisation) sont demandées; la caméra n'est utilisée que pendant une session ouverte et est libérée dès que la session se ferme, que le script se termine ou qu'AutoJs6 est arrêté.
- Réseau et stockage: aucune autorisation réseau ni stockage; les images et les photos ne circulent qu'entre le plugin et AutoJs6 et n'atteignent jamais la galerie du système.
- Usage exclusif: une session à la fois (un script ou l'écran de prise de vue du plugin), et le message d'erreur indique le détenteur.

******

### Interface Du Plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins; l'hôte utilise ces identifiants pour découvrir le plugin et négocier les capacités:

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

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.0.0

_2026/11/05_

- `Note` Préversion de développement: AutoJs6 peut déjà utiliser le cœur caméra (ouverture de la caméra, images en direct, prise de photos, aperçu et réglages); l'écran de prise de vue et le module de script camera ne sont pas encore livrés
- `Note` Nécessite AutoJs6 6.8.1 (6641) ou ultérieur (la version minimale exacte sera renseignée une fois les modifications de l'hôte intégrées)
- `Note` L'icône actuelle est une proposition et peut encore changer
- `Fonctionnalité` Squelette du plugin: protocole d'activation Wake et service d'identité du centre des plugins
- `Fonctionnalité` Service de capacité caméra: une seule session peut utiliser la caméra à la fois et une caméra occupée indique son détenteur; une notification de premier plan s'affiche tant que la caméra est utilisée, et quand AutoJs6 se ferme la caméra est libérée aussitôt et la notification disparaît
- `Fonctionnalité` Images en direct: les images RGBA parviennent à AutoJs6 à tour de rôle par 3 blocs de mémoire partagée (Android 8.1 ou ultérieur) ou 3 fichiers temporaires (Android 7.0 - 8.0), avec la résolution et la fréquence choisies; les images qui ne peuvent pas être traitées à temps sont sautées et comptées
- `Fonctionnalité` Prise de photos: le JPEG parvient à AutoJs6 sous forme de fichier en lecture seule avec ses dimensions, l'orientation EXIF, le nombre d'octets et l'indication miroir; la qualité, la résolution et le mode flash sont réglables
- `Fonctionnalité` Aperçu: l'image s'affiche sur une surface fournie par AutoJs6 et suit la rotation de l'écran; l'aperçu de l'objectif avant est en miroir par défaut, pas les images ni les photos
- `Fonctionnalité` Réglages: lampe torche, mode flash, zoom (rapport ou linéaire), correction d'exposition, mise au point et mesure par toucher; ce que l'appareil ne prend pas en charge (comme le flash de l'objectif avant) renvoie UNSUPPORTED
- `Fonctionnalité` Autorisation caméra: ouvrir la caméra sans elle renvoie PERMISSION_DENIED et l'autorisation peut être demandée; sur Android 13 ou ultérieur, l'autorisation des notifications est aussi demandée, pour la notification de caméra en cours d'utilisation
- `Fonctionnalité` Un paquet par ABI (arm64-v8a, armeabi-v7a, x86_64, x86) et un paquet universal pour tous les appareils; les bibliothèques natives fournies avec CameraX et Compose sont alignées sur des pages mémoire de 16 KB
- `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
- `Dépendance` Ajout de Jetpack CameraX 1.6.2 (ouvre la caméra, prend les photos, fournit les images et le viseur)
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
- `Dépendance` Ajout de AndroidX Activity Compose 1.12.2
- `Dépendance` Ajout de AndroidX Core KTX 1.16.0

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### Licence

******

Le code du projet est distribué sous [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE). Les licences des composants tiers figurent dans les mentions tierces.

- [Mentions tierces (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
