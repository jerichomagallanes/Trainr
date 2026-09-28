#include <android/log.h>
#include <jni.h>

#include <algorithm>
#include <atomic>
#include <string>
#include <vector>

#include "ggml-backend.h"
#include "llama.h"

namespace {

constexpr const char * TAG = "trainr_llama";
constexpr uint32_t CONTEXT_TOKENS = 1024;
constexpr int32_t BATCH_TOKENS = 512;

struct Session {
    llama_model * model = nullptr;
    llama_context * context = nullptr;
    std::atomic<bool> cancelled{false};
};

void fail(const char * what) {
    __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", what);
}

void forward_log(ggml_log_level level, const char * text, void *) {
    if (level == GGML_LOG_LEVEL_ERROR) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "%s", text);
    }
}

bool should_abort(void * data) {
    return static_cast<Session *>(data)->cancelled.load();
}

Session * session_of(jlong handle) {
    return reinterpret_cast<Session *>(handle);
}

// jstring is modified UTF-8, which the tokenizer would misread outside the BMP.
std::string bytes_of(JNIEnv * env, jbyteArray array) {
    const jsize length = env->GetArrayLength(array);
    std::string out(static_cast<size_t>(length), '\0');
    env->GetByteArrayRegion(array, 0, length, reinterpret_cast<jbyte *>(out.data()));
    return out;
}

jbyteArray bytes_to_java(JNIEnv * env, const std::string & text) {
    const auto length = static_cast<jsize>(text.size());
    jbyteArray out = env->NewByteArray(length);
    if (out != nullptr) {
        env->SetByteArrayRegion(out, 0, length, reinterpret_cast<const jbyte *>(text.data()));
    }
    return out;
}

bool format_prompt(
    const llama_model * model,
    const std::string & system,
    const std::string & user,
    std::string & prompt
) {
    const char * tmpl = llama_model_chat_template(model, nullptr);
    if (tmpl == nullptr) {
        fail("model has no chat template");
        return false;
    }
    const llama_chat_message messages[] = {
        {"system", system.c_str()},
        {"user", user.c_str()},
    };
    std::vector<char> buffer((system.size() + user.size()) * 2 + 256);
    int32_t written = llama_chat_apply_template(
        tmpl, messages, 2, true, buffer.data(), static_cast<int32_t>(buffer.size()));
    if (written > static_cast<int32_t>(buffer.size())) {
        buffer.resize(static_cast<size_t>(written));
        written = llama_chat_apply_template(
            tmpl, messages, 2, true, buffer.data(), static_cast<int32_t>(buffer.size()));
    }
    if (written < 0) {
        fail("chat template could not be applied");
        return false;
    }
    prompt.assign(buffer.data(), static_cast<size_t>(written));
    return true;
}

bool tokenize(const llama_vocab * vocab, const std::string & text, std::vector<llama_token> & tokens) {
    const auto length = static_cast<int32_t>(text.size());
    int32_t count = llama_tokenize(vocab, text.c_str(), length, nullptr, 0, true, true);
    if (count == INT32_MIN) {
        fail("prompt too long to tokenize");
        return false;
    }
    tokens.resize(static_cast<size_t>(count < 0 ? -count : count));
    count = llama_tokenize(
        vocab, text.c_str(), length, tokens.data(), static_cast<int32_t>(tokens.size()), true, true);
    if (count < 0) {
        fail("prompt could not be tokenized");
        return false;
    }
    tokens.resize(static_cast<size_t>(count));
    return true;
}

bool decode(llama_context * context, llama_token * tokens, int32_t count) {
    for (int32_t offset = 0; offset < count; offset += BATCH_TOKENS) {
        const int32_t n = std::min(BATCH_TOKENS, count - offset);
        if (llama_decode(context, llama_batch_get_one(tokens + offset, n)) != 0) {
            return false;
        }
    }
    return true;
}

bool append_piece(const llama_vocab * vocab, llama_token token, std::string & text) {
    std::vector<char> piece(64);
    int32_t n = llama_token_to_piece(
        vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
    if (n < 0) {
        piece.resize(static_cast<size_t>(-n));
        n = llama_token_to_piece(
            vocab, token, piece.data(), static_cast<int32_t>(piece.size()), 0, false);
    }
    if (n < 0) {
        return false;
    }
    text.append(piece.data(), static_cast<size_t>(n));
    return true;
}

} // namespace

