package io.github.karino2.paoogo.goengine

import io.github.karino2.paoogo.goengine.gnugo2.MovePos

interface GoEngine : EngineConfig {
    companion object {
        fun internalToPos(move: Int): MovePos {
            if (move == -1) {
                return MovePos.Companion.PASS
            }
            return MovePos(move and 0xff, move shr 16)
        }

        fun xyToIntMove(x: Int, y:Int) =
            (y shl 16) or x
    }
    fun genMoveInternal(isBlack: Boolean) : Int
    fun genMove(isBlack: Boolean) : MovePos {
        val move = genMoveInternal(isBlack)
        return internalToPos(move)
    }


    fun debugInfo(): String?
}