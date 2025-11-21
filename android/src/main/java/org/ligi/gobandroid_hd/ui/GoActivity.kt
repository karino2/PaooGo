/**
 * gobandroid
 * by Marcus -Ligi- Bueschleb
 * http://ligi.de
 *
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation;
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package org.ligi.gobandroid_hd.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.print.PrintManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnKeyListener
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.github.karino2.paoogo.ui.GameStartActivity
import io.github.karino2.paoogo.ui.ReviewActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.ligi.gobandroid_hd.BuildConfig
import org.ligi.gobandroid_hd.InteractionScope
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.databinding.GameBinding
import org.ligi.gobandroid_hd.events.GameChangedEvent
import org.ligi.gobandroid_hd.events.OptionsItemClickedEvent
import org.ligi.gobandroid_hd.logic.Cell
import org.ligi.gobandroid_hd.logic.GoGame
import org.ligi.gobandroid_hd.logic.GoGame.MoveStatus.INVALID_CELL_NO_LIBERTIES
import org.ligi.gobandroid_hd.logic.GoGame.MoveStatus.INVALID_IS_KO
import org.ligi.gobandroid_hd.logic.GoGame.MoveStatus.INVALID_NOT_ON_BOARD
import org.ligi.gobandroid_hd.logic.sgf.SGFWriter
import org.ligi.gobandroid_hd.ui.GoSoundManager.Sound.PICKUP1
import org.ligi.gobandroid_hd.ui.GoSoundManager.Sound.PICKUP2
import org.ligi.gobandroid_hd.ui.GoSoundManager.Sound.PLACE1
import org.ligi.gobandroid_hd.ui.GoSoundManager.Sound.PLACE2
import org.ligi.gobandroid_hd.ui.application.GobandroidFragmentActivity
import org.ligi.gobandroid_hd.ui.fragments.DefaultGameExtrasFragment
import org.ligi.gobandroid_hd.ui.recording.SaveSGFDialog
import org.ligi.gobandroid_hd.ui.scoring.GameScoringActivity
import org.ligi.gobandroid_hd.ui.share.ShareSGFDialog
import org.ligi.kaxt.disableRotation
import org.ligi.snackengage.SnackEngage
import org.ligi.snackengage.conditions.AfterNumberOfOpportunities
import org.ligi.snackengage.conditions.NeverAgainWhenClickedOnce
import org.ligi.snackengage.conditions.locale.IsOneOfTheseLocales
import org.ligi.snackengage.snacks.RateSnack
import org.ligi.snackengage.snacks.TranslateSnack
import org.ligi.tracedroid.sending.sendTraceDroidStackTracesIfExist
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.RuntimePermissions
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale


/**
 * Activity for a Go Game
 */
open class GoActivity : GobandroidFragmentActivity(), OnTouchListener, OnKeyListener {
    var sound_man: GoSoundManager? = null

    private var info_toast: Toast? = null
    private var last_processed_move_change_num = 0

    open val isBoardFocusWanted = true
    open val gameExtraFragment: Fragment = DefaultGameExtrasFragment()
    protected val bus = EventBus.getDefault()
    lateinit var binding: GameBinding


    /**
     * @return true if we want to ask the user - different in modes
     */
    open fun isAsk4QuitEnabled() = true

    fun updatePlayerName()
    {
        binding.whitePlayerNameTv!!.text = game.metaData.whiteName
        binding.blackPlayerNameTv!!.text = game.metaData.blackName
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.game)
        binding = GameBinding.bind(pbinding.contentFrame.getChildAt(0))

        supportActionBar!!.setHomeButtonEnabled(true)

        disableRotation()

        if (GoPrefs.isConstantLightWanted) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (sound_man == null) {
            sound_man = GoSoundManager(this, env).apply{
                ensureSound(PICKUP1)
                ensureSound(PICKUP2)
                ensureSound(PLACE1)
                ensureSound(PLACE2)
            }
        }

        val customNav = CustomActionBar(this)

        val fragmentTransAction = supportFragmentManager.beginTransaction()
        fragmentTransAction.add(R.id.game_extra_container, gameExtraFragment).commit()
        supportFragmentManager.executePendingTransactions()


        supportActionBar!!.customView = customNav
        supportActionBar!!.setDisplayShowCustomEnabled(true)
        customNav.isFocusable = false

        createInfoToast()

        setupBoard()

