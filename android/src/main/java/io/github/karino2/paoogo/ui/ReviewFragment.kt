package io.github.karino2.paoogo.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.EventBus
import org.ligi.gobandroid_hd.App
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.events.GameChangedEvent
import org.ligi.gobandroid_hd.ui.GoPrefs
import org.ligi.gobandroid_hd.ui.alerts.GameForwardAlert
import org.ligi.gobandroid_hd.ui.fragments.GobandroidGameAwareFragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.github.karino2.paoogo.goengine.GoAnalyzer
import io.github.karino2.paoogo.ui.vs_engine.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.ligi.gobandroid_hd.databinding.ReviewExtraFragmentBinding
import org.ligi.gobandroid_hd.logic.GoGame
import timber.log.Timber

class UpdatingScoreDialogFragment(val cancelListener: ()->Unit) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context).setTitle("Analyzing")
            .setMessage("Analyzing...")
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        cancelListener()
        super.onCancel(dialog)
    }
}

class GraphUpdater(val plot: LineChart, val xy: LineDataSet, val game: GoGame, val analyzer: GoAnalyzer) {
    var cancel = false

    suspend fun updating() {
        analyzer.clearBoard()
        val replay_moves = game.replayMoves()
        var pos = 0
        for (tmp_move in replay_moves) {
            // どうもisFirstMoveがtrueの時は何も無いらしい。
            if (tmp_move.isFirstMove)
                continue

            if(cancel)
                return

            if (tmp_move.isPassMove) {
                analyzer.doPass(tmp_move.isBlack)
            } else {
                analyzer.doMove(tmp_move.cell!!.x, tmp_move.cell!!.y, tmp_move.isBlack)
            }
            val score = analyzer.score(300, !tmp_move.isBlack)
            println("score=${score}, pos=${pos}")
            xy.getEntryForIndex(pos)?.let {entry->
                entry.y = score.toFloat()
            }
            pos++
            withContext(Dispatchers.Main) {
                plot.notifyDataSetChanged()
                plot.invalidate()
            }
        }
    }
}

class ReviewFragment : GobandroidGameAwareFragment() {
    private var _binding: ReviewExtraFragmentBinding? = null
    private val binding get() = _binding!!

    override fun createView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        _binding = ReviewExtraFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        updateButtonStates()

        binding.btnNext.setOnClickListener {
            game.clearAnalyzerInfo()
            if (game.isInReviewVariation && game.possibleVariationCount > 1) {
                game.redo(1)
            } else if (GoPrefs.isShowForwardAlertWanted) {
                    GameForwardAlert.showIfNeeded(requireActivity(), game)
            } else {
                game.redo(0)
            }
        }

        binding.btnPrev.setOnClickListener {
            game.clearAnalyzerInfo()
            if (game.canUndo()) {
                game.undo()
            }
        }

        binding.btnFirst.setOnClickListener {
            game.clearAnalyzerInfo()
            val nextJunction = game.findPrevJunction()
            if (nextJunction!!.isFirstMove) {
                game.jump(nextJunction)
            } else {
                showJunctionInfoSnack(R.string.found_junction_snack_for_first)
                game.jump(nextJunction.nextMoveVariations[0])
            }
        }

        binding.btnFirst.setOnLongClickListener {
            game.clearAnalyzerInfo()
            game.jump(game.findFirstMove())
            true
        }

        binding.btnLast.setOnClickListener {
            game.clearAnalyzerInfo()
            val nextJunction = game.findNextJunction()
            if (nextJunction!!.hasNextMove()) {
                showJunctionInfoSnack(R.string.found_junction_snack_for_last)
                game.jump(nextJunction.nextMoveVariations[0])
            } else {
                game.jump(nextJunction)
            }
        }

        binding.btnLast.setOnLongClickListener {
            game.clearAnalyzerInfo()
            game.jump(game.findLastMove())
            true
        }

        binding.btnMainline.setOnClickListener {
            game.clearAnalyzerInfo()
            game.revertToMainLine()
            postGameChangeEvent()
        }

        binding.btnAnalyze.setOnClickListener {
            val busyIndicator = requireActivity().findViewById<ProgressBar>(R.id.busy_indicator)
            busyIndicator.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                analyzer.sync(game)
                val info = withContext(Dispatchers.IO) {
                    analyzer.analyzeSituation(game.isBlackToMove, game)
                }
                busyIndicator.visibility = View.GONE
                game.setAnalyzeInfo(info)
                postGameChangeEvent()
            }
        }
        val yaxis = binding.plot.axisLeft
        yaxis.axisMinimum = -1.0F
        yaxis.axisMaximum = 1.0F
        yaxis.isGranularityEnabled = true

        binding.plot.setOnChartValueSelectedListener(object : OnChartValueSelectedListener  {
            override fun onValueSelected(
                e: Entry?,
                h: Highlight?
            ) {
                println("e: ${e.toString()}")
            }

            override fun onNothingSelected() {
            }

        })

        binding.btnGraph.setOnClickListener {
            analyzeGraph()
        }

    }

    private fun analyzeGraph() {
        val scores = (0..<game.totalMove).toList().map { Entry(it.toFloat(), 0.0F) }
        val dataSet = LineDataSet(scores, "Scores")
        val lineData = LineData(dataSet)
        binding.plot.data = lineData
        binding.plot.invalidate()

        val updater = GraphUpdater(binding.plot, dataSet, game, analyzer)

        val dialog = UpdatingScoreDialogFragment({
            println("cancel")
            updater.cancel = true
        })
        dialog.show(parentFragmentManager, "AnalyzingDialog")

        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                updater.updating()
            }
            withContext(Dispatchers.Main) {
                if (!updater.cancel)
                    dialog.dismiss()
            }
        }
    }

    private fun postGameChangeEvent() {
        EventBus.getDefault().post(GameChangedEvent)
    }

    private val app : App
        get() = requireActivity().applicationContext as App

    private val analyzer by lazy {
        app.engineRepository.getAnalyzer().apply {
            setKomi(game.komi)
            setBoardSize(game.boardSize)
            clearBoard()
        }
    }

    override fun onGoGameChanged(gameChangedEvent: GameChangedEvent?) {
        super.onGoGameChanged(gameChangedEvent)
        updateButtonStates()
    }

    private fun updateButtonStates() {
        setImageViewState(game.canUndo(), binding.btnFirst, binding.btnPrev)
        setImageViewState(game.canRedo(), binding.btnNext, binding.btnLast)
        binding.btnMainline.isEnabled = game.isInReviewVariation
    }

    private fun setImageViewState(state: Boolean, vararg views: ImageView) {
        views.forEach {
            it.isEnabled = state
            it.alpha = if (state) 1f else 0.4f
        }
    }

    private fun showJunctionInfoSnack(found_junction_snack_for_last: Int) {
        if (!GoPrefs.hasAcknowledgedJunctionInfo) {
            Snackbar.make(binding.btnLast, found_junction_snack_for_last, Snackbar.LENGTH_LONG).setAction(android.R.string.ok) { GoPrefs.hasAcknowledgedJunctionInfo = true }.show()
        }
    }

    @Subscribe
    fun showMessage(msg: Message) {
        binding.txtMsg.text = msg.msg
    }


}
