package com.translate.myapplication.runpoad

data class TranscriptionResponse(
    val identifier: String,
    val status: String,
    val task_type: String,
    val result_type: String,
    val result: List<ResultItem>,
    val task_params: TaskParams,
    val error: String?,
    val duration: Double,
    val progress: Double
)

data class ResultItem(
    val id: Any?,
    val seek: Any?,
    val text: String,
    val start: Double,
    val end: Double,
    val tokens: Any?,
    val temperature: Any?,
    val avg_logprob: Any?,
    val compression_ratio: Any?,
    val no_speech_prob: Any?,
    val words: Any?
)

data class TaskParams(
    val whisper: WhisperParams,
    val vad: VadParams,
    val diarization: DiarizationParams,
    val bgm_separation: BgmSeparationParams
)

data class WhisperParams(
    val model_size: String,
    val lang: String?,
    val is_translate: Boolean,
    val beam_size: Int,
    val log_prob_threshold: Double,
    val no_speech_threshold: Double,
    val compute_type: String,
    val best_of: Int,
    val patience: Double,
    val condition_on_previous_text: Boolean,
    val prompt_reset_on_temperature: Double,
    val initial_prompt: String?,
    val temperature: Double,
    val compression_ratio_threshold: Double,
    val length_penalty: Double,
    val repetition_penalty: Double,
    val no_repeat_ngram_size: Int,
    val prefix: String?,
    val suppress_blank: Boolean,
    val suppress_tokens: List<Int>,
    val max_initial_timestamp: Double,
    val word_timestamps: Boolean,
    val prepend_punctuations: String,
    val append_punctuations: String,
    val max_new_tokens: Any?,
    val chunk_length: Int,
    val hallucination_silence_threshold: Any?,
    val hotwords: Any?,
    val language_detection_threshold: Double,
    val language_detection_segments: Int,
    val batch_size: Int,
    val enable_offload: Boolean
)

data class VadParams(
    val vad_filter: Boolean,
    val threshold: Double,
    val min_speech_duration_ms: Int,
    val max_speech_duration_s: Any?,
    val min_silence_duration_ms: Int,
    val speech_pad_ms: Int
)

data class DiarizationParams(
    val is_diarize: Boolean,
    val diarization_device: String,
    val hf_token: String,
    val enable_offload: Boolean
)

data class BgmSeparationParams(
    val is_separate_bgm: Boolean,
    val uvr_model_size: String,
    val uvr_device: String,
    val segment_size: Int,
    val save_file: Boolean,
    val enable_offload: Boolean
)
