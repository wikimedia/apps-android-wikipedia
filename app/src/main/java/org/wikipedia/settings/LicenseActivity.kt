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

        val libraryNameStart = 24
        val path = (intent.data ?: return).path ?: return
        if (path.length > libraryNameStart) {
            // Example string: "/android_asset/licenses/Otto"
            title = getString(R.string.license_title, path.substring(libraryNameStart))
            try {
                val assetPathStart = 15
                val text = readFile(assets.open(path.substring(assetPathStart)))
                binding.licenseText.text = StringUtil.fromHtml(text.replace("\n\n", "<br/><br/>"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
