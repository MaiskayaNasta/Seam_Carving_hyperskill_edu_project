package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

class Seams(val inputFileName: String, val outputFileName: String, val widthToRemove: Int, val heightToRemove: Int) {
    val inputFile = File(inputFileName)
    val originalImage: BufferedImage = ImageIO.read(inputFile)
    val outputFile = File(outputFileName)


    fun energyCalc(image: BufferedImage, i: Int, j: Int): Double {
        val x = i.coerceIn(1, image.width - 2)
        val y = j.coerceIn(1, image.height - 2)
        val left = Color(image.getRGB(x-1, j))
        val right = Color(image.getRGB(x+1, j))
        val up = Color(image.getRGB(i, y-1))
        val down = Color(image.getRGB(i, y+1))
        val energy = sqrt(((left.red - right.red.toDouble()).pow(2) + (left.blue - right.blue.toDouble()).pow(2)
                + (left.green - right.green.toDouble()).pow(2)) + ((up.red - down.red.toDouble()).pow(2) + (up.green - down.green.toDouble()).pow(2) + (up.blue - down.blue.toDouble()).pow(2))
        )
        return energy
    }

    fun transpose(image: BufferedImage): BufferedImage {
        val transposedImage: BufferedImage = BufferedImage(image.height, image.width, image.type)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val color = Color(image.getRGB(x, y))
                transposedImage.setRGB(y, x, color.rgb)
            }
        }
        return transposedImage
    }
    fun removeSeam(image: BufferedImage): BufferedImage {
        val energyMatrix = Array(image.width) { DoubleArray(image.height) }
        val dp = Array(image.width) { DoubleArray(image.height) }
        val path = Array(image.width) { IntArray(image.height) }

        for (i in 0 until image.width) {
            for (j in 0 until image.height) {
                energyMatrix[i][j] = energyCalc(image, i, j)
            }
        }
        for (i in 0 until image.width) {
            dp[i][0] = energyMatrix[i][0]
        }
        for (j in 1 until image.height) {
            for (i in 0 until image.width) {
                dp[i][j] = energyMatrix[i][j] + dp[i][j - 1]
                path[i][j] = i

                if (i > 0 && dp[i - 1][j - 1] + energyMatrix[i][j] < dp[i][j]) {
                    dp[i][j] = dp[i - 1][j - 1] + energyMatrix[i][j]
                    path[i][j] = i - 1
                }
                if (i < image.width - 1 && dp[i + 1][j - 1] + energyMatrix[i][j] < dp[i][j]) {
                    dp[i][j] = dp[i + 1][j - 1] + energyMatrix[i][j]
                    path[i][j] = i + 1
                }
            }
        }
        var minEnergy = dp[0][image.height - 1]
        var minEnergyIndex = 0
        for (i in 1 until image.width) {
            if (dp[i][image.height - 1] < minEnergy) {
                minEnergy = dp[i][image.height - 1]
                minEnergyIndex = i
            }
        }
        val minZeroRow = dp[image.width - 1].toList().minOrNull()!!
        for(i in 0..dp[image.width - 1].toList().size-1) {
            if (dp[image.width - 1][i] == minZeroRow) path[image.width - 1][i] = i
        }

        val newImage = BufferedImage(image.width - 1, image.height, image.type)
        for (j in image.height - 1 downTo 0) {
            var columnIndex = 0
            for (i in 0 until image.width) {
                if (i != minEnergyIndex) {
                    newImage.setRGB(columnIndex, j, image.getRGB(i, j))
                    columnIndex++
                }
            }
            minEnergyIndex = path[minEnergyIndex][j]
        }
        return newImage
    }
    fun imageResize() {
        var vertImage: BufferedImage = originalImage
        repeat(widthToRemove) {
            vertImage = removeSeam(vertImage)
        }
        val trImage = transpose(vertImage)

        var horisImage: BufferedImage = trImage
        repeat(heightToRemove) {
            horisImage = removeSeam(horisImage)
        }
        val result = transpose(horisImage)
        ImageIO.write(result, "png", outputFile)
    }
}
fun main(args: Array<String>) {
    val seam = Seams(args[1], args[3], args[5].toInt(), args[7].toInt())
    seam.imageResize()
}