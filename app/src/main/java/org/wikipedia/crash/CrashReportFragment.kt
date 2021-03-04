package org.wikipedia.crash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.R

class CrashReportFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_crash_report, container, false)

        view.findViewById<View>(R.id.crash_report_start_over).setOnClickListener {
            startActivity(requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            requireActivity().finish()
        }

        view.findViewById<View>(R.id.crash_report_quit).setOnClickListener {
            requireActivity().finish()
        }

        return view
    }

    companion object {
        fun newInstance(): CrashReportFragment {
            return CrashReportFragment()
        }
    }
}
