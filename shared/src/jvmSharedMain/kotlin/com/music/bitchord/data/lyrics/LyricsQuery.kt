package com.music.bitchord.data.lyrics

/**
 * A YouTube title, as a lyrics database would have indexed it.
 *
 * Every source here but [SimpMusicLyrics] is asked for a name, and the name
 * this app has is YouTube's, which is not the name anyone catalogued. "Dracula
 * (feat. JENNIE)" is filed by Apple, LRCLIB and everyone else as "Dracula", and
 * asked for verbatim it misses all of them — which is exactly how a track ends
 * up answered by whichever source happened to clean the title on its own
 * ([PaxSenix] was the only one that did) regardless of the order the user put
 * their sources in.
 *
 * ### What is taken off, and what is deliberately left on
 *
 * Only credits, packaging, producer brackets, and promotional labels: who else is
 * on the record, how the upload was labelled, and release markers (`[Free DL]`,
 * `(Prod. by ...)`, `[Official Audio]`, `[Out Now]`). Those are not part of the
 * recording's name anywhere.
 *
 * Anything that names a *different recording* stays — "(Remix)", "(Acoustic)",
 * "(Live)", "(Remastered 2011)", "(Sped Up)". Those look like the same kind of
 * bracket and are the opposite: stripping them turns a search for one recording
 * into a search for another, and the lyrics that come back are confidently
 * wrong rather than merely missing. A miss is recoverable; the wrong words
 * scrolling in time with the right song is not.
 */
fun String.forLyricsSearch(): String {
    var name = this
    CREDITS.forEach { pattern -> name = pattern.replace(name, " ") }
    return name.replace(WHITESPACE, " ").trim().trimEnd(',', '-', '–', '—', '|', '/').trim()
        // A title that was *only* packaging is no title at all; better to ask
        // with what we were given than with nothing.
        .ifBlank { trim() }
}

/**
 * Trims auto-generated channel noise (" - Topic", "VEVO", "Official") off an artist name.
 *
 * YouTube's own artist channels for licensed music and official distribution channels
 * are named this way, and they reach the player as the artist on anything played.
 */
fun String.artistForLyricsSearch(): String {
    var name = this.removeSuffix(" - Topic").trim()
    for (suffix in ARTIST_SUFFIXES) {
        if (name.endsWith(suffix, ignoreCase = true)) {
            val candidate = name.dropLast(suffix.length).trim()
            if (candidate.isNotBlank()) {
                name = candidate
            }
        }
    }
    return name.ifBlank { trim() }
}

private val ARTIST_SUFFIXES = listOf(
    " - Official Channel",
    " Official Channel",
    " Official",
    " (Official)",
    " VEVO",
    "VEVO",
)

private val WHITESPACE = Regex("""\s+""")

private val CREDITS = listOf(
    // Bracketed credits: (feat. X), [ft. X], (with X).
    Regex("""\s*[(\[]\s*(feat|ft|featuring|with)\b[^)\]]*[)\]]""", RegexOption.IGNORE_CASE),
    // The same, unbracketed and running to the end of the title.
    Regex("""\s+(feat|ft|featuring)\.?\s+.*$""", RegexOption.IGNORE_CASE),

    // Producer tags: (Prod. by Metro), [prod. Pierre], etc.
    Regex("""\s*[(\[]\s*(prod|produced)\.?\s*(by|:)?\b[^)\]]*[)\]]""", RegexOption.IGNORE_CASE),
    Regex("""\s+(prod|produced)\.?\s+(by|:)\s+.*$""", RegexOption.IGNORE_CASE),

    // Promotional & release labels: [Free DL], [Free Download], [FREE], [Out Now], [Stream/Buy], etc.
    Regex(
        """\s*[(\[]\s*(free(\s*(dl|download|stream|beat))?|out\s*now!?|(buy|stream)(\s*/\s*(buy|stream|download))?|exclusive|premiere|(monstercat|ncs|nocopyrightsounds)\s*(release)?)\s*[)\]]""",
        RegexOption.IGNORE_CASE,
    ),

    // How the upload was labelled, not what was recorded.
    Regex(
        """\s*[(\[]\s*(official\s*)?(music\s*|audio\s*|lyric\s*|visual\s*)?""" +
            """(video|audio|track|visuali[sz]er|lyrics?\s*video|lyrics?|m/?v|hd|hq|4k|1080p|60fps|high\s*quality|full\s*song|full\s*track|clip\s*officiel|video\s*oficial)""" +
            """\s*[)\]]""",
        RegexOption.IGNORE_CASE,
    ),
    Regex("""\s*[(\[]\s*(clean(\s*version)?|explicit|snippet)\s*[)\]]""", RegexOption.IGNORE_CASE),
    Regex("""\s*[(\[]\s*(remix\s+20\d\d|20\d\d\s+remix)\s*[)\]]""", RegexOption.IGNORE_CASE),
    Regex("""\s*[(\[]\s*20\d\d\s*[)\]]""", RegexOption.IGNORE_CASE),
    Regex("""\s*[(\[]\s*official\s*[)\]]""", RegexOption.IGNORE_CASE),

    // Pipe / slash promo trailer: e.g. "Song | Official Music Video"
    Regex(
        """\s*[|/]\s*(official\s*)?(music\s*|audio\s*|lyric\s*)?(video|audio|track|visuali[sz]er|lyrics?|m/?v|hd|hq|4k).*$""",
        RegexOption.IGNORE_CASE,
    ),
)
