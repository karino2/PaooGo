package io.github.karino2.paoogo.goengine

import android.util.Log
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random


class Policy(val level : Int) {
    /*
       policy is adjusted for gnugo2.8 and liberty.
       For opening, liberty is too weak, so use stronger one.

       In the middle, we want to use liberty.
       For end game, we want to use gnugo2.8 again.

       So the ideal value of isStrong prob is:

       |<---- opening -->!<-- middle -- -->|<-- end game -->|
       1.0, 1.0 ..., 1.0, 0.0, 0.0 ..., 0.0, 1.0, 1.0, 1.0...

       But when middle end eng game begins is hard to know, so interpolate between 0.0 and 1.0,
       use that prob to randomize result.

       We have two guessed number:

       END_OF_OPENING (about 14 te in 9x9)
       MIDDLE

       MIDDLE is the peak of weaker engine prob.
       And We define the start of endgame as the point opposite to init, centered on middle.
       BEGIN_OF_ENDGAME = 2*MIDDLE - END_OF_OPENING
       After BEGIN_OF_ENDGAME, the prob becomes 1.0
     */
    private fun weakProb(te: Int, total: Int) : Double{
        // 14
        val endOfOpening = (total*14)/81

        val middle = when(level) {
            1-> total*2/3
            2-> total/2
            else-> total/3
        }

        val w = abs(middle - endOfOpening).toDouble()
        val beginEndGame = 2*middle - endOfOpening
        return if (te < endOfOpening) {
            0.0 // strong
        } else if (middle-te > 0) {
            /* interpolation from 1.0 to 0.5 toward middle, concave */
            1.0 - 0.5*(te-endOfOpening).toDouble().pow(2.0)/w.pow(2.0)
        } else if(te > beginEndGame) {
            0.0 // strong
        } else {
            /* interpolation from 0.5 to 0.0 toward beginEndGame, concave */
            0.5*(te-beginEndGame).toDouble().pow(2.0)/w.pow(2.0)
        }
    }
    fun isStrong(te: Int, boardSize: Int) : Boolean {
        val pval = weakProb(te, boardSize*boardSize)
        val isStrong = Random.nextDouble() > pval
        Log.d("PaooGo", "te=${te}, prob=${pval}, isStrong=${isStrong}")

        return isStrong
    }
}

class HybridEngine(private val strong: GoEngine, private val weak: GoEngine, val policy: Policy) : GoEngine {
    private var te = 0
    private var boardSize = 9
    private var lastStrong = true
    override fun genMoveInternal(isBlack: Boolean): Int {
        te += 1
        lastStrong = policy.isStrong(te, boardSize)
        val (main, sub) =  if(lastStrong) {
            Pair(strong, weak)
        } else {
            Pair(weak, strong)
        }
        val res = main.genMoveInternal(isBlack)
        val pos = GoEngine.internalToPos(res)
        if (pos.pass)
            sub.doPass(isBlack)
        else
            sub.doMove(pos.x, pos.y, isBlack)
        return res
    }

    override fun debugInfo(): String? {
        return "te=${te}, lastStrong=${lastStrong}"
    }

    override fun setKomi(komi: Float) {
        strong.setKomi(komi)
        weak.setKomi(komi)
    }

    override fun clearBoard() {
        strong.clearBoard()
        weak.clearBoard()
        te = 0
    }

    override fun setBoardSize(size: Int) {
        strong.setBoardSize(size)
        weak.setBoardSize(size)
        boardSize = size
    }

    override fun doMove(x: Int, y: Int, isBlack: Boolean): Boolean {
        te += 1
        strong.doMove(x, y, isBlack)
        return weak.doMove(x, y, isBlack)
    }

    override fun doPass(isBlack: Boolean) {
        te += 1
        strong.doPass(isBlack)
        weak.doPass(isBlack)
    }

}