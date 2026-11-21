package codes.tashif.paisa.ingest

import codes.tashif.paisa.ai.ladder.AiLadderRunner
import codes.tashif.paisa.ai.ladder.AiService
import codes.tashif.paisa.ai.provider.LlmRequest
import codes.tashif.paisa.data.repository.SettingsRepository

/** The cloud models on the "SMS and alerts" ladder, skipping Gemini Nano rungs. */
class LadderMessageCloud(
    private val runner: AiLadderRunner,
    private val settings: SettingsRepository,
) : MessageCloud {

    override suspend fun canRun(): Boolean =
        settings.getSettings()?.aiEnabled != false && runner.canRun(AiService.MESSAGE_PARSE, cloudOnly = true)

    override suspend fun complete(request: LlmRequest): String =
        runner.run(AiService.MESSAGE_PARSE, request, cloudOnly = true).text
}
