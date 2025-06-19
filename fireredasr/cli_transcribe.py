#!/usr/bin/env python3
"""Simple wrapper around FireRedASR for RTranslator."""
import sys
from fireredasr.models.fireredasr import FireRedAsr

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: cli_transcribe.py <model_dir> <wav_path> <asr_type>")
        sys.exit(1)
    model_dir, wav_path, asr_type = sys.argv[1:4]
    model = FireRedAsr.from_pretrained(asr_type, model_dir)
    result = model.transcribe(["utt"], [wav_path], {"use_gpu": 0})[0]
    print(result["text"], flush=True)
