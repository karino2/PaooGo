package io.github.karino2.paoogo.ui.vs_engine

import org.ligi.gobandroid_hd.logic.GoGame

class EngineGoGame internal constructor(
    val playingBlack: Boolean,
    val playingWhite: Boolean,
    private val game: GoGame
) {

    var aiIsThinking = false

    fun engineNowWhite(): Boolean {
        return !game.isBlackToMove && playingWhite
    }

    fun engineNowBlack(): Boolean {
        return game.isBlackToMove && playingBlack
    }
}