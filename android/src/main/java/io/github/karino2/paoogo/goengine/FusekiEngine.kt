package io.github.karino2.paoogo.goengine

import io.github.karino2.paoogo.goengine.fuseki.FusekiSet
import org.ligi.gobandroid_hd.logic.CellImpl
import kotlin.math.max
import kotlin.random.Random

enum class FusekiLevel {
    SELDOM,
    ALMOST
}

class FusekiEngine(val fusekiSetOrg: FusekiSet, val level: FusekiLevel, val engine: GoEngine) : GoEngine {
    var fusekiSet = fusekiSetOrg.clone()

    var pos = 0

    var fusekiValid = true

    val stillFuseki: Boolean
        get() = fusekiValid && fusekiSet.stillFuseki

    val fusekiRate: Double
        get() {
            when(level) {
                FusekiLevel.SELDOM -> {
                    // first 2, 90%
                    if(pos < 3)
                        return 0.9
                    // after that, 50%
                    return 0.5
                }
                FusekiLevel.ALMOST -> {
                    // first 4, 95%
                    if(pos < 4)
                        return 0.95
                    // 0.90, 0.85, 0.80, 0.75 for each white move
                    return max(0.0, 0.95 + 0.05 - (pos/2)*0.5)
                }
            }
        }

    override fun genMoveInternal(isBlack: Boolean): Int {
        if(stillFuseki)
        {
            println("rate: $fusekiRate")
            if(Random.nextDouble() < fusekiRate) {
                pos += 1
                val next = fusekiSet.peekMove()
                fusekiSet.moveTo(next)
                engine.doMove(next.x, next.y, isBlack)
                return GoEngine.xyToIntMove(next.x, next.y)
            }
        }
        val nextInt = engine.genMoveInternal(isBlack)
        if (stillFuseki) {
            val move = GoEngine.internalToPos(nextInt)
            fusekiSet.moveTo(CellImpl(move.x, move.y))
        }
        pos += 1

        return nextInt
    }

    override fun debugInfo(): String? {
        return "stillFuseki: ${fusekiSet.stillFuseki}, remainNum: ${fusekiSet.fusekiList.size}"
    }

    override fun setKomi(komi: Float) {
        engine.setKomi(komi)
        pos = 0
    }

    override fun clearBoard() {
        engine.clearBoard()
        fusekiSet = fusekiSetOrg.clone()
        pos = 0
    }

    override fun setBoardSize(size: Int) {
        engine.setBoardSize(size)
        fusekiValid = size == 9
        pos = 0
    }

    override fun doMove(x: Int, y: Int, isBlack: Boolean): Boolean {
        pos += 1
        if(stillFuseki)
            fusekiSet.moveTo(CellImpl(x, y))
        return engine.doMove(x, y, isBlack)
    }

    override fun doPass(isBlack: Boolean) {
        pos += 1
        // once player pass, do not use fuseki anymore.
        fusekiValid = false
        engine.doPass(isBlack)
    }
}