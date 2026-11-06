<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-prism-camera-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>CameraX による写真撮影, 連続フレーム解析, ファインダープレビューを提供し, フレームを OCR, バーコード, YOLO などの認識モジュールへ直接渡せます</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 言語 (Languages)

******

現在の README.md は次の言語に対応しています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/.readme/README-ar.md)

******

### 概要

******

3-Prism Camera は AutoJs6 のカメラプラグインです. Jetpack CameraX を基盤に, スクリプトへ写真撮影, 連続フレーム解析, ファインダープレビューを提供します. ライブフレームは AutoJs6 の `ImageWrapper` としてスクリプトに届くため, `ocr`, `barcode`, `yolo` などの既存モジュールにそのまま渡して, リアルタイムのコード読み取り, 文字認識, 物体検出を実現できます.

プラグインがカメラを保持し, Binder で AutoJs6 と通信します: ライブフレームは共有メモリのリングバッファで渡され (Android 7.0 - 8.0 では一時ファイル), 写真は JPEG として AutoJs6 に渡ってデコードまたはそのまま保存され, プレビューはスクリプト画面の `SurfaceView` に直接描画されるため, 画素は Binder を通りません. セッションが開いている間はカメラのフォアグラウンドサービスが動作するので, スクリプトがバックグラウンドにあっても画面オフでもフレームを受け取り続けられます.

> 1.0.0 のカメラ中核機能, ファインダー画面と設定画面は完成しており, リリース前の検証を行っています. スクリプトで使う `camera` モジュールには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です.

******

### 機能ハイライト

******

- 写真: EXIF の向きに合わせて正立させた `ImageWrapper` を返すほか, 元の JPEG (EXIF 付き) を指定のパスに保存できます; フラッシュモード, 画質, 解像度を設定できます.
- ライブフレーム: RGBA フレームは既定 640x480 (設定画面で変更可), 最大 1920x1080 で, 既定で画面の向きに合わせて正立します; 処理が追いつかない場合はフレームを破棄して数え, 溜め込みません.
- プレビュー: ui スクリプトまたはフローティングウィンドウの `SurfaceView` をセッションに渡すとファインダーが表示され, ライブフレームや写真撮影と同時に使えます.
- 操作: レンズ選択, 解像度, 目標フレームレート, ライト, フラッシュモード, ズーム, 露出補正, タップフォーカスに対応し, 各範囲と現在の状態を読み取れます.
- 撮影画面: ランチャーからプラグインを開くと撮影画面になり, レンズの切り替え, ライト, ズーム, タップでのフォーカスができます. 撮影した写真は表示するだけで保存せず, 右上のボタンで設定を開きます. `camera.capture({ ui: true })` は同じ画面を開き, ユーザーがシャッターを押すとスクリプトが写真を受け取ります; キャンセル時は `null` を返します.
- バックグラウンド対応: セッション中はカメラのフォアグラウンドサービスが動作し, バックグラウンドや画面オフでもフレームが届きます; スクリプトの終了時や AutoJs6 の停止時にはカメラを自動で解放します.

******

### 使用方法

******

1. [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/releases) から端末の ABI に合った APK をダウンロードしてインストールします. ほとんどのスマートフォンは `arm64-v8a` です. わからない場合は, すべての端末で使えるもののサイズが大きい `universal` を選んでください.
2. プラグインを開き, 案内に従ってカメラの権限を許可します; スクリプトから `camera.requestPermission()` を呼んで許可画面を開くこともできます.
3. スクリプトで `camera` モジュールを使うには, AutoJs6 を内部バージョン 6641 (6.8.1) 以降に更新し, プラグインセンターで本プラグインを有効にしてください.

> プラグインは Android 7.0 (API 24) 以降の端末に対応しています. APK は ABI ごとに 1 つあり, すべての端末で使える `universal` もあります; CameraX と Compose に付属する少量のネイティブライブラリ (ABI ごとに約 50 KB) は 16 KB メモリページに揃えてあるため, 16 KB メモリページを採用した新しい端末にもインストールできます.

******

### 互換性

******

- Android 7.0 (API 24) 以降, ABI の制限なし: arm64-v8a, armeabi-v7a, x86_64, x86 それぞれのパッケージがあり, `universal` パッケージはすべての端末で使えます.
- Android 7.0 - 8.0 (API 24 - 26) には共有メモリがないため, フレームは一時ファイル経由で AutoJs6 に渡され, ライブフレームのフレームレートは新しいシステムより低くなります.
- スクリプトで `camera` モジュールを使うには AutoJs6 6.8.1 (内部バージョン 6641) 以降が必要です. それより前の AutoJs6 にはこのモジュールがありませんが, ランチャーからプラグインのファインダー画面は開けます.
- 一部のメーカーのシステム (例: ColorOS) はバックグラウンドアプリのカメラ使用をさらに制限します. 端末ごとの検証結果はロードマップの付録 F にあります.

******

### スクリプト API

******

