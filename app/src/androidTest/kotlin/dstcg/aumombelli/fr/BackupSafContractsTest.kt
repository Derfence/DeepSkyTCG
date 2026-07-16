package fr.aumombelli.dstcg

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupSafContractsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun create_document_contract_requests_portable_backup_destination() {
        val intent = ActivityResultContracts.CreateDocument("application/octet-stream")
            .createIntent(context, "deep-sky.dstcgsave")

        assertEquals(Intent.ACTION_CREATE_DOCUMENT, intent.action)
        assertEquals("application/octet-stream", intent.type)
        assertEquals("deep-sky.dstcgsave", intent.getStringExtra(Intent.EXTRA_TITLE))
    }

    @Test
    fun open_document_contract_accepts_supported_backup_mime_types() {
        val mimeTypes = arrayOf("application/octet-stream", "application/json", "text/plain")
        val intent = ActivityResultContracts.OpenDocument().createIntent(context, mimeTypes)

        assertEquals(Intent.ACTION_OPEN_DOCUMENT, intent.action)
        assertEquals("*/*", intent.type)
        assertEquals(mimeTypes.toSet(), intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.toSet())
    }
}
