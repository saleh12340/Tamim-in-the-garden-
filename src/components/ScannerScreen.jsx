import React, { useState, useRef, useEffect } from 'react';
import { 
  Camera, 
  ScanLine, 
  Sparkles, 
  Check, 
  RotateCw, 
  Crop, 
  Download, 
  Trash2, 
  Share2, 
  Printer, 
  Maximize2, 
  Search, 
  Folder, 
  FileText, 
  RefreshCw, 
  Upload, 
  Sliders, 
  Image as ImageIcon, 
  X, 
  Plus,
  Eye,
  ZoomIn,
  ZoomOut
} from 'lucide-react';
import { 
  autoDetectDocumentBounds, 
  cropCanvas, 
  applyDocumentFilter, 
  downloadScannedInvoiceFile 
} from '../utils/imageProcessor';
import { getSampleInvoiceDataUrl } from '../utils/sampleInvoice';
import { formatDate } from '../storage';
import { shareViaWhatsApp } from '../utils/sharing';

export default function ScannerScreen({ data, onSaveScannedInvoice, onDeleteScannedInvoice, showToast }) {
  const { scannedInvoices = [], storeInfo = {} } = data;

  // View tabs: 'camera' | 'gallery'
  const [activeTab, setActiveTab] = useState('camera');

  // Camera & Stream State
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const fileInputRef = useRef(null);
  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState(null);
  const [facingMode, setFacingMode] = useState('environment'); // 'environment' (back) or 'user' (front)

  // Scanning & Processing Workflow States
  // step: 'stream' | 'adjust_crop' | 'naming_modal'
  const [currentStep, setCurrentStep] = useState('stream');
  const [rawImageCanvas, setRawImageCanvas] = useState(null);
  const [processedCanvas, setProcessedCanvas] = useState(null);
  const [previewDataUrl, setPreviewDataUrl] = useState(null);

  // Crop & Adjust State
  const [cropBox, setCropBox] = useState({ x: 0, y: 0, width: 0, height: 0 });
  const [rotation, setRotation] = useState(0);
  const [filterType, setFilterType] = useState('magic'); // 'magic' | 'bw' | 'grayscale' | 'color_boost' | 'original'
  const [adjustments, setAdjustments] = useState({ brightness: 0, contrast: 0, sharpness: 25 });
  const [showFiltersPanel, setShowFiltersPanel] = useState(false);

  // Naming Modal State (Required: naming dialog pops up after auto capture & crop)
  const [showNamingModal, setShowNamingModal] = useState(false);
  const [invoiceName, setInvoiceName] = useState('');
  const [invoiceCategory, setInvoiceCategory] = useState('مشتريات وتوريد');
  const [invoiceNotes, setInvoiceNotes] = useState('');

  // Gallery & Viewer State
  const [gallerySearch, setGallerySearch] = useState('');
  const [galleryCategory, setGalleryCategory] = useState('all');
  const [selectedInvoiceForView, setSelectedInvoiceForView] = useState(null);
  const [zoomLevel, setZoomLevel] = useState(1);

  // Initialize sample invoice if gallery is empty on first mount
  useEffect(() => {
    if (scannedInvoices.length === 0) {
      const sampleUrl = getSampleInvoiceDataUrl();
      onSaveScannedInvoice({
        id: 'scan_sample_1',
        name: 'فاتورة توريد بضاعة - شركة السعيد #8902',
        fileName: 'فاتورة_توريد_السعيد_8902.jpg',
        folder: 'invoices_images',
        category: 'مشتريات وتوريد',
        date: new Date().toISOString(),
        dataUrl: sampleUrl,
        sizeKB: 145,
        notes: 'فاتورة مورد المواد الغذائية - مدفوعة نقداً'
      });
    }
  }, []);

  // 1. Auto-start camera when entering 'camera' tab or 'stream' step
  useEffect(() => {
    if (activeTab === 'camera' && currentStep === 'stream') {
      startCamera();
    } else {
      stopCamera();
    }
    return () => {
      stopCamera();
    };
  }, [activeTab, currentStep, facingMode]);

  const startCamera = async () => {
    stopCamera();
    setCameraError(null);
    try {
      const constraints = {
        video: {
          facingMode: { ideal: facingMode },
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        },
        audio: false
      };
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play().catch(() => {});
      }
      setCameraActive(true);
    } catch (err) {
      console.warn('Camera access failed:', err);
      setCameraActive(false);
      setCameraError('لم نتمكن من الوصول التلقائي للكاميرا. يمكنك اختيار صورة من جهازك أو التحقق من أذونات المتصفح.');
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setCameraActive(false);
  };

  const toggleCameraFacing = () => {
    setFacingMode(prev => (prev === 'environment' ? 'user' : 'environment'));
  };

  // 2. Auto Capture, Edge Detection, Auto Crop & Magic Enhancement
  const handleCapturePhoto = () => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    processCapturedCanvas(canvas);
  };

  const handleFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.naturalWidth || img.width;
        canvas.height = img.naturalHeight || img.height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);

        processCapturedCanvas(canvas, file.name);
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  };

  const processCapturedCanvas = (canvas, originalFileName = '') => {
    stopCamera();
    setRawImageCanvas(canvas);
    setRotation(0);

    // 1. Auto-detect document boundaries (Smart edge detection)
    const detected = autoDetectDocumentBounds(canvas);
    setCropBox(detected);

    // 2. Automatically crop around the detected edges
    const cropped = cropCanvas(canvas, detected, 0);

    // 3. Automatically apply Magic CamScanner filter (Background clean & Text Sharpen)
    const enhanced = applyDocumentFilter(cropped, 'magic', { brightness: 0, contrast: 0, sharpness: 25 });
    setProcessedCanvas(enhanced);

    const dataUrl = enhanced.toDataURL('image/jpeg', 0.92);
    setPreviewDataUrl(dataUrl);

    // 4. Set default invoice name & Open Naming Modal immediately
    const today = new Date();
    const dateFormatted = `${today.getDate()}-${today.getMonth() + 1}`;
    const defaultName = originalFileName 
      ? originalFileName.replace(/\.[^/.]+$/, "") 
      : `فاتورة توريد ${dateFormatted}_#${scannedInvoices.length + 1}`;
    
    setInvoiceName(defaultName);
    setInvoiceCategory('مشتريات وتوريد');
    setInvoiceNotes('');
    setShowNamingModal(true);
    setCurrentStep('adjust_crop');

    if (showToast) showToast('⚡ تم التعرف على الفاتورة واقتصاصها وتحسينها تلقائياً!', 'success');
  };

  // Re-apply filters / crop adjustments
  const reprocessImage = (newFilter = filterType, newRotation = rotation, newCrop = cropBox, newAdj = adjustments) => {
    if (!rawImageCanvas) return;
    const cropped = cropCanvas(rawImageCanvas, newCrop, newRotation);
    const enhanced = applyDocumentFilter(cropped, newFilter, newAdj);
    setProcessedCanvas(enhanced);
    const dataUrl = enhanced.toDataURL('image/jpeg', 0.92);
    setPreviewDataUrl(dataUrl);
  };

  const handleRotate = () => {
    const nextRot = (rotation + 90) % 360;
    setRotation(nextRot);
    reprocessImage(filterType, nextRot, cropBox, adjustments);
  };

  const handleFilterChange = (fType) => {
    setFilterType(fType);
    reprocessImage(fType, rotation, cropBox, adjustments);
  };

  const handleResetCrop = () => {
    if (!rawImageCanvas) return;
    const fullBox = { x: 0, y: 0, width: rawImageCanvas.width, height: rawImageCanvas.height };
    setCropBox(fullBox);
    reprocessImage(filterType, rotation, fullBox, adjustments);
    if (showToast) showToast('تم إرجاع الصورة كاملة بدون اقتصاص', 'info');
  };

  const handleSmartAutoCrop = () => {
    if (!rawImageCanvas) return;
    const detected = autoDetectDocumentBounds(rawImageCanvas);
    setCropBox(detected);
    reprocessImage(filterType, rotation, detected, adjustments);
    if (showToast) showToast('تم الاقتصاص التلقائي الذكي للحدود', 'success');
  };

  // 3. Save Scanned Invoice into invoices_images folder
  const handleSaveInvoice = (andDownload = false) => {
    if (!previewDataUrl) return;

    const trimmedName = invoiceName.trim() || `فاتورة_${Date.now()}`;
    const cleanFileName = trimmedName.replace(/[/\\?%*:|"<>]/g, '_') + '.jpg';
    
    // Estimate size in KB
    const head = 'data:image/jpeg;base64,';
    const sizeKB = Math.round(((previewDataUrl.length - head.length) * 3 / 4) / 1024);

    const newScannedDoc = {
      id: 'scan_' + Date.now(),
      name: trimmedName,
      fileName: cleanFileName,
      folder: 'invoices_images',
      category: invoiceCategory,
      notes: invoiceNotes.trim(),
      date: new Date().toISOString(),
      dataUrl: previewDataUrl,
      sizeKB: sizeKB > 0 ? sizeKB : 120
    };

    onSaveScannedInvoice(newScannedDoc);

    if (andDownload) {
      downloadScannedInvoiceFile(previewDataUrl, cleanFileName);
      if (showToast) showToast(`تم الحفظ وتنزيل "${cleanFileName}" بجهازك`, 'success');
    } else {
      if (showToast) showToast(`✅ تم حفظ "${trimmedName}" في مجلد invoices_images`, 'success');
    }

    setShowNamingModal(false);
    setCurrentStep('stream');
    setRawImageCanvas(null);
    setProcessedCanvas(null);
    setPreviewDataUrl(null);
    setActiveTab('gallery'); // Switch to gallery to view newly saved invoice!
  };

  const handleRetake = () => {
    setShowNamingModal(false);
    setCurrentStep('stream');
    setRawImageCanvas(null);
    setProcessedCanvas(null);
    setPreviewDataUrl(null);
    startCamera();
  };

  // Gallery Filter
  const categoriesList = ['all', 'مشتريات وتوريد', 'مبيعات', 'سند قبض', 'مصاريف وفواتير', 'مستند رسمي', 'أخرى'];
  const filteredInvoices = scannedInvoices.filter(inv => {
    const matchesSearch = (inv.name || '').toLowerCase().includes(gallerySearch.toLowerCase()) ||
                          (inv.notes || '').toLowerCase().includes(gallerySearch.toLowerCase()) ||
                          (inv.fileName || '').toLowerCase().includes(gallerySearch.toLowerCase());
    if (!matchesSearch) return false;
    if (galleryCategory !== 'all' && inv.category !== galleryCategory) return false;
    return true;
  });

  return (
    <div className="scanner-screen">
      {/* Top Header & Tab Switcher */}
      <div className="section-title" style={{ marginBottom: 12 }}>
        <h2>
          <ScanLine size={24} color="#15803d" />
          <span>الماسح الضوئي الذكي للفواتير (CamScanner)</span>
        </h2>

        {/* View Switcher: Scanner Camera vs Gallery */}
        <div style={{ display: 'flex', gap: 6, background: '#f1f5f9', padding: 4, borderRadius: 10 }}>
          <button
            className={`btn btn-sm ${activeTab === 'camera' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => {
              setActiveTab('camera');
              setCurrentStep('stream');
            }}
          >
            <Camera size={16} />
            <span>كاميرا الماسح</span>
          </button>

          <button
            className={`btn btn-sm ${activeTab === 'gallery' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('gallery')}
          >
            <Folder size={16} />
            <span>معرض الفواتير ({scannedInvoices.length})</span>
          </button>
        </div>
      </div>

      {/* Hidden file input for gallery upload */}
      <input
        type="file"
        ref={fileInputRef}
        accept="image/*"
        style={{ display: 'none' }}
        onChange={handleFileUpload}
      />

      {/* ========================================================================= */}
      {/* TAB 1: CAMERA SCANNER & REAL-TIME DOCUMENT CAPTURE */}
      {/* ========================================================================= */}
      {activeTab === 'camera' && (
        <div>
          {/* STEP A: LIVE CAMERA STREAM VIEW */}
          {currentStep === 'stream' && (
            <div className="card" style={{ padding: 12, overflow: 'hidden' }}>
              <div style={{
                position: 'relative',
                background: '#090d16',
                borderRadius: 12,
                overflow: 'hidden',
                minHeight: 380,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                {/* Live Video */}
                <video
                  ref={videoRef}
                  playsInline
                  autoPlay
                  muted
                  style={{
                    width: '100%',
                    maxHeight: '65vh',
                    objectFit: 'contain',
                    display: cameraActive ? 'block' : 'none'
                  }}
                />

                {/* Animated CamScanner Laser / Edge Finder Overlay */}
                {cameraActive && (
                  <div className="scanner-viewfinder-overlay" style={{
                    position: 'absolute',
                    inset: '12% 10%',
                    border: '2px solid rgba(34, 197, 94, 0.85)',
                    borderRadius: 12,
                    boxShadow: '0 0 0 4000px rgba(0, 0, 0, 0.45)',
                    pointerEvents: 'none',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    padding: 8
                  }}>
                    {/* Viewfinder Corners */}
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div style={{ width: 24, height: 24, borderTop: '4px solid #22c55e', borderRight: '4px solid #22c55e' }} />
                      <div style={{ width: 24, height: 24, borderTop: '4px solid #22c55e', borderLeft: '4px solid #22c55e' }} />
                    </div>

                    {/* Animated Scanning Laser Line */}
                    <div className="scanner-laser-line" />

                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <div style={{ width: 24, height: 24, borderBottom: '4px solid #22c55e', borderRight: '4px solid #22c55e' }} />
                      <div style={{ width: 24, height: 24, borderBottom: '4px solid #22c55e', borderLeft: '4px solid #22c55e' }} />
                    </div>

                    {/* Instruction tip badge */}
                    <div style={{
                      position: 'absolute',
                      bottom: 12,
                      left: '50%',
                      transform: 'translateX(-50%)',
                      background: 'rgba(0, 0, 0, 0.75)',
                      color: '#a7f3d0',
                      padding: '4px 12px',
                      borderRadius: 20,
                      fontSize: '0.78rem',
                      fontWeight: 600,
                      whiteSpace: 'nowrap'
                    }}>
                      📐 وجّه الكاميرا نحو الفاتورة للاقتصاص التلقائي
                    </div>
                  </div>
                )}

                {/* Fallback & Loading state if camera inactive */}
                {!cameraActive && (
                  <div style={{ textAlign: 'center', padding: 30, color: '#94a3b8' }}>
                    <Camera size={48} style={{ opacity: 0.5, marginBottom: 12 }} />
                    <p style={{ color: '#e2e8f0', fontWeight: 700, marginBottom: 8 }}>
                      {cameraError || 'جاري تشغيل كاميرا الماسح الضوئي...'}
                    </p>
                    <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap', marginTop: 14 }}>
                      <button className="btn btn-primary btn-sm" onClick={startCamera}>
                        <RefreshCw size={15} />
                        <span>إعادة تشغيل الكاميرا</span>
                      </button>
                      <button className="btn btn-gold btn-sm" onClick={() => fileInputRef.current?.click()}>
                        <Upload size={15} />
                        <span>اختيار صورة فاتورة من جهازك</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Camera Action Controls Bar */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 6px 4px 6px',
                flexWrap: 'wrap',
                gap: 10
              }}>
                <button
                  className="btn btn-secondary btn-sm"
                  onClick={() => fileInputRef.current?.click()}
                  title="رفع صورة من المعرض"
                >
                  <Upload size={16} />
                  <span>معرض الصور</span>
                </button>

                {/* Big Shutter Trigger Button */}
                <button
                  className="btn btn-primary"
                  style={{
                    borderRadius: 35,
                    padding: '12px 28px',
                    fontSize: '1.05rem',
                    fontWeight: 900,
                    boxShadow: '0 4px 18px rgba(21, 128, 61, 0.4)',
                    background: 'linear-gradient(135deg, #15803d 0%, #16a34a 100%)'
                  }}
                  onClick={handleCapturePhoto}
                  disabled={!cameraActive}
                >
                  <ScanLine size={20} />
                  <span>التقاط واقتصاص الفاتورة ⚡</span>
                </button>

                <button
                  className="btn btn-secondary btn-sm btn-icon"
                  onClick={toggleCameraFacing}
                  title="تبديل الكاميرا (أمامية / خلفية)"
                >
                  <RotateCw size={17} />
                </button>
              </div>
            </div>
          )}

          {/* STEP B: AUTO-CROPPED & ENHANCED PREVIEW VIEW */}
          {currentStep === 'adjust_crop' && previewDataUrl && (
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 8 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <span className="badge badge-success">✨ اقتصاص وتحسين تلقائي</span>
                  <span style={{ fontSize: '0.82rem', color: '#64748b' }}>فلتر CamScanner مفعّل</span>
                </div>

                <div style={{ display: 'flex', gap: 6 }}>
                  <button className="btn btn-sm btn-secondary" onClick={handleRotate} title="تدوير 90 درجة">
                    <RotateCw size={14} />
                    <span>تدوير</span>
                  </button>
                  <button className="btn btn-sm btn-secondary" onClick={handleResetCrop} title="إلغاء الاقتصاص">
                    <Crop size={14} />
                    <span>كامل الصورة</span>
                  </button>
                  <button className="btn btn-sm btn-secondary" onClick={handleSmartAutoCrop} title="اقتصاص تلقائي للحدود">
                    <Sparkles size={14} color="#15803d" />
                    <span>اقتصاص ذكي</span>
                  </button>
                </div>
              </div>

              {/* Scanned Document Image Container */}
              <div style={{
                background: '#0f172a',
                borderRadius: 10,
                padding: 10,
                textAlign: 'center',
                maxHeight: '55vh',
                overflow: 'hidden',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <img
                  src={previewDataUrl}
                  alt="Scanned invoice"
                  style={{
                    maxHeight: '52vh',
                    maxWidth: '100%',
                    objectFit: 'contain',
                    borderRadius: 6,
                    boxShadow: '0 4px 20px rgba(0,0,0,0.4)'
                  }}
                />
              </div>

              {/* Magic Filter Presets Selector */}
              <div style={{ marginTop: 12 }}>
                <div style={{ fontSize: '0.82rem', fontWeight: 700, color: '#475569', marginBottom: 6 }}>
                  فلاتر المعالجة والتوضيح (CamScanner Filters):
                </div>
                <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4 }}>
                  {[
                    { id: 'magic', label: '🪄 سحري (Magic Color)', desc: 'توضيح النصوص وبياض الورق' },
                    { id: 'bw', label: '📄 أبيض وأسود فائق', desc: 'مستند ثنائي' },
                    { id: 'grayscale', label: '🔘 رمادي نقي', desc: 'درجات رمادية' },
                    { id: 'color_boost', label: '🎨 ألوان معززة', desc: 'ألوان مشبعة' },
                    { id: 'original', label: '🖼️ الأصلية', desc: 'بدون تعديل' },
                  ].map(f => (
                    <button
                      key={f.id}
                      className={`btn btn-sm ${filterType === f.id ? 'btn-primary' : 'btn-secondary'}`}
                      style={{ whiteSpace: 'nowrap', fontSize: '0.8rem' }}
                      onClick={() => handleFilterChange(f.id)}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Action Buttons: Save & Naming / Retake */}
              <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
                <button
                  className="btn btn-primary"
                  style={{ flex: 2, padding: '12px 18px', fontWeight: 800 }}
                  onClick={() => setShowNamingModal(true)}
                >
                  <Check size={18} />
                  <span>تسمية وحفظ الفاتورة ({invoiceName || 'فاتورة'})</span>
                </button>

                <button
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                  onClick={handleRetake}
                >
                  <Camera size={16} />
                  <span>إعادة التقاط</span>
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: INVOICES GALLERY & FILE STORAGE (invoices_images) */}
      {/* ========================================================================= */}
      {activeTab === 'gallery' && (
        <div className="scanned-gallery-view">
          {/* Search & Filter Bar */}
          <div className="card" style={{ marginBottom: 14 }}>
            <div className="form-row" style={{ alignItems: 'center' }}>
              <div className="form-group" style={{ flex: '2 1 200px', marginBottom: 0 }}>
                <div style={{ position: 'relative' }}>
                  <input
                    className="form-control"
                    placeholder="بحث في أسماء الفواتير والملاحظات..."
                    value={gallerySearch}
                    onChange={(e) => setGallerySearch(e.target.value)}
                    style={{ paddingRight: 38 }}
                  />
                  <Search size={18} style={{ position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', color: '#64748b' }} />
                </div>
              </div>

              <div className="form-group" style={{ flex: '1 1 150px', marginBottom: 0 }}>
                <select
                  className="form-control"
                  value={galleryCategory}
                  onChange={(e) => setGalleryCategory(e.target.value)}
                >
                  {categoriesList.map(cat => (
                    <option key={cat} value={cat}>
                      {cat === 'all' ? 'جميع التصنيفات' : cat}
                    </option>
                  ))}
                </select>
              </div>

              <button
                className="btn btn-primary btn-sm"
                onClick={() => {
                  setActiveTab('camera');
                  setCurrentStep('stream');
                }}
                style={{ height: 42 }}
              >
                <Plus size={16} />
                <span>مسح فاتورة جديدة</span>
              </button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.82rem', color: '#64748b', marginTop: 10, borderTop: '1px solid #e2e8f0', paddingTop: 8 }}>
              <span>📁 مجلد الحفظ: <strong>invoices_images</strong></span>
              <span>عدد الفواتير الممسوحة: <strong>{filteredInvoices.length}</strong></span>
            </div>
          </div>

          {/* Grid of Saved Invoices */}
          {filteredInvoices.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '40px 20px', color: '#64748b' }}>
              <FileText size={40} style={{ opacity: 0.35, marginBottom: 10 }} />
              <p style={{ fontWeight: 700 }}>لا توجد فواتير ممسوحة مطابقة للبحث</p>
              <button
                className="btn btn-primary btn-sm"
                style={{ marginTop: 12 }}
                onClick={() => {
                  setActiveTab('camera');
                  setCurrentStep('stream');
                }}
              >
                <Camera size={15} />
                <span>التقاط فاتورة الآن</span>
              </button>
            </div>
          ) : (
            <div style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
              gap: 14
            }}>
              {filteredInvoices.map((inv) => (
                <div
                  key={inv.id}
                  className="card"
                  style={{
                    padding: 0,
                    overflow: 'hidden',
                    display: 'flex',
                    flexDirection: 'column',
                    transition: 'all 0.18s ease',
                    border: '1.5px solid #e2e8f0'
                  }}
                >
                  {/* Thumbnail Image with overlay click to expand */}
                  <div
                    style={{
                      height: 180,
                      background: '#0f172a',
                      position: 'relative',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      overflow: 'hidden'
                    }}
                    onClick={() => {
                      setSelectedInvoiceForView(inv);
                      setZoomLevel(1);
                    }}
                  >
                    <img
                      src={inv.dataUrl}
                      alt={inv.name}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover',
                        transition: 'transform 0.2s ease'
                      }}
                    />
                    <div style={{
                      position: 'absolute',
                      inset: 0,
                      background: 'rgba(0,0,0,0.25)',
                      opacity: 0,
                      transition: 'opacity 0.2s',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'white',
                      fontWeight: 700,
                      gap: 6
                    }}
                    onMouseEnter={(e) => e.currentTarget.style.opacity = '1'}
                    onMouseLeave={(e) => e.currentTarget.style.opacity = '0'}
                    >
                      <Eye size={18} />
                      <span>معاينة وتكبير</span>
                    </div>

                    <span className="badge badge-success" style={{ position: 'absolute', top: 8, right: 8, fontSize: '0.72rem' }}>
                      {inv.category || 'مستند'}
                    </span>
                  </div>

                  {/* Info Card Content */}
                  <div style={{ padding: 12, flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      {/* Prominent User-Defined Name */}
                      <strong style={{ fontSize: '0.98rem', color: '#0f172a', display: 'block', marginBottom: 4, lineHeight: 1.35 }}>
                        {inv.name}
                      </strong>

                      <div style={{ fontSize: '0.78rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
                        <span>📅 {formatDate(inv.date)}</span>
                        <span>•</span>
                        <span>{inv.sizeKB || 120} KB</span>
                      </div>

                      {inv.notes && (
                        <div style={{ fontSize: '0.78rem', color: '#475569', background: '#f8fafc', padding: '4px 8px', borderRadius: 6, marginBottom: 8 }}>
                          {inv.notes}
                        </div>
                      )}
                    </div>

                    {/* Card Actions Bar */}
                    <div style={{
                      display: 'flex',
                      gap: 4,
                      borderTop: '1px solid #f1f5f9',
                      paddingTop: 8,
                      marginTop: 6,
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}>
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button
                          className="btn btn-sm btn-secondary btn-icon"
                          onClick={() => {
                            setSelectedInvoiceForView(inv);
                            setZoomLevel(1);
                          }}
                          title="معاينة كاملة"
                        >
                          <Maximize2 size={14} />
                        </button>

                        <button
                          className="btn btn-sm btn-secondary btn-icon"
                          onClick={() => downloadScannedInvoiceFile(inv.dataUrl, inv.fileName || inv.name + '.jpg')}
                          title="تنزيل الملف بجهازك"
                        >
                          <Download size={14} />
                        </button>

                        <button
                          className="btn btn-sm btn-whatsapp btn-icon"
                          onClick={() => {
                            const shareText = `🧾 فاتورة ممسوحة ضوئياً: ${inv.name}\n📅 التاريخ: ${formatDate(inv.date)}\nبقالة العنزي للمواد الغذائية`;
                            shareViaWhatsApp('', shareText);
                            if (showToast) showToast('جاري فتح واتساب...', 'success');
                          }}
                          title="مشاركة عبر واتساب"
                        >
                          <Share2 size={13} />
                        </button>
                      </div>

                      <button
                        className="btn btn-sm btn-danger btn-icon"
                        onClick={() => {
                          if (window.confirm(`هل أنت متأكد من حذف الفاتورة "${inv.name}"؟`)) {
                            onDeleteScannedInvoice(inv.id);
                            if (showToast) showToast('تم حذف الفاتورة من المعرض', 'success');
                          }
                        }}
                        title="حذف"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 1: NAMING DIALOG (رسالة التسمية المنبثقة) */}
      {/* ========================================================================= */}
      {showNamingModal && previewDataUrl && (
        <div className="modal-overlay" onClick={() => setShowNamingModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 480 }}>
            <div className="modal-header">
              <h3 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <FileText size={20} color="#15803d" />
                <span>تسمية وحفظ الفاتورة الممسوحة</span>
              </h3>
              <button className="btn btn-sm btn-secondary" onClick={() => setShowNamingModal(false)}>✕</button>
            </div>

            {/* Thumbnail Preview */}
            <div style={{
              display: 'flex',
              gap: 12,
              background: '#f8fafc',
              padding: 10,
              borderRadius: 10,
              marginBottom: 14,
              border: '1px solid #e2e8f0',
              alignItems: 'center'
            }}>
              <img
                src={previewDataUrl}
                alt="Thumbnail"
                style={{
                  width: 70,
                  height: 90,
                  objectFit: 'cover',
                  borderRadius: 6,
                  border: '1px solid #cbd5e1'
                }}
              />
              <div style={{ flex: 1, fontSize: '0.82rem', color: '#475569' }}>
                <div style={{ fontWeight: 800, color: '#15803d', marginBottom: 2 }}>
                  ✅ تم الاقتصاص والتحسين بنجاح
                </div>
                <div>📁 الحفظ في: <code>invoices_images/</code></div>
                <div style={{ fontSize: '0.76rem', color: '#64748b', marginTop: 2 }}>
                  {formatDate(new Date().toISOString())}
                </div>
              </div>
            </div>

            {/* Form Input: Invoice Name */}
            <div className="form-group">
              <label style={{ fontWeight: 800, color: '#0f172a' }}>
                اسم الفاتورة / المستند <span style={{ color: '#dc2626' }}>*</span>
              </label>
              <input
                className="form-control"
                placeholder="مثال: فاتورة توريد الألبان، فاتورة بضاعة #105..."
                value={invoiceName}
                onChange={(e) => setInvoiceName(e.target.value)}
                autoFocus
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSaveInvoice(false);
                }}
              />
            </div>

            {/* Form Input: Category */}
            <div className="form-group">
              <label>التصنيف</label>
              <select
                className="form-control"
                value={invoiceCategory}
                onChange={(e) => setInvoiceCategory(e.target.value)}
              >
                <option value="مشتريات وتوريد">مشتريات وتوريد بضائع</option>
                <option value="مبيعات">فاتورة مبيعات</option>
                <option value="سند قبض">سند قبض / إيصال سداد</option>
                <option value="مصاريف وفواتير">مصاريف (كهرباء، إيجار، نقل...)</option>
                <option value="مستند رسمي">مستند رسمي / ترخيص</option>
                <option value="أخرى">أخرى</option>
              </select>
            </div>

            {/* Optional Notes */}
            <div className="form-group">
              <label>ملاحظات إضافية (اختياري)</label>
              <input
                className="form-control"
                placeholder="مثال: مورد الألبان، تم السداد نقداً..."
                value={invoiceNotes}
                onChange={(e) => setInvoiceNotes(e.target.value)}
              />
            </div>

            {/* Action Buttons */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 18 }}>
              <button
                className="btn btn-primary"
                style={{ padding: '12px 16px', fontWeight: 800 }}
                onClick={() => handleSaveInvoice(false)}
              >
                <Check size={18} />
                <span>حفظ في مجلد الفواتير (invoices_images)</span>
              </button>

              <button
                className="btn btn-gold btn-sm"
                onClick={() => handleSaveInvoice(true)}
              >
                <Download size={15} />
                <span>حفظ وتنزيل الملف بجهازك مباشرة</span>
              </button>

              <button
                className="btn btn-secondary btn-sm"
                onClick={handleRetake}
              >
                <span>إعادة التقاط الصورة</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: FULLSCREEN IMAGE VIEWER & ZOOM MODAL */}
      {/* ========================================================================= */}
      {selectedInvoiceForView && (
        <div className="modal-overlay" onClick={() => setSelectedInvoiceForView(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: 720, width: '95vw', padding: 16 }}
          >
            <div className="modal-header">
              <div>
                <h3 style={{ fontSize: '1.1rem', marginBottom: 2 }}>{selectedInvoiceForView.name}</h3>
                <span style={{ fontSize: '0.78rem', color: '#64748b' }}>
                  {formatDate(selectedInvoiceForView.date)} • {selectedInvoiceForView.category || 'فاتورة'}
                </span>
              </div>
              <button className="btn btn-sm btn-secondary" onClick={() => setSelectedInvoiceForView(null)}>✕</button>
            </div>

            {/* Image Preview Box with Zoom */}
            <div style={{
              background: '#090d16',
              borderRadius: 10,
              overflow: 'auto',
              maxHeight: '65vh',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 10,
              position: 'relative'
            }}>
              <img
                src={selectedInvoiceForView.dataUrl}
                alt={selectedInvoiceForView.name}
                style={{
                  transform: `scale(${zoomLevel})`,
                  transformOrigin: 'center center',
                  transition: 'transform 0.15s ease',
                  maxWidth: '100%',
                  borderRadius: 4
                }}
              />
            </div>

            {/* Viewer Controls */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginTop: 12,
              flexWrap: 'wrap',
              gap: 8
            }}>
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <button
                  className="btn btn-sm btn-secondary btn-icon"
                  onClick={() => setZoomLevel(prev => Math.max(0.6, prev - 0.2))}
                  title="تصغير"
                >
                  <ZoomOut size={15} />
                </button>
                <span style={{ fontSize: '0.8rem', fontWeight: 700, minWidth: 45, textAlign: 'center' }}>
                  {Math.round(zoomLevel * 100)}%
                </span>
                <button
                  className="btn btn-sm btn-secondary btn-icon"
                  onClick={() => setZoomLevel(prev => Math.min(2.5, prev + 0.2))}
                  title="تكبير"
                >
                  <ZoomIn size={15} />
                </button>
              </div>

              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  className="btn btn-sm btn-primary"
                  onClick={() => downloadScannedInvoiceFile(selectedInvoiceForView.dataUrl, selectedInvoiceForView.fileName || selectedInvoiceForView.name + '.jpg')}
                >
                  <Download size={14} />
                  <span>تحميل الملف</span>
                </button>

                <button
                  className="btn btn-sm btn-whatsapp"
                  onClick={() => {
                    const text = `🧾 ${selectedInvoiceForView.name}\n📅 ${formatDate(selectedInvoiceForView.date)}\nبقالة العنزي`;
                    shareViaWhatsApp('', text);
                    if (showToast) showToast('جاري فتح واتساب...', 'success');
                  }}
                >
                  <Share2 size={14} />
                  <span>واتساب</span>
                </button>

                <button
                  className="btn btn-sm btn-secondary"
                  onClick={() => window.print()}
                >
                  <Printer size={14} />
                  <span>طباعة</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
