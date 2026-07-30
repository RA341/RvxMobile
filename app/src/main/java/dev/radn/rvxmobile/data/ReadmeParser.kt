package dev.radn.rvxmobile.data

import java.util.regex.Pattern

object ReadmeParser {
    private val HEADER_PATTERN = Pattern.compile("^(#{2,4})\\s+(.*)")
    private val LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+\\.apk)\\)")
    private val PARENTHESIS_PATTERN = Pattern.compile("\\(([^)]+)\\)")

    fun parse(markdown: String): List<AppInfo> {
        val lines = markdown.lineSequence()
        val apps = mutableListOf<AppInfo>()
        var currentApp: AppInfo? = null
        val currentAppPatchers = mutableListOf<PatcherInfo>()
        var currentPatcher: PatcherInfo? = null
        val currentPatcherApks = mutableListOf<ApkInfo>()

        var isOutdated = false
        var isLite = false
        val summaryStack = mutableListOf<String>()

        fun cleanTitle(title: String): String {
            // Strip markdown link brackets like [Morphe:](link) -> Morphe
            val unlinked = title.replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            return unlinked.replace(":", "").replace("<[^>]*>".toRegex(), "").trim()
        }

        for (line in lines) {
            val lineStrip = line.trim()

            if (lineStrip.contains("<details>")) {
                summaryStack.add("")
                continue
            }
            if (lineStrip.contains("</details>")) {
                if (summaryStack.isNotEmpty()) {
                    val popped = summaryStack.removeAt(summaryStack.size - 1)
                    if (popped.contains("Outdated", ignoreCase = true)) {
                        isOutdated = false
                    } else if (popped.contains("Lite", ignoreCase = true)) {
                        isLite = false
                    }
                }
                continue
            }
            if (lineStrip.contains("<summary>")) {
                val summaryMatch = Regex("<summary>(.*?)</summary>").find(lineStrip)
                if (summaryMatch != null) {
                    val summaryText = cleanTitle(summaryMatch.groupValues[1])
                    if (summaryStack.isNotEmpty()) {
                        summaryStack[summaryStack.size - 1] = summaryText
                    }
                    if (summaryText.contains("Outdated", ignoreCase = true)) {
                        isOutdated = true
                    } else if (summaryText.contains("Lite", ignoreCase = true)) {
                        isLite = true
                    }
                }
                continue
            }

            val headerMatcher = HEADER_PATTERN.matcher(lineStrip)
            if (headerMatcher.matches()) {
                val rawContent = headerMatcher.group(2) ?: ""
                val cleaned = cleanTitle(rawContent)

                val hasPlayStore = lineStrip.contains("play.google.com")
                val isMicroG = cleaned.contains("MicroG", ignoreCase = true)

                if (hasPlayStore || isMicroG) {
                    // Save previous patcher if exists
                    if (currentPatcher != null) {
                        currentAppPatchers.add(currentPatcher.copy(apks = currentPatcherApks.toList()))
                        currentPatcherApks.clear()
                        currentPatcher = null
                    }
                    // Save previous app if exists
                    if (currentApp != null) {
                        apps.add(currentApp.copy(patchers = currentAppPatchers.toList()))
                        currentAppPatchers.clear()
                    }

                    val parenMatch = PARENTHESIS_PATTERN.matcher(lineStrip)
                    val linkUrl = if (parenMatch.find()) parenMatch.group(1) ?: "" else ""
                    currentApp = AppInfo(name = cleaned, playStoreLink = linkUrl, patchers = emptyList())
                } else if (currentApp != null) {
                    // Patcher Header
                    if (currentPatcher != null) {
                        currentAppPatchers.add(currentPatcher.copy(apks = currentPatcherApks.toList()))
                        currentPatcherApks.clear()
                    }
                    val githubMatch = Regex("\\((https://github.com/[^)]+)\\)").find(lineStrip)
                    val patcherLink = githubMatch?.groupValues?.get(1) ?: ""
                    currentPatcher = PatcherInfo(name = cleaned, githubLink = patcherLink, apks = emptyList())
                }
            }

            if (currentApp != null && currentPatcher != null) {
                if (lineStrip.startsWith("|") && !lineStrip.startsWith("| --")) {
                    val parts = lineStrip.split("|").map { it.trim() }.filterIndexed { index, _ -> index > 0 }
                    // Col 0: Stable, Col 1: Beta
                    for ((colIdx, part) in parts.withIndex()) {
                        val isBetaCol = colIdx == 1
                        val linkMatcher = LINK_PATTERN.matcher(part)
                        while (linkMatcher.find()) {
                            val label = linkMatcher.group(1) ?: ""
                            val rawUrl = linkMatcher.group(2) ?: ""
                            val isBeta = isBetaCol || rawUrl.contains("beta", ignoreCase = true) || label.contains("beta", ignoreCase = true)
                            currentPatcherApks.add(
                                ApkInfo(
                                    label = label,
                                    url = rawUrl,
                                    isBeta = isBeta,
                                    isLite = isLite,
                                    isOutdated = isOutdated
                                )
                            )
                        }
                    }
                } else {
                    // Non-table inline links
                    val linkMatcher = LINK_PATTERN.matcher(lineStrip)
                    while (linkMatcher.find()) {
                        val label = linkMatcher.group(1) ?: ""
                        val rawUrl = linkMatcher.group(2) ?: ""
                        val isBeta = rawUrl.contains("beta", ignoreCase = true) || label.contains("beta", ignoreCase = true)
                        currentPatcherApks.add(
                            ApkInfo(
                                label = label,
                                url = rawUrl,
                                isBeta = isBeta,
                                isLite = isLite,
                                isOutdated = isOutdated
                            )
                        )
                    }
                }
            }
        }

        // Flush remaining
        if (currentPatcher != null && currentApp != null) {
            currentAppPatchers.add(currentPatcher.copy(apks = currentPatcherApks.toList()))
        }
        if (currentApp != null) {
            apps.add(currentApp.copy(patchers = currentAppPatchers.toList()))
        }

        return apps
    }
}