        game2ui()
        updatePlayerName()
    }

    @SuppressLint("ShowToast")
    private // this is correct - we do not want to show the toast at this stage
    fun createInfoToast() {
        info_toast = Toast.makeText(this.baseContext, "", Toast.LENGTH_LONG)
    }

    /**
     * find the go board widget and set up some properties
     */
    private fun setupBoard() {
        binding.goBoard.setOnTouchListener(this)
        binding.goBoard.setOnKeyListener(this)
        binding.goBoard.move_stone_mode = false
    }


    /**
     * set some preferences on the go board - intended to be called in onResume
     */
    private fun setBoardPreferences() {
        binding.goBoard.do_legend = GoPrefs.isLegendEnabled
        binding.goBoard.legend_sgf_mode = GoPrefs.isSGFLegendEnabled
        binding.goBoard.setLineSize(GoPrefs.boardLineWidth.toFloat())
    }

    override fun onResume() {
        super.onResume()

        if (isBoardFocusWanted) {
            binding.goBoard.isFocusableInTouchMode = true
            binding.goBoard.isFocusable = true
            binding.goBoard.requestFocus()
        } else {
            binding.goBoard.isFocusableInTouchMode = false
            binding.goBoard.isFocusable = false
        }
        setBoardPreferences()


        bus.register(this)
    }

    override fun doFullScreen() =
        GoPrefs.isFullscreenEnabled or resources.getBoolean(R.bool.force_fullscreen)

    override fun onCreateOptionsMenu(menu: Menu) = super.onCreateOptionsMenu(menu.apply {
        menuInflater.inflate(R.menu.ingame_common, this)
    })


    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // NYI
        menu.findItem(R.id.menu_game_info)?.let{ it.isVisible = false }

        return super.onPrepareOptionsMenu(menu)
    }

    open fun doPass() : Boolean {
        game.pass()

        bus.post(GameChangedEvent)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        bus.post(OptionsItemClickedEvent(item.itemId))
        when (item.itemId) {

            R.id.menu_game_undo -> {
                if (game.canUndo()) {
                    requestUndo()
                    return true
                }
            }

            R.id.menu_game_pass -> {
                return doPass()
            }

            /*
            NYI
            R.id.menu_write_sgf -> {
                prepareSaveWithPermissionCheck()
                return true
            }

            R.id.menu_game_share -> {
                //new ShareWithMultipleOptionsDialog(this).show();
                ShareSGFDialog(this).show()
                return true
            }
             */
        }

        return super.onOptionsItemSelected(item)
    }

    /*
    NYI
    fun prepareSave() {
        SaveSGFDialog(this).show()
    }
     */

    fun switchToCounting() {
        interactionScope.mode = InteractionScope.Mode.COUNT
        startActivity(Intent(this, GameScoringActivity::class.java))
        finish()
    }

    fun switchToReview() {
        interactionScope.mode = InteractionScope.Mode.REVIEW
        startActivity(Intent(this, ReviewActivity::class.java))
        finish()
    }

    fun goToTitle() {
        gameProvider.set(GoGame(game.size))
        Intent(this, GameStartActivity::class.java).apply{ flags = Intent.FLAG_ACTIVITY_CLEAR_TOP  }.let { startActivity(it) }
    }

    open fun doBack() {
        if (!isAsk4QuitEnabled()) {
            goToTitle();
        } else {
            AlertDialog.Builder(this).setTitle(R.string.end_game_quesstion_title)
                .setMessage(R.string.quit_confirm)
                .setPositiveButton(R.string.yes) { _, _ -> goToTitle() }
                .setCancelable(true)
                .setNegativeButton(R.string.no, null)
                .show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                doBack()
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * show a the info toast with a specified text from a resource ID
     */
    protected fun showInfoToast(@StringRes resId: Int) {
        info_toast!!.setText(resId)
        info_toast!!.show()
    }

    protected open fun doMoveWithUIFeedback(cell: Cell?): GoGame.MoveStatus {
        if (cell == null) {
            return INVALID_NOT_ON_BOARD
        }

        val res = game.do_move(cell)

        if (res == INVALID_IS_KO || res == INVALID_CELL_NO_LIBERTIES) {
            showInfoToast(getToastForResult(res))
        }

        return res
    }

    @StringRes
    private fun getToastForResult(res: GoGame.MoveStatus) = when (res) {
        INVALID_IS_KO -> R.string.invalid_move_ko
        INVALID_CELL_NO_LIBERTIES -> R.string.invalid_move_no_liberties
        else -> throw RuntimeException("Illegal game result " + res)
    }


    fun game2ui() {
        binding.goBoard.postInvalidate()
        refreshZoomFragment()
    }

    protected fun eventForZoomBoard(event: MotionEvent) {
        interactionScope.touchCell = binding.goBoard.pixel2cell(event.x, event.y)

        if (!app.isTesting) {
            if (event.action == MotionEvent.ACTION_UP) {
                binding.gameExtraContainer.visibility = View.VISIBLE
                binding.zoomBoard.visibility = View.GONE
            } else if (event.action == MotionEvent.ACTION_DOWN) {
                binding.gameExtraContainer.visibility = View.GONE
                binding.zoomBoard.visibility = View.VISIBLE
            }
        }
        refreshZoomFragment()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {

        eventForZoomBoard(event)

        if (event.action == MotionEvent.ACTION_UP) {
            if (resources.getBoolean(R.bool.small)) {
                supportActionBar!!.show()
            }

        } else if (event.action == MotionEvent.ACTION_DOWN) {

            // for very small devices we want to hide the ActionBar to actually
            // see something in the Zoom-Fragment
            if (resources.getBoolean(R.bool.small)) {
                supportActionBar!!.hide()
            }

            if (game.isBlackToMove) {
                sound_man!!.playSound(PICKUP1)
            } else {
                sound_man!!.playSound(PICKUP2)
            }
        }

        doTouch(event)

        return true
    }

    public override fun onPause() {

        binding.goBoard.move_stone_mode = false

        bus.unregister(this)
        super.onPause()
    }

    open fun doTouch(event: MotionEvent) {

        // calculate position on the field by position on the touchscreen

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> interactionScope.touchCell =
                binding.goBoard.pixel2cell(event.x, event.y)

            MotionEvent.ACTION_OUTSIDE -> interactionScope.touchCell = null

            MotionEvent.ACTION_UP -> {

                if (binding.goBoard.move_stone_mode) {
                    // TODO check if this is an illegal move ( e.g. in variants )

                    if (interactionScope.touchCell != null && game.visualBoard.isCellFree(
                            interactionScope.touchCell!!
                        )
                    ) {
                        game.repositionActMove(interactionScope.touchCell!!)
                    }
                    binding.goBoard.move_stone_mode = false // moving of stone done
                } else if (game.actMove.isOnCell(interactionScope.touchCell)) {
                    initializeStoneMove()
                } else {
                    doMoveWithUIFeedback(interactionScope.touchCell)
                }

                interactionScope.touchCell = null
            }
        }

        bus.post(GameChangedEvent)
    }


    open fun initializeStoneMove() {

        if (binding.goBoard.move_stone_mode) { // already in the mode
            return  // -> do nothing
        }

        binding.goBoard.move_stone_mode = true

        // TODO check if we only want this in certain modes
        if (GoPrefs.isAnnounceMoveActive) {

            AlertDialog.Builder(this).setMessage(R.string.hint_stone_move).setPositiveButton(
                R.string.ok

            ) { _, _ -> GoPrefs.isAnnounceMoveActive = false }.show()
        }
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val ensuredTouchPosition =
                interactionScope.touchCell ?: game.statelessGoBoard.getCell(0, 0)
            val boardCell = game.statelessGoBoard.getCell(ensuredTouchPosition)
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (boardCell.up != null) {
                    interactionScope.touchCell = boardCell.up
                } else {
                    return false
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> if (boardCell.left != null) {
                    interactionScope.touchCell = boardCell.left
                } else {
                    return false
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> if (boardCell.down != null) {
                    interactionScope.touchCell = boardCell.down
                } else {
                    return false
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> if (boardCell.right != null) {
                    interactionScope.touchCell = boardCell.right
                } else {
                    return false
                }

                KeyEvent.KEYCODE_DPAD_CENTER -> doMoveWithUIFeedback(boardCell)

                else -> return false
            }

            binding.goBoard.postInvalidate()
            refreshZoomFragment()
            return true
        }
        return false
    }

    fun refreshZoomFragment() {
        binding.zoomBoard.postInvalidate()
    }

    open fun requestUndo() {

        binding.goBoard.move_stone_mode = false

        UndoWithVariationDialog.userInvokedUndo(this, interactionScope, game)
    }

    @Subscribe
    open fun onGameChanged(gameChangedEvent: GameChangedEvent) {
        Timber.i("onGoGameChange in GoActivity")
        if (game.actMove.movePos > last_processed_move_change_num) {
            if (game.isBlackToMove) {
                sound_man!!.playSound(PLACE1)
            } else {
                sound_man!!.playSound(PLACE2)
            }
        }
        last_processed_move_change_num = game.actMove.movePos

        game2ui()
    }

    protected fun notifyGoGameChange() {
        EventBus.getDefault().post(GameChangedEvent)
    }
}