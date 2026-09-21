/**
 * Smart Document Processing & CamScanner Enhancement Engine
 * Provides auto edge detection, cropping, perspective straightening, and text sharpening filters.
 */

/**
 * Auto-detect document boundaries in an image/canvas.
 * Uses edge gradients and luminance contrast to find the document bounding rectangle.
 */
export function autoDetectDocumentBounds(canvas) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const w = canvas.width;
  const h = canvas.height;

  // Scale down for fast edge detection
  const sampleW = 200;
  const sampleH = Math.round((h / w) * 200);
  
  const tempCanvas = document.createElement('canvas');
  tempCanvas.width = sampleW;
  tempCanvas.height = sampleH;
  const tempCtx = tempCanvas.getContext('2d', { willReadFrequently: true });
  tempCtx.drawImage(canvas, 0, 0, sampleW, sampleH);

  const imgData = tempCtx.getImageData(0, 0, sampleW, sampleH);
  const data = imgData.data;

  // Compute average luminance and find high-contrast bounding box
  let minX = sampleW, maxX = 0, minY = sampleH, maxY = 0;
  let totalLum = 0;
  const lums = new Float32Array(sampleW * sampleH);

  for (let i = 0; i < data.length; i += 4) {
    const lum = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
    lums[i / 4] = lum;
    totalLum += lum;
  }

  const avgLum = totalLum / (sampleW * sampleH);
  // In typical document photos, document paper is significantly brighter than the table surface,
  // or documents have strong edge contrast.
  const threshold = avgLum * 0.95;

  for (let y = 0; y < sampleH; y++) {
    for (let x = 0; x < sampleW; x++) {
      const idx = y * sampleW + x;
      const lum = lums[idx];

      // Edge detection gradient
      const rightLum = x < sampleW - 1 ? lums[idx + 1] : lum;
      const bottomLum = y < sampleH - 1 ? lums[idx + sampleW] : lum;
      const grad = Math.abs(lum - rightLum) + Math.abs(lum - bottomLum);

      if (lum > threshold || grad > 25) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  }

  // Fallback / safety margin: if detection is too small or too edge-hugging, give balanced 6% document margin
  const marginW = sampleW * 0.05;
  const marginH = sampleH * 0.05;
  if (maxX - minX < sampleW * 0.4 || maxY - minY < sampleH * 0.4) {
    minX = marginW;
    maxX = sampleW - marginW;
    minY = marginH;
    maxY = sampleH - marginH;
  } else {
    // Add small padding around detected edges
    minX = Math.max(0, minX - marginW * 0.4);
    maxX = Math.min(sampleW, maxX + marginW * 0.4);
    minY = Math.max(0, minY - marginH * 0.4);
    maxY = Math.min(sampleH, maxY + marginH * 0.4);
  }

  // Scale back to original coordinates
  const scaleX = w / sampleW;
  const scaleY = h / sampleH;

  const left = Math.round(minX * scaleX);
  const top = Math.round(minY * scaleY);
  const right = Math.round(maxX * scaleX);
  const bottom = Math.round(maxY * scaleY);

  return {
    x: left,
    y: top,
    width: Math.max(50, right - left),
    height: Math.max(50, bottom - top),
    corners: {
      tl: { x: left, y: top },
      tr: { x: right, y: top },
      br: { x: right, y: bottom },
      bl: { x: left, y: bottom }
    }
  };
}

/**
 * Crops a canvas region and rotates if requested.
 */
export function cropCanvas(sourceCanvas, cropBox, rotation = 0) {
  const { x, y, width, height } = cropBox;
  const targetCanvas = document.createElement('canvas');

  if (rotation === 90 || rotation === 270) {
    targetCanvas.width = height;
    targetCanvas.height = width;
  } else {
    targetCanvas.width = width;
    targetCanvas.height = height;
  }

  const ctx = targetCanvas.getContext('2d', { willReadFrequently: true });
  ctx.save();

  if (rotation === 90) {
    ctx.translate(height, 0);
    ctx.rotate((90 * Math.PI) / 180);
  } else if (rotation === 180) {
    ctx.translate(width, height);
    ctx.rotate((180 * Math.PI) / 180);
  } else if (rotation === 270) {
    ctx.translate(0, width);
    ctx.rotate((270 * Math.PI) / 180);
  }

  ctx.drawImage(sourceCanvas, x, y, width, height, 0, 0, width, height);
  ctx.restore();

  return targetCanvas;
}

/**
 * CamScanner-style Magic Filters
 * - 'magic': CamScanner Magic Color (cleans background to white, darkens and sharpens text, boosts contrast)
 * - 'bw': B&W High-Contrast Doc (binarized crisp document)
 * - 'grayscale': Clean Smooth Grayscale
 * - 'color_boost': High contrast color
 * - 'original': No modification
 */
