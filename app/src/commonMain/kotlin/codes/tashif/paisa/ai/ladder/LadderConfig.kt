package codes.tashif.paisa.ai.ladder

import codes.tashif.paisa.ai.store.AiConfig

/** Reads and edits the ladders inside an [AiConfig]. Pure. */
object LadderConfig {

    /** The ladder [service] runs on: its own, or the default. Null is the Default ladder. */
    fun rungs(config: AiConfig, service: AiService?): List<Rung> =
        if (service == null) config.defaultLadder else config.serviceLadders[service] ?: config.defaultLadder

    /**
     * Applies [change] to the ladder of [service] in [config]. An empty
     * result on a service makes it use the default again. When nothing
     * changes, [config] comes back as it was.
     */
    fun edit(config: AiConfig, service: AiService?, change: (List<Rung>) -> List<Rung>): AiConfig {
        val current = rungs(config, service)
        val next = change(current)
        if (next == current) return config
        return when {
            service == null -> config.copy(defaultLadder = next)
            next.isEmpty() -> config.copy(serviceLadders = config.serviceLadders - service)
            else -> config.copy(serviceLadders = config.serviceLadders + (service to next))
        }
    }
}
