3-Prism Camera est le plugin caméra d'AutoJs6: les scripts peuvent prendre des photos, recevoir des images en direct et afficher l'aperçu du viseur. Les images peuvent être passées directement à des modules de reconnaissance comme `ocr`, `barcode` ou YOLO pour lire des codes, reconnaître du texte ou détecter des objets en temps réel.

### Prise en main

1. Ouvrez 3-Prism Camera depuis le lanceur et appuyez sur "Accorder l'autorisation" dans le viseur pour permettre au plugin d'utiliser la caméra.
2. Appuyez sur "Prendre une photo" pour un essai. La photo s'affiche seulement dans "Aperçu de la photo" et n'est pas enregistrée.
3. Activez ce plugin dans le "Centre des plugins" d'AutoJs6.
4. Appelez le module `camera` dans un script pour prendre des photos ou recevoir des images, comme dans les exemples ci-dessous.

### Fonctionnalités principales

- **Viseur**: ouvrir le plugin depuis le lanceur affiche le viseur, où vous pouvez "Changer de caméra", allumer ou éteindre la lampe, zoomer avec le curseur ou en pinçant, et appuyer sur l'image pour faire la mise au point.
- **Photos**: les scripts reçoivent une image redressée et peuvent aussi enregistrer le JPEG d'origine dans un chemin; avec `camera.capture({ ui: true })`, c'est l'utilisateur qui prend la photo dans le viseur.
- **Images en direct**: 640x480 par défaut et jusqu'à 1920x1080, redressées selon l'écran; les images qu'un script n'a pas le temps de traiter sont ignorées et comptées au lieu de s'accumuler.
- **Aperçu**: confiez à la session une `SurfaceView` d'une mise en page ui ou d'une fenêtre flottante pour afficher l'image du viseur, en parallèle des images en direct et des photos.
- **Commandes**: les scripts contrôlent la lampe, le mode flash, le zoom, la compensation d'exposition et la mise au point par appui, et lisent la plage et la valeur actuelle de chacun.
- **Une session à la fois**: une seule session utilise la caméra à la fois. Quand un script la détient, le viseur indique lequel, et "Arrêter ce script" permet de reprendre la caméra.
- **Images en arrière-plan**: la notification "Caméra en cours d'utilisation" s'affiche tant qu'une session est ouverte, et les images continuent d'arriver quand AutoJs6 est en arrière-plan ou l'écran éteint; la caméra est libérée dès que le script se termine.
- **Valeurs par défaut**: dans "Paramètres", en haut à droite du viseur, réglez "Caméra par défaut", "Taille d'analyse par défaut" et "Fréquence d'analyse par défaut"; les valeurs passées par un script sont toujours prioritaires.
- **Diagnostic et mises à jour**: la section "Diagnostic" des paramètres liste les capacités de chaque caméra et le "Détenteur actuel"; "Rechercher les mises à jour" cherche les nouvelles versions dans l'index officiel des plugins.
- **Apparence**: "Langue", "Mode nuit" et "Couleur du thème" suivent AutoJs6 par défaut et peuvent aussi être réglés à part; le réglage "Icône du lanceur" permet aussi de changer d'icône.

### Utilisation dans les scripts

Dans AutoJs6, les scripts Rhino utilisent la caméra via le module global `camera`. L'exemple ci-dessous prend une photo et enregistre le JPEG sous prism.jpg à la racine du stockage interne:

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

L'exemple ci-dessous reçoit des images en 1280x720 et reconnaît les codes-barres, puis ferme la session dès qu'un code est lu. La reconnaissance des codes-barres nécessite le plugin ML Kit Barcode activé dans le "Centre des plugins":

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

- Une image reçue dans le rappel de `analyze` n'est valable que pendant l'exécution du rappel; pour l'utiliser ensuite, appelez `img.clone()` pour obtenir une copie, puis `recycle()` une fois terminé.
- `camera.capture` ne peut pas être appelé sur le thread UI; dans les rappels d'interface des scripts ui, utilisez `camera.captureAsync`.
- Les scripts Node.js utilisent les mêmes méthodes via `require('autojs6:camera')`, qui renvoient des Promise; Node.js et Bun ne peuvent pas afficher d'aperçu.
- L'utilisation complète et les codes d'erreur sont décrits dans [Documentation AutoJs6 > Camera](https://docs.autojs6.com/#/camera).

### Prérequis

- Android 7.0 ou version ultérieure. Le viseur et les paramètres n'ont pas besoin d'AutoJs6.
- Le module `camera` nécessite AutoJs6 6.8.1 ou version ultérieure, avec ce plugin activé dans le "Centre des plugins".
- L'autorisation caméra appartient au plugin lui-même; celle d'AutoJs6 ne la remplace pas. Accordez-la sur la ligne "Autorisation Caméra" des paramètres, ou appelez `camera.requestPermission()` depuis un script.
- L'autorisation des notifications (Android 13 et versions ultérieures) sert à afficher la notification "Caméra en cours d'utilisation"; l'accès réseau sert uniquement à rechercher les mises à jour, et les images de la caméra et les photos ne quittent jamais l'appareil.

### Questions fréquentes

**Raisons pour lesquelles l'ouverture de la caméra signale `CAMERA_DISABLED`**

Sur Android 11 et versions ultérieures, le système n'autorise pas la caméra quand AutoJs6 est en arrière-plan ou que le téléphone est sur l'écran de verrouillage. Ramenez AutoJs6 au premier plan, puis rouvrez la caméra; une session ouverte au premier plan continue de recevoir des images en arrière-plan ou écran éteint.

**Cas où la caméra est occupée (`CAMERA_BUSY`)**

Une seule session peut utiliser la caméra à la fois, et `details.holder` dans l'erreur indique qui la détient: le chemin d'un script, ou `pluginUi` pour le viseur. Fermez cette session ou le viseur, puis rouvrez la caméra; dans le viseur, vous pouvez aussi appuyer sur "Arrêter ce script".

**Emplacement d'enregistrement des photos**

Le plugin n'enregistre pas les photos et n'écrit pas dans la galerie du système. AutoJs6 n'écrit le JPEG que si un script passe `path`.

La compatibilité avec les systèmes des fabricants et les autorisations sont détaillées sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera).