export function applyDocumentFilter(canvas, filterType = 'magic', customAdjust = { brightness: 0, contrast: 0, sharpness: 0 }) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const w = canvas.width;
  const h = canvas.height;
  const imgData = ctx.getImageData(0, 0, w, h);
  const data = imgData.data;

  if (filterType === 'original' && customAdjust.brightness === 0 && customAdjust.contrast === 0) {
    return canvas;
  }

  // 1. Calculate image statistics for adaptive background leveling
  let totalLum = 0;
  const len = data.length;
  for (let i = 0; i < len; i += 4) {
    totalLum += 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
  }
  const avgLum = totalLum / (len / 4);

  // Magic Color parameters
  const whiteThreshold = Math.min(240, Math.max(160, avgLum * 1.12));
  const blackThreshold = Math.max(40, avgLum * 0.45);

  const contrastFactor = 1.35 + (customAdjust.contrast / 100);
  const brightnessOffset = 15 + customAdjust.brightness;

  for (let i = 0; i < len; i += 4) {
    let r = data[i];
    let g = data[i + 1];
    let b = data[i + 2];

    const lum = 0.299 * r + 0.587 * g + 0.114 * b;

    if (filterType === 'magic') {
      // CamScanner Magic Color Algorithm:
      // Paper background whitening & Text edge enhancement
      if (lum >= whiteThreshold) {
        // Boost near-white background towards clean paper white
        const boost = Math.min(255, lum + (255 - lum) * 0.75 + brightnessOffset);
        r = Math.min(255, r * (boost / lum));
        g = Math.min(255, g * (boost / lum));
        b = Math.min(255, b * (boost / lum));
      } else if (lum <= blackThreshold) {
        // Deepen dark text & lines
        r = Math.max(0, r * 0.65 - 10);
        g = Math.max(0, g * 0.65 - 10);
        b = Math.max(0, b * 0.65 - 10);
      } else {
        // Midtones contrast stretch
        const normalized = (lum - blackThreshold) / (whiteThreshold - blackThreshold);
        const curve = Math.pow(normalized, 1.4);
        const targetLum = curve * 255;
        const ratio = targetLum / (lum || 1);
        r = Math.min(255, Math.max(0, (r - 128) * contrastFactor + 128 + brightnessOffset));
        g = Math.min(255, Math.max(0, (g - 128) * contrastFactor + 128 + brightnessOffset));
        b = Math.min(255, Math.max(0, (b - 128) * contrastFactor + 128 + brightnessOffset));
      }
    } else if (filterType === 'bw') {
      // Super Clean Black & White Document
      const thresh = avgLum * 0.92;
      const val = lum > thresh ? 255 : 0;
      r = val;
      g = val;
      b = val;
    } else if (filterType === 'grayscale') {
      // Clean Grayscale
      let gray = (lum - 128) * (contrastFactor * 1.1) + 128 + brightnessOffset;
      if (gray > 220) gray = 255; // clean paper
      gray = Math.min(255, Math.max(0, gray));
      r = gray;
      g = gray;
      b = gray;
    } else if (filterType === 'color_boost') {
      // High Saturation Color
      r = Math.min(255, Math.max(0, (r - 128) * 1.4 + 128 + brightnessOffset));
      g = Math.min(255, Math.max(0, (g - 128) * 1.4 + 128 + brightnessOffset));
      b = Math.min(255, Math.max(0, (b - 128) * 1.4 + 128 + brightnessOffset));
    }

    data[i] = r;
    data[i + 1] = g;
    data[i + 2] = b;
  }

  ctx.putImageData(imgData, 0, 0);

  // Apply subtle unsharp masking for text sharpness if requested or in magic mode
  if (filterType === 'magic' || customAdjust.sharpness > 0) {
    applyUnsharpMask(ctx, w, h, customAdjust.sharpness || 25);
  }

  return canvas;
}

/**
 * Fast Text Sharpening Convolution Filter
 */
function applyUnsharpMask(ctx, w, h, amount = 25) {
  try {
    const imgData = ctx.getImageData(0, 0, w, h);
    const src = new Uint8ClampedArray(imgData.data);
    const dst = imgData.data;
    const factor = amount / 100;

    for (let y = 1; y < h - 1; y++) {
      for (let x = 1; x < w - 1; x++) {
        const idx = (y * w + x) * 4;

        for (let c = 0; c < 3; c++) {
          const center = src[idx + c];
          const up = src[((y - 1) * w + x) * 4 + c];
          const down = src[((y + 1) * w + x) * 4 + c];
          const left = src[(y * w + (x - 1)) * 4 + c];
          const right = src[(y * w + (x + 1)) * 4 + c];

          const edge = center * 5 - (up + down + left + right);
          const sharpened = center + (edge - center) * factor;

          dst[idx + c] = Math.min(255, Math.max(0, sharpened));
        }
      }
    }
    ctx.putImageData(imgData, 0, 0);
  } catch (e) {
    // Ignore in case of memory constraints
  }
}

/**
 * Downloads a scanned invoice file with the user-defined name.
 */
export function downloadScannedInvoiceFile(dataUrl, fileName = 'فاتورة_ممسوحة.jpg') {
  try {
    let cleanName = fileName.trim();
    if (!cleanName.endsWith('.jpg') && !cleanName.endsWith('.png') && !cleanName.endsWith('.jpeg')) {
      cleanName += '.jpg';
    }

    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = cleanName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (e) {
    console.error('Download error', e);
  }
}
