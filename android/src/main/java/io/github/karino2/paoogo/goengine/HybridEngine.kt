package io.github.karino2.paoogo.goengine


class Policy(val threshold : Int) {
    fun isStrong(te: Int) : Boolean {
        return te < threshold
    }
}

class HybridEngine(private val strong: GoEngine, private val weak: GoEngine, val policy: Policy) : GoEngine {
    var te = 0
    override fun genMoveInternal(isBlack: Boolean): Int {
        te += 1
        val (main, sub) =  if(policy.isStrong(te)) {
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
        return "te=${te}, isFirst=${policy.isStrong(te)}"
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