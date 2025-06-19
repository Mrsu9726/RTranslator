# FireRedASR Integration

This directory contains a minimal copy of the [FireRedASR](https://github.com/FireRedTeam/FireRedASR) project.
It is used to run an alternative speech recognizer within RTranslator.

## Required model files
Place a pretrained FireRedASR model inside `fireredasr_model` under the app's
files directory. A typical layout for the AED model is:

```
fireredasr_model/
├── cmvn.ark
├── model.pth.tar
├── dict.txt
└── train_bpe1000.model
```

If using the LLM variant you also need `asr_encoder.pth.tar` and the `Qwen2-7B-Instruct` directory.

## Running manually
You can test the recognizer with the helper script:

```bash
python3 fireredasr/cli_transcribe.py <model_dir> <wav_path> <aed|llm>
```

## Android integration
`FireRedAsrRecognizer` wraps this script so it can be called from Java. Set
`USE_FIRERED_ASR` to `true` in `Recognizer.java` to route recognition through
FireRedASR instead of Whisper.
