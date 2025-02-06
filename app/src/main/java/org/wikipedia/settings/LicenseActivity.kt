package org.wikipedia.settings

import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityLicenseBinding
import org.wikipedia.util.FileUtil.readFile
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.StringUtil
import java.io.IOException

class LicenseActivity : BaseActivity() {
    private lateinit var binding: ActivityLicenseBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationBarColor(getThemedColor(this, android.R.attr.windowBackground))

        val asset = intent.getStringExtra("asset") ?: ""
        if (asset.isNotEmpty()) {
            // Example string: "/android_asset/licenses/Otto"
            val strings = asset.split("/")
            title = getString(R.string.license_title, strings[strings.size - 1])
            try {
                val text = readFile(assets.open(asset))
                binding.licenseText.text = StringUtil.fromHtml(text.replace("\n\n", "<br/><br/>"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
