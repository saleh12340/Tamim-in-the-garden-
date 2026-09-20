import React from 'react';
import { 
  Printer, 
  Share2, 
  X, 
  MessageCircle, 
  Copy, 
  Check, 
  Phone, 
  Calendar,
  Store,
  FileText
} from 'lucide-react';
import { formatNumber, formatDate } from '../storage';
import { 
  generateInvoiceShareText, 
  shareViaWhatsApp, 
  shareContentNative, 
  copyToClipboard 
} from '../utils/sharing';

export default function ThermalReceiptModal({ receipt, storeInfo, onClose, showToast }) {
  if (!receipt) return null;

  const handlePrint = () => {
    window.print();
  };

  const invoiceShareText = generateInvoiceShareText(receipt, storeInfo);

  const handleWhatsAppShare = () => {
    shareViaWhatsApp(receipt.customerPhone, invoiceShareText);
    if (showToast) showToast('جاري فتح واتساب لمشاركة الفاتورة 💬', 'success');
  };

  const handleNativeShare = async () => {
    const res = await shareContentNative({
      title: `فاتورة #${receipt.number} - ${storeInfo?.name || 'بقالة العنزي'}`,
      text: invoiceShareText,
      phone: receipt.customerPhone
    });
    if (res.success && showToast) {
      showToast('تمت مشاركة الفاتورة بنجاح', 'success');
    }
  };

  const handleCopy = async () => {
    const success = await copyToClipboard(invoiceShareText);
    if (success && showToast) {
      showToast('تم نسخ نص الفاتورة للحافظة بنجاح 📋', 'success');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div 
        className="modal-content" 
        onClick={(e) => e.stopPropagation()} 
        style={{ maxWidth: 440, padding: 20 }}
      >
        <div className="modal-header">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Printer size={20} color="#15803d" />
            <span>معاينة وطباعة الإيصال الحراري</span>
          </h3>
          <button className="btn btn-sm btn-secondary" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        {/* Action Buttons Bar */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 14 }}>
          <button className="btn btn-primary" onClick={handlePrint} style={{ fontWeight: 800 }}>
            <Printer size={16} />
            <span>طباعة الإيصال</span>
          </button>
          
          <button className="btn btn-whatsapp" onClick={handleWhatsAppShare} style={{ fontWeight: 800 }}>
            <MessageCircle size={16} />
            <span>إرسال بالواتساب</span>
          </button>

          <button className="btn btn-secondary" onClick={handleNativeShare}>
            <Share2 size={16} />
            <span>مشاركة الرابط/النص</span>
          </button>

          <button className="btn btn-secondary" onClick={handleCopy}>
            <Copy size={16} />
            <span>نسخ نص الفاتورة</span>
          </button>
        </div>

        {/* Realistic Thermal Receipt Paper */}
        <div className="thermal-receipt" id="thermal-receipt-printable">
          {/* Header */}
          <div className="receipt-header">
            <h3>{storeInfo?.name || 'بقالة العنزي للمواد الغذائية'}</h3>
            <p>{storeInfo?.subtitle || 'مبيعات جملة وتجزئة - مواد غذائية واستهلاكية'}</p>
            <p>هاتف: {storeInfo?.phone || '776425052'}</p>
            <div style={{ margin: '4px 0', fontSize: '10px' }}>********************************</div>
            <p style={{ fontWeight: 'bold' }}>فاتورة مبيعات #{receipt.number}</p>
          </div>

          {/* Meta Info */}
          <div className="receipt-meta">
            <div>التاريخ: {formatDate(receipt.date)}</div>
            <div>العميل: {receipt.customerName || 'عميل نقدي'}</div>
            {receipt.customerPhone && <div>هاتف العميل: {receipt.customerPhone}</div>}
            <div>نوع السداد: {receipt.paymentType === 'cash' ? 'نقدي كامل' : 'آجل على الحساب'}</div>
          </div>

          <div className="receipt-divider"></div>

          {/* Items Table */}
          <table className="receipt-items-table">
            <thead>
              <tr>
                <th style={{ textAlign: 'right' }}>الصنف</th>
                <th style={{ textAlign: 'center' }}>الكمية</th>
                <th style={{ textAlign: 'center' }}>السعر</th>
                <th style={{ textAlign: 'left' }}>الإجمالي</th>
              </tr>
            </thead>
            <tbody>
              {receipt.items && receipt.items.map((it, idx) => (
                <tr key={idx}>
                  <td style={{ textAlign: 'right' }}>{it.name}</td>
                  <td style={{ textAlign: 'center' }}>{it.qty}</td>
                  <td style={{ textAlign: 'center' }}>{formatNumber(it.price)}</td>
                  <td style={{ textAlign: 'left', fontWeight: 'bold' }}>{formatNumber(it.total)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="receipt-divider"></div>

          {/* Totals */}
          <div className="receipt-summary">
            <div className="summary-row total-row">
              <span>الإجمالي الكلي:</span>
              <span>{formatNumber(receipt.total)} {storeInfo?.currency || 'ريال'}</span>
            </div>

            <div className="summary-row">
              <span>المبلغ المسدد:</span>
              <span>{formatNumber(receipt.paid || 0)} {storeInfo?.currency || 'ريال'}</span>
            </div>

            <div className="summary-row remaining-row">
              <span>المبلغ المتبقي:</span>
              <span>{formatNumber(receipt.remaining || 0)} {storeInfo?.currency || 'ريال'}</span>
            </div>
          </div>

          {receipt.notes && (
            <div style={{ fontSize: '11px', marginTop: 8, fontStyle: 'italic', textAlign: 'center' }}>
              ملاحظة: {receipt.notes}
            </div>
          )}

          {/* Footer */}
          <div className="receipt-footer">
            <p>شكراً لزيارتكم ونسعد بخدمتكم دائماً</p>
            <p>البضاعة المباعة تستبدل خلال 24 ساعة بموجب الفاتورة</p>
            <div style={{ marginTop: 6, fontSize: '9px', opacity: 0.8 }}>
              نظام بقالة العنزي لإدارة المبيعات والحسابات
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
