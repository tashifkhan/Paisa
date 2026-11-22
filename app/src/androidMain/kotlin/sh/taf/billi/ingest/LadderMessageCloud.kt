package sh.taf.billi.ingest

import sh.taf.billi.ai.ladder.AiLadderRunner
import sh.taf.billi.ai.ladder.AiService
import sh.taf.billi.ai.provider.LlmRequest
import sh.taf.billi.data.repository.SettingsRepository

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
