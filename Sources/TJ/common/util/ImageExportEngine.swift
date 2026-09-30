import Foundation
import UIKit
import CoreImage

public final class ImageExportEngine {

    public static func processAndCompressImage(
        inputURL: URL,
        outputURL: URL,
        cropRectNormalized: CGRect = CGRect(x: 0, y: 0, width: 1, height: 1),
        rotationDegrees: Int = 0,
        maxTargetDim: Int = 2048,
        quality: CGFloat = 0.85
    ) async -> Bool {
        do {
            let data = try Data(contentsOf: inputURL)
            guard let image = UIImage(data: data) else { return false }

            let fullWidth = image.size.width
            let fullHeight = image.size.height
            guard fullWidth > 0, fullHeight > 0 else { return false }

            // 1. Normalized crop box -> Pixel crop box
            let cropPixelX = max(cropRectNormalized.origin.x * fullWidth, 0)
            let cropPixelY = max(cropRectNormalized.origin.y * fullHeight, 0)
            let cropPixelWidth = min(cropRectNormalized.size.width * fullWidth, fullWidth - cropPixelX)
            let cropPixelHeight = min(cropRectNormalized.size.height * fullHeight, fullHeight - cropPixelY)

            let pixelCrop = CGRect(x: cropPixelX, y: cropPixelY, width: cropPixelWidth, height: cropPixelHeight)

            // 2. Perform Cropping
            guard let cgImage = image.cgImage?.cropping(to: pixelCrop) else { return false }
            var croppedImage = UIImage(cgImage: cgImage, scale: image.scale, orientation: image.imageOrientation)

            // 3. Rotation if required
            if rotationDegrees != 0 {
                let radians = CGFloat(rotationDegrees) * .pi / 180.0
                var newSize = CGRect(origin: .zero, size: croppedImage.size)
                    .applying(CGAffineTransform(rotationAngle: radians)).size
                newSize.width = floor(newSize.width)
                newSize.height = floor(newSize.height)

                UIGraphicsBeginImageContextWithOptions(newSize, false, 1.0)
                if let context = UIGraphicsGetCurrentContext() {
                    context.translateBy(x: newSize.width / 2.0, y: newSize.height / 2.0)
                    context.rotate(by: radians)
                    croppedImage.draw(in: CGRect(
                        x: -croppedImage.size.width / 2.0,
                        y: -croppedImage.size.height / 2.0,
                        width: croppedImage.size.width,
                        height: croppedImage.size.height
                    ))
                    if let rotated = UIGraphicsGetImageFromCurrentImageContext() {
                        croppedImage = rotated
                    }
                }
                UIGraphicsEndImageContext()
            }

            // 4. Downscale if max side > maxTargetDim
            let currentMaxDim = max(croppedImage.size.width, croppedImage.size.height)
            var finalImage = croppedImage
            if currentMaxDim > CGFloat(maxTargetDim) {
                let scale = CGFloat(maxTargetDim) / currentMaxDim
                let targetSize = CGSize(width: croppedImage.size.width * scale, height: croppedImage.size.height * scale)

                UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
                croppedImage.draw(in: CGRect(origin: .zero, size: targetSize))
                if let scaled = UIGraphicsGetImageFromCurrentImageContext() {
                    finalImage = scaled
                }
                UIGraphicsEndImageContext()
            }

            // 5. Write JPEG compressed data to outputURL
            guard let compressedData = finalImage.jpegData(compressionQuality: quality) else {
                return false
            }

            let parentDir = outputURL.deletingLastPathComponent()
            try FileManager.default.createDirectory(at: parentDir, withIntermediateDirectories: true)
            try compressedData.write(to: outputURL, options: .atomic)
            return true
        } catch {
            print("[ImageExportEngine] Image processing failed: \(error.localizedDescription)")
            return false
        }
    }
}
