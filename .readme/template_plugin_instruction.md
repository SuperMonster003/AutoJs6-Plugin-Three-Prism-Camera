{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let img = camera.capture({ path: files.join(files.getSdcardPath(), 'prism.jpg') });
toastLog(img.getWidth() + 'x' + img.getHeight());
img.recycle();
```

{{ instruction_p_script_analyze }}

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

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
