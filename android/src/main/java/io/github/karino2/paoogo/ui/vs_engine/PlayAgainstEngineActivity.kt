package io.github.karino2.paoogo.ui.vs_engine

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.github.karino2.paoogo.goengine.EngineConfig
import io.github.karino2.paoogo.goengine.EngineRepository
import io.github.karino2.paoogo.goengine.GoAnalyzer
import io.github.karino2.paoogo.goengine.GoEngine
import org.greenrobot.eventbus.Subscribe
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.events.GameChangedEvent
import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.GoDefinitions
import org.ligi.gobandroid_hd.logic.GoGame
import org.ligi.gobandroid_hd.ui.GoActivity
import org.ligi.gobandroid_hd.ui.GoPrefs
import timber.log.Timber
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Waiter(private val minMillis: Long) {

    private val startTime = System.currentTimeMillis()

    suspend fun mayWait() {
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        val requiredDelay = minMillis - elapsedTime

        if (requiredDelay > 0) {
            delay(requiredDelay)
        }
    }
}

/*
    Similar to PlayAgainstGnuGoActivity, but use local engine instead.
 */
class PlayAgainstEngineActivity : GoActivity() {
    private val engineGoGame by lazy { EngineGoGame(false, true, game) }
    private lateinit var engine : GoEngine

    private val engineRepository : EngineRepository
        get() = app.engineRepository

    private val analyzer by lazy {
        engineRepository.getAnalyzer().apply {
            setKomi(game.komi)
            setBoardSize(game.boardSize)
            clearBoard()
        }
    }

    private val busyIndicator: ProgressBar by lazy {
        findViewById(R.id.busy_indicator)
    }

    private var running = false
    private var syncing = false;

    override fun doTouch(event: MotionEvent) {
        if (!::engine.isInitialized)
            return
        if (engineGoGame.engineNowBlack() or engineGoGame.engineNowWhite()) {
            showInfoToast(R.string.not_your_turn)
        } else {
            super.doTouch(event)
        }
    }

    override fun onPause() {
        running = false
        super.onPause()
    }
    val handler = Handler(Looper.getMainLooper())

    override fun onResume() {
        super.onResume()

        // Timber.plant(Timber.DebugTree())
        running = true
        busyIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                setupEngine()
            }
            updatePlayerName()
            syncFromScratch()
            busyIndicator.visibility = View.GONE

            Thread( {
                        while(running) {
                            SystemClock.sleep(100)
                            handler.post { engineTick() }
                        }
            }).start()
        }
    }

    private fun engineTick()
    {
        if (syncing || engineGoGame.aiIsThinking)
            return
        if (engineGoGame.engineNowBlack() || engineGoGame.engineNowWhite() ) {
            genMove()
        }
    }

    private fun setupEngine() {
        val epair = engineRepository.getEngine(GoPrefs.engineLevel)
        engine = epair.first
        game.setPlayerName( getString(R.string.you),getString(epair.second))
        engine.setKomi(game.komi)
        engine.setBoardSize(game.boardSize)
        engine.clearBoard()
    }

    private fun syncAnalyzer() : GoAnalyzer {
        analyzer.sync(game)
        return analyzer
    }

    public override fun doMoveWithUIFeedback(cell: Cell?): GoGame.MoveStatus {
        if (cell != null) {
            if (engineGoGame.aiIsThinking) {
                Toast.makeText(this, R.string.ai_is_thinking, Toast.LENGTH_SHORT).show()
                return GoGame.MoveStatus.VALID
            }
            game.clearHint()
            // use value before doMoveWithUIFeedback.
            val isBlack = game.isBlackToMove
            val ret = super.doMoveWithUIFeedback(cell)
            if (ret != GoGame.MoveStatus.VALID)
                return ret

            manualMove(cell, isBlack)
            return ret;
        }

        return super.doMoveWithUIFeedback(cell)
    }

    private fun manualMove(cell: Cell, isBlack: Boolean) {
        if(!engine.doMove(cell.x, cell.y, isBlack))
        {
            Timber.w("problem processing move to (%d, %d)", cell.x, cell.y)
        }
    }

    private fun genMove() {
        engineGoGame.aiIsThinking = true
        game.clearHint()
        busyIndicator.visibility = View.VISIBLE
        val waiter = Waiter(200)
        lifecycleScope.launch {
            val move = withContext(Dispatchers.IO) {
                engine.genMove(game.isBlackToMove)
            }
            waiter.mayWait()
            withContext(Dispatchers.Main) {
                if (move.pass) {
                    game.pass()
                    Toast.makeText(this@PlayAgainstEngineActivity, R.string.pass, Toast.LENGTH_SHORT).show()
                    bus.post(Message(getString(R.string.pass)))
                } else {
                    val boardCell = game.calcBoard.getCell(move.x, move.y)
                    game.do_move(boardCell)
                }
                engineGoGame.aiIsThinking = false
                busyIndicator.visibility = View.GONE
            }
        }
    }

    fun syncFromScratch() {
        Timber.w("sync start")
        val engineConfig: EngineConfig = engine

        engineConfig.sync(game)
        syncing = false
    }

    override fun requestUndo() {
        try {
            if (game.canUndo()) {
                syncing = true
                game.undo(false)
            }

            if (game.canUndo()) {
                syncing = true
                game.undo(false)
            }

            if(syncing) {
                syncFromScratch()
            }
        } catch (e: Exception) {
            Timber.w(e, "RemoteException when undoing")
        }
    }

    override fun doPass(): Boolean {
        val isBlack = game.isBlackToMove
        game.pass()
        engine.doPass(isBlack)

        bus.post(GameChangedEvent)
        return true
    }

    override fun initializeStoneMove() {
        // we do not want this behaviour so we override and do nothing
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_game_pass).isVisible = !game.isFinished
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menuInflater.inflate(R.menu.ingame_vs, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_back_to_title -> {
                goToTitle()
                return true
            }
            R.id.menu_game_hint -> {
                busyIndicator.visibility = View.VISIBLE
                syncAnalyzer()
                lifecycleScope.launch {
                    val move = withContext(Dispatchers.IO) {
                        analyzer.hint(game.isBlackToMove, game)
                    }
                    busyIndicator.visibility = View.GONE
                    if (move.pass)
                    {
                        bus.post(Message(getString(R.string.suggestion_pass)))
                    }
                    else
                    {
                        val color = if(engineGoGame.playingBlack) GoDefinitions.STONE_WHITE else GoDefinitions.STONE_BLACK
                        game.setHint(move.x, move.y, color)
                        bus.post(GameChangedEvent)
                    }
                }
                return true;
            }
            R.id.menu_goto_review -> {
                switchToReview()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onGameChanged(gameChangedEvent: GameChangedEvent) {
        super.onGameChanged(gameChangedEvent)

        if (game.isFinished) {
            switchToCounting()
        }

    }

    override val gameExtraFragment: Fragment
        get() = ConsoleGameExtrasFragment().apply {
            askNewInfo = { if (::engine.isInitialized) engine.debugInfo() ?: "" else ""}
            bus.register(this)
        }
}

data class Message(val msg: String)

class ConsoleGameExtrasFragment : Fragment() {
    var askNewInfo : Function0<String> = {""}

    val textView by lazy {
        TextView(this.activity).apply {
            text = "Deb: "
            setOnClickListener { text = askNewInfo() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            = textView

    @Subscribe
    fun showMessage(msg: Message) {
        textView.text = msg.msg
    }

}