extern "C" JNIEXPORT void JNICALL
Java_com_jericx_trainr_llama_LlamaNative_loadBackends(JNIEnv * env, jobject, jstring jdirectory) {
    static std::atomic<bool> loaded{false};
    if (loaded.exchange(true)) {
        return;
    }
    llama_log_set(forward_log, nullptr);
    const char * directory = env->GetStringUTFChars(jdirectory, nullptr);
    ggml_backend_load_all_from_path(directory);
    env->ReleaseStringUTFChars(jdirectory, directory);
    llama_backend_init();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_jericx_trainr_llama_LlamaNative_load(JNIEnv * env, jobject, jstring jpath, jint threads) {
    llama_model_params model_params = llama_model_default_params();
    model_params.load_mode = LLAMA_LOAD_MODE_MMAP;

    const char * path = env->GetStringUTFChars(jpath, nullptr);
    llama_model * model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(jpath, path);
    if (model == nullptr) {
        fail("model could not be loaded");
        return 0;
    }

    auto * session = new Session;
    session->model = model;

    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = CONTEXT_TOKENS;
    context_params.n_batch = BATCH_TOKENS;
    context_params.n_ubatch = BATCH_TOKENS;
    context_params.n_threads = threads;
    context_params.n_threads_batch = threads;
    context_params.abort_callback = should_abort;
    context_params.abort_callback_data = session;

    session->context = llama_init_from_model(model, context_params);
    if (session->context == nullptr) {
        fail("context could not be created");
        llama_model_free(model);
        delete session;
        return 0;
    }
    return reinterpret_cast<jlong>(session);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_jericx_trainr_llama_LlamaNative_complete(
    JNIEnv * env,
    jobject,
    jlong handle,
    jbyteArray jsystem,
    jbyteArray juser,
    jbyteArray jgrammar,
    jint max_tokens
) {
    Session * session = session_of(handle);
    if (session == nullptr) {
        fail("no model loaded");
        return nullptr;
    }
    session->cancelled.store(false);

    const std::string system = bytes_of(env, jsystem);
    const std::string user = bytes_of(env, juser);
    const std::string grammar = bytes_of(env, jgrammar);

    std::string prompt;
    if (!format_prompt(session->model, system, user, prompt)) {
        return nullptr;
    }
    const llama_vocab * vocab = llama_model_get_vocab(session->model);
    std::vector<llama_token> tokens;
    if (!tokenize(vocab, prompt, tokens)) {
        return nullptr;
    }
    if (tokens.size() + static_cast<size_t>(max_tokens) > llama_n_ctx(session->context)) {
        fail("prompt and answer would not fit the context");
        return nullptr;
    }

    llama_sampler * grammar_sampler = llama_sampler_init_grammar(vocab, grammar.c_str(), "root");
    if (grammar_sampler == nullptr) {
        fail("grammar did not parse");
        return nullptr;
    }
    llama_sampler * sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, grammar_sampler);
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());

    llama_memory_clear(llama_get_memory(session->context), true);

    std::string text;
    bool ok = decode(session->context, tokens.data(), static_cast<int32_t>(tokens.size()));
    for (int32_t produced = 0; ok && produced < max_tokens; produced++) {
        if (session->cancelled.load()) {
            ok = false;
            break;
        }
        llama_token token = llama_sampler_sample(sampler, session->context, -1);
        if (llama_vocab_is_eog(vocab, token)) {
            break;
        }
        ok = append_piece(vocab, token, text) &&
            llama_decode(session->context, llama_batch_get_one(&token, 1)) == 0;
    }
    llama_sampler_free(sampler);

    if (!ok) {
        if (!session->cancelled.load()) {
            fail("decoding failed");
        }
        return nullptr;
    }
    return bytes_to_java(env, text);
}

extern "C" JNIEXPORT void JNICALL
Java_com_jericx_trainr_llama_LlamaNative_cancel(JNIEnv *, jobject, jlong handle) {
    Session * session = session_of(handle);
    if (session != nullptr) {
        session->cancelled.store(true);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_jericx_trainr_llama_LlamaNative_free(JNIEnv *, jobject, jlong handle) {
    Session * session = session_of(handle);
    if (session == nullptr) {
        return;
    }
    llama_free(session->context);
    llama_model_free(session->model);
    delete session;
}
