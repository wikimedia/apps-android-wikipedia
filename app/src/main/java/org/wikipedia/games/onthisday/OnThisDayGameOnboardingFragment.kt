package org.wikipedia.games.onthisday

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentOnThisDayGameOnboardingBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.FeedbackUtil

class OnThisDayGameOnboardingFragment : Fragment() {
    private var _binding: FragmentOnThisDayGameOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnThisDayGameViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentOnThisDayGameOnboardingBinding.inflate(inflater, container, false)

        // TODO: add analytics for InvokeSource

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.playGameButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            (requireActivity() as? OnThisDayGameActivity)?.animateQuestions()
        }

        binding.dateText.text = DateUtil.getShortDateString(viewModel.currentDate)
    }

    companion object {
        fun newInstance(invokeSource: Constants.InvokeSource): OnThisDayGameOnboardingFragment {
            return OnThisDayGameOnboardingFragment().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource)
            }
        }

        fun maybeShowOnThisDayGameDialog(activity: Activity, wikiSite: WikiSite = WikipediaApp.instance.wikiSite) {
            if (!Prefs.otdEntryDialogShown && OnThisDayGameViewModel.LANG_CODES_SUPPORTED.contains(wikiSite.languageCode)) {
                Prefs.otdEntryDialogShown = true
                val dialogView = activity.layoutInflater.inflate(R.layout.dialog_on_this_day_game, null)
                val dialog = MaterialAlertDialogBuilder(activity)
                    .setView(dialogView)
                    .setCancelable(false)
                    .show()
                dialogView.findViewById<Button>(R.id.playGameButton).setOnClickListener {
                    activity.startActivity(OnThisDayGameActivity.newIntent(activity, InvokeSource.PAGE_ACTIVITY, wikiSite))
                    dialog.dismiss()
                }
                dialogView.findViewById<ImageView>(R.id.closeButton).setOnClickListener {
                    FeedbackUtil.showMessage(activity, R.string.on_this_day_game_entry_dialog_snackbar_message)
                    dialog.dismiss()
                }
            }
        }

        fun maybeShowOnThisDayGameSurvey(activity: Activity, wikiSite: WikiSite = WikipediaApp.instance.wikiSite) {
            if (/*!Prefs.otdEntryDialogShown && */OnThisDayGameViewModel.LANG_CODES_SUPPORTED.contains(wikiSite.languageCode)) {


                val dialog = MaterialAlertDialogBuilder(activity)
                    .setCancelable(false)
                    .setTitle("Help improve Wikipedia games")
                    .setMessage("How satisfied were you with the On This Day game?")
                    .setSingleChoiceItems(arrayOf("Satisfied", "Neutral", "Unsatisfied"), -1) { _, which ->
                        when (which) {
                            0 -> FeedbackUtil.showMessage(activity, "Thank you for your feedback!")
                            1 -> FeedbackUtil.showMessage(activity, "Thank you for your feedback!")
                            2 -> FeedbackUtil.showMessage(activity, "Thank you for your feedback!")
                        }
                    }
                    .setPositiveButton("Submit") { _, _ ->
                        FeedbackUtil.showMessage(activity, "Thank you for your feedback!")
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        FeedbackUtil.showMessage(activity, "Thank you for your feedback!")
                    }
                    .show()


            }
        }
    }
}
