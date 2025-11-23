package io.github.karino2.paoogo.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import androidx.fragment.app.Fragment
import io.github.karino2.paoogo.ui.vs_engine.Message
import io.github.karino2.paoogo.ui.vs_engine.PlayAgainstEngineActivity
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.GTPHelper
import org.ligi.gobandroid_hd.logic.GoGame
import org.ligi.gobandroid_hd.logic.sgf.SGFReader
import org.ligi.gobandroid_hd.ui.GoActivity

class ReviewActivity: GoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        game.clearHint()

        intent?.getStringExtra("SGF_DATA")?.let {text->
            SGFReader.sgf2game(text, null)?.let {
                gameProvider.set(it)
            }
        }
    }

    fun formatPV(cells: List<Cell>) : String {
        return "PV: " + cells.joinToString(" ") {
            GTPHelper.coordinates2gtpstr(it, game.boardSize)
        }
    }

    override fun doMoveWithUIFeedback(cell: Cell?): GoGame.MoveStatus {
        cell?.let { game.findAnalyzeInfo(it) }?.let {
            bus.post(Message(formatPV(it.pv)))
        }
        game.clearAnalyzerInfo()
        game.ensureStartReviewVariation()
        return super.doMoveWithUIFeedback(cell)
    }

    override val gameExtraFragment: Fragment
        get() = ReviewFragment()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.ingame_review, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_back_to_title -> {
                goToTitle()
                return true
            }
            R.id.menu_resume_from_here -> {
                gameProvider.set(game.becomeMainline())
                Intent(this, PlayAgainstEngineActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }.let { startActivity(it) }
                return true
            }
            else ->
                return super.onOptionsItemSelected(item)
        }
    }
}