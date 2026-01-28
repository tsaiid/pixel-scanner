package com.example.pixelscanner.image

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class ImageProcessor {

    suspend fun removeShadow(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert to LAB
        val lab = Mat()
        Imgproc.cvtColor(src, lab, Imgproc.COLOR_RGB2Lab)

        // Split channels
        val labPlanes = ArrayList<Mat>()
        Core.split(lab, labPlanes)
        val lChannel = labPlanes[0]

        // Estimate background illumination (Gaussian Blur)
        val kernelSize = 121.0
        val blurredL = Mat()
        Imgproc.GaussianBlur(lChannel, blurredL, Size(kernelSize, kernelSize), 0.0)

        // Normalize illumination: result = (original / background) * 255
        val lFloat = Mat()
        lChannel.convertTo(lFloat, CvType.CV_32F)
        
        val blurredFloat = Mat()
        blurredL.convertTo(blurredFloat, CvType.CV_32F)

        val resultLFloat = Mat()
        Core.divide(lFloat, blurredFloat, resultLFloat)
        Core.multiply(resultLFloat, Scalar(255.0), resultLFloat)

        val resultL = Mat()
        resultLFloat.convertTo(resultL, CvType.CV_8U)

        // Merge back
        labPlanes[0] = resultL
        val resultLab = Mat()
        Core.merge(labPlanes, resultLab)

        // Convert back to RGB
        val resultMat = Mat()
        Imgproc.cvtColor(resultLab, resultMat, Imgproc.COLOR_Lab2RGB)

        // Convert to Bitmap
        val resultBitmap = Bitmap.createBitmap(resultMat.cols(), resultMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resultMat, resultBitmap)

        // Release Mats
        src.release()
        lab.release()
        lChannel.release()
        blurredL.release()
        lFloat.release()
        blurredFloat.release()
        resultLFloat.release()
        resultL.release()
        resultLab.release()
        resultMat.release()
        labPlanes.forEach { it.release() }

        return@withContext resultBitmap
    }
}
