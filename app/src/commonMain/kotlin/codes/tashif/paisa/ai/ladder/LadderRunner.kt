package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.LlmException
import codes.tashif.paisa.ai.LlmFailure
import codes.tashif.paisa.ai.provider.LlmCall
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.ai.provider.ModelCaller
import kotlinx.coroutines.CancellationException
import codes.tashif.paisa.ai.json.JSONArray
import codes.tashif.paisa.ai.json.JSONObject
import codes.tashif.paisa.ai.json.JSONTokener

/** A rung after its provider, key and catalogue entry are looked up. */
sealed interface ResolvedRung {
    val label: String

    data class Ready(val call: LlmCall, override val label: String) : ResolvedRung
    data class Skip(override val label: String, val reason: String) : ResolvedRung
}

data class RungFailure(val label: String, val reason: String, val skipped: Boolean)

data class LadderResult(
    val text: String,
    val rung: Rung,
    val label: String,
    /** Rungs that failed or were skipped before this one answered. */
    val failures: List<RungFailure>,
)

/** Every rung failed or was skipped. The message lists each one. */
class AiLadderException(
    message: String,
    val failures: List<RungFailure>,
) : LlmException(message, LlmFailure.CONFIG)

/**
 * Tries rungs top to bottom. It moves on after an HTTP error, a network
 * error, a timeout, an empty reply, or, when JSON was asked for, a reply
 * that doesn't parse as JSON. Rungs that can't take the input, or whose
 * provider has no key, are skipped without a call.
 */
object LadderRunner {

    suspend fun run(
        serviceLabel: String,
        rungs: List<Rung>,
        request: LlmRequest,
        resolve: (Rung) -> ResolvedRung,
        caller: ModelCaller,
    ): LadderResult {
        if (rungs.isEmpty()) {
            throw AiLadderException("No models set up for $serviceLabel. Add one under AI.", emptyList())
        }
        val failures = ArrayList<RungFailure>()
        for (rung in rungs) {
            val ready = when (val resolved = resolve(rung)) {
                is ResolvedRung.Skip -> {
                    failures += RungFailure(resolved.label, resolved.reason, skipped = true)
                    continue
                }
                is ResolvedRung.Ready -> resolved
            }
            val inputReason = inputProblem(ready.call, request)
            if (inputReason != null) {
                failures += RungFailure(ready.label, inputReason, skipped = true)
                continue
            }
            try {
                val text = caller.complete(ready.call, request)
                val problem = replyProblem(text, request.json)
                if (problem == null) return LadderResult(text, rung, ready.label, failures)
                failures += RungFailure(ready.label, problem, skipped = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures += RungFailure(ready.label, e.message ?: e::class.simpleName.orEmpty(), skipped = false)
            }
        }
        throw AiLadderException(failureMessage(serviceLabel, failures), failures)
    }

    /** Why this rung can't take the request, or null when it can. Unknown capability gets a try. */
    fun inputProblem(call: LlmCall, request: LlmRequest): String? {
        val info = call.modelInfo
        if (request.image != null && info?.acceptsImage == false) return "doesn't read images"
        if (request.pdf != null && (!call.style.carriesPdf || info?.acceptsPdf != true)) {
            return "doesn't read PDFs"
        }
        // Unknown counts as no: most models that can't hear reject the part outright.
        if (request.audio != null && (!call.style.carriesAudio || info?.acceptsAudio != true)) {
            return "doesn't take audio"
        }
        return null
    }

    /** Why a reply can't be used, or null when it's fine. */
    fun replyProblem(text: String, wantJson: Boolean): String? = when {
        text.isBlank() -> "empty reply"
        wantJson && !JsonReply.parses(text) -> "reply wasn't valid JSON"
        else -> null
    }

    fun failureMessage(serviceLabel: String, failures: List<RungFailure>): String {
        val detail = failures.joinToString("; ") { "${it.label}: ${it.reason}" }
        val head = if (failures.all { it.skipped }) {
            "No model in the $serviceLabel ladder could run."
        } else {
            "Every model in the $serviceLabel ladder failed."
        }
        return "$head $detail"
    }
}

/** Whether a model reply holds JSON, allowing for code fences and text around it. */
object JsonReply {
    fun parses(text: String): Boolean {
        val body = text.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```")
            .trim()
        if (tryParse(body)) return true
        val start = body.indexOfFirst { it == '{' || it == '[' }
        if (start < 0) return false
        val close = if (body[start] == '{') '}' else ']'
        val end = body.lastIndexOf(close)
        return end > start && tryParse(body.substring(start, end + 1))
    }

    private fun tryParse(candidate: String): Boolean {
        if (candidate.isEmpty()) return false
        return try {
            val value = JSONTokener(candidate).nextValue()
            value is JSONObject || value is JSONArray
        } catch (_: Exception) {
            false
        }
    }
}
