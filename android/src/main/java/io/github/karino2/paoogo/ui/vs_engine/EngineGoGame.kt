package io.github.karino2.paoogo.ui.vs_engine

import org.ligi.gobandroid_hd.logic.GoGame

class EngineGoGame internal constructor(
    private val game: GoGame
) {
    var playingBlack = false
    var playingWhite = true

    var aiIsThinking = false

    fun engineNowWhite(): Boolean {
        return !game.isBlackToMove && playingWhite
    }

    fun engineNowBlack(): Boolean {
        return game.isBlackToMove && playingBlack
    }

    fun setPlayerIsBlack(playerBlack: Boolean) {
        playingBlack = !playerBlack
        playingWhite = !playingBlack
    }
}