スクリプトはグローバルモジュール `camera` (`$camera` とも書けます) からこれらの機能を使います. 詳しくは [AutoJs6 ドキュメント > Camera](https://docs.autojs6.com/#/camera) を参照してください:

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

- `camera.capture(options)` は開く, 撮影, 閉じるを一度に行います; `camera.open(options)` は排他的なセッションを開き, セッションオブジェクトは `preview`, `analyze`, `capture`, `torch`, `zoom`, `exposure`, `focusAt`, `state`, `close` を提供します.
- フレームの寿命: `analyze` コールバックの `img` はコールバックの実行中だけ有効で, 戻るとすぐに回収されます; コールバックの外で使うには `img.clone()` を呼び, 使い終わったら自分で `recycle()` してください.
- `camera.list()` はレンズとその解像度, ズーム範囲, 露出範囲を一覧にします; `camera.capabilities()` は契約バージョン, 機能一覧, 権限の状態を返します.
- セッションは同時に 1 つだけです: 2 回目の `open` は `CAMERA_BUSY` で失敗し, 使用中の相手を示します; スクリプトの終了時にセッションは自動で閉じます.

******

### よくある質問

******

#### なぜカメラは AutoJs6 本体ではなくプラグインなのですか?

カメラには CameraX とフォアグラウンドサービスの権限が必要です. プラグインにすることで, カメラを使わないユーザーはその容量も権限も負担せずに済み, プラグインは AutoJs6 とは別に更新できます.

#### スクリプトはバックグラウンドでもフレームを受け取れますか?

はい. ただし, セッションは AutoJs6 がフォアグラウンドにあるときに開く必要があります. セッション中はカメラのフォアグラウンドサービスが動作し, 通知でカメラの使用中を示します. AutoJs6 がすでにバックグラウンドにあるときや端末がロック画面のままのとき (Android 11 以降) はシステムがカメラの使用を許可せず, そのとき開いたセッションは CAMERA_DISABLED を返して閉じます. 一部のメーカーのシステムはバックグラウンドのカメラをさらに制限するため, 対応状況はロードマップの端末一覧を参照してください.

#### プラグインは写真をギャラリーに保存しますか?

いいえ. プラグインはストレージの権限を要求せず, システムのギャラリーにも書き込みません; スクリプトが `path` を渡したときだけ, AutoJs6 がそのパスに写真を書き込みます.

******

### 権限とセキュリティ

******

プラグインは必要な権限をできるだけ少なくしています:

- 保護された入口: 機能サービスと起動入口は AutoJs6 のプラグイン権限 (`org.autojs.permission.PLUGIN`) で保護され, 他のアプリからは直接呼び出せません; フォアグラウンドサービスと透明な起動画面はエクスポートされません.
- カメラ: 要求するのは `CAMERA`, カメラ種別のフォアグラウンドサービス権限, そして Android 13 以降でカメラ使用中の通知を表示するための通知権限だけです; カメラはセッション中だけ使用し, セッションの終了, スクリプトの終了, AutoJs6 の停止のいずれでもすぐに解放します.
- ネットワークとストレージ: ネットワークの権限は設定画面の更新確認だけに使います (AutoJs6 の公式プラグインインデックスのみ読み取ります, 自動確認は 12 時間あたり最大 1 回でオフにできます). ストレージの権限は要求しません; フレームと写真はプラグインと AutoJs6 の間だけでやり取りされ, アップロードされず, システムのギャラリーにも書き込まれません.
- 排他利用: セッションは同時に 1 つだけで (スクリプトまたはプラグインの撮影画面), 使用中の相手はエラーメッセージに記載されます.

******

### プラグインインターフェース

******

以下の情報は AutoJs6 ホストとプラグイン開発者向けです. ホストはこれらの識別子でプラグインを検出し, 機能ネゴシエーションを行います:

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

### 開発ロードマップ

******

計画, 設計上の決定, 進捗は ROADMAP.md のチェックリストで管理しています. 未チェックの項目は計画であり, 現在のバージョンの機能ではありません.

- [ROADMAP.md を見る](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/11/05_

- `ヒント` スクリプト API: [AutoJs6 ドキュメント > Camera](https://docs.autojs6.com/#/camera)
- `ヒント` AutoJs6 6.8.1 (6641) 以降が必要です (正確な最低バージョンはホスト側の変更が反映された後に記入されます)
- `ヒント` 現在のアイコンは候補のデザインで, 今後変わる可能性があります
- `機能` プラグインの骨組み: Wake 有効化プロトコルとプラグインセンターの識別サービス
- `機能` カメラ機能サービス: カメラを使えるセッションは同時に 1 つだけで, 使用中の場合は使用中の相手を返します. カメラの使用中はフォアグラウンド通知を表示し, AutoJs6 が終了するとカメラはすぐに解放され, 通知も消えます
- `機能` AutoJs6 がバックグラウンドにあるときや端末がロック画面のままのとき (Android 11 以降) はシステムがカメラの使用を許可しないため, そのとき開いたセッションは CAMERA_DISABLED を返してから閉じます. フォアグラウンドで開いたセッションは, AutoJs6 がバックグラウンドに移ったり画面が消えたりした後もフレームを取得し続けます
- `機能` ライブフレーム: RGBA フレームを 3 つの共有メモリ (Android 8.1 以降) または 3 つの一時ファイル (Android 7.0 - 8.0) で順番に AutoJs6 へ渡します. 解像度とフレームレートを指定でき, 処理が間に合わないフレームは飛ばして件数を数えます
- `機能` 写真撮影: JPEG を読み取り専用ファイルとして AutoJs6 に渡し, 幅と高さ, EXIF の向き, バイト数, ミラーの有無を添えます. 画質, 解像度, フラッシュモードを指定できます. ミラー撮影では EXIF の向きだけを書き換え, 再エンコードしません. 写真の向きを書き込まないカメラ (例: Android 7.0 エミュレーター) では, プラグインが 90 / 270 度の向きを補って書き込み, 写真が端末の持ち方どおりに表示されるようにします (書き漏れた 180 度は判別できません)
- `機能` プレビュー: AutoJs6 が用意した画面に表示し, 画面を回転すると向きも追従します. 前面レンズのプレビューは既定でミラー表示し, フレームと写真は既定でミラーしません. プレビューを表示する画面がなくなるとプレビューは自動的に停止し, フレームの取得と撮影はそのまま続きます
- `機能` コントロール: ライト, フラッシュモード, ズーム (倍率またはリニア), 露出補正, タップによるフォーカスと測光. 端末が対応しない項目 (前面レンズのフラッシュなど) は UNSUPPORTED を返します
- `機能` カメラ権限: 未許可のままカメラを開くと PERMISSION_DENIED を返し, 権限をリクエストできます. Android 13 以降では, カメラ使用中の通知を表示するため通知の権限も同時にリクエストします
- `機能` ファインダー画面: ランチャーから開き, レンズの切り替え, ライト, スライダーまたはピンチでのズーム, タップでのフォーカスができます. 撮影した写真は画像と解像度を表示するだけで保存しません. 前面レンズの画像は画面上でミラー表示し, 写真はミラーしません
- `機能` スクリプトで camera.capture({ ui: true }) を呼ぶと同じファインダー画面が開き, 写真はそのままスクリプトに渡り, キャンセルすると null を返します. AutoJs6 がバックグラウンドにあってファインダー画面を開けない場合は, いつまでも待たずに 10 秒後に UI_REQUIRES_FOREGROUND を返します
- `機能` スクリプトがカメラを使用中のときは, ファインダー画面にそのスクリプトを表示し, 停止してカメラを引き継げます
- `機能` 設定画面 (ファインダー画面の右上): 外観 (言語, ナイトモード, テーマ色, ランチャーアイコン), カメラと通知の権限, 既定のカメラ, 既定の解析解像度とフレームレート, フォアグラウンドサービスの通知, 診断 (CameraX のバージョン, 各レンズの機能, 現在カメラを使っているもの), バージョン履歴
- `機能` スクリプトがカメラ, 解像度またはフレームレートを指定しないときは設定画面の既定値を使います (初期値は背面カメラ, 640x480, デバイス自身のフレームレート). スクリプトが指定した値が常に優先されます
- `機能` 更新の確認: 設定画面で 公式プラグインインデックスに登録された新しいリリースとその説明を確認でき, 特定のリリースを無視できます. 設定画面を開いたときに自動で確認します (12 時間あたり最大 1 回, オフにできます). ネットワークの権限は更新の確認だけに使い, プラグインはカメラの映像や写真をアップロードしません
- `機能` ABI ごとのインストールパッケージ (arm64-v8a, armeabi-v7a, x86_64, x86) と, すべての端末で使える universal パッケージを提供します. CameraX と Compose に付属するネイティブライブラリは 16 KB メモリページに揃えてあります
- `機能` 10 言語の README, プラグインセンターの説明, 変更履歴を JSON ソースから一括生成
- `依存関係` Jetpack CameraX 1.6.2 を追加 (カメラの起動, 写真撮影, フレームとファインダーの提供)
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (プラグイン画面)
- `依存関係` AndroidX Activity Compose 1.12.2 を追加
- `依存関係` AndroidX Core KTX 1.16.0 を追加
- `依存関係` AndroidX ExifInterface 1.4.2 を追加 (写真の EXIF の向きの書き込み)
- `依存関係` Material Color Utilities (MaterialKolor) 4.1.1 を追加 (プラグイン画面のテーマ色)

##### その他のリリース履歴は以下を参照

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

この節はソースからプラグインをビルドしたい開発者向けです.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\check_repository.py
```

******

### ライセンス

******

プロジェクトのコードは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/LICENSE) で提供されます. サードパーティ製コンポーネントのライセンスはサードパーティ通知を参照してください.

- [サードパーティの表示 (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Prism-Camera/blob/master/THIRD_PARTY_NOTICES.md)

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
