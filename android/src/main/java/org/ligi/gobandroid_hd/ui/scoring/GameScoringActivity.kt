package org.ligi.gobandroid_hd.ui.scoring

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import io.github.karino2.paoogo.ui.GameStartActivity
import io.github.karino2.paoogo.ui.ReviewActivity
import org.ligi.gobandroid_hd.InteractionScope

import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.events.GameChangedEvent
import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.GoGame
import org.ligi.gobandroid_hd.logic.GoGameMetadata
import org.ligi.gobandroid_hd.logic.StatelessBoardCell
import org.ligi.gobandroid_hd.logic.cell_gatherer.LooseConnectedCellGatherer
import org.ligi.gobandroid_hd.logic.cell_gatherer.MustBeConnectedCellGatherer
import org.ligi.gobandroid_hd.ui.GoActivity
import org.ligi.gobandroid_hd.ui.GoPrefs
import org.ligi.gobandroid_hd.ui.recording.SaveSGFDialog
import java.util.*
import kotlin.reflect.KClass

/**
 * Activity to score a Game
 */
class GameScoringActivity : GoActivity() {
    override val gameExtraFragment: GameScoringExtrasFragment by lazy {
        GameScoringExtrasFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO the next line works but needs investigation - i thought more of
        // getBoard().requestFocus(); - but that was not working ..
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        for (boardCells in inclusiveGroups) {
            // TODO find a nicer approach to detect dead stones - this does only cover the common cases
            if (boardCells.size <= 4) {
                for (boardCell in boardCells) {
                    game.calcBoard.toggleCellDead(boardCell)
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        game.initScorer()
        gameExtraFragment.refresh()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.ingame_score, menu)
        return super.onCreateOptionsMenu(menu)
    }


    private val inclusiveGroups: Set<Set<StatelessBoardCell>>
        get() {
            val allProcessed = HashSet<Cell>()
            val allGroups = HashSet<Set<StatelessBoardCell>>()

            game.statelessGoBoard.withAllCells {
                val cell = game.statelessGoBoard.getCell(it)
                val inGroup = LooseConnectedCellGatherer(game.calcBoard, cell).gatheredCells
                allProcessed.addAll(inGroup)
                if (!game.calcBoard.isCellFree(it)) {
                    allGroups.add(inGroup)
                }
            }

            return allGroups
        }

    override fun doTouch(event: MotionEvent) {
        //super.doTouch(event); - Do not call! Not needed and breaks marking dead stones

        eventForZoomBoard(event)
        val touchCell = binding.goBoard.pixel2cell(event.x, event.y)
        interactionScope.touchCell = touchCell

        // calculate position on the field by position on the touchscreen

        if (event.action == MotionEvent.ACTION_UP && touchCell != null) {
            doMoveWithUIFeedback(touchCell)
            interactionScope.touchCell = null
        }

    }

    override fun doMoveWithUIFeedback(cell: Cell?): GoGame.MoveStatus {
        if (cell != null) {
            do_score_touch(cell)
        }
        bus.post(GameChangedEvent)
        return GoGame.MoveStatus.VALID
    }

    private val getStoreDirUrl = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        // if cancel, null coming.
        uri?.let {
            contentResolver.takePersistableUriPermission(it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            GoPrefs.storeDirUri = it.toString()
            saveSGFDialogUnder(it)
        }
    }

    fun saveSGFDialogUnder(dirUri: Uri) {
        SaveSGFDialog(this, dirUri, {
            getStoreDirUrl.launch(null)
        }).show()
    }

    fun saveSGF() {
        if(GoPrefs.storeDirUri.isEmpty()) {
            getStoreDirUrl.launch(null)
        } else {
            saveSGFDialogUnder(Uri.parse(GoPrefs.storeDirUri))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_back_to_title -> {
                gameProvider.set(GoGame(game.size))
                Intent(this, GameStartActivity::class.java).apply{ flags = Intent.FLAG_ACTIVITY_CLEAR_TOP  }.let { startActivity(it) }
                return true
            }
            R.id.menu_goto_review -> {
                switchToReview()

            }
            R.id.menu_write_sgf -> {
                saveSGF()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        // if we go back to other modes we want to have them alive again ( Zombies ?)
        game.statelessGoBoard.withAllCells {
            if (game.calcBoard.isCellDead(it)) {
                game.calcBoard.toggleCellDead(it)
            }
        }

        game.copyVisualBoard()
        game.removeScorer()
    }


    fun do_score_touch(cell: Cell) {
        val calcBoard = game.calcBoard
        if (!calcBoard.isCellFree(cell) || calcBoard.isCellDead(cell)) {
            // if there is a stone/cellGathering
            val cellGathering = MustBeConnectedCellGatherer(calcBoard, calcBoard.getCell(cell)).gatheredCells
            for (groupCell in cellGathering) {
                calcBoard.toggleCellDead(groupCell)
            }
        }

        game.scorer?.calculateScore()

    }
}
