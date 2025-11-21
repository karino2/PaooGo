package org.ligi.gobandroid_hd.ui.recording

import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.Toast
import org.ligi.gobandroid_hd.R
import org.ligi.gobandroid_hd.databinding.DialogSaveSgfBinding
import org.ligi.gobandroid_hd.logic.sgf.FastFile
import org.ligi.gobandroid_hd.logic.sgf.SGFWriter
import org.ligi.gobandroid_hd.ui.GobandroidDialog
import org.ligi.gobandroid_hd.ui.application.GobandroidFragmentActivity
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog to save a game to SGF file and ask the user about how in here
 *
 *
 * TODO check if file exists

 * @author ligi
 */
class SaveSGFDialog(private val context: GobandroidFragmentActivity, private val storeDirUri: Uri, private val notifyUrlInvalid: ()->Unit) : GobandroidDialog(context) {
    private val binding: DialogSaveSgfBinding

    private val storeDir = FastFile.fromTreeUri(context, storeDirUri)

    init {
        setContentView(R.layout.dialog_save_sgf)
        binding = DialogSaveSgfBinding.bind(pbinding.dialogContent.getChildAt(0))

        setIconResource(R.drawable.ic_content_save)

        setPositiveButton(R.string.save_label, { _ ->
            try
            {
                createTargetFile()?.let { file->
                    val sgfTxt = SGFWriter.game2sgf(gameProvider.get(), context.getString(R.string.app_name))
                    file.writeText(sgfTxt)
                    Toast.makeText(context, String.format(context.getString(R.string.file_saved), file.name), Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }catch(_: Exception) {
                Toast.makeText(context, "Can't create file. Please give permission again.", Toast.LENGTH_SHORT).show()
                notifyUrlInvalid()
                dismiss()
            }
        })

        val (name, _, _, _, blackName, _, whiteName) = gameProvider.get().metaData

        binding.sgfNameEdittext.setText("${datePrefix()}_${blackName}_vs_${whiteName}")

        binding.buttonRemoveBlackName.setOnClickListener {
            binding.sgfNameEdittext.setText("${datePrefix()}_${whiteName}")
        }

        setTitle(R.string.save_sgf)
    }

    private fun datePrefix(): String {
        val datePrefix = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return datePrefix
    }

    fun Button.prepareButton(condition: Boolean, adder: View.OnClickListener) = this.apply {
        setVisibility(condition)
        setOnClickListener(adder)
    }

    private fun completeFileName(): String? {
        var fileName = binding.sgfNameEdittext.text.toString()

        if (fileName.isEmpty())
            return null

        fileName += ".sgf"
        return fileName
    }


    private fun createTargetFile(): FastFile? {
        return completeFileName()?.let {
            storeDir.createFile("application/x-go-sgf", it)
        }
    }
}
