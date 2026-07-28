package com.kuikly.kuiklychartkit.chart

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.DeclarativeBaseView.ImageType
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** Output representation requested from Kuikly's asynchronous `toImage` bridge. */
enum class ChartImageExportOutput {
    /** A self-contained data URI. This is the default because no native image cache is retained. */
    DATA_URI,
    /** A platform-managed file path. The host application owns file lifecycle and sharing. */
    FILE,
}

/** Immutable request for an asynchronous chart image export. */
data class ChartImageExportRequest(
    /** Requested output representation. */
    val output: ChartImageExportOutput = ChartImageExportOutput.DATA_URI,
    /** Native sampling factor. Values below one are invalid. */
    val sampleSize: Int = 1,
) {
    init {
        require(sampleSize >= 1) { "imageExport.sampleSize must be >= 1" }
    }
}

/** One-shot result returned by [exportImage]. The payload is never retained by ChartKit. */
data class ChartImageExportResult(
    /** Native result code. `0` indicates a successful bridge call. */
    val code: Int,
    /** Exported data URI or file path when [isSuccess] is true. */
    val data: String?,
    /** Native error message or a deterministic fallback when no payload is returned. */
    val message: String?,
) {
    /** A successful bridge response with a usable output payload. */
    val isSuccess: Boolean get() = code == 0 && !data.isNullOrBlank()
}

/**
 * Marks the image-export bridge as experimental until it has passed Android,
 * iOS and OpenHarmony version, memory and device validation.
 */
@RequiresOptIn(
    message = "Chart image export is experimental until cross-platform Kuikly validation is complete.",
    level = RequiresOptIn.Level.WARNING,
)
annotation class ExperimentalChartImageExportApi

/**
 * Exports this rendered chart through Kuikly's one-shot asynchronous `toImage` bridge.
 *
 * The default [ChartImageExportOutput.DATA_URI] avoids the native cache-key output mode,
 * and ChartKit keeps neither the callback nor its payload after [onResult] returns. A
 * file result is owned by the embedding application and must be cleaned up by that host.
 */
@ExperimentalChartImageExportApi
fun ComposeView<*, *>.exportImage(
    request: ChartImageExportRequest = ChartImageExportRequest(),
    onResult: (ChartImageExportResult) -> Unit,
) {
    var delivered = false
    toImage(request.nativeImageType(), request.sampleSize) { response ->
        if (!delivered) {
            delivered = true
            onResult(parseChartImageExportResponse(response))
        }
    }
}

internal fun parseChartImageExportResponse(response: JSONObject?): ChartImageExportResult {
    val code = response?.optInt("code", -1) ?: -1
    val data = response?.optString("data").orEmpty().ifBlank { null }
    val nativeMessage = response?.optString("message").orEmpty().ifBlank { null }
    val message = nativeMessage ?: if (code == 0 && data != null) null else "Image export returned no usable payload"
    return ChartImageExportResult(code = code, data = data, message = message)
}

private fun ChartImageExportRequest.nativeImageType(): ImageType = when (output) {
    ChartImageExportOutput.DATA_URI -> ImageType.DATA_URI
    ChartImageExportOutput.FILE -> ImageType.FILE
}